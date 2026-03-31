package com.skothr.ephemeris.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.settings.model.VisualSettings

@Composable
fun ChartWheel(chartData: ChartData, visualSettings: VisualSettings, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val availableRadius = minOf(cx, cy)
        val radii = WheelMath.calculateRingRadii(availableRadius, visualSettings)
        val asc = chartData.houseData.ascendant

        drawCircle(ChartColors.wheelBackground, availableRadius)

        with(WheelDrawing) {
            drawZodiacRing(radii, cx, cy, asc)
            drawHouseRing(radii, cx, cy, chartData.houseData, asc)
            drawBodies(radii, cx, cy, chartData.positions, asc)
            drawAspects(radii, cx, cy, chartData.aspects, chartData.positions, asc, visualSettings)
            drawAngles(radii, cx, cy, chartData.houseData, asc)
        }
    }
}
