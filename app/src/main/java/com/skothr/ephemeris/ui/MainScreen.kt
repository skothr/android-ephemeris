package com.skothr.ephemeris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.skothr.ephemeris.settings.SettingsRepository
import com.skothr.ephemeris.settings.SettingsScreen
import com.skothr.ephemeris.settings.model.AppTheme
import com.skothr.ephemeris.ui.chart.ChartColors
import com.skothr.ephemeris.ui.chart.ChartWheel
import com.skothr.ephemeris.ui.controls.DateTimeControls
import com.skothr.ephemeris.ui.controls.LocationControls
import com.skothr.ephemeris.ui.theme.EphemerisTheme

@Composable
fun MainScreen(viewModel: ChartViewModel, settingsRepository: SettingsRepository) {
    val chartData by viewModel.chartData.collectAsState()
    val dateTime by viewModel.dateTime.collectAsState()
    val location by viewModel.location.collectAsState()
    val timezone by viewModel.timezone.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    EphemerisTheme(darkTheme = true) {
        if (showSettings) {
            SettingsScreen(
                settings = settings,
                repository = settingsRepository,
                onBack = { showSettings = false },
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ChartColors.appBackground(settings.visual.theme),
            ) {
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .imePadding(),
                ) {
                    // Chart area — scrollable, takes remaining space
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        chartData?.let { data ->
                            ChartWheel(
                                chartData = data,
                                visualSettings = settings.visual,
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

                        // Gear icon overlay
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (isCalculating) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }

                    // Controls — fixed at bottom, outside scroll
                    DateTimeControls(
                        dateTime = dateTime,
                        onDateTimeChanged = { viewModel.updateDateTime(it) },
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    LocationControls(
                        location = location,
                        timezone = timezone,
                        onLocationChanged = { loc, tz -> viewModel.updateLocation(loc, tz) },
                        onResultsVisible = {
                            coroutineScope.launch {
                                delay(100)
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                    )
                }
            }
        }
    }
}
