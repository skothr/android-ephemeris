package com.skothr.ephemeris.chart.models

enum class HouseSystem(val displayName: String, val swissEphCode: Char) {
    PLACIDUS("Placidus", 'P'),
    WHOLE_SIGN("Whole Sign", 'W'),
    EQUAL("Equal", 'E'),
    KOCH("Koch", 'K');
}
