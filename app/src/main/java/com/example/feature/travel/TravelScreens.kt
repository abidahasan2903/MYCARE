package com.example.feature.travel

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TravelAttachmentEntity
import com.example.core.database.entity.TravelChecklistItemEntity
import com.example.core.database.entity.TravelExpenseEntity
import com.example.core.database.entity.TravelPlanEntity
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.storage.FileStorageManager
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.formatDateShort
import com.example.core.ui.components.nextFutureTriggerMillis
import com.example.core.ui.components.startOfDayMillis
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TravelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TravelViewModel(
    private val travelRepo: TravelRepository,
    private val reminderRepo: ReminderRepository,
    val fileStorage: FileStorageManager
) : ViewModel() {

    val allPlans: StateFlow<List<TravelPlanEntity>> = travelRepo.allPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observePlan(id: Long): Flow<TravelPlanEntity?> = travelRepo.observePlan(id)
    fun observeExpenses(travelId: Long): Flow<List<TravelExpenseEntity>> = travelRepo.observeExpenses(travelId)
    fun observeChecklist(travelId: Long): Flow<List<TravelChecklistItemEntity>> = travelRepo.observeChecklist(travelId)
    fun observeAttachments(travelId: Long): Flow<List<TravelAttachmentEntity>> = travelRepo.observeAttachments(travelId)

    fun savePlan(
        existing: TravelPlanEntity?,
        tripName: String,
        destination: String,
        origin: String,
        startDate: Long,
        endDate: Long,
        hotelInfo: String,
        contacts: String,
        budget: Double,
        places: String,
        notes: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val normalizedStart = startOfDayMillis(startDate)
            val normalizedEnd = startOfDayMillis(endDate).coerceAtLeast(normalizedStart)
            val safeBudget = if (budget.isFinite() && budget >= 0.0) budget else 0.0
            val plan = (existing ?: TravelPlanEntity(tripName = tripName, destination = destination)).copy(
                tripName = tripName.trim(),
                destination = destination.trim(),
                origin = origin.trim(),
                startDateMillis = normalizedStart,
                endDateMillis = normalizedEnd,
                hotelInfo = hotelInfo.trim(),
                contactNumbers = contacts.trim(),
                budget = safeBudget,
                placesToVisit = places.trim(),
                notes = notes.trim()
            )
            val id = travelRepo.savePlan(plan)
            onSaved(id)
        }
    }

    fun deletePlan(plan: TravelPlanEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            travelRepo.deletePlan(plan)
            onDone()
        }
    }

    fun addExpense(travelId: Long, title: String, amount: Double) {
        viewModelScope.launch {
            if (title.isNotBlank() && amount.isFinite() && amount > 0.0) {
                travelRepo.addExpense(TravelExpenseEntity(travelId = travelId, title = title.trim(), amount = amount))
            }
        }
    }

    fun deleteExpense(exp: TravelExpenseEntity) {
        viewModelScope.launch { travelRepo.deleteExpense(exp) }
    }

    fun addChecklistItem(travelId: Long, title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                travelRepo.saveChecklistItem(TravelChecklistItemEntity(travelId = travelId, itemTitle = title.trim()))
            }
        }
    }

    fun toggleChecklist(item: TravelChecklistItemEntity) {
        viewModelScope.launch { travelRepo.saveChecklistItem(item.copy(isChecked = !item.isChecked)) }
    }

    fun deleteChecklistItem(item: TravelChecklistItemEntity) {
        viewModelScope.launch { travelRepo.deleteChecklistItem(item) }
    }

    fun addAttachment(travelId: Long, uri: Uri, onError: () -> Unit = {}) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri).onSuccess { imported ->
                travelRepo.addAttachment(
                    TravelAttachmentEntity(
                        travelId = travelId,
                        title = imported.displayName,
                        mediaType = imported.mediaType,
                        filePath = imported.filePath,
                        mimeType = imported.mimeType
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun deleteAttachment(att: TravelAttachmentEntity) {
        viewModelScope.launch { travelRepo.deleteAttachment(att) }
    }

    fun scheduleTravelReminder(plan: TravelPlanEntity, label: String, draft: ReminderDraft, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!draft.enabled) {
                onDone(true)
                return@launch
            }
            val trigger = nextFutureTriggerMillis(draft.dateMillis, draft.hour, draft.minute, draft.repeatType)
            val (_, exactOk) = reminderRepo.saveAndSchedule(
                ReminderEntity(
                    title = "${plan.tripName}: $label",
                    description = plan.destination,
                    featureType = "TRAVEL",
                    linkedEntityId = plan.id,
                    triggerTimeMillis = trigger,
                    repeatType = draft.repeatType,
                    repeatIntervalDays = draft.repeatIntervalDays,
                    weekdaysCsv = draft.weekdays.joinToString(","),
                    isEnabled = true,
                    soundEnabled = true,
                    vibrationEnabled = draft.vibrationEnabled,
                    notificationEnabled = true,
                    ringtoneUri = null,
                    ringingDurationSec = draft.ringingDurationSec,
                    snoozeMinutes = draft.snoozeMinutes
                )
            )
            onDone(exactOk)
        }
    }
}

