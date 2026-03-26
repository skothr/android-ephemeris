package com.skothr.ephemeris.ui.controls

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DateTimeControls(
    dateTime: LocalDateTime,
    onDateTimeChanged: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScrubField("Year", dateTime.year.toString(), Modifier.weight(1.2f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusYears(delta.toLong()) })
            }
            ScrubField("Mon", dateTime.monthValue.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusMonths(delta.toLong()) })
            }
            ScrubField("Day", dateTime.dayOfMonth.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusDays(delta.toLong()) })
            }
            ScrubField("Hr", dateTime.hour.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusHours(delta.toLong()) })
            }
            ScrubField("Min", dateTime.minute.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusMinutes(delta.toLong()) })
            }
            ScrubField("Sec", dateTime.second.toString().padStart(2, '0'), Modifier.weight(1f)) { delta ->
                onDateTimeChanged(safeAdjust { dateTime.plusSeconds(delta.toLong()) })
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = { onDateTimeChanged(LocalDateTime.now()) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Now")
        }
    }
}

@Composable
private fun ScrubField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDelta: (Int) -> Unit,
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 20f

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        val speed = 1 + (abs(accumulatedDrag) / 200f).toInt()
                        val units = (accumulatedDrag / dragThreshold).roundToInt()
                        if (units != 0) {
                            onDelta(-units * speed)
                            accumulatedDrag -= units * dragThreshold
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private inline fun safeAdjust(block: () -> LocalDateTime): LocalDateTime {
    return try {
        block()
    } catch (e: Exception) {
        LocalDateTime.now()
    }
}
