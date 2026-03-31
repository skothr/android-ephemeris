package com.skothr.ephemeris.chart.models

enum class Element { FIRE, EARTH, AIR, WATER }
enum class Modality { CARDINAL, FIXED, MUTABLE }

enum class ZodiacSign(
    val displayName: String,
    val symbol: String,
    val astroSymbol: String,
    val startDegree: Double,
    val element: Element,
    val modality: Modality,
) {
    ARIES("Aries", "\u2648", "A", 0.0, Element.FIRE, Modality.CARDINAL),
    TAURUS("Taurus", "\u2649", "B", 30.0, Element.EARTH, Modality.FIXED),
    GEMINI("Gemini", "\u264A", "C", 60.0, Element.AIR, Modality.MUTABLE),
    CANCER("Cancer", "\u264B", "D", 90.0, Element.WATER, Modality.CARDINAL),
    LEO("Leo", "\u264C", "E", 120.0, Element.FIRE, Modality.FIXED),
    VIRGO("Virgo", "\u264D", "F", 150.0, Element.EARTH, Modality.MUTABLE),
    LIBRA("Libra", "\u264E", "G", 180.0, Element.AIR, Modality.CARDINAL),
    SCORPIO("Scorpio", "\u264F", "H", 210.0, Element.WATER, Modality.FIXED),
    SAGITTARIUS("Sagittarius", "\u2650", "I", 240.0, Element.FIRE, Modality.MUTABLE),
    CAPRICORN("Capricorn", "\u2651", "J", 270.0, Element.EARTH, Modality.CARDINAL),
    AQUARIUS("Aquarius", "\u2652", "K", 300.0, Element.AIR, Modality.FIXED),
    PISCES("Pisces", "\u2653", "L", 330.0, Element.WATER, Modality.MUTABLE);

    companion object {
        fun fromLongitude(longitude: Double): ZodiacSign {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            val index = (normalized / 30.0).toInt().coerceIn(0, 11)
            return entries[index]
        }

        fun signDegree(longitude: Double): Double {
            val normalized = ((longitude % 360.0) + 360.0) % 360.0
            return normalized % 30.0
        }
    }
}
