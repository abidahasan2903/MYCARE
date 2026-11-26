package com.example.feature.memory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.database.entity.AlbumAttachmentEntity
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.MyCareTopBar
import java.io.File

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MemoryViewModel,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val album by viewModel.observeAlbum(albumId).collectAsStateWithLifecycle(initialValue = null)
    val attachments by viewModel.observeAlbumAttachments(albumId).collectAsStateWithLifecycle(initialValue = emptyList())
    var isGridView by remember { mutableStateOf(true) }
    var showEditAlbum by remember { mutableStateOf(false) }
    var showDeleteAlbum by remember { mutableStateOf(false) }
    var renamingAtt by remember { mutableStateOf<AlbumAttachmentEntity?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri -> viewModel.addAlbumAttachment(albumId, uri) }
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.addAlbumAttachment(albumId, uri)
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = album?.name ?: stringResource(R.string.mem_tab_albums),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { showEditAlbum = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { showDeleteAlbum = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            album?.let { alb ->
                if (alb.description.isNotBlank() || alb.location.isNotBlank()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        if (alb.location.isNotBlank()) {
                            Text(alb.location, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        if (alb.description.isNotBlank()) {
                            Text(alb.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Photos / Videos", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { docPicker.launch(arrayOf("application/pdf", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("PDF / Files", maxLines = 1)
                }
            }

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(attachments, key = { it.id }) { att ->
                        ElevatedCard(
                            onClick = { onOpenMedia(att.title, att.filePath, att.mediaType, att.mimeType) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column {
                                if (att.mediaType == "IMAGE" && viewModel.fileStorage.fileExists(att.filePath)) {
                                    AsyncImage(
                                        model = File(att.filePath),
                                        contentDescription = att.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            when (att.mediaType) {
                                                "VIDEO" -> Icons.Default.VideoFile
                                                "PDF" -> Icons.Default.PictureAsPdf
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        att.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.deleteAlbumAttachment(att) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(attachments, key = { it.id }) { att ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMedia(att.title, att.filePath, att.mediaType, att.mimeType) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (att.mediaType == "IMAGE" && viewModel.fileStorage.fileExists(att.filePath)) {
                                    AsyncImage(
                                        model = File(att.filePath),
                                        contentDescription = att.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(40.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(att.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(att.mediaType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                if (att.mediaType == "IMAGE") {
                                    TextButton(onClick = { album?.let { viewModel.setAlbumCoverFromPath(it, att.filePath) } }) {
                                        Text(stringResource(R.string.album_set_cover))
                                    }
                                }
                                IconButton(onClick = { renamingAtt = att }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_rename))
                                }
                                IconButton(onClick = { viewModel.deleteAlbumAttachment(att) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingAtt?.let { att ->
        var newName by remember { mutableStateOf(att.title) }
        AlertDialog(
            onDismissRequest = { renamingAtt = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true) },
            confirmButton = {
                Button(onClick = { viewModel.renameAlbumAttachment(att, newName); renamingAtt = null }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingAtt = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    val currentAlbum = album
    if (showEditAlbum && currentAlbum != null) {
        AlbumEditorDialog(
            existing = currentAlbum,
            onDismiss = { showEditAlbum = false },
            onSave = { name, desc, loc, coverUri ->
                viewModel.saveAlbum(currentAlbum, name, desc, loc, coverUri) { showEditAlbum = false }
            }
        )
    }

    if (showDeleteAlbum && currentAlbum != null) {
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteAlbum(currentAlbum) { showDeleteAlbum = false; onBack() } },
            onDismiss = { showDeleteAlbum = false }
        )
    }
}
