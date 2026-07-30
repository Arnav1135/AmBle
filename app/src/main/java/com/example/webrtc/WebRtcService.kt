package com.example.webrtc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoQualityProfile(
    val resolutionLabel: String,
    val width: Int,
    val height: Int,
    val maxFps: Int,
    val videoBitrateKbps: Int,
    val audioBitrateKbps: Int,
    val isHd: Boolean
)

data class NetworkHealthMetrics(
    val latencyMs: Int = 24,
    val jitterMs: Float = 1.2f,
    val packetLossPct: Float = 0.0f,
    val currentQualityProfile: VideoQualityProfile = WebRtcService.PROFILE_1080P
)

object WebRtcService {

    val PROFILE_1440P = VideoQualityProfile(
        resolutionLabel = "1440p Quad HD (2560x1440)",
        width = 2560,
        height = 1440,
        maxFps = 60,
        videoBitrateKbps = 6000,
        audioBitrateKbps = 256,
        isHd = true
    )

    val PROFILE_1080P = VideoQualityProfile(
        resolutionLabel = "1080p Full HD",
        width = 1920,
        height = 1080,
        maxFps = 60,
        videoBitrateKbps = 3500,
        audioBitrateKbps = 128,
        isHd = true
    )

    val PROFILE_720P = VideoQualityProfile(
        resolutionLabel = "720p HD",
        width = 1280,
        height = 720,
        maxFps = 45,
        videoBitrateKbps = 2000,
        audioBitrateKbps = 96,
        isHd = true
    )

    val PROFILE_480P = VideoQualityProfile(
        resolutionLabel = "480p SD",
        width = 854,
        height = 480,
        maxFps = 30,
        videoBitrateKbps = 1000,
        audioBitrateKbps = 48,
        isHd = false
    )

    val PROFILE_360P = VideoQualityProfile(
        resolutionLabel = "360p Low Latency",
        width = 640,
        height = 360,
        maxFps = 24,
        videoBitrateKbps = 500,
        audioBitrateKbps = 32,
        isHd = false
    )

    private val _networkHealth = MutableStateFlow(NetworkHealthMetrics())
    val networkHealth: StateFlow<NetworkHealthMetrics> = _networkHealth.asStateFlow()

    /**
     * WebRTC encoding quality adjustment hook based on real-time session latency.
     */
    fun adjustEncodingQualityForLatency(
        latencyMs: Int,
        jitterMs: Float = _networkHealth.value.jitterMs,
        packetLossPct: Float = _networkHealth.value.packetLossPct
    ): VideoQualityProfile {
        val selectedProfile = when {
            latencyMs < 20 -> PROFILE_1440P
            latencyMs in 20..45 -> PROFILE_1080P
            latencyMs in 46..95 -> PROFILE_720P
            latencyMs in 96..180 -> PROFILE_480P
            else -> PROFILE_360P
        }

        _networkHealth.value = NetworkHealthMetrics(
            latencyMs = latencyMs,
            jitterMs = jitterMs,
            packetLossPct = packetLossPct,
            currentQualityProfile = selectedProfile
        )

        return selectedProfile
    }

    fun registerQualityAdjustmentHook(onQualityChanged: (VideoQualityProfile) -> Unit): (Int, Float, Float) -> Unit {
        return { latency, jitter, packetLoss ->
            val profile = adjustEncodingQualityForLatency(latency, jitter, packetLoss)
            onQualityChanged(profile)
        }
    }
}
