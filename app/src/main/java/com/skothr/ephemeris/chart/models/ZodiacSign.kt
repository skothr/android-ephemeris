package com.skothr.ephemeris.chart.models

enum class Element { FIRE, EARTH, AIR, WATER }
enum class Modality { CARDINAL, FIXED, MUTABLE }

enum class ZodiacSign(
    val displayName: String,
    val symbol: String,
    val startDegree: Double,
    val element: Element,
    val modality: Modality,
) {
    ARIES("Aries", "\u2648", 0.0, Element.FIRE, Modality.CARDINAL),
    TAURUS("Taurus", "\u2649", 30.0, Element.EARTH, Modality.FIXED),
    GEMINI("Gemini", "\u264A", 60.0, Element.AIR, Modality.MUTABLE),
    CANCER("Cancer", "\u264B", 90.0, Element.WATER, Modality.CARDINAL),
    LEO("Leo", "\u264C", 120.0, Element.FIRE, Modality.FIXED),
    VIRGO("Virgo", "\u264D", 150.0, Element.EARTH, Modality.MUTABLE),
    LIBRA("Libra", "\u264E", 180.0, Element.AIR, Modality.CARDINAL),
    SCORPIO("Scorpio", "\u264F", 210.0, Element.WATER, Modality.FIXED),
    SAGITTARIUS("Sagittarius", "\u2650", 240.0, Element.FIRE, Modality.MUTABLE),
    CAPRICORN("Capricorn", "\u2651", 270.0, Element.EARTH, Modality.CARDINAL),
    AQUARIUS("Aquarius", "\u2652", 300.0, Element.AIR, Modality.FIXED),
    PISCES("Pisces", "\u2653", 330.0, Element.WATER, Modality.MUTABLE);

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
