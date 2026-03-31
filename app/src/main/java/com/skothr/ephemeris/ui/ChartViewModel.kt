package com.skothr.ephemeris.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skothr.ephemeris.chart.ChartCalculator
import com.skothr.ephemeris.chart.models.ChartData
import com.skothr.ephemeris.chart.models.Location
import com.skothr.ephemeris.ephemeris.EphemerisProvider
import com.skothr.ephemeris.settings.SettingsRepository
import com.skothr.ephemeris.settings.model.AppSettings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ChartViewModel(
    ephemeris: EphemerisProvider,
    private val ephemerisReady: kotlinx.coroutines.Deferred<Unit>,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val chartCalculator = ChartCalculator(ephemeris)

    private val _dateTime = MutableStateFlow(LocalDateTime.now())
    val dateTime: StateFlow<LocalDateTime> = _dateTime.asStateFlow()

    private val _location = MutableStateFlow(Location(40.7128, -74.0060))
    val location: StateFlow<Location> = _location.asStateFlow()

    private val _timezone = MutableStateFlow("America/New_York")
    val timezone: StateFlow<String> = _timezone.asStateFlow()

    private val _chartData = MutableStateFlow<ChartData?>(null)
    val chartData: StateFlow<ChartData?> = _chartData.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            ephemerisReady.await()
            combine(_dateTime, _location, settingsRepository.settings) { dt, loc, s ->
                Triple(dt, loc, s)
            }
                .debounce(33)
                .collectLatest { (dt, loc, s) ->
                    _isCalculating.value = true
                    try {
                        _chartData.value = chartCalculator.calculate(dt, loc, s)
                    } catch (e: Exception) {
                        Log.e("ChartViewModel", "Chart calculation failed", e)
                    } finally {
                        _isCalculating.value = false
                    }
                }
        }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _dateTime.value = dateTime
    }

    fun updateLocation(location: Location, timezone: String) {
        _location.value = location
        _timezone.value = timezone
    }
}
