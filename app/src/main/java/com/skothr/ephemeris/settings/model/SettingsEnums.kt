package com.skothr.ephemeris.settings.model

enum class ZodiacType(val displayName: String) {
    TROPICAL("Tropical"),
    SIDEREAL("Sidereal"),
}

enum class Ayanamsa(val displayName: String, val swissEphId: Int) {
    LAHIRI("Lahiri", 1),
    RAMAN("Raman", 3),
    KRISHNAMURTI("Krishnamurti", 5),
    FAGAN_BRADLEY("Fagan-Bradley", 0),
}

enum class NodeType(val displayName: String) {
    TRUE_NODE("True Node"),
    MEAN_NODE("Mean Node"),
}

enum class Center(val displayName: String) {
    GEOCENTRIC("Geocentric"),
    HELIOCENTRIC("Heliocentric"),
    TOPOCENTRIC("Topocentric"),
}

enum class AppTheme(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
}

enum class SymbolStyle(val displayName: String) {
    SYSTEM("System"),
    ASTRO("Astro"),
}

enum class LineStyle(val displayName: String) {
    SOLID("Solid"),
    DASHED("Dashed"),
}
