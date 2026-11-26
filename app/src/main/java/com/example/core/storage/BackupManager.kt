package com.example.core.storage

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.core.database.MyCareDatabase
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
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.datastore.AppSettingsState
import com.example.core.reminders.ReminderScheduler
import com.example.core.theme.AccentTheme
import com.example.core.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupSettingsPayload(
    val languageCode: String,
    val themeMode: String,
    val accentTheme: String,
    val fontScale: Float,
    val notificationsEnabled: Boolean = true,
    val defaultRingtoneUri: String? = null,
    val defaultRingtoneName: String? = null,
    val defaultRingingDurationSec: Int,
    val defaultSnoozeMinutes: Int,
    val defaultVibration: Boolean,
    val notifMedicine: Boolean,
    val notifExercise: Boolean,
    val notifTasks: Boolean,
    val notifGrocery: Boolean,
    val notifAppointments: Boolean,
    val notifNutrition: Boolean,
    val notifTravel: Boolean,
    val notifGeneral: Boolean
)

@Serializable
data class MyCareBackupPayload(
    val format: String = "MYCARE_BACKUP_V1",
    val version: Int = 1,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val settings: BackupSettingsPayload,
    val medicines: List<MedicineEntity> = emptyList(),
    val medicineSchedules: List<MedicineScheduleEntity> = emptyList(),
    val medicineDoseLogs: List<MedicineDoseLogEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val exerciseAttachments: List<ExerciseAttachmentEntity> = emptyList(),
    val exerciseSessions: List<ExerciseSessionEntity> = emptyList(),
    val taskCategories: List<TaskCategoryEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val financeCategories: List<FinanceCategoryEntity> = emptyList(),
    val financeTransactions: List<FinanceTransactionEntity> = emptyList(),
    val groceryLists: List<GroceryListEntity> = emptyList(),
    val groceryItems: List<GroceryItemEntity> = emptyList(),
    val noteFolders: List<NoteFolderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val memoryAlbums: List<MemoryAlbumEntity> = emptyList(),
    val albumAttachments: List<AlbumAttachmentEntity> = emptyList(),
    val voiceNotes: List<VoiceNoteEntity> = emptyList(),
    val medicalFolders: List<MedicalFolderEntity> = emptyList(),
    val medicalDocuments: List<MedicalDocumentEntity> = emptyList(),
    val nutritionEntries: List<NutritionEntryEntity> = emptyList(),
    val foodChartFiles: List<FoodChartFileEntity> = emptyList(),
    val travelPlans: List<TravelPlanEntity> = emptyList(),
    val travelExpenses: List<TravelExpenseEntity> = emptyList(),
    val travelChecklistItems: List<TravelChecklistItemEntity> = emptyList(),
    val travelAttachments: List<TravelAttachmentEntity> = emptyList(),
    val emergencyContacts: List<EmergencyContactEntity> = emptyList(),
    val emergencyProfile: EmergencyProfileEntity? = null,
    val reminders: List<ReminderEntity> = emptyList()
)

