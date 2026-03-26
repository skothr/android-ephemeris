package com.skothr.ephemeris.ephemeris

import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import java.time.LocalDateTime

interface EphemerisProvider {
    suspend fun julianDay(dateTime: LocalDateTime): Double
    suspend fun calculateBody(julianDay: Double, body: CelestialBody): CelestialPosition
    suspend fun calculateHouses(
        julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
    ): HouseData
}
