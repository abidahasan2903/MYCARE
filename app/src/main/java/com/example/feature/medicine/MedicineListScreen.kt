package com.example.feature.medicine

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entity.MedicineEntity
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.formatDateShort
import com.example.core.ui.components.formatDateTimeShort
import com.example.core.ui.components.formatTime12h
import com.example.core.ui.components.startOfDayMillis

@Composable
fun MedicineListScreen(
    viewModel: MedicineViewModel,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<MedicineEntity?>(null) }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_medicine_title),
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    modifier = Modifier.testTag("fab_add_medicine")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.med_add))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.med_tab_list)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.med_tab_schedule)) }
                )
            }

            if (selectedTab == 0) {
                MedicineListTab(
                    state = state,
                    onSearchChange = viewModel::setSearch,
                    onFilterChange = viewModel::setFilterTab,
                    onOpenDetail = onOpenDetail,
                    onDeleteRequest = { medicineToDelete = it }
                )
            } else {
                MedicineScheduleTrackingTab(
                    state = state,
                    viewModel = viewModel,
                    onOpenDetail = onOpenDetail
                )
            }
        }
    }

    if (showAddSheet) {
        MedicineEditorSheet(
            existing = null,
            onDismiss = { showAddSheet = false },
            onSave = { name, strength, type, dosage, start, end, doc, notes, img, docUri ->
                viewModel.saveMedicine(
                    existing = null,
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
                ) { newId ->
                    showAddSheet = false
                    onOpenDetail(newId)
                }
            }
        )
    }

    medicineToDelete?.let { med ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteMedicine(med)
                medicineToDelete = null
            },
            onDismiss = { medicineToDelete = null }
        )
    }
}

