package com.skothr.ephemeris.chart.models

enum class Aspect(
    val displayName: String,
    val symbol: String,
    val angle: Double,
    val defaultOrb: Double,
    val isMajor: Boolean,
) {
    CONJUNCTION("Conjunction", "\u260C", 0.0, 8.0, true),
    OPPOSITION("Opposition", "\u260D", 180.0, 8.0, true),
    TRINE("Trine", "\u25B3", 120.0, 8.0, true),
    SQUARE("Square", "\u25A1", 90.0, 7.0, true),
    SEXTILE("Sextile", "\u2731", 60.0, 6.0, true),
    QUINCUNX("Quincunx", "\u26BB", 150.0, 3.0, false),
    SEMI_SEXTILE("Semi-Sextile", "\u26BA", 30.0, 2.0, false),
    SEMI_SQUARE("Semi-Square", "\u2220", 45.0, 2.0, false),
    SESQUIQUADRATE("Sesquiquadrate", "\u26BC", 135.0, 2.0, false),
}
