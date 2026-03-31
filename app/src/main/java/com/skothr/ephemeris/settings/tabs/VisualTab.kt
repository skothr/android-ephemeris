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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.skothr.ephemeris.R
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
    onLockAscendantChanged: (Boolean) -> Unit,
    onColoredZodiacBandsChanged: (Boolean) -> Unit,
    onZodiacOuterChanged: (Float) -> Unit,
    onZodiacInnerChanged: (Float) -> Unit,
    onHouseOuterChanged: (Float) -> Unit,
    onHouseInnerChanged: (Float) -> Unit,
    onBodyRingChanged: (Float) -> Unit,
    onAspectInnerChanged: (Float) -> Unit,
    onAspectThicknessChanged: (Float) -> Unit,
    onAspectOpacityChanged: (Float) -> Unit,
    onMajorStyleChanged: (LineStyle) -> Unit,
    onMinorStyleChanged: (LineStyle) -> Unit,
    onScaleWidthByOrbChanged: (Boolean) -> Unit,
    onWidthScaleOrbChanged: (Float) -> Unit,
) {
    val astroFontFamily = FontFamily(Font(R.font.astronomicon))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Chart Theme
        SectionLabel("Chart Theme")
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
            val useAstro = settings.symbolStyle == SymbolStyle.ASTRO
            val previewSymbols = listOf(
                if (useAstro) ZodiacSign.ARIES.astroSymbol else ZodiacSign.ARIES.symbol,
                if (useAstro) ZodiacSign.TAURUS.astroSymbol else ZodiacSign.TAURUS.symbol,
                if (useAstro) ZodiacSign.GEMINI.astroSymbol else ZodiacSign.GEMINI.symbol,
                if (useAstro) ZodiacSign.CANCER.astroSymbol else ZodiacSign.CANCER.symbol,
                if (useAstro) CelestialBody.SUN.astroSymbol else CelestialBody.SUN.symbol,
                if (useAstro) CelestialBody.MOON.astroSymbol else CelestialBody.MOON.symbol,
                if (useAstro) CelestialBody.MARS.astroSymbol else CelestialBody.MARS.symbol,
                if (useAstro) CelestialBody.JUPITER.astroSymbol else CelestialBody.JUPITER.symbol,
            )
            val fontFamily = if (useAstro) astroFontFamily else null
            previewSymbols.forEach { symbol ->
                Text(symbol, fontSize = 24.sp, fontFamily = fontFamily)
            }
        }

        // Chart options
        SectionLabel("Chart")
        SettingsToggleRow(
            label = "Lock ascendant to left",
            checked = settings.lockAscendant,
            onCheckedChange = onLockAscendantChanged,
        )
        SettingsToggleRow(
            label = "Colored zodiac bands",
            checked = settings.coloredZodiacBands,
            onCheckedChange = onColoredZodiacBandsChanged,
        )

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
        SettingsSlider(
            label = "Aspect Inner",
            value = settings.aspectInnerRadius,
            onValueChange = onAspectInnerChanged,
            valueRange = 0.0f..0.50f,
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
        SettingsToggleRow(
            label = "Scale width by orb",
            checked = settings.scaleWidthByOrb,
            onCheckedChange = onScaleWidthByOrbChanged,
        )
        if (settings.scaleWidthByOrb) {
            SettingsSlider(
                label = "Width Scale Orb",
                value = settings.widthScaleOrb,
                onValueChange = onWidthScaleOrbChanged,
                valueRange = 1.0f..15.0f,
                formatValue = { "%.1f°".format(it) },
            )
        }
    }
}
