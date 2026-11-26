package com.example.feature.medicine

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.MedicineDoseLogEntity
import com.example.core.database.entity.MedicineEntity
import com.example.core.database.entity.MedicineScheduleEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.storage.FileStorageManager
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.combineDateAndTime
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.core.ui.components.startOfDayMillis
import com.example.data.repository.MedicineRepository
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedicineUiState(
    val medicines: List<MedicineEntity> = emptyList(),
    val activeSchedules: List<MedicineScheduleEntity> = emptyList(),
    val doseLogs: List<MedicineDoseLogEntity> = emptyList(),
    val searchQuery: String = "",
    val filterTab: String = "ACTIVE" // ACTIVE, COMPLETED, ALL
)

class MedicineViewModel(
    private val medicineRepo: MedicineRepository,
    private val reminderRepo: ReminderRepository,
    val fileStorage: FileStorageManager,
    private val settingsStore: AppSettingsDataStore
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val filterTab = MutableStateFlow("ACTIVE")

    val allReminders: StateFlow<List<ReminderEntity>> = reminderRepo.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<MedicineUiState> = combine(
        medicineRepo.allMedicines,
        medicineRepo.activeSchedules,
        medicineRepo.allDoseLogs,
        searchQuery,
        filterTab
    ) { meds, schedules, logs, q, tab ->
        val filtered = meds.filter { med ->
            val matchesQuery = q.isBlank() ||
                med.name.contains(q, ignoreCase = true) ||
                med.prescribedBy.contains(q, ignoreCase = true) ||
                med.medicineType.contains(q, ignoreCase = true)
            val matchesTab = when (tab) {
                "ACTIVE" -> !med.isCompleted
                "COMPLETED" -> med.isCompleted
                else -> true
            }
            matchesQuery && matchesTab
        }
        MedicineUiState(
            medicines = filtered,
            activeSchedules = schedules,
            doseLogs = logs,
            searchQuery = q,
            filterTab = tab
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MedicineUiState())

    fun setSearch(query: String) { searchQuery.value = query }
    fun setFilterTab(tab: String) { filterTab.value = tab }

    fun observeMedicine(id: Long): Flow<MedicineEntity?> = medicineRepo.observeMedicine(id)
    fun observeSchedules(medicineId: Long): Flow<List<MedicineScheduleEntity>> =
        medicineRepo.observeSchedulesForMedicine(medicineId)

    suspend fun getDefaultReminderDraft(): ReminderDraft {
        val s = settingsStore.settingsFlow.first()
        return ReminderDraft(
            enabled = true,
            repeatType = "DAILY",
            ringtoneUri = s.defaultRingtoneUri,
            ringingDurationSec = s.defaultRingingDurationSec,
            snoozeMinutes = s.defaultSnoozeMinutes,
            vibrationEnabled = s.defaultVibration
        )
    }

    suspend fun loadReminderForSchedule(reminderId: Long?): ReminderDraft {
        val defaultDraft = getDefaultReminderDraft()
        if (reminderId == null) return defaultDraft.copy(enabled = false)
        val r = reminderRepo.getReminderById(reminderId) ?: return defaultDraft.copy(enabled = false)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = r.triggerTimeMillis }
        return ReminderDraft(
            enabled = r.isEnabled,
            dateMillis = r.triggerTimeMillis,
            hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            minute = cal.get(java.util.Calendar.MINUTE),
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

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            reminderRepo.toggleReminder(reminder, !reminder.isEnabled)
        }
    }

    fun saveMedicine(
        existing: MedicineEntity?,
        name: String,
        strength: String,
        type: String,
        dosage: String,
        startDate: Long,
        endDate: Long?,
        prescribedBy: String,
        notes: String,
        imageUri: Uri?,
        docUri: Uri?,
        onSaved: (Long) -> Unit
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
            val entity = (existing ?: MedicineEntity(name = name)).copy(
                name = name.trim(),
                strength = strength.trim(),
                medicineType = type,
                dosageInstructions = dosage.trim(),
                startDateMillis = startDate,
                endDateMillis = endDate,
                prescribedBy = prescribedBy.trim(),
                notes = notes.trim(),
                imagePath = imgPath,
                documentPath = docPath,
                documentName = docName
            )
            val id = medicineRepo.saveMedicine(entity)
            onSaved(id)
        }
    }

    fun toggleCompleted(medicine: MedicineEntity) {
        viewModelScope.launch {
            medicineRepo.saveMedicine(medicine.copy(isCompleted = !medicine.isCompleted))
        }
    }

    fun deleteMedicine(medicine: MedicineEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            medicineRepo.deleteMedicine(medicine)
            onDone()
        }
    }

    fun saveSchedule(
        medicine: MedicineEntity,
        existingSchedule: MedicineScheduleEntity?,
        slotType: String,
        mealTiming: String,
        hour: Int,
        minute: Int,
        startDateMillis: Long,
        endDateMillis: Long?,
        reminderDraft: ReminderDraft,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val triggerMillis = nextFutureTriggerMillis(
                dateMillis = reminderDraft.dateMillis,
                hour = hour,
                minute = minute,
                repeatType = reminderDraft.repeatType
            )
            val reminder = if (reminderDraft.enabled) {
                ReminderEntity(
                    id = existingSchedule?.reminderId ?: 0L,
                    title = "${medicine.name} ${medicine.strength}".trim(),
                    description = medicine.dosageInstructions.ifBlank { mealTiming },
                    featureType = "MEDICINE",
                    linkedEntityId = medicine.id,
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

            val sched = (existingSchedule ?: MedicineScheduleEntity(medicineId = medicine.id)).copy(
                medicineId = medicine.id,
                slotType = slotType,
                mealTiming = mealTiming,
                hour = hour,
                minute = minute,
                repeatType = reminderDraft.repeatType,
                repeatIntervalDays = reminderDraft.repeatIntervalDays,
                weekdaysCsv = reminderDraft.weekdays.joinToString(","),
                startDateMillis = startDateMillis,
                endDateMillis = endDateMillis,
                isEnabled = true
            )
            val (_, exactOk) = medicineRepo.saveSchedule(sched, reminder)
            onComplete(exactOk)
        }
    }

    fun deleteSchedule(schedule: MedicineScheduleEntity) {
        viewModelScope.launch { medicineRepo.deleteSchedule(schedule) }
    }

    fun logDose(medicineId: Long, schedule: MedicineScheduleEntity, medicineName: String, status: String) {
        viewModelScope.launch {
            val scheduledToday = combineDateAndTime(startOfDayMillis(), schedule.hour, schedule.minute)
            medicineRepo.logDose(
                medicineId = medicineId,
                scheduleId = schedule.id,
                medicineName = medicineName,
                scheduledMillis = scheduledToday,
                status = status
            )
        }
    }
}
