package com.kbridge.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KBridgeColors = lightColorScheme(
    primary = Color(0xFF167F78),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7F2EB),
    onPrimaryContainer = Color(0xFF073B37),
    secondary = Color(0xFFB5603D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCE),
    onSecondaryContainer = Color(0xFF421B0D),
    background = Color(0xFFF4F7F6),
    onBackground = Color(0xFF172A2A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172A2A),
    surfaceVariant = Color(0xFFE8F0ED),
    onSurfaceVariant = Color(0xFF536664),
    outline = Color(0xFFB7C8C4),
    outlineVariant = Color(0xFFD5E1DD),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun KBridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KBridgeColors,
        content = content,
    )
}
