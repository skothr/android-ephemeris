package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.config.AspectConfig
import com.skothr.ephemeris.chart.config.BodyConfig
import com.skothr.ephemeris.chart.models.*
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
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
        CelestialBody.SUN to CelestialPosition(280.0, 0.0, 1.0, 1.0),
        CelestialBody.MOON to CelestialPosition(45.0, 5.0, 0.002, 13.0),
        CelestialBody.MERCURY to CelestialPosition(275.0, -1.0, 0.8, 1.5),
    )

    private val fakeEphemeris = object : EphemerisProvider {
        override suspend fun julianDay(dateTime: LocalDateTime): Double = 2451545.0
        override suspend fun calculateBody(julianDay: Double, body: CelestialBody): CelestialPosition =
            fakePositions[body] ?: CelestialPosition(0.0, 0.0, 1.0, 1.0)
        override suspend fun calculateHouses(
            julianDay: Double, latitude: Double, longitude: Double, system: HouseSystem
        ): HouseData = fakeHouseData
    }

    private val bodyConfig = BodyConfig(enabledBodies = setOf(
        CelestialBody.SUN, CelestialBody.MOON, CelestialBody.MERCURY
    ))

    @Test
    fun `calculate returns ChartData with all enabled bodies`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), HouseSystem.PLACIDUS,
        )
        assertEquals(3, result.positions.size)
        assertTrue(result.positions.containsKey(CelestialBody.SUN))
        assertTrue(result.positions.containsKey(CelestialBody.MOON))
        assertTrue(result.positions.containsKey(CelestialBody.MERCURY))
    }

    @Test
    fun `calculate includes house data`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), HouseSystem.PLACIDUS,
        )
        assertEquals(12, result.houseData.cusps.size)
        assertEquals(0.0, result.houseData.ascendant, 0.001)
    }

    @Test
    fun `calculate includes aspects`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), HouseSystem.PLACIDUS,
        )
        assertTrue(result.aspects.any {
            it.aspect == Aspect.CONJUNCTION && it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MERCURY
        })
    }

    @Test
    fun `calculate stores correct metadata`() = runTest {
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), bodyConfig)
        val dt = LocalDateTime.of(2024, 6, 15, 12, 0, 0)
        val loc = Location(40.7128, -74.006)
        val result = calc.calculate(dt, loc, HouseSystem.PLACIDUS)
        assertEquals(dt, result.dateTime)
        assertEquals(loc, result.location)
        assertEquals(HouseSystem.PLACIDUS, result.houseSystem)
    }

    @Test
    fun `disabled bodies are excluded`() = runTest {
        val smallConfig = BodyConfig(enabledBodies = setOf(CelestialBody.SUN))
        val calc = ChartCalculator(fakeEphemeris, AspectConfig(), smallConfig)
        val result = calc.calculate(
            LocalDateTime.of(2024, 6, 15, 12, 0, 0), Location(40.7128, -74.006), HouseSystem.PLACIDUS,
        )
        assertEquals(1, result.positions.size)
        assertTrue(result.positions.containsKey(CelestialBody.SUN))
    }
}
