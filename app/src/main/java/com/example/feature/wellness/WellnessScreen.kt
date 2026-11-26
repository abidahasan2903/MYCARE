package com.example.feature.wellness

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.ReminderConfigSection
import com.example.core.ui.components.ReminderDraft
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun WellnessScreen(viewModel: WellnessViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("250") }
    var goal by remember(state.waterDailyGoalMl) { mutableStateOf(state.waterDailyGoalMl.toString()) }
    var waterNote by remember { mutableStateOf("") }
    var bedtime by remember { mutableStateOf(defaultBedtime()) }
    var wakeTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var sleepNotes by remember { mutableStateOf("") }
    var waterReminder by remember { mutableStateOf(ReminderDraft(repeatType = "DAILY", hour = 9)) }
    var sleepReminder by remember { mutableStateOf(ReminderDraft(repeatType = "DAILY", hour = 21)) }
    val waterLinkedId = 9001L
    val sleepLinkedId = 9002L

    LaunchedEffect(Unit) {
        waterReminder = viewModel.getReminderDraft("GENERAL", waterLinkedId, 9)
        sleepReminder = viewModel.getReminderDraft("GENERAL", sleepLinkedId, 21)
    }

    val todayStart = remember { Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis }
    val todayWater = state.waterIntakes.filter { it.recordedAtMillis >= todayStart }.sumOf { it.amountMl }

    Column(Modifier.fillMaxSize()) {
        MyCareTopBar(stringResource(R.string.feature_wellness_title), onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.wellness_water_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.wellness_water_today, todayWater, state.waterDailyGoalMl))
                        LinearProgressIndicator(progress = { (todayWater.toFloat() / state.waterDailyGoalMl).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(amount, { amount = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.wellness_amount_ml)) }, modifier = Modifier.weight(1f), singleLine = true)
                            Button(onClick = {
                                val ml = amount.toIntOrNull()
                                if (ml != null) viewModel.addWater(ml, waterNote) { result ->
                                    if (result.isSuccess) { waterNote = "" } else android.widget.Toast.makeText(context, context.getString(R.string.wellness_invalid_water), android.widget.Toast.LENGTH_SHORT).show()
                                } else android.widget.Toast.makeText(context, context.getString(R.string.wellness_invalid_water), android.widget.Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.wellness_add_water)) }
                        }
                        OutlinedTextField(waterNote, { waterNote = it }, label = { Text(stringResource(R.string.wellness_note_optional)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(goal, { goal = it.filter(Char::isDigit).take(5) }, label = { Text(stringResource(R.string.wellness_daily_goal)) }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedButton(onClick = { goal.toIntOrNull()?.let(viewModel::setWaterGoal) }) { Text(stringResource(R.string.action_save)) }
                        }
                    }
                }
            }
            item {
                ReminderConfigSection(
                    draft = waterReminder,
                    onDraftChange = { waterReminder = it; viewModel.saveReminderDraft("GENERAL", waterLinkedId, context.getString(R.string.wellness_water_reminder), context.getString(R.string.wellness_water_title), it) {} },
                    fileStorage = viewModel.fileStorage,
                    audioPlayer = viewModel.audioPlayer
                )
            }
            item { Text(stringResource(R.string.wellness_water_history), style = MaterialTheme.typography.titleMedium) }
            if (state.waterIntakes.isEmpty()) item { Text(stringResource(R.string.wellness_empty_water), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.waterIntakes, key = { "water-${it.id}" }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${entry.amountMl} ml", style = MaterialTheme.typography.titleMedium)
                            Text(formatDate(entry.recordedAtMillis) + if (entry.note.isBlank()) "" else " · ${entry.note}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.deleteWater(entry) }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete)) }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.wellness_sleep_title), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = { chooseDateTime(context, bedtime) { bedtime = it } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wellness_bedtime, formatDate(bedtime))) }
                        OutlinedButton(onClick = { chooseDateTime(context, wakeTime) { wakeTime = it } }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wellness_wake_time, formatDate(wakeTime))) }
                        OutlinedTextField(sleepNotes, { sleepNotes = it }, label = { Text(stringResource(R.string.wellness_sleep_notes)) }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            viewModel.addSleep(bedtime, wakeTime, sleepNotes) { result ->
                                if (result.isSuccess) sleepNotes = "" else android.widget.Toast.makeText(context, context.getString(R.string.wellness_invalid_sleep), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.wellness_save_sleep)) }
                    }
                }
            }
            item {
                ReminderConfigSection(
                    draft = sleepReminder,
                    onDraftChange = { sleepReminder = it; viewModel.saveReminderDraft("GENERAL", sleepLinkedId, context.getString(R.string.wellness_sleep_reminder), context.getString(R.string.wellness_sleep_title), it) {} },
                    fileStorage = viewModel.fileStorage,
                    audioPlayer = viewModel.audioPlayer
                )
            }
            item { Text(stringResource(R.string.wellness_sleep_history), style = MaterialTheme.typography.titleMedium) }
            if (state.sleepRecords.isEmpty()) item { Text(stringResource(R.string.wellness_empty_sleep), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.sleepRecords, key = { "sleep-${it.id}" }) { record ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            val hours = sleepDurationMinutes(record.bedtimeMillis, record.wakeTimeMillis)
                            Text(stringResource(R.string.wellness_duration, hours / 60, hours % 60), style = MaterialTheme.typography.titleMedium)
                            Text("${formatDate(record.bedtimeMillis)} – ${formatDate(record.wakeTimeMillis)}${if (record.notes.isBlank()) "" else " · ${record.notes}"}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.deleteSleep(record) }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete)) }
                    }
                }
            }
        }
    }
}

private fun defaultBedtime(): Long = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 22); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

private fun chooseDateTime(context: android.content.Context, initial: Long, onResult: (Long) -> Unit) {
    val current = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            onResult(Calendar.getInstance().apply { set(year, month, day, hour, minute, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
        }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), false).show()
    }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
}

private fun formatDate(time: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(time))
