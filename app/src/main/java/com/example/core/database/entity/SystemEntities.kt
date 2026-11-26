package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "travel_plans")
data class TravelPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripName: String,
    val destination: String,
    val origin: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = System.currentTimeMillis(),
    val hotelInfo: String = "",
    val contactNumbers: String = "",
    val budget: Double = 0.0,
    val placesToVisit: String = "",
    val notes: String = "",
    val statusOverride: String? = null, // null = auto by date, or UPCOMING / ONGOING / COMPLETED
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "travel_expenses",
    foreignKeys = [
        ForeignKey(
            entity = TravelPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["travelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("travelId")]
)
data class TravelExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val travelId: Long,
    val title: String,
    val amount: Double,
    val dateMillis: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "travel_checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = TravelPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["travelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("travelId")]
)
data class TravelChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val travelId: Long,
    val itemTitle: String,
    val isChecked: Boolean = false
)

@Serializable
@Entity(
    tableName = "travel_attachments",
    foreignKeys = [
        ForeignKey(
            entity = TravelPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["travelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("travelId")]
)
data class TravelAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val travelId: Long,
    val title: String,
    val mediaType: String, // IMAGE, PDF, DOCUMENT
    val filePath: String,
    val mimeType: String = "*/*",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String = "",
    val phoneNumber: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "emergency_profile")
data class EmergencyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val bloodGroup: String = "",
    val allergies: String = "",
    val medicalNotes: String = "",
    val emergencyInstructions: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "reminders", indices = [Index("isEnabled"), Index("triggerTimeMillis")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val featureType: String = "GENERAL", // MEDICINE, EXERCISE, TASK, GROCERY, APPOINTMENT, NUTRITION, TRAVEL, BILL, GENERAL
    val linkedEntityId: Long? = null,
    val triggerTimeMillis: Long,
    val repeatType: String = "ONCE", // ONCE, DAILY, WEEKLY, CUSTOM
    val repeatIntervalDays: Int = 1,
    val weekdaysCsv: String = "", // e.g. "1,3,5" using Calendar.MONDAY..SUNDAY
    val isEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val ringtoneUri: String? = null, // null -> system notification sound or default
    val ringingDurationSec: Int = 30,
    val snoozeMinutes: Int = 5,
    val lastTriggeredMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
