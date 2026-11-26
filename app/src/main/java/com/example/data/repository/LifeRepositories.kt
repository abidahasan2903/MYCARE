package com.example.data.repository

import com.example.core.database.dao.EmergencyDao
import com.example.core.database.dao.GroceryDao
import com.example.core.database.dao.MemoryDao
import com.example.core.database.dao.TaskDao
import com.example.core.database.dao.TravelDao
import com.example.core.database.dao.WalletDao
import com.example.core.database.entity.AlbumAttachmentEntity
import com.example.core.database.entity.EmergencyContactEntity
import com.example.core.database.entity.EmergencyProfileEntity
import com.example.core.database.entity.FinanceCategoryEntity
import com.example.core.database.entity.FinanceTransactionEntity
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.GroceryListEntity
import com.example.core.database.entity.MemoryAlbumEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteFolderEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TaskCategoryEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.database.entity.TravelAttachmentEntity
import com.example.core.database.entity.TravelChecklistItemEntity
import com.example.core.database.entity.TravelExpenseEntity
import com.example.core.database.entity.TravelPlanEntity
import com.example.core.database.entity.VoiceNoteEntity
import com.example.core.storage.FileStorageManager
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderRepo: ReminderRepository,
    private val fileStorage: FileStorageManager
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()
    val categories: Flow<List<TaskCategoryEntity>> = taskDao.observeCategories()

    suspend fun saveTask(task: TaskEntity, reminder: ReminderEntity?): Pair<Long, Boolean> {
        val id = taskDao.insertTask(task)
        var reminderId = task.reminderId
        var exactOk = true
        if (reminder != null) {
            val res = reminderRepo.saveAndSchedule(
                reminder.copy(id = reminderId ?: 0L, featureType = "TASK", linkedEntityId = id)
            )
            reminderId = res.first
            exactOk = res.second
            taskDao.updateTask(task.copy(id = id, reminderId = reminderId))
        } else if (reminderId != null) {
            reminderRepo.deleteReminderById(reminderId)
            taskDao.updateTask(task.copy(id = id, reminderId = null))
        }
        return id to exactOk
    }

    suspend fun toggleCompleted(task: TaskEntity) {
        val nowCompleted = !task.isCompleted
        taskDao.updateTask(
            task.copy(
                isCompleted = nowCompleted,
                completedAtMillis = if (nowCompleted) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        reminderRepo.deleteReminderById(task.reminderId)
        fileStorage.deleteFile(task.imagePath)
        fileStorage.deleteFile(task.documentPath)
        taskDao.deleteTask(task)
    }

    suspend fun addCategory(name: String) {
        if (name.isNotBlank()) {
            taskDao.insertCategory(TaskCategoryEntity(name = name.trim(), isDefault = false))
        }
    }
}

class WalletRepository(private val walletDao: WalletDao) {
    val allTransactions: Flow<List<FinanceTransactionEntity>> = walletDao.observeAllTransactions()
    val categories: Flow<List<FinanceCategoryEntity>> = walletDao.observeCategories()

    suspend fun saveTransaction(tx: FinanceTransactionEntity): Long = walletDao.insertTransaction(tx)
    suspend fun deleteTransaction(tx: FinanceTransactionEntity) = walletDao.deleteTransaction(tx)

    suspend fun addCategory(name: String, type: String) {
        if (name.isNotBlank()) {
            walletDao.insertCategory(FinanceCategoryEntity(name = name.trim(), type = type, isDefault = false))
        }
    }
}

class GroceryRepository(
    private val groceryDao: GroceryDao,
    private val fileStorage: FileStorageManager
) {
    val allLists: Flow<List<GroceryListEntity>> = groceryDao.observeAllLists()
    val allItems: Flow<List<GroceryItemEntity>> = groceryDao.observeAllItems()

    fun observeList(id: Long): Flow<GroceryListEntity?> = groceryDao.observeList(id)
    fun observeItemsForList(listId: Long): Flow<List<GroceryItemEntity>> = groceryDao.observeItemsForList(listId)

    suspend fun saveList(list: GroceryListEntity): Long = groceryDao.insertList(list)

    suspend fun deleteList(list: GroceryListEntity) {
        fileStorage.deleteFile(list.importedFilePath)
        groceryDao.deleteList(list)
    }

    suspend fun saveItem(item: GroceryItemEntity): Long = groceryDao.insertItem(item)
    suspend fun deleteItem(item: GroceryItemEntity) = groceryDao.deleteItem(item)
    suspend fun resetPurchased(listId: Long) = groceryDao.resetPurchasedForList(listId)
    suspend fun clearList(listId: Long) = groceryDao.clearItemsForList(listId)
}

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val fileStorage: FileStorageManager
) {
    val noteFolders: Flow<List<NoteFolderEntity>> = memoryDao.observeNoteFolders()
    val allNotes: Flow<List<NoteEntity>> = memoryDao.observeAllNotes()
    val allAlbums: Flow<List<MemoryAlbumEntity>> = memoryDao.observeAllAlbums()
    val allVoiceNotes: Flow<List<VoiceNoteEntity>> = memoryDao.observeAllVoiceNotes()

    fun observeAlbum(id: Long): Flow<MemoryAlbumEntity?> = memoryDao.observeAlbum(id)
    fun observeAlbumAttachments(albumId: Long): Flow<List<AlbumAttachmentEntity>> =
        memoryDao.observeAlbumAttachments(albumId)

    suspend fun saveNoteFolder(name: String): Long =
        memoryDao.insertNoteFolder(NoteFolderEntity(name = name.trim()))

    suspend fun deleteNoteFolder(folder: NoteFolderEntity) = memoryDao.deleteNoteFolder(folder)

    suspend fun saveNote(note: NoteEntity): Long =
        memoryDao.insertNote(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNote(note: NoteEntity) = memoryDao.deleteNote(note)

    suspend fun saveAlbum(album: MemoryAlbumEntity): Long = memoryDao.insertAlbum(album)

    suspend fun deleteAlbum(album: MemoryAlbumEntity) {
        memoryDao.getAlbumAttachmentsSync(album.id).forEach {
            fileStorage.deleteFile(it.filePath)
        }
        memoryDao.deleteAlbum(album)
    }

    suspend fun addAlbumAttachment(attachment: AlbumAttachmentEntity): Long =
        memoryDao.insertAlbumAttachment(attachment)

    suspend fun updateAlbumAttachment(attachment: AlbumAttachmentEntity) =
        memoryDao.updateAlbumAttachment(attachment)

    suspend fun deleteAlbumAttachment(attachment: AlbumAttachmentEntity) {
        fileStorage.deleteFile(attachment.filePath)
        memoryDao.deleteAlbumAttachment(attachment)
    }

    suspend fun saveVoiceNote(voiceNote: VoiceNoteEntity): Long = memoryDao.insertVoiceNote(voiceNote)

    suspend fun deleteVoiceNote(voiceNote: VoiceNoteEntity) {
        fileStorage.deleteFile(voiceNote.filePath)
        memoryDao.deleteVoiceNote(voiceNote)
    }
}

class TravelRepository(
    private val travelDao: TravelDao,
    private val fileStorage: FileStorageManager
) {
    val allPlans: Flow<List<TravelPlanEntity>> = travelDao.observeAllPlans()
    fun observePlan(id: Long): Flow<TravelPlanEntity?> = travelDao.observePlan(id)
    fun observeExpenses(travelId: Long): Flow<List<TravelExpenseEntity>> = travelDao.observeExpenses(travelId)
    fun observeChecklist(travelId: Long): Flow<List<TravelChecklistItemEntity>> = travelDao.observeChecklist(travelId)
    fun observeAttachments(travelId: Long): Flow<List<TravelAttachmentEntity>> = travelDao.observeAttachments(travelId)

    suspend fun savePlan(plan: TravelPlanEntity): Long = travelDao.insertPlan(plan)

    suspend fun deletePlan(plan: TravelPlanEntity) {
        travelDao.getAllAttachmentsSync().filter { it.travelId == plan.id }.forEach {
            fileStorage.deleteFile(it.filePath)
        }
        travelDao.deletePlan(plan)
    }

    suspend fun addExpense(expense: TravelExpenseEntity): Long = travelDao.insertExpense(expense)
    suspend fun deleteExpense(expense: TravelExpenseEntity) = travelDao.deleteExpense(expense)
    suspend fun saveChecklistItem(item: TravelChecklistItemEntity): Long = travelDao.insertChecklistItem(item)
    suspend fun deleteChecklistItem(item: TravelChecklistItemEntity) = travelDao.deleteChecklistItem(item)
    suspend fun addAttachment(attachment: TravelAttachmentEntity): Long = travelDao.insertAttachment(attachment)
    suspend fun deleteAttachment(attachment: TravelAttachmentEntity) {
        fileStorage.deleteFile(attachment.filePath)
        travelDao.deleteAttachment(attachment)
    }
}

class EmergencyRepository(private val emergencyDao: EmergencyDao) {
    val allContacts: Flow<List<EmergencyContactEntity>> = emergencyDao.observeAllContacts()
    val profile: Flow<EmergencyProfileEntity?> = emergencyDao.observeProfile()

    suspend fun saveContact(contact: EmergencyContactEntity): Long = emergencyDao.insertContact(contact)
    suspend fun deleteContact(contact: EmergencyContactEntity) = emergencyDao.deleteContact(contact)
    suspend fun saveProfile(profile: EmergencyProfileEntity) =
        emergencyDao.upsertProfile(profile.copy(id = 1, updatedAt = System.currentTimeMillis()))
}
