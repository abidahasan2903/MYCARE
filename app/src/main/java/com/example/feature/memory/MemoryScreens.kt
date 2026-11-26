package com.example.feature.memory

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.database.entity.MemoryAlbumEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.VoiceNoteEntity
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.formatDateShort
import com.example.core.ui.components.formatDateTimeShort
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onOpenAlbum: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Notes, 1 = Albums, 2 = Voice Notes
    var showNoteSheet by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showAlbumDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.audioPlayer.stop() }
    }

    Scaffold(
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { editingNote = null; showNoteSheet = true },
                    modifier = Modifier.testTag("fab_add_note")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.note_add))
                }
                1 -> FloatingActionButton(
                    onClick = { showAlbumDialog = true },
                    modifier = Modifier.testTag("fab_add_album")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.album_add))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.mem_tab_notes)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.mem_tab_albums)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(stringResource(R.string.mem_tab_voice)) })
            }

            when (selectedTab) {
                0 -> NotesTabContent(
                    state = state,
                    viewModel = viewModel,
                    onEditNote = { editingNote = it; showNoteSheet = true }
                )
                1 -> AlbumsTabContent(
                    state = state,
                    viewModel = viewModel,
                    onOpenAlbum = onOpenAlbum
                )
                2 -> VoiceNotesTabContent(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }

    if (showNoteSheet) {
        NoteEditorSheet(
            existing = editingNote,
            folders = state.noteFolders,
            onDismiss = { showNoteSheet = false },
            onSave = { title, content, cat, folderId ->
                viewModel.saveNote(editingNote, title, content, cat, folderId) {
                    showNoteSheet = false
                }
            }
        )
    }

    if (showAlbumDialog) {
        AlbumEditorDialog(
            existing = null,
            onDismiss = { showAlbumDialog = false },
            onSave = { name, desc, loc, coverUri ->
                viewModel.saveAlbum(null, name, desc, loc, coverUri) { newId ->
                    showAlbumDialog = false
                    onOpenAlbum(newId)
                }
            }
        )
    }
}

