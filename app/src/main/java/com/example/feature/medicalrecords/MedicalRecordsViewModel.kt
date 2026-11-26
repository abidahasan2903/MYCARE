package com.example.feature.medicalrecords

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.MedicalDocumentEntity
import com.example.core.database.entity.MedicalFolderEntity
import com.example.core.storage.FileStorageManager
import com.example.data.repository.MedicalRecordsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedicalRecordsUiState(
    val folders: List<MedicalFolderEntity> = emptyList(),
    val recentDocuments: List<MedicalDocumentEntity> = emptyList(),
    val searchQuery: String = "",
    val sortByDateAsc: Boolean = false
)

class MedicalRecordsViewModel(
    private val repo: MedicalRecordsRepository,
    val fileStorage: FileStorageManager
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sortByDateAsc = MutableStateFlow(false)

    val uiState: StateFlow<MedicalRecordsUiState> = combine(
        repo.allFolders,
        repo.allDocuments,
        searchQuery,
        sortByDateAsc
    ) { folders, docs, q, asc ->
        val matchingFolderIdsByDocs = if (q.isBlank()) emptySet() else {
            docs.filter { it.title.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
                .map { it.folderId }
                .toSet()
        }
        val filteredFolders = folders.filter { f ->
            q.isBlank() ||
                f.conditionName.contains(q, ignoreCase = true) ||
                f.doctorName.contains(q, ignoreCase = true) ||
                f.hospitalName.contains(q, ignoreCase = true) ||
                f.id in matchingFolderIdsByDocs
        }.let { list ->
            if (asc) list.sortedBy { it.diagnosisDateMillis ?: it.createdAt }
            else list.sortedByDescending { it.diagnosisDateMillis ?: it.createdAt }
        }
        val filteredDocs = docs.filter { d ->
            q.isBlank() || d.title.contains(q, ignoreCase = true) || d.description.contains(q, ignoreCase = true)
        }
        MedicalRecordsUiState(
            folders = filteredFolders,
            recentDocuments = filteredDocs.take(10),
            searchQuery = q,
            sortByDateAsc = asc
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MedicalRecordsUiState())

    fun setSearch(q: String) { searchQuery.value = q }
    fun toggleSortOrder() { sortByDateAsc.value = !sortByDateAsc.value }

    fun observeFolder(id: Long): Flow<MedicalFolderEntity?> = repo.observeFolder(id)
    fun observeDocumentsForFolder(folderId: Long): Flow<List<MedicalDocumentEntity>> =
        repo.observeDocumentsForFolder(folderId)

    fun saveFolder(
        existing: MedicalFolderEntity?,
        conditionName: String,
        description: String,
        diagnosisDateMillis: Long?,
        doctorName: String,
        hospitalName: String,
        treatmentNotes: String,
        importantDates: String,
        observations: String,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val entity = (existing ?: MedicalFolderEntity(conditionName = conditionName)).copy(
                conditionName = conditionName.trim(),
                description = description.trim(),
                diagnosisDateMillis = diagnosisDateMillis,
                doctorName = doctorName.trim(),
                hospitalName = hospitalName.trim(),
                treatmentNotes = treatmentNotes.trim(),
                importantDates = importantDates.trim(),
                observations = observations.trim()
            )
            val id = repo.saveFolder(entity)
            onSaved(id)
        }
    }

    fun deleteFolder(folder: MedicalFolderEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteFolder(folder)
            onDone()
        }
    }

    fun addDocument(
        folderId: Long,
        uri: Uri,
        titleOverride: String,
        description: String,
        documentDateMillis: Long,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri).onSuccess { imported ->
                repo.saveDocument(
                    MedicalDocumentEntity(
                        folderId = folderId,
                        title = titleOverride.trim().ifBlank { imported.displayName },
                        description = description.trim(),
                        mediaType = imported.mediaType,
                        filePath = imported.filePath,
                        mimeType = imported.mimeType,
                        documentDateMillis = documentDateMillis
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun updateDocumentMeta(doc: MedicalDocumentEntity, title: String, description: String) {
        viewModelScope.launch {
            repo.saveDocument(
                doc.copy(
                    title = title.trim().ifBlank { doc.title },
                    description = description.trim()
                )
            )
        }
    }

    fun deleteDocument(doc: MedicalDocumentEntity) {
        viewModelScope.launch { repo.deleteDocument(doc) }
    }
}
