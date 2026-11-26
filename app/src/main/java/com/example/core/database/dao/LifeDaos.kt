package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.AlbumAttachmentEntity
import com.example.core.database.entity.FinanceCategoryEntity
import com.example.core.database.entity.FinanceTransactionEntity
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.GroceryListEntity
import com.example.core.database.entity.MemoryAlbumEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteFolderEntity
import com.example.core.database.entity.TaskCategoryEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.database.entity.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateMillis ASC, timeHour ASC, timeMinute ASC")
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM task_categories ORDER BY isDefault DESC, name ASC")
    fun observeCategories(): Flow<List<TaskCategoryEntity>>

    @Query("SELECT * FROM task_categories")
    suspend fun getCategoriesSync(): List<TaskCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TaskCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: TaskCategoryEntity)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM finance_transactions ORDER BY dateMillis DESC, createdAt DESC")
    fun observeAllTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions")
    suspend fun getAllTransactionsSync(): List<FinanceTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: FinanceTransactionEntity): Long

    @Update
    suspend fun updateTransaction(tx: FinanceTransactionEntity)

    @Delete
    suspend fun deleteTransaction(tx: FinanceTransactionEntity)

    @Query("SELECT * FROM finance_categories ORDER BY isDefault DESC, name ASC")
    fun observeCategories(): Flow<List<FinanceCategoryEntity>>

    @Query("SELECT * FROM finance_categories")
    suspend fun getCategoriesSync(): List<FinanceCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FinanceCategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: FinanceCategoryEntity)
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_lists ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllLists(): Flow<List<GroceryListEntity>>

    @Query("SELECT * FROM grocery_lists WHERE id = :id")
    fun observeList(id: Long): Flow<GroceryListEntity?>

    @Query("SELECT * FROM grocery_lists")
    suspend fun getAllListsSync(): List<GroceryListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: GroceryListEntity): Long

    @Update
    suspend fun updateList(list: GroceryListEntity)

    @Delete
    suspend fun deleteList(list: GroceryListEntity)

    @Query("SELECT * FROM grocery_items WHERE listId = :listId ORDER BY isPurchased ASC, createdAt DESC")
    fun observeItemsForList(listId: Long): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items ORDER BY isPurchased ASC, createdAt DESC")
    fun observeAllItems(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items")
    suspend fun getAllItemsSync(): List<GroceryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GroceryItemEntity): Long

    @Update
    suspend fun updateItem(item: GroceryItemEntity)

    @Delete
    suspend fun deleteItem(item: GroceryItemEntity)

    @Query("UPDATE grocery_items SET isPurchased = 0 WHERE listId = :listId")
    suspend fun resetPurchasedForList(listId: Long)

    @Query("DELETE FROM grocery_items WHERE listId = :listId")
    suspend fun clearItemsForList(listId: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM note_folders ORDER BY name ASC")
    fun observeNoteFolders(): Flow<List<NoteFolderEntity>>

    @Query("SELECT * FROM note_folders")
    suspend fun getNoteFoldersSync(): List<NoteFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteFolder(folder: NoteFolderEntity): Long

    @Delete
    suspend fun deleteNoteFolder(folder: NoteFolderEntity)

    @Query("SELECT * FROM notes ORDER BY isFavorite DESC, updatedAt DESC")
    fun observeAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT * FROM memory_albums ORDER BY createdAt DESC")
    fun observeAllAlbums(): Flow<List<MemoryAlbumEntity>>

    @Query("SELECT * FROM memory_albums WHERE id = :id")
    fun observeAlbum(id: Long): Flow<MemoryAlbumEntity?>

    @Query("SELECT * FROM memory_albums")
    suspend fun getAllAlbumsSync(): List<MemoryAlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: MemoryAlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: MemoryAlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: MemoryAlbumEntity)

    @Query("SELECT * FROM album_attachments WHERE albumId = :albumId ORDER BY createdAt DESC")
    fun observeAlbumAttachments(albumId: Long): Flow<List<AlbumAttachmentEntity>>

    @Query("SELECT * FROM album_attachments WHERE albumId = :albumId")
    suspend fun getAlbumAttachmentsSync(albumId: Long): List<AlbumAttachmentEntity>

    @Query("SELECT * FROM album_attachments")
    suspend fun getAllAlbumAttachmentsSync(): List<AlbumAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumAttachment(attachment: AlbumAttachmentEntity): Long

    @Update
    suspend fun updateAlbumAttachment(attachment: AlbumAttachmentEntity)

    @Delete
    suspend fun deleteAlbumAttachment(attachment: AlbumAttachmentEntity)

    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun observeAllVoiceNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes")
    suspend fun getAllVoiceNotesSync(): List<VoiceNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(voiceNote: VoiceNoteEntity): Long

    @Update
    suspend fun updateVoiceNote(voiceNote: VoiceNoteEntity)

    @Delete
    suspend fun deleteVoiceNote(voiceNote: VoiceNoteEntity)
}
