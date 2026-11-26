package com.example.feature.medicalrecords

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.example.core.database.entity.MedicalDocumentEntity
import com.example.core.database.entity.MedicalFolderEntity
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.DatePickerField
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.formatDateShort
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MedicalRecordsScreen(
    viewModel: MedicalRecordsViewModel,
    onBack: () -> Unit,
    onOpenFolder: (Long) -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateSheet by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<MedicalFolderEntity?>(null) }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_medical_records_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::toggleSortOrder) {
                        Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.action_sort))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                modifier = Modifier.testTag("fab_add_medical_folder")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.medrec_new_folder))
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

            if (state.folders.isEmpty()) {
                item {
                    MyCareEmptyState(
                        icon = Icons.Default.FolderShared,
                        message = stringResource(R.string.medrec_empty)
                    )
                }
            } else {
                items(state.folders, key = { it.id }) { folder ->
                    ElevatedCard(
                        onClick = { onOpenFolder(folder.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder.conditionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val sub = listOfNotNull(
                                    folder.doctorName.takeIf { it.isNotBlank() },
                                    folder.hospitalName.takeIf { it.isNotBlank() },
                                    folder.diagnosisDateMillis?.let { formatDateShort(it) }
                                ).joinToString(" • ")
                                if (sub.isNotBlank()) {
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { folderToDelete = folder }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }

            if (state.recentDocuments.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.dash_recent_medical),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.recentDocuments, key = { it.id }) { doc ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMedia(doc.title, doc.filePath, doc.mediaType, doc.mimeType) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (doc.mediaType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(formatDateShort(doc.documentDateMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        MedicalFolderEditorSheet(
            existing = null,
            onDismiss = { showCreateSheet = false },
            onSave = { name, desc, diagDate, doc, hosp, treat, dates, obs ->
                viewModel.saveFolder(null, name, desc, diagDate, doc, hosp, treat, dates, obs) { newId ->
                    showCreateSheet = false
                    onOpenFolder(newId)
                }
            }
        )
    }

    folderToDelete?.let { f ->
        ConfirmDeleteDialog(
            message = stringResource(R.string.medrec_delete_folder_warn),
            onConfirm = { viewModel.deleteFolder(f); folderToDelete = null },
            onDismiss = { folderToDelete = null }
        )
    }
}

@Composable
fun MedicalFolderDetailScreen(
    folderId: Long,
    viewModel: MedicalRecordsViewModel,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val folder by viewModel.observeFolder(folderId).collectAsStateWithLifecycle(initialValue = null)
    val documents by viewModel.observeDocumentsForFolder(folderId).collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showEditFolder by remember { mutableStateOf(false) }
    var showDeleteFolder by remember { mutableStateOf(false) }
    var pendingUploadUri by remember { mutableStateOf<Uri?>(null) }
    var editingDoc by remember { mutableStateOf<MedicalDocumentEntity?>(null) }
    var deletingDoc by remember { mutableStateOf<MedicalDocumentEntity?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingUploadUri = uri
    }
    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingUploadUri = uri
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = folder?.conditionName ?: stringResource(R.string.feature_medical_records_title),
                onBack = onBack,
                actions = {
                    folder?.let {
                        IconButton(onClick = { showEditFolder = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDeleteFolder = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val f = folder ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(f.conditionName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        f.diagnosisDateMillis?.let {
                            Text("${stringResource(R.string.medrec_diagnosis_date)}: ${formatDateShort(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (f.doctorName.isNotBlank()) Text("${stringResource(R.string.medrec_doctor_name)}: ${f.doctorName}")
                        if (f.hospitalName.isNotBlank()) Text("${stringResource(R.string.medrec_hospital)}: ${f.hospitalName}")
                        if (f.description.isNotBlank()) Text(f.description, style = MaterialTheme.typography.bodyMedium)
                        if (f.treatmentNotes.isNotBlank()) Text("${stringResource(R.string.medrec_treatment_notes)}: ${f.treatmentNotes}", style = MaterialTheme.typography.bodySmall)
                        if (f.observations.isNotBlank()) Text("${stringResource(R.string.medrec_observations)}: ${f.observations}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Text(stringResource(R.string.medrec_upload_doc), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
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
                        Text("PDF / Report", maxLines = 1)
                    }
                }
            }

            items(documents, key = { it.id }) { doc ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMedia(doc.title, doc.filePath, doc.mediaType, doc.mimeType) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (doc.mediaType == "IMAGE" && viewModel.fileStorage.fileExists(doc.filePath)) {
                            AsyncImage(
                                model = File(doc.filePath),
                                contentDescription = doc.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Icon(
                                if (doc.mediaType == "PDF") Icons.Default.PictureAsPdf else Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(formatDateShort(doc.documentDateMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (doc.description.isNotBlank()) Text(doc.description, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { editingDoc = doc }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_rename))
                        }
                        IconButton(onClick = { deletingDoc = doc }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    pendingUploadUri?.let { uri ->
        var title by remember { mutableStateOf(viewModel.fileStorage.queryDisplayName(uri) ?: "Medical Document") }
        var desc by remember { mutableStateOf("") }
        var docDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
        AlertDialog(
            onDismissRequest = { pendingUploadUri = null },
            title = { Text(stringResource(R.string.medrec_upload_doc)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.action_rename)) }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.label_description)) }, minLines = 2)
                    DatePickerField(label = stringResource(R.string.label_date), dateMillis = docDate, onDateSelected = { docDate = it }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addDocument(folderId, uri, title, desc, docDate) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
                    }
                    pendingUploadUri = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUploadUri = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    editingDoc?.let { doc ->
        var title by remember { mutableStateOf(doc.title) }
        var desc by remember { mutableStateOf(doc.description) }
        AlertDialog(
            onDismissRequest = { editingDoc = null },
            title = { Text(stringResource(R.string.action_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.label_description)) })
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateDocumentMeta(doc, title, desc); editingDoc = null }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDoc = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    deletingDoc?.let { d ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteDocument(d); deletingDoc = null },
            onDismiss = { deletingDoc = null }
        )
    }

    val currentFolder = folder
    if (showEditFolder && currentFolder != null) {
        MedicalFolderEditorSheet(
            existing = currentFolder,
            onDismiss = { showEditFolder = false },
            onSave = { name, desc, diagDate, doc, hosp, treat, dates, obs ->
                viewModel.saveFolder(currentFolder, name, desc, diagDate, doc, hosp, treat, dates, obs) {
                    showEditFolder = false
                }
            }
        )
    }

    if (showDeleteFolder && currentFolder != null) {
        ConfirmDeleteDialog(
            message = stringResource(R.string.medrec_delete_folder_warn),
            onConfirm = {
                viewModel.deleteFolder(currentFolder) {
                    showDeleteFolder = false
                    onBack()
                }
            },
            onDismiss = { showDeleteFolder = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicalFolderEditorSheet(
    existing: MedicalFolderEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String, desc: String, diagDate: Long?, doctor: String,
        hospital: String, treatment: String, importantDates: String, obs: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.conditionName ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var hasDate by remember { mutableStateOf(existing?.diagnosisDateMillis != null) }
    var diagDate by remember { mutableLongStateOf(existing?.diagnosisDateMillis ?: System.currentTimeMillis()) }
    var doctor by remember { mutableStateOf(existing?.doctorName ?: "") }
    var hospital by remember { mutableStateOf(existing?.hospitalName ?: "") }
    var treatment by remember { mutableStateOf(existing?.treatmentNotes ?: "") }
    var dates by remember { mutableStateOf(existing?.importantDates ?: "") }
    var obs by remember { mutableStateOf(existing?.observations ?: "") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.medrec_new_folder), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text(stringResource(R.string.medrec_condition_name) + " *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.label_description)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasDate, onCheckedChange = { hasDate = it })
                Text(stringResource(R.string.medrec_diagnosis_date))
            }
            if (hasDate) {
                DatePickerField(label = stringResource(R.string.medrec_diagnosis_date), dateMillis = diagDate, onDateSelected = { diagDate = it }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = doctor, onValueChange = { doctor = it }, label = { Text(stringResource(R.string.medrec_doctor_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = hospital, onValueChange = { hospital = it }, label = { Text(stringResource(R.string.medrec_hospital)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = treatment, onValueChange = { treatment = it }, label = { Text(stringResource(R.string.medrec_treatment_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dates, onValueChange = { dates = it }, label = { Text("Important Dates") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text(stringResource(R.string.medrec_observations)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (showError) Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = {
                    if (name.isBlank()) showError = true
                    else onSave(name, desc, if (hasDate) diagDate else null, doctor, hospital, treatment, dates, obs)
                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
