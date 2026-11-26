package com.example.core.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.storage.FileStorageManager

data class ReminderDraft(
    val enabled: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis(),
    val hour: Int = 9,
    val minute: Int = 0,
    val repeatType: String = "ONCE",
    val repeatIntervalDays: Int = 1,
    val weekdays: Set<Int> = emptySet(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val ringtoneUri: String? = null,
    val ringingDurationSec: Int = 30,
    val snoozeMinutes: Int = 5
)

/**
 * Simplified per-feature reminder control section:
 * Exposes ONLY:
 * 1. Reminder ON/OFF toggle
 * 2. Reminder Time selection
 *
 * All ringtone, sound, vibration, duration, and notification settings
 * are configured centrally in Settings -> Notifications & Ringtone.
 */
@Composable
fun ReminderConfigSection(
    draft: ReminderDraft,
    onDraftChange: (ReminderDraft) -> Unit,
    @Suppress("UNUSED_PARAMETER") fileStorage: FileStorageManager,
    @Suppress("UNUSED_PARAMETER") audioPlayer: AudioPlaybackManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var askedNotifPerm by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        askedNotifPerm = true
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Reminder ON/OFF Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.reminder_enable),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Switch(
                    checked = draft.enabled,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable && !askedNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !PermissionManager.hasNotificationPermission(context)
                        ) {
                            askedNotifPerm = true
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onDraftChange(draft.copy(enabled = wantEnable))
                    },
                    modifier = Modifier.testTag("reminder_toggle_switch")
                )
            }

            if (draft.enabled) {
                // 2. Reminder Time Selection
                TimePickerField(
                    label = stringResource(R.string.label_time),
                    hour = draft.hour,
                    minute = draft.minute,
                    onTimeSelected = { h, m ->
                        onDraftChange(draft.copy(hour = h, minute = m))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_time_picker")
                )

                Text(
                    text = stringResource(R.string.reminder_global_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!PermissionManager.hasNotificationPermission(context)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_permission_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { PermissionManager.openNotificationSettings(context) }) {
                            Text(stringResource(R.string.action_open_settings))
                        }
                    }
                } else if (!PermissionManager.canScheduleExactAlarms(context)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.exact_alarm_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { PermissionManager.openExactAlarmSettings(context) }) {
                            Text(stringResource(R.string.action_open_settings))
                        }
                    }
                }
            }
        }
    }
}
