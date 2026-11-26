package com.example.feature.exercise

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.core.database.entity.ExerciseAttachmentEntity
import com.example.core.database.entity.ExerciseEntity
import com.example.core.media.AudioPlaybackManager
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.formatDateShort
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    viewModel: ExerciseViewModel,
    audioPlayer: AudioPlaybackManager,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val exercise by viewModel.observeExercise(exerciseId).collectAsStateWithLifecycle(initialValue = null)
    val attachments by viewModel.observeAttachments(exerciseId).collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var replacingAttachment by remember { mutableStateOf<ExerciseAttachmentEntity?>(null) }
    var editingAttachment by remember { mutableStateOf<ExerciseAttachmentEntity?>(null) }

    // Live Exercise Session State
    var sessionActive by remember { mutableStateOf(false) }
    var sessionPaused by remember { mutableStateOf(false) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var setsCompleted by remember { mutableIntStateOf(0) }
    var restCountdownSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(sessionActive, sessionPaused) {
        while (sessionActive && !sessionPaused) {
            delay(1000L)
            elapsedSec++
            if (restCountdownSec > 0) restCountdownSec--
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.addAttachment(exerciseId, uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.addAttachment(exerciseId, uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }
    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = replacingAttachment
        if (uri != null && target != null) {
            viewModel.replaceAttachment(target, uri)
        }
        replacingAttachment = null
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = exercise?.name ?: stringResource(R.string.feature_exercise_title),
                onBack = onBack,
                actions = {
                    exercise?.let { ex ->
                        IconButton(onClick = { viewModel.toggleCompleted(ex) }) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.label_completed),
                                tint = if (ex.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val ex = exercise ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exercise Overview Card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ex.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${ex.category} • ${ex.difficulty} • ${formatDateShort(ex.startDateMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${ex.durationMinutes} min • ${ex.sets} sets × ${ex.repetitions} reps • ${ex.restSeconds}s rest",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (ex.description.isNotBlank()) Text(ex.description, style = MaterialTheme.typography.bodyMedium)
                        if (ex.instructions.isNotBlank()) {
                            Text(
                                "${stringResource(R.string.ex_instructions)}: ${ex.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (ex.notes.isNotBlank()) {
                            Text("${stringResource(R.string.label_notes)}: ${ex.notes}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Live Exercise Session Card
            item {
                ExerciseSessionCard(
                    exercise = ex,
                    sessionActive = sessionActive,
                    sessionPaused = sessionPaused,
                    elapsedSec = elapsedSec,
                    setsCompleted = setsCompleted,
                    restCountdownSec = restCountdownSec,
                    onStart = {
                        sessionActive = true
                        sessionPaused = false
                        elapsedSec = 0
                        setsCompleted = 0
                        restCountdownSec = 0
                    },
                    onPauseResume = { sessionPaused = !sessionPaused },
                    onCompleteSet = {
                        if (setsCompleted < ex.sets) {
                            setsCompleted++
                            restCountdownSec = ex.restSeconds
                        }
                    },
                    onComplete = {
                        viewModel.recordCompletedSession(ex, elapsedSec, setsCompleted.coerceAtLeast(1), ex.repetitions)
                        sessionActive = false
                        sessionPaused = false
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_saved_success)) }
                    },
                    onCancel = {
                        sessionActive = false
                        sessionPaused = false
                        elapsedSec = 0
                        setsCompleted = 0
                        restCountdownSec = 0
                    }
                )
            }

            // Mandatory Media & Attachments Section
            item {
                Text(
                    text = stringResource(R.string.label_attachments),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Photo / Video", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { docPicker.launch(arrayOf("application/pdf", "video/*", "image/*", "*/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("PDF / File", maxLines = 1)
                    }
                }
            }

            items(attachments, key = { it.id }) { att ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMedia(att.title, att.filePath, att.mediaType, att.mimeType) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (att.mediaType == "IMAGE" && viewModel.fileStorage.fileExists(att.filePath)) {
                                AsyncImage(
                                    model = File(att.filePath),
                                    contentDescription = att.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = when (att.mediaType) {
                                        "VIDEO" -> Icons.Default.VideoFile
                                        "PDF" -> Icons.Default.PictureAsPdf
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(att.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (att.caption.isNotBlank()) {
                                    Text(att.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(att.mediaType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { editingAttachment = att }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_rename))
                            }
                            IconButton(onClick = {
                                replacingAttachment = att
                                replacePicker.launch(arrayOf("*/*"))
                            }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.action_replace))
                            }
                            IconButton(onClick = { viewModel.deleteAttachment(att) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_remove))
                            }
                        }
                    }
                }
            }
        }
    }

    editingAttachment?.let { att ->
        var newTitle by remember { mutableStateOf(att.title) }
        var newCaption by remember { mutableStateOf(att.caption) }
        var newOrder by remember { mutableStateOf(att.sortOrder.toString()) }
        AlertDialog(
            onDismissRequest = { editingAttachment = null },
            title = { Text(stringResource(R.string.action_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text(stringResource(R.string.action_rename)) })
                    OutlinedTextField(value = newCaption, onValueChange = { newCaption = it }, label = { Text(stringResource(R.string.label_caption)) })
                    OutlinedTextField(value = newOrder, onValueChange = { newOrder = it.filter(Char::isDigit) }, label = { Text("Order") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateAttachmentMeta(att, newTitle, newCaption, newOrder.toIntOrNull() ?: att.sortOrder)
                    editingAttachment = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingAttachment = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    exercise?.let { ex ->
        if (showEditSheet) {
            ExerciseEditorSheet(
                existing = ex,
                viewModel = viewModel,
                audioPlayer = audioPlayer,
                onDismiss = { showEditSheet = false },
                onSaved = { _, reminderEnabled, exactOk ->
                    showEditSheet = false
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

        if (showDeleteConfirm) {
            ConfirmDeleteDialog(
                onConfirm = {
                    viewModel.deleteExercise(ex) {
                        showDeleteConfirm = false
                        onBack()
                    }
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@Composable
private fun ExerciseSessionCard(
    exercise: ExerciseEntity,
    sessionActive: Boolean,
    sessionPaused: Boolean,
    elapsedSec: Int,
    setsCompleted: Int,
    restCountdownSec: Int,
    onStart: () -> Unit,
    onPauseResume: () -> Unit,
    onCompleteSet: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!sessionActive) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ex_start_session))
                }
            } else {
                val mins = elapsedSec / 60
                val secs = elapsedSec % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${stringResource(R.string.ex_sets)}: $setsCompleted / ${exercise.sets} • ${stringResource(R.string.ex_reps)}: ${exercise.repetitions}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (restCountdownSec > 0) {
                    Text(
                        "${stringResource(R.string.ex_rest_timer)}: ${restCountdownSec}s",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                LinearProgressIndicator(
                    progress = { (setsCompleted.toFloat() / exercise.sets.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPauseResume, modifier = Modifier.weight(1f)) {
                        Icon(if (sessionPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (sessionPaused) R.string.ex_resume_session else R.string.ex_pause_session))
                    }
                    OutlinedButton(onClick = onCompleteSet, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ex_next_set))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.ex_cancel_session))
                    }
                    Button(onClick = onComplete, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.ex_complete_session))
                    }
                }
            }
        }
    }
}
