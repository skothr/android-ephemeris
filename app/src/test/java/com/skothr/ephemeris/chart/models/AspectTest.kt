package com.skothr.ephemeris.chart.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AspectTest {

    @Test
    fun `all aspects have correct angles`() {
        assertEquals(0.0, Aspect.CONJUNCTION.angle, 0.001)
        assertEquals(180.0, Aspect.OPPOSITION.angle, 0.001)
        assertEquals(120.0, Aspect.TRINE.angle, 0.001)
        assertEquals(90.0, Aspect.SQUARE.angle, 0.001)
        assertEquals(60.0, Aspect.SEXTILE.angle, 0.001)
        assertEquals(150.0, Aspect.QUINCUNX.angle, 0.001)
        assertEquals(30.0, Aspect.SEMI_SEXTILE.angle, 0.001)
        assertEquals(45.0, Aspect.SEMI_SQUARE.angle, 0.001)
        assertEquals(135.0, Aspect.SESQUIQUADRATE.angle, 0.001)
    }

    @Test
    fun `all aspects have correct default orbs`() {
        assertEquals(8.0, Aspect.CONJUNCTION.defaultOrb, 0.001)
        assertEquals(8.0, Aspect.OPPOSITION.defaultOrb, 0.001)
        assertEquals(8.0, Aspect.TRINE.defaultOrb, 0.001)
        assertEquals(7.0, Aspect.SQUARE.defaultOrb, 0.001)
        assertEquals(6.0, Aspect.SEXTILE.defaultOrb, 0.001)
        assertEquals(3.0, Aspect.QUINCUNX.defaultOrb, 0.001)
        assertEquals(2.0, Aspect.SEMI_SEXTILE.defaultOrb, 0.001)
        assertEquals(2.0, Aspect.SEMI_SQUARE.defaultOrb, 0.001)
        assertEquals(2.0, Aspect.SESQUIQUADRATE.defaultOrb, 0.001)
    }

    @Test
    fun `major aspects are correctly identified`() {
        assertTrue(Aspect.CONJUNCTION.isMajor)
        assertTrue(Aspect.OPPOSITION.isMajor)
        assertTrue(Aspect.TRINE.isMajor)
        assertTrue(Aspect.SQUARE.isMajor)
        assertTrue(Aspect.SEXTILE.isMajor)
    }

    @Test
    fun `minor aspects are correctly identified`() {
        assertFalse(Aspect.QUINCUNX.isMajor)
        assertFalse(Aspect.SEMI_SEXTILE.isMajor)
        assertFalse(Aspect.SEMI_SQUARE.isMajor)
        assertFalse(Aspect.SESQUIQUADRATE.isMajor)
    }

    @Test
    fun `all aspects have non-empty display names`() {
        for (aspect in Aspect.entries) {
            assertTrue("${aspect.name} has empty displayName", aspect.displayName.isNotEmpty())
        }
    }

    @Test
    fun `all aspects have non-empty symbols`() {
        for (aspect in Aspect.entries) {
            assertTrue("${aspect.name} has empty symbol", aspect.symbol.isNotEmpty())
        }
    }

    @Test
    fun `enum contains exactly 9 aspects`() {
        assertEquals(9, Aspect.entries.size)
    }
}
