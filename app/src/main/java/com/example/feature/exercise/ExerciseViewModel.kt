package com.example.feature.exercise

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.ExerciseAttachmentEntity
import com.example.core.database.entity.ExerciseEntity
import com.example.core.database.entity.ExerciseSessionEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.storage.FileStorageManager
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ExerciseListUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val sessions: List<ExerciseSessionEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "ALL",
    val statusTab: String = "ACTIVE" // ACTIVE, COMPLETED, ALL
)

class ExerciseViewModel(
    private val exerciseRepo: ExerciseRepository,
    private val reminderRepo: ReminderRepository,
    val fileStorage: FileStorageManager,
    private val settingsStore: AppSettingsDataStore
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("ALL")
    private val statusTab = MutableStateFlow("ACTIVE")

    val uiState: StateFlow<ExerciseListUiState> = combine(
        exerciseRepo.allExercises,
        exerciseRepo.allSessions,
        searchQuery,
        selectedCategory,
        statusTab
    ) { exercises, sessions, query, cat, tab ->
        val filtered = exercises.filter { ex ->
            val matchQ = query.isBlank() ||
                ex.name.contains(query, ignoreCase = true) ||
                ex.description.contains(query, ignoreCase = true)
            val matchCat = cat == "ALL" || ex.category.equals(cat, ignoreCase = true)
            val matchTab = when (tab) {
                "ACTIVE" -> !ex.isCompleted
                "COMPLETED" -> ex.isCompleted
                else -> true
            }
            matchQ && matchCat && matchTab
        }
        ExerciseListUiState(
            exercises = filtered,
            sessions = sessions,
            searchQuery = query,
            selectedCategory = cat,
            statusTab = tab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseListUiState())

    fun setSearch(q: String) { searchQuery.value = q }
    fun setCategory(c: String) { selectedCategory.value = c }
    fun setStatusTab(t: String) { statusTab.value = t }

    fun observeExercise(id: Long): Flow<ExerciseEntity?> = exerciseRepo.observeExercise(id)
    fun observeAttachments(exerciseId: Long): Flow<List<ExerciseAttachmentEntity>> =
        exerciseRepo.observeAttachments(exerciseId)

    suspend fun getDefaultReminderDraft(): ReminderDraft {
        val s = settingsStore.settingsFlow.first()
        return ReminderDraft(
            enabled = false,
            ringtoneUri = s.defaultRingtoneUri,
            ringingDurationSec = s.defaultRingingDurationSec,
            snoozeMinutes = s.defaultSnoozeMinutes,
            vibrationEnabled = s.defaultVibration
        )
    }

    suspend fun loadReminder(reminderId: Long?): ReminderDraft {
        val base = getDefaultReminderDraft()
        if (reminderId == null) return base
        val r = reminderRepo.getReminderById(reminderId) ?: return base
        val cal = Calendar.getInstance().apply { timeInMillis = r.triggerTimeMillis }
        return ReminderDraft(
            enabled = r.isEnabled,
            dateMillis = r.triggerTimeMillis,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            repeatType = r.repeatType,
            repeatIntervalDays = r.repeatIntervalDays,
            weekdays = r.weekdaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
            soundEnabled = r.soundEnabled,
            vibrationEnabled = r.vibrationEnabled,
            notificationEnabled = r.notificationEnabled,
            ringtoneUri = r.ringtoneUri,
            ringingDurationSec = r.ringingDurationSec,
            snoozeMinutes = r.snoozeMinutes
        )
    }

    fun saveExercise(
        existing: ExerciseEntity?,
        name: String,
        category: String,
        description: String,
        instructions: String,
        durationMin: Int,
        sets: Int,
        reps: Int,
        restSec: Int,
        difficulty: String,
        startDate: Long,
        notes: String,
        reminderDraft: ReminderDraft,
        onSaved: (Long, Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val triggerMillis = nextFutureTriggerMillis(
                dateMillis = startDate,
                hour = reminderDraft.hour,
                minute = reminderDraft.minute,
                repeatType = reminderDraft.repeatType
            )
            val reminder = if (reminderDraft.enabled) {
                ReminderEntity(
                    id = existing?.reminderId ?: 0L,
                    title = name.trim(),
                    description = "$sets×$reps • ${durationMin}m",
                    featureType = "EXERCISE",
                    linkedEntityId = existing?.id,
                    triggerTimeMillis = triggerMillis,
                    repeatType = reminderDraft.repeatType,
                    repeatIntervalDays = reminderDraft.repeatIntervalDays,
                    weekdaysCsv = reminderDraft.weekdays.joinToString(","),
                    isEnabled = true,
                    soundEnabled = true,
                    vibrationEnabled = reminderDraft.vibrationEnabled,
                    notificationEnabled = true,
                    ringtoneUri = null,
                    ringingDurationSec = reminderDraft.ringingDurationSec,
                    snoozeMinutes = reminderDraft.snoozeMinutes
                )
            } else null

            val entity = (existing ?: ExerciseEntity(name = name.trim())).copy(
                name = name.trim(),
                category = category,
                description = description.trim(),
                instructions = instructions.trim(),
                durationMinutes = durationMin.coerceAtLeast(1),
                sets = sets.coerceAtLeast(1),
                repetitions = reps.coerceAtLeast(1),
                restSeconds = restSec.coerceAtLeast(0),
                difficulty = difficulty,
                startDateMillis = startDate,
                notes = notes.trim()
            )
            val (id, exactOk) = exerciseRepo.saveExercise(entity, reminder)
            onSaved(id, exactOk)
        }
    }

    fun toggleCompleted(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepo.saveExercise(exercise.copy(isCompleted = !exercise.isCompleted), null)
        }
    }

    fun deleteExercise(exercise: ExerciseEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            exerciseRepo.deleteExercise(exercise)
            onDone()
        }
    }

    fun addAttachment(exerciseId: Long, uri: Uri, forcedType: String? = null, onError: () -> Unit = {}) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri, forcedType).onSuccess { imported ->
                exerciseRepo.addAttachment(
                    ExerciseAttachmentEntity(
                        exerciseId = exerciseId,
                        title = imported.displayName,
                        mediaType = imported.mediaType,
                        filePath = imported.filePath,
                        mimeType = imported.mimeType
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun replaceAttachment(existing: ExerciseAttachmentEntity, newUri: Uri, onError: () -> Unit = {}) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(newUri).onSuccess { imported ->
                fileStorage.deleteFile(existing.filePath)
                exerciseRepo.updateAttachment(
                    existing.copy(
                        title = imported.displayName,
                        mediaType = imported.mediaType,
                        filePath = imported.filePath,
                        mimeType = imported.mimeType
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun updateAttachmentMeta(attachment: ExerciseAttachmentEntity, newTitle: String, newCaption: String, newOrder: Int) {
        viewModelScope.launch {
            exerciseRepo.updateAttachment(
                attachment.copy(
                    title = newTitle.trim().ifBlank { attachment.title },
                    caption = newCaption.trim(),
                    sortOrder = newOrder
                )
            )
        }
    }

    fun deleteAttachment(attachment: ExerciseAttachmentEntity) {
        viewModelScope.launch { exerciseRepo.deleteAttachment(attachment) }
    }

    fun recordCompletedSession(exercise: ExerciseEntity, durationSec: Int, setsDone: Int, repsDone: Int) {
        viewModelScope.launch {
            exerciseRepo.recordSession(
                ExerciseSessionEntity(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    completedDurationSec = durationSec.coerceAtLeast(1),
                    setsCompleted = setsDone,
                    repetitionsCompleted = repsDone,
                    completedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }
}
