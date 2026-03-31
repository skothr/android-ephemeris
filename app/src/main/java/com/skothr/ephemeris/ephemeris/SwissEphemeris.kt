package com.skothr.ephemeris.ephemeris

import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime

class SwissEphemeris : EphemerisProvider {

    companion object {
        init {
            System.loadLibrary("swisseph_jni")
        }
    }

    private val mutex = Mutex()
    private var initialized = false

    suspend fun init(ephePath: String) = mutex.withLock {
        nativeInit(ephePath)
        initialized = true
    }

    suspend fun close() = mutex.withLock {
        nativeClose()
        initialized = false
    }

    override suspend fun julianDay(dateTime: LocalDateTime): Double = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        val hour = dateTime.hour + dateTime.minute / 60.0 + dateTime.second / 3600.0
        nativeJulianDay(dateTime.year, dateTime.monthValue, dateTime.dayOfMonth, hour)
    }

    override suspend fun calculateBody(julianDay: Double, bodyId: Int, flags: Int): CelestialPosition =
        mutex.withLock {
            check(initialized) { "SwissEphemeris not initialized" }
            val result = nativeCalculateBodyWithFlags(julianDay, bodyId, flags)
                ?: throw RuntimeException("Failed to calculate position for body $bodyId")
            CelestialPosition(
                longitude = result[0],
                latitude = result[1],
                distance = result[2],
                speed = result[3],
            )
        }

    override suspend fun calculateHouses(
        julianDay: Double,
        latitude: Double,
        longitude: Double,
        system: HouseSystem,
    ): HouseData = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        val result = nativeCalculateHouses(julianDay, latitude, longitude, system.swissEphCode)
            ?: throw RuntimeException("Failed to calculate houses for ${system.name}")
        HouseData(
            cusps = result.take(12),
            ascendant = result[12],
            midheaven = result[13],
            descendant = result[14],
            imumCoeli = result[15],
        )
    }

    override suspend fun setSiderealMode(ayanamsa: Int) = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        nativeSetSiderealMode(ayanamsa)
    }

    override suspend fun setTopographicPosition(longitude: Double, latitude: Double, altitude: Double) = mutex.withLock {
        check(initialized) { "SwissEphemeris not initialized" }
        nativeSetTopographicPosition(longitude, latitude, altitude)
    }

    private external fun nativeInit(ephePath: String)
    private external fun nativeClose()
    private external fun nativeJulianDay(year: Int, month: Int, day: Int, hour: Double): Double
    private external fun nativeCalculateBody(julianDay: Double, bodyId: Int): DoubleArray?
    private external fun nativeCalculateBodyWithFlags(julianDay: Double, bodyId: Int, flags: Int): DoubleArray?
    private external fun nativeCalculateHouses(
        julianDay: Double, lat: Double, lon: Double, houseSystem: Char
    ): DoubleArray?
    private external fun nativeSetSiderealMode(sidMode: Int)
    private external fun nativeSetTopographicPosition(lon: Double, lat: Double, alt: Double)
}
