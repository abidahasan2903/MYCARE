package com.example.feature.exercise

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entity.ExerciseEntity
import com.example.core.database.entity.ExerciseSessionEntity
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.BarChartEntry
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import com.example.core.ui.components.SimpleBarChart
import com.example.core.ui.components.formatDateTimeShort
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExerciseListScreen(
    viewModel: ExerciseViewModel,
    audioPlayer: AudioPlaybackManager,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    val categories = listOf("ALL", "Cardio", "Strength", "Flexibility", "Yoga", "Rehab", "Other")

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_exercise_title),
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    modifier = Modifier.testTag("fab_add_exercise")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ex_add))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.feature_exercise_title)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.ex_history_tab)) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = state.selectedCategory == cat,
                                    onClick = { viewModel.setCategory(cat) },
                                    label = { Text(if (cat == "ALL") stringResource(R.string.label_all) else cat) }
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "ACTIVE" to R.string.label_active,
                                "COMPLETED" to R.string.label_completed,
                                "ALL" to R.string.label_all
                            ).forEach { (key, res) ->
                                FilterChip(
                                    selected = state.statusTab == key,
                                    onClick = { viewModel.setStatusTab(key) },
                                    label = { Text(stringResource(res)) }
                                )
                            }
                        }
                    }

                    if (state.exercises.isEmpty()) {
                        item {
                            MyCareEmptyState(
                                icon = Icons.Default.FitnessCenter,
                                message = stringResource(R.string.ex_empty)
                            )
                        }
                    } else {
                        items(state.exercises, key = { it.id }) { ex ->
                            ElevatedCard(
                                onClick = { onOpenDetail(ex.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${ex.category} • ${ex.difficulty} • ${ex.durationMinutes}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "${ex.sets} sets × ${ex.repetitions} reps • ${ex.restSeconds}s rest",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { toDelete = ex }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ExerciseHistoryTab(sessions = state.sessions)
            }
        }
    }

    if (showCreateSheet) {
        ExerciseEditorSheet(
            existing = null,
            viewModel = viewModel,
            audioPlayer = audioPlayer,
            onDismiss = { showCreateSheet = false },
            onSaved = { newId, reminderEnabled, exactOk ->
                showCreateSheet = false
                if (reminderEnabled && !exactOk) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.reminder_schedule_failed),
                            actionLabel = context.getString(R.string.action_open_settings),
                            duration = SnackbarDuration.Short
                        ).let { res ->
                            if (res == SnackbarResult.ActionPerformed) {
                                PermissionManager.openExactAlarmSettings(context)
                            }
                        }
                    }
                }
                onOpenDetail(newId)
            }
        )
    }

    toDelete?.let { ex ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteExercise(ex)
                toDelete = null
            },
            onDismiss = { toDelete = null }
        )
    }
}

