package com.skothr.ephemeris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.skothr.ephemeris.ui.ChartViewModel
import com.skothr.ephemeris.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ChartViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as EphemerisApp
                return ChartViewModel(app.swissEphemeris, app.ephemerisReady) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel)
        }
    }
}