private fun resolveTripStatus(plan: TravelPlanEntity): String {
    val todayStart = startOfDayMillis()
    val startDay = startOfDayMillis(plan.startDateMillis)
    val endDay = startOfDayMillis(plan.endDateMillis)
    return when {
        todayStart < startDay -> "UPCOMING"
        todayStart <= endDay -> "ONGOING"
        else -> "COMPLETED"
    }
}

@Composable
fun TravelListScreen(
    viewModel: TravelViewModel,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val plans by viewModel.allPlans.collectAsStateWithLifecycle()
    var filterStatus by remember { mutableStateOf("ALL") }
    var showCreate by remember { mutableStateOf(false) }
    var planToDelete by remember { mutableStateOf<TravelPlanEntity?>(null) }

    val filtered = remember(plans, filterStatus) {
        if (filterStatus == "ALL") plans
        else plans.filter { resolveTripStatus(it) == filterStatus }
    }

    Scaffold(
        topBar = { MyCareTopBar(title = stringResource(R.string.feature_travel_title), onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.travel_add))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ALL" to R.string.label_all,
                        "UPCOMING" to R.string.travel_status_upcoming,
                        "ONGOING" to R.string.travel_status_ongoing,
                        "COMPLETED" to R.string.travel_status_completed
                    ).forEach { (k, res) ->
                        FilterChip(selected = filterStatus == k, onClick = { filterStatus = k }, label = { Text(stringResource(res)) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    MyCareEmptyState(icon = Icons.Default.FlightTakeoff, message = stringResource(R.string.travel_empty))
                }
            } else {
                items(filtered, key = { it.id }) { plan ->
                    val status = resolveTripStatus(plan)
                    ElevatedCard(
                        onClick = { onOpenDetail(plan.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.tripName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${plan.destination} • ${formatDateShort(plan.startDateMillis)} → ${formatDateShort(plan.endDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { planToDelete = plan }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        TravelPlanEditorSheet(
            existing = null,
            onDismiss = { showCreate = false },
            onSave = { name, dest, orig, start, end, hotel, contacts, budget, places, notes ->
                viewModel.savePlan(null, name, dest, orig, start, end, hotel, contacts, budget, places, notes) { newId ->
                    showCreate = false
                    onOpenDetail(newId)
                }
            }
        )
    }

    planToDelete?.let { p ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deletePlan(p); planToDelete = null },
            onDismiss = { planToDelete = null }
        )
    }
}

@Composable
fun TravelDetailScreen(
    travelId: Long,
    viewModel: TravelViewModel,
    audioPlayer: AudioPlaybackManager,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val plan by viewModel.observePlan(travelId).collectAsStateWithLifecycle(initialValue = null)
    val expenses by viewModel.observeExpenses(travelId).collectAsStateWithLifecycle(initialValue = emptyList())
    val checklist by viewModel.observeChecklist(travelId).collectAsStateWithLifecycle(initialValue = emptyList())
    val attachments by viewModel.observeAttachments(travelId).collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showEditPlan by remember { mutableStateOf(false) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddCheck by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }

    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.addAttachment(travelId, uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.addAttachment(travelId, uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = plan?.tripName ?: stringResource(R.string.feature_travel_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showReminderSheet = true }) {
                        Icon(Icons.Default.AddAlarm, contentDescription = stringResource(R.string.reminder_enable))
                    }
                    IconButton(onClick = { showEditPlan = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val p = plan ?: return@Scaffold
        val spent = expenses.sumOf { it.amount }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.tripName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${p.origin.ifBlank { "-" }} → ${p.destination}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${formatDateShort(p.startDateMillis)} — ${formatDateShort(p.endDateMillis)}", style = MaterialTheme.typography.bodySmall)
                        if (p.hotelInfo.isNotBlank()) Text("${stringResource(R.string.travel_hotel)}: ${p.hotelInfo}")
                        if (p.contactNumbers.isNotBlank()) Text("${stringResource(R.string.travel_contacts)}: ${p.contactNumbers}")
                        if (p.placesToVisit.isNotBlank()) Text("${stringResource(R.string.travel_places)}: ${p.placesToVisit}")
                        Text(
                            "${stringResource(R.string.travel_budget)}: ${String.format("%.2f", p.budget)} • Spent: ${String.format("%.2f", spent)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (p.notes.isNotBlank()) Text(p.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Packing Checklist
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.travel_packing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    AssistChip(onClick = { showAddCheck = true }, label = { Text("+ " + stringResource(R.string.action_add)) })
                }
            }
            items(checklist, key = { it.id }) { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = item.isChecked, onCheckedChange = { viewModel.toggleChecklist(item) })
                    Text(
                        item.itemTitle,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.deleteChecklistItem(item) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }

            // Travel Expenses
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.travel_expenses), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    AssistChip(onClick = { showAddExpense = true }, label = { Text("+ " + stringResource(R.string.action_add)) })
                }
            }
            items(expenses, key = { it.id }) { exp ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(exp.title, modifier = Modifier.weight(1f))
                    Text(String.format("%.2f", exp.amount), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            }

            // Attachments
            item {
                Text(stringResource(R.string.label_attachments), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedButton(onClick = { imgPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.label_add_photo), maxLines = 1)
                    }
                    OutlinedButton(onClick = { docPicker.launch(arrayOf("application/pdf", "image/*", "*/*")) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.label_add_document), maxLines = 1)
                    }
                }
            }
            items(attachments, key = { it.id }) { att ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenMedia(att.title, att.filePath, att.mediaType, att.mimeType) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(att.title, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteAttachment(att) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    if (showAddCheck) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCheck = false },
            title = { Text(stringResource(R.string.travel_packing)) },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = { viewModel.addChecklistItem(travelId, text); showAddCheck = false }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = { showAddCheck = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    if (showAddExpense) {
        var title by remember { mutableStateOf("") }
        var amt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddExpense = false },
            title = { Text(stringResource(R.string.travel_expenses)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.wallet_title)) }, singleLine = true)
                    OutlinedTextField(value = amt, onValueChange = { amt = it }, label = { Text(stringResource(R.string.wallet_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addExpense(travelId, title, amt.toDoubleOrNull() ?: 0.0)
                    showAddExpense = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { showAddExpense = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    plan?.let { p ->
        if (showReminderSheet) {
            TravelReminderSheet(
                plan = p,
                viewModel = viewModel,
                audioPlayer = audioPlayer,
                onDismiss = { showReminderSheet = false },
                onSaved = { reminderEnabled, exactOk ->
                    showReminderSheet = false
                    scope.launch {
                        if (reminderEnabled && !exactOk) {
                            val res = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.reminder_schedule_failed),
                                actionLabel = context.getString(R.string.action_open_settings),
                                duration = SnackbarDuration.Long
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                PermissionManager.openExactAlarmSettings(context)
                            }
                        } else {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_saved_success))
                        }
                    }
                }
            )
        }

        if (showEditPlan) {
            TravelPlanEditorSheet(
                existing = p,
                onDismiss = { showEditPlan = false },
                onSave = { name, dest, orig, start, end, hotel, contacts, budget, places, notes ->
                    viewModel.savePlan(p, name, dest, orig, start, end, hotel, contacts, budget, places, notes) {
                        showEditPlan = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelReminderSheet(
    plan: TravelPlanEntity,
    viewModel: TravelViewModel,
    audioPlayer: AudioPlaybackManager,
    onDismiss: () -> Unit,
    onSaved: (reminderEnabled: Boolean, exactOk: Boolean) -> Unit
) {
    var label by remember { mutableStateOf("Departure") }
    var draft by remember { mutableStateOf(ReminderDraft(enabled = true, dateMillis = plan.startDateMillis)) }
    val types = listOf("Departure", "Hotel Check-in", "Booking", "Preparation", "Return Journey")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Travel Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                types.take(3).forEach { t ->
                    FilterChip(selected = label == t, onClick = { label = t }, label = { Text(t) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                types.drop(3).forEach { t ->
                    FilterChip(selected = label == t, onClick = { label = t }, label = { Text(t) })
                }
            }
            DatePickerField(
                label = stringResource(R.string.label_date),
                dateMillis = draft.dateMillis,
                onDateSelected = { draft = draft.copy(dateMillis = it) },
                modifier = Modifier.fillMaxWidth()
            )
            ReminderConfigSection(
                draft = draft,
                onDraftChange = { draft = it },
                fileStorage = viewModel.fileStorage,
                audioPlayer = audioPlayer
            )
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = { viewModel.scheduleTravelReminder(plan, label, draft) { exactOk -> onSaved(draft.enabled, exactOk) } }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelPlanEditorSheet(
    existing: TravelPlanEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String, dest: String, orig: String, start: Long, end: Long,
        hotel: String, contacts: String, budget: Double, places: String, notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.tripName ?: "") }
    var dest by remember { mutableStateOf(existing?.destination ?: "") }
    var orig by remember { mutableStateOf(existing?.origin ?: "") }
    var startDate by remember { mutableLongStateOf(existing?.startDateMillis ?: System.currentTimeMillis()) }
    var endDate by remember {
        mutableLongStateOf(
            existing?.endDateMillis ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis
        )
    }
    var hotel by remember { mutableStateOf(existing?.hotelInfo ?: "") }
    var contacts by remember { mutableStateOf(existing?.contactNumbers ?: "") }
    var budgetStr by remember { mutableStateOf(existing?.budget?.takeIf { it > 0 }?.toString() ?: "") }
    var places by remember { mutableStateOf(existing?.placesToVisit ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.travel_add), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = { name = it; showError = false }, label = { Text(stringResource(R.string.travel_name) + " *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dest, onValueChange = { dest = it; showError = false }, label = { Text(stringResource(R.string.travel_destination) + " *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = orig, onValueChange = { orig = it }, label = { Text(stringResource(R.string.travel_origin)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(label = stringResource(R.string.label_start_date), dateMillis = startDate, onDateSelected = { startDate = it }, modifier = Modifier.weight(1f))
                DatePickerField(label = stringResource(R.string.label_end_date), dateMillis = endDate, onDateSelected = { endDate = it }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = hotel, onValueChange = { hotel = it }, label = { Text(stringResource(R.string.travel_hotel)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contacts, onValueChange = { contacts = it }, label = { Text(stringResource(R.string.travel_contacts)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = budgetStr, onValueChange = { budgetStr = it }, label = { Text(stringResource(R.string.travel_budget)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = places, onValueChange = { places = it }, label = { Text(stringResource(R.string.travel_places)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.label_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (showError) Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = {
                    if (name.isBlank() || dest.isBlank()) showError = true
                    else onSave(name, dest, orig, startDate, endDate, hotel, contacts, budgetStr.toDoubleOrNull() ?: 0.0, places, notes)
                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
