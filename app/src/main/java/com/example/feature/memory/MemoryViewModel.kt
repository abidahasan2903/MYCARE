package com.example.feature.memory

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.AlbumAttachmentEntity
import com.example.core.database.entity.MemoryAlbumEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteFolderEntity
import com.example.core.database.entity.VoiceNoteEntity
import com.example.core.media.AudioPlaybackManager
import com.example.core.media.AudioRecorderManager
import com.example.core.storage.FileStorageManager
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MemoryUiState(
    val noteFolders: List<NoteFolderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val albums: List<MemoryAlbumEntity> = emptyList(),
    val voiceNotes: List<VoiceNoteEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFolderId: Long? = null,
    val favoritesOnly: Boolean = false
)

class MemoryViewModel(
    private val memoryRepo: MemoryRepository,
    val fileStorage: FileStorageManager,
    val audioRecorder: AudioRecorderManager,
    val audioPlayer: AudioPlaybackManager
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFolderId = MutableStateFlow<Long?>(null)
    private val favoritesOnly = MutableStateFlow(false)

    private val filterFlow = combine(searchQuery, selectedFolderId, favoritesOnly) { q, fId, fav ->
        Triple(q, fId, fav)
    }

    val uiState: StateFlow<MemoryUiState> = combine(
        memoryRepo.noteFolders,
        memoryRepo.allNotes,
        memoryRepo.allAlbums,
        memoryRepo.allVoiceNotes,
        filterFlow
    ) { folders, notes, albums, voices, filter ->
        val (q, fId, fav) = filter
        val filteredNotes = notes.filter { n ->
            val matchQ = q.isBlank() || n.title.contains(q, ignoreCase = true) || n.content.contains(q, ignoreCase = true)
            val matchFolder = fId == null || n.folderId == fId
            val matchFav = !fav || n.isFavorite
            matchQ && matchFolder && matchFav
        }
        val filteredVoices = voices.filter { v ->
            q.isBlank() || v.title.contains(q, ignoreCase = true) || v.description.contains(q, ignoreCase = true)
        }
        val filteredAlbums = albums.filter { a ->
            q.isBlank() || a.name.contains(q, ignoreCase = true) || a.location.contains(q, ignoreCase = true)
        }
        MemoryUiState(
            noteFolders = folders,
            notes = filteredNotes,
            albums = filteredAlbums,
            voiceNotes = filteredVoices,
            searchQuery = q,
            selectedFolderId = fId,
            favoritesOnly = fav
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MemoryUiState())

    fun setSearch(q: String) { searchQuery.value = q }
    fun setSelectedFolder(id: Long?) { selectedFolderId.value = id }
    fun setFavoritesOnly(fav: Boolean) { favoritesOnly.value = fav }

    fun createNoteFolder(name: String) {
        viewModelScope.launch { if (name.isNotBlank()) memoryRepo.saveNoteFolder(name) }
    }

    fun saveNote(existing: NoteEntity?, title: String, content: String, category: String, folderId: Long?, onDone: () -> Unit) {
        viewModelScope.launch {
            val note = (existing ?: NoteEntity(title = title, content = content)).copy(
                title = title.trim(),
                content = content.trim(),
                category = category.trim().ifBlank { "Personal" },
                folderId = folderId
            )
            memoryRepo.saveNote(note)
            onDone()
        }
    }

    fun toggleNoteFavorite(note: NoteEntity) {
        viewModelScope.launch { memoryRepo.saveNote(note.copy(isFavorite = !note.isFavorite)) }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { memoryRepo.deleteNote(note) }
    }

    fun observeAlbum(id: Long): Flow<MemoryAlbumEntity?> = memoryRepo.observeAlbum(id)
    fun observeAlbumAttachments(albumId: Long): Flow<List<AlbumAttachmentEntity>> =
        memoryRepo.observeAlbumAttachments(albumId)

    fun saveAlbum(
        existing: MemoryAlbumEntity?,
        name: String,
        description: String,
        location: String,
        coverUri: Uri?,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            var coverPath = existing?.coverPhotoPath
            if (coverUri != null) {
                fileStorage.importUriToPrivateStorage(coverUri, "IMAGE").onSuccess {
                    coverPath = it.filePath
                }
            }
            val album = (existing ?: MemoryAlbumEntity(name = name)).copy(
                name = name.trim(),
                description = description.trim(),
                location = location.trim(),
                coverPhotoPath = coverPath
            )
            val id = memoryRepo.saveAlbum(album)
            onSaved(id)
        }
    }

    fun deleteAlbum(album: MemoryAlbumEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            memoryRepo.deleteAlbum(album)
            onDone()
        }
    }

    fun addAlbumAttachment(albumId: Long, uri: Uri, onError: () -> Unit = {}) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri).onSuccess { imported ->
                memoryRepo.addAlbumAttachment(
                    AlbumAttachmentEntity(
                        albumId = albumId,
                        title = imported.displayName,
                        mediaType = imported.mediaType,
                        filePath = imported.filePath,
                        mimeType = imported.mimeType
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun setAlbumCoverFromPath(album: MemoryAlbumEntity, path: String) {
        viewModelScope.launch {
            memoryRepo.saveAlbum(album.copy(coverPhotoPath = path))
        }
    }

    fun renameAlbumAttachment(att: AlbumAttachmentEntity, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) memoryRepo.updateAlbumAttachment(att.copy(title = newTitle.trim()))
        }
    }

    fun deleteAlbumAttachment(att: AlbumAttachmentEntity) {
        viewModelScope.launch { memoryRepo.deleteAlbumAttachment(att) }
    }

    fun saveVoiceNote(title: String, description: String, filePath: String, durationMs: Long) {
        viewModelScope.launch {
            memoryRepo.saveVoiceNote(
                VoiceNoteEntity(
                    title = title.trim().ifBlank { "Voice Note" },
                    description = description.trim(),
                    filePath = filePath,
                    durationMillis = durationMs
                )
            )
        }
    }

    fun updateVoiceNote(voiceNote: VoiceNoteEntity, newTitle: String, newDesc: String) {
        viewModelScope.launch {
            memoryRepo.saveVoiceNote(
                voiceNote.copy(
                    title = newTitle.trim().ifBlank { voiceNote.title },
                    description = newDesc.trim()
                )
            )
        }
    }

    fun deleteVoiceNote(voiceNote: VoiceNoteEntity) {
        viewModelScope.launch {
            if (audioPlayer.state.value.activeSource == voiceNote.filePath) {
                audioPlayer.stop()
            }
            memoryRepo.deleteVoiceNote(voiceNote)
        }
    }
}
