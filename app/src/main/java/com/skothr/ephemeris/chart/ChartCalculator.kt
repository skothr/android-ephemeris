package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.settings.model.AppSettings
import com.skothr.ephemeris.settings.model.CalculationSettings
import com.skothr.ephemeris.settings.model.Center
import com.skothr.ephemeris.settings.model.NodeType
import com.skothr.ephemeris.settings.model.ZodiacType
import java.time.LocalDateTime

class ChartCalculator(private val ephemeris: EphemerisProvider) {

    suspend fun calculate(
        dateTime: LocalDateTime, location: Location, settings: AppSettings,
    ): ChartData {
        val calc = settings.calculation
        val display = settings.display

        // Configure sidereal mode if needed
        if (calc.zodiacType == ZodiacType.SIDEREAL) {
            ephemeris.setSiderealMode(calc.ayanamsa.swissEphId)
        }

        // Configure topocentric position if needed
        if (calc.center == Center.TOPOCENTRIC) {
            ephemeris.setTopographicPosition(location.longitude, location.latitude, 0.0)
        }

        val flags = calc.toSwissEphFlags()
        val jd = ephemeris.julianDay(dateTime)

        val positions = mutableMapOf<CelestialBody, CelestialPosition>()
        for (body in display.enabledBodies) {
            // For North Node, switch body ID based on NodeType setting
            val bodyId = if (body == CelestialBody.NORTH_NODE && calc.nodeType == NodeType.MEAN_NODE) {
                CalculationSettings.SE_MEAN_NODE
            } else {
                body.swissEphId
            }
            positions[body] = ephemeris.calculateBody(jd, bodyId, flags)
        }

        val houseData = ephemeris.calculateHouses(
            jd, location.latitude, location.longitude, calc.houseSystem
        )

        val aspectCalculator = AspectCalculator(display.enabledAspects, display.aspectOrbs)
        val aspects = aspectCalculator.calculate(positions)

        return ChartData(
            dateTime = dateTime, location = location, positions = positions,
            houseData = houseData, aspects = aspects, houseSystem = calc.houseSystem,
        )
    }
}
