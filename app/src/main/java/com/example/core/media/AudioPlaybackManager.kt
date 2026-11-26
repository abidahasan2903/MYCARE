package com.example.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AudioPlayState(
    val activeSource: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

class AudioPlaybackManager(private val context: Context) {

    private var player: MediaPlayer? = null
    private val _state = MutableStateFlow(AudioPlayState())
    val state: StateFlow<AudioPlayState> = _state.asStateFlow()

    fun playFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            stop()
            return
        }
        if (_state.value.activeSource == filePath && player != null) {
            resume()
            return
        }
        stop()
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _state.value = AudioPlayState(
                        activeSource = null,
                        isPlaying = false,
                        positionMs = 0,
                        durationMs = 0
                    )
                    releasePlayerOnly()
                }
                start()
            }
            player = mp
            _state.value = AudioPlayState(
                activeSource = filePath,
                isPlaying = true,
                positionMs = 0,
                durationMs = mp.duration.coerceAtLeast(0)
            )
        }.onFailure {
            stop()
        }
    }

    fun playUri(uriString: String) {
        if (_state.value.activeSource == uriString && player != null) {
            resume()
            return
        }
        stop()
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                if (uriString.startsWith("/")) {
                    setDataSource(uriString)
                } else {
                    setDataSource(context, Uri.parse(uriString))
                }
                prepare()
                setOnCompletionListener {
                    stop()
                }
                start()
            }
            player = mp
            _state.value = AudioPlayState(
                activeSource = uriString,
                isPlaying = true,
                positionMs = 0,
                durationMs = mp.duration.coerceAtLeast(0)
            )
        }.onFailure {
            stop()
        }
    }

    fun pause() {
        runCatching {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        positionMs = it.currentPosition
                    )
                }
            }
        }
    }

    fun resume() {
        runCatching {
            player?.let {
                if (!it.isPlaying) {
                    it.start()
                    _state.value = _state.value.copy(isPlaying = true)
                }
            }
        }
    }

    fun refreshProgress() {
        runCatching {
            player?.let {
                if (it.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = it.currentPosition,
                        durationMs = it.duration.coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun seekTo(ms: Int) {
        runCatching {
            player?.seekTo(ms)
            _state.value = _state.value.copy(positionMs = ms)
        }
    }

    fun stop() {
        releasePlayerOnly()
        _state.value = AudioPlayState()
    }

    private fun releasePlayerOnly() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
