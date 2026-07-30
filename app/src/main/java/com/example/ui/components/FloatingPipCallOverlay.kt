package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FloatingPipCallOverlay(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val activeCall by viewModel.activeCall.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isCallInPipMode by viewModel.isCallInPipMode.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isMuted by viewModel.isCallMuted.collectAsState()
    val isCameraOn by viewModel.isCallCameraOn.collectAsState()
    val isFrontCamera by viewModel.isCallFrontCamera.collectAsState()
    val isSpeakerOn by viewModel.isCallSpeakerOn.collectAsState()
    val isPipLarge by viewModel.isPipLargeSize.collectAsState()
    val isInCallChatOpen by viewModel.isInCallChatOpen.collectAsState()

    val call = activeCall
    if (call == null || (call.status != "active" && call.status != "ringing")) return

    // Show floating overlay when explicit PiP mode is enabled OR user navigated away from full "call" screen OR in-call chat is open
    val shouldShowOverlay = isCallInPipMode || currentScreen != "call" || isInCallChatOpen
    if (!shouldShowOverlay) return

    val partnerId = if (call.callerId == currentUser?.uid) call.calleeId else call.callerId
    val otherUser = users.firstOrNull { it.uid == partnerId }

    // Live call duration timer
    var durationSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(call.callId, call.status) {
        if (call.status == "active") {
            durationSeconds = 0
            while (true) {
                delay(1000)
                durationSeconds += 1
            }
        }
    }

    val formatTime = remember {
        { secs: Int ->
            val m = secs / 60
            val s = secs % 60
            String.format("%02d:%02d", m, s)
        }
    }

    val isVideo = call.type == "video"

    // Draggable position with screen bounds clamping
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { constraints.maxWidth.toDp().toPx() }
        val screenHeightPx = with(density) { constraints.maxHeight.toDp().toPx() }

        // PiP Size dimensions
        val pipWidthDp = if (!isVideo) 260.dp else if (isPipLarge) 260.dp else 200.dp
        val pipHeightDp = if (!isVideo) 72.dp else if (isPipLarge) 210.dp else 160.dp

        val pipWidthPx = with(density) { pipWidthDp.toPx() }
        val pipHeightPx = with(density) { pipHeightDp.toPx() }

        val animatedWidth by animateDpAsState(targetValue = pipWidthDp, label = "pip_width")
        val animatedHeight by animateDpAsState(targetValue = pipHeightDp, label = "pip_height")

        // Render In-Call Chat Overlay if toggled
        InCallChatOverlay(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Draggable Call Window
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Clamp offsets to keep PiP inside screen edges
                            val minX = -(screenWidthPx - pipWidthPx - 32.dp.toPx())
                            val maxX = 0f
                            val minY = -(screenHeightPx - pipHeightPx - 32.dp.toPx())
                            val maxY = 0f

                            offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                        }
                    }
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .shadow(20.dp, RoundedCornerShape(20.dp))
                    .testTag(if (isVideo) "pip_video_overlay" else "pip_voice_overlay"),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.85f))
            ) {
                if (isVideo) {
                    // Video Call PiP Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366))
                                )
                                Text(
                                    text = "PiP Video",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = formatTime(durationSeconds),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Video Stream Display Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { viewModel.expandPipCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                contentDescription = "Remote Video Stream",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Remote User Name Overlay Badge
                            Text(
                                text = otherUser?.name ?: "Remote Peer",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Self-view Thumbnail (Overlaid in top-right corner of PiP)
                            if (isCameraOn) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(width = 46.dp, height = 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = currentUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                        contentDescription = "Self View",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Control Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Mic
                            IconButton(
                                onClick = { viewModel.toggleCallMute() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(if (isMuted) Color(0xFFF15C6D) else Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute Mic",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Camera Toggle
                            IconButton(
                                onClick = { viewModel.toggleCallCamera() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(if (!isCameraOn) Color.Yellow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Camera Toggle",
                                    tint = if (!isCameraOn) Color.Yellow else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // In-Call Chat Button
                            IconButton(
                                onClick = { viewModel.toggleInCallChat() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(if (isInCallChatOpen) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "In-Call Chat",
                                    tint = if (isInCallChatOpen) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Resize PiP Size Toggle
                            IconButton(
                                onClick = { viewModel.togglePipLargeSize() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPipLarge) Icons.Default.Compress else Icons.Default.Expand,
                                    contentDescription = "Resize PiP",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Expand Full Screen
                            IconButton(
                                onClick = { viewModel.expandPipCall() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF38BDF8), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Expand Fullscreen",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // End Call
                            IconButton(
                                onClick = { viewModel.endActiveCall() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF15C6D), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Voice Call Floating Mini Bar / Pill Layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable { viewModel.expandPipCall() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // User Avatar & Name & Duration
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box {
                                AsyncImage(
                                    model = otherUser?.photoUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366))
                                        .align(Alignment.BottomEnd)
                                )
                            }

                            Column {
                                Text(
                                    text = otherUser?.name ?: "Voice Call",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = null,
                                        tint = Color(0xFF25D366),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = formatTime(durationSeconds),
                                        color = Color(0xFF25D366),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Quick Control Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute
                            IconButton(
                                onClick = { viewModel.toggleCallMute() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(if (isMuted) Color(0xFFF15C6D) else Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // In-Call Chat
                            IconButton(
                                onClick = { viewModel.toggleInCallChat() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(if (isInCallChatOpen) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "In-Call Chat",
                                    tint = if (isInCallChatOpen) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Expand
                            IconButton(
                                onClick = { viewModel.expandPipCall() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color(0xFF38BDF8), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Expand Call",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // End Call
                            IconButton(
                                onClick = { viewModel.endActiveCall() },
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(Color(0xFFF15C6D), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
