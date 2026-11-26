package com.example.feature.medicine

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.database.entity.MedicineEntity
import com.example.core.database.entity.MedicineScheduleEntity
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.TimePickerField
import com.example.core.ui.components.formatDateShort
import com.example.core.ui.components.formatTime12h
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MedicineDetailScreen(
    medicineId: Long,
    viewModel: MedicineViewModel,
    audioPlayer: AudioPlaybackManager,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val medicine by viewModel.observeMedicine(medicineId).collectAsStateWithLifecycle(initialValue = null)
    val schedules by viewModel.observeSchedules(medicineId).collectAsStateWithLifecycle(initialValue = emptyList())
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val reminderMap = remember(allReminders) { allReminders.associateBy { it.id } }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showEditSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<MedicineScheduleEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = medicine?.name ?: stringResource(R.string.feature_medicine_title),
                onBack = onBack,
                actions = {
                    medicine?.let { med ->
                        IconButton(onClick = { viewModel.toggleCompleted(med) }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.label_completed),
                                tint = if (med.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (medicine != null) {
                FloatingActionButton(onClick = {
                    editingSchedule = null
                    showScheduleSheet = true
                }) {
                    Icon(Icons.Default.AddAlarm, contentDescription = stringResource(R.string.med_add_schedule))
                }
            }
        }
    ) { padding ->
        val med = medicine
        if (med == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.med_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${med.name} ${med.strength}".trim(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${med.medicineType} • ${formatDateShort(med.startDateMillis)}" +
                                    (med.endDateMillis?.let { " → ${formatDateShort(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (med.prescribedBy.isNotBlank()) {
                                Text("${stringResource(R.string.med_prescribed_by)}: ${med.prescribedBy}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (med.dosageInstructions.isNotBlank()) {
                                Text(med.dosageInstructions, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (med.notes.isNotBlank()) {
                                Text(med.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            med.imagePath?.let { path ->
                                if (viewModel.fileStorage.fileExists(path)) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = med.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpenMedia(med.name, path, "IMAGE", "image/*") }
                                    )
                                }
                            }
                            med.documentPath?.let { docPath ->
                                OutlinedButton(onClick = {
                                    val mType = viewModel.fileStorage.classifyMediaType("", docPath)
                                    onOpenMedia(med.documentName ?: med.name, docPath, mType, "*/*")
                                }) {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(med.documentName ?: stringResource(R.string.label_add_document))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.med_tab_schedule),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(schedules, key = { it.id }) { sched ->
                    val linkedReminder = sched.reminderId?.let { reminderMap[it] }
                    ElevatedCard(
                        onClick = {
                            editingSchedule = sched
                            showScheduleSheet = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = formatTime12h(sched.hour, sched.minute),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${sched.slotType} • ${sched.mealTiming} • ${sched.repeatType}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (linkedReminder != null) {
                                    Text(
                                        text = stringResource(
                                            if (linkedReminder.isEnabled) R.string.reminder_status_on else R.string.reminder_status_off
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (linkedReminder.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteSchedule(sched) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    medicine?.let { med ->
        if (showEditSheet) {
            MedicineEditorSheet(
                existing = med,
                onDismiss = { showEditSheet = false },
                onSave = { name, strength, type, dosage, start, end, doc, notes, img, docUri ->
                    viewModel.saveMedicine(
                        existing = med,
                        name = name,
                        strength = strength,
                        type = type,
                        dosage = dosage,
                        startDate = start,
                        endDate = end,
                        prescribedBy = doc,
                        notes = notes,
                        imageUri = img,
                        docUri = docUri
                    ) { showEditSheet = false }
                }
            )
        }

        if (showScheduleSheet) {
            MedicineScheduleSheet(
                medicine = med,
                existing = editingSchedule,
                viewModel = viewModel,
                audioPlayer = audioPlayer,
                onDismiss = { showScheduleSheet = false },
                onSave = { slot, meal, h, m, draft ->
                    viewModel.saveSchedule(
                        medicine = med,
                        existingSchedule = editingSchedule,
                        slotType = slot,
                        mealTiming = meal,
                        hour = h,
                        minute = m,
                        startDateMillis = med.startDateMillis,
                        endDateMillis = med.endDateMillis,
                        reminderDraft = draft
                    ) { exactOk ->
                        showScheduleSheet = false
                        if (draft.enabled && !exactOk) {
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
                }
            )
        }

        if (showDeleteConfirm) {
            ConfirmDeleteDialog(
                onConfirm = {
                    viewModel.deleteMedicine(med) {
                        showDeleteConfirm = false
                        onBack()
                    }
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MedicineScheduleSheet(
    medicine: MedicineEntity,
    existing: MedicineScheduleEntity?,
    viewModel: MedicineViewModel,
    audioPlayer: AudioPlaybackManager,
    onDismiss: () -> Unit,
    onSave: (slot: String, meal: String, hour: Int, minute: Int, draft: ReminderDraft) -> Unit
) {
    var slotType by remember { mutableStateOf(existing?.slotType ?: "MORNING") }
    var mealTiming by remember { mutableStateOf(existing?.mealTiming ?: "AFTER_MEAL") }
    var hour by remember { mutableIntStateOf(existing?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(existing?.minute ?: 0) }
    var reminderDraft by remember { mutableStateOf(ReminderDraft(enabled = true, repeatType = "DAILY", hour = 8, minute = 0)) }

    LaunchedEffect(existing) {
        reminderDraft = if (existing != null) {
            viewModel.loadReminderForSchedule(existing.reminderId)
        } else {
            viewModel.getDefaultReminderDraft().copy(repeatType = "DAILY", hour = hour, minute = minute)
        }
    }

    val slots = listOf(
        "MORNING" to (8 to 0),
        "AFTERNOON" to (13 to 0),
        "EVENING" to (18 to 0),
        "NIGHT" to (21 to 30),
        "CUSTOM" to (hour to minute)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.med_add_schedule), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(medicine.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { (slotKey, defaultTime) ->
                    FilterChip(
                        selected = slotType == slotKey,
                        onClick = {
                            slotType = slotKey
                            if (slotKey != "CUSTOM") {
                                hour = defaultTime.first
                                minute = defaultTime.second
                                reminderDraft = reminderDraft.copy(hour = hour, minute = minute)
                            }
                        },
                        label = {
                            Text(
                                when (slotKey) {
                                    "MORNING" -> stringResource(R.string.med_slot_morning)
                                    "AFTERNOON" -> stringResource(R.string.med_slot_afternoon)
                                    "EVENING" -> stringResource(R.string.med_slot_evening)
                                    "NIGHT" -> stringResource(R.string.med_slot_night)
                                    else -> stringResource(R.string.med_slot_custom)
                                }
                            )
                        }
                    )
                }
            }

            TimePickerField(
                label = stringResource(R.string.label_time),
                hour = hour,
                minute = minute,
                onTimeSelected = { h, m ->
                    hour = h
                    minute = m
                    reminderDraft = reminderDraft.copy(hour = h, minute = m)
                },
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "BEFORE_MEAL" to R.string.med_meal_before,
                    "AFTER_MEAL" to R.string.med_meal_after,
                    "ANYTIME" to R.string.med_meal_any
                ).forEach { (key, res) ->
                    FilterChip(
                        selected = mealTiming == key,
                        onClick = { mealTiming = key },
                        label = { Text(stringResource(res)) }
                    )
                }
            }

            ReminderConfigSection(
                draft = reminderDraft,
                onDraftChange = {
                    reminderDraft = it
                    hour = it.hour
                    minute = it.minute
                },
                fileStorage = viewModel.fileStorage,
                audioPlayer = audioPlayer
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = { onSave(slotType, mealTiming, hour, minute, reminderDraft) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
