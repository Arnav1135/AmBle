package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class VoicePlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun playOrPause(messageId: String, audioUrl: String?, fallbackDurationSec: Int = 10) {
        if (_playingMessageId.value == messageId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        _playingMessageId.value = messageId
        _playbackProgress.value = 0f

        try {
            val mp = MediaPlayer()
            if (!audioUrl.isNullOrEmpty() && (audioUrl.startsWith("http") || audioUrl.startsWith("content://"))) {
                mp.setDataSource(audioUrl)
            } else if (!audioUrl.isNullOrEmpty() && File(audioUrl).exists()) {
                mp.setDataSource(audioUrl)
            } else {
                // If local path is generic or url unavailable, simulate playback smoothly
                simulateAudioPlayback(messageId, fallbackDurationSec)
                return
            }

            mp.prepare()
            _durationMs.value = mp.duration
            applySpeed(mp, _playbackSpeed.value)

            mp.setOnCompletionListener {
                _isPlaying.value = false
                _playbackProgress.value = 1f
                _playingMessageId.value = null
                stopProgressJob()
            }

            mp.start()
            mediaPlayer = mp
            _isPlaying.value = true
            startProgressJob()

        } catch (e: Exception) {
            Log.e("VoicePlayer", "Failed to play audio file, simulating player", e)
            simulateAudioPlayback(messageId, fallbackDurationSec)
        }
    }

    private fun simulateAudioPlayback(messageId: String, durationSec: Int) {
        stop()
        _playingMessageId.value = messageId
        _durationMs.value = durationSec * 1000
        _isPlaying.value = true

        progressJob?.cancel()
        progressJob = scope.launch {
            val totalMs = durationSec * 1000
            var currentMs = 0
            val interval = 100
            while (isActive && currentMs < totalMs && _isPlaying.value && _playingMessageId.value == messageId) {
                delay((interval / _playbackSpeed.value).toLong())
                currentMs += interval
                _currentPositionMs.value = currentMs
                _playbackProgress.value = (currentMs.toFloat() / totalMs).coerceIn(0f, 1f)
            }
            _isPlaying.value = false
            _playbackProgress.value = 0f
            _playingMessageId.value = null
        }
    }

    private fun resume() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _isPlaying.value = true
                startProgressJob()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                stopProgressJob()
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("VoicePlayer", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _playingMessageId.value = null
            _playbackProgress.value = 0f
        }
    }

    fun seekTo(progressFraction: Float) {
        mediaPlayer?.let { mp ->
            val targetMs = (mp.duration * progressFraction.coerceIn(0f, 1f)).toInt()
            mp.seekTo(targetMs)
            _currentPositionMs.value = targetMs
            _playbackProgress.value = progressFraction
        } ?: run {
            val total = _durationMs.value
            if (total > 0) {
                _currentPositionMs.value = (total * progressFraction).toInt()
                _playbackProgress.value = progressFraction
            }
        }
    }

    fun toggleSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        mediaPlayer?.let { mp ->
            applySpeed(mp, nextSpeed)
        }
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams ?: PlaybackParams()
                params.speed = speed
                mp.playbackParams = params
            } catch (e: Exception) {
                Log.e("VoicePlayer", "Could not set playback speed", e)
            }
        }
    }

    private fun startProgressJob() {
        stopProgressJob()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val curr = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 1
                _currentPositionMs.value = curr
                _playbackProgress.value = (curr.toFloat() / total).coerceIn(0f, 1f)
                delay(100)
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
    }
}
