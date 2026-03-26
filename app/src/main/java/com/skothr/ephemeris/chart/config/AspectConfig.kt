package com.skothr.ephemeris.chart.config

import com.skothr.ephemeris.chart.models.Aspect

data class AspectConfig(
    val enabledAspects: Map<Aspect, Double> = Aspect.entries.associateWith { it.defaultOrb },
)
