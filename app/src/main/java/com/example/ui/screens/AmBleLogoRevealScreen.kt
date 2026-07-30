package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.*

// Color Palette matching uploaded icon gradient & glass accents
val AmBleGradientTopLeft = Color(0xFF1E40AF)     // Deep Royal Blue (~65-70% Dominant)
val AmBleGradientMiddle = Color(0xFF38BDF8)      // Mid Sky Blue
val AmBleGradientBottomRight = Color(0xFF2DD4BF)  // Soft Glossy Mint/Teal-Green (~20-25%)
val AmBleHoloCyan = Color(0xFF38BDF8)            // Sky Blue Cyan
val AmBleHoloPink = Color(0xFFF472B6)            // Subtle Rose Pink Rim Highlight (~5-10% Max)
val AmBleHoloGold = Color(0xFFFDE047)            // Holographic Gold Sparkle Fleck
val AmBleGlassBase = Color(0xE6FFFFFF)           // Translucent Frosted Glass White
val AmBleCharcoalShadow = Color(0xFF0F172A)       // Deep Ambient Shadow
val AmBleRecRed = Color(0xFFEF4444)              // Warm Accent Glow

// Sparkle fleck inside glass
private data class OpalSparkle(
    val relX: Float,
    val relY: Float,
    val size: Float,
    val hueShift: Float,
    val phase: Float
)

data class SplashSceneSpec(
    val sceneNumber: Int,
    val timeRange: String,
    val title: String,
    val description: String,
    val startTimeSeconds: Float
)

val AMBLE_SPLASH_SCENES = listOf(
    SplashSceneSpec(1, "0.0s – 0.8s", "Dormant State", "Glass 'A' centered in blue-to-teal squircle. Subtle dolly push-in, internal tube glow wakes up, opal sparkles shimmer.", 0.0f),
    SplashSceneSpec(2, "0.8s – 1.8s", "Dissolve to 3 Forms", "Glass 'A' liquefies into 3 mercury-like glass strokes drifting in 3D arcs with sky blue to teal light ribbons.", 0.8f),
    SplashSceneSpec(3, "1.8s – 3.2s", "Three Icons Form", "Reshapes into 3 holographic glass icons: Chat (typing dots), Call (soundwaves), Video (REC scanning lens).", 1.8f),
    SplashSceneSpec(4, "3.2s – 4.0s", "Energy Build", "Icons pulse like a heartbeat while electric sky blue & teal threads arc between them in magnetic field lines.", 3.2f),
    SplashSceneSpec(5, "4.0s – 4.8s", "Reconvergence", "Icons accelerate along arcs in a fluid liquid-glass merge with a soft holographic flash bloom.", 4.0f),
    SplashSceneSpec(6, "4.8s – 5.6s", "Final Reveal", "Reformed AmBle glass 'A' icon with activated internal glow, rotating specular light sweep, micro-breathing loop.", 4.8f)
)

