package com.skothr.ephemeris.chart.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CelestialBodyTest {

    @Test
    fun `all bodies have correct Swiss Ephemeris IDs`() {
        assertEquals(0, CelestialBody.SUN.swissEphId)
        assertEquals(1, CelestialBody.MOON.swissEphId)
        assertEquals(2, CelestialBody.MERCURY.swissEphId)
        assertEquals(3, CelestialBody.VENUS.swissEphId)
        assertEquals(4, CelestialBody.MARS.swissEphId)
        assertEquals(5, CelestialBody.JUPITER.swissEphId)
        assertEquals(6, CelestialBody.SATURN.swissEphId)
        assertEquals(7, CelestialBody.URANUS.swissEphId)
        assertEquals(8, CelestialBody.NEPTUNE.swissEphId)
        assertEquals(9, CelestialBody.PLUTO.swissEphId)
        assertEquals(15, CelestialBody.CHIRON.swissEphId)
        assertEquals(11, CelestialBody.NORTH_NODE.swissEphId)
        assertEquals(12, CelestialBody.LILITH.swissEphId)
    }

    @Test
    fun `all bodies have non-empty display names`() {
        for (body in CelestialBody.entries) {
            assertTrue("${body.name} has empty displayName", body.displayName.isNotEmpty())
        }
    }

    @Test
    fun `all bodies have non-empty symbols`() {
        for (body in CelestialBody.entries) {
            assertTrue("${body.name} has empty symbol", body.symbol.isNotEmpty())
        }
    }

    @Test
    fun `enum contains exactly 13 bodies`() {
        assertEquals(13, CelestialBody.entries.size)
    }

    @Test
    fun `display names are human readable`() {
        assertEquals("Sun", CelestialBody.SUN.displayName)
        assertEquals("Moon", CelestialBody.MOON.displayName)
        assertEquals("Mercury", CelestialBody.MERCURY.displayName)
        assertEquals("Venus", CelestialBody.VENUS.displayName)
        assertEquals("Mars", CelestialBody.MARS.displayName)
        assertEquals("Jupiter", CelestialBody.JUPITER.displayName)
        assertEquals("Saturn", CelestialBody.SATURN.displayName)
        assertEquals("Uranus", CelestialBody.URANUS.displayName)
        assertEquals("Neptune", CelestialBody.NEPTUNE.displayName)
        assertEquals("Pluto", CelestialBody.PLUTO.displayName)
        assertEquals("Chiron", CelestialBody.CHIRON.displayName)
        assertEquals("North Node", CelestialBody.NORTH_NODE.displayName)
        assertEquals("Lilith", CelestialBody.LILITH.displayName)
    }
}
