package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import com.example.core.database.entity.SleepRecordEntity
import com.example.core.database.entity.WaterIntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines")
    suspend fun getAllMedicinesSync(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun observeMedicine(id: Long): Flow<MedicineEntity?>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("SELECT * FROM medicine_schedules WHERE medicineId = :medicineId ORDER BY hour ASC, minute ASC")
    fun observeSchedulesForMedicine(medicineId: Long): Flow<List<MedicineScheduleEntity>>

    @Query("SELECT * FROM medicine_schedules WHERE isEnabled = 1 ORDER BY hour ASC, minute ASC")
    fun observeActiveSchedules(): Flow<List<MedicineScheduleEntity>>

    @Query("SELECT * FROM medicine_schedules")
    suspend fun getAllSchedulesSync(): List<MedicineScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MedicineScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: MedicineScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: MedicineScheduleEntity)

    @Query("SELECT * FROM medicine_dose_logs ORDER BY actionTimestampMillis DESC")
    fun observeAllDoseLogs(): Flow<List<MedicineDoseLogEntity>>

    @Query("SELECT * FROM medicine_dose_logs WHERE scheduledMillis >= :sinceMillis ORDER BY actionTimestampMillis DESC")
    fun observeDoseLogsSince(sinceMillis: Long): Flow<List<MedicineDoseLogEntity>>

    @Query("SELECT * FROM medicine_dose_logs")
    suspend fun getAllDoseLogsSync(): List<MedicineDoseLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(log: MedicineDoseLogEntity): Long
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeExercise(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesSync(): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercise_attachments WHERE exerciseId = :exerciseId ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAttachments(exerciseId: Long): Flow<List<ExerciseAttachmentEntity>>

    @Query("SELECT * FROM exercise_attachments")
    suspend fun getAllAttachmentsSync(): List<ExerciseAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: ExerciseAttachmentEntity): Long

    @Update
    suspend fun updateAttachment(attachment: ExerciseAttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: ExerciseAttachmentEntity)

    @Query("SELECT * FROM exercise_sessions ORDER BY completedAtMillis DESC")
    fun observeAllSessions(): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions")
    suspend fun getAllSessionsSync(): List<ExerciseSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExerciseSessionEntity): Long
}

@Dao
interface MedicalRecordsDao {
    @Query("SELECT * FROM medical_folders ORDER BY createdAt DESC")
    fun observeAllFolders(): Flow<List<MedicalFolderEntity>>

    @Query("SELECT * FROM medical_folders WHERE id = :id")
    fun observeFolder(id: Long): Flow<MedicalFolderEntity?>

    @Query("SELECT * FROM medical_folders")
    suspend fun getAllFoldersSync(): List<MedicalFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: MedicalFolderEntity): Long

    @Update
    suspend fun updateFolder(folder: MedicalFolderEntity)

    @Delete
    suspend fun deleteFolder(folder: MedicalFolderEntity)

    @Query("SELECT * FROM medical_documents WHERE folderId = :folderId ORDER BY documentDateMillis DESC, createdAt DESC")
    fun observeDocumentsForFolder(folderId: Long): Flow<List<MedicalDocumentEntity>>

    @Query("SELECT * FROM medical_documents ORDER BY createdAt DESC")
    fun observeAllDocuments(): Flow<List<MedicalDocumentEntity>>

    @Query("SELECT * FROM medical_documents WHERE folderId = :folderId")
    suspend fun getDocumentsForFolderSync(folderId: Long): List<MedicalDocumentEntity>

    @Query("SELECT * FROM medical_documents")
    suspend fun getAllDocumentsSync(): List<MedicalDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: MedicalDocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: MedicalDocumentEntity)

    @Delete
    suspend fun deleteDocument(doc: MedicalDocumentEntity)
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_entries ORDER BY mealDateMillis DESC, mealHour ASC, mealMinute ASC")
    fun observeAllEntries(): Flow<List<NutritionEntryEntity>>

    @Query("SELECT * FROM nutrition_entries")
    suspend fun getAllEntriesSync(): List<NutritionEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: NutritionEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: NutritionEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: NutritionEntryEntity)

    @Query("SELECT * FROM food_chart_files ORDER BY createdAt DESC")
    fun observeAllChartFiles(): Flow<List<FoodChartFileEntity>>

    @Query("SELECT * FROM food_chart_files")
    suspend fun getAllChartFilesSync(): List<FoodChartFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChartFile(file: FoodChartFileEntity): Long

    @Delete
    suspend fun deleteChartFile(file: FoodChartFileEntity)
}

@Dao
interface WellnessDao {
    @Query("SELECT * FROM water_intake_records ORDER BY recordedAtMillis DESC")
    fun observeWaterIntakes(): Flow<List<WaterIntakeEntity>>

    @Query("SELECT * FROM water_intake_records ORDER BY recordedAtMillis DESC")
    suspend fun getAllWaterIntakesSync(): List<WaterIntakeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterIntake(entry: WaterIntakeEntity): Long

    @Delete
    suspend fun deleteWaterIntake(entry: WaterIntakeEntity)

    @Query("SELECT * FROM sleep_records ORDER BY bedtimeMillis DESC")
    fun observeSleepRecords(): Flow<List<SleepRecordEntity>>

    @Query("SELECT * FROM sleep_records ORDER BY bedtimeMillis DESC")
    suspend fun getAllSleepRecordsSync(): List<SleepRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecordEntity): Long

    @Delete
    suspend fun deleteSleepRecord(record: SleepRecordEntity)
}
