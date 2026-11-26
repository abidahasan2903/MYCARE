package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val strength: String = "",
    val medicineType: String = "Tablet",
    val dosageInstructions: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long? = null,
    val prescribedBy: String = "",
    val notes: String = "",
    val imagePath: String? = null,
    val documentPath: String? = null,
    val documentName: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "medicine_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId")]
)
data class MedicineScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val slotType: String = "MORNING",
    val mealTiming: String = "AFTER_MEAL",
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatType: String = "DAILY",
    val repeatIntervalDays: Int = 1,
    val weekdaysCsv: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long? = null,
    val isEnabled: Boolean = true,
    val reminderId: Long? = null
)

@Serializable
@Entity(
    tableName = "medicine_dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicineId"), Index("scheduledMillis")]
)
data class MedicineDoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val scheduleId: Long? = null,
    val medicineName: String,
    val scheduledMillis: Long,
    val status: String, // TAKEN, SKIPPED, SNOOZED, PENDING
    val actionTimestampMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Strength",
    val description: String = "",
    val instructions: String = "",
    val durationMinutes: Int = 20,
    val sets: Int = 3,
    val repetitions: Int = 12,
    val restSeconds: Int = 45,
    val difficulty: String = "Beginner",
    val startDateMillis: Long = System.currentTimeMillis(),
    val scheduleNotes: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val reminderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "exercise_attachments",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ExerciseAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val title: String,
    val caption: String = "",
    val mediaType: String, // IMAGE, VIDEO, PDF, DOCUMENT
    val filePath: String,
    val mimeType: String = "*/*",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "exercise_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId"), Index("completedAtMillis")]
)
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val exerciseName: String,
    val completedDurationSec: Int,
    val setsCompleted: Int,
    val repetitionsCompleted: Int,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "medical_folders")
data class MedicalFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conditionName: String,
    val description: String = "",
    val diagnosisDateMillis: Long? = null,
    val doctorName: String = "",
    val hospitalName: String = "",
    val treatmentNotes: String = "",
    val importantDates: String = "",
    val observations: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "medical_documents",
    foreignKeys = [
        ForeignKey(
            entity = MedicalFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId"), Index("documentDateMillis")]
)
data class MedicalDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val title: String,
    val description: String = "",
    val mediaType: String, // IMAGE, PDF, DOCUMENT
    val filePath: String,
    val mimeType: String = "*/*",
    val documentDateMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "nutrition_entries")
data class NutritionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodName: String,
    val quantity: Double = 1.0,
    val unit: String = "serving",
    val calories: Int = 0,
    val mealCategory: String = "Breakfast",
    val mealDateMillis: Long = System.currentTimeMillis(),
    val mealHour: Int = 8,
    val mealMinute: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val reminderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "food_chart_files")
data class FoodChartFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val mediaType: String, // IMAGE, PDF, DOCUMENT
    val filePath: String,
    val mimeType: String = "*/*",
    val createdAt: Long = System.currentTimeMillis()
)
