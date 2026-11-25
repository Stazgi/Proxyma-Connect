package com.v2ray.ang.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager.rememberMmkvString

data class ProxymaPalette(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val drawerBg: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val accentCyan: Color,
    val accentIndigo: Color,
    val connectedGreen: Color,
    val blockRed: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

private val DarkProxymaPalette = ProxymaPalette(
    isDark = true,
    bgTop = Color(0xFF121316),
    bgBottom = Color(0xFF17181C),
    drawerBg = Color(0xFF141519),
    cardSurface = Color(0xFF1E2025),
    cardBorder = Color(0xFF2E3138),
    accentCyan = Color(0xFF00E5FF),
    accentIndigo = Color(0xFF6366F1),
    connectedGreen = Color(0xFF10B981),
    blockRed = Color(0xFFEF4444),
    textPrimary = Color(0xFFF3F4F6),
    textSecondary = Color(0xFF9CA3AF)
)

private val LightProxymaPalette = ProxymaPalette(
    isDark = false,
    bgTop = Color(0xFFF3F6FC),
    bgBottom = Color(0xFFE7EEF8),
    drawerBg = Color(0xFFF8FAFC),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFD5DFEC),
    accentCyan = Color(0xFF0284C7),
    accentIndigo = Color(0xFF4F46E5),
    connectedGreen = Color(0xFF059669),
    blockRed = Color(0xFFDC2626),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569)
)

@Composable
fun rememberProxymaColors(): ProxymaPalette {
    val systemDark = isSystemInDarkTheme()
    val uiMode by rememberMmkvString(AppConfig.PREF_UI_MODE_NIGHT, "0")
    val isDark = when (uiMode) {
        "1" -> false
        "2" -> true
        else -> systemDark
    }
    return if (isDark) DarkProxymaPalette else LightProxymaPalette
}

val LocalProxymaColors = staticCompositionLocalOf<ProxymaPalette> {
    error("No ProxymaPalette provided")
}

object ProxymaTheme {
    val colors: ProxymaPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalProxymaColors.current
}

@Composable
fun ProxymaThemeWrapper(
    content: @Composable () -> Unit
) {
    val palette = rememberProxymaColors()

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accentCyan,
            background = palette.bgTop,
            surface = palette.cardSurface,
            onPrimary = palette.bgTop,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            outline = palette.cardBorder
        )
    } else {
        lightColorScheme(
            primary = palette.accentCyan,
            background = palette.bgTop,
            surface = palette.cardSurface,
            onPrimary = Color.White,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            outline = palette.cardBorder
        )
    }

    CompositionLocalProvider(LocalProxymaColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
