package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// AmBle Glass Edition Brand Colors & Design Tokens
val AmBlePrimary = Color(0xFF2483C9)        // Glass Primary Blue (#2483C9)
val AmBlePrimaryDark = Color(0xFF00609A)    // Deep Primary (#00609A)
val AmBleSky = Color(0xFF7AC9F3)            // Sky Blue (#7AC9F3)
val AmBleChart = Color(0xFFC5DEF2)          // Chart Soft Blue (#C5DEF2)
val AmBlePale1 = Color(0xFFD2E5F4)          // Pale 1 (#D2E5F4)
val AmBlePale2 = Color(0xFFF1F6FC)          // Pale 2 (#F1F6FC)
val AmBleInk = Color(0xFF0C2B41)            // Ink Deep (#0C2B41)
val AmBleInkSoft = Color(0xFF3F6480)        // Ink Soft (#3F6480)
val AmBleInkFaint = Color(0xFF83A0B4)       // Ink Faint (#83A0B4)

// Glass Edition Specific Tokens from YAML
val GlassBg = Color(0x8CFFFFFF)            // rgba(255, 255, 255, 0.55)
val GlassStroke = Color(0xBFFFFFFF)        // rgba(255, 255, 255, 0.75)
val BgRadialSky = Color(0xFFD2E5F4)        // #D2E5F4
val BgLinearPale = Color(0xFFF1F6FC)       // #F1F6FC
val StatusSuccess = Color(0xFF3FBF72)      // #3FBF72
val StatusError = Color(0xFFE0554A)        // #E0554A

// AmBle Glass Gradient Canvas Background
val AmBleBgStart = Color(0xFFEEF6FC)
val AmBleBgMiddle = Color(0xFFDFEEFA)
val AmBleBgEnd = Color(0xFFE7F2FB)

// WavePay / Compatibility Aliases
val WavePayPrimaryDark = AmBleInk
val WavePaySecondary = AmBlePrimary
val WavePayAccent1 = AmBleSky
val WavePayAccent2 = AmBleChart
val WavePayBgStart = AmBleBgStart
val WavePayBgEnd = AmBleBgEnd
val WavePayTextDark = Color(0xFFFFFFFF)
val WavePayTextLight = AmBleInk
val WavePayAlert = Color(0xFFE0554A)
val WavePaySuccess = Color(0xFF3FBF72)

// Premium WhatsApp / ChatWave Palette
val ChatWaveTeal = AmBlePrimary
val ChatWaveTealDark = AmBleInk
val ChatWaveGreen = Color(0xFF3FBF72)
val ChatWaveOrange = AmBleSky

// Dark Theme Colors
val ChatWaveDarkBackground = Color(0xFF0C2B41)
val ChatWaveDarkSurface = Color(0xFF123A5C)
val ChatWaveDarkSurfaceVariant = Color(0xFF1F6BA3)
val ChatWaveDarkBubbleMe = AmBlePrimary
val ChatWaveDarkBubbleOther = Color(0xFF1F6BA3)

// Light Theme Colors (Glass Reskin)
val ChatWaveLightBackground = AmBleBgStart
val ChatWaveLightSurface = Color.White.copy(alpha = 0.75f)
val ChatWaveLightSurfaceVariant = AmBlePale2
val ChatWaveLightBubbleMe = AmBlePrimary
val ChatWaveLightBubbleOther = Color.White.copy(alpha = 0.65f)
val ChatWaveWallpaperLight = AmBleBgStart


