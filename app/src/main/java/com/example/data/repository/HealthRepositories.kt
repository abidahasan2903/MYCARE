package com.example.data.repository

import com.example.core.database.dao.ExerciseDao
import com.example.core.database.dao.MedicalRecordsDao
import com.example.core.database.dao.MedicineDao
import com.example.core.database.dao.NutritionDao
import com.example.core.database.dao.ReminderDao
import com.example.core.database.entity.ExerciseAttachmentEntity
import com.example.core.database.entity.ExerciseEntity
import com.example.core.database.entity.ExerciseSessionEntity
import com.example.core.database.entity.FoodChartFileEntity
import com.example.core.database.entity.MedicalDocumentEntity
import com.example.core.database.entity.MedicalFolderEntity
import com.example.core.database.entity.MedicineDoseLogEntity
import com.example.core.database.entity.MedicineEntity
import com.example.core.database.entity.MedicineScheduleEntity
import com.example.core.database.entity.NutritionEntryEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.reminders.ReminderScheduler
import com.example.core.storage.FileStorageManager
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler
) {
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.observeAllReminders()

    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)

    suspend fun getReminderForEntity(featureType: String, linkedId: Long): ReminderEntity? =
        reminderDao.getReminderForEntity(featureType, linkedId)

    suspend fun saveAndSchedule(reminder: ReminderEntity): Pair<Long, Boolean> {
        val id = reminderDao.insertReminder(reminder)
        val saved = reminder.copy(id = id)
        val exactOk = scheduler.scheduleReminder(saved)
        return id to exactOk
    }

    suspend fun toggleReminder(reminder: ReminderEntity, enabled: Boolean) {
        val updated = reminder.copy(isEnabled = enabled)
        reminderDao.updateReminder(updated)
        if (enabled) scheduler.scheduleReminder(updated) else scheduler.cancelReminder(updated.id)
    }

    suspend fun deleteReminderById(id: Long?) {
        if (id == null || id <= 0L) return
        scheduler.cancelReminder(id)
        reminderDao.deleteReminderById(id)
    }
}

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val reminderRepo: ReminderRepository,
    private val fileStorage: FileStorageManager
) {
    val allMedicines: Flow<List<MedicineEntity>> = medicineDao.observeAllMedicines()
    val activeSchedules: Flow<List<MedicineScheduleEntity>> = medicineDao.observeActiveSchedules()
    val allDoseLogs: Flow<List<MedicineDoseLogEntity>> = medicineDao.observeAllDoseLogs()

    fun observeMedicine(id: Long): Flow<MedicineEntity?> = medicineDao.observeMedicine(id)
    fun observeSchedulesForMedicine(medicineId: Long): Flow<List<MedicineScheduleEntity>> =
        medicineDao.observeSchedulesForMedicine(medicineId)

    suspend fun saveMedicine(medicine: MedicineEntity): Long = medicineDao.insertMedicine(medicine)

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        val schedules = medicineDao.getAllSchedulesSync().filter { it.medicineId == medicine.id }
        schedules.forEach { reminderRepo.deleteReminderById(it.reminderId) }
        fileStorage.deleteFile(medicine.imagePath)
        fileStorage.deleteFile(medicine.documentPath)
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun saveSchedule(schedule: MedicineScheduleEntity, reminder: ReminderEntity?): Pair<Long, Boolean> {
        var reminderId = schedule.reminderId
        var exactOk = true
        if (reminder != null) {
            val result = reminderRepo.saveAndSchedule(
                reminder.copy(id = reminderId ?: 0L, featureType = "MEDICINE", linkedEntityId = schedule.medicineId)
            )
            reminderId = result.first
            exactOk = result.second
        } else if (reminderId != null) {
            reminderRepo.deleteReminderById(reminderId)
            reminderId = null
        }
        val id = medicineDao.insertSchedule(schedule.copy(reminderId = reminderId))
        return id to exactOk
    }

    suspend fun deleteSchedule(schedule: MedicineScheduleEntity) {
        reminderRepo.deleteReminderById(schedule.reminderId)
        medicineDao.deleteSchedule(schedule)
    }

    suspend fun logDose(
        medicineId: Long,
        scheduleId: Long?,
        medicineName: String,
        scheduledMillis: Long,
        status: String
    ) {
        medicineDao.insertDoseLog(
            MedicineDoseLogEntity(
                medicineId = medicineId,
                scheduleId = scheduleId,
                medicineName = medicineName,
                scheduledMillis = scheduledMillis,
                status = status,
                actionTimestampMillis = System.currentTimeMillis()
            )
        )
    }
}

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val reminderRepo: ReminderRepository,
    private val fileStorage: FileStorageManager
) {
    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.observeAllExercises()
    val allSessions: Flow<List<ExerciseSessionEntity>> = exerciseDao.observeAllSessions()

    fun observeExercise(id: Long): Flow<ExerciseEntity?> = exerciseDao.observeExercise(id)
    fun observeAttachments(exerciseId: Long): Flow<List<ExerciseAttachmentEntity>> =
        exerciseDao.observeAttachments(exerciseId)

    suspend fun saveExercise(exercise: ExerciseEntity, reminder: ReminderEntity?): Pair<Long, Boolean> {
        val id = exerciseDao.insertExercise(exercise)
        var reminderId = exercise.reminderId
        var exactOk = true
        if (reminder != null) {
            val res = reminderRepo.saveAndSchedule(
                reminder.copy(id = reminderId ?: 0L, featureType = "EXERCISE", linkedEntityId = id)
            )
            reminderId = res.first
            exactOk = res.second
            exerciseDao.updateExercise(exercise.copy(id = id, reminderId = reminderId))
        } else if (reminderId != null) {
            reminderRepo.deleteReminderById(reminderId)
            exerciseDao.updateExercise(exercise.copy(id = id, reminderId = null))
        }
        return id to exactOk
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        reminderRepo.deleteReminderById(exercise.reminderId)
        exerciseDao.getAllAttachmentsSync().filter { it.exerciseId == exercise.id }.forEach {
            fileStorage.deleteFile(it.filePath)
        }
        exerciseDao.deleteExercise(exercise)
    }

    suspend fun addAttachment(attachment: ExerciseAttachmentEntity): Long =
        exerciseDao.insertAttachment(attachment)

    suspend fun updateAttachment(attachment: ExerciseAttachmentEntity) =
        exerciseDao.updateAttachment(attachment)

    suspend fun deleteAttachment(attachment: ExerciseAttachmentEntity) {
        fileStorage.deleteFile(attachment.filePath)
        exerciseDao.deleteAttachment(attachment)
    }

    suspend fun recordSession(session: ExerciseSessionEntity): Long =
        exerciseDao.insertSession(session)
}

