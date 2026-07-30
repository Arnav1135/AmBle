package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBg
import com.example.ui.theme.GlassStroke

/**
 * Reusable GlassPanel Composable implementing the Glass Edition design system:
 * - 16px background blur
 * - 55% opacity white background (GlassBg = rgba(255, 255, 255, 0.55))
 * - 1px white border with 75% opacity (GlassStroke = rgba(255, 255, 255, 0.75))
 * - 16.dp rounded corners (Rounded-LG) with subtle chromatic shadow
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = GlassBg,
    borderColor: Color = GlassStroke,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 16.dp,
    shadowElevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color(0xFF2483C9).copy(alpha = 0.25f),
                spotColor = Color(0xFF2483C9).copy(alpha = 0.35f)
            )
            .border(
                border = BorderStroke(borderWidth, borderColor),
                shape = shape
            ),
        shape = shape,
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.blur(blurRadius),
            content = content
        )
    }
}
