package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.EmergencyDao
import com.example.core.database.dao.ExerciseDao
import com.example.core.database.dao.GroceryDao
import com.example.core.database.dao.MedicalRecordsDao
import com.example.core.database.dao.MedicineDao
import com.example.core.database.dao.MemoryDao
import com.example.core.database.dao.NutritionDao
import com.example.core.database.dao.ReminderDao
import com.example.core.database.dao.TaskDao
import com.example.core.database.dao.TravelDao
import com.example.core.database.dao.WalletDao
import com.example.core.database.entity.AlbumAttachmentEntity
import com.example.core.database.entity.EmergencyContactEntity
import com.example.core.database.entity.EmergencyProfileEntity
import com.example.core.database.entity.ExerciseAttachmentEntity
import com.example.core.database.entity.ExerciseEntity
import com.example.core.database.entity.ExerciseSessionEntity
import com.example.core.database.entity.FinanceCategoryEntity
import com.example.core.database.entity.FinanceTransactionEntity
import com.example.core.database.entity.FoodChartFileEntity
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.GroceryListEntity
import com.example.core.database.entity.MedicalDocumentEntity
import com.example.core.database.entity.MedicalFolderEntity
import com.example.core.database.entity.MedicineDoseLogEntity
import com.example.core.database.entity.MedicineEntity
import com.example.core.database.entity.MedicineScheduleEntity
import com.example.core.database.entity.MemoryAlbumEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteFolderEntity
import com.example.core.database.entity.NutritionEntryEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TaskCategoryEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.database.entity.TravelAttachmentEntity
import com.example.core.database.entity.TravelChecklistItemEntity
import com.example.core.database.entity.TravelExpenseEntity
import com.example.core.database.entity.TravelPlanEntity
import com.example.core.database.entity.VoiceNoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MedicineEntity::class,
        MedicineScheduleEntity::class,
        MedicineDoseLogEntity::class,
        ExerciseEntity::class,
        ExerciseAttachmentEntity::class,
        ExerciseSessionEntity::class,
        TaskCategoryEntity::class,
        TaskEntity::class,
        FinanceCategoryEntity::class,
        FinanceTransactionEntity::class,
        GroceryListEntity::class,
        GroceryItemEntity::class,
        NoteFolderEntity::class,
        NoteEntity::class,
        MemoryAlbumEntity::class,
        AlbumAttachmentEntity::class,
        VoiceNoteEntity::class,
        MedicalFolderEntity::class,
        MedicalDocumentEntity::class,
        NutritionEntryEntity::class,
        FoodChartFileEntity::class,
        TravelPlanEntity::class,
        TravelExpenseEntity::class,
        TravelChecklistItemEntity::class,
        TravelAttachmentEntity::class,
        EmergencyContactEntity::class,
        EmergencyProfileEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MyCareDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun medicalRecordsDao(): MedicalRecordsDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun taskDao(): TaskDao
    abstract fun walletDao(): WalletDao
    abstract fun groceryDao(): GroceryDao
    abstract fun memoryDao(): MemoryDao
    abstract fun travelDao(): TravelDao
    abstract fun emergencyDao(): EmergencyDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DB_NAME = "mycare_database.db"

        @Volatile
        private var INSTANCE: MyCareDatabase? = null

        fun getInstance(context: Context): MyCareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): MyCareDatabase {
            return Room.databaseBuilder(
                appContext,
                MyCareDatabase::class.java,
                DB_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedDefaultCategories(getInstance(appContext))
                        }
                    }
                })
                .build()
        }

        private suspend fun seedDefaultCategories(db: MyCareDatabase) {
            val taskCategories = listOf("Personal", "Study", "Work", "Health", "Shopping", "Other")
            taskCategories.forEach { name ->
                db.taskDao().insertCategory(TaskCategoryEntity(name = name, isDefault = true))
            }
            val expenseCategories = listOf(
                "Food", "Transport", "Education", "Shopping",
                "Medical", "Bills", "Entertainment", "Other"
            )
            expenseCategories.forEach { name ->
                db.walletDao().insertCategory(
                    FinanceCategoryEntity(name = name, type = "EXPENSE", isDefault = true)
                )
            }
            val incomeCategories = listOf("Salary", "Business", "Freelance", "Allowance", "Gift", "Other")
            incomeCategories.forEach { name ->
                db.walletDao().insertCategory(
                    FinanceCategoryEntity(name = name, type = "INCOME", isDefault = true)
                )
            }
        }
    }
}
