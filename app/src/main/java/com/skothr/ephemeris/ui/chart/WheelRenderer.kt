package com.skothr.ephemeris.ui.chart

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import com.skothr.ephemeris.chart.models.AspectResult
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.ZodiacSign
import com.skothr.ephemeris.ephemeris.models.HouseData
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

object WheelDrawing {

    // Cached Paint objects — reused every frame to avoid GC pressure during scrubbing
    private val signPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.WHITE
        isAntiAlias = true
    }
    private val houseNumPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.GRAY
        isAntiAlias = true
    }
    private val bodyPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.WHITE
        isAntiAlias = true
    }
    private val angleLabelPaint = Paint().apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
        color = android.graphics.Color.parseColor("#FFD54F")
        isAntiAlias = true
        isFakeBoldText = true
    }

    fun DrawScope.drawZodiacRing(radii: RingRadii, cx: Float, cy: Float, ascendant: Double) {
        // Clip to annular ring so arcs don't bleed into center
        val clipPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(
                cx - radii.zodiacOuter, cy - radii.zodiacOuter,
                cx + radii.zodiacOuter, cy + radii.zodiacOuter,
            ))
            // Subtract inner circle using EvenOdd fill
            addOval(androidx.compose.ui.geometry.Rect(
                cx - radii.zodiacInner, cy - radii.zodiacInner,
                cx + radii.zodiacInner, cy + radii.zodiacInner,
            ))
        }

        clipPath(clipPath, androidx.compose.ui.graphics.ClipOp.Intersect) {
            val signs = ZodiacSign.entries
            for (sign in signs) {
                val startAngle = WheelMath.longitudeToAngle(sign.startDegree, ascendant)
                val color = ChartColors.elementColor[sign.element] ?: Color.Gray

                drawArc(
                    color = color.copy(alpha = 0.3f),
                    startAngle = Math.toDegrees(startAngle).toFloat(),
                    sweepAngle = -30f,
                    useCenter = true,
                    topLeft = Offset(cx - radii.zodiacOuter, cy - radii.zodiacOuter),
                    size = Size(radii.zodiacOuter * 2, radii.zodiacOuter * 2),
                )
            }
        }

        // Sign glyphs
        signPaint.textSize = (radii.zodiacOuter - radii.zodiacInner) * 0.5f
        val signs = ZodiacSign.entries
        for (sign in signs) {
            val midLon = sign.startDegree + 15.0
            val midAngle = WheelMath.longitudeToAngle(midLon, ascendant)
            val midRadius = (radii.zodiacOuter + radii.zodiacInner) / 2f
            val (gx, gy) = WheelMath.pointOnCircle(cx, cy, midRadius, midAngle)
            drawContext.canvas.nativeCanvas.drawText(sign.symbol, gx, gy + 8f, signPaint)
        }

        drawCircle(ChartColors.ringStroke, radii.zodiacOuter, Offset(cx, cy), style = Stroke(1.5f))
        drawCircle(ChartColors.ringStroke, radii.zodiacInner, Offset(cx, cy), style = Stroke(1.5f))
    }

    fun DrawScope.drawHouseRing(
        radii: RingRadii, cx: Float, cy: Float,
        houseData: HouseData, ascendant: Double,
    ) {
        houseNumPaint.textSize = (radii.houseOuter - radii.houseInner) * 0.3f
        for (i in houseData.cusps.indices) {
            val angle = WheelMath.longitudeToAngle(houseData.cusps[i], ascendant)
            val (ox, oy) = WheelMath.pointOnCircle(cx, cy, radii.zodiacInner, angle)
            val (ix, iy) = WheelMath.pointOnCircle(cx, cy, radii.houseInner, angle)
            drawLine(ChartColors.ringStroke, Offset(ox, oy), Offset(ix, iy), strokeWidth = 1f)

            val nextCusp = houseData.cusps[(i + 1) % 12]
            var midLon = (houseData.cusps[i] + nextCusp) / 2.0
            if (nextCusp < houseData.cusps[i]) midLon = ((houseData.cusps[i] + nextCusp + 360.0) / 2.0) % 360.0
            val midAngle = WheelMath.longitudeToAngle(midLon, ascendant)
            val numRadius = (radii.houseOuter + radii.houseInner) / 2f
            val (nx, ny) = WheelMath.pointOnCircle(cx, cy, numRadius, midAngle)

            drawContext.canvas.nativeCanvas.drawText("${i + 1}", nx, ny + 6f, houseNumPaint)
        }
        drawCircle(ChartColors.ringStroke, radii.houseInner, Offset(cx, cy), style = Stroke(1.5f))
    }

    fun DrawScope.drawBodies(
        radii: RingRadii, cx: Float, cy: Float,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double,
    ) {
        bodyPaint.textSize = (radii.houseInner - radii.bodyRing) * 0.6f
        val bodiesByAngle = positions.entries
            .map { (body, pos) ->
                Triple(body, pos, WheelMath.longitudeToAngle(pos.longitude, ascendant))
            }
            .sortedBy { it.third }

        val angles = bodiesByAngle.map { it.third }
        val minSep = Math.toRadians(8.0)
        val resolvedAngles = WheelMath.resolveCollisions(angles, minSep)

        for (i in bodiesByAngle.indices) {
            val (body, _, originalAngle) = bodiesByAngle[i]
            val displayAngle = resolvedAngles[i]

            val (bx, by) = WheelMath.pointOnCircle(cx, cy, radii.bodyRing, displayAngle)
            drawContext.canvas.nativeCanvas.drawText(body.symbol, bx, by + 8f, bodyPaint)

            val (tx, ty) = WheelMath.pointOnCircle(cx, cy, radii.houseInner, originalAngle)
            val (tx2, ty2) = WheelMath.pointOnCircle(cx, cy, radii.houseInner - 8f, originalAngle)
            drawLine(ChartColors.bodyGlyph.copy(alpha = 0.5f), Offset(tx, ty), Offset(tx2, ty2), strokeWidth = 1f)
        }
    }

    fun DrawScope.drawAspects(
        radii: RingRadii, cx: Float, cy: Float,
        aspects: List<AspectResult>,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double,
    ) {
        for (aspect in aspects) {
            val pos1 = positions[aspect.body1] ?: continue
            val pos2 = positions[aspect.body2] ?: continue
            val angle1 = WheelMath.longitudeToAngle(pos1.longitude, ascendant)
            val angle2 = WheelMath.longitudeToAngle(pos2.longitude, ascendant)

            val innerRadius = radii.aspectInner + (radii.bodyRing - radii.aspectInner) * 0.3f
            val (x1, y1) = WheelMath.pointOnCircle(cx, cy, innerRadius, angle1)
            val (x2, y2) = WheelMath.pointOnCircle(cx, cy, innerRadius, angle2)

            val color = ChartColors.aspectColor[aspect.aspect] ?: Color.Gray
            val alpha = 1f - (aspect.orb.toFloat() / aspect.aspect.defaultOrb.toFloat()) * 0.7f
            val pathEffect = if (aspect.aspect.isMajor) null
                else PathEffect.dashPathEffect(floatArrayOf(8f, 4f))

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.2f, 1f)),
                start = Offset(x1, y1), end = Offset(x2, y2),
                strokeWidth = if (aspect.aspect.isMajor) 1.5f else 1f,
                pathEffect = pathEffect,
            )
        }
    }

    fun DrawScope.drawAngles(
        radii: RingRadii, cx: Float, cy: Float,
        houseData: HouseData, ascendant: Double,
    ) {
        val angles = listOf(
            houseData.ascendant to "ASC",
            houseData.midheaven to "MC",
            houseData.descendant to "DSC",
            houseData.imumCoeli to "IC",
        )
        for ((longitude, label) in angles) {
            val angle = WheelMath.longitudeToAngle(longitude, ascendant)
            val (ox, oy) = WheelMath.pointOnCircle(cx, cy, radii.zodiacOuter, angle)
            val (ix, iy) = WheelMath.pointOnCircle(cx, cy, radii.aspectInner, angle)
            drawLine(ChartColors.angleMarker, Offset(ox, oy), Offset(ix, iy), strokeWidth = 2f)

            val (lx, ly) = WheelMath.pointOnCircle(cx, cy, radii.zodiacOuter + 14f, angle)
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly + 5f, angleLabelPaint)
        }
    }
}
