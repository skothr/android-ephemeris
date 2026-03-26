package com.skothr.ephemeris.ui.chart

import kotlin.math.*

data class RingRadii(
    val zodiacOuter: Float,
    val zodiacInner: Float,
    val houseOuter: Float,
    val houseInner: Float,
    val bodyRing: Float,
    val aspectInner: Float,
)

object WheelMath {
    fun longitudeToAngle(longitude: Double, ascendant: Double): Double {
        val adjusted = ascendant - longitude
        val radians = Math.toRadians(adjusted)
        return ((radians % (2 * PI)) + 2 * PI) % (2 * PI)
    }

    fun pointOnCircle(cx: Float, cy: Float, radius: Float, angleRadians: Double): Pair<Float, Float> {
        val x = cx + radius * cos(angleRadians).toFloat()
        val y = cy + radius * sin(angleRadians).toFloat()
        return Pair(x, y)
    }

    fun calculateRingRadii(availableRadius: Float): RingRadii {
        return RingRadii(
            zodiacOuter = availableRadius * 0.95f,
            zodiacInner = availableRadius * 0.82f,
            houseOuter = availableRadius * 0.82f,
            houseInner = availableRadius * 0.68f,
            bodyRing = availableRadius * 0.58f,
            aspectInner = availableRadius * 0.10f,
        )
    }

    fun resolveCollisions(sortedAngles: List<Double>, minSeparation: Double): List<Double> {
        if (sortedAngles.size <= 1) return sortedAngles
        val result = sortedAngles.toMutableList()
        repeat(10) {
            for (i in 0 until result.size - 1) {
                val diff = result[i + 1] - result[i]
                if (diff < minSeparation) {
                    val shift = (minSeparation - diff) / 2.0
                    result[i] -= shift
                    result[i + 1] += shift
                }
            }
        }
        return result
    }
}
