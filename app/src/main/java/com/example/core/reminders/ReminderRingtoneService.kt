package com.example.core.reminders

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.R
import java.io.File

class ReminderRingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var activeReminderId: Long = 0L
    private var activeStartId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val stopId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
            if (stopId <= 0L || stopId == activeReminderId) {
                stopAlarmAndSelf()
            }
            return START_NOT_STICKY
        }

        val reminderId = intent?.getLongExtra(EXTRA_REMINDER_ID, 0L) ?: 0L
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
        val desc = intent?.getStringExtra(EXTRA_DESC) ?: ""
        val ringtoneUri = intent?.getStringExtra(EXTRA_RINGTONE_URI)
        val soundEnabled = intent?.getBooleanExtra(EXTRA_SOUND, true) ?: true
        val vibrationEnabled = intent?.getBooleanExtra(EXTRA_VIBRATION, true) ?: true
        val durationSec = intent?.getIntExtra(EXTRA_DURATION_SEC, 30) ?: 30
        val snoozeMin = intent?.getIntExtra(EXTRA_SNOOZE_MIN, 5) ?: 5

        activeReminderId = reminderId
        activeStartId = startId

        ReminderChannels.ensureChannels(this)
        val notification = buildNotification(reminderId, title, desc, snoozeMin)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, notificationIdFor(reminderId), notification, fgType)

        stopPlaybackOnly()

        if (soundEnabled) {
            startSound(ringtoneUri)
        }
        if (vibrationEnabled) {
            startVibration()
        }

        val runnable = Runnable {
            if (activeStartId == startId) {
                stopAlarmAndSelf()
            }
        }
        stopRunnable = runnable
        handler.postDelayed(runnable, durationSec.coerceIn(5, 300) * 1000L)

        return START_NOT_STICKY
    }

    private fun buildNotification(
        reminderId: Long,
        title: String,
        desc: String,
        snoozeMin: Int
    ): Notification {
        val openPending = ReminderIntents.openAppPendingIntent(this, reminderId)
        val dismissPending = ReminderIntents.actionPendingIntent(this, reminderId, ReminderActionReceiver.ACTION_DISMISS)
        val snoozePending = ReminderIntents.actionPendingIntent(this, reminderId, ReminderActionReceiver.ACTION_SNOOZE)
        val turnOffPending = ReminderIntents.actionPendingIntent(this, reminderId, ReminderActionReceiver.ACTION_TURN_OFF)

        return NotificationCompat.Builder(this, ReminderChannels.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.mycare_logo)
            .setContentTitle(title)
            .setContentText(desc.ifBlank { getString(R.string.app_tagline) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.action_dismiss), dismissPending)
            .addAction(0, "${getString(R.string.action_snooze_btn)} ($snoozeMin m)", snoozePending)
            .addAction(0, getString(R.string.action_turn_off_reminder), turnOffPending)
            .build()
    }

    private fun startSound(ringtoneUri: String?) {
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 1. Try custom global ringtone if valid & accessible
        if (!ringtoneUri.isNullOrBlank()) {
            val isValidCustom = if (ringtoneUri.startsWith("/")) {
                val f = File(ringtoneUri)
                f.exists() && f.canRead() && f.length() > 0L
            } else {
                true
            }
            if (isValidCustom) {
                val startedCustom = runCatching {
                    val mp = MediaPlayer().apply {
                        setAudioAttributes(alarmAttrs)
                        isLooping = true
                        if (ringtoneUri.startsWith("/")) {
                            setDataSource(ringtoneUri)
                        } else {
                            setDataSource(this@ReminderRingtoneService, Uri.parse(ringtoneUri))
                        }
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                    true
                }.getOrDefault(false)
                if (startedCustom) return
            }
        }

        // 2. Automatic device default fallback chain (Alarm -> Notification -> Ringtone)
        val systemCandidates = listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )

        for (candidateUri in systemCandidates) {
            val started = runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(alarmAttrs)
                    isLooping = true
                    setDataSource(this@ReminderRingtoneService, candidateUri)
                    prepare()
                    start()
                }
                mediaPlayer = mp
                true
            }.getOrDefault(false)
            if (started) return
        }

        // 3. Final RingtoneManager direct playback fallback for strict vendor ROMs
        for (candidateUri in systemCandidates) {
            val started = runCatching {
                val rt = RingtoneManager.getRingtone(this, candidateUri)
                if (rt != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        rt.isLooping = true
                    }
                    rt.audioAttributes = alarmAttrs
                    rt.play()
                    fallbackRingtone = rt
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (started) return
        }
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vib == null || !vib.hasVibrator()) return
        vibrator = vib

        val pattern = longArrayOf(0, 600, 350, 600, 750)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0), audioAttrs)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, 0)
        }
    }

    private fun stopPlaybackOnly() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { fallbackRingtone?.stop() }
        fallbackRingtone = null
        runCatching { vibrator?.cancel() }
    }

    private fun stopAlarmAndSelf() {
        stopPlaybackOnly()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onDestroy() {
        stopPlaybackOnly()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.example.core.reminders.ACTION_STOP"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESC = "extra_desc"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_SNOOZE_MIN = "extra_snooze_min"
        const val NOTIF_BASE_ID = 7000

        fun notificationIdFor(reminderId: Long): Int {
            val mixed = (reminderId xor (reminderId ushr 32)).toInt() and 0x00FFFFFF
            return NOTIF_BASE_ID + mixed
        }

        fun cancelNotification(context: Context, reminderId: Long) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationIdFor(reminderId))
        }
    }
}
