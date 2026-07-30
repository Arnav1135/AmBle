package com.example.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiApiClient
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiLiveVoiceScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isListening by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap microphone to start Live Voice session") }
    var userSpeechInput by remember { mutableStateOf("") }
    
    val transcriptList = remember { mutableStateListOf<Pair<String, String>>() }
    val listState = rememberLazyListState()

    // TextToSpeech for real-time model voice responses
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
                isTtsReady = true
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    fun speakResponse(text: String) {
        if (isSpeakerOn && isTtsReady && tts != null) {
            // Clean markdown or tags before speaking
            val cleanText = text.replace(Regex("<[^>]*>"), "")
                .replace(Regex("⚡|\\[|\\]|\\*"), "")
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "gemini_voice_id")
        }
    }

    fun handleSendVoicePrompt(prompt: String) {
        if (prompt.isBlank()) return
        scope.launch {
            isProcessing = true
            statusText = "gemini-3.1-flash-live-preview processing stream..."
            transcriptList.add("user" to prompt)
            userSpeechInput = ""
            
            // Scroll to latest turn
            listState.animateScrollToItem((transcriptList.size - 1).coerceAtLeast(0))

            val response = GeminiApiClient.generateLiveVoiceResponse(
                voicePrompt = prompt,
                conversationContext = transcriptList.toList()
            )

            isProcessing = false
            statusText = "LIVE API • gemini-3.1-flash-live-preview (${response.latencyMs}ms)"
            transcriptList.add("model" to response.text)
            
            speakResponse(response.text)
            listState.animateScrollToItem((transcriptList.size - 1).coerceAtLeast(0))
        }
    }

    // Animation for pulsing waveform bars
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val waveScale3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Gemini Live Voice",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF3ECF8E).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3ECF8E))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "LIVE API",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3ECF8E)
                                    )
                                }
                            }
                        }
                        Text(
                            "Model: gemini-3.1-flash-live-preview",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isSpeakerOn = !isSpeakerOn }) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) Color(0xFF3ECF8E) else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Live Visualizer Orb Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Gradient Glowing Orb Background
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(if (isListening || isProcessing) waveScale3 else 0.95f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                        Color(0xFF06B6D4).copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated Waveform Bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(60.dp)
                        ) {
                            val scales = listOf(waveScale1, waveScale2, waveScale3, waveScale2, waveScale1)
                            scales.forEach { sc ->
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height((45 * if (isListening || isProcessing) sc else 0.2f).dp.coerceAtLeast(8.dp))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF38BDF8), Color(0xFFA855F7))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Quick Prompts Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presetPrompts = listOf(
                    "Hello! Introduces yourself.",
                    "Tell me a short joke.",
                    "Explain Live API.",
                    "Sing a 2-line rhyme."
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presetPrompts.forEach { chipText ->
                                Surface(
                                    onClick = { handleSendVoicePrompt(chipText) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF334155),
                                    modifier = Modifier.border(1.dp, Color(0xFF475569), RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = chipText,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Conversation Transcript
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f))
            ) {
                if (transcriptList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Live Voice Transcript",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Speak or choose a preset prompt to begin.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transcriptList) { (role, text) ->
                            val isUser = role == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    ),
                                    color = if (isUser) Color(0xFF2563EB) else Color(0xFF334155),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isUser) "You (Voice Input)" else "Gemini Live API (gemini-3.1-flash-live-preview)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF38BDF8)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = text,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text / Voice Input Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userSpeechInput,
                    onValueChange = { userSpeechInput = it },
                    placeholder = { Text("Type voice prompt...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Microphone / Send Action Button
                FloatingActionButton(
                    onClick = {
                        if (userSpeechInput.isNotBlank()) {
                            handleSendVoicePrompt(userSpeechInput)
                        } else {
                            // Toggle mic simulation
                            isListening = !isListening
                            if (isListening) {
                                statusText = "Listening... Speak your prompt now."
                                scope.launch {
                                    kotlinx.coroutines.delay(2500)
                                    if (isListening) {
                                        isListening = false
                                        handleSendVoicePrompt("Can you explain how Gemini Live API works in real time?")
                                    }
                                }
                            } else {
                                statusText = "Mic paused. Tap to speak."
                            }
                        }
                    },
                    containerColor = if (isListening) Color(0xFFEF4444) else Color(0xFF2563EB),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (userSpeechInput.isNotBlank()) Icons.Default.Send else if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input"
                    )
                }
            }
        }
    }
}
}
