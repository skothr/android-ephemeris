package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.models.*
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AspectCalculatorTest {

    private lateinit var calculator: AspectCalculator

    @Before
    fun setup() {
        calculator = AspectCalculator(
            enabledAspects = Aspect.entries.toSet(),
            aspectOrbs = Aspect.entries.associateWith { it.defaultOrb },
        )
    }

    private fun pos(longitude: Double, speed: Double = 1.0) =
        CelestialPosition(longitude, 0.0, 1.0, speed)

    @Test
    fun `exact conjunction detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(100.0),
        )
        val aspects = calculator.calculate(positions)
        assertEquals(1, aspects.size)
        assertEquals(Aspect.CONJUNCTION, aspects[0].aspect)
        assertEquals(0.0, aspects[0].orb, 0.001)
    }

    @Test
    fun `conjunction within orb detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(107.5),
        )
        val aspects = calculator.calculate(positions)
        assertEquals(1, aspects.size)
        assertEquals(Aspect.CONJUNCTION, aspects[0].aspect)
        assertEquals(7.5, aspects[0].orb, 0.001)
    }

    @Test
    fun `conjunction outside orb not detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(109.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.none { it.aspect == Aspect.CONJUNCTION })
    }

    @Test
    fun `opposition detected across 0-360 boundary`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(5.0),
            CelestialBody.MOON to pos(185.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.OPPOSITION })
    }

    @Test
    fun `trine detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(10.0),
            CelestialBody.MOON to pos(130.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.TRINE })
    }

    @Test
    fun `square detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(10.0),
            CelestialBody.MOON to pos(100.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.SQUARE })
    }

    @Test
    fun `sextile detected`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(10.0),
            CelestialBody.MOON to pos(70.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.SEXTILE })
    }

    @Test
    fun `applying aspect when faster body approaches`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0, speed = 1.0),
            CelestialBody.MOON to pos(95.0, speed = 13.0),
        )
        val aspects = calculator.calculate(positions)
        val conjunction = aspects.first { it.aspect == Aspect.CONJUNCTION }
        assertTrue("Should be applying", conjunction.isApplying)
    }

    @Test
    fun `separating aspect when faster body moves away`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0, speed = 1.0),
            CelestialBody.MOON to pos(105.0, speed = 13.0),
        )
        val aspects = calculator.calculate(positions)
        val conjunction = aspects.first { it.aspect == Aspect.CONJUNCTION }
        assertFalse("Should be separating", conjunction.isApplying)
    }

    @Test
    fun `orb exactly at limit is included`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(100.0),
            CelestialBody.MOON to pos(108.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.aspect == Aspect.CONJUNCTION })
    }

    @Test
    fun `multiple aspects between multiple bodies`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(0.0),
            CelestialBody.MOON to pos(90.0),
            CelestialBody.MARS to pos(120.0),
        )
        val aspects = calculator.calculate(positions)
        assertTrue(aspects.any { it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MOON && it.aspect == Aspect.SQUARE })
        assertTrue(aspects.any { it.body1 == CelestialBody.SUN && it.body2 == CelestialBody.MARS && it.aspect == Aspect.TRINE })
        assertTrue(aspects.any { it.body1 == CelestialBody.MOON && it.body2 == CelestialBody.MARS && it.aspect == Aspect.SEMI_SEXTILE })
    }

    @Test
    fun `disabled aspect not detected`() {
        val calc = AspectCalculator(
            enabledAspects = setOf(Aspect.CONJUNCTION),
            aspectOrbs = mapOf(Aspect.CONJUNCTION to 8.0),
        )
        val positions = mapOf(
            CelestialBody.SUN to pos(0.0),
            CelestialBody.MOON to pos(90.0),
        )
        val aspects = calc.calculate(positions)
        assertTrue(aspects.isEmpty())
    }

    @Test
    fun `aspects sorted by orb ascending`() {
        val positions = mapOf(
            CelestialBody.SUN to pos(0.0),
            CelestialBody.MOON to pos(93.0),
            CelestialBody.MARS to pos(125.0),
        )
        val aspects = calculator.calculate(positions)
        for (i in 0 until aspects.size - 1) {
            assertTrue(aspects[i].orb <= aspects[i + 1].orb)
        }
    }
}
