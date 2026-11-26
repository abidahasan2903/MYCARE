package com.example.feature.tasks

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entity.TaskEntity
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.TimePickerField
import com.example.core.ui.components.formatDateShort
import com.example.core.ui.components.formatTime12h
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    audioPlayer: AudioPlaybackManager,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showNewCatDialog by remember { mutableStateOf(false) }

    val orgTabs = listOf(
        "TODAY" to R.string.task_today,
        "TOMORROW" to R.string.task_tomorrow,
        "UPCOMING" to R.string.task_upcoming,
        "PENDING" to R.string.label_pending,
        "COMPLETED" to R.string.label_completed,
        "HISTORY" to R.string.label_history
    )

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_task_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.action_sort))
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Date") }, onClick = { viewModel.setSortMode("DATE"); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Priority") }, onClick = { viewModel.setSortMode("PRIORITY"); showSortMenu = false })
                        DropdownMenuItem(text = { Text("Title") }, onClick = { viewModel.setSortMode("TITLE"); showSortMenu = false })
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showEditor = true
                },
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.task_add))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MyCareSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::setSearch,
                    placeholder = stringResource(R.string.action_search)
                )
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orgTabs.forEach { (key, res) ->
                        FilterChip(
                            selected = state.orgTab == key,
                            onClick = { viewModel.setOrgTab(key) },
                            label = { Text(stringResource(res)) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.categoryFilter == "ALL",
                        onClick = { viewModel.setCategoryFilter("ALL") },
                        label = { Text(stringResource(R.string.label_all)) }
                    )
                    state.categories.forEach { cat ->
                        FilterChip(
                            selected = state.categoryFilter == cat.name,
                            onClick = { viewModel.setCategoryFilter(cat.name) },
                            label = { Text(cat.name) }
                        )
                    }
                    AssistChip(
                        onClick = { showNewCatDialog = true },
                        label = { Text("+ " + stringResource(R.string.task_custom_category)) }
                    )
                }
            }

            if (state.tasks.isEmpty()) {
                item {
                    MyCareEmptyState(
                        icon = Icons.Default.AddTask,
                        message = stringResource(R.string.task_empty)
                    )
                }
            } else {
                items(state.tasks, key = { it.id }) { task ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleCompleted(task) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                    )
                                    Text(
                                        text = "${task.categoryName} • ${formatDateShort(task.dueDateMillis)} ${formatTime12h(task.timeHour, task.timeMinute)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AssistChip(
                                    onClick = {
                                        val next = when (task.priority) {
                                            "HIGH" -> "MEDIUM"
                                            "MEDIUM" -> "LOW"
                                            else -> "HIGH"
                                        }
                                        viewModel.changePriority(task, next)
                                    },
                                    label = { Text(task.priority) }
                                )
                            }

                            if (task.description.isNotBlank()) {
                                Text(task.description, style = MaterialTheme.typography.bodySmall)
                            }
                            task.deadlineMillis?.let { dl ->
                                Text(
                                    "${stringResource(R.string.task_deadline)}: ${formatDateShort(dl)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                task.imagePath?.let { path ->
                                    IconButton(onClick = { onOpenMedia(task.title, path, "IMAGE", "image/*") }) {
                                        Icon(Icons.Default.Image, contentDescription = null)
                                    }
                                }
                                task.documentPath?.let { path ->
                                    IconButton(onClick = {
                                        val mType = viewModel.fileStorage.classifyMediaType("", path)
                                        onOpenMedia(task.documentName ?: task.title, path, mType, "*/*")
                                    }) {
                                        Icon(Icons.Default.Description, contentDescription = null)
                                    }
                                }
                                IconButton(onClick = {
                                    editingTask = task
                                    showEditor = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                                }
                                IconButton(onClick = { taskToDelete = task }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        TaskEditorSheet(
            existing = editingTask,
            categories = state.categories.map { it.name }.ifEmpty { listOf("Personal", "Study", "Work", "Health", "Shopping", "Other") },
            viewModel = viewModel,
            audioPlayer = audioPlayer,
            onDismiss = { showEditor = false },
            onSaved = { reminderEnabled, exactOk ->
                showEditor = false
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

    if (showNewCatDialog) {
        var newCat by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCatDialog = false },
            title = { Text(stringResource(R.string.task_custom_category)) },
            text = {
                OutlinedTextField(
                    value = newCat,
                    onValueChange = { newCat = it },
                    label = { Text(stringResource(R.string.label_category)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomCategory(newCat)
                    showNewCatDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewCatDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    taskToDelete?.let { t ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteTask(t)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorSheet(
    existing: TaskEntity?,
    categories: List<String>,
    viewModel: TasksViewModel,
    audioPlayer: AudioPlaybackManager,
    onDismiss: () -> Unit,
    onSaved: (reminderEnabled: Boolean, exactOk: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var dueDate by remember { mutableLongStateOf(existing?.dueDateMillis ?: System.currentTimeMillis()) }
    var hour by remember { mutableIntStateOf(existing?.timeHour ?: 9) }
    var minute by remember { mutableIntStateOf(existing?.timeMinute ?: 0) }
    var priority by remember { mutableStateOf(existing?.priority ?: "MEDIUM") }
    var hasDeadline by remember { mutableStateOf(existing?.deadlineMillis != null) }
    var deadline by remember {
        mutableLongStateOf(
            existing?.deadlineMillis ?: Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        )
    }
    var category by remember { mutableStateOf(existing?.categoryName ?: categories.firstOrNull() ?: "Personal") }
    var pickedImage by remember { mutableStateOf<Uri?>(null) }
    var pickedDoc by remember { mutableStateOf<Uri?>(null) }
    var reminderDraft by remember { mutableStateOf(ReminderDraft()) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        reminderDraft = if (existing != null) viewModel.loadReminder(existing.reminderId)
        else viewModel.getDefaultReminderDraft()
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedImage = uri
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pickedDoc = uri
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(if (existing == null) R.string.task_add else R.string.task_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; showError = false },
                label = { Text(stringResource(R.string.task_title) + " *") },
                isError = showError && title.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("task_title_input")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.label_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    label = stringResource(R.string.label_date),
                    dateMillis = dueDate,
                    onDateSelected = {
                        dueDate = it
                        reminderDraft = reminderDraft.copy(dateMillis = it)
                    },
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = stringResource(R.string.label_time),
                    hour = hour,
                    minute = minute,
                    onTimeSelected = { h, m ->
                        hour = h
                        minute = m
                        reminderDraft = reminderDraft.copy(hour = h, minute = m)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(stringResource(R.string.task_priority), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                    FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                }
            }

            Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDeadline, onCheckedChange = { hasDeadline = it })
                Text(stringResource(R.string.task_deadline))
            }
            if (hasDeadline) {
                DatePickerField(
                    label = stringResource(R.string.task_deadline),
                    dateMillis = deadline,
                    onDateSelected = { deadline = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
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

            ReminderConfigSection(
                draft = reminderDraft,
                onDraftChange = { reminderDraft = it },
                fileStorage = viewModel.fileStorage,
                audioPlayer = audioPlayer
            )

            if (showError) {
                Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
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
                        if (title.isBlank()) {
                            showError = true
                        } else {
                            viewModel.saveTask(
                                existing = existing,
                                title = title,
                                description = description,
                                dueDateMillis = dueDate,
                                hour = hour,
                                minute = minute,
                                priority = priority,
                                deadlineMillis = if (hasDeadline) deadline else null,
                                categoryName = category,
                                imageUri = pickedImage,
                                docUri = pickedDoc,
                                reminderDraft = reminderDraft
                            ) { exactOk -> onSaved(reminderDraft.enabled, exactOk) }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("save_task_btn")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