@Composable
fun AmBleLogoRevealScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    var currentTime by remember { mutableFloatStateOf(0.0f) }
    var isPlaying by remember { mutableStateOf(true) }

    // Ticker loop for smooth 60fps animation (accelerated for snappy startup)
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var previousFrameTimeNanos = 0L
        while (isActive && isPlaying) {
            withFrameNanos { frameTimeNanos ->
                if (previousFrameTimeNanos != 0L) {
                    val dt = (frameTimeNanos - previousFrameTimeNanos) / 1_000_000_000f
                    val safeDt = dt.coerceIn(0f, 0.1f)
                    // Multiply time scale by 4.5f so splash completes smoothly in ~1.2 seconds
                    val nextTime = currentTime + safeDt * 4.5f
                    if (nextTime >= 5.6f) {
                        currentTime = 5.6f
                        isPlaying = false
                        onBack()
                    } else {
                        currentTime = nextTime
                    }
                }
                previousFrameTimeNanos = frameTimeNanos
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmBleGradientTopLeft)
            .clickable {
                isPlaying = false
                onBack()
            }
            .testTag("amble_splash_reveal_screen")
    ) {
        // Pure Full-Screen 3D Procedural Canvas with 6-Scene Cinematic Animation
        AmBle3DSplashCanvas(
            currentTime = currentTime,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Procedural 3D Canvas rendering the exact AmBle glass "A" letterform,
 * diagonal blue-to-teal squircle gradient, iridescent opal sparkles,
 * 3 liquid glass icon transformations, electric tendrils, bloom flash, and micro-float.
 */
@Composable
fun AmBle3DSplashCanvas(
    currentTime: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Load bitmap asset if available for reference
    val imageBitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_logo_3d_glass)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    // Opal Holographic Sparkles embedded inside glass letterform
    val sparkles = remember {
        val rand = Random(123)
        List(35) {
            OpalSparkle(
                relX = rand.nextFloat() * 160f - 80f,
                relY = rand.nextFloat() * 180f - 90f,
                size = rand.nextFloat() * 7f + 3f,
                hueShift = rand.nextFloat(),
                phase = rand.nextFloat() * Math.PI.toFloat() * 2f
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        // 1. Draw Full-Screen Studio Background Gradient (Blue -> Sky -> Teal-Mint)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    AmBleGradientTopLeft,
                    AmBleGradientMiddle,
                    AmBleGradientBottomRight
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )

        // Volumetric Light Rays passing through background
        drawVolumetricBackgroundRays(center = center, width = width, height = height, time = currentTime)

        // Scene Logic
        when {
            // SCENE 1 — DORMANT STATE (0.0s – 0.8s)
            currentTime < 0.8f -> {
                val t = (currentTime / 0.8f).coerceIn(0f, 1f)
                val dollyScale = 0.95f + 0.05f * t
                val tubeGlowAlpha = 0.2f + 0.5f * sin(t * Math.PI.toFloat())

                drawAmBleSquircleBadge(
                    center = center,
                    badgeSize = 260.dp.toPx() * dollyScale,
                    time = currentTime,
                    tubeGlowAlpha = tubeGlowAlpha,
                    sparkles = sparkles,
                    imageBitmap = imageBitmap
                )
            }

            // SCENE 2 — DISSOLVE INTO THREE FORMS (0.8s – 1.8s)
            currentTime < 1.8f -> {
                val t = ((currentTime - 0.8f) / 1.0f).coerceIn(0f, 1f)
                val arcDist = 170.dp.toPx() * t

                // Draw exposed glowing gradient squircle behind separating strokes
                drawSquircleBackgroundShape(
                    center = center,
                    size = 260.dp.toPx(),
                    alpha = 0.95f
                )

                // 3 Arcing fracture stroke positions
                val pos1 = center + Offset(-arcDist * 0.9f, -arcDist * 0.15f)
                val pos2 = center + Offset(0f, -arcDist * 0.1f)
                val pos3 = center + Offset(arcDist * 0.9f, arcDist * 0.2f)

                // Trailing ribbons of holographic light (sky blue to teal)
                drawHolographicRibbon(start = center, end = pos1, progress = t, color = AmBleHoloCyan)
                drawHolographicRibbon(start = center, end = pos2, progress = t, color = AmBleGradientMiddle)
                drawHolographicRibbon(start = center, end = pos3, progress = t, color = AmBleGradientBottomRight)

                // 3 Liquid-glass stroke segments
                drawLiquidGlassStrokeSegment(center = pos1, size = 95.dp.toPx(), progress = t, type = 1, sparkles = sparkles)
                drawLiquidGlassStrokeSegment(center = pos2, size = 105.dp.toPx(), progress = t, type = 2, sparkles = sparkles)
                drawLiquidGlassStrokeSegment(center = pos3, size = 95.dp.toPx(), progress = t, type = 3, sparkles = sparkles)
            }

            // SCENE 3 — THREE ICONS FORM (1.8s – 3.2s)
            currentTime < 3.2f -> {
                val t = ((currentTime - 1.8f) / 1.4f).coerceIn(0f, 1f)
                val spread = 120.dp.toPx()

                val posLeft = center + Offset(-spread, 0f)
                val posCenter = center + Offset(0f, -18.dp.toPx())
                val posRight = center + Offset(spread, 0f)

                // Parallax camera arc tilt
                val parallaxX = sin(t * Math.PI.toFloat() * 2f) * 14.dp.toPx()

                // LEFT: Glass Chat Bubble with typing dots
                drawGlassChatIcon(
                    center = posLeft + Offset(parallaxX * 0.5f, 0f),
                    size = 110.dp.toPx(),
                    time = currentTime,
                    sparkles = sparkles
                )

                // CENTER: Glass Phone/Voice Call Icon with soundwave ripples
                drawGlassPhoneIcon(
                    center = posCenter,
                    size = 135.dp.toPx(),
                    time = currentTime,
                    sparkles = sparkles
                )

                // RIGHT: Glass Video Camera Icon with scanline & REC dot
                drawGlassVideoIcon(
                    center = posRight - Offset(parallaxX * 0.5f, 0f),
                    size = 110.dp.toPx(),
                    time = currentTime,
                    sparkles = sparkles
                )
            }

            // SCENE 4 — ENERGY BUILD (3.2s – 4.0s)
            currentTime < 4.0f -> {
                val t = ((currentTime - 3.2f) / 0.8f).coerceIn(0f, 1f)
                val spread = 120.dp.toPx()

                val posLeft = center + Offset(-spread, 0f)
                val posCenter = center + Offset(0f, -18.dp.toPx())
                val posRight = center + Offset(spread, 0f)

                // Heartbeat accelerating pulse
                val heartbeatScale = 1.0f + 0.14f * sin(t * 22f)

                // Glowing electric threads arcing between icons
                drawElectricFieldLine(start = posLeft, end = posCenter, t = t, color = AmBleHoloCyan)
                drawElectricFieldLine(start = posCenter, end = posRight, t = t, color = AmBleGradientBottomRight)
                drawElectricFieldLine(start = posLeft, end = posRight, t = t, color = AmBleHoloPink)

                // Draw pulsing icons
                drawGlassChatIcon(posLeft, 110.dp.toPx() * heartbeatScale, currentTime, sparkles)
                drawGlassPhoneIcon(posCenter, 135.dp.toPx() * heartbeatScale, currentTime, sparkles)
                drawGlassVideoIcon(posRight, 110.dp.toPx() * heartbeatScale, currentTime, sparkles)
            }

            // SCENE 5 — THE RECONVERGENCE (4.0s – 4.8s)
            currentTime < 4.8f -> {
                val t = ((currentTime - 4.0f) / 0.8f).coerceIn(0f, 1f)
                val convergeDist = (1.0f - t) * 120.dp.toPx()

                val posLeft = center + Offset(-convergeDist, 0f)
                val posCenter = center
                val posRight = center + Offset(convergeDist, 0f)

                if (t < 0.65f) {
                    drawGlassChatIcon(posLeft, 110.dp.toPx() * (1f - t * 0.3f), currentTime, sparkles)
                    drawGlassPhoneIcon(posCenter, 135.dp.toPx() * (1f - t * 0.2f), currentTime, sparkles)
                    drawGlassVideoIcon(posRight, 110.dp.toPx() * (1f - t * 0.3f), currentTime, sparkles)
                } else {
                    val fusionProgress = (t - 0.65f) / 0.35f
                    drawAmBleSquircleBadge(
                        center = center,
                        badgeSize = 260.dp.toPx() * fusionProgress,
                        time = currentTime,
                        tubeGlowAlpha = 0.9f,
                        sparkles = sparkles,
                        imageBitmap = imageBitmap
                    )
                }

                // Holographic bloom flash on contact
                if (t in 0.45f..0.85f) {
                    val flashAlpha = sin(((t - 0.45f) / 0.4f) * Math.PI.toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f * flashAlpha),
                                AmBleHoloCyan.copy(alpha = 0.7f * flashAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = width * 0.8f
                        ),
                        radius = width * 0.8f,
                        center = center
                    )
                }
            }

            // SCENE 6 — FINAL REVEAL (4.8s – 5.6s+)
            else -> {
                val t = (currentTime - 4.8f)
                val microFloatScale = 1.0f + 0.02f * sin(t * 3.2f)
                val specularSweepRatio = (t * 0.4f) % 1.0f

                drawAmBleSquircleBadge(
                    center = center,
                    badgeSize = 260.dp.toPx() * microFloatScale,
                    time = currentTime,
                    tubeGlowAlpha = 0.8f + 0.2f * sin(t * 4f),
                    specularSweepRatio = specularSweepRatio,
                    sparkles = sparkles,
                    imageBitmap = imageBitmap,
                    isFinalActivated = true
                )
            }
        }
    }
}

private fun DrawScope.drawVolumetricBackgroundRays(center: Offset, width: Float, height: Float, time: Float) {
    for (i in 0 until 4) {
        val angle = (time * 12f + i * 90f) * Math.PI.toFloat() / 180f
        val rayEnd = center + Offset(cos(angle) * width * 0.8f, sin(angle) * height * 0.8f)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                start = center,
                end = rayEnd
            ),
            start = center,
            end = rayEnd,
            strokeWidth = 40.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawAmBleSquircleBadge(
    center: Offset,
    badgeSize: Float,
    time: Float,
    tubeGlowAlpha: Float,
    specularSweepRatio: Float = 0f,
    sparkles: List<OpalSparkle>,
    imageBitmap: ImageBitmap?,
    isFinalActivated: Boolean = false
) {
    val corner = badgeSize * 0.28f
    val rect = Rect(
        center.x - badgeSize / 2f,
        center.y - badgeSize / 2f,
        center.x + badgeSize / 2f,
        center.y + badgeSize / 2f
    )

    // Soft Ambient Occlusion Shadow
    drawRoundRect(
        color = AmBleCharcoalShadow.copy(alpha = 0.22f),
        topLeft = Offset(rect.left + 8f, rect.top + 16f),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner)
    )

    // Squircle Background Gradient (Top-Left Blue -> Sky -> Teal-Mint)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                AmBleGradientTopLeft,
                AmBleGradientMiddle,
                AmBleGradientBottomRight
            ),
            start = Offset(rect.left, rect.top),
            end = Offset(rect.right, rect.bottom)
        ),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner)
    )

    // If bitmap asset is present, render high-res image overlay
    if (imageBitmap != null) {
        drawImage(
            image = imageBitmap,
            dstOffset = androidx.compose.ui.unit.IntOffset(rect.left.toInt(), rect.top.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(rect.width.toInt(), rect.height.toInt())
        )
    } else {
        // Procedural Glass "A" Letterform rendering
        drawGlassLetterformA(center = center, size = badgeSize * 0.68f, tubeGlowAlpha = tubeGlowAlpha)
    }

    // Opal Holographic Sparkle Flecks embedded inside glass
    sparkles.forEachIndexed { i, sp ->
        val sparkleAlpha = (0.3f + 0.7f * sin(time * 6f + sp.phase)).coerceIn(0f, 1f)
        val spCenter = center + Offset(sp.relX * (badgeSize / 260f), sp.relY * (badgeSize / 260f))
        val spColor = when (i % 3) {
            0 -> AmBleHoloCyan
            1 -> AmBleHoloPink
            else -> AmBleHoloGold
        }
        drawCircle(
            color = spColor.copy(alpha = sparkleAlpha),
            radius = sp.size * (badgeSize / 260f),
            center = spCenter
        )
    }

    // Rotating Specular Highlight Sweep
    if (specularSweepRatio > 0f || isFinalActivated) {
        val sweepRatio = if (specularSweepRatio > 0f) specularSweepRatio else ((time * 0.4f) % 1.0f)
        val sweepX = rect.left + rect.width * sweepRatio
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.65f), Color.Transparent),
                start = Offset(sweepX - 25f, rect.top),
                end = Offset(sweepX + 25f, rect.bottom)
            ),
            start = Offset(sweepX - 25f, rect.top),
            end = Offset(sweepX + 25f, rect.bottom),
            strokeWidth = 22.dp.toPx()
        )
    }

    // Outer Soft Glass Edge Highlight
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                AmBleHoloCyan.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.8f)
            )
        ),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawSquircleBackgroundShape(center: Offset, size: Float, alpha: Float) {
    val corner = size * 0.28f
    val rect = Rect(center.x - size / 2f, center.y - size / 2f, center.x + size / 2f, center.y + size / 2f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                AmBleGradientTopLeft.copy(alpha = alpha),
                AmBleGradientMiddle.copy(alpha = alpha),
                AmBleGradientBottomRight.copy(alpha = alpha)
            ),
            start = Offset(rect.left, rect.top),
            end = Offset(rect.right, rect.bottom)
        ),
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(corner, corner)
    )
}

