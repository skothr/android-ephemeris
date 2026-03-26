package com.skothr.ephemeris.chart.config

import com.skothr.ephemeris.chart.models.CelestialBody

data class BodyConfig(
    val enabledBodies: Set<CelestialBody> = CelestialBody.entries.toSet(),
)
