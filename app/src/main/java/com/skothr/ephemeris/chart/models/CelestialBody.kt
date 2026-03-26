package com.skothr.ephemeris.chart.models

enum class CelestialBody(
    val displayName: String,
    val symbol: String,
    val swissEphId: Int,
) {
    SUN("Sun", "\u2609", 0),
    MOON("Moon", "\u263D", 1),
    MERCURY("Mercury", "\u263F", 2),
    VENUS("Venus", "\u2640", 3),
    MARS("Mars", "\u2642", 4),
    JUPITER("Jupiter", "\u2643", 5),
    SATURN("Saturn", "\u2644", 6),
    URANUS("Uranus", "\u2645", 7),
    NEPTUNE("Neptune", "\u2646", 8),
    PLUTO("Pluto", "\u2647", 9),
    CHIRON("Chiron", "\u26B7", 15),
    NORTH_NODE("North Node", "\u260A", 11),
    LILITH("Lilith", "\u26B8", 12),
}
