package com.example.core.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.core.database.MyCareDatabase
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.localization.LocaleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return
        val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MyCareDatabase.getInstance(context)
                val settings = AppSettingsDataStore(context).settingsFlow.first()
                val localizedCtx = LocaleManager.getLocalizedContext(context, settings.languageCode)
                val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch

                // Allow snoozed ONCE reminders to fire cleanly even though initial trigger set isEnabled = false
                if (!reminder.isEnabled && !isSnooze) return@launch

                val featureAllowed = when (reminder.featureType) {
                    "MEDICINE" -> settings.notifMedicine
                    "EXERCISE" -> settings.notifExercise
                    "TASK" -> settings.notifTasks
                    "GROCERY" -> settings.notifGrocery
                    "APPOINTMENT" -> settings.notifAppointments
                    "NUTRITION" -> settings.notifNutrition
                    "TRAVEL" -> settings.notifTravel
                    else -> settings.notifGeneral
                }

                val nowMillis = System.currentTimeMillis()
                if (isSnooze) {
                    db.reminderDao().updateReminder(
                        reminder.copy(lastTriggeredMillis = nowMillis)
                    )
                } else if (reminder.repeatType == "ONCE") {
                    db.reminderDao().updateReminder(
                        reminder.copy(
                            lastTriggeredMillis = nowMillis,
                            isEnabled = false
                        )
                    )
                } else {
                    val (nextTrigger, _) = ReminderScheduler(context).scheduleNextOccurrence(reminder)
                    db.reminderDao().updateReminder(
                        reminder.copy(
                            lastTriggeredMillis = nowMillis,
                            triggerTimeMillis = nextTrigger ?: reminder.triggerTimeMillis,
                            isEnabled = true
                        )
                    )
                }

                // Respect master notification toggle AND per-feature notification preference
                if (!settings.notificationsEnabled || !featureAllowed || !reminder.notificationEnabled) {
                    return@launch
                }

                // Always use the centralized global ringtone, duration, snooze, and vibration settings
                val globalRingtone = settings.defaultRingtoneUri
                val globalDuration = settings.defaultRingingDurationSec
                val globalSnooze = settings.defaultSnoozeMinutes
                val globalVibration = settings.defaultVibration

                val serviceIntent = Intent(localizedCtx, ReminderRingtoneService::class.java).apply {
                    putExtra(ReminderRingtoneService.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(ReminderRingtoneService.EXTRA_TITLE, reminder.title)
                    putExtra(ReminderRingtoneService.EXTRA_DESC, reminder.description)
                    putExtra(ReminderRingtoneService.EXTRA_RINGTONE_URI, globalRingtone)
                    putExtra(ReminderRingtoneService.EXTRA_SOUND, true)
                    putExtra(ReminderRingtoneService.EXTRA_VIBRATION, globalVibration)
                    putExtra(ReminderRingtoneService.EXTRA_DURATION_SEC, globalDuration)
                    putExtra(ReminderRingtoneService.EXTRA_SNOOZE_MIN, globalSnooze)
                }
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        localizedCtx.startForegroundService(serviceIntent)
                    } else {
                        localizedCtx.startService(serviceIntent)
                    }
                }.onFailure {
                    postFallbackNotification(
                        context = localizedCtx,
                        reminderId = reminder.id,
                        title = reminder.title,
                        desc = reminder.description,
                        snoozeMin = globalSnooze
                    )
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun postFallbackNotification(
        context: Context,
        reminderId: Long,
        title: String,
        desc: String,
        snoozeMin: Int
    ) {
        ReminderChannels.ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = ReminderIntents.openAppPendingIntent(context, reminderId)
        val dismissIntent = ReminderIntents.actionPendingIntent(context, reminderId, ReminderActionReceiver.ACTION_DISMISS)
        val snoozeIntent = ReminderIntents.actionPendingIntent(context, reminderId, ReminderActionReceiver.ACTION_SNOOZE)
        val turnOffIntent = ReminderIntents.actionPendingIntent(context, reminderId, ReminderActionReceiver.ACTION_TURN_OFF)

        val notif = NotificationCompat.Builder(context, ReminderChannels.CHANNEL_REMINDERS_ALERT)
            .setSmallIcon(R.drawable.mycare_logo)
            .setContentTitle(title)
            .setContentText(desc.ifBlank { context.getString(R.string.app_tagline) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .addAction(0, context.getString(R.string.action_dismiss), dismissIntent)
            .addAction(0, "${context.getString(R.string.action_snooze_btn)} ($snoozeMin m)", snoozeIntent)
            .addAction(0, context.getString(R.string.action_turn_off_reminder), turnOffIntent)
            .build()
        nm.notify(ReminderRingtoneService.notificationIdFor(reminderId), notif)
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        // Stop ringtone & vibration safely without triggering background startService restrictions on API 26+
        runCatching { context.stopService(Intent(context, ReminderRingtoneService::class.java)) }
        ReminderRingtoneService.cancelNotification(context, reminderId)

        when (intent.action) {
            ACTION_SNOOZE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = MyCareDatabase.getInstance(context)
                        val settings = AppSettingsDataStore(context).settingsFlow.first()
                        val reminder = db.reminderDao().getReminderById(reminderId)
                        if (reminder != null) {
                            ReminderScheduler(context).scheduleSnooze(
                                reminder.copy(snoozeMinutes = settings.defaultSnoozeMinutes)
                            )
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
            ACTION_TURN_OFF -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ReminderScheduler(context).cancelReminder(reminderId)
                        val db = MyCareDatabase.getInstance(context)
                        val reminder = db.reminderDao().getReminderById(reminderId)
                        if (reminder != null && reminder.isEnabled) {
                            db.reminderDao().updateReminder(reminder.copy(isEnabled = false))
                        }
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.example.core.reminders.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.core.reminders.ACTION_SNOOZE"
        const val ACTION_TURN_OFF = "com.example.core.reminders.ACTION_TURN_OFF"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MyCareDatabase.getInstance(context)
                    val scheduler = ReminderScheduler(context)
                    val enabled = db.reminderDao().getEnabledRemindersSync()
                    enabled.forEach { scheduler.scheduleReminder(it) }
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
