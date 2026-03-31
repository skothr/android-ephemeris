package com.skothr.ephemeris.chart.models

enum class CelestialBody(
    val displayName: String,
    val symbol: String,
    val astroSymbol: String,
    val swissEphId: Int,
) {
    SUN("Sun", "\u2609", "Q", 0),
    MOON("Moon", "\u263D", "R", 1),
    MERCURY("Mercury", "\u263F", "S", 2),
    VENUS("Venus", "\u2640", "T", 3),
    MARS("Mars", "\u2642", "U", 4),
    JUPITER("Jupiter", "\u2643", "V", 5),
    SATURN("Saturn", "\u2644", "W", 6),
    URANUS("Uranus", "\u2645", "X", 7),
    NEPTUNE("Neptune", "\u2646", "Y", 8),
    PLUTO("Pluto", "\u2647", "Z", 9),
    CHIRON("Chiron", "\u26B7", "q", 15),
    NORTH_NODE("North Node", "\u260A", "g", 11),
    LILITH("Lilith", "\u26B8", "z", 12),
}
