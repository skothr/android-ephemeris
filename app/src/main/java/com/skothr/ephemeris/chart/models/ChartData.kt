package com.skothr.ephemeris.chart.models

import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import com.skothr.ephemeris.ephemeris.models.HouseData
import java.time.LocalDateTime

data class Location(
    val latitude: Double,
    val longitude: Double,
)

data class ChartData(
    val dateTime: LocalDateTime,
    val location: Location,
    val positions: Map<CelestialBody, CelestialPosition>,
    val houseData: HouseData,
    val aspects: List<AspectResult>,
    val houseSystem: HouseSystem,
)
