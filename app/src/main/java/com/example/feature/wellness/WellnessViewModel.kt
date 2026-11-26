package com.example.feature.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.SleepRecordEntity
import com.example.core.database.entity.WaterIntakeEntity
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.media.AudioPlaybackManager
import com.example.core.storage.FileStorageManager
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.data.repository.ReminderRepository
import com.example.data.repository.WellnessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class WellnessUiState(
    val waterIntakes: List<WaterIntakeEntity> = emptyList(),
    val sleepRecords: List<SleepRecordEntity> = emptyList(),
    val waterDailyGoalMl: Int = 2000
)

class WellnessViewModel(
    private val wellnessRepository: WellnessRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsStore: AppSettingsDataStore,
    val fileStorage: FileStorageManager,
    val audioPlayer: AudioPlaybackManager
) : ViewModel() {
    val uiState: StateFlow<WellnessUiState> = combine(
        wellnessRepository.waterIntakes,
        wellnessRepository.sleepRecords,
        settingsStore.settingsFlow
    ) { water, sleep, settings ->
        WellnessUiState(water, sleep, settings.waterDailyGoalMl)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WellnessUiState())

    suspend fun getReminderDraft(featureType: String, linkedId: Long, defaultHour: Int): ReminderDraft {
        val settings = settingsStore.settingsFlow.first()
        val base = ReminderDraft(
            hour = defaultHour,
            repeatType = "DAILY",
            ringtoneUri = settings.defaultRingtoneUri,
            ringingDurationSec = settings.defaultRingingDurationSec,
            snoozeMinutes = settings.defaultSnoozeMinutes,
            vibrationEnabled = settings.defaultVibration
        )
        val reminder = reminderRepository.getReminderForEntity(featureType, linkedId) ?: return base
        val calendar = Calendar.getInstance().apply { timeInMillis = reminder.triggerTimeMillis }
        return base.copy(
            enabled = reminder.isEnabled,
            dateMillis = reminder.triggerTimeMillis,
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            repeatType = reminder.repeatType,
            repeatIntervalDays = reminder.repeatIntervalDays,
            weekdays = reminder.weekdaysCsv.split(',').mapNotNull(String::toIntOrNull).toSet(),
            soundEnabled = reminder.soundEnabled,
            vibrationEnabled = reminder.vibrationEnabled,
            notificationEnabled = reminder.notificationEnabled,
            ringtoneUri = reminder.ringtoneUri,
            ringingDurationSec = reminder.ringingDurationSec,
            snoozeMinutes = reminder.snoozeMinutes
        )
    }

    fun saveReminderDraft(
        featureType: String,
        linkedId: Long,
        title: String,
        description: String,
        draft: ReminderDraft,
        onSaved: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val existing = reminderRepository.getReminderForEntity(featureType, linkedId)
                if (!draft.enabled) {
                    reminderRepository.deleteReminderById(existing?.id)
                    return@runCatching true
                }
                val reminder = ReminderEntity(
                    id = existing?.id ?: 0L,
                    title = title,
                    description = description,
                    featureType = featureType,
                    linkedEntityId = linkedId,
                    triggerTimeMillis = nextFutureTriggerMillis(
                        dateMillis = draft.dateMillis,
                        hour = draft.hour,
                        minute = draft.minute,
                        repeatType = draft.repeatType,
                    ),
                    repeatType = draft.repeatType,
                    repeatIntervalDays = draft.repeatIntervalDays,
                    weekdaysCsv = draft.weekdays.joinToString(","),
                    isEnabled = true,
                    soundEnabled = draft.soundEnabled,
                    vibrationEnabled = draft.vibrationEnabled,
                    notificationEnabled = draft.notificationEnabled,
                    ringtoneUri = draft.ringtoneUri,
                    ringingDurationSec = draft.ringingDurationSec,
                    snoozeMinutes = draft.snoozeMinutes
                )
                reminderRepository.saveAndSchedule(reminder).second
            }.onSuccess { onSaved(Result.success(it)) }
                .onFailure { onSaved(Result.failure(it)) }
        }
    }

    fun setWaterGoal(goalMl: Int) {
        viewModelScope.launch { settingsStore.setWaterDailyGoalMl(goalMl) }
    }

    fun addWater(amountMl: Int, note: String, onDone: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { wellnessRepository.addWater(amountMl, note) })
        }
    }

    fun deleteWater(entry: WaterIntakeEntity) {
        viewModelScope.launch { wellnessRepository.deleteWater(entry) }
    }

    fun addSleep(bedtimeMillis: Long, wakeTimeMillis: Long, notes: String, onDone: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { wellnessRepository.addSleep(bedtimeMillis, wakeTimeMillis, notes) })
        }
    }

    fun deleteSleep(record: SleepRecordEntity) {
        viewModelScope.launch { wellnessRepository.deleteSleep(record) }
    }
}

fun sleepDurationMinutes(bedtimeMillis: Long, wakeTimeMillis: Long): Long =
    ((wakeTimeMillis - bedtimeMillis).coerceAtLeast(0L) / 60_000L)
