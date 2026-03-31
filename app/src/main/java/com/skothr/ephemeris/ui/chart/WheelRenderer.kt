package com.skothr.ephemeris.ui.chart

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.skothr.ephemeris.chart.models.AspectResult
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.chart.models.ZodiacSign
import com.skothr.ephemeris.ephemeris.models.HouseData
import com.skothr.ephemeris.settings.model.AppTheme
import com.skothr.ephemeris.settings.model.SymbolStyle
import com.skothr.ephemeris.settings.model.VisualSettings
import com.skothr.ephemeris.settings.model.LineStyle
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
        val adjusted = ascendant - longitude + 180.0
        val radians = Math.toRadians(adjusted)
        return ((radians % (2 * PI)) + 2 * PI) % (2 * PI)
    }

    fun pointOnCircle(cx: Float, cy: Float, radius: Float, angleRadians: Double): Pair<Float, Float> {
        val x = cx + radius * cos(angleRadians).toFloat()
        val y = cy + radius * sin(angleRadians).toFloat()
        return Pair(x, y)
    }

    fun calculateRingRadii(availableRadius: Float, visual: VisualSettings): RingRadii {
        return RingRadii(
            zodiacOuter = availableRadius * visual.zodiacOuterRadius,
            zodiacInner = availableRadius * visual.zodiacInnerRadius,
            houseOuter = availableRadius * visual.houseOuterRadius,
            houseInner = availableRadius * visual.houseInnerRadius,
            bodyRing = availableRadius * visual.bodyRingRadius,
            aspectInner = availableRadius * visual.aspectInnerRadius,
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

    private var astroTypeface: Typeface? = null

    fun loadFont(context: Context) {
        if (astroTypeface == null) {
            astroTypeface = context.resources.getFont(com.skothr.ephemeris.R.font.astronomicon)
        }
    }

    // Cached Paint objects — reused every frame to avoid GC pressure during scrubbing
    private val signPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val houseNumPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val bodyPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val angleLabelPaint = Paint().apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    private fun applyStyle(visual: VisualSettings) {
        val theme = visual.theme
        val typeface = if (visual.symbolStyle == SymbolStyle.ASTRO) astroTypeface else null
        signPaint.color = ChartColors.signGlyph(theme).toArgb()
        signPaint.typeface = typeface
        houseNumPaint.color = ChartColors.houseNumber(theme).toArgb()
        bodyPaint.color = ChartColors.bodyGlyph(theme).toArgb()
        bodyPaint.typeface = typeface
        angleLabelPaint.color = ChartColors.angleMarker(theme).toArgb()
    }

    fun DrawScope.drawZodiacRing(radii: RingRadii, cx: Float, cy: Float, ascendant: Double, visual: VisualSettings) {
        val theme = visual.theme
        applyStyle(visual)

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
            for ((index, sign) in signs.withIndex()) {
                val startAngle = WheelMath.longitudeToAngle(sign.startDegree, ascendant)

                val arcColor = if (visual.coloredZodiacBands) {
                    val color = ChartColors.elementColor[sign.element] ?: Color.Gray
                    color.copy(alpha = ChartColors.zodiacArcAlpha(theme))
                } else {
                    // Alternating greyscale bands
                    val bandColor = ChartColors.ringStroke(theme)
                    bandColor.copy(alpha = if (index % 2 == 0) 0.15f else 0.08f)
                }

                drawArc(
                    color = arcColor,
                    startAngle = Math.toDegrees(startAngle).toFloat(),
                    sweepAngle = -30f,
                    useCenter = true,
                    topLeft = Offset(cx - radii.zodiacOuter, cy - radii.zodiacOuter),
                    size = Size(radii.zodiacOuter * 2, radii.zodiacOuter * 2),
                )
            }
        }

        // Sign glyphs
        val useAstro = visual.symbolStyle == SymbolStyle.ASTRO && astroTypeface != null
        signPaint.textSize = (radii.zodiacOuter - radii.zodiacInner) * if (useAstro) 0.6f else 0.5f
        val signs = ZodiacSign.entries
        for (sign in signs) {
            val midLon = sign.startDegree + 15.0
            val midAngle = WheelMath.longitudeToAngle(midLon, ascendant)
            val midRadius = (radii.zodiacOuter + radii.zodiacInner) / 2f
            val (gx, gy) = WheelMath.pointOnCircle(cx, cy, midRadius, midAngle)
            val glyph = if (useAstro) sign.astroSymbol else sign.symbol
            drawContext.canvas.nativeCanvas.drawText(glyph, gx, gy + 8f, signPaint)
        }

        val stroke = ChartColors.ringStroke(theme)
        drawCircle(stroke, radii.zodiacOuter, Offset(cx, cy), style = Stroke(1.5f))
        drawCircle(stroke, radii.zodiacInner, Offset(cx, cy), style = Stroke(1.5f))
    }

    fun DrawScope.drawHouseRing(
        radii: RingRadii, cx: Float, cy: Float,
        houseData: HouseData, ascendant: Double, visual: VisualSettings,
    ) {
        val stroke = ChartColors.ringStroke(visual.theme)
        houseNumPaint.textSize = (radii.houseOuter - radii.houseInner) * 0.3f
        for (i in houseData.cusps.indices) {
            val angle = WheelMath.longitudeToAngle(houseData.cusps[i], ascendant)
            val (ox, oy) = WheelMath.pointOnCircle(cx, cy, radii.zodiacInner, angle)
            val (ix, iy) = WheelMath.pointOnCircle(cx, cy, radii.houseInner, angle)
            drawLine(stroke, Offset(ox, oy), Offset(ix, iy), strokeWidth = 1f)

            val nextCusp = houseData.cusps[(i + 1) % 12]
            var midLon = (houseData.cusps[i] + nextCusp) / 2.0
            if (nextCusp < houseData.cusps[i]) midLon = ((houseData.cusps[i] + nextCusp + 360.0) / 2.0) % 360.0
            val midAngle = WheelMath.longitudeToAngle(midLon, ascendant)
            val numRadius = (radii.houseOuter + radii.houseInner) / 2f
            val (nx, ny) = WheelMath.pointOnCircle(cx, cy, numRadius, midAngle)

            drawContext.canvas.nativeCanvas.drawText("${i + 1}", nx, ny + 6f, houseNumPaint)
        }
        drawCircle(stroke, radii.houseInner, Offset(cx, cy), style = Stroke(1.5f))
    }

    fun DrawScope.drawBodies(
        radii: RingRadii, cx: Float, cy: Float,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double, visual: VisualSettings,
    ) {
        val useAstro = visual.symbolStyle == SymbolStyle.ASTRO && astroTypeface != null
        val glyphColor = ChartColors.bodyGlyph(visual.theme)
        bodyPaint.textSize = (radii.houseInner - radii.bodyRing) * if (useAstro) 0.7f else 0.6f
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
            val glyph = if (useAstro) body.astroSymbol else body.symbol
            drawContext.canvas.nativeCanvas.drawText(glyph, bx, by + 8f, bodyPaint)

            val (tx, ty) = WheelMath.pointOnCircle(cx, cy, radii.houseInner, originalAngle)
            val (tx2, ty2) = WheelMath.pointOnCircle(cx, cy, radii.houseInner - 8f, originalAngle)
            drawLine(glyphColor.copy(alpha = 0.5f), Offset(tx, ty), Offset(tx2, ty2), strokeWidth = 1f)
        }
    }

    fun DrawScope.drawAspects(
        radii: RingRadii, cx: Float, cy: Float,
        aspects: List<AspectResult>,
        positions: Map<CelestialBody, com.skothr.ephemeris.ephemeris.models.CelestialPosition>,
        ascendant: Double,
        visual: VisualSettings,
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
            val orbFraction = (aspect.orb.toFloat() / (aspect.aspect.defaultOrb.toFloat())).coerceIn(0f, 1f)
            val alpha = visual.aspectLineOpacity * (1f - orbFraction * 0.7f)

            val lineStyle = if (aspect.aspect.isMajor) visual.majorAspectStyle else visual.minorAspectStyle
            val pathEffect = if (lineStyle == LineStyle.DASHED)
                PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null

            val baseWidth = if (aspect.aspect.isMajor) visual.aspectLineThickness else visual.aspectLineThickness * 0.7f
            val strokeWidth = if (visual.scaleWidthByOrb) {
                val widthOrbFraction = (aspect.orb.toFloat() / visual.widthScaleOrb).coerceIn(0f, 1f)
                baseWidth * (1f - widthOrbFraction * 0.7f)
            } else {
                baseWidth
            }

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.1f, 1f)),
                start = Offset(x1, y1), end = Offset(x2, y2),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect,
            )
        }
    }

    fun DrawScope.drawAngles(
        radii: RingRadii, cx: Float, cy: Float,
        houseData: HouseData, ascendant: Double, visual: VisualSettings,
    ) {
        val markerColor = ChartColors.angleMarker(visual.theme)
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
            drawLine(markerColor, Offset(ox, oy), Offset(ix, iy), strokeWidth = 2f)

            val (lx, ly) = WheelMath.pointOnCircle(cx, cy, radii.zodiacOuter + 14f, angle)
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly + 5f, angleLabelPaint)
        }
    }
}
