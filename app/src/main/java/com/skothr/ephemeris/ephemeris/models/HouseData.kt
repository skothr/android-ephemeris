package com.skothr.ephemeris.ephemeris.models

data class HouseData(
    val cusps: List<Double>,
    val ascendant: Double,
    val midheaven: Double,
    val descendant: Double,
    val imumCoeli: Double,
) {
    init {
        require(cusps.size == 12) { "House cusps must contain exactly 12 values, got ${cusps.size}" }
    }
}
