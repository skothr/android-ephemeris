package com.skothr.ephemeris.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.ui.chart.ChartWheel
import com.skothr.ephemeris.ui.controls.DateTimeControls
import com.skothr.ephemeris.ui.controls.LocationControls
import com.skothr.ephemeris.ui.theme.EphemerisTheme

@Composable
fun MainScreen(viewModel: ChartViewModel) {
    val chartData by viewModel.chartData.collectAsState()
    val dateTime by viewModel.dateTime.collectAsState()
    val location by viewModel.location.collectAsState()
    val timezone by viewModel.timezone.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()

    EphemerisTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                chartData?.let { data ->
                    ChartWheel(
                        chartData = data,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                if (isCalculating) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                DateTimeControls(
                    dateTime = dateTime,
                    onDateTimeChanged = { viewModel.updateDateTime(it) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                LocationControls(
                    location = location,
                    timezone = timezone,
                    onLocationChanged = { loc, tz -> viewModel.updateLocation(loc, tz) },
                )
            }
        }
    }
}
