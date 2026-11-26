package com.example.core.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.core.database.entity.ReminderEntity
import com.example.core.permissions.PermissionManager
import java.util.Calendar

object ReminderChannels {
    const val CHANNEL_REMINDERS = "mycare_reminders_channel"
    const val CHANNEL_REMINDERS_ALERT = "mycare_reminders_alert_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for ReminderRingtoneService foreground notifications (service plays exact custom/default ringtone)
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                "MYCARE Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled medicine, exercise, task, nutrition, and travel reminders"
                enableVibration(true)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)

            // Audible fallback channel used only if foreground service cannot start from background
            val defaultAlertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val alertAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val fallbackChannel = NotificationChannel(
                CHANNEL_REMINDERS_ALERT,
                "MYCARE Reminder Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fallback reminder alerts when background audio service is restricted"
                enableVibration(true)
                if (defaultAlertUri != null) {
                    setSound(defaultAlertUri, alertAttrs)
                }
            }
            nm.createNotificationChannel(fallbackChannel)
        }
    }
}

object ReminderIntents {
    fun requestCode(reminderId: Long, kindOffset: Int): Int {
        return (((reminderId * 31L) + kindOffset) and 0x7FFFFFFFL).toInt()
    }

    fun openAppPendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("mycare://reminder/$reminderId/open")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode(reminderId, 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun actionPendingIntent(context: Context, reminderId: Long, action: String): PendingIntent {
        val (kindOffset, kindName) = when (action) {
            ReminderActionReceiver.ACTION_DISMISS -> 1 to "dismiss"
            ReminderActionReceiver.ACTION_SNOOZE -> 2 to "snooze_action"
            ReminderActionReceiver.ACTION_TURN_OFF -> 3 to "turn_off"
            else -> 4 to "other"
        }
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("mycare://reminder/$reminderId/$kindName")
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, kindOffset),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderScheduler(private val context: Context) {

    fun scheduleReminder(reminder: ReminderEntity): Boolean {
        return scheduleAtReferenceNow(reminder, System.currentTimeMillis()).second
    }

    /**
     * Schedules the next recurring occurrence strictly after [reminder.triggerTimeMillis] and now,
     * eliminating double-fire when OEMs deliver exact alarms slightly early within the same minute.
     */
    fun scheduleNextOccurrence(reminder: ReminderEntity): Pair<Long?, Boolean> {
        val referenceNow = maxOf(System.currentTimeMillis(), reminder.triggerTimeMillis) + 1_000L
        return scheduleAtReferenceNow(reminder, referenceNow)
    }

    private fun scheduleAtReferenceNow(
        reminder: ReminderEntity,
        referenceNowMillis: Long
    ): Pair<Long?, Boolean> {
        ReminderChannels.ensureChannels(context)
        // Always cancel stale alarm for this reminder ID before re-scheduling (without touching snooze)
        cancelPrimaryTrigger(reminder.id)
        if (!reminder.isEnabled) {
            cancelSnooze(reminder.id)
            return null to true
        }
        val nextTrigger = computeNextTriggerMillis(reminder, referenceNowMillis)
            ?: return null to false

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildTriggerPendingIntent(reminder.id, isSnooze = false)

        val scheduledExact = if (PermissionManager.canScheduleExactAlarms(context)) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger,
                    pendingIntent
                )
                true
            }.getOrDefault(false)
        } else {
            false
        }

        if (!scheduledExact) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger,
                    pendingIntent
                )
            }
        }
        return nextTrigger to scheduledExact
    }

    fun scheduleSnooze(reminder: ReminderEntity) {
        ReminderChannels.ensureChannels(context)
        cancelSnooze(reminder.id)
        val triggerAt = System.currentTimeMillis() + reminder.snoozeMinutes.coerceAtLeast(1) * 60_000L
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildTriggerPendingIntent(reminder.id, isSnooze = true)
        if (PermissionManager.canScheduleExactAlarms(context)) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }.onFailure {
                runCatching {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
        } else {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancelReminder(reminderId: Long) {
        cancelPrimaryTrigger(reminderId)
        cancelSnooze(reminderId)
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        }
    }

    private fun cancelPrimaryTrigger(reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildTriggerPendingIntent(reminderId, isSnooze = false)
        alarmManager.cancel(pi)
        pi.cancel()
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork("reminder_$reminderId")
        }
    }

    private fun cancelSnooze(reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozePi = buildTriggerPendingIntent(reminderId, isSnooze = true)
        alarmManager.cancel(snoozePi)
        snoozePi.cancel()
    }

    private fun buildTriggerPendingIntent(reminderId: Long, isSnooze: Boolean): PendingIntent {
        val kindName = if (isSnooze) "snooze_trigger" else "trigger"
        val kindOffset = if (isSnooze) 11 else 10
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.parse("mycare://reminder/$reminderId/$kindName")
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            ReminderIntents.requestCode(reminderId, kindOffset),
            intent,
            flags
        )
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        fun computeNextTriggerMillis(reminder: ReminderEntity, nowMillis: Long): Long? {
            val base = Calendar.getInstance().apply { timeInMillis = reminder.triggerTimeMillis }
            val hour = base.get(Calendar.HOUR_OF_DAY)
            val minute = base.get(Calendar.MINUTE)

            return when (reminder.repeatType) {
                "ONCE" -> if (reminder.triggerTimeMillis > nowMillis) reminder.triggerTimeMillis else null
                "DAILY" -> {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (cal.timeInMillis <= nowMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.timeInMillis
                }
                "WEEKLY" -> {
                    val targetDay = base.get(Calendar.DAY_OF_WEEK)
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    var guard = 0
                    while ((cal.get(Calendar.DAY_OF_WEEK) != targetDay || cal.timeInMillis <= nowMillis) && guard < 8) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        guard++
                    }
                    cal.timeInMillis
                }
                "CUSTOM" -> {
                    val selectedWeekdays = reminder.weekdaysCsv
                        .split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
                        .toSet()
                    if (selectedWeekdays.isNotEmpty()) {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = nowMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        var guard = 0
                        while ((cal.get(Calendar.DAY_OF_WEEK) !in selectedWeekdays || cal.timeInMillis <= nowMillis) && guard < 14) {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            guard++
                        }
                        cal.timeInMillis
                    } else {
                        val interval = reminder.repeatIntervalDays.coerceAtLeast(1)
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = reminder.triggerTimeMillis
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        var guard = 0
                        while (cal.timeInMillis <= nowMillis && guard < 3650) {
                            cal.add(Calendar.DAY_OF_YEAR, interval)
                            guard++
                        }
                        cal.timeInMillis
                    }
                }
                else -> if (reminder.triggerTimeMillis > nowMillis) reminder.triggerTimeMillis else null
            }
        }
    }
}

class ReminderFallbackWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val id = inputData.getLong(ReminderScheduler.EXTRA_REMINDER_ID, -1L)
        if (id > 0L) {
            val intent = Intent(applicationContext, ReminderReceiver::class.java).apply {
                putExtra(ReminderScheduler.EXTRA_REMINDER_ID, id)
            }
            applicationContext.sendBroadcast(intent)
        }
        return Result.success()
    }
}
