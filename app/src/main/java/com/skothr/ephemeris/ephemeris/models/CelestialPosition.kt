package com.skothr.ephemeris.ephemeris.models

data class CelestialPosition(
    val longitude: Double,
    val latitude: Double,
    val distance: Double,
    val speed: Double,
) {
    val isRetrograde: Boolean get() = speed < 0.0
}
