package com.skothr.ephemeris.ui.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
            ScrubField("Year", dateTime.year.toString(), Modifier.weight(1.2f),
                onDelta = { delta -> onDateTimeChanged(safeAdjust { dateTime.plusYears(delta.toLong()) }) },
                onValueTyped = { typed ->
                    typed.toIntOrNull()?.let { y ->
                        onDateTimeChanged(safeAdjust { dateTime.withYear(y.coerceIn(1800, 2400)) })
                    }
                },
            )
            ScrubField("Mon", dateTime.monthValue.toString().padStart(2, '0'), Modifier.weight(1f),
                onDelta = { delta -> onDateTimeChanged(safeAdjust { dateTime.plusMonths(delta.toLong()) }) },
                onValueTyped = { typed ->
                    typed.toIntOrNull()?.let { m ->
                        onDateTimeChanged(safeAdjust { dateTime.withMonth(m.coerceIn(1, 12)) })
                    }
                },
            )
            ScrubField("Day", dateTime.dayOfMonth.toString().padStart(2, '0'), Modifier.weight(1f),
                onDelta = { delta -> onDateTimeChanged(safeAdjust { dateTime.plusDays(delta.toLong()) }) },
                onValueTyped = { typed ->
                    typed.toIntOrNull()?.let { d ->
                        onDateTimeChanged(safeAdjust { dateTime.withDayOfMonth(d.coerceIn(1, dateTime.toLocalDate().lengthOfMonth())) })
                    }
                },
            )
            ScrubField("Hr", dateTime.hour.toString().padStart(2, '0'), Modifier.weight(1f),
                onDelta = { delta -> onDateTimeChanged(safeAdjust { dateTime.plusHours(delta.toLong()) }) },
                onValueTyped = { typed ->
                    typed.toIntOrNull()?.let { h ->
                        onDateTimeChanged(safeAdjust { dateTime.withHour(h.coerceIn(0, 23)) })
                    }
                },
            )
            ScrubField("Min", dateTime.minute.toString().padStart(2, '0'), Modifier.weight(1f),
                onDelta = { delta -> onDateTimeChanged(safeAdjust { dateTime.plusMinutes(delta.toLong()) }) },
                onValueTyped = { typed ->
                    typed.toIntOrNull()?.let { m ->
                        onDateTimeChanged(safeAdjust { dateTime.withMinute(m.coerceIn(0, 59)) })
                    }
                },
            )
            ScrubField("Sec", dateTime.second.toString().padStart(2, '0'), Modifier.weight(1f),
                onDelta = { delta -> onDateTimeChanged(safeAdjust { dateTime.plusSeconds(delta.toLong()) }) },
                onValueTyped = { typed ->
                    typed.toIntOrNull()?.let { s ->
                        onDateTimeChanged(safeAdjust { dateTime.withSecond(s.coerceIn(0, 59)) })
                    }
                },
            )
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
    onValueTyped: (String) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 48f  // pixels per unit — higher = smoother, less twitchy
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Consume vertical scroll so parent verticalScroll doesn't steal the gesture
    val consumeVerticalScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset(0f, available.y)
            }
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .nestedScroll(consumeVerticalScroll)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        if (!isEditing) {
                            accumulatedDrag += dragAmount
                            val units = (accumulatedDrag / dragThreshold).toInt()
                            if (units != 0) {
                                onDelta(-units)
                                accumulatedDrag -= units * dragThreshold
                            }
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

        if (isEditing) {
            BasicTextField(
                value = editText,
                onValueChange = { editText = it.filter { c -> c.isDigit() } },
                modifier = Modifier
                    .width(48.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (!it.isFocused) { onValueTyped(editText); isEditing = false } },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onValueTyped(editText); isEditing = false; focusManager.clearFocus() },
                ),
                singleLine = true,
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else {
            Text(
                text = value,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable {
                    editText = value
                    isEditing = true
                },
            )
        }
    }
}

private inline fun safeAdjust(block: () -> LocalDateTime): LocalDateTime {
    return try {
        block()
    } catch (e: Exception) {
        LocalDateTime.now()
    }
}
