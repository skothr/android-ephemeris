package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.config.BodyConfig
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.chart.models.CelestialBody
import java.time.LocalDateTime

class ChartCalculator(
    private val ephemeris: EphemerisProvider,
    private val aspectConfig: AspectConfig,
    private val bodyConfig: BodyConfig,
) {
    private val aspectCalculator = AspectCalculator(aspectConfig)

    suspend fun calculate(
        dateTime: LocalDateTime, location: Location, houseSystem: HouseSystem,
    ): ChartData {
        val jd = ephemeris.julianDay(dateTime)
        val positions = mutableMapOf<CelestialBody, CelestialPosition>()
        for (body in bodyConfig.enabledBodies) {
            positions[body] = ephemeris.calculateBody(jd, body)
        }
        val houseData = ephemeris.calculateHouses(jd, location.latitude, location.longitude, houseSystem)
        val aspects = aspectCalculator.calculate(positions)
        return ChartData(dateTime = dateTime, location = location, positions = positions,
            houseData = houseData, aspects = aspects, houseSystem = houseSystem)
    }
}
