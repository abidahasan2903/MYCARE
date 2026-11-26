package com.example.feature.settings

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.datastore.AppSettingsState
import com.example.core.localization.LocaleManager
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.storage.BackupManager
import com.example.core.storage.FileStorageManager
import com.example.core.storage.StorageBreakdown
import com.example.core.theme.AccentTheme
import com.example.core.theme.ThemeMode
import com.example.core.ui.components.ConfirmDeleteDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SettingsViewModel(
    private val settingsStore: AppSettingsDataStore,
    val fileStorage: FileStorageManager,
    private val backupManager: BackupManager,
    val audioPlayer: AudioPlaybackManager
) : ViewModel() {

    val settings: StateFlow<AppSettingsState> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsState())

    private val _storageBreakdown = MutableStateFlow(StorageBreakdown())
    val storageBreakdown: StateFlow<StorageBreakdown> = _storageBreakdown.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun refreshStorage() {
        viewModelScope.launch {
            _storageBreakdown.value = fileStorage.calculateStorageBreakdown()
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }
    fun setAccentTheme(accent: AccentTheme) = viewModelScope.launch { settingsStore.setAccentTheme(accent) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsStore.setFontScale(scale) }
    fun setLanguage(code: String) = viewModelScope.launch { settingsStore.setLanguageCode(code) }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setNotificationsEnabled(enabled)
    }

    fun importDefaultRingtone(uri: Uri, onError: () -> Unit = {}) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri, forcedMediaType = "AUDIO")
                .onSuccess {
                    audioPlayer.stop()
                    settingsStore.setDefaultRingtone(it.filePath, it.displayName)
                    refreshStorage()
                }
                .onFailure {
                    onError()
                }
        }
    }

    fun clearDefaultRingtone() = viewModelScope.launch {
        audioPlayer.stop()
        settingsStore.setDefaultRingtone(null, null)
    }

    fun setDefaultRingingDuration(sec: Int) = viewModelScope.launch {
        settingsStore.setDefaultRingingDurationSec(sec)
    }

    fun setDefaultSnooze(min: Int) = viewModelScope.launch {
        settingsStore.setDefaultSnoozeMinutes(min)
    }

    fun setDefaultVibration(enabled: Boolean) = viewModelScope.launch {
        settingsStore.setDefaultVibration(enabled)
    }

    fun setFeatureNotification(key: String, enabled: Boolean) = viewModelScope.launch {
        settingsStore.setFeatureNotification(key, enabled)
    }

    fun clearCategoryStorage(category: String) {
        viewModelScope.launch {
            fileStorage.clearCategoryFiles(category)
            refreshStorage()
        }
    }

    fun exportBackup(destUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val ok = backupManager.exportBackupToUri(destUri).isSuccess
            _isBusy.value = false
            onResult(ok)
        }
    }

    fun restoreBackup(sourceUri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val ok = backupManager.validateAndRestoreBackup(sourceUri).isSuccess
            _isBusy.value = false
            refreshStorage()
            onResult(ok)
        }
    }
}

