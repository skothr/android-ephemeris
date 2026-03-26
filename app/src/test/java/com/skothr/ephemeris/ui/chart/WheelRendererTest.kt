package com.skothr.ephemeris.ui.chart

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class WheelRendererTest {

    @Test
    fun `longitudeToAngle converts 0 degrees longitude to top of chart (ASC left)`() {
        val angle = WheelMath.longitudeToAngle(0.0, ascendant = 0.0)
        assertEquals(PI, angle, 0.001)
    }

    @Test
    fun `longitudeToAngle rotates based on ascendant`() {
        val angle = WheelMath.longitudeToAngle(90.0, ascendant = 90.0)
        assertEquals(PI, angle, 0.001)
    }

    @Test
    fun `longitudeToAngle 180 degrees from ASC is at right`() {
        val angle = WheelMath.longitudeToAngle(180.0, ascendant = 0.0)
        assertEquals(0.0, angle, 0.001)
    }

    @Test
    fun `pointOnCircle at angle 0 is at right`() {
        val (x, y) = WheelMath.pointOnCircle(100f, 100f, 50f, 0.0)
        assertEquals(150f, x, 0.1f)
        assertEquals(100f, y, 0.1f)
    }

    @Test
    fun `pointOnCircle at angle PI_2 is at bottom`() {
        val (x, y) = WheelMath.pointOnCircle(100f, 100f, 50f, PI / 2.0)
        assertEquals(100f, x, 0.1f)
        assertEquals(150f, y, 0.1f)
    }

    @Test
    fun `pointOnCircle at angle PI is at left`() {
        val (x, y) = WheelMath.pointOnCircle(100f, 100f, 50f, PI)
        assertEquals(50f, x, 0.1f)
        assertEquals(100f, y, 0.1f)
    }

    @Test
    fun `ringRadii are properly nested`() {
        val radii = WheelMath.calculateRingRadii(500f)
        assertTrue("Zodiac outer > zodiac inner", radii.zodiacOuter > radii.zodiacInner)
        assertTrue("Zodiac inner > house outer", radii.zodiacInner > radii.houseOuter)
        assertTrue("House outer > house inner", radii.houseOuter > radii.houseInner)
        assertTrue("House inner > body ring", radii.houseInner > radii.bodyRing)
        assertTrue("Body ring > aspect inner", radii.bodyRing > radii.aspectInner)
        assertTrue("All radii positive", radii.aspectInner > 0f)
    }

    @Test
    fun `resolveCollisions separates overlapping bodies`() {
        val inputAngles = listOf(1.0, 1.0, 3.0)
        val minSeparation = 0.2
        val resolved = WheelMath.resolveCollisions(inputAngles, minSeparation)
        for (i in resolved.indices) {
            for (j in i + 1 until resolved.size) {
                val diff = kotlin.math.abs(resolved[i] - resolved[j])
                assertTrue("Bodies at $i and $j should be >= $minSeparation apart, got $diff",
                    diff >= minSeparation - 0.001)
            }
        }
    }

    @Test
    fun `resolveCollisions preserves order`() {
        val inputAngles = listOf(1.0, 1.05, 3.0)
        val resolved = WheelMath.resolveCollisions(inputAngles, 0.2)
        for (i in 0 until resolved.size - 1) {
            assertTrue(resolved[i] <= resolved[i + 1])
        }
    }

    @Test
    fun `resolveCollisions no change when already separated`() {
        val inputAngles = listOf(1.0, 2.0, 3.0)
        val resolved = WheelMath.resolveCollisions(inputAngles, 0.2)
        assertEquals(1.0, resolved[0], 0.001)
        assertEquals(2.0, resolved[1], 0.001)
        assertEquals(3.0, resolved[2], 0.001)
    }
}
