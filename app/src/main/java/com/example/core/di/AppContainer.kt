package com.example.core.di

import android.content.Context
import com.example.core.database.MyCareDatabase
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.media.AudioPlaybackManager
import com.example.core.media.AudioRecorderManager
import com.example.core.reminders.ReminderScheduler
import com.example.core.storage.BackupManager
import com.example.core.storage.FileStorageManager
import com.example.data.repository.EmergencyRepository
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.GroceryRepository
import com.example.data.repository.MedicalRecordsRepository
import com.example.data.repository.MedicineRepository
import com.example.data.repository.MemoryRepository
import com.example.data.repository.NutritionRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.TravelRepository
import com.example.data.repository.WalletRepository
import com.example.data.repository.WellnessRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database = MyCareDatabase.getInstance(appContext)
    val settingsStore = AppSettingsDataStore(appContext)
    val fileStorage = FileStorageManager(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val audioRecorder = AudioRecorderManager(appContext)
    val audioPlayer = AudioPlaybackManager(appContext)
    val backupManager = BackupManager(appContext, database, settingsStore)

    val reminderRepository = ReminderRepository(database.reminderDao(), reminderScheduler)
    val medicineRepository = MedicineRepository(database.medicineDao(), reminderRepository, fileStorage)
    val exerciseRepository = ExerciseRepository(database.exerciseDao(), reminderRepository, fileStorage)
    val medicalRecordsRepository = MedicalRecordsRepository(database.medicalRecordsDao(), fileStorage)
    val nutritionRepository = NutritionRepository(database.nutritionDao(), reminderRepository, fileStorage)
    val taskRepository = TaskRepository(database.taskDao(), reminderRepository, fileStorage)
    val walletRepository = WalletRepository(database.walletDao())
    val groceryRepository = GroceryRepository(database.groceryDao(), fileStorage)
    val memoryRepository = MemoryRepository(database.memoryDao(), fileStorage)
    val travelRepository = TravelRepository(database.travelDao(), fileStorage)
    val emergencyRepository = EmergencyRepository(database.emergencyDao())
    val wellnessRepository = WellnessRepository(database.wellnessDao())
}