private data class ThemePreviewSpec(
    val theme: AccentTheme,
    val titleRes: Int,
    val descRes: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storage by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val playState by viewModel.audioPlayer.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var clearCategoryTarget by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var infoDialogContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    val backupSuccessMsg = stringResource(R.string.backup_success)
    val restoreSuccessMsg = stringResource(R.string.restore_success)
    val backupInvalidMsg = stringResource(R.string.backup_invalid)
    val attachmentErrorMsg = stringResource(R.string.msg_attachment_error)

    LaunchedEffect(Unit) {
        viewModel.refreshStorage()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.audioPlayer.stop() }
    }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDefaultRingtone(uri) {
                scope.launch { snackbarHostState.showSnackbar(attachmentErrorMsg) }
            }
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri) { ok ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (ok) backupSuccessMsg else backupInvalidMsg)
                }
            }
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    val themeOptions = remember {
        listOf(
            ThemePreviewSpec(AccentTheme.SOFT_ASH, R.string.theme_soft_ash, R.string.theme_soft_ash_desc),
            ThemePreviewSpec(AccentTheme.SOFT_PINK, R.string.theme_soft_pink, R.string.theme_soft_pink_desc),
            ThemePreviewSpec(AccentTheme.SOFT_BLUE, R.string.theme_soft_blue, R.string.theme_soft_blue_desc),
            ThemePreviewSpec(AccentTheme.SOFT_WHITE_GREEN, R.string.theme_soft_white_green, R.string.theme_soft_white_green_desc)
        )
    }

    val hasValidCustomRingtone = remember(settings.defaultRingtoneUri) {
        val path = settings.defaultRingtoneUri
        !path.isNullOrBlank() && viewModel.fileStorage.fileExists(path)
    }

    val displayedRingtoneName = when {
        hasValidCustomRingtone && !settings.defaultRingtoneName.isNullOrBlank() -> settings.defaultRingtoneName!!
        hasValidCustomRingtone -> settings.defaultRingtoneUri!!.substringAfterLast('/')
        else -> stringResource(R.string.ringtone_device_default)
    }

    val previewSourceUri = remember(hasValidCustomRingtone, settings.defaultRingtoneUri) {
        if (hasValidCustomRingtone) {
            settings.defaultRingtoneUri
        } else {
            (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.toString()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // A. Appearance -> App Theme (4 Visual Theme Preview Cards) + Mode & Text Size
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_appearance),
                        icon = Icons.Default.Palette
                    ) {
                        Text(stringResource(R.string.set_app_theme), style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            themeOptions.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Max),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { spec ->
                                        ThemePreviewCard(
                                            spec = spec,
                                            selected = settings.accentTheme == spec.theme,
                                            onClick = { viewModel.setAccentTheme(spec.theme) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .testTag("theme_card_${spec.theme.key}")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                ThemeMode.LIGHT to R.string.set_theme_light,
                                ThemeMode.SYSTEM to R.string.set_theme_system,
                                ThemeMode.DARK to R.string.set_theme_dark
                            ).forEach { (mode, labelRes) ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.set_font_scale), style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${(settings.fontScale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.fontScale,
                            onValueChange = { viewModel.setFontScale(it) },
                            valueRange = 0.85f..1.25f,
                            steps = 3
                        )
                    }
                }

                // B. Language
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_language),
                        icon = Icons.Default.Language
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = settings.languageCode == LocaleManager.LANG_ENGLISH,
                                onClick = { viewModel.setLanguage(LocaleManager.LANG_ENGLISH) },
                                label = { Text(stringResource(R.string.lang_english)) },
                                modifier = Modifier.testTag("settings_lang_english")
                            )
                            FilterChip(
                                selected = settings.languageCode == LocaleManager.LANG_BANGLA,
                                onClick = { viewModel.setLanguage(LocaleManager.LANG_BANGLA) },
                                label = { Text(stringResource(R.string.lang_bangla)) },
                                modifier = Modifier.testTag("settings_lang_bangla")
                            )
                        }
                    }
                }

                // C. Notifications & Ringtone (Centralized Master Toggle, Custom Ringtone Upload & Global Controls)
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_notifications_ringtone),
                        icon = Icons.Default.Notifications
                    ) {
                        // Master MYCARE Notification ON/OFF Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.notif_master_label),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.notif_master_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                modifier = Modifier.testTag("master_notifications_switch")
                            )
                        }

                        if (settings.notificationsEnabled && !PermissionManager.hasNotificationPermission(context)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.reminder_permission_needed),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { PermissionManager.openNotificationSettings(context) }) {
                                        Text(stringResource(R.string.action_open_settings))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Centralized Global Ringtone Management
                        Text(
                            text = stringResource(R.string.reminder_ringtone),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayedRingtoneName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("selected_ringtone_name")
                                    )
                                }
                                if (previewSourceUri != null) {
                                    val isPlaying = playState.isPlaying && playState.activeSource == previewSourceUri
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) viewModel.audioPlayer.stop()
                                            else viewModel.audioPlayer.playUri(previewSourceUri)
                                        },
                                        modifier = Modifier.testTag("preview_ringtone_btn")
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = stringResource(
                                                if (isPlaying) R.string.ringtone_stop_preview else R.string.ringtone_preview
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ringtonePicker.launch(
                                        arrayOf(
                                            "audio/*",
                                            "audio/mpeg",
                                            "audio/wav",
                                            "audio/mp4",
                                            "audio/x-m4a",
                                            "audio/ogg"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browse_ringtone_btn")
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        if (hasValidCustomRingtone) R.string.ringtone_replace else R.string.ringtone_browse
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (hasValidCustomRingtone) {
                                OutlinedButton(
                                    onClick = { viewModel.clearDefaultRingtone() },
                                    modifier = Modifier.testTag("remove_ringtone_btn")
                                ) {
                                    Text(stringResource(R.string.ringtone_remove_custom))
                                }
                            }
                        }

                        Text(
                            text = stringResource(R.string.ringtone_fallback_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(stringResource(R.string.reminder_duration), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                10 to R.string.duration_10s,
                                30 to R.string.duration_30s,
                                60 to R.string.duration_1m,
                                120 to R.string.duration_2m,
                                300 to R.string.duration_5m
                            ).forEach { (sec, labelRes) ->
                                FilterChip(
                                    selected = settings.defaultRingingDurationSec == sec,
                                    onClick = { viewModel.setDefaultRingingDuration(sec) },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }

                        Text(stringResource(R.string.reminder_snooze), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                5 to R.string.snooze_5m,
                                10 to R.string.snooze_10m,
                                15 to R.string.snooze_15m
                            ).forEach { (min, labelRes) ->
                                FilterChip(
                                    selected = settings.defaultSnoozeMinutes == min,
                                    onClick = { viewModel.setDefaultSnooze(min) },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.reminder_vibration), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = settings.defaultVibration,
                                onCheckedChange = { viewModel.setDefaultVibration(it) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = stringResource(R.string.set_notifications),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val toggles = listOf(
                            Triple("MEDICINE", R.string.feature_medicine_title, settings.notifMedicine),
                            Triple("EXERCISE", R.string.feature_exercise_title, settings.notifExercise),
                            Triple("TASK", R.string.feature_task_title, settings.notifTasks),
                            Triple("GROCERY", R.string.feature_grocery_title, settings.notifGrocery),
                            Triple("APPOINTMENT", R.string.notif_appointments, settings.notifAppointments),
                            Triple("NUTRITION", R.string.feature_nutrition_title, settings.notifNutrition),
                            Triple("TRAVEL", R.string.feature_travel_title, settings.notifTravel),
                            Triple("GENERAL", R.string.notif_general, settings.notifGeneral)
                        )
                        toggles.forEachIndexed { idx, (key, labelRes, checked) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = checked && settings.notificationsEnabled,
                                    enabled = settings.notificationsEnabled,
                                    onCheckedChange = { viewModel.setFeatureNotification(key, it) }
                                )
                            }
                            if (idx < toggles.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }

                // D. Storage Management
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_storage),
                        icon = Icons.Default.Storage
                    ) {
                        StorageRow(
                            label = stringResource(R.string.storage_database),
                            sizeText = viewModel.fileStorage.formatBytes(storage.databaseBytes),
                            onClear = null
                        )
                        StorageRow(
                            label = stringResource(R.string.storage_photos),
                            sizeText = viewModel.fileStorage.formatBytes(storage.photosBytes),
                            onClear = if (storage.photosBytes > 0L) ({ clearCategoryTarget = "PHOTOS" }) else null
                        )
                        StorageRow(
                            label = stringResource(R.string.storage_videos),
                            sizeText = viewModel.fileStorage.formatBytes(storage.videosBytes),
                            onClear = if (storage.videosBytes > 0L) ({ clearCategoryTarget = "VIDEOS" }) else null
                        )
                        StorageRow(
                            label = stringResource(R.string.storage_documents),
                            sizeText = viewModel.fileStorage.formatBytes(storage.documentsBytes),
                            onClear = if (storage.documentsBytes > 0L) ({ clearCategoryTarget = "DOCUMENTS" }) else null
                        )
                        StorageRow(
                            label = stringResource(R.string.storage_voice),
                            sizeText = viewModel.fileStorage.formatBytes(storage.voiceBytes),
                            onClear = if (storage.voiceBytes > 0L) ({ clearCategoryTarget = "VOICE" }) else null
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.storage_total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                viewModel.fileStorage.formatBytes(storage.totalBytes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // E. Backup & Restore
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_backup),
                        icon = Icons.Default.Backup
                    ) {
                        if (isBusy) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.set_backup), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    backupExportLauncher.launch("mycare_backup_$stamp.zip")
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_export_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.backup_export))
                            }
                            OutlinedButton(
                                onClick = {
                                    backupRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("backup_restore_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.backup_restore))
                            }
                        }
                    }
                }

                // F. Permissions Overview (including Exact Alarms & Battery Optimization)
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_permissions),
                        icon = Icons.Default.Security
                    ) {
                        PermissionRow(
                            label = stringResource(R.string.perm_notifications),
                            status = stringResource(
                                if (PermissionManager.hasNotificationPermission(context)) R.string.perm_granted
                                else R.string.perm_not_granted
                            ),
                            isGranted = PermissionManager.hasNotificationPermission(context),
                            onSettingsClick = { PermissionManager.openNotificationSettings(context) }
                        )
                        PermissionRow(
                            label = stringResource(R.string.perm_exact_alarm),
                            status = stringResource(
                                if (PermissionManager.canScheduleExactAlarms(context)) R.string.perm_granted
                                else R.string.perm_not_granted
                            ),
                            isGranted = PermissionManager.canScheduleExactAlarms(context),
                            onSettingsClick = { PermissionManager.openExactAlarmSettings(context) }
                        )
                        val batteryUnrestricted = PermissionManager.isIgnoringBatteryOptimizations(context)
                        PermissionRow(
                            label = stringResource(R.string.perm_battery_opt),
                            status = stringResource(
                                if (batteryUnrestricted) R.string.perm_battery_unrestricted
                                else R.string.perm_battery_optimized
                            ),
                            isGranted = batteryUnrestricted,
                            onSettingsClick = { PermissionManager.openBatteryOptimizationSettings(context) }
                        )
                        PermissionRow(
                            label = stringResource(R.string.perm_location),
                            status = stringResource(
                                if (PermissionManager.hasLocationPermission(context)) R.string.perm_granted
                                else R.string.perm_not_granted
                            ),
                            isGranted = PermissionManager.hasLocationPermission(context),
                            onSettingsClick = { PermissionManager.openAppSettings(context) }
                        )
                        PermissionRow(
                            label = stringResource(R.string.perm_microphone),
                            status = stringResource(
                                if (PermissionManager.hasMicrophonePermission(context)) R.string.perm_granted
                                else R.string.perm_not_granted
                            ),
                            isGranted = PermissionManager.hasMicrophonePermission(context),
                            onSettingsClick = { PermissionManager.openAppSettings(context) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.perm_media), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.perm_system_picker),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // G. About MYCARE
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.set_about),
                        icon = Icons.Default.Info
                    ) {
                        Text(
                            text = "${stringResource(R.string.app_name)} — ${stringResource(R.string.about_version)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.about_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.developer_credit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("developer_credit_text")
                        )

                        val privacyTitle = stringResource(R.string.about_privacy)
                        val privacyBody = stringResource(R.string.about_privacy_text)
                        val termsTitle = stringResource(R.string.about_terms)
                        val termsBody = stringResource(R.string.about_terms_text)
                        val helpTitle = stringResource(R.string.about_help)
                        val helpBody = stringResource(R.string.about_help_text)

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { infoDialogContent = privacyTitle to privacyBody }) {
                                Text(privacyTitle)
                            }
                            OutlinedButton(onClick = { infoDialogContent = termsTitle to termsBody }) {
                                Text(termsTitle)
                            }
                            OutlinedButton(onClick = { infoDialogContent = helpTitle to helpBody }) {
                                Text(helpTitle)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    clearCategoryTarget?.let { cat ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.clearCategoryStorage(cat)
                clearCategoryTarget = null
            },
            onDismiss = { clearCategoryTarget = null }
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text(stringResource(R.string.backup_restore)) },
            text = { Text(stringResource(R.string.backup_confirm_restore)) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.restoreBackup(uri) { ok ->
                            scope.launch {
                                snackbarHostState.showSnackbar(if (ok) restoreSuccessMsg else backupInvalidMsg)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    infoDialogContent?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { infoDialogContent = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = { infoDialogContent = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun ThemePreviewCard(
    spec: ThemePreviewSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = spec.theme.previewBg,
        border = BorderStroke(
            width = if (selected) 2.5.dp else 1.dp,
            color = if (selected) spec.theme.previewAccent else spec.theme.previewAccent.copy(alpha = 0.3f)
        ),
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mini Screen Preview (Background -> Card Surface -> Nav Bar -> Accent)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(spec.theme.previewAccent)
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(spec.theme.previewSurface)
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(spec.theme.previewNavBar)
                    )
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(spec.theme.previewAccent.copy(alpha = 0.35f))
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = spec.theme.previewAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = spec.theme.previewSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(spec.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2026),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(spec.descRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF454E5B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun StorageRow(
    label: String,
    sizeText: String,
    onClear: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(sizeText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (onClear != null) {
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    status: String,
    isGranted: Boolean,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        TextButton(onClick = onSettingsClick) {
            Text(stringResource(R.string.action_open_settings))
        }
    }
}