@Composable
private fun MedicineListTab(
    state: MedicineUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onDeleteRequest: (MedicineEntity) -> Unit
) {
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
                    text = stringResource(R.string.med_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        item {
            MyCareSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = stringResource(R.string.action_search)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ACTIVE" to R.string.label_active,
                    "COMPLETED" to R.string.label_completed,
                    "ALL" to R.string.label_all
                ).forEach { (key, res) ->
                    FilterChip(
                        selected = state.filterTab == key,
                        onClick = { onFilterChange(key) },
                        label = { Text(stringResource(res)) }
                    )
                }
            }
        }

        if (state.medicines.isEmpty()) {
            item {
                MyCareEmptyState(
                    icon = Icons.Default.Medication,
                    message = stringResource(R.string.med_empty)
                )
            }
        } else {
            items(state.medicines, key = { it.id }) { med ->
                ElevatedCard(
                    onClick = { onOpenDetail(med.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${med.name} ${med.strength}".trim(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = med.medicineType +
                                    if (med.prescribedBy.isNotBlank()) " • ${med.prescribedBy}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (med.dosageInstructions.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = med.dosageInstructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteRequest(med) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineScheduleTrackingTab(
    state: MedicineUiState,
    viewModel: MedicineViewModel,
    onOpenDetail: (Long) -> Unit
) {
    val medMap = remember(state.medicines) { state.medicines.associateBy { it.id } }
    val todayStart = remember { startOfDayMillis() }
    val weekStart = remember(todayStart) {
        java.util.Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(java.util.Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
    }

    val todayLogs = remember(state.doseLogs) { state.doseLogs.filter { it.scheduledMillis >= todayStart } }
    val weekLogs = remember(state.doseLogs) { state.doseLogs.filter { it.scheduledMillis >= weekStart } }

    val todayTaken = todayLogs.count { it.status == "TAKEN" }
    val todayTotal = state.activeSchedules.size.coerceAtLeast(todayLogs.size)
    val weekTaken = weekLogs.count { it.status == "TAKEN" }
    val weekTotal = weekLogs.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.med_adherence_today),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val todayPct = if (todayTotal > 0) todayTaken.toFloat() / todayTotal else 0f
                    LinearProgressIndicator(
                        progress = { todayPct.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("$todayTaken / $todayTotal (${(todayPct * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.med_adherence_weekly),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val weekPct = if (weekTotal > 0) weekTaken.toFloat() / weekTotal else 0f
                    LinearProgressIndicator(
                        progress = { weekPct.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("$weekTaken / $weekTotal (${(weekPct * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (state.activeSchedules.isEmpty()) {
            item {
                MyCareEmptyState(
                    icon = Icons.Default.Medication,
                    message = stringResource(R.string.dash_no_upcoming_medicine)
                )
            }
        } else {
            items(state.activeSchedules, key = { it.id }) { sched ->
                val med = medMap[sched.medicineId]
                val medName = med?.let { "${it.name} ${it.strength}".trim() } ?: "#${sched.medicineId}"
                val latestTodayLog = todayLogs.firstOrNull { it.scheduleId == sched.id }

                ElevatedCard(
                    onClick = { onOpenDetail(sched.medicineId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(medName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatTime12h(sched.hour, sched.minute)} • ${sched.slotType} • ${sched.mealTiming}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        when (latestTodayLog?.status) {
                                            "TAKEN" -> stringResource(R.string.med_status_taken)
                                            "SKIPPED" -> stringResource(R.string.med_status_skipped)
                                            "SNOOZED" -> stringResource(R.string.med_status_snoozed)
                                            else -> stringResource(R.string.med_status_pending)
                                        }
                                    )
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.logDose(sched.medicineId, sched, medName, "TAKEN") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.med_status_taken), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { viewModel.logDose(sched.medicineId, sched, medName, "SNOOZED") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.med_status_snoozed), maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { viewModel.logDose(sched.medicineId, sched, medName, "SKIPPED") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.med_status_skipped), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        if (state.doseLogs.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.label_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            items(state.doseLogs.take(25), key = { it.id }) { log ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.medicineName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                formatDateTimeShort(log.actionTimestampMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = log.status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicineEditorSheet(
    existing: MedicineEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        strength: String,
        type: String,
        dosage: String,
        startDate: Long,
        endDate: Long?,
        prescribedBy: String,
        notes: String,
        imageUri: Uri?,
        docUri: Uri?
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var strength by remember { mutableStateOf(existing?.strength ?: "") }
    var type by remember { mutableStateOf(existing?.medicineType ?: "Tablet") }
    var dosage by remember { mutableStateOf(existing?.dosageInstructions ?: "") }
    var startDate by remember { mutableLongStateOf(existing?.startDateMillis ?: System.currentTimeMillis()) }
    var hasEndDate by remember { mutableStateOf(existing?.endDateMillis != null) }
    var endDate by remember { mutableLongStateOf(existing?.endDateMillis ?: (System.currentTimeMillis() + 7 * 86_400_000L)) }
    var prescribedBy by remember { mutableStateOf(existing?.prescribedBy ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var pickedImage by remember { mutableStateOf<Uri?>(null) }
    var pickedDoc by remember { mutableStateOf<Uri?>(null) }
    var showError by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedImage = uri
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pickedDoc = uri
    }

    val medTypes = listOf("Tablet", "Capsule", "Syrup", "Injection", "Drops", "Inhaler", "Other")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(if (existing == null) R.string.med_add else R.string.med_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text(stringResource(R.string.med_name) + " *") },
                singleLine = true,
                isError = showError && name.isBlank(),
                modifier = Modifier.fillMaxWidth().testTag("med_name_input")
            )

            OutlinedTextField(
                value = strength,
                onValueChange = { strength = it },
                label = { Text(stringResource(R.string.med_strength)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.med_type), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                medTypes.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t) }
                    )
                }
            }

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text(stringResource(R.string.med_dosage)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = prescribedBy,
                onValueChange = { prescribedBy = it },
                label = { Text(stringResource(R.string.med_prescribed_by)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            DatePickerField(
                label = stringResource(R.string.label_start_date),
                dateMillis = startDate,
                onDateSelected = { startDate = it },
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                Text(stringResource(R.string.label_end_date))
            }
            if (hasEndDate) {
                DatePickerField(
                    label = stringResource(R.string.label_end_date),
                    dateMillis = endDate,
                    onDateSelected = { endDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedImage != null) "Selected" else stringResource(R.string.label_add_photo), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { docPicker.launch(arrayOf("application/pdf", "image/*", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedDoc != null) "Selected" else stringResource(R.string.label_add_document), maxLines = 1)
                }
            }

            if (showError) {
                Text(
                    stringResource(R.string.label_required_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            showError = true
                        } else {
                            onSave(
                                name,
                                strength,
                                type,
                                dosage,
                                startDate,
                                if (hasEndDate) endDate else null,
                                prescribedBy,
                                notes,
                                pickedImage,
                                pickedDoc
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("save_medicine_btn")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
