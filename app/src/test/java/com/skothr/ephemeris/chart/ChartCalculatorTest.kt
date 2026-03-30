package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.models.*
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import com.skothr.ephemeris.settings.model.AppSettings
import com.skothr.ephemeris.settings.model.DisplaySettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ChartCalculatorTest {

    private val fakeHouseData = HouseData(
        cusps = List(12) { it * 30.0 },
        ascendant = 0.0, midheaven = 270.0, descendant = 180.0, imumCoeli = 90.0,
    )

    private val fakePositions = mapOf(
        0 to CelestialPosition(280.0, 0.0, 1.0, 1.0),   // Sun
        1 to CelestialPosition(45.0, 5.0, 0.002, 13.0),  // Moon
        2 to CelestialPosition(275.0, -1.0, 0.8, 1.5),   // Mercury
    )

    private val fakeEphemeris = object : EphemerisProvider {
        override suspend fun julianDay(dateTime: LocalDateTime): Double = 2451545.0
        override suspend fun calculateBody(julianDay: Double, bodyId: Int, flags: Int): CelestialPosition =
            fakePositions[bodyId] ?: CelestialPosition(0.0, 0.0, 1.0, 1.0)
        override suspend fun calculateHouses(
            julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
        ): HouseData = fakeHouseData
        override suspend fun setSiderealMode(ayanamsa: Int) {}
        override suspend fun setTopographicPosition(longitude: Double, latitude: Double, altitude: Double) {}
    }

    private val threeBodySettings = AppSettings(
        display = DisplaySettings(
            enabledBodies = setOf(CelestialBody.SUN, CelestialBody.MOON, CelestialBody.MERCURY),
        ),
    )

    @Test
    fun `calculate returns ChartData with all enabled bodies`() = runTest {
        val calc = ChartCalculator(fakeEphemeris)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), threeBodySettings,
        )
        assertEquals(3, result.positions.size)
        assertTrue(result.positions.containsKey(CelestialBody.SUN))
        assertTrue(result.positions.containsKey(CelestialBody.MOON))
        assertTrue(result.positions.containsKey(CelestialBody.MERCURY))
    }

    @Test
    fun `calculate includes house data`() = runTest {
        val calc = ChartCalculator(fakeEphemeris)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), threeBodySettings,
        )
        assertEquals(12, result.houseData.cusps.size)
        assertEquals(0.0, result.houseData.ascendant, 0.001)
    }

    @Test
    fun `calculate includes aspects`() = runTest {
        val calc = ChartCalculator(fakeEphemeris)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), threeBodySettings,
        )
        assertTrue(result.aspects.any {
            it.aspect == Aspect.CONJUNCTION && it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MERCURY
        })
    }

    @Test
    fun `calculate stores correct metadata`() = runTest {
        val calc = ChartCalculator(fakeEphemeris)
        val dt = LocalDateTime.of(2024, 6, 15, 12, 0, 0)
        val loc = Location(40.7128, -74.006)
        val result = calc.calculate(dt, loc, threeBodySettings)
        assertEquals(dt, result.dateTime)
        assertEquals(loc, result.location)
        assertEquals(HouseSystem.PLACIDUS, result.houseSystem)
    }

    @Test
    fun `disabled bodies are excluded`() = runTest {
        val sunOnlySettings = AppSettings(
            display = DisplaySettings(enabledBodies = setOf(CelestialBody.SUN)),
        )
        val calc = ChartCalculator(fakeEphemeris)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), sunOnlySettings,
        )
        assertEquals(1, result.positions.size)
        assertTrue(result.positions.containsKey(CelestialBody.SUN))
    }
}
