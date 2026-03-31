package com.skothr.ephemeris.chart

import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.AspectResult
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.ephemeris.models.CelestialPosition
import kotlin.math.abs
import kotlin.math.min

class AspectCalculator(
    private val enabledAspects: Set<Aspect>,
    private val aspectOrbs: Map<Aspect, Double>,
) {

    fun calculate(positions: Map<CelestialBody, CelestialPosition>): List<AspectResult> {
        val bodies = positions.keys.toList()
        val results = mutableListOf<AspectResult>()

        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val body1 = bodies[i]
                val body2 = bodies[j]
                val pos1 = positions[body1]!!
                val pos2 = positions[body2]!!

                val angularDiff = angularDistance(pos1.longitude, pos2.longitude)

                for (aspect in enabledAspects) {
                    val orb = aspectOrbs[aspect] ?: aspect.defaultOrb
                    val aspectOrb = abs(angularDiff - aspect.angle)
                    if (aspectOrb <= orb) {
                        val applying = isApplying(pos1, pos2, aspect)
                        results.add(
                            AspectResult(
                                body1 = body1, body2 = body2, aspect = aspect,
                                exactAngle = angularDiff, orb = aspectOrb, isApplying = applying,
                            )
                        )
                        break
                    }
                }
            }
        }
        return results.sortedBy { it.orb }
    }

    private fun angularDistance(lon1: Double, lon2: Double): Double {
        val diff = abs(lon1 - lon2) % 360.0
        return min(diff, 360.0 - diff)
    }

    private fun isApplying(pos1: CelestialPosition, pos2: CelestialPosition, aspect: Aspect): Boolean {
        val currentDist = angularDistance(pos1.longitude, pos2.longitude)
        val futureDist = angularDistance(
            pos1.longitude + pos1.speed * 0.01,
            pos2.longitude + pos2.speed * 0.01,
        )
        val futureOrb = abs(futureDist - aspect.angle)
        val currentOrb = abs(currentDist - aspect.angle)
        return futureOrb < currentOrb
    }
}
