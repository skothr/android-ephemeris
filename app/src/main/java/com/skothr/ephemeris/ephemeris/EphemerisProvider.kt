package com.skothr.ephemeris.ephemeris

import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import java.time.LocalDateTime

interface EphemerisProvider {
    suspend fun julianDay(dateTime: LocalDateTime): Double
    suspend fun calculateBody(julianDay: Double, bodyId: Int, flags: Int): CelestialPosition
    suspend fun calculateHouses(
        julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
    ): HouseData
    suspend fun setSiderealMode(ayanamsa: Int)
    suspend fun setTopographicPosition(longitude: Double, latitude: Double, altitude: Double)
}
