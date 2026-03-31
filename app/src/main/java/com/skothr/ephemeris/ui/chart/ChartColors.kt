package com.skothr.ephemeris.ui.chart

import androidx.compose.ui.graphics.Color
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.Element
import com.skothr.ephemeris.settings.model.AppTheme

object ChartColors {
    // Element colors used for zodiac sign arc fills — same in both themes
    val elementColor = mapOf(
        Element.FIRE to Color(0xFFE53935),
        Element.EARTH to Color(0xFF43A047),
        Element.AIR to Color(0xFFFFB300),
        Element.WATER to Color(0xFF1E88E5),
    )

    // Aspect colors — same in both themes (colored lines on greyscale chart)
    val aspectColor = mapOf(
        Aspect.CONJUNCTION to Color(0xFF66BB6A),
        Aspect.OPPOSITION to Color(0xFFEF5350),
        Aspect.TRINE to Color(0xFF42A5F5),
        Aspect.SQUARE to Color(0xFFEF5350),
        Aspect.SEXTILE to Color(0xFF42A5F5),
        Aspect.QUINCUNX to Color(0xFFAB47BC),
        Aspect.SEMI_SEXTILE to Color(0xFF78909C),
        Aspect.SEMI_SQUARE to Color(0xFFFF7043),
        Aspect.SESQUIQUADRATE to Color(0xFFFF7043),
    )

    // Area behind the chart and controls
    fun appBackground(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFF121212)
        AppTheme.LIGHT -> Color(0xFF2A2622)
    }

    // Dark: light-on-dark. Light: muted warm mid-tone, dark strokes.
    fun wheelBackground(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFF181818)
        AppTheme.LIGHT -> Color(0xFF8C8070)
    }

    fun ringStroke(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFF555555)
        AppTheme.LIGHT -> Color(0xFF5C5040)
    }

    fun bodyGlyph(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFFDDDDDD)
        AppTheme.LIGHT -> Color(0xFFF0E8D8)
    }

    fun angleMarker(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFFCCCCCC)
        AppTheme.LIGHT -> Color(0xFFE8DCC8)
    }

    fun houseNumber(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFF888888)
        AppTheme.LIGHT -> Color(0xFFB0A090)
    }

    fun signGlyph(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> Color(0xFFDDDDDD)
        AppTheme.LIGHT -> Color(0xFFF0E8D8)
    }

    fun zodiacArcAlpha(theme: AppTheme) = when (theme) {
        AppTheme.DARK -> 0.20f
        AppTheme.LIGHT -> 0.20f
    }
}
