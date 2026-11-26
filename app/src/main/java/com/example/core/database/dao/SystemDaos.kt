package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.EmergencyContactEntity
import com.example.core.database.entity.EmergencyProfileEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TravelAttachmentEntity
import com.example.core.database.entity.TravelChecklistItemEntity
import com.example.core.database.entity.TravelExpenseEntity
import com.example.core.database.entity.TravelPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelDao {
    @Query("SELECT * FROM travel_plans ORDER BY startDateMillis ASC")
    fun observeAllPlans(): Flow<List<TravelPlanEntity>>

    @Query("SELECT * FROM travel_plans WHERE id = :id")
    fun observePlan(id: Long): Flow<TravelPlanEntity?>

    @Query("SELECT * FROM travel_plans")
    suspend fun getAllPlansSync(): List<TravelPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TravelPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: TravelPlanEntity)

    @Delete
    suspend fun deletePlan(plan: TravelPlanEntity)

    @Query("SELECT * FROM travel_expenses WHERE travelId = :travelId ORDER BY dateMillis DESC")
    fun observeExpenses(travelId: Long): Flow<List<TravelExpenseEntity>>

    @Query("SELECT * FROM travel_expenses")
    suspend fun getAllExpensesSync(): List<TravelExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: TravelExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: TravelExpenseEntity)

    @Query("SELECT * FROM travel_checklist_items WHERE travelId = :travelId ORDER BY isChecked ASC, id ASC")
    fun observeChecklist(travelId: Long): Flow<List<TravelChecklistItemEntity>>

    @Query("SELECT * FROM travel_checklist_items")
    suspend fun getAllChecklistSync(): List<TravelChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: TravelChecklistItemEntity): Long

    @Update
    suspend fun updateChecklistItem(item: TravelChecklistItemEntity)

    @Delete
    suspend fun deleteChecklistItem(item: TravelChecklistItemEntity)

    @Query("SELECT * FROM travel_attachments WHERE travelId = :travelId ORDER BY createdAt DESC")
    fun observeAttachments(travelId: Long): Flow<List<TravelAttachmentEntity>>

    @Query("SELECT * FROM travel_attachments")
    suspend fun getAllAttachmentsSync(): List<TravelAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: TravelAttachmentEntity): Long

    @Delete
    suspend fun deleteAttachment(attachment: TravelAttachmentEntity)
}

@Dao
interface EmergencyDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun observeAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts")
    suspend fun getAllContactsSync(): List<EmergencyContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)

    @Delete
    suspend fun deleteContact(contact: EmergencyContactEntity)

    @Query("SELECT * FROM emergency_profile WHERE id = 1")
    fun observeProfile(): Flow<EmergencyProfileEntity?>

    @Query("SELECT * FROM emergency_profile WHERE id = 1")
    suspend fun getProfileSync(): EmergencyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: EmergencyProfileEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerTimeMillis ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1")
    suspend fun getEnabledRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE featureType = :featureType AND linkedEntityId = :linkedId LIMIT 1")
    suspend fun getReminderForEntity(featureType: String, linkedId: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}
