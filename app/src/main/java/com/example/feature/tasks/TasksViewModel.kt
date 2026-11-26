package com.example.feature.tasks

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TaskCategoryEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.storage.FileStorageManager
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.core.ui.components.startOfDayMillis
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TasksUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val categories: List<TaskCategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val orgTab: String = "TODAY", // TODAY, TOMORROW, UPCOMING, PENDING, COMPLETED, HISTORY
    val categoryFilter: String = "ALL",
    val sortMode: String = "DATE" // DATE, PRIORITY, TITLE
)

class TasksViewModel(
    private val taskRepo: TaskRepository,
    private val reminderRepo: ReminderRepository,
    val fileStorage: FileStorageManager,
    private val settingsStore: AppSettingsDataStore
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val orgTab = MutableStateFlow("TODAY")
    private val categoryFilter = MutableStateFlow("ALL")
    private val sortMode = MutableStateFlow("DATE")

    private val filterInputs = combine(searchQuery, orgTab, categoryFilter, sortMode) { q, tab, cat, sort ->
        arrayOf(q, tab, cat, sort)
    }

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepo.allTasks,
        taskRepo.categories,
        filterInputs
    ) { tasks, categories, filters ->
        val q = filters[0]
        val tab = filters[1]
        val cat = filters[2]
        val sort = filters[3]

        val todayStart = startOfDayMillis()
        val tomorrowStart = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val dayAfterTomorrowStart = Calendar.getInstance().apply {
            timeInMillis = tomorrowStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        val filtered = tasks.filter { task ->
            val matchQ = q.isBlank() ||
                task.title.contains(q, ignoreCase = true) ||
                task.description.contains(q, ignoreCase = true)
            val matchCat = cat == "ALL" || task.categoryName.equals(cat, ignoreCase = true)
            val matchTab = when (tab) {
                "TODAY" -> !task.isCompleted && task.dueDateMillis in todayStart until tomorrowStart
                "TOMORROW" -> !task.isCompleted && task.dueDateMillis in tomorrowStart until dayAfterTomorrowStart
                "UPCOMING" -> !task.isCompleted && task.dueDateMillis >= dayAfterTomorrowStart
                "PENDING" -> !task.isCompleted
                "COMPLETED" -> task.isCompleted
                "HISTORY" -> true
                else -> true
            }
            matchQ && matchCat && matchTab
        }

        val sorted = when (sort) {
            "PRIORITY" -> filtered.sortedBy {
                when (it.priority) {
                    "HIGH" -> 0
                    "MEDIUM" -> 1
                    else -> 2
                }
            }
            "TITLE" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedWith(compareBy<TaskEntity> { it.dueDateMillis }.thenBy { it.timeHour }.thenBy { it.timeMinute })
        }

        TasksUiState(
            tasks = sorted,
            categories = categories,
            searchQuery = q,
            orgTab = tab,
            categoryFilter = cat,
            sortMode = sort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun setSearch(q: String) { searchQuery.value = q }
    fun setOrgTab(t: String) { orgTab.value = t }
    fun setCategoryFilter(c: String) { categoryFilter.value = c }
    fun setSortMode(s: String) { sortMode.value = s }

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

    fun saveTask(
        existing: TaskEntity?,
        title: String,
        description: String,
        dueDateMillis: Long,
        hour: Int,
        minute: Int,
        priority: String,
        deadlineMillis: Long?,
        categoryName: String,
        imageUri: Uri?,
        docUri: Uri?,
        reminderDraft: ReminderDraft,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            var imgPath = existing?.imagePath
            if (imageUri != null) {
                fileStorage.importUriToPrivateStorage(imageUri, "IMAGE").onSuccess {
                    imgPath?.let { old -> fileStorage.deleteFile(old) }
                    imgPath = it.filePath
                }
            }
            var docPath = existing?.documentPath
            var docName = existing?.documentName
            if (docUri != null) {
                fileStorage.importUriToPrivateStorage(docUri).onSuccess {
                    docPath?.let { old -> fileStorage.deleteFile(old) }
                    docPath = it.filePath
                    docName = it.displayName
                }
            }

            val triggerMillis = nextFutureTriggerMillis(
                dateMillis = dueDateMillis,
                hour = reminderDraft.hour,
                minute = reminderDraft.minute,
                repeatType = reminderDraft.repeatType
            )
            val reminder = if (reminderDraft.enabled) {
                ReminderEntity(
                    id = existing?.reminderId ?: 0L,
                    title = title.trim(),
                    description = description.trim(),
                    featureType = "TASK",
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

            val task = (existing ?: TaskEntity(title = title.trim())).copy(
                title = title.trim(),
                description = description.trim(),
                dueDateMillis = startOfDayMillis(dueDateMillis),
                timeHour = hour,
                timeMinute = minute,
                priority = priority,
                deadlineMillis = deadlineMillis,
                categoryName = categoryName,
                repeatType = if (reminderDraft.enabled) reminderDraft.repeatType else "ONCE",
                imagePath = imgPath,
                documentPath = docPath,
                documentName = docName
            )
            val (_, exactOk) = taskRepo.saveTask(task, reminder)
            onDone(exactOk)
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        viewModelScope.launch { taskRepo.toggleCompleted(task) }
    }

    fun changePriority(task: TaskEntity, newPriority: String) {
        viewModelScope.launch { taskRepo.saveTask(task.copy(priority = newPriority), null) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepo.deleteTask(task) }
    }

    fun addCustomCategory(name: String) {
        viewModelScope.launch { taskRepo.addCategory(name) }
    }
}