class BackupManager(
    private val context: Context,
    private val db: MyCareDatabase,
    private val settingsStore: AppSettingsDataStore
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun exportBackupToUri(destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = settingsStore.settingsFlow.first()
            val payload = MyCareBackupPayload(
                settings = BackupSettingsPayload(
                    languageCode = s.languageCode,
                    themeMode = s.themeMode.key,
                    accentTheme = s.accentTheme.key,
                    fontScale = s.fontScale,
                    notificationsEnabled = s.notificationsEnabled,
                    defaultRingtoneUri = s.defaultRingtoneUri,
                    defaultRingtoneName = s.defaultRingtoneName,
                    defaultRingingDurationSec = s.defaultRingingDurationSec,
                    defaultSnoozeMinutes = s.defaultSnoozeMinutes,
                    defaultVibration = s.defaultVibration,
                    notifMedicine = s.notifMedicine,
                    notifExercise = s.notifExercise,
                    notifTasks = s.notifTasks,
                    notifGrocery = s.notifGrocery,
                    notifAppointments = s.notifAppointments,
                    notifNutrition = s.notifNutrition,
                    notifTravel = s.notifTravel,
                    notifGeneral = s.notifGeneral
                ),
                medicines = db.medicineDao().getAllMedicinesSync(),
                medicineSchedules = db.medicineDao().getAllSchedulesSync(),
                medicineDoseLogs = db.medicineDao().getAllDoseLogsSync(),
                exercises = db.exerciseDao().getAllExercisesSync(),
                exerciseAttachments = db.exerciseDao().getAllAttachmentsSync(),
                exerciseSessions = db.exerciseDao().getAllSessionsSync(),
                taskCategories = db.taskDao().getCategoriesSync(),
                tasks = db.taskDao().getAllTasksSync(),
                financeCategories = db.walletDao().getCategoriesSync(),
                financeTransactions = db.walletDao().getAllTransactionsSync(),
                groceryLists = db.groceryDao().getAllListsSync(),
                groceryItems = db.groceryDao().getAllItemsSync(),
                noteFolders = db.memoryDao().getNoteFoldersSync(),
                notes = db.memoryDao().getAllNotesSync(),
                memoryAlbums = db.memoryDao().getAllAlbumsSync(),
                albumAttachments = db.memoryDao().getAllAlbumAttachmentsSync(),
                voiceNotes = db.memoryDao().getAllVoiceNotesSync(),
                medicalFolders = db.medicalRecordsDao().getAllFoldersSync(),
                medicalDocuments = db.medicalRecordsDao().getAllDocumentsSync(),
                nutritionEntries = db.nutritionDao().getAllEntriesSync(),
                foodChartFiles = db.nutritionDao().getAllChartFilesSync(),
                travelPlans = db.travelDao().getAllPlansSync(),
                travelExpenses = db.travelDao().getAllExpensesSync(),
                travelChecklistItems = db.travelDao().getAllChecklistSync(),
                travelAttachments = db.travelDao().getAllAttachmentsSync(),
                emergencyContacts = db.emergencyDao().getAllContactsSync(),
                emergencyProfile = db.emergencyDao().getProfileSync(),
                reminders = db.reminderDao().getAllRemindersSync()
            )

            val payloadJson = json.encodeToString(payload)
            val filesDir = context.filesDir

            context.contentResolver.openOutputStream(destUri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry("backup.json"))
                    zip.write(payloadJson.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    listOf("media", "voice_notes").forEach { folderName ->
                        val root = File(filesDir, folderName)
                        if (root.exists()) {
                            root.walkTopDown().filter { it.isFile }.forEach { file ->
                                val relPath = file.relativeTo(filesDir).path.replace('\\', '/')
                                zip.putNextEntry(ZipEntry("files/$relPath"))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            } ?: error("Unable to open output stream for backup")
        }
    }

    suspend fun validateAndRestoreBackup(sourceUri: Uri): Result<MyCareBackupPayload> = withContext(Dispatchers.IO) {
        runCatching {
            var jsonText: String? = null
            val tempRestoreDir = File(context.cacheDir, "restore_tmp").apply {
                deleteRecursively()
                mkdirs()
            }

            try {
                val tempRootPrefix = tempRestoreDir.canonicalPath + File.separator
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                if (entry.name == "backup.json") {
                                    jsonText = zip.readBytes().toString(Charsets.UTF_8)
                                } else if (entry.name.startsWith("files/")) {
                                    val rel = entry.name.removePrefix("files/")
                                    val target = File(tempRestoreDir, rel).canonicalFile
                                    if (target.path.startsWith(tempRootPrefix)) {
                                        target.parentFile?.mkdirs()
                                        target.outputStream().use { fos -> zip.copyTo(fos) }
                                    }
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                } ?: error("Unable to read backup file")

                val rawJson = jsonText ?: error("Missing backup.json inside archive")
                val payload = json.decodeFromString<MyCareBackupPayload>(rawJson)
                require(payload.format == "MYCARE_BACKUP_V1") { "Unsupported backup format" }

                // Cancel all existing alarms before clearing/replacing database records
                val scheduler = ReminderScheduler(context)
                db.reminderDao().getAllRemindersSync().forEach { scheduler.cancelReminder(it.id) }

                // Copy restored media files into app filesDir
                val filesDir = context.filesDir
                listOf("media", "voice_notes").forEach { dir ->
                    val src = File(tempRestoreDir, dir)
                    if (src.exists()) {
                        val dst = File(filesDir, dir)
                        dst.deleteRecursively()
                        src.copyRecursively(dst, overwrite = true)
                    }
                }

                // Remap any old absolute file paths to current filesDir
                fun remapPath(oldPath: String?): String? {
                    if (oldPath.isNullOrBlank()) return null
                    val idxMedia = oldPath.indexOf("/media/")
                    if (idxMedia >= 0) return File(filesDir, oldPath.substring(idxMedia + 1)).absolutePath
                    val idxVoice = oldPath.indexOf("/voice_notes/")
                    if (idxVoice >= 0) return File(filesDir, oldPath.substring(idxVoice + 1)).absolutePath
                    return oldPath
                }

                db.withTransaction {
                db.clearAllTables()
                payload.medicines.forEach {
                    db.medicineDao().insertMedicine(
                        it.copy(imagePath = remapPath(it.imagePath), documentPath = remapPath(it.documentPath))
                    )
                }
                payload.medicineSchedules.forEach { db.medicineDao().insertSchedule(it) }
                payload.medicineDoseLogs.forEach { db.medicineDao().insertDoseLog(it) }
                payload.exercises.forEach { db.exerciseDao().insertExercise(it) }
                payload.exerciseAttachments.forEach {
                    db.exerciseDao().insertAttachment(it.copy(filePath = remapPath(it.filePath) ?: it.filePath))
                }
                payload.exerciseSessions.forEach { db.exerciseDao().insertSession(it) }
                payload.taskCategories.forEach { db.taskDao().insertCategory(it) }
                payload.tasks.forEach {
                    db.taskDao().insertTask(
                        it.copy(imagePath = remapPath(it.imagePath), documentPath = remapPath(it.documentPath))
                    )
                }
                payload.financeCategories.forEach { db.walletDao().insertCategory(it) }
                payload.financeTransactions.forEach { db.walletDao().insertTransaction(it) }
                payload.groceryLists.forEach {
                    db.groceryDao().insertList(it.copy(importedFilePath = remapPath(it.importedFilePath)))
                }
                payload.groceryItems.forEach { db.groceryDao().insertItem(it) }
                payload.noteFolders.forEach { db.memoryDao().insertNoteFolder(it) }
                payload.notes.forEach { db.memoryDao().insertNote(it) }
                payload.memoryAlbums.forEach {
                    db.memoryDao().insertAlbum(it.copy(coverPhotoPath = remapPath(it.coverPhotoPath)))
                }
                payload.albumAttachments.forEach {
                    db.memoryDao().insertAlbumAttachment(it.copy(filePath = remapPath(it.filePath) ?: it.filePath))
                }
                payload.voiceNotes.forEach {
                    db.memoryDao().insertVoiceNote(it.copy(filePath = remapPath(it.filePath) ?: it.filePath))
                }
                payload.medicalFolders.forEach { db.medicalRecordsDao().insertFolder(it) }
                payload.medicalDocuments.forEach {
                    db.medicalRecordsDao().insertDocument(it.copy(filePath = remapPath(it.filePath) ?: it.filePath))
                }
                payload.nutritionEntries.forEach { db.nutritionDao().insertEntry(it) }
                payload.foodChartFiles.forEach {
                    db.nutritionDao().insertChartFile(it.copy(filePath = remapPath(it.filePath) ?: it.filePath))
                }
                payload.travelPlans.forEach { db.travelDao().insertPlan(it) }
                payload.travelExpenses.forEach { db.travelDao().insertExpense(it) }
                payload.travelChecklistItems.forEach { db.travelDao().insertChecklistItem(it) }
                payload.travelAttachments.forEach {
                    db.travelDao().insertAttachment(it.copy(filePath = remapPath(it.filePath) ?: it.filePath))
                }
                payload.emergencyContacts.forEach { db.emergencyDao().insertContact(it) }
                payload.emergencyProfile?.let { db.emergencyDao().upsertProfile(it) }
                payload.reminders.forEach {
                    db.reminderDao().insertReminder(it.copy(ringtoneUri = remapPath(it.ringtoneUri)))
                }
            }

            settingsStore.restoreFromState(
                AppSettingsState(
                    isLanguageSelected = true,
                    languageCode = payload.settings.languageCode,
                    themeMode = ThemeMode.fromKey(payload.settings.themeMode),
                    accentTheme = AccentTheme.fromKey(payload.settings.accentTheme),
                    fontScale = payload.settings.fontScale,
                    notificationsEnabled = payload.settings.notificationsEnabled,
                    defaultRingtoneUri = remapPath(payload.settings.defaultRingtoneUri),
                    defaultRingtoneName = payload.settings.defaultRingtoneName,
                    defaultRingingDurationSec = payload.settings.defaultRingingDurationSec,
                    defaultSnoozeMinutes = payload.settings.defaultSnoozeMinutes,
                    defaultVibration = payload.settings.defaultVibration,
                    notifMedicine = payload.settings.notifMedicine,
                    notifExercise = payload.settings.notifExercise,
                    notifTasks = payload.settings.notifTasks,
                    notifGrocery = payload.settings.notifGrocery,
                    notifAppointments = payload.settings.notifAppointments,
                    notifNutrition = payload.settings.notifNutrition,
                    notifTravel = payload.settings.notifTravel,
                    notifGeneral = payload.settings.notifGeneral
                )
            )
                db.reminderDao().getEnabledRemindersSync().forEach { scheduler.scheduleReminder(it) }
                payload
            } finally {
                tempRestoreDir.deleteRecursively()
            }
        }
    }
}
