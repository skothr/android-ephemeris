package com.skothr.ephemeris.ephemeris

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.HouseSystem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class SwissEphemerisTest {

    private lateinit var swe: SwissEphemeris
    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val epheDir = File(context.filesDir, "ephe")
        if (!epheDir.exists()) {
            epheDir.mkdirs()
            context.assets.list("ephe")?.forEach { filename ->
                context.assets.open("ephe/$filename").use { input ->
                    File(epheDir, filename).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        swe = SwissEphemeris()
        swe.init(epheDir.absolutePath)
    }

    @After
    fun teardown() = runTest { swe.close() }

    @Test
    fun julianDay_j2000Epoch_returnsCorrectValue() = runTest {
        val dt = LocalDateTime.of(2000, 1, 1, 12, 0, 0)
        val jd = swe.julianDay(dt)
        assertEquals(2451545.0, jd, 0.001)
    }

    @Test
    fun julianDay_unixEpoch_returnsCorrectValue() = runTest {
        val dt = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
        val jd = swe.julianDay(dt)
        assertEquals(2440587.5, jd, 0.001)
    }

    @Test
    fun calculateBody_sunAtJ2000_longitudeNear280() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2000, 1, 1, 12, 0, 0))
        val pos = swe.calculateBody(jd, CelestialBody.SUN)
        assertTrue("Sun longitude should be near 280, got ${pos.longitude}",
            pos.longitude in 279.0..282.0)
    }

    @Test
    fun calculateBody_allBodiesReturnValidLongitudes() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        CelestialBody.entries.forEach { body ->
            val pos = swe.calculateBody(jd, body)
            assertTrue("${body.name} longitude should be 0-360, got ${pos.longitude}",
                pos.longitude in 0.0..360.0)
        }
    }

    @Test
    fun calculateBody_sunHasPositiveSpeed() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        val pos = swe.calculateBody(jd, CelestialBody.SUN)
        assertTrue("Sun should always move direct", pos.speed > 0.0)
        assertFalse(pos.isRetrograde)
    }

    @Test
    fun calculateHouses_placidus_returns12Cusps() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        val houses = swe.calculateHouses(jd, 40.7128, -74.0060, HouseSystem.PLACIDUS)
        assertEquals(12, houses.cusps.size)
        houses.cusps.forEach { cusp ->
            assertTrue("Cusp should be 0-360, got $cusp", cusp in 0.0..360.0)
        }
    }

    @Test
    fun calculateHouses_allSystems_returnValidData() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        HouseSystem.entries.forEach { system ->
            val houses = swe.calculateHouses(jd, 40.7128, -74.0060, system)
            assertEquals("${system.name} should have 12 cusps", 12, houses.cusps.size)
            assertTrue("${system.name} ASC should be 0-360", houses.ascendant in 0.0..360.0)
            assertTrue("${system.name} MC should be 0-360", houses.midheaven in 0.0..360.0)
        }
    }

    @Test
    fun calculateHouses_dscIsOppositeAsc() = runTest {
        val jd = swe.julianDay(LocalDateTime.of(2024, 6, 15, 12, 0, 0))
        val houses = swe.calculateHouses(jd, 40.7128, -74.0060, HouseSystem.PLACIDUS)
        val expectedDsc = (houses.ascendant + 180.0) % 360.0
        assertEquals(expectedDsc, houses.descendant, 0.01)
    }
}