class MedicalRecordsRepository(
    private val medicalRecordsDao: MedicalRecordsDao,
    private val fileStorage: FileStorageManager
) {
    val allFolders: Flow<List<MedicalFolderEntity>> = medicalRecordsDao.observeAllFolders()
    val allDocuments: Flow<List<MedicalDocumentEntity>> = medicalRecordsDao.observeAllDocuments()

    fun observeFolder(id: Long): Flow<MedicalFolderEntity?> = medicalRecordsDao.observeFolder(id)
    fun observeDocumentsForFolder(folderId: Long): Flow<List<MedicalDocumentEntity>> =
        medicalRecordsDao.observeDocumentsForFolder(folderId)

    suspend fun saveFolder(folder: MedicalFolderEntity): Long = medicalRecordsDao.insertFolder(folder)

    suspend fun deleteFolder(folder: MedicalFolderEntity) {
        medicalRecordsDao.getDocumentsForFolderSync(folder.id).forEach {
            fileStorage.deleteFile(it.filePath)
        }
        medicalRecordsDao.deleteFolder(folder)
    }

    suspend fun saveDocument(doc: MedicalDocumentEntity): Long = medicalRecordsDao.insertDocument(doc)

    suspend fun deleteDocument(doc: MedicalDocumentEntity) {
        fileStorage.deleteFile(doc.filePath)
        medicalRecordsDao.deleteDocument(doc)
    }
}

class NutritionRepository(
    private val nutritionDao: NutritionDao,
    private val reminderRepo: ReminderRepository,
    private val fileStorage: FileStorageManager
) {
    val allEntries: Flow<List<NutritionEntryEntity>> = nutritionDao.observeAllEntries()
    val allChartFiles: Flow<List<FoodChartFileEntity>> = nutritionDao.observeAllChartFiles()

    suspend fun saveEntry(entry: NutritionEntryEntity, reminder: ReminderEntity?): Pair<Long, Boolean> {
        val id = nutritionDao.insertEntry(entry)
        var reminderId = entry.reminderId
        var exactOk = true
        if (reminder != null) {
            val res = reminderRepo.saveAndSchedule(
                reminder.copy(id = reminderId ?: 0L, featureType = "NUTRITION", linkedEntityId = id)
            )
            reminderId = res.first
            exactOk = res.second
            nutritionDao.updateEntry(entry.copy(id = id, reminderId = reminderId))
        } else if (reminderId != null) {
            reminderRepo.deleteReminderById(reminderId)
            nutritionDao.updateEntry(entry.copy(id = id, reminderId = null))
        }
        return id to exactOk
    }

    suspend fun toggleCompleted(entry: NutritionEntryEntity) {
        nutritionDao.updateEntry(entry.copy(isCompleted = !entry.isCompleted))
    }

    suspend fun deleteEntry(entry: NutritionEntryEntity) {
        reminderRepo.deleteReminderById(entry.reminderId)
        nutritionDao.deleteEntry(entry)
    }

    suspend fun addChartFile(file: FoodChartFileEntity): Long = nutritionDao.insertChartFile(file)

    suspend fun deleteChartFile(file: FoodChartFileEntity) {
        fileStorage.deleteFile(file.filePath)
        nutritionDao.deleteChartFile(file)
    }
}
