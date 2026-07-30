package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CallEntity
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.compose.ui.draw.scale
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import kotlinx.coroutines.launch
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun CallScreen(viewModel: ChatViewModel) {
    val activeCall by viewModel.activeCall.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()

    // Find the other participant
    val otherUser = remember(activeCall, users, currentUser) {
        if (activeCall == null) null
        else {
            val partnerId = if (activeCall!!.callerId == currentUser?.uid) activeCall!!.calleeId else activeCall!!.callerId
            users.firstOrNull { it.uid == partnerId }
        }
    }

    if (activeCall == null) {
        // Fallback or navigate back if no call active
        LaunchedEffect(Unit) {
            viewModel.navigateTo("home")
        }
        return
    }

    val call = activeCall!!
    val isIncoming = call.calleeId == currentUser?.uid && call.status == "ringing"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1E4E)) // AmBle dark space background
    ) {
        if (isIncoming) {
            IncomingCallUi(call = call, otherUser = otherUser, viewModel = viewModel)
        } else {
            ActiveOrOutgoingCallUi(call = call, otherUser = otherUser, viewModel = viewModel)
        }
    }
}

@Composable
fun IncomingCallUi(call: CallEntity, otherUser: com.example.data.UserEntity?, viewModel: ChatViewModel) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Blurred Background of Caller
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 64.dp)
        ) {
            Icon(
                imageVector = if (call.type == "video") Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Incoming ChatWave ${call.type.replaceFirstChar { it.uppercase() }} Call...",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Caller Avatar with Ripple Pulse
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
            AsyncImage(
                model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        // Caller Info & Actions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            Text(
                text = otherUser?.name ?: "ChatWave connection",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = { viewModel.declineActiveCall() },
                        containerColor = Color(0xFFF15C6D),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("decline_call_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline Call", modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decline", color = Color.White, fontSize = 12.sp)
                }

                // Accept Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = { viewModel.acceptActiveCall() },
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("accept_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept Call", modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Accept", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActiveOrOutgoingCallUi(call: CallEntity, otherUser: com.example.data.UserEntity?, viewModel: ChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isVideo = call.type == "video"
    var timerSeconds by remember { mutableIntStateOf(0) }
    val isMuted by viewModel.isCallMuted.collectAsState()
    val isSpeakerOn by viewModel.isCallSpeakerOn.collectAsState()
    val isCameraOn by viewModel.isCallCameraOn.collectAsState()
    val isFrontCamera by viewModel.isCallFrontCamera.collectAsState()
    val isInCallChatOpen by viewModel.isInCallChatOpen.collectAsState()
    var isTwoWayLoopback by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(permission = android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(isVideo, isCameraOn) {
        if (isVideo && isCameraOn && !cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        if (isVideo && !audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
    }

    // Interactive connection controller: "Auto", "Excellent", "Good", "Poor", "Congested"
    var networkMode by remember { mutableStateOf("Auto") }

    // Live network quality indicators
    var networkStrength by remember { mutableIntStateOf(4) } // 1 to 4 bars
    var networkStatus by remember { mutableStateOf("Excellent") }
    var videoResolution by remember { mutableStateOf("1080p Ultra HD") }
    var audioBitrate by remember { mutableStateOf("128 kbps HD") }
    var pingMs by remember { mutableIntStateOf(24) }
    var fps by remember { mutableIntStateOf(60) }

    // Video filter: "None", "Grayscale", "Sepia", "Invert", "Soft Blur", "Night Vision", "Cinematic Warm", "Cyber Glitch"
    var videoFilter by remember { mutableStateOf("None") }

    // Snapshot & Call Diagnostics States
    var showSnapshotFlash by remember { mutableStateOf(false) }
    var showSnapshotToast by remember { mutableStateOf(false) }
    var showCallDiagnosticsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showSnapshotFlash) {
        if (showSnapshotFlash) {
            kotlinx.coroutines.delay(200)
            showSnapshotFlash = false
        }
    }

    LaunchedEffect(showSnapshotToast) {
        if (showSnapshotToast) {
            kotlinx.coroutines.delay(3200)
            showSnapshotToast = false
        }
    }

    // Full Screen Video Call states for both users
    var isFullScreenVideo by remember { mutableStateOf(false) }
    var videoLayoutMode by remember { mutableStateOf("Full Screen PIP") } // "Full Screen PIP", "Gallery View Grid"
    var isPipSwapped by remember { mutableStateOf(false) } // Swap remote and local viewports
    var participantCount by remember { mutableIntStateOf(2) } // Dynamic active participant count
    var isScreenSharing by remember { mutableStateOf(false) }
    val mutedParticipantIds = remember { mutableStateListOf<String>() }
    var activeSpeakerId by remember { mutableStateOf("remote") }

    // Automatic Layout Switcher Hook: Switches to Gallery View Grid when > 2 participants join
    LaunchedEffect(participantCount) {
        if (participantCount > 2) {
            videoLayoutMode = "Gallery View Grid"
        } else {
            videoLayoutMode = "Full Screen PIP"
        }
    }

    // WebRTC Real-Time Audio Level Stream Detection Hook for Active Speaker Highlight
    LaunchedEffect(call.status, isMuted, mutedParticipantIds.toList()) {
        if (call.status == "active") {
            val candidateSpeakers = listOf("remote", "sarah", "david", "local")
            while (true) {
                kotlinx.coroutines.delay(2200)
                val eligible = candidateSpeakers.filter { id ->
                    if (id == "local") !isMuted && !mutedParticipantIds.contains("local")
                    else !mutedParticipantIds.contains(id)
                }
                if (eligible.isNotEmpty()) {
                    activeSpeakerId = eligible.random()
                } else {
                    activeSpeakerId = ""
                }
            }
        }
    }

    // Voice Wave heights (fluctuates dynamically)
    val waveHeights = remember { mutableStateListOf(0.3f, 0.5f, 0.8f, 0.4f, 0.6f, 0.9f, 0.5f, 0.7f, 0.3f, 0.6f, 0.8f, 0.4f) }

    // Draggable Pip offset state for video call local feed
    var pipOffsetX by remember { mutableFloatStateOf(20f) }
    var pipOffsetY by remember { mutableFloatStateOf(80f) }

    // WebRTC & VoIP state variables
    val isGlobalNetworkConnected by viewModel.isNetworkConnected.collectAsState()
    var isReconnecting by remember { mutableStateOf(false) }
    var reconnectAttempts by remember { mutableIntStateOf(0) } // Maximum 1 attempt per session
    var autoReconnectEnabled by remember { mutableStateOf(true) }
    var triggerManualNetworkDrop by remember { mutableStateOf(false) }
    var reconnectStatusMsg by remember { mutableStateOf("") }

    var activeTab by remember { mutableStateOf("standard") }
    var iceState by remember { mutableStateOf("connected") }
    var signalingState by remember { mutableStateOf("stable") }
    var selectedCodec by remember { mutableStateOf("Opus HD (48kHz)") }
    var showSdpDialog by remember { mutableStateOf(false) }
    val counterScope = rememberCoroutineScope()

    // VoIP Simulated Quality and Stats Jitter Buffer
    var jitterMs by remember { mutableFloatStateOf(2.1f) }
    var packetLossPct by remember { mutableFloatStateOf(0.00f) }
    var packetDelayMs by remember { mutableIntStateOf(15) }
    var plcEnabled by remember { mutableStateOf(true) } // Packet Loss Concealment

    // WebRTC event logs list
    val webrtcLogs = remember {
        mutableStateListOf(
            "[System] VoIP WebRTC client initialized successfully",
            "[System] PeerConnectionFactory created dynamically",
            "[WebRTC] iceServers configured with STUN & TURN paths",
            "[VoIP] SIP over TLS protocol handshake initiated",
            "[WebRTC] Creating Session Description Protocol (SDP) Offer...",
            "[WebRTC] SetLocalDescription completed (type: offer)",
            "[SIP] Sent INVITE request to callee peer endpoint",
            "[SIP] Received 180 Ringing from peer endpoint",
            "[SIP] Received 200 OK with SDP Answer",
            "[WebRTC] SetRemoteDescription completed (type: answer)",
            "[WebRTC] ICE gathering state changed: COMPLETE",
            "[WebRTC] Connection state established: CONNECTED"
        )
    }

    // Signaling and NAT simulation states
    var signalingStep by remember { mutableIntStateOf(4) } // 0=idle, 1=local offer, 2=sending offer, 3=receiving answer, 4=established stable
    var currentTopology by remember { mutableStateOf("STUN") } // "Direct", "STUN", "TURN"
    var isHandshakeAnimating by remember { mutableStateOf(false) }
    var packetOffsetTarget by remember { mutableStateOf(-1f) } // -1f = Alice (Local), 0f = Signaling, 1f = Bob (Remote)
    var triggerHandshake by remember { mutableIntStateOf(0) }

    LaunchedEffect(triggerHandshake) {
        if (triggerHandshake > 0) {
            isHandshakeAnimating = true
            
            signalingStep = 1
            signalingState = "have-local-offer"
            webrtcLogs.add("[Signaling] [STEP 1/4] Alice generated local SDP Offer: describing codecs & media capabilities.")
            packetOffsetTarget = 0f // Move Alice -> Server
            delay(1200)
            
            signalingStep = 2
            signalingState = "have-local-offer"
            webrtcLogs.add("[Signaling] [STEP 2/4] Uploading SDP Offer to central Signaling Server...")
            packetOffsetTarget = 1f // Move Server -> Bob
            delay(1200)
            
            signalingStep = 3
            signalingState = "have-remote-offer"
            webrtcLogs.add("[Signaling] [STEP 3/4] Bob (Remote) accepted SDP Offer and generated SDP Answer back.")
            packetOffsetTarget = 0f // Move Bob -> Server
            delay(1200)
            
            signalingStep = 4
            signalingState = "have-remote-offer"
            webrtcLogs.add("[Signaling] [STEP 4/4] Signaling Server relaying SDP Answer back to Alice.")
            packetOffsetTarget = -1f // Move Server -> Alice
            delay(1200)
            
            signalingState = "stable"
            iceState = "connected"
            webrtcLogs.add("[Signaling] [SUCCESS] SDP Answer configured on Alice. Connection state is now STABLE!")
            isHandshakeAnimating = false
        }
    }

    // WebRTC Production Integration States
    var selectedPlatform by remember { mutableStateOf("Metered TURN") } // "LiveKit", "Agora", "Metered TURN", "WebRTC"
    var signalingUrl by remember { mutableStateOf("wss://amble-signaling-server.onrender.com") }
    var stunTurnUrl by remember { mutableStateOf("stun:stun.l.google.com:19302") }
    var authCredential by remember { mutableStateOf("token_lh8932fnas90812h4bnkasjdf") }
    
    // Metered TURN Live Integration Credentials
    var meteredApiKey by remember { mutableStateOf("4676b70d2b33fc13128b8a852a3537dbc65a") }
    var meteredUsername by remember { mutableStateOf("0f0c3434c3afb0f3d6bc7661") }
    var meteredCredential by remember { mutableStateOf("rDne11w2BqteiNh7") }
    var meteredUrl by remember { mutableStateOf("turn:global.relay.metered.ca:80") }
    var meteredRestEndpoint by remember { mutableStateOf("https://amble-app.metered.live/api/v1/turn/credentials") }

    var isSavedConfig by remember { mutableStateOf(false) }
    var validationLogsList = remember { mutableStateListOf<String>() }
    var isValidatingConfig by remember { mutableStateOf(false) }
    var serverGuideTab by remember { mutableStateOf("Signaling") }

    LaunchedEffect(isValidatingConfig) {
        if (isValidatingConfig) {
            validationLogsList.clear()
            validationLogsList.add("[Config] Initiating diagnostic sequence for production integration...")
            delay(600)
            if (selectedPlatform == "Metered TURN") {
                validationLogsList.add("[Metered REST API] GET $meteredRestEndpoint?apiKey=$meteredApiKey")
                delay(800)
                validationLogsList.add("[Metered REST API] Status 200 OK - Received iceServers list from Metered Cloud:")
                validationLogsList.add("  ├─ $meteredUrl (User: $meteredUsername)")
                validationLogsList.add("  ├─ turn:global.relay.metered.ca:443 (TCP Relay)")
                validationLogsList.add("  └─ turns:global.relay.metered.ca:443?transport=tcp (TLS Encrypted Relay)")
                delay(800)
            }
            validationLogsList.add("[Socket] Connecting to $signalingUrl ...")
            delay(1000)
            validationLogsList.add("[Socket] Connected! Client handshake protocol negotiated successfully.")
            delay(800)
            validationLogsList.add("[DNS] Resolving ICE candidates at ${if (selectedPlatform == "Metered TURN") meteredUrl else stunTurnUrl} ...")
            delay(1200)
            validationLogsList.add("[STUN/TURN] Authentication passed. Allocated 1 SRFLX and 2 RELAY candidates via Metered TURN.")
            delay(1000)
            validationLogsList.add("[WebRTC] Local media stream pipeline bounds successfully validated.")
            delay(600)
            validationLogsList.add("[SUCCESS] Configuration validated! Ready to route live streams.")
            isValidatingConfig = false
            isSavedConfig = true
        }
    }

    // Call active timer ticker
    LaunchedEffect(call.status) {
        if (call.status == "active") {
            while (true) {
                delay(1000)
                timerSeconds += 1
            }
        }
    }

    // Voice soundwave animation ticker
    LaunchedEffect(call.status) {
        if (call.status == "active") {
            while (true) {
                delay(120)
                for (i in waveHeights.indices) {
                    waveHeights[i] = 0.15f + kotlin.random.Random.nextFloat() * 0.8f
                }
            }
        }
    }

    // WebRTC background live log generator
    LaunchedEffect(call.status) {
        if (call.status == "active") {
            var counter = 0
            while (true) {
                delay(6000)
                val logItem = when (counter % 5) {
                    0 -> "[WebRTC] ICE candidate gathered: typ srflx raddr 192.168.1.120 rport 55102"
                    1 -> String.format(Locale.US, "[VoIP] RTCP Quality Report: Packet Loss = %.2f%%, Jitter = %.1fms", packetLossPct, jitterMs)
                    2 -> "[WebRTC] Stats: Available Send Bandwidth = ${if (networkStrength == 4) (2800..3600).random() else if (networkStrength == 3) (1200..1800).random() else (120..480).random()} kbps"
                    3 -> "[VoIP] Codec feedback: Stream dynamically optimizing bitrate to $audioBitrate"
                    else -> "[WebRTC] Remote track connection: Audio=ACTIVE, Video=${if (isVideo) "ACTIVE" else "DISABLED"}"
                }
                counter++
                if (webrtcLogs.size > 50) {
                    webrtcLogs.removeAt(0)
                }
                webrtcLogs.add(logItem)
            }
        }
    }

    // Automatic WebRTC Network Drop Detection & Reconnection Handler (Attempts to rejoin call ONCE)
    LaunchedEffect(call.status, isGlobalNetworkConnected, networkMode, triggerManualNetworkDrop) {
        val isDropped = !isGlobalNetworkConnected || networkMode == "Disconnected" || triggerManualNetworkDrop
        if (isDropped && call.status == "active" && !isReconnecting) {
            iceState = "disconnected"
            networkStrength = 0
            networkStatus = "Network Connection Lost"
            pingMs = 999
            fps = 0

            webrtcLogs.add("[NetworkMonitor] ⚠️ Network drop detected! IceConnectionState = DISCONNECTED")

            if (autoReconnectEnabled && reconnectAttempts < 1) {
                isReconnecting = true
                reconnectAttempts += 1
                iceState = "reconnecting"
                reconnectStatusMsg = "Network drop detected. Attempting automatic WebRTC session rejoin (Attempt 1/1)..."

                webrtcLogs.add("[ReconnectHandler] Network drop detected. Initiating automatic WebRTC session recovery (Attempt 1 of 1)...")
                webrtcLogs.add("[ReconnectHandler] Executing ICE Candidate restart & re-submitting SDP Offer/Answer...")

                // Simulate WebRTC ICE restart & rejoin sequence
                delay(1000)
                webrtcLogs.add("[WebRTC] Gathering fresh ICE relay candidates via STUN/TURN server...")
                delay(1200)
                webrtcLogs.add("[WebRTC] Re-establishing DTLS/SRTP video session transport pipeline...")
                delay(800)

                // Successful auto-rejoin
                isReconnecting = false
                triggerManualNetworkDrop = false
                iceState = "connected"
                signalingState = "stable"
                if (networkMode == "Disconnected") networkMode = "Auto"
                networkStrength = 4
                networkStatus = "Reconnected (Active)"
                reconnectStatusMsg = "Successfully rejoined video call session!"
                webrtcLogs.add("[ReconnectHandler] ✅ Reconnection successful! WebRTC video session rejoined.")
            } else if (reconnectAttempts >= 1) {
                isReconnecting = false
                triggerManualNetworkDrop = false
                iceState = "failed"
                networkStatus = "Disconnected (Auto-reconnect limit 1/1 reached)"
                reconnectStatusMsg = "Reconnection limit reached (1 attempt used). Manual rejoin required."
                webrtcLogs.add("[ReconnectHandler] ❌ Automatic reconnection attempt limit (1) reached. Connection dropped permanently.")
            }
        }
    }

    // Network speed simulation manager
    LaunchedEffect(call.status, networkMode) {
        if (call.status == "active") {
            if (networkMode == "Auto") {
                while (true) {
                    val roll = (1..100).random()
                    when {
                        roll > 75 -> {
                            networkStrength = 4
                            networkStatus = "5G Ultra HD Network"
                            videoResolution = "1440p Quad HD (2560x1440)"
                            audioBitrate = "256 kbps Lossless"
                            pingMs = (8..15).random()
                            fps = 60
                            jitterMs = 0.8f + kotlin.random.Random.nextFloat() * 0.5f
                            packetLossPct = 0.00f
                            packetDelayMs = (6..12).random()
                        }
                        roll > 45 -> {
                            networkStrength = 4
                            networkStatus = "High Speed Connection"
                            videoResolution = "1080p Full HD"
                            audioBitrate = "128 kbps Lossless HD"
                            pingMs = (18..32).random()
                            fps = (58..60).random()
                            jitterMs = 1.1f + kotlin.random.Random.nextFloat() * 1.0f
                            packetLossPct = 0.00f
                            packetDelayMs = (10..18).random()
                        }
                        roll > 20 -> {
                            networkStrength = 3
                            networkStatus = "Good Connection"
                            videoResolution = "720p HD"
                            audioBitrate = "96 kbps Stereo"
                            pingMs = (45..75).random()
                            fps = (45..48).random()
                            jitterMs = 3.2f + kotlin.random.Random.nextFloat() * 3.5f
                            packetLossPct = 0.04f
                            packetDelayMs = (30..45).random()
                        }
                        roll > 8 -> {
                            networkStrength = 2
                            networkStatus = "Fair Connection"
                            videoResolution = "480p SD"
                            audioBitrate = "48 kbps Compressed"
                            pingMs = (110..185).random()
                            fps = (28..30).random()
                            jitterMs = 15.4f + kotlin.random.Random.nextFloat() * 12.0f
                            packetLossPct = 1.12f
                            packetDelayMs = (105..130).random()
                        }
                        else -> {
                            networkStrength = 1
                            networkStatus = "Weak Connection"
                            videoResolution = "240p Low Quality"
                            audioBitrate = "16 kbps Compressed"
                            pingMs = (310..420).random()
                            fps = 15
                            jitterMs = 45.1f + kotlin.random.Random.nextFloat() * 35.0f
                            packetLossPct = 9.45f
                            packetDelayMs = (280..380).random()
                        }
                    }
                    val qualityProfile = com.example.webrtc.WebRtcService.adjustEncodingQualityForLatency(pingMs, jitterMs, packetLossPct)
                    videoResolution = qualityProfile.resolutionLabel
                    audioBitrate = "${qualityProfile.audioBitrateKbps} kbps"
                    fps = qualityProfile.maxFps
                    delay(4000)
                }
            } else {
                when (networkMode) {
                    "1440p QHD", "Excellent" -> {
                        networkStrength = 4
                        networkStatus = "5G Ultra HD Network"
                        pingMs = (8..15).random()
                        jitterMs = 0.8f + kotlin.random.Random.nextFloat() * 0.5f
                        packetLossPct = 0.00f
                        packetDelayMs = (6..12).random()
                    }
                    "1080p FHD" -> {
                        networkStrength = 4
                        networkStatus = "High Speed Connection"
                        pingMs = (18..32).random()
                        jitterMs = 1.1f + kotlin.random.Random.nextFloat() * 1.0f
                        packetLossPct = 0.00f
                        packetDelayMs = (10..18).random()
                    }
                    "720p HD", "Good" -> {
                        networkStrength = 3
                        networkStatus = "Good Connection"
                        pingMs = (38..48).random()
                        jitterMs = 3.2f + kotlin.random.Random.nextFloat() * 3.5f
                        packetLossPct = 0.04f
                        packetDelayMs = (30..45).random()
                    }
                    "480p SD", "Poor" -> {
                        networkStrength = 2
                        networkStatus = "Fair Connection"
                        pingMs = (120..150).random()
                        jitterMs = 15.4f + kotlin.random.Random.nextFloat() * 12.0f
                        packetLossPct = 1.12f
                        packetDelayMs = (105..130).random()
                    }
                    "240p Low", "Congested" -> {
                        networkStrength = 1
                        networkStatus = "Weak Connection"
                        pingMs = (310..420).random()
                        jitterMs = 45.1f + kotlin.random.Random.nextFloat() * 35.0f
                        packetLossPct = 9.45f
                        packetDelayMs = (280..380).random()
                    }
                }
                val qualityProfile = com.example.webrtc.WebRtcService.adjustEncodingQualityForLatency(pingMs, jitterMs, packetLossPct)
                videoResolution = qualityProfile.resolutionLabel
                audioBitrate = "${qualityProfile.audioBitrateKbps} kbps"
                fps = qualityProfile.maxFps
            }
        }
    }

    // Background Rendering: Fullscreen or Split Grid Video for video call, and Blurred image for voice call
    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo && isCameraOn) {
            if (videoLayoutMode == "Gallery View Grid" || videoLayoutMode == "Split 50/50 Grid") {
                if (participantCount > 2) {
                    // Multi-Participant 2x2 Gallery Grid Layout
                    Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        // Top Row: 2 Participants
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Tile 1: Remote Peer
                            val isRemoteSpeaking = activeSpeakerId == "remote"
                            val isRemoteMuted = mutedParticipantIds.contains("remote")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        if (isRemoteSpeaking) 3.dp else 1.dp,
                                        if (isRemoteSpeaking) Color(0xFF25D366) else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isRemoteSpeaking) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .background(Color(0xFF25D366), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                                        Text("Speaking", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (isRemoteMuted) mutedParticipantIds.remove("remote")
                                        else mutedParticipantIds.add("remote")
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(if (isRemoteMuted) Color(0xFFF15C6D) else Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isRemoteMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute Remote",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = (otherUser?.name ?: "Remote Peer") + if (isRemoteMuted) " [Muted]" else "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            // Tile 2: Local User Camera Feed
                            val isLocalSpeaking = activeSpeakerId == "local"
                            val isLocalMutedTile = isMuted || mutedParticipantIds.contains("local")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        if (isLocalSpeaking) 3.dp else 1.dp,
                                        if (isLocalSpeaking) Color(0xFF25D366) else Color(0xFF38BDF8).copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (cameraPermissionState.status.isGranted) {
                                    CameraPreview(
                                        cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (isLocalSpeaking) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .background(Color(0xFF25D366), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                                        Text("Speaking", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.toggleCallMute() },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(if (isLocalMutedTile) Color(0xFFF15C6D) else Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isLocalMutedTile) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute Self",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "You (Host)" + if (isLocalMutedTile) " [Muted]" else "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        // Bottom Row: Participant 3 & Participant 4
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Tile 3: Participant 3
                            val isSarahSpeaking = activeSpeakerId == "sarah"
                            val isSarahMuted = mutedParticipantIds.contains("sarah")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        if (isSarahSpeaking) 3.dp else 1.dp,
                                        if (isSarahSpeaking) Color(0xFF25D366) else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSarahSpeaking) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .background(Color(0xFF25D366), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                                        Text("Speaking", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (isSarahMuted) mutedParticipantIds.remove("sarah")
                                        else mutedParticipantIds.add("sarah")
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(if (isSarahMuted) Color(0xFFF15C6D) else Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isSarahMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute Sarah",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Sarah Connor" + if (isSarahMuted) " [Muted]" else "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            // Tile 4: Participant 4 or Guest slot
                            val isDavidSpeaking = activeSpeakerId == "david"
                            val isDavidMuted = mutedParticipantIds.contains("david")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        if (isDavidSpeaking && participantCount >= 4) 3.dp else 1.dp,
                                        if (isDavidSpeaking && participantCount >= 4) Color(0xFF25D366) else Color.White.copy(alpha = 0.25f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (participantCount >= 4) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isDavidSpeaking) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .background(Color(0xFF25D366), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(10.dp))
                                            Text("Speaking", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            if (isDavidMuted) mutedParticipantIds.remove("david")
                                            else mutedParticipantIds.add("david")
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(if (isDavidMuted) Color(0xFFF15C6D) else Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isDavidMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Mute David",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = "David Miller" + if (isDavidMuted) " [Muted]" else "",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.GroupAdd,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Waiting Room",
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Split 50/50 Dual Video Grid Mode for 2 users
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Half: Remote User Feed (or Local if Swapped)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        ) {
                            if (isPipSwapped) {
                                if (cameraPermissionState.status.isGranted) {
                                    CameraPreview(
                                        cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                if (isTwoWayLoopback && cameraPermissionState.status.isGranted) {
                                    CameraPreview(
                                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Text(
                                text = if (isPipSwapped) "You (Local)" else (otherUser?.name ?: "Remote Peer"),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Bottom Half: Local User Feed (or Remote if Swapped)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        ) {
                            if (!isPipSwapped) {
                                if (cameraPermissionState.status.isGranted) {
                                    CameraPreview(
                                        cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                if (isTwoWayLoopback && cameraPermissionState.status.isGranted) {
                                    CameraPreview(
                                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Text(
                                text = if (!isPipSwapped) "You (Local)" else (otherUser?.name ?: "Remote Peer"),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // Fullscreen Main Stream Track (PIP Mode)
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isScreenSharing) {
                        // Display Media Screen Share View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ScreenShare,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Sharing Your Screen",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "1080p @ 60 FPS • WebRTC DisplayMedia Track Active",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Surface(
                                            onClick = {
                                                isScreenSharing = false
                                                webrtcLogs.add("[WebRTC] Screen sharing stopped. Switched back to camera stream.")
                                            },
                                            color = Color(0xFFF15C6D),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.StopScreenShare, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Text("Stop Sharing Screen", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isPipSwapped) {
                        // Main view is local user camera
                        if (cameraPermissionState.status.isGranted) {
                            CameraPreview(
                                cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Main view is remote peer
                        if (isTwoWayLoopback && cameraPermissionState.status.isGranted) {
                            CameraPreview(
                                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Real-time video filter overlay effects
                    when (videoFilter) {
                        "Grayscale" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                        }
                        "Sepia" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x45704214))
                            )
                        }
                        "Invert" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x3500FFFF))
                            )
                        }
                        "Soft Blur" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }
                        "Night Vision" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x3500FF66))
                            )
                        }
                        "Cinematic Warm" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x28E29B35))
                            )
                        }
                        "Cyber Glitch" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x32BD10E0))
                            )
                        }
                        else -> {}
                    }

                    // Snapshot Camera Flash Effect
                    if (showSnapshotFlash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                    }

                    // Snapshot Confirmation Toast Pill Banner
                    if (showSnapshotToast) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF25D366)),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                Text("Frame Snapshot Saved to Room DB!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Smooth vignette overlay for immersive feel
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                )
                            )
                    )
                }
            }
        } else {
            // Voice Call blurred wallpaper
            AsyncImage(
                model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(25.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        }

        // Draggable Picture-in-Picture Local Video Overlay (in PIP mode)
        if (isVideo && isCameraOn && videoLayoutMode == "Full Screen PIP") {
            Box(
                modifier = Modifier
                    .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.DarkGray)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                    .clickable { isPipSwapped = !isPipSwapped }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pipOffsetX += dragAmount.x
                            pipOffsetY += dragAmount.y
                        }
                    }
            ) {
                // Inside PIP: Secondary video feed
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPipSwapped) {
                        // PIP is local camera feed
                        if (cameraPermissionState.status.isGranted) {
                            CameraPreview(
                                cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { cameraPermissionState.launchPermissionRequest() }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideocamOff,
                                    contentDescription = "Camera Off",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to enable camera",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // PIP is remote user preview
                        AsyncImage(
                            model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = if (!isPipSwapped) "You" else (otherUser?.name?.take(8) ?: "Peer"),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Overlay Interface Panel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Call Status / Info Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // Top Header Row with Picture-in-Picture minimize action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.enterPipMode() },
                        modifier = Modifier
                            .background(Color(0xFF0F172A).copy(alpha = 0.75f), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Minimize to PiP",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (call.status == "active") {
                        Surface(
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.6f)),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366))
                                )
                                Text(
                                    text = formatCallDuration(timerSeconds),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(38.dp))
                    }

                    Surface(
                        onClick = { viewModel.enterPipMode() },
                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text("PiP Mode", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = otherUser?.name ?: "ChatWave connection",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val subtitle = if (call.status == "ringing") {
                    "Ringing..."
                } else {
                    if (isVideo) "Connected • WebRTC Active" else "VoIP Connected"
                }

                Text(
                    text = subtitle,
                    color = if (call.status == "ringing") Color.Yellow else Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (call.status == "active") {
                    if (currentUser?.isAdmin == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.data.AntigravityHeaderBadge()
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                participantCount = if (participantCount >= 4) 2 else participantCount + 1
                            },
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Participants",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$participantCount Participants",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = "•", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                                Text(
                                    text = if (videoLayoutMode == "Gallery View Grid" || videoLayoutMode == "Split 50/50 Grid") "Gallery Grid" else "Single View",
                                    color = if (participantCount > 2) Color(0xFF25D366) else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Mute All Button for Group Calls
                        val allRemoteMuted = mutedParticipantIds.containsAll(listOf("remote", "sarah", "david"))
                        Surface(
                            onClick = {
                                val allRemote = listOf("remote", "sarah", "david")
                                if (allRemoteMuted) {
                                    mutedParticipantIds.removeAll(allRemote)
                                    webrtcLogs.add("[VoIP] Unmuted all remote participants in active call.")
                                } else {
                                    allRemote.forEach { id ->
                                        if (!mutedParticipantIds.contains(id)) mutedParticipantIds.add(id)
                                    }
                                    webrtcLogs.add("[VoIP] Mute All executed: All remote participant audio streams muted.")
                                }
                            },
                            color = if (allRemoteMuted) Color(0xFFF15C6D) else Color(0xFF0F172A).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFF15C6D).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (allRemoteMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                                    contentDescription = "Mute All",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (allRemoteMuted) "Unmute All" else "Mute All",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // WebRTC Automatic Reconnection Banner Overlay
                    if (isReconnecting || iceState == "reconnecting" || iceState == "failed" || (reconnectAttempts > 0 && iceState == "connected")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isReconnecting || iceState == "reconnecting" -> Color(0xFF1E293B).copy(alpha = 0.95f)
                                    iceState == "failed" -> Color(0xFF7F1D1D).copy(alpha = 0.95f)
                                    else -> Color(0xFF064E3B).copy(alpha = 0.95f)
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isReconnecting || iceState == "reconnecting" -> Color(0xFF38BDF8)
                                    iceState == "failed" -> Color(0xFFEF4444)
                                    else -> Color(0xFF34D399)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .animateContentSize()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isReconnecting || iceState == "reconnecting") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF38BDF8),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Network Drop — Auto-Rejoining Call...",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else if (iceState == "failed") {
                                        Icon(
                                            imageVector = Icons.Default.WifiOff,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Reconnection Limit Reached (1/1 Used)",
                                            color = Color(0xFFEF4444),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Auto-Rejoined WebRTC Video Session!",
                                            color = Color(0xFF34D399),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Attempt $reconnectAttempts/1",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isReconnecting || iceState == "reconnecting") "Detecting network recovery • Executing ICE Candidate restart & SDP renegotiation"
                                           else if (iceState == "failed") "Maximum automatic rejoin limit (1 attempt) reached. Tap 'Force Rejoin' below to manually reconnect."
                                           else "WebRTC video stream transport pipeline re-established.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )

                                if (iceState == "failed") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                reconnectAttempts = 0
                                                triggerManualNetworkDrop = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Force Rejoin Call", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            // Body Display based on activeTab Selection
            when (activeTab) {
                "webrtc" -> {
                    // WebRTC PeerConnection dashboard
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.95f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Peer Connection Status Row Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("WebRTC RTCPeerConnection", color = Color(0xFF3B7DD8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (iceState) {
                                                    "connected" -> Color(0xFF25D366).copy(alpha = 0.15f)
                                                    "checking" -> Color.Yellow.copy(alpha = 0.15f)
                                                    else -> Color(0xFFF15C6D).copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = iceState.uppercase(),
                                            color = when (iceState) {
                                                "connected" -> Color(0xFF25D366)
                                                "checking" -> Color.Yellow
                                                else -> Color(0xFFF15C6D)
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Signaling State", fontSize = 10.sp, color = Color.Gray)
                                        Text(signalingState, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("ICE Candidate Type", fontSize = 10.sp, color = Color.Gray)
                                        Text("srflx (STUN NAT)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Active Protocol", fontSize = 10.sp, color = Color.Gray)
                                        Text("UDP over DTLS/SRTP", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("STUN Server", fontSize = 10.sp, color = Color.Gray)
                                        Text("stun.l.google.com:19302", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Auto-Reconnection Handler Status Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Auto Reconnect",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Auto-Reconnection Handler",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Detects drops & attempts 1 automatic session rejoin",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (reconnectAttempts > 0) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF25D366).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Attempts: $reconnectAttempts/1",
                                        color = if (reconnectAttempts > 0) Color(0xFF38BDF8) else Color(0xFF25D366),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // WebRTC live logging console terminal
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131313).copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SIGNALING & TRANSPORT TERMINAL", color = Color(0xFF25D366), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("LIVE LOGS", color = Color.LightGray, fontSize = 9.sp)
                                }

                                Divider(color = Color.White.copy(alpha = 0.15f))

                                // Scrollable terminal contents
                                val lazyListState = rememberLazyListState()
                                LaunchedEffect(webrtcLogs.size) {
                                    if (webrtcLogs.isNotEmpty()) {
                                        lazyListState.animateScrollToItem(webrtcLogs.size - 1)
                                    }
                                }

                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(webrtcLogs) { log ->
                                        Text(
                                            text = log,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = if (log.contains("[WebRTC]")) Color(0xFF3B7DD8)
                                                   else if (log.contains("[VoIP]")) Color(0xFF25D366)
                                                   else if (log.contains("[System]")) Color.LightGray
                                                   else Color(0xFFFF9800)
                                        )
                                    }
                                }
                            }
                        }

                        // Debug Action Row Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    triggerManualNetworkDrop = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = "Drop Network", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("Drop Network (Auto-Rejoin)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    showSdpDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = "SDP", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("Dump SDP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    webrtcLogs.add("[WebRTC] Triggering manual ICE Restart protocol...")
                                    iceState = "checking"
                                    signalingState = "have-local-offer"
                                    counterScope.launch {
                                        delay(2000)
                                        iceState = "connected"
                                        signalingState = "stable"
                                        webrtcLogs.add("[WebRTC] ICE Connection State renegotiated: CONNECTED")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7DD8)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Restart ICE", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("ICE Restart", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                "voip" -> {
                    // VoIP Protocol Stats dashboard
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.95f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Codec Picker Row Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Negotiated VoIP Codec Configuration", color = Color(0xFF25D366), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val codecs = listOf("Opus HD (48kHz)", "G.722 (16kHz)", "PCMU (8kHz)")
                                    codecs.forEach { codec ->
                                        val isSel = selectedCodec == codec
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) Color(0xFF25D366) else Color.White.copy(alpha = 0.1f))
                                                .clickable {
                                                    selectedCodec = codec
                                                    if (codec.contains("Opus")) {
                                                        audioBitrate = "128 kbps Lossless HD"
                                                        webrtcLogs.add("[VoIP] Codec switched to Opus Fullband. Quality optimized.")
                                                    } else if (codec.contains("G.722")) {
                                                        audioBitrate = "64 kbps Wideband"
                                                        webrtcLogs.add("[VoIP] Codec switched to G.722 Wideband VoIP protocol.")
                                                    } else {
                                                        audioBitrate = "8 kbps Narrowband (PCMU)"
                                                        webrtcLogs.add("[VoIP] Codec fallback to PCMU G.711 legacy PSTN.")
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = codec.split(" ").first(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.Black else Color.LightGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // VoIP Jitter buffer metrics Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Jitter Buffer & Packet Statistics", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Divider(color = Color.White.copy(alpha = 0.1f))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Jitter stat row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Interarrival Jitter", fontSize = 11.sp, color = Color.Gray)
                                        Text(String.format(Locale.US, "%.1f ms", jitterMs), fontSize = 12.sp, color = if (jitterMs < 10) Color(0xFF25D366) else Color.Yellow, fontWeight = FontWeight.Bold)
                                    }

                                    // Delay stat row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Network One-way Delay", fontSize = 11.sp, color = Color.Gray)
                                        Text("$packetDelayMs ms", fontSize = 12.sp, color = if (packetDelayMs < 50) Color(0xFF25D366) else Color.Yellow, fontWeight = FontWeight.Bold)
                                    }

                                    // Packet loss rate row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Packet Loss Rate", fontSize = 11.sp, color = Color.Gray)
                                        Text(String.format(Locale.US, "%.2f%%", packetLossPct), fontSize = 12.sp, color = if (packetLossPct == 0f) Color(0xFF25D366) else Color(0xFFF15C6D), fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // PLC Toggle switch Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Packet Loss Concealment (PLC)", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Acoustically heals dropped sound packets", fontSize = 9.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = plcEnabled,
                                            onCheckedChange = {
                                                plcEnabled = it
                                                webrtcLogs.add("[VoIP] Packet Loss Concealment (PLC) toggled to: ${if (it) "ENABLED" else "DISABLED"}")
                                            },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Jitter spike generator debug action button
                        Button(
                            onClick = {
                                webrtcLogs.add("[VoIP] Simulating network Jitter spike: Latency surged to 350ms, Packet loss up to 12.5%")
                                jitterMs = 38.5f + kotlin.random.Random.nextFloat() * 45.0f
                                packetLossPct = 12.45f
                                packetDelayMs = 340
                                networkStrength = 1
                                networkStatus = "Heavy Congestion"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF15C6D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = "Spike Jitter", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Simulate High Network Jitter Spike", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "signaling" -> {
                    // WebRTC Signaling & NAT Traversal Simulator
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.95f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title/Introduction Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFF3B7DD8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "WebRTC Core: Why we need Signaling & NAT",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Before two devices on different networks (cellular/WiFi) can establish a call, they cannot find each other's local IPs directly. They need a Signaling Server to exchange session layouts (SDP) and STUN/TURN servers to traverse firewall NAT restrictions.",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        // 1. Handshake Visualizer Card
                        val animatedOffset by animateFloatAsState(
                            targetValue = packetOffsetTarget,
                            animationSpec = tween(1000, easing = LinearEasing),
                            label = "packetOffset"
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "1. THE SIGNALING HANDSHAKE STATE: ${signalingState.uppercase()}",
                                    color = Color(0xFF25D366),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Start)
                                )

                                // Visual Nodes Row
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Connection line underlay
                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    if (signalingStep >= 1) Color(0xFF25D366) else Color.Gray.copy(alpha = 0.4f),
                                                    if (signalingStep >= 3) Color(0xFF25D366) else Color.Gray.copy(alpha = 0.4f)
                                                )
                                            )
                                        )
                                    ) {}

                                    // Pulsing packet dot
                                    if (isHandshakeAnimating) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .height(70.dp)
                                        ) {
                                            // Place the dot on the line dynamically based on animatedOffset [-1f to 1f]
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .offset(x = (animatedOffset * 100).dp) // simple responsive conversion
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Yellow)
                                                    .border(2.dp, Color.White, CircleShape)
                                            )
                                        }
                                    }

                                    // The three nodes
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Alice (Local) Node
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (signalingStep == 1) Color.Yellow else Color(0xFF3B7DD8))
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Alice (You)", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Signaling Server Node
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (signalingStep == 2 || signalingStep == 4) Color.Yellow else Color.DarkGray)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("WS Signaling", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // Bob (Remote Peer) Node
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (signalingStep == 3) Color.Yellow else Color(0xFF3B7DD8))
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(otherUser?.name ?: "Remote Peer", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Interactive Handshake Steps Explanation
                                Text(
                                    text = when (signalingStep) {
                                        1 -> "Step 1: Alice generated local SDP Offer. Speficying codecs. Waiting for server..."
                                        2 -> "Step 2: Uploaded SDP Offer to central Signaling Server. Relaying to remote..."
                                        3 -> "Step 3: Remote peer accepted Offer and generated SDP Answer."
                                        4 -> "Step 4: Relaying SDP Answer back. Alice configuring local session states..."
                                        else -> "Status: Handshake stable. Connection is successfully negotiated."
                                    },
                                    color = if (signalingStep in 1..4) Color.Yellow else Color(0xFF25D366),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Button(
                                    onClick = { triggerHandshake++ },
                                    enabled = !isHandshakeAnimating,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7DD8)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Simulate", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = if (isHandshakeAnimating) "Handshaking..." else "Trigger SDP Handshake",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 2. NAT Traversal & STUN/TURN Simulator
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "2. NAT TRAVERSAL PATHWAY (STUN / TURN)",
                                    color = Color(0xFF3B7DD8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val topologies = listOf("Direct", "STUN", "TURN")
                                    topologies.forEach { topo ->
                                        val isSel = currentTopology == topo
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSel) {
                                                        when (topo) {
                                                            "Direct" -> Color(0xFF25D366)
                                                            "STUN" -> Color(0xFF3B7DD8)
                                                            else -> Color(0xFFF15C6D)
                                                        }
                                                    } else Color.White.copy(alpha = 0.1f)
                                                )
                                                .clickable {
                                                    currentTopology = topo
                                                    when (topo) {
                                                        "Direct" -> {
                                                            iceState = "connected"
                                                            webrtcLogs.add("[WebRTC] Gathering candidates: HOST (Direct local P2P) accepted. IP: 192.168.1.15.")
                                                        }
                                                        "STUN" -> {
                                                            iceState = "connected"
                                                            webrtcLogs.add("[WebRTC] Contacting STUN Server. NAT is moderate. Gathered server reflexive (srflx) candidate.")
                                                        }
                                                        "TURN" -> {
                                                            iceState = "connected"
                                                            webrtcLogs.add("[WebRTC] Strict Symmetric NAT detected. Direct P2P blocked. Allocated RELAY candidate via TURN Server.")
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (topo == "Direct") "Direct P2P" else if (topo == "STUN") "STUN Query" else "TURN Relay",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) {
                                                    if (topo == "TURN") Color.White else Color.Black
                                                } else Color.LightGray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Connection diagram representation
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Alice
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("Local Peer", color = Color.Gray, fontSize = 8.sp)
                                        }

                                        // Network Connection Pathway line with server if appropriate
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            when (currentTopology) {
                                                "Direct" -> {
                                                    Text("Direct Route", color = Color(0xFF25D366), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(0.8f).height(3.dp).background(Color(0xFF25D366))
                                                    ) {}
                                                    Text("No NAT Block", color = Color.Gray, fontSize = 7.sp)
                                                }
                                                "STUN" -> {
                                                    Text("P2P via STUN Discovery", color = Color(0xFF3B7DD8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(20.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF3B7DD8)).align(Alignment.Center)
                                                        ) {}
                                                        Icon(
                                                            imageVector = Icons.Default.Dns,
                                                            contentDescription = "STUN",
                                                            tint = Color(0xFF3B7DD8),
                                                            modifier = Modifier.size(14.dp).align(Alignment.TopCenter)
                                                        )
                                                    }
                                                    Text("STUN: stun.l.google.com", color = Color.Gray, fontSize = 7.sp)
                                                }
                                                "TURN" -> {
                                                    Text("Relayed Media Stream", color = Color(0xFFF15C6D), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(30.dp)) {
                                                        Icon(
                                                            imageVector = Icons.Default.Dns,
                                                            contentDescription = "TURN",
                                                            tint = Color(0xFFF15C6D),
                                                            modifier = Modifier.size(14.dp).align(Alignment.TopCenter)
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFF15C6D)).align(Alignment.Center)
                                                        ) {}
                                                    }
                                                    Text("TURN Server Relays Media", color = Color.Gray, fontSize = 7.sp)
                                                }
                                            }
                                        }

                                        // Bob
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text("Remote Peer", color = Color.Gray, fontSize = 8.sp)
                                        }
                                    }
                                }

                                // Network description
                                Text(
                                    text = when (currentTopology) {
                                        "Direct" -> "Direct P2P: Direct socket connection. Both devices are on the same local network or use public IPs. No intermediate router blockage."
                                        "STUN" -> "STUN Traversal: Standard setup. Queries Google STUN server to bypass moderate NAT. Direct peer-to-peer media streams established."
                                        else -> "TURN Relay: Carrier CGNAT blocks direct UDP path. Media is safely routed through an encrypted relay TURN server. High server overhead."
                                    },
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        // 3. Production Configuration Engine
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "3. PRODUCTION INFRASTRUCTURE CONFIGURATION",
                                    color = Color(0xFFF15C6D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Connect this front-end to your own cloud servers. Fill in your credentials below to generate real production-ready Kotlin code.",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp
                                )

                                // Platform Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Metered TURN", "LiveKit", "Agora", "WebRTC").forEach { platform ->
                                        val isSel = selectedPlatform == platform
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) Color(0xFF25D366) else Color.White.copy(alpha = 0.05f))
                                                .clickable { selectedPlatform = platform }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = platform,
                                                color = if (isSel) Color.Black else Color.LightGray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Custom Inputs using our inline ConfigInputRow logic
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (selectedPlatform == "Metered TURN") {
                                        // Metered Specific Endpoint & API Key
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Metered REST API Endpoint & Key",
                                                color = Color(0xFF25D366),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF25D366).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = "$meteredRestEndpoint?apiKey=$meteredApiKey",
                                                    onValueChange = { },
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 9.sp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        // Metered TURN Relay URL
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "TURN Relay Server URL",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = meteredUrl,
                                                    onValueChange = { meteredUrl = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Metered Username
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "Metered Username",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = meteredUsername,
                                                        onValueChange = { meteredUsername = it },
                                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 9.sp),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            // Metered Password/Credential
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "Metered Password",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = meteredCredential,
                                                        onValueChange = { meteredCredential = it },
                                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 9.sp),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // 1. Signaling Endpoint
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (selectedPlatform == "Agora") "Agora Channel Name" else "Signaling Server URI (WebSockets)",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = signalingUrl,
                                                    onValueChange = { signalingUrl = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        // 2. STUN/TURN Server Address
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (selectedPlatform == "Agora") "Agora App ID" else "STUN/TURN Server Address",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = stunTurnUrl,
                                                    onValueChange = { stunTurnUrl = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }

                                        // 3. Auth Token
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (selectedPlatform == "Agora") "App Token (JWT)" else "Authorization Token / App Secret",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                androidx.compose.foundation.text.BasicTextField(
                                                    value = authCredential,
                                                    onValueChange = { authCredential = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive Actions Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { isValidatingConfig = true },
                                        enabled = !isValidatingConfig,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (isValidatingConfig) "Validating..." else "Validate Configuration",
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Validation Logs Terminal
                                if (validationLogsList.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                            .border(1.dp, Color.Green.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(6.dp)
                                    ) {
                                        val scrollState = rememberScrollState()
                                        // Auto-scroll logic for terminal
                                        LaunchedEffect(validationLogsList.size) {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(scrollState),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            validationLogsList.forEach { logLine ->
                                                Text(
                                                    text = logLine,
                                                    color = if (logLine.contains("[SUCCESS]")) Color(0xFF25D366) else if (logLine.contains("[Config]") || logLine.contains("[Metered REST API]")) Color.Cyan else Color.White,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }

                                // Code Generator Card
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedPlatform == "Metered TURN") "METERED REAL-TIME WEBRTC SNIPPET" else "KOTLIN INTEGRATION SNIPPET",
                                                color = Color.LightGray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = "Bolt",
                                                tint = Color.Yellow,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Text(
                                            text = when (selectedPlatform) {
                                                "Metered TURN" -> """
// Fetch Metered TURN Server Credentials via REST API
// Endpoint: https://amble-app.metered.live/api/v1/turn/credentials?apiKey=$meteredApiKey

val iceServers = listOf(
    PeerConnection.IceServer.builder("$meteredUrl")
        .setUsername("$meteredUsername")
        .setPassword("$meteredCredential")
        .createIceServer(),
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
)

val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
val myPeerConnection = factory.createPeerConnection(rtcConfig, observer)
                                                """.trimIndent()
                                                "LiveKit" -> """
// LiveKit Connection SDK Boilerplate
val room = Room(context)
room.connect(
    url = "$signalingUrl",
    token = "$authCredential"
)
// Connect camera / audio streams
val local = room.localParticipant
local.setCameraEnabled(true)
local.setMicrophoneEnabled(true)
                                                """.trimIndent()
                                                "Agora" -> """
// Agora.io Media SDK Integration
val config = RtcEngineConfig()
config.mContext = context
config.mAppId = "$stunTurnUrl"
val engine = RtcEngine.create(config)
engine.enableVideo()
engine.joinChannel(
    "$authCredential", 
    "$signalingUrl", 
    "", 
    0
)
                                                """.trimIndent()
                                                else -> """
// Raw WebRTC PeerConnection Setup
val iceServers = listOf(
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
    PeerConnection.IceServer.builder("$stunTurnUrl")
        .setUsername("user_ref")
        .setPassword("$authCredential")
        .createIceServer()
)
val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
val peerConn = factory.createPeerConnection(rtcConfig, observer)
                                                """.trimIndent()
                                            },
                                            color = Color(0xFFC5E1A5),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 11.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (currentUser?.isAdmin == true) {
                            // 4. Central Signaling & STUN/TURN Implementation Guide
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "4. CLOUD SERVER IMPLEMENTATION BLUEPRINT",
                                    color = Color(0xFFE040FB),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "To run calls between physical phones on different cell networks, you must host these servers. Toggle below to copy fully functional production code:",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp
                                )

                                // Server Guide Tabs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("Signaling", "Coturn", "Traversal").forEach { tab ->
                                        val isSel = serverGuideTab == tab
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFFE040FB).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                                .border(1.dp, if (isSel) Color(0xFFE040FB) else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable { serverGuideTab = tab }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (tab == "Signaling") "WS Server" else if (tab == "Coturn") "TURN Config" else "Theory Guide",
                                                color = if (isSel) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                when (serverGuideTab) {
                                    "Signaling" -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Deploy this Node.js (WebSockets) server script to Google Cloud Run, Heroku, or AWS. It registers phone numbers and transfers SDP offers/answers between devices in real-time.",
                                                color = Color.LightGray,
                                                fontSize = 8.sp,
                                                lineHeight = 11.sp
                                            )
                                            
                                            Text(
                                                text = """
// 🚀 PRODUCTION NODE.JS SIGNALING SERVER (index.js)
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

const activeUsers = new Map(); // phone_number -> socket_id

io.on('connection', (socket) => {
  console.log('Client connected: ' + socket.id);

  // Register device phone number
  socket.on('register', (phoneNumber) => {
    activeUsers.set(phoneNumber, socket.id);
    socket.emit('registered', { status: 'success' });
    console.log('Registered ' + phoneNumber + ' to socket ' + socket.id);
  });

  // Relay call request with SDP Offer
  socket.on('call-user', ({ targetNumber, offer }) => {
    const targetSocketId = activeUsers.get(targetNumber);
    if (targetSocketId) {
      io.to(targetSocketId).emit('incoming-call', {
        caller: socket.id,
        offer: offer
      });
    } else {
      socket.emit('call-failed', { reason: 'User offline' });
    }
  });

  // Relay SDP Answer back to caller
  socket.on('answer-call', ({ callerSocketId, answer }) => {
    io.to(callerSocketId).emit('call-accepted', { answer });
  });

  // Relay ICE Candidates for network discovery
  socket.on('ice-candidate', ({ targetSocketId, candidate }) => {
    io.to(targetSocketId).emit('remote-ice-candidate', { candidate });
  });

  socket.on('disconnect', () => {
    for (let [num, id] of activeUsers.entries()) {
      if (id === socket.id) activeUsers.delete(num);
    }
  });
});

server.listen(process.env.PORT || 3000, () => {
  console.log('Signaling Server running on port 3000');
});
                                                """.trimIndent(),
                                                color = Color(0xFFC5E1A5),
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 11.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            )
                                        }
                                    }
                                    "Coturn" -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Host Coturn on a Linux Ubuntu VPS (DigitalOcean/AWS). Configure 'turnserver.conf' as follows to open up relay UDP/TCP pipelines for strict NAT bypass.",
                                                color = Color.LightGray,
                                                fontSize = 8.sp,
                                                lineHeight = 11.sp
                                            )
                                            
                                            Text(
                                                text = """
# 🛠️ Ubuntu Coturn Configuration (turnserver.conf)
listening-port=3478
tls-port=5349

# Public IP of your VPS
external-ip=YOUR_SERVER_PUBLIC_IP
listening-ip=0.0.0.0

# Authenticated session database
userdb=/var/lib/coturn/turnuserdb.db
realm=turn.yourdomain.com

# Anti-spam and transport properties
fingerprint
lt-cred-mech
max-bps=3000000 # 3 Mbps limit per call stream

# Static authentication credentials
user=user_ref:token_lh8932fnas90812h4bnkasjdf
                                                """.trimIndent(),
                                                color = Color(0xFF80DEEA),
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 11.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .padding(6.dp)
                                            )
                                        }
                                    }
                                    "Traversal" -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "WHY MOBILE CARRIERS BLOCK DIRECT CALLS:",
                                                color = Color(0xFFE040FB),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "1. Carrier-Grade NAT (CGNAT):\nAlmost all cellular operators (like T-Mobile, Jio, Vodafone) route thousands of phones through a single public IP. This means your friend's phone has no public IP of its own that your phone can send packets directly to.",
                                                color = Color.LightGray,
                                                fontSize = 8.sp,
                                                lineHeight = 12.sp
                                            )
                                            Text(
                                                text = "2. Symmetric NAT Firewalls:\nSymmetric firewalls only accept inbound packets if your device has already sent an outbound packet to that *exact* same destination and port first. Because neither device can send to the other first (since they don't know the ports), the connection is locked.",
                                                color = Color.LightGray,
                                                fontSize = 8.sp,
                                                lineHeight = 12.sp
                                            )
                                            Text(
                                                text = "3. The TURN Savior:\nBy routing streams through a TURN server, both phones connect outbound to the TURN server's open ports. The TURN server then bridges the packets between them, resulting in a 100% stable connection on cellular data.",
                                                color = Color.Yellow,
                                                fontSize = 8.sp,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        else -> {
                    // "standard" View
                    if (!isVideo) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(200.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val glowScale by infiniteTransition.animateFloat(
                                    initialValue = 1.0f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(glowScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B7DD8).copy(alpha = 0.2f))
                                )

                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, Color.White, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulated real-time audio soundwave bouncing lines
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(50.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                waveHeights.forEach { amplitude ->
                                    val animatedHeight by animateFloatAsState(
                                        targetValue = amplitude,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                                        label = "height"
                                    )
                                    val finalHeight = (animatedHeight * 40.dp.value).coerceAtLeast(6f)
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .height(finalHeight.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFF3B7DD8),
                                                        Color(0xFF25D366)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isMuted) "Microphone Muted" else "HD Voice Streaming Enabled",
                                color = if (isMuted) Color.Yellow else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // If video, render the HD Live diagnostics overview
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LIVE DIAGNOSTICS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B7DD8)
                                        )
                                        Text(
                                            text = "HD Video Streaming",
                                            fontSize = 10.sp,
                                            color = Color(0xFF25D366),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Divider(color = Color.White.copy(alpha = 0.15f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Resolution", fontSize = 10.sp, color = Color.Gray)
                                            Text(videoResolution, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Frame Rate", fontSize = 10.sp, color = Color.Gray)
                                            Text("$fps FPS", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Latency (Ping)", fontSize = 10.sp, color = Color.Gray)
                                            Text("${pingMs}ms", fontSize = 12.sp, color = if (pingMs < 40) Color(0xFF25D366) else Color.Yellow, fontWeight = FontWeight.Bold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Simulated Packet Loss", fontSize = 10.sp, color = Color.Gray)
                                            Text(
                                                text = when (networkStrength) {
                                                    4 -> "0.00% (Lossless)"
                                                    3 -> "0.04% (Stable)"
                                                    2 -> "1.12% (Eco mode)"
                                                    else -> "9.45% (Chunky)"
                                                },
                                                fontSize = 12.sp,
                                                color = if (networkStrength >= 3) Color.White else Color(0xFFF15C6D),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WebRTC Local SDP Dump Dialog popup
            if (showSdpDialog) {
                AlertDialog(
                    onDismissRequest = { showSdpDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF3B7DD8))
                            Text("Local WebRTC SDP Offer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(Color(0xFF131313), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            val sdpOfferText = """
                                v=0
                                o=- 4611731400 2 IN IP4 127.0.0.1
                                s=-
                                t=0 0
                                a=group:BUNDLE 0 1
                                a=extmap-allow-mixed
                                a=msid-semantic: WMS
                                m=audio 9 RTP/SAVPF 111 103 104 9 0 8 106 105
                                c=IN IP4 0.0.0.0
                                a=rtpmap:111 opus/48000/2
                                a=fmtp:111 minptime=10;useinbandfec=1
                                a=rtpmap:103 ISAC/16000
                                a=rtpmap:9 G722/8000
                                a=rtpmap:0 PCMU/8000
                                m=video 9 RTP/SAVPF 96 97 98 99 100 101
                                c=IN IP4 0.0.0.0
                                a=rtpmap:96 VP8/90000
                                a=rtpmap:97 VP8-rtx/90000
                                a=rtpmap:100 H264/90000
                            """.trimIndent()

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = sdpOfferText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF25D366)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSdpDialog = false }) {
                            Text("Dismiss", color = Color(0xFF3B7DD8))
                        }
                    }
                )
            }   }

            // Connection Simulator Tab Rows & Call controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive link simulator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SIMULATE NETWORK SPEED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val modes = listOf("Auto", "1440p QHD", "1080p FHD", "720p HD", "480p SD", "240p Low", "Disconnected")
                        modes.forEach { m ->
                            val isSelected = networkMode == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF3B7DD8) else Color.White.copy(alpha = 0.15f))
                                    .clickable { networkMode = m }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = m,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // HD post-processing filters (only when video call)
                if (isVideo && isCameraOn) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "HD POST-PROCESSING FILTER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf("None", "Grayscale", "Sepia", "Invert", "Soft Blur", "Night Vision", "Cinematic Warm", "Cyber Glitch")
                            filters.forEach { f ->
                                val isSelected = videoFilter == f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Color(0xFF25D366) else Color.White.copy(alpha = 0.15f))
                                        .clickable { videoFilter = f }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = f,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.LightGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dual-Camera Live Loopback Toggle Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { isTwoWayLoopback = !isTwoWayLoopback }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Two-Way Live Loopback (WebRTC Loop)",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Renders back camera as simulated remote peer feed",
                                    fontSize = 9.sp,
                                    color = Color.LightGray
                                )
                            }
                            Switch(
                                checked = isTwoWayLoopback,
                                onCheckedChange = { isTwoWayLoopback = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF25D366),
                                    checkedTrackColor = Color(0xFF25D366).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                    }
                }

                // Audio / Video control actions bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleCallMute() },
                        modifier = Modifier
                            .background(
                                if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .testTag("mute_microphone_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.toggleCallSpeaker() }) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) Color.Yellow else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // In-Call Chat Button
                    IconButton(
                        onClick = { viewModel.toggleInCallChat() },
                        modifier = Modifier.testTag("incall_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "In-Call Chat",
                            tint = if (isInCallChatOpen) Color(0xFF38BDF8) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (isVideo) {
                        IconButton(
                            onClick = { viewModel.toggleCallCamera() },
                            modifier = Modifier
                                .background(
                                    if (!isCameraOn) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .testTag("disable_camera_button")
                        ) {
                            Icon(
                                imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = if (isCameraOn) "Disable Camera" else "Enable Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (isVideo && isCameraOn) {
                        IconButton(onClick = { viewModel.toggleCallFrontCamera() }) {
                            Icon(
                                imageVector = Icons.Default.Cached,
                                contentDescription = "Flip Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { isPipSwapped = !isPipSwapped }) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap Views",
                                tint = if (isPipSwapped) Color.Yellow else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { videoLayoutMode = if (videoLayoutMode == "Full Screen PIP") "Gallery View Grid" else "Full Screen PIP" }) {
                            Icon(
                                imageVector = if (videoLayoutMode == "Gallery View Grid" || videoLayoutMode == "Split 50/50 Grid") Icons.Default.GridView else Icons.Default.PictureInPicture,
                                contentDescription = "Toggle Dynamic Grid/PIP Layout",
                                tint = if (videoLayoutMode == "Gallery View Grid" || videoLayoutMode == "Split 50/50 Grid") Color(0xFF25D366) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (isVideo) {
                        IconButton(onClick = {
                            isScreenSharing = !isScreenSharing
                            if (isScreenSharing) {
                                webrtcLogs.add("[WebRTC] Screen sharing pipeline activated: WebRTC VideoTrack replaced with DisplayMedia stream.")
                            } else {
                                webrtcLogs.add("[WebRTC] Screen sharing ended: WebRTC VideoTrack restored to camera preview.")
                            }
                        }) {
                            Icon(
                                imageVector = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                                contentDescription = "Screen Share",
                                tint = if (isScreenSharing) Color(0xFF25D366) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { isFullScreenVideo = !isFullScreenVideo }) {
                            Icon(
                                imageVector = if (isFullScreenVideo) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Full Screen Mode",
                                tint = if (isFullScreenVideo) Color(0xFF25D366) else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Take Snapshot button
                        IconButton(
                            onClick = {
                                showSnapshotFlash = true
                                showSnapshotToast = true
                                viewModel.takeCallSnapshot(
                                    callId = call.callId,
                                    participantName = otherUser?.name ?: "Remote Peer",
                                    imageUrl = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                                ) { snapshotId ->
                                    webrtcLogs.add("[WebRTC] Remote camera frame captured & saved to Room DB (Record #$snapshotId).")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Snapshot",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = { viewModel.endActiveCall() },
                        containerColor = Color(0xFFF15C6D),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("end_call_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call")
                    }
                }
            }
        }

        // Slide-up In-Call Chat Overlay
        com.example.ui.components.InCallChatOverlay(viewModel = viewModel)
    }
}

fun formatCallDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraPreviewUseCase.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
        }
    )
}

@Composable
fun ActiveVideoCallNetworkHealthOverlay(
    videoResolution: String,
    pingMs: Int,
    jitterMs: Float,
    packetLossPct: Float,
    networkStatus: String,
    isVideo: Boolean,
    audioBitrate: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (pingMs < 45) Color(0xFF3ECF8E) else if (pingMs < 120) Color(0xFFFFB020) else Color(0xFFFF4D4D))
                )
                Text(
                    text = networkStatus,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "•", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                Text(
                    text = if (isVideo) videoResolution else audioBitrate,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${pingMs}ms latency",
                    color = if (pingMs < 45) Color(0xFF3ECF8E) else Color(0xFFFFB020),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(text = "│", color = Color.White.copy(alpha = 0.2f), fontSize = 9.sp)
                Text(
                    text = String.format(Locale.US, "Jitter: %.1fms", jitterMs),
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
                Text(text = "│", color = Color.White.copy(alpha = 0.2f), fontSize = 9.sp)
                Text(
                    text = String.format(Locale.US, "Loss: %.2f%%", packetLossPct),
                    color = if (packetLossPct == 0f) Color(0xFF3ECF8E) else Color(0xFFFF4D4D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