private fun DrawScope.drawGlassLetterformA(center: Offset, size: Float, tubeGlowAlpha: Float) {
    val strokeWidth = size * 0.14f
    val pathA = Path().apply {
        // Top rounded arch
        moveTo(center.x - size * 0.35f, center.y + size * 0.35f)
        cubicTo(
            center.x - size * 0.35f, center.y - size * 0.15f,
            center.x - size * 0.15f, center.y - size * 0.42f,
            center.x, center.y - size * 0.42f
        )
        cubicTo(
            center.x + size * 0.15f, center.y - size * 0.42f,
            center.x + size * 0.35f, center.y - size * 0.15f,
            center.x + size * 0.35f, center.y + size * 0.35f
        )
    }

    // Tube Glow Inner core
    drawPath(
        path = pathA,
        color = AmBleHoloCyan.copy(alpha = tubeGlowAlpha * 0.8f),
        style = Stroke(width = strokeWidth * 1.2f, cap = StrokeCap.Round)
    )

    // Glass Tube Translucent Body
    drawPath(
        path = pathA,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                AmBleHoloCyan.copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.9f)
            )
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Faint Pink Rim-Light Accent (~5-10% warmth along top-right edge)
    val pinkRimPath = Path().apply {
        moveTo(center.x, center.y - size * 0.42f)
        cubicTo(
            center.x + size * 0.15f, center.y - size * 0.42f,
            center.x + size * 0.35f, center.y - size * 0.15f,
            center.x + size * 0.35f, center.y + size * 0.10f
        )
    }
    drawPath(
        path = pinkRimPath,
        color = AmBleHoloPink.copy(alpha = 0.55f),
        style = Stroke(width = strokeWidth * 0.28f, cap = StrokeCap.Round)
    )

    // Curved Crossbar
    val crossPath = Path().apply {
        moveTo(center.x - size * 0.28f, center.y + size * 0.05f)
        quadraticTo(center.x, center.y + size * 0.18f, center.x + size * 0.28f, center.y + size * 0.05f)
    }

    drawPath(
        path = crossPath,
        color = Color.White.copy(alpha = 0.95f),
        style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawHolographicRibbon(start: Offset, end: Offset, progress: Float, color: Color) {
    val control = Offset((start.x + end.x) / 2f + 35f, (start.y + end.y) / 2f - 35f)
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(control.x, control.y, end.x, end.y)
    }

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = 0.8f * (1f - progress * 0.4f))),
            start = start,
            end = end
        ),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawLiquidGlassStrokeSegment(center: Offset, size: Float, progress: Float, type: Int, sparkles: List<OpalSparkle>) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, AmBleHoloCyan.copy(alpha = 0.8f), Color.Transparent),
            center = center,
            radius = size / 2f
        ),
        radius = size / 2f,
        center = center
    )
    drawCircle(color = Color.White, radius = size / 2f, center = center, style = Stroke(width = 2.5.dp.toPx()))
}

