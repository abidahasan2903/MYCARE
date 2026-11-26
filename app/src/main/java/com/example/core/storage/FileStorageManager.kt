package com.example.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.core.database.MyCareDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class StorageBreakdown(
    val databaseBytes: Long = 0L,
    val photosBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val voiceBytes: Long = 0L,
    val ringtonesBytes: Long = 0L
) {
    val totalBytes: Long
        get() = databaseBytes + photosBytes + videosBytes + documentsBytes + voiceBytes + ringtonesBytes
}

data class ImportedFileResult(
    val filePath: String,
    val displayName: String,
    val mimeType: String,
    val mediaType: String // IMAGE, VIDEO, PDF, DOCUMENT, AUDIO
)

class FileStorageManager(private val context: Context) {

    private val mediaRoot = File(context.filesDir, "media").apply { mkdirs() }
    val photosDir = File(mediaRoot, "photos").apply { mkdirs() }
    val videosDir = File(mediaRoot, "videos").apply { mkdirs() }
    val documentsDir = File(mediaRoot, "documents").apply { mkdirs() }
    val ringtonesDir = File(mediaRoot, "ringtones").apply { mkdirs() }
    val voiceDir = File(context.filesDir, "voice_notes").apply { mkdirs() }

    suspend fun importUriToPrivateStorage(
        uri: Uri,
        forcedMediaType: String? = null
    ): Result<ImportedFileResult> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val detectedMime = resolver.getType(uri) ?: "*/*"
            val displayName = queryDisplayName(uri) ?: "file_${System.currentTimeMillis()}"
            val mediaType = forcedMediaType ?: classifyMediaType(detectedMime, displayName)
            val targetDir = when (mediaType) {
                "IMAGE" -> photosDir
                "VIDEO" -> videosDir
                "AUDIO" -> ringtonesDir
                else -> documentsDir
            }
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedMime)
                ?: displayName.substringAfterLast('.', "")
            val safeExt = if (ext.isNotBlank() && ext.length <= 6) ".$ext" else ""
            val destFile = File(targetDir, "${UUID.randomUUID()}$safeExt")

            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open selected file")

            ImportedFileResult(
                filePath = destFile.absolutePath,
                displayName = displayName,
                mimeType = detectedMime,
                mediaType = mediaType
            )
        }
    }

    fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }

    fun classifyMediaType(mimeType: String, fileName: String): String {
        val lowerName = fileName.lowercase()
        return when {
            mimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> "IMAGE"
            mimeType.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                lowerName.endsWith(".mov") -> "VIDEO"
            mimeType.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                lowerName.endsWith(".m4a") || lowerName.endsWith(".ogg") -> "AUDIO"
            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> "PDF"
            else -> "DOCUMENT"
        }
    }

    fun fileExists(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val f = File(path)
        return f.exists() && f.length() > 0L
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching {
            val f = File(path)
            if (f.exists()) f.delete() else false
        }.getOrDefault(false)
    }

    fun getFileProviderUri(path: String): Uri? {
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }

    fun openFileExternally(path: String, mimeType: String): Boolean {
        val uri = getFileProviderUri(path) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    suspend fun calculateStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(MyCareDatabase.DB_NAME)
        val dbWal = File(dbFile.path + "-wal")
        val dbShm = File(dbFile.path + "-shm")
        val dbBytes = (if (dbFile.exists()) dbFile.length() else 0L) +
            (if (dbWal.exists()) dbWal.length() else 0L) +
            (if (dbShm.exists()) dbShm.length() else 0L)

        StorageBreakdown(
            databaseBytes = dbBytes,
            photosBytes = dirSize(photosDir),
            videosBytes = dirSize(videosDir),
            documentsBytes = dirSize(documentsDir),
            voiceBytes = dirSize(voiceDir),
            ringtonesBytes = dirSize(ringtonesDir)
        )
    }

    suspend fun clearCategoryFiles(category: String) = withContext(Dispatchers.IO) {
        when (category) {
            "PHOTOS" -> photosDir.listFiles()?.forEach { it.delete() }
            "VIDEOS" -> videosDir.listFiles()?.forEach { it.delete() }
            "DOCUMENTS" -> documentsDir.listFiles()?.forEach { it.delete() }
            "VOICE" -> voiceDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }
}
