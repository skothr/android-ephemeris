package com.skothr.ephemeris.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.settings.model.VisualSettings

@Composable
fun ChartWheel(chartData: ChartData, visualSettings: VisualSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    remember { WheelDrawing.loadFont(context); true }

    Canvas(
        modifier = modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val availableRadius = minOf(cx, cy)
        val radii = WheelMath.calculateRingRadii(availableRadius, visualSettings)
        val asc = if (visualSettings.lockAscendant) chartData.houseData.ascendant else 180.0

        drawCircle(ChartColors.wheelBackground(visualSettings.theme), availableRadius)

        with(WheelDrawing) {
            drawZodiacRing(radii, cx, cy, asc, visualSettings)
            drawHouseRing(radii, cx, cy, chartData.houseData, asc, visualSettings)
            drawBodies(radii, cx, cy, chartData.positions, asc, visualSettings)
            drawAspects(radii, cx, cy, chartData.aspects, chartData.positions, asc, visualSettings)
            drawAngles(radii, cx, cy, chartData.houseData, asc, visualSettings)
        }
    }
}
