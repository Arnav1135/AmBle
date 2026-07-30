package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Aesthetic Glass Presets for AmBle Glass Edition.
 * Allows switching dynamic color palettes across the app:
 * - Arctic Blue (Default Sky/Primary Glass)
 * - Midnight Glow (Deep Cosmic Blue & Cyan Glow)
 * - Sunset Amber (Warm Amber & Tangerine Glass)
 * - Emerald Aurora (Crystal Mint & Emerald Glass)
 * - Neon Cyber (Futuristic Violet & Magenta Glass)
 */
enum class GlassPreset(
    val id: String,
    val displayName: String,
    val primary: Color,
    val primaryDark: Color,
    val sky: Color,
    val chart: Color,
    val pale1: Color,
    val pale2: Color,
    val glassBg: Color,
    val glassStroke: Color,
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val ink: Color,
    val inkSoft: Color,
    val iconEmoji: String
) {
    ArcticBlue(
        id = "arctic_blue",
        displayName = "Arctic Blue",
        primary = Color(0xFF2483C9),
        primaryDark = Color(0xFF00609A),
        sky = Color(0xFF7AC9F3),
        chart = Color(0xFFC5DEF2),
        pale1 = Color(0xFFD2E5F4),
        pale2 = Color(0xFFF1F6FC),
        glassBg = Color(0x8CFFFFFF),
        glassStroke = Color(0xBFFFFFFF),
        backgroundStart = Color(0xFFEEF6FC),
        backgroundEnd = Color(0xFFE7F2FB),
        ink = Color(0xFF0C2B41),
        inkSoft = Color(0xFF3F6480),
        iconEmoji = "❄️"
    ),
    MidnightGlow(
        id = "midnight_glow",
        displayName = "Midnight Glow",
        primary = Color(0xFF38BDF8),
        primaryDark = Color(0xFF0284C7),
        sky = Color(0xFF0284C7),
        chart = Color(0xFF0369A1),
        pale1 = Color(0xFF0F172A),
        pale2 = Color(0xFF1E293B),
        glassBg = Color(0x990F172A),
        glassStroke = Color(0x6638BDF8),
        backgroundStart = Color(0xFF0B132B),
        backgroundEnd = Color(0xFF1C2541),
        ink = Color(0xFFF8FAFC),
        inkSoft = Color(0xFF94A3B8),
        iconEmoji = "🌌"
    ),
    SunsetAmber(
        id = "sunset_amber",
        displayName = "Sunset Amber",
        primary = Color(0xFFEA580C),
        primaryDark = Color(0xFFC2410C),
        sky = Color(0xFFFDBA74),
        chart = Color(0xFFFED7AA),
        pale1 = Color(0xFFFFEDD5),
        pale2 = Color(0xFFFFF7ED),
        glassBg = Color(0x99FFFFFF),
        glassStroke = Color(0xCCFDBA74),
        backgroundStart = Color(0xFFFFF7ED),
        backgroundEnd = Color(0xFFFFEDD5),
        ink = Color(0xFF431407),
        inkSoft = Color(0xFF7C2D12),
        iconEmoji = "🌅"
    ),
    EmeraldAurora(
        id = "emerald_aurora",
        displayName = "Emerald Aurora",
        primary = Color(0xFF10B981),
        primaryDark = Color(0xFF047857),
        sky = Color(0xFF6EE7B7),
        chart = Color(0xFFA7F3D0),
        pale1 = Color(0xFFD1FAE5),
        pale2 = Color(0xFFECFDF5),
        glassBg = Color(0x99FFFFFF),
        glassStroke = Color(0xCC6EE7B7),
        backgroundStart = Color(0xFFECFDF5),
        backgroundEnd = Color(0xFFD1FAE5),
        ink = Color(0xFF064E3B),
        inkSoft = Color(0xFF047857),
        iconEmoji = "🌿"
    ),
    NeonCyber(
        id = "neon_cyber",
        displayName = "Neon Cyber",
        primary = Color(0xFFEC4899),
        primaryDark = Color(0xFFBE185D),
        sky = Color(0xFFA855F7),
        chart = Color(0xFFC084FC),
        pale1 = Color(0xFF18181B),
        pale2 = Color(0xFF27272A),
        glassBg = Color(0x9918181B),
        glassStroke = Color(0x66EC4899),
        backgroundStart = Color(0xFF09090B),
        backgroundEnd = Color(0xFF18181B),
        ink = Color(0xFFFAFAFA),
        inkSoft = Color(0xFFA1A1AA),
        iconEmoji = "🔮"
    )
}