@Composable
private fun ExerciseHistoryTab(sessions: List<ExerciseSessionEntity>) {
    var period by remember { mutableStateOf("WEEKLY") }
    val chartEntries = remember(sessions, period) {
        val now = Calendar.getInstance()
        when (period) {
            "DAILY" -> {
                // Last 7 days (minutes per day)
                (6 downTo 0).map { offset ->
                    val cal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
                    val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
                    val dayStart = (cal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = (cal.clone() as Calendar).apply {
                        timeInMillis = dayStart
                        add(Calendar.DAY_OF_YEAR, 1)
                    }.timeInMillis
                    val mins = sessions.filter { it.completedAtMillis in dayStart until dayEnd }
                        .sumOf { it.completedDurationSec } / 60f
                    BarChartEntry(dayFmt.format(Date(dayStart)), mins)
                }
            }
            "WEEKLY" -> {
                // Last 4 weeks
                (3 downTo 0).map { w ->
                    val end = now.timeInMillis - w * 7 * 86_400_000L
                    val start = end - 7 * 86_400_000L
                    val mins = sessions.filter { it.completedAtMillis in start until end }
                        .sumOf { it.completedDurationSec } / 60f
                    BarChartEntry("W${4 - w}", mins)
                }
            }
            else -> {
                // Last 6 months
                (5 downTo 0).map { m ->
                    val cal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -m) }
                    val targetM = cal.get(Calendar.MONTH)
                    val targetY = cal.get(Calendar.YEAR)
                    val mins = sessions.filter {
                        val sc = Calendar.getInstance().apply { timeInMillis = it.completedAtMillis }
                        sc.get(Calendar.MONTH) == targetM && sc.get(Calendar.YEAR) == targetY
                    }.sumOf { it.completedDurationSec } / 60f
                    val mFmt = SimpleDateFormat("MMM", Locale.getDefault())
                    BarChartEntry(mFmt.format(cal.time), mins)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "DAILY" to R.string.ex_daily_activity,
                    "WEEKLY" to R.string.ex_weekly_activity,
                    "MONTHLY" to R.string.ex_monthly_activity
                ).forEach { (key, res) ->
                    FilterChip(
                        selected = period == key,
                        onClick = { period = key },
                        label = { Text(stringResource(res)) }
                    )
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val totalMin = sessions.sumOf { it.completedDurationSec } / 60
                    val totalSets = sessions.sumOf { it.setsCompleted }
                    Text(
                        "${sessions.size} sessions • $totalMin min • $totalSets sets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SimpleBarChart(entries = chartEntries)
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                MyCareEmptyState(
                    icon = Icons.Default.FitnessCenter,
                    message = stringResource(R.string.ex_empty)
                )
            }
        } else {
            items(sessions, key = { it.id }) { s ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(formatDateTimeShort(s.completedAtMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${s.completedDurationSec / 60}m ${s.completedDurationSec % 60}s • ${s.setsCompleted}×${s.repetitionsCompleted}",
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
fun ExerciseEditorSheet(
    existing: ExerciseEntity?,
    viewModel: ExerciseViewModel,
    audioPlayer: AudioPlaybackManager,
    onDismiss: () -> Unit,
    onSaved: (Long, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Strength") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var instructions by remember { mutableStateOf(existing?.instructions ?: "") }
    var durationStr by remember { mutableStateOf((existing?.durationMinutes ?: 20).toString()) }
    var setsStr by remember { mutableStateOf((existing?.sets ?: 3).toString()) }
    var repsStr by remember { mutableStateOf((existing?.repetitions ?: 12).toString()) }
    var restStr by remember { mutableStateOf((existing?.restSeconds ?: 45).toString()) }
    var difficulty by remember { mutableStateOf(existing?.difficulty ?: "Beginner") }
    var startDate by remember { mutableLongStateOf(existing?.startDateMillis ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var reminderDraft by remember { mutableStateOf(ReminderDraft()) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        reminderDraft = if (existing != null) viewModel.loadReminder(existing.reminderId)
        else viewModel.getDefaultReminderDraft()
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
                text = stringResource(if (existing == null) R.string.ex_add else R.string.ex_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text(stringResource(R.string.ex_name) + " *") },
                isError = showError && name.isBlank(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("ex_name_input")
            )

            Text(stringResource(R.string.label_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Cardio", "Strength", "Flexibility", "Yoga", "Rehab", "Other").forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.label_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text(stringResource(R.string.ex_instructions)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.ex_duration_min)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = setsStr,
                    onValueChange = { setsStr = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.ex_sets)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = repsStr,
                    onValueChange = { repsStr = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.ex_reps)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = restStr,
                    onValueChange = { restStr = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.ex_rest_sec)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(stringResource(R.string.ex_difficulty), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Beginner", "Intermediate", "Advanced").forEach { d ->
                    FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d) })
                }
            }

            DatePickerField(
                label = stringResource(R.string.label_start_date),
                dateMillis = startDate,
                onDateSelected = { startDate = it },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

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
                        if (name.isBlank()) {
                            showError = true
                        } else {
                            viewModel.saveExercise(
                                existing = existing,
                                name = name,
                                category = category,
                                description = description,
                                instructions = instructions,
                                durationMin = durationStr.toIntOrNull() ?: 20,
                                sets = setsStr.toIntOrNull() ?: 3,
                                reps = repsStr.toIntOrNull() ?: 12,
                                restSec = restStr.toIntOrNull() ?: 45,
                                difficulty = difficulty,
                                startDate = startDate,
                                notes = notes,
                                reminderDraft = reminderDraft
                            ) { id, exactOk -> onSaved(id, reminderDraft.enabled, exactOk) }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("save_exercise_btn")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
