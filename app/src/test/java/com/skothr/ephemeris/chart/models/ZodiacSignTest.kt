package com.skothr.ephemeris.chart.models

import org.junit.Assert.assertEquals
import org.junit.Test

class ZodiacSignTest {

    @Test
    fun `fromLongitude returns correct sign for each 30-degree segment`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(0.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(15.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(29.99))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromLongitude(30.0))
        assertEquals(ZodiacSign.GEMINI, ZodiacSign.fromLongitude(60.0))
        assertEquals(ZodiacSign.CANCER, ZodiacSign.fromLongitude(90.0))
        assertEquals(ZodiacSign.LEO, ZodiacSign.fromLongitude(120.0))
        assertEquals(ZodiacSign.VIRGO, ZodiacSign.fromLongitude(150.0))
        assertEquals(ZodiacSign.LIBRA, ZodiacSign.fromLongitude(180.0))
        assertEquals(ZodiacSign.SCORPIO, ZodiacSign.fromLongitude(210.0))
        assertEquals(ZodiacSign.SAGITTARIUS, ZodiacSign.fromLongitude(240.0))
        assertEquals(ZodiacSign.CAPRICORN, ZodiacSign.fromLongitude(270.0))
        assertEquals(ZodiacSign.AQUARIUS, ZodiacSign.fromLongitude(300.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(330.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(359.99))
    }

    @Test
    fun `fromLongitude normalizes values above 360`() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(360.0))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromLongitude(390.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(720.0))
    }

    @Test
    fun `fromLongitude normalizes negative values`() {
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromLongitude(-10.0))
        assertEquals(ZodiacSign.AQUARIUS, ZodiacSign.fromLongitude(-50.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromLongitude(-360.0))
    }

    @Test
    fun `signDegree returns degree within sign`() {
        assertEquals(0.0, ZodiacSign.signDegree(0.0), 0.001)
        assertEquals(15.0, ZodiacSign.signDegree(15.0), 0.001)
        assertEquals(0.0, ZodiacSign.signDegree(30.0), 0.001)
        assertEquals(15.0, ZodiacSign.signDegree(45.0), 0.001)
        assertEquals(29.99, ZodiacSign.signDegree(359.99), 0.01)
    }

    @Test
    fun `signDegree normalizes negative values`() {
        assertEquals(20.0, ZodiacSign.signDegree(-10.0), 0.001)
    }

    @Test
    fun `signDegree normalizes values above 360`() {
        assertEquals(0.0, ZodiacSign.signDegree(360.0), 0.001)
        assertEquals(15.0, ZodiacSign.signDegree(375.0), 0.001)
    }

    @Test
    fun `each sign has correct element`() {
        assertEquals(Element.FIRE, ZodiacSign.ARIES.element)
        assertEquals(Element.EARTH, ZodiacSign.TAURUS.element)
        assertEquals(Element.AIR, ZodiacSign.GEMINI.element)
        assertEquals(Element.WATER, ZodiacSign.CANCER.element)
        assertEquals(Element.FIRE, ZodiacSign.LEO.element)
        assertEquals(Element.EARTH, ZodiacSign.VIRGO.element)
        assertEquals(Element.AIR, ZodiacSign.LIBRA.element)
        assertEquals(Element.WATER, ZodiacSign.SCORPIO.element)
        assertEquals(Element.FIRE, ZodiacSign.SAGITTARIUS.element)
        assertEquals(Element.EARTH, ZodiacSign.CAPRICORN.element)
        assertEquals(Element.AIR, ZodiacSign.AQUARIUS.element)
        assertEquals(Element.WATER, ZodiacSign.PISCES.element)
    }

    @Test
    fun `each sign has correct modality`() {
        assertEquals(Modality.CARDINAL, ZodiacSign.ARIES.modality)
        assertEquals(Modality.FIXED, ZodiacSign.TAURUS.modality)
        assertEquals(Modality.MUTABLE, ZodiacSign.GEMINI.modality)
        assertEquals(Modality.CARDINAL, ZodiacSign.CANCER.modality)
        assertEquals(Modality.FIXED, ZodiacSign.LEO.modality)
        assertEquals(Modality.MUTABLE, ZodiacSign.VIRGO.modality)
        assertEquals(Modality.CARDINAL, ZodiacSign.LIBRA.modality)
        assertEquals(Modality.FIXED, ZodiacSign.SCORPIO.modality)
        assertEquals(Modality.MUTABLE, ZodiacSign.SAGITTARIUS.modality)
        assertEquals(Modality.CARDINAL, ZodiacSign.CAPRICORN.modality)
        assertEquals(Modality.FIXED, ZodiacSign.AQUARIUS.modality)
        assertEquals(Modality.MUTABLE, ZodiacSign.PISCES.modality)
    }

    @Test
    fun `each sign has correct start degree`() {
        assertEquals(0.0, ZodiacSign.ARIES.startDegree, 0.001)
        assertEquals(30.0, ZodiacSign.TAURUS.startDegree, 0.001)
        assertEquals(60.0, ZodiacSign.GEMINI.startDegree, 0.001)
        assertEquals(90.0, ZodiacSign.CANCER.startDegree, 0.001)
        assertEquals(120.0, ZodiacSign.LEO.startDegree, 0.001)
        assertEquals(150.0, ZodiacSign.VIRGO.startDegree, 0.001)
        assertEquals(180.0, ZodiacSign.LIBRA.startDegree, 0.001)
        assertEquals(210.0, ZodiacSign.SCORPIO.startDegree, 0.001)
        assertEquals(240.0, ZodiacSign.SAGITTARIUS.startDegree, 0.001)
        assertEquals(270.0, ZodiacSign.CAPRICORN.startDegree, 0.001)
        assertEquals(300.0, ZodiacSign.AQUARIUS.startDegree, 0.001)
        assertEquals(330.0, ZodiacSign.PISCES.startDegree, 0.001)
    }

    @Test
    fun `signs have non-empty display names and symbols`() {
        for (sign in ZodiacSign.entries) {
            assert(sign.displayName.isNotEmpty()) { "${sign.name} has empty displayName" }
            assert(sign.symbol.isNotEmpty()) { "${sign.name} has empty symbol" }
        }
    }
}
