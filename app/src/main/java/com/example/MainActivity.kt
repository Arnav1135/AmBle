package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AmBleLogoRevealScreen
import com.example.ui.screens.AuthScreens
import com.example.ui.screens.CallScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.GeminiLiveVoiceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.ChatWaveTheme
import com.example.viewmodel.ChatViewModel

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import com.example.ui.components.FloatingPipCallOverlay

class MainActivity : ComponentActivity() {
    private var chatViewModel: ChatViewModel? = null

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val activeCall = chatViewModel?.activeCall?.value
        if (activeCall != null && (activeCall.status == "active" || activeCall.status == "ringing")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(9, 16))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    chatViewModel?.enterPipMode()
                }
            } else {
                chatViewModel?.enterPipMode()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        chatViewModel?.setIsInAndroidPipMode(isInPictureInPictureMode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = viewModel()
            chatViewModel = viewModel
            val currentScreen by viewModel.currentScreen.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val glassPreset by viewModel.glassPreset.collectAsState()

            // Request necessary permissions on start for mic and camera functionality
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
                val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
                if (!recordGranted || !cameraGranted) {
                    Toast.makeText(
                        this,
                        "Camera and Mic permissions are recommended for full calling and voice experiences!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            LaunchedEffect(Unit) {
                viewModel.triggerSplashOnOpen()
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    )
                )
            }

            ChatWaveTheme(darkTheme = isDarkMode, glassPreset = glassPreset) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = androidx.compose.animation.core.tween(220)) togetherWith
                                        fadeOut(animationSpec = androidx.compose.animation.core.tween(220))
                            },
                            label = "screen_navigation"
                        ) { screen ->
                            when (screen) {
                                "auth" -> AuthScreens(viewModel = viewModel)
                                "home" -> HomeScreen(viewModel = viewModel)
                                "chat" -> ChatDetailScreen(viewModel = viewModel)
                                "call" -> CallScreen(viewModel = viewModel)
                                "gemini_live_voice" -> GeminiLiveVoiceScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateTo("home") }
                                )
                                "logo_reveal" -> AmBleLogoRevealScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.onSplashFinished() }
                                )
                                else -> AuthScreens(viewModel = viewModel)
                            }
                        }

                        // Floating Picture-in-Picture Mini Overlay across banking dashboard
                        FloatingPipCallOverlay(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
