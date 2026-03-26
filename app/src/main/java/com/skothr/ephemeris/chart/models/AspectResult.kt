package com.skothr.ephemeris.chart.models

data class AspectResult(
    val body1: CelestialBody,
    val body2: CelestialBody,
    val aspect: Aspect,
    val exactAngle: Double,
    val orb: Double,
    val isApplying: Boolean,
)
