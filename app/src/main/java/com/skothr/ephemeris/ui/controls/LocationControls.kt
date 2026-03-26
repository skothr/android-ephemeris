package com.skothr.ephemeris.ui.controls

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skothr.ephemeris.chart.models.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

data class CityData(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val population: Long,
)

class CityDatabase(context: Context) {
    private val cities: List<CityData> by lazy {
        val result = mutableListOf<CityData>()
        try {
            val resId = context.resources.getIdentifier("cities15000", "raw", context.packageName)
            if (resId != 0) {
                context.resources.openRawResource(resId).use { stream ->
                    BufferedReader(InputStreamReader(stream)).useLines { lines ->
                        lines.forEach { line ->
                            val cols = line.split("\t")
                            if (cols.size >= 18) {
                                try {
                                    result.add(CityData(
                                        name = cols[1],
                                        country = cols[8],
                                        latitude = cols[4].toDouble(),
                                        longitude = cols[5].toDouble(),
                                        timezone = cols[17],
                                        population = cols[14].toLongOrNull() ?: 0L,
                                    ))
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        result.sortedByDescending { it.population }
    }

    fun search(query: String, limit: Int = 20): List<CityData> {
        if (query.length < 2) return emptyList()
        val lower = query.lowercase()
        return cities
            .filter { it.name.lowercase().startsWith(lower) }
            .take(limit)
    }
}

@Composable
fun LocationControls(
    location: Location,
    timezone: String,
    onLocationChanged: (Location, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cityDb = remember { CityDatabase(context) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CityData>>(emptyList()) }
    var showSearch by remember { mutableStateOf(true) }

    // Debounce city search at 300ms
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            kotlinx.coroutines.delay(300)
            searchResults = cityDb.search(searchQuery)
        } else {
            searchResults = emptyList()
        }
    }

    Column(modifier = modifier.padding(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                showSearch = true
            },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (showSearch && searchResults.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(searchResults) { city ->
                    Text(
                        text = "${city.name}, ${city.country}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLocationChanged(
                                    Location(city.latitude, city.longitude),
                                    city.timezone,
                                )
                                searchQuery = "${city.name}, ${city.country}"
                                showSearch = false
                            }
                            .padding(8.dp),
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CoordField("Lat", "%.4f".format(location.latitude), Modifier.weight(1f)) { delta ->
                val newLat = (location.latitude + delta * 0.1).coerceIn(-90.0, 90.0)
                onLocationChanged(Location(newLat, location.longitude), timezone)
            }
            CoordField("Lon", "%.4f".format(location.longitude), Modifier.weight(1f)) { delta ->
                val newLon = (location.longitude + delta * 0.1).let { l ->
                    ((l + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                }
                onLocationChanged(Location(location.latitude, newLon), timezone)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("TZ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timezone, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CoordField(
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
                        val units = (accumulatedDrag / dragThreshold).roundToInt()
                        if (units != 0) {
                            onDelta(-units)
                            accumulatedDrag -= units * dragThreshold
                        }
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}
