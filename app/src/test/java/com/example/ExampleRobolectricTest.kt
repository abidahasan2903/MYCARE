package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.MyCareDatabase
import com.example.core.navigation.MyCareNavHost
import com.example.core.database.entity.EmergencyContactEntity
import com.example.core.database.entity.EmergencyProfileEntity
import com.example.core.database.entity.ExerciseEntity
import com.example.core.database.entity.FinanceTransactionEntity
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.GroceryListEntity
import com.example.core.database.entity.MedicalFolderEntity
import com.example.core.database.entity.MedicineEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NutritionEntryEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.database.entity.TravelPlanEntity
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.localization.LocaleManager
import com.example.core.media.AudioPlaybackManager
import com.example.core.reminders.BootReceiver
import com.example.core.reminders.ReminderActionReceiver
import com.example.core.reminders.ReminderReceiver
import com.example.core.reminders.ReminderRingtoneService
import com.example.core.reminders.ReminderScheduler
import com.example.core.storage.FileStorageManager
import com.example.core.theme.AccentTheme
import com.example.core.theme.MyCareTheme
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.GroceryRepository
import com.example.data.repository.MedicalRecordsRepository
import com.example.data.repository.MedicineRepository
import com.example.data.repository.NutritionRepository
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.WalletRepository
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.dashboard.DashboardViewModel
import com.example.feature.wallet.WalletScreen
import com.example.feature.wallet.WalletViewModel
import com.example.feature.onboarding.LanguageSelectionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: MyCareDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MyCareDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appBranding_andLocalization_areConfiguredForEnglishAndBangla() {
        assertEquals("MYCARE", context.getString(R.string.app_name))
        assertEquals("Developed by Abid Ahasan", context.getString(R.string.developer_credit))

        val bnContext = LocaleManager.getLocalizedContext(context, LocaleManager.LANG_BANGLA)
        assertEquals("MYCARE", bnContext.getString(R.string.app_name))
        assertTrue(bnContext.getString(R.string.welcome_title).contains("MYCARE"))
        assertEquals("হোম", bnContext.getString(R.string.nav_home))
        assertEquals("Developed by Abid Ahasan", bnContext.getString(R.string.developer_credit))
    }

    @Test
    fun mainActivity_launchesOnColdStart() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertEquals(androidx.lifecycle.Lifecycle.State.RESUMED, activity.get().lifecycle.currentState)
        activity.pause().stop().destroy()
    }

    @Test
    fun roomDatabase_startsCleanAndPersistsAllModules() = runTest {
        assertTrue(db.medicineDao().getAllMedicinesSync().isEmpty())
        assertTrue(db.taskDao().getAllTasksSync().isEmpty())
        assertTrue(db.walletDao().getAllTransactionsSync().isEmpty())
        assertTrue(db.emergencyDao().getAllContactsSync().isEmpty())

        val medId = db.medicineDao().insertMedicine(
            MedicineEntity(name = "Paracetamol", strength = "500 mg", dosageInstructions = "After meal")
        )
        assertTrue(medId > 0)
        assertEquals(1, db.medicineDao().observeAllMedicines().first().size)

        val exId = db.exerciseDao().insertExercise(
            ExerciseEntity(name = "Morning Walk", durationMinutes = 30, category = "Cardio")
        )
        assertTrue(exId > 0)
        assertEquals(1, db.exerciseDao().observeAllExercises().first().size)

        val taskId = db.taskDao().insertTask(TaskEntity(title = "Buy vitamins", priority = "HIGH"))
        assertTrue(taskId > 0)
        assertEquals(1, db.taskDao().observeAllTasks().first().size)

        db.walletDao().insertTransaction(
            FinanceTransactionEntity(type = "INCOME", title = "Salary", amount = 50000.0, category = "Salary")
        )
        db.walletDao().insertTransaction(
            FinanceTransactionEntity(type = "EXPENSE", title = "Groceries", amount = 2500.0, category = "Food")
        )
        assertEquals(2, db.walletDao().observeAllTransactions().first().size)

        val listId = db.groceryDao().insertList(GroceryListEntity(name = "Weekly Market"))
        db.groceryDao().insertItem(
            GroceryItemEntity(listId = listId, name = "Apples", quantity = 2.0, unit = "kg", estimatedPrice = 300.0)
        )
        assertEquals(1, db.groceryDao().observeItemsForList(listId).first().size)

        db.memoryDao().insertNote(NoteEntity(title = "Checkup Notes", content = "Drink warm water"))
        assertEquals(1, db.memoryDao().observeAllNotes().first().size)

        db.medicalRecordsDao().insertFolder(MedicalFolderEntity(conditionName = "Routine Checkup"))
        assertEquals(1, db.medicalRecordsDao().observeAllFolders().first().size)

        db.nutritionDao().insertEntry(NutritionEntryEntity(foodName = "Oats", mealCategory = "Breakfast", calories = 250))
        assertEquals(1, db.nutritionDao().observeAllEntries().first().size)

        db.travelDao().insertPlan(TravelPlanEntity(tripName = "Cox's Bazar", destination = "Cox's Bazar", budget = 15000.0))
        assertEquals(1, db.travelDao().observeAllPlans().first().size)

        db.emergencyDao().insertContact(EmergencyContactEntity(name = "Doctor", phoneNumber = "01700000000"))
        db.emergencyDao().upsertProfile(EmergencyProfileEntity(bloodGroup = "O+"))
        assertEquals(1, db.emergencyDao().observeAllContacts().first().size)
        assertEquals("O+", db.emergencyDao().observeProfile().first()?.bloodGroup)
    }

    @Test
    fun roomDatabase_migratesVersionOneToTwoWithoutLosingWalletData() = runBlocking {
        val dbName = "mycare-migration-${System.nanoTime()}.db"
        val initial = Room.databaseBuilder(context, MyCareDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        initial.openHelper.writableDatabase
        initial.walletDao().insertTransaction(
            FinanceTransactionEntity(type = "INCOME", title = "Keep during migration", amount = 12.0, category = "Other")
        )
        initial.close()

        val oldDb = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        try {
            oldDb.execSQL("DROP INDEX IF EXISTS `index_water_intake_records_recordedAtMillis`")
            oldDb.execSQL("DROP INDEX IF EXISTS `index_sleep_records_bedtimeMillis`")
            oldDb.execSQL("DROP INDEX IF EXISTS `index_sleep_records_wakeTimeMillis`")
            oldDb.execSQL("DROP TABLE IF EXISTS `water_intake_records`")
            oldDb.execSQL("DROP TABLE IF EXISTS `sleep_records`")
            oldDb.execSQL("PRAGMA user_version = 1")
        } finally {
            oldDb.close()
        }

        val upgraded = Room.databaseBuilder(context, MyCareDatabase::class.java, dbName)
            .addMigrations(MyCareDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            upgraded.openHelper.writableDatabase
            assertTrue(upgraded.wellnessDao().getAllWaterIntakesSync().isEmpty())
            assertTrue(upgraded.wellnessDao().getAllSleepRecordsSync().isEmpty())
            assertEquals("Keep during migration", upgraded.walletDao().getAllTransactionsSync().single().title)
        } finally {
            upgraded.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun appSettingsDataStore_persistsThemeMasterNotifAndGlobalRingtone() = runTest {
        val store = AppSettingsDataStore(context)
        store.setAccentTheme(AccentTheme.SOFT_PINK)
        store.setNotificationsEnabled(false)
        store.setDefaultRingtone("/tmp/custom_alarm.mp3", "custom_alarm.mp3")

        val updated = store.settingsFlow.first()
        assertEquals(AccentTheme.SOFT_PINK, updated.accentTheme)
        assertEquals(false, updated.notificationsEnabled)
        assertEquals("/tmp/custom_alarm.mp3", updated.defaultRingtoneUri)
        assertEquals("custom_alarm.mp3", updated.defaultRingtoneName)

        // Removing custom ringtone resets to null so device default fallback activates
        store.setDefaultRingtone(null, null)
        store.setNotificationsEnabled(true)
        val reset = store.settingsFlow.first()
        assertNull(reset.defaultRingtoneUri)
        assertNull(reset.defaultRingtoneName)
        assertEquals(true, reset.notificationsEnabled)
    }

    @Test
    fun languageSelectionScreen_selectsLanguageAndContinues() {
        var selectedLanguage: String? = null
        composeRule.setContent {
            MyCareTheme {
                LanguageSelectionScreen(onContinue = { selectedLanguage = it })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Welcome to MYCARE").assertIsDisplayed()
        composeRule.onNodeWithTag("lang_option_bangla").performScrollTo().performClick()
        composeRule.onNodeWithTag("lang_continue_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(LocaleManager.LANG_BANGLA, selectedLanguage)
    }

    @Test
    fun reminderConfigSection_exposesOnlyToggleAndTimePicker() {
        val storage = FileStorageManager(context)
        val audioPlayer = AudioPlaybackManager(context)
        var draft by mutableStateOf(ReminderDraft(enabled = false))

        composeRule.setContent {
            MyCareTheme {
                ReminderConfigSection(
                    draft = draft,
                    onDraftChange = { draft = it },
                    fileStorage = storage,
                    audioPlayer = audioPlayer
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reminder_toggle_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("reminder_time_picker").assertDoesNotExist()

        composeRule.onNodeWithTag("reminder_toggle_switch").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reminder_time_picker").assertIsDisplayed()
        // Per-feature sound/ringtone/duration/vibration controls must NOT appear
        composeRule.onNodeWithText("Ringing Duration").assertDoesNotExist()
        composeRule.onNodeWithText("Browse Audio File").assertDoesNotExist()
    }

    @Test
    fun dashboardScreen_rendersTwoColumnGridAndBranding() {
        val storage = FileStorageManager(context)
        val scheduler = ReminderScheduler(context)
        val reminderRepo = ReminderRepository(db.reminderDao(), scheduler)
        val vm = DashboardViewModel(
            medicineRepo = MedicineRepository(db.medicineDao(), reminderRepo, storage),
            exerciseRepo = ExerciseRepository(db.exerciseDao(), reminderRepo, storage),
            taskRepo = TaskRepository(db.taskDao(), reminderRepo, storage),
            groceryRepo = GroceryRepository(db.groceryDao(), storage),
            medicalRepo = MedicalRecordsRepository(db.medicalRecordsDao(), storage),
            walletRepo = WalletRepository(db.walletDao()),
            nutritionRepo = NutritionRepository(db.nutritionDao(), reminderRepo, storage),
            reminderRepo = reminderRepo
        )

        composeRule.setContent {
            MyCareTheme(accentTheme = AccentTheme.SOFT_BLUE) {
                DashboardScreen(viewModel = vm, onNavigate = {})
            }
        }
        composeRule.waitForIdle()

        val cardTags = listOf(
            "card_medicine",
            "card_exercise",
            "card_tasks",
            "card_wallet",
            "card_grocery",
            "card_medical_records",
            "card_doctor_search",
            "card_nutrition",
            "card_travel",
            "card_emergency"
        )
        cardTags.forEach { tag ->
            composeRule.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun walletScreen_exposesOverviewTransactionsAndCalculatorNavigation() {
        val viewModel = WalletViewModel(WalletRepository(db.walletDao()))
        composeRule.setContent {
            MyCareTheme { WalletScreen(viewModel = viewModel) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("My Wallet").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_wallet_overview").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_wallet_transactions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab_wallet_calculator").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tab_wallet_overview").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_add_income").assertIsDisplayed()
    }

    @Test
    fun bottomNavigation_opensMyWalletFeature() {
        val app = context.applicationContext as MyCareApplication
        composeRule.setContent {
            MyCareTheme { MyCareNavHost(container = app.container, isLanguageSelected = true) }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_wallet").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("My Wallet").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_wallet_overview").assertIsDisplayed()
        composeRule.onNodeWithTag("top_bar_back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_list").assertIsDisplayed()
    }

    @Test
    fun myCareTheme_transformsBackgroundSurfaceAndContainerAcrossAllFourThemes() {
        var activeTheme by mutableStateOf(AccentTheme.SOFT_ASH)
        val captured = mutableMapOf<AccentTheme, List<androidx.compose.ui.graphics.Color>>()

        composeRule.setContent {
            MyCareTheme(accentTheme = activeTheme) {
                val cs = androidx.compose.material3.MaterialTheme.colorScheme
                captured[activeTheme] = listOf(cs.background, cs.surface, cs.surfaceContainer, cs.primary)
            }
        }
        composeRule.waitForIdle()

        AccentTheme.entries.forEach { theme ->
            activeTheme = theme
            composeRule.waitForIdle()
        }

        val backgrounds = AccentTheme.entries.map { captured.getValue(it)[0] }.toSet()
        val surfaces = AccentTheme.entries.map { captured.getValue(it)[1] }.toSet()
        val containers = AccentTheme.entries.map { captured.getValue(it)[2] }.toSet()
        val primaries = AccentTheme.entries.map { captured.getValue(it)[3] }.toSet()

        assertEquals(4, backgrounds.size)
        assertEquals(4, surfaces.size)
        assertEquals(4, containers.size)
        assertEquals(4, primaries.size)
    }

    private fun waitUntil(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            Thread.sleep(25L)
        }
        assertTrue("Condition timed out after ${timeoutMs}ms", predicate())
    }

    @Test
    fun reminderActionReceiver_turnOffDisablesOnlyTargetedReminderAndCancelsAlarm() = runBlocking {
        val singletonDb = MyCareDatabase.getInstance(context)
        withContext(Dispatchers.IO) { singletonDb.clearAllTables() }
        val scheduler = ReminderScheduler(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val futureTime = System.currentTimeMillis() + 600_000L
        val r1Id = singletonDb.reminderDao().insertReminder(
            ReminderEntity(
                title = "Morning Medicine",
                triggerTimeMillis = futureTime,
                repeatType = "DAILY",
                isEnabled = true
            )
        )
        val r2Id = singletonDb.reminderDao().insertReminder(
            ReminderEntity(
                title = "Evening Exercise",
                triggerTimeMillis = futureTime + 60_000L,
                repeatType = "DAILY",
                isEnabled = true
            )
        )
        val r1 = singletonDb.reminderDao().getReminderById(r1Id)!!
        val r2 = singletonDb.reminderDao().getReminderById(r2Id)!!
        scheduler.scheduleReminder(r1)
        scheduler.scheduleReminder(r2)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        val turnOffIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_TURN_OFF
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, r1Id)
        }
        ReminderActionReceiver().onReceive(context, turnOffIntent)

        waitUntil {
            val updated1 = runBlocking(Dispatchers.IO) { singletonDb.reminderDao().getReminderById(r1Id) }
            updated1 != null && !updated1.isEnabled && shadowAlarmManager.scheduledAlarms.size == 1
        }

        val updated1 = singletonDb.reminderDao().getReminderById(r1Id)
        val updated2 = singletonDb.reminderDao().getReminderById(r2Id)
        assertNotNull(updated1)
        assertFalse(updated1!!.isEnabled)
        assertNotNull(updated2)
        assertTrue(updated2!!.isEnabled)

        scheduler.cancelReminder(r2Id)
        withContext(Dispatchers.IO) { singletonDb.clearAllTables() }
    }

    @Test
    fun reminderSnooze_worksForOnceReminderEvenAfterInitialFireDisablesIt() = runBlocking {
        val singletonDb = MyCareDatabase.getInstance(context)
        withContext(Dispatchers.IO) { singletonDb.clearAllTables() }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)

        val onceId = singletonDb.reminderDao().insertReminder(
            ReminderEntity(
                title = "One-Time Dose",
                triggerTimeMillis = System.currentTimeMillis() + 300_000L,
                repeatType = "ONCE",
                isEnabled = true
            )
        )

        // Initial trigger disables ONCE reminder in DB
        val fireIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, onceId)
            putExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false)
        }
        ReminderReceiver().onReceive(context, fireIntent)
        waitUntil {
            runBlocking(Dispatchers.IO) { singletonDb.reminderDao().getReminderById(onceId)?.isEnabled == false }
        }

        // User taps Snooze
        val snoozeAction = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, onceId)
        }
        ReminderActionReceiver().onReceive(context, snoozeAction)
        waitUntil { shadowAlarmManager.scheduledAlarms.isNotEmpty() }

        // Snoozed ONCE trigger arrives and updates lastTriggeredMillis cleanly
        val snoozeFire = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, onceId)
            putExtra(ReminderScheduler.EXTRA_IS_SNOOZE, true)
        }
        ReminderReceiver().onReceive(context, snoozeFire)
        waitUntil {
            val item = runBlocking(Dispatchers.IO) { singletonDb.reminderDao().getReminderById(onceId) }
            (item?.lastTriggeredMillis ?: 0L) > 0L
        }

        ReminderScheduler(context).cancelReminder(onceId)
        withContext(Dispatchers.IO) { singletonDb.clearAllTables() }
    }

    @Test
    fun bootReceiver_reschedulesOnlyEnabledRemindersOnBootAndTimeSet() = runBlocking {
        val singletonDb = MyCareDatabase.getInstance(context)
        withContext(Dispatchers.IO) { singletonDb.clearAllTables() }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        while (shadowAlarmManager.scheduledAlarms.isNotEmpty()) {
            shadowAlarmManager.scheduledAlarms.clear()
        }

        val futureTime = System.currentTimeMillis() + 600_000L
        val enabledId = singletonDb.reminderDao().insertReminder(
            ReminderEntity(
                title = "Enabled Boot Test",
                triggerTimeMillis = futureTime,
                repeatType = "DAILY",
                isEnabled = true
            )
        )
        val disabledId = singletonDb.reminderDao().insertReminder(
            ReminderEntity(
                title = "Disabled Boot Test",
                triggerTimeMillis = futureTime,
                repeatType = "DAILY",
                isEnabled = false
            )
        )

        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        waitUntil { shadowAlarmManager.scheduledAlarms.size == 1 }

        BootReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))
        waitUntil { shadowAlarmManager.scheduledAlarms.size == 1 }

        ReminderScheduler(context).cancelReminder(enabledId)
        ReminderScheduler(context).cancelReminder(disabledId)
        withContext(Dispatchers.IO) { singletonDb.clearAllTables() }
    }

    @Test
    fun reminderRingtoneService_notificationExposesDismissSnoozeAndTurnOffActions() {
        val serviceIntent = Intent(context, ReminderRingtoneService::class.java).apply {
            putExtra(ReminderRingtoneService.EXTRA_REMINDER_ID, 42L)
            putExtra(ReminderRingtoneService.EXTRA_TITLE, "Test Pill")
            putExtra(ReminderRingtoneService.EXTRA_DESC, "With water")
            putExtra(ReminderRingtoneService.EXTRA_SOUND, false)
            putExtra(ReminderRingtoneService.EXTRA_VIBRATION, false)
            putExtra(ReminderRingtoneService.EXTRA_DURATION_SEC, 15)
            putExtra(ReminderRingtoneService.EXTRA_SNOOZE_MIN, 5)
        }
        val controller = Robolectric.buildService(ReminderRingtoneService::class.java, serviceIntent)
            .create()
            .startCommand(0, 1)
        val service = controller.get()
        val shadowService = Shadows.shadowOf(service)
        val notif = shadowService.lastForegroundNotification
        assertNotNull(notif)
        val titles = notif.actions.map { it.title.toString() }
        assertTrue(titles.contains(context.getString(R.string.action_dismiss)))
        assertTrue(titles.any { it.contains(context.getString(R.string.action_snooze_btn)) })
        assertTrue(titles.contains(context.getString(R.string.action_turn_off_reminder)))

        controller.destroy()
    }
}
