package com.example.core.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isPaused = false

    fun startRecording(): Result<File> = runCatching {
        stopAndReleaseInternal()
        val voiceDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
        val outputFile = File(voiceDir, "voice_${UUID.randomUUID()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        currentFile = outputFile
        isPaused = false
        outputFile
    }

    fun pauseRecording(): Boolean {
        val mr = recorder ?: return false
        if (isPaused) return true
        return runCatching {
            mr.pause()
            isPaused = true
            true
        }.getOrDefault(false)
    }

    fun resumeRecording(): Boolean {
        val mr = recorder ?: return false
        if (!isPaused) return true
        return runCatching {
            mr.resume()
            isPaused = false
            true
        }.getOrDefault(false)
    }

    fun stopAndSave(): File? {
        val file = currentFile
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
        isPaused = false
        return if (file != null && file.exists() && file.length() > 0L) {
            file
        } else {
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        val file = currentFile
        stopAndReleaseInternal()
        file?.delete()
    }

    fun getMaxAmplitude(): Int {
        return runCatching {
            if (!isPaused) recorder?.maxAmplitude ?: 0 else 0
        }.getOrDefault(0)
    }

    private fun stopAndReleaseInternal() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
        isPaused = false
    }
}