private fun DrawScope.drawGlassChatIcon(center: Offset, size: Float, time: Float, sparkles: List<OpalSparkle>) {
    val radius = size / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, AmBleGradientMiddle, AmBleGradientTopLeft),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))

    // Chat Bubble Body
    val iconSize = size * 0.42f
    val bubblePath = Path().apply {
        addOval(Rect(center.x - iconSize / 2f, center.y - iconSize / 2f, center.x + iconSize / 2f, center.y + iconSize / 2f))
    }
    drawPath(bubblePath, color = AmBleGradientTopLeft)

    // Animated Typing Dots inside Left Icon
    for (i in 0..2) {
        val dotAlpha = 0.3f + 0.7f * ((sin(time * 8f + i * 1.5f) + 1f) / 2f)
        drawCircle(
            color = Color.White.copy(alpha = dotAlpha),
            radius = 3.5.dp.toPx(),
            center = Offset(center.x + (i - 1) * 12.dp.toPx(), center.y)
        )
    }
}

private fun DrawScope.drawGlassPhoneIcon(center: Offset, size: Float, time: Float, sparkles: List<OpalSparkle>) {
    val radius = size / 2f

    // Concentric Soundwaves
    for (w in 1..3) {
        val waveRadius = radius + (w * 18.dp.toPx() + (time * 40f) % 36.dp.toPx())
        val waveAlpha = (1.0f - (waveRadius - radius) / (60.dp.toPx())).coerceIn(0f, 1f)
        drawCircle(
            color = AmBleHoloCyan.copy(alpha = waveAlpha * 0.7f),
            radius = waveRadius,
            center = center,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, AmBleHoloCyan, AmBleGradientTopLeft),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 3.5.dp.toPx()))

    val phonePath = Path().apply {
        moveTo(center.x - 12f, center.y - 14f)
        lineTo(center.x + 12f, center.y + 14f)
    }
    drawPath(phonePath, color = Color.White, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawGlassVideoIcon(center: Offset, size: Float, time: Float, sparkles: List<OpalSparkle>) {
    val radius = size / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, AmBleGradientBottomRight, AmBleGradientTopLeft),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))

    val camRect = Rect(center.x - 16f, center.y - 12f, center.x + 6f, center.y + 12f)
    drawRoundRect(color = AmBleGradientTopLeft, topLeft = Offset(camRect.left, camRect.top), size = Size(camRect.width, camRect.height), cornerRadius = CornerRadius(4f, 4f))

    val recAlpha = 0.4f + 0.6f * sin(time * 10f)
    drawCircle(color = AmBleRecRed.copy(alpha = recAlpha), radius = 4.dp.toPx(), center = Offset(center.x + 16f, center.y - 12f))

    val scanY = center.y - radius + ((time * 80f) % (radius * 2f))
    drawLine(color = Color.White.copy(alpha = 0.65f), start = Offset(center.x - radius * 0.7f, scanY), end = Offset(center.x + radius * 0.7f, scanY), strokeWidth = 2.dp.toPx())
}

private fun DrawScope.drawElectricFieldLine(start: Offset, end: Offset, t: Float, color: Color) {
    val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f - sin(t * 14f) * 28f)
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(mid.x, mid.y, end.x, end.y)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = 0.75f + 0.25f * sin(t * 22f)),
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
    )
}
