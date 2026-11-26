package com.example.feature.nutrition

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.R
import com.example.core.database.entity.FoodChartFileEntity
import com.example.core.database.entity.NutritionEntryEntity
import com.example.core.database.entity.ReminderEntity
import com.example.core.datastore.AppSettingsDataStore
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.storage.FileStorageManager
import com.example.core.ui.components.BarChartEntry
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.SimpleBarChart
import com.example.core.ui.components.TimePickerField
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.core.ui.components.formatDateShort
import com.example.core.ui.components.formatTime12h
import com.example.core.ui.components.startOfDayMillis
import com.example.data.repository.NutritionRepository
import com.example.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NutritionUiState(
    val entries: List<NutritionEntryEntity> = emptyList(),
    val chartFiles: List<FoodChartFileEntity> = emptyList()
)

class NutritionViewModel(
    private val nutritionRepo: NutritionRepository,
    private val reminderRepo: ReminderRepository,
    val fileStorage: FileStorageManager,
    private val settingsStore: AppSettingsDataStore
) : ViewModel() {

    val uiState: StateFlow<NutritionUiState> = combine(
        nutritionRepo.allEntries,
        nutritionRepo.allChartFiles
    ) { entries, files ->
        NutritionUiState(entries, files)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionUiState())

    suspend fun getDefaultReminderDraft(): ReminderDraft {
        val s = settingsStore.settingsFlow.first()
        return ReminderDraft(
            enabled = false,
            ringtoneUri = s.defaultRingtoneUri,
            ringingDurationSec = s.defaultRingingDurationSec,
            snoozeMinutes = s.defaultSnoozeMinutes,
            vibrationEnabled = s.defaultVibration
        )
    }

    suspend fun loadReminder(reminderId: Long?): ReminderDraft {
        val base = getDefaultReminderDraft()
        if (reminderId == null) return base
        val r = reminderRepo.getReminderById(reminderId) ?: return base
        val cal = Calendar.getInstance().apply { timeInMillis = r.triggerTimeMillis }
        return ReminderDraft(
            enabled = r.isEnabled,
            dateMillis = r.triggerTimeMillis,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            repeatType = r.repeatType,
            repeatIntervalDays = r.repeatIntervalDays,
            weekdays = r.weekdaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
            soundEnabled = r.soundEnabled,
            vibrationEnabled = r.vibrationEnabled,
            notificationEnabled = r.notificationEnabled,
            ringtoneUri = r.ringtoneUri,
            ringingDurationSec = r.ringingDurationSec,
            snoozeMinutes = r.snoozeMinutes
        )
    }

    fun saveEntry(
        existing: NutritionEntryEntity?,
        foodName: String,
        qty: Double,
        unit: String,
        calories: Int,
        category: String,
        dateMillis: Long,
        hour: Int,
        minute: Int,
        notes: String,
        reminderDraft: ReminderDraft,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val triggerMillis = nextFutureTriggerMillis(
                dateMillis = dateMillis,
                hour = reminderDraft.hour,
                minute = reminderDraft.minute,
                repeatType = reminderDraft.repeatType
            )
            val reminder = if (reminderDraft.enabled) {
                ReminderEntity(
                    id = existing?.reminderId ?: 0L,
                    title = "$category: $foodName",
                    description = "$qty $unit ($calories kcal)",
                    featureType = "NUTRITION",
                    linkedEntityId = existing?.id,
                    triggerTimeMillis = triggerMillis,
                    repeatType = reminderDraft.repeatType,
                    repeatIntervalDays = reminderDraft.repeatIntervalDays,
                    weekdaysCsv = reminderDraft.weekdays.joinToString(","),
                    isEnabled = true,
                    soundEnabled = true,
                    vibrationEnabled = reminderDraft.vibrationEnabled,
                    notificationEnabled = true,
                    ringtoneUri = null,
                    ringingDurationSec = reminderDraft.ringingDurationSec,
                    snoozeMinutes = reminderDraft.snoozeMinutes
                )
            } else null

            val entry = (existing ?: NutritionEntryEntity(foodName = foodName)).copy(
                foodName = foodName.trim(),
                quantity = qty,
                unit = unit.trim().ifBlank { "serving" },
                calories = calories.coerceAtLeast(0),
                mealCategory = category,
                mealDateMillis = startOfDayMillis(dateMillis),
                mealHour = hour,
                mealMinute = minute,
                notes = notes.trim()
            )
            val (_, exactOk) = nutritionRepo.saveEntry(entry, reminder)
            onDone(exactOk)
        }
    }

    fun toggleCompleted(entry: NutritionEntryEntity) {
        viewModelScope.launch { nutritionRepo.toggleCompleted(entry) }
    }

    fun deleteEntry(entry: NutritionEntryEntity) {
        viewModelScope.launch { nutritionRepo.deleteEntry(entry) }
    }

    fun uploadChartFile(uri: android.net.Uri, onError: () -> Unit = {}) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri).onSuccess { imported ->
                nutritionRepo.addChartFile(
                    FoodChartFileEntity(
                        title = imported.displayName,
                        mediaType = imported.mediaType,
                        filePath = imported.filePath,
                        mimeType = imported.mimeType
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun deleteChartFile(file: FoodChartFileEntity) {
        viewModelScope.launch { nutritionRepo.deleteChartFile(file) }
    }
}

@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    audioPlayer: AudioPlaybackManager,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Meal Schedule, 1 = Food Charts
    var historyRange by remember { mutableStateOf("DAILY") } // DAILY, WEEKLY
    var showAddSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<NutritionEntryEntity?>(null) }
    var entryToDelete by remember { mutableStateOf<NutritionEntryEntity?>(null) }
    var chartToDelete by remember { mutableStateOf<FoodChartFileEntity?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.uploadChartFile(uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.uploadChartFile(uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }

    val todayStart = remember { startOfDayMillis() }
    val tomorrowStart = remember(todayStart) {
        Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
    val todayEntries = remember(state.entries, todayStart, tomorrowStart) {
        state.entries.filter { it.mealDateMillis in todayStart until tomorrowStart }
    }
    val todayCalories = todayEntries.sumOf { it.calories }
    val weeklyChartEntries = remember(state.entries) {
        val now = Calendar.getInstance()
        (6 downTo 0).map { offset ->
            val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val start = startOfDayMillis(cal.timeInMillis)
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            val calSum = state.entries.filter { it.mealDateMillis in start until end }.sumOf { it.calories }
            val label = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(start))
            BarChartEntry(label, calSum.toFloat())
        }
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_nutrition_title),
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { editingEntry = null; showAddSheet = true },
                    modifier = Modifier.testTag("fab_add_nutrition")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nut_add_entry))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.nut_tab_meals)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.nut_tab_charts)) })
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.nut_user_entered_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.nut_total_calories), style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "$todayCalories kcal",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = historyRange == "DAILY", onClick = { historyRange = "DAILY" }, label = { Text("Today") })
                                    FilterChip(selected = historyRange == "WEEKLY", onClick = { historyRange = "WEEKLY" }, label = { Text("Weekly Chart") })
                                }
                                if (historyRange == "WEEKLY") {
                                    SimpleBarChart(entries = weeklyChartEntries)
                                }
                            }
                        }
                    }

                    val shownEntries = if (historyRange == "DAILY") todayEntries else state.entries
                    if (shownEntries.isEmpty()) {
                        item {
                            MyCareEmptyState(icon = Icons.Default.Restaurant, message = stringResource(R.string.nut_empty_meals))
                        }
                    } else {
                        items(shownEntries, key = { it.id }) { entry ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = entry.isCompleted,
                                        onCheckedChange = { viewModel.toggleCompleted(entry) }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.foodName} (${entry.quantity} ${entry.unit})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else null
                                        )
                                        Text(
                                            text = "${entry.mealCategory} • ${formatDateShort(entry.mealDateMillis)} ${formatTime12h(entry.mealHour, entry.mealMinute)} • ${entry.calories} kcal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { editingEntry = entry; showAddSheet = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                                    }
                                    IconButton(onClick = { entryToDelete = entry }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.label_add_photo), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { docPicker.launch(arrayOf("application/pdf", "image/*", "*/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.nut_upload_chart), maxLines = 1)
                            }
                        }
                    }

                    if (state.chartFiles.isEmpty()) {
                        item {
                            MyCareEmptyState(icon = Icons.Default.Restaurant, message = stringResource(R.string.nut_empty_charts))
                        }
                    } else {
                        items(state.chartFiles, key = { it.id }) { cf ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenMedia(cf.title, cf.filePath, cf.mediaType, cf.mimeType) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (cf.mediaType == "IMAGE" && viewModel.fileStorage.fileExists(cf.filePath)) {
                                        AsyncImage(
                                            model = File(cf.filePath),
                                            contentDescription = cf.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Icon(
                                            if (cf.mediaType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(cf.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { chartToDelete = cf }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        NutritionEntrySheet(
            existing = editingEntry,
            viewModel = viewModel,
            audioPlayer = audioPlayer,
            onDismiss = { showAddSheet = false },
            onSaved = { reminderEnabled, exactOk ->
                showAddSheet = false
                if (reminderEnabled && !exactOk) {
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.reminder_schedule_failed),
                            actionLabel = context.getString(R.string.action_open_settings),
                            duration = SnackbarDuration.Long
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            PermissionManager.openExactAlarmSettings(context)
                        }
                    }
                }
            }
        )
    }

    entryToDelete?.let { e ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteEntry(e); entryToDelete = null },
            onDismiss = { entryToDelete = null }
        )
    }

    chartToDelete?.let { cf ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteChartFile(cf); chartToDelete = null },
            onDismiss = { chartToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NutritionEntrySheet(
    existing: NutritionEntryEntity?,
    viewModel: NutritionViewModel,
    audioPlayer: AudioPlaybackManager,
    onDismiss: () -> Unit,
    onSaved: (reminderEnabled: Boolean, exactOk: Boolean) -> Unit
) {
    var foodName by remember { mutableStateOf(existing?.foodName ?: "") }
    var qtyStr by remember { mutableStateOf((existing?.quantity ?: 1.0).toString()) }
    var unit by remember { mutableStateOf(existing?.unit ?: "serving") }
    var calStr by remember { mutableStateOf((existing?.calories ?: 0).takeIf { it > 0 }?.toString() ?: "") }
    var category by remember { mutableStateOf(existing?.mealCategory ?: "Breakfast") }
    var dateMillis by remember { mutableLongStateOf(existing?.mealDateMillis ?: System.currentTimeMillis()) }
    var hour by remember { mutableIntStateOf(existing?.mealHour ?: 8) }
    var minute by remember { mutableIntStateOf(existing?.mealMinute ?: 0) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var reminderDraft by remember { mutableStateOf(ReminderDraft()) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        reminderDraft = if (existing != null) viewModel.loadReminder(existing.reminderId)
        else viewModel.getDefaultReminderDraft()
    }

    val mealCategories = listOf("Breakfast", "Mid-morning", "Lunch", "Afternoon Snack", "Dinner", "Before Sleep", "Custom Meal")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.nut_add_entry), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it; showError = false },
                label = { Text(stringResource(R.string.nut_food_name) + " *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text(stringResource(R.string.grocery_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.grocery_unit)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = calStr,
                onValueChange = { calStr = it.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.nut_calories)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.nut_meal_cat), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mealCategories.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = stringResource(R.string.label_date),
                    dateMillis = dateMillis,
                    onDateSelected = { dateMillis = it; reminderDraft = reminderDraft.copy(dateMillis = it) },
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = stringResource(R.string.label_time),
                    hour = hour,
                    minute = minute,
                    onTimeSelected = { h, m -> hour = h; minute = m; reminderDraft = reminderDraft.copy(hour = h, minute = m) },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.label_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())

            ReminderConfigSection(
                draft = reminderDraft,
                onDraftChange = { reminderDraft = it },
                fileStorage = viewModel.fileStorage,
                audioPlayer = audioPlayer
            )

            if (showError) Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = {
                    if (foodName.isBlank()) showError = true
                    else viewModel.saveEntry(
                        existing = existing,
                        foodName = foodName,
                        qty = qtyStr.toDoubleOrNull() ?: 1.0,
                        unit = unit,
                        calories = calStr.toIntOrNull() ?: 0,
                        category = category,
                        dateMillis = dateMillis,
                        hour = hour,
                        minute = minute,
                        notes = notes,
                        reminderDraft = reminderDraft,
                        onDone = { exactOk -> onSaved(reminderDraft.enabled, exactOk) }
                    )
                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
