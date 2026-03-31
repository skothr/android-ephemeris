package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.ZodiacSign
import com.skothr.ephemeris.settings.components.SegmentedToggle
import com.skothr.ephemeris.settings.components.SettingsSlider
import com.skothr.ephemeris.settings.model.*

@Composable
fun VisualTab(
    settings: VisualSettings,
    onThemeChanged: (AppTheme) -> Unit,
    onSymbolStyleChanged: (SymbolStyle) -> Unit,
    onZodiacOuterChanged: (Float) -> Unit,
    onZodiacInnerChanged: (Float) -> Unit,
    onHouseOuterChanged: (Float) -> Unit,
    onHouseInnerChanged: (Float) -> Unit,
    onBodyRingChanged: (Float) -> Unit,
    onAspectThicknessChanged: (Float) -> Unit,
    onAspectOpacityChanged: (Float) -> Unit,
    onMajorStyleChanged: (LineStyle) -> Unit,
    onMinorStyleChanged: (LineStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Theme
        SectionLabel("Theme")
        SegmentedToggle(
            options = AppTheme.entries,
            selected = settings.theme,
            onSelected = onThemeChanged,
            label = { it.displayName },
        )

        // Symbol Style
        SectionLabel("Symbol Style")
        SegmentedToggle(
            options = SymbolStyle.entries,
            selected = settings.symbolStyle,
            onSelected = onSymbolStyleChanged,
            label = { it.displayName },
        )

        // Glyph preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp),
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val previewSymbols = listOf(
                ZodiacSign.ARIES.symbol, ZodiacSign.TAURUS.symbol,
                ZodiacSign.GEMINI.symbol, ZodiacSign.CANCER.symbol,
                CelestialBody.SUN.symbol, CelestialBody.MOON.symbol,
                CelestialBody.MARS.symbol, CelestialBody.JUPITER.symbol,
            )
            previewSymbols.forEach { symbol ->
                Text(symbol, fontSize = 24.sp)
            }
        }

        // Ring Proportions
        SectionLabel("Ring Proportions")
        SettingsSlider(
            label = "Zodiac Outer",
            value = settings.zodiacOuterRadius,
            onValueChange = onZodiacOuterChanged,
            valueRange = 0.80f..1.00f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "Zodiac Inner",
            value = settings.zodiacInnerRadius,
            onValueChange = onZodiacInnerChanged,
            valueRange = 0.70f..0.90f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "House Outer",
            value = settings.houseOuterRadius,
            onValueChange = onHouseOuterChanged,
            valueRange = 0.60f..0.90f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "House Inner",
            value = settings.houseInnerRadius,
            onValueChange = onHouseInnerChanged,
            valueRange = 0.50f..0.80f,
            formatValue = { "${(it * 100).toInt()}%" },
        )
        SettingsSlider(
            label = "Body Ring",
            value = settings.bodyRingRadius,
            onValueChange = onBodyRingChanged,
            valueRange = 0.40f..0.70f,
            formatValue = { "${(it * 100).toInt()}%" },
        )

        // Aspect Lines
        SectionLabel("Aspect Lines")
        SettingsSlider(
            label = "Line Thickness",
            value = settings.aspectLineThickness,
            onValueChange = onAspectThicknessChanged,
            valueRange = 0.5f..5.0f,
            formatValue = { "%.1f".format(it) },
        )
        SettingsSlider(
            label = "Line Opacity",
            value = settings.aspectLineOpacity,
            onValueChange = onAspectOpacityChanged,
            valueRange = 0.1f..1.0f,
            formatValue = { "${(it * 100).toInt()}%" },
        )

        SectionLabel("Major Aspect Style")
        SegmentedToggle(
            options = LineStyle.entries,
            selected = settings.majorAspectStyle,
            onSelected = onMajorStyleChanged,
            label = { it.displayName },
        )

        SectionLabel("Minor Aspect Style")
        SegmentedToggle(
            options = LineStyle.entries,
            selected = settings.minorAspectStyle,
            onSelected = onMinorStyleChanged,
            label = { it.displayName },
        )
    }
}
