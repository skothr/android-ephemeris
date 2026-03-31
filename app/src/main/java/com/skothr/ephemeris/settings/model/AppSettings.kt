package com.skothr.ephemeris.settings.model

import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem

data class CalculationSettings(
    val zodiacType: ZodiacType = ZodiacType.TROPICAL,
    val ayanamsa: Ayanamsa = Ayanamsa.LAHIRI,
    val nodeType: NodeType = NodeType.TRUE_NODE,
    val center: Center = Center.GEOCENTRIC,
    val houseSystem: HouseSystem = HouseSystem.PLACIDUS,
    val speedInLongitude: Boolean = true,
    val equatorialCoordinates: Boolean = false,
) {
    fun toSwissEphFlags(): Int {
        var flags = SEFLG_SWIEPH
        if (speedInLongitude) flags = flags or SEFLG_SPEED
        if (zodiacType == ZodiacType.SIDEREAL) flags = flags or SEFLG_SIDEREAL
        if (center == Center.HELIOCENTRIC) flags = flags or SEFLG_HELCTR
        if (center == Center.TOPOCENTRIC) flags = flags or SEFLG_TOPOCTR
        if (equatorialCoordinates) flags = flags or SEFLG_EQUATORIAL
        return flags
    }

    // Note: NodeType is NOT a flag — it's handled by switching the body ID
    // for North Node between SE_TRUE_NODE (11) and SE_MEAN_NODE (10).
    // See ChartCalculator for the body ID mapping.

    companion object {
        const val SEFLG_SWIEPH = 2
        const val SEFLG_HELCTR = 8
        const val SEFLG_SPEED = 256
        const val SEFLG_EQUATORIAL = 2 * 1024
        const val SEFLG_TOPOCTR = 32 * 1024
        const val SEFLG_SIDEREAL = 64 * 1024

        const val SE_MEAN_NODE = 10
        const val SE_TRUE_NODE = 11
    }
}

data class DisplaySettings(
    val enabledBodies: Set<CelestialBody> = CelestialBody.entries.toSet(),
    val enabledAspects: Set<Aspect> = Aspect.entries.toSet(),
    val aspectOrbs: Map<Aspect, Double> = Aspect.entries.associateWith { it.defaultOrb },
)

data class VisualSettings(
    val theme: AppTheme = AppTheme.DARK,
    val symbolStyle: SymbolStyle = SymbolStyle.SYSTEM,
    val lockAscendant: Boolean = false,
    val coloredZodiacBands: Boolean = false,
    val zodiacOuterRadius: Float = 0.95f,
    val zodiacInnerRadius: Float = 0.82f,
    val houseOuterRadius: Float = 0.82f,
    val houseInnerRadius: Float = 0.68f,
    val bodyRingRadius: Float = 0.58f,
    val aspectInnerRadius: Float = 0.10f,
    val aspectLineThickness: Float = 2.0f,
    val aspectLineOpacity: Float = 0.8f,
    val majorAspectStyle: LineStyle = LineStyle.SOLID,
    val minorAspectStyle: LineStyle = LineStyle.DASHED,
    val scaleWidthByOrb: Boolean = false,
    val widthScaleOrb: Float = 6.0f,
)

data class AppSettings(
    val calculation: CalculationSettings = CalculationSettings(),
    val display: DisplaySettings = DisplaySettings(),
    val visual: VisualSettings = VisualSettings(),
)