@Composable
private fun NotesTabContent(
    state: MemoryUiState,
    viewModel: MemoryViewModel,
    onEditNote: (NoteEntity) -> Unit
) {
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }

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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.selectedFolderId == null && !state.favoritesOnly,
                    onClick = { viewModel.setSelectedFolder(null); viewModel.setFavoritesOnly(false) },
                    label = { Text(stringResource(R.string.label_all)) }
                )
                FilterChip(
                    selected = state.favoritesOnly,
                    onClick = { viewModel.setFavoritesOnly(!state.favoritesOnly) },
                    label = { Text(stringResource(R.string.note_favorites)) }
                )
                state.noteFolders.forEach { folder ->
                    FilterChip(
                        selected = state.selectedFolderId == folder.id,
                        onClick = { viewModel.setSelectedFolder(if (state.selectedFolderId == folder.id) null else folder.id) },
                        label = { Text(folder.name) }
                    )
                }
                AssistChip(onClick = { showNewFolder = true }, label = { Text("+ " + stringResource(R.string.note_new_folder)) })
            }
        }
        if (state.notes.isEmpty()) {
            item {
                MyCareEmptyState(icon = Icons.Default.Description, message = stringResource(R.string.note_empty))
            }
        } else {
            items(state.notes, key = { it.id }) { note ->
                ElevatedCard(
                    onClick = { onEditNote(note) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.toggleNoteFavorite(note) }) {
                                Icon(
                                    if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(R.string.note_favorites),
                                    tint = if (note.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { noteToDelete = note }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                        Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                        Text(
                            "${note.category} • ${formatDateTimeShort(note.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text(stringResource(R.string.note_new_folder)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = { viewModel.createNoteFolder(name); showNewFolder = false }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    noteToDelete?.let { n ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteNote(n); noteToDelete = null },
            onDismiss = { noteToDelete = null }
        )
    }
}

@Composable
private fun AlbumsTabContent(
    state: MemoryUiState,
    viewModel: MemoryViewModel,
    onOpenAlbum: (Long) -> Unit
) {
    var albumToDelete by remember { mutableStateOf<MemoryAlbumEntity?>(null) }

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
        if (state.albums.isEmpty()) {
            item {
                MyCareEmptyState(icon = Icons.Default.PhotoAlbum, message = stringResource(R.string.album_empty))
            }
        } else {
            items(state.albums, key = { it.id }) { album ->
                ElevatedCard(
                    onClick = { onOpenAlbum(album.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (album.coverPhotoPath != null && viewModel.fileStorage.fileExists(album.coverPhotoPath)) {
                            AsyncImage(
                                model = File(album.coverPhotoPath),
                                contentDescription = album.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoAlbum, contentDescription = null, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (album.location.isNotBlank()) {
                                Text(album.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(formatDateShort(album.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { albumToDelete = album }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    albumToDelete?.let { a ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteAlbum(a); albumToDelete = null },
            onDismiss = { albumToDelete = null }
        )
    }
}

@Composable
private fun VoiceNotesTabContent(
    state: MemoryUiState,
    viewModel: MemoryViewModel
) {
    val context = LocalContext.current
    val playState by viewModel.audioPlayer.state.collectAsStateWithLifecycle()

    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var showSaveDialog by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var editingVoice by remember { mutableStateOf<VoiceNoteEntity?>(null) }
    var deletingVoice by remember { mutableStateOf<VoiceNoteEntity?>(null) }
    var showMicPermissionNotice by remember { mutableStateOf(false) }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showMicPermissionNotice = false
            viewModel.audioRecorder.startRecording().onSuccess {
                isRecording = true
                isPaused = false
                elapsedMs = 0L
            }
        } else {
            showMicPermissionNotice = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                viewModel.audioRecorder.cancelRecording()
            }
        }
    }

    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(250L)
            elapsedMs += 250L
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val secs = (elapsedMs / 1000L)
                    Text(
                        text = String.format("%02d:%02d", secs / 60, secs % 60),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    if (showMicPermissionNotice) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.voice_mic_needed),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { PermissionManager.openAppSettings(context) }) {
                                Text(stringResource(R.string.action_open_settings))
                            }
                        }
                    }

                    if (!isRecording) {
                        Button(
                            onClick = {
                                if (PermissionManager.hasMicrophonePermission(context)) {
                                    viewModel.audioRecorder.startRecording().onSuccess {
                                        isRecording = true
                                        isPaused = false
                                        elapsedMs = 0L
                                    }
                                } else {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("voice_record_btn")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.voice_record))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (isPaused) {
                                        if (viewModel.audioRecorder.resumeRecording()) isPaused = false
                                    } else {
                                        if (viewModel.audioRecorder.pauseRecording()) isPaused = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(if (isPaused) R.string.voice_resume else R.string.voice_pause))
                            }
                            Button(
                                onClick = {
                                    val savedFile = viewModel.audioRecorder.stopAndSave()
                                    val dur = elapsedMs
                                    isRecording = false
                                    isPaused = false
                                    if (savedFile != null) {
                                        showSaveDialog = savedFile.absolutePath to dur
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.voice_stop_save))
                            }
                        }
                    }
                }
            }
        }

        item {
            MyCareSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::setSearch,
                placeholder = stringResource(R.string.action_search)
            )
        }

        if (state.voiceNotes.isEmpty()) {
            item {
                MyCareEmptyState(icon = Icons.Default.Mic, message = stringResource(R.string.voice_empty))
            }
        } else {
            items(state.voiceNotes, key = { it.id }) { vn ->
                val isPlayingThis = playState.isPlaying && playState.activeSource == vn.filePath
                val durSec = vn.durationMillis / 1000L
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (isPlayingThis) viewModel.audioPlayer.pause()
                            else viewModel.audioPlayer.playFile(vn.filePath)
                        }) {
                            Icon(
                                if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vn.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${formatDateTimeShort(vn.createdAt)} • ${String.format("%02d:%02d", durSec / 60, durSec % 60)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (vn.description.isNotBlank()) {
                                Text(vn.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { editingVoice = vn }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_rename))
                        }
                        IconButton(onClick = { deletingVoice = vn }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    showSaveDialog?.let { (path, dur) ->
        var title by remember { mutableStateOf("Voice Note") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                viewModel.fileStorage.deleteFile(path)
                showSaveDialog = null
            },
            title = { Text(stringResource(R.string.voice_title_prompt)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.voice_title_prompt)) }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.label_description)) }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveVoiceNote(title, desc, path, dur)
                    showSaveDialog = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.fileStorage.deleteFile(path)
                    showSaveDialog = null
                }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    editingVoice?.let { vn ->
        var title by remember { mutableStateOf(vn.title) }
        var desc by remember { mutableStateOf(vn.description) }
        AlertDialog(
            onDismissRequest = { editingVoice = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.label_description)) })
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateVoiceNote(vn, title, desc); editingVoice = null }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVoice = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    deletingVoice?.let { vn ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteVoiceNote(vn); deletingVoice = null },
            onDismiss = { deletingVoice = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorSheet(
    existing: NoteEntity?,
    folders: List<com.example.core.database.entity.NoteFolderEntity>,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String, folderId: Long?) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "Personal") }
    var folderId by remember { mutableStateOf(existing?.folderId) }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.note_add), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; showError = false },
                label = { Text(stringResource(R.string.note_title) + " *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.note_content)) },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.label_category)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (folders.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(selected = folderId == null, onClick = { folderId = null }, label = { Text("No Folder") })
                    folders.forEach { f ->
                        FilterChip(selected = folderId == f.id, onClick = { folderId = f.id }, label = { Text(f.name) })
                    }
                }
            }
            if (showError) Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        if (title.isBlank()) showError = true else onSave(title, content, category, folderId)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

@Composable
fun AlbumEditorDialog(
    existing: MemoryAlbumEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, location: String, coverUri: Uri?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var location by remember { mutableStateOf(existing?.location ?: "") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) coverUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.album_add else R.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.album_name) + " *") }, singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.album_location)) }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.label_description)) }, minLines = 2)
                OutlinedButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (coverUri != null) "Cover Selected" else stringResource(R.string.album_set_cover))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onSave(name, desc, location, coverUri)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
