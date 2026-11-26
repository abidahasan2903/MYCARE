package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "task_categories")
data class TaskCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false
)

@Serializable
@Entity(tableName = "tasks", indices = [Index("dueDateMillis"), Index("isCompleted")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long = System.currentTimeMillis(),
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val deadlineMillis: Long? = null,
    val categoryName: String = "Personal",
    val repeatType: String = "ONCE",
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val imagePath: String? = null,
    val documentPath: String? = null,
    val documentName: String? = null,
    val reminderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "finance_categories")
data class FinanceCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // INCOME or EXPENSE
    val isDefault: Boolean = false
)

@Serializable
@Entity(tableName = "finance_transactions", indices = [Index("type"), Index("dateMillis")])
data class FinanceTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // INCOME or EXPENSE
    val title: String,
    val amount: Double,
    val category: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Cash",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "grocery_lists")
data class GroceryListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val importedFilePath: String? = null,
    val importedFileType: String? = null,
    val importedFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "grocery_items",
    foreignKeys = [
        ForeignKey(
            entity = GroceryListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("isPurchased")]
)
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pcs",
    val estimatedPrice: Double = 0.0,
    val actualPrice: Double? = null,
    val notes: String = "",
    val isPurchased: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "note_folders")
data class NoteFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "notes", indices = [Index("folderId"), Index("isFavorite")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val title: String,
    val content: String,
    val category: String = "Personal",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "memory_albums")
data class MemoryAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val location: String = "",
    val coverPhotoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "album_attachments",
    foreignKeys = [
        ForeignKey(
            entity = MemoryAlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("albumId")]
)
data class AlbumAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long,
    val title: String,
    val mediaType: String, // IMAGE, VIDEO, PDF, DOCUMENT
    val filePath: String,
    val mimeType: String = "*/*",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val filePath: String,
    val durationMillis: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
