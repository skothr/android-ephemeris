package com.skothr.ephemeris.ui.chart

import androidx.compose.ui.graphics.Color
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.Element

object ChartColors {
    val elementColor = mapOf(
        Element.FIRE to Color(0xFFE53935),
        Element.EARTH to Color(0xFF43A047),
        Element.AIR to Color(0xFFFFB300),
        Element.WATER to Color(0xFF1E88E5),
    )

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

    val wheelBackground = Color(0xFF1A1A2E)
    val ringStroke = Color(0xFF4A4A6A)
    val bodyGlyph = Color(0xFFE0E0E0)
    val angleMarker = Color(0xFFFFD54F)
    val houseNumber = Color(0xFF9E9E9E)
    val signGlyph = Color(0xFFFFFFFF)
}
