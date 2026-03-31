package com.skothr.ephemeris.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.skothr.ephemeris.settings.model.AppSettings
import com.skothr.ephemeris.settings.tabs.CalculationTab
import com.skothr.ephemeris.settings.tabs.DisplayTab
import com.skothr.ephemeris.settings.tabs.AboutTab
import com.skothr.ephemeris.settings.tabs.VisualTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    repository: SettingsRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Calculation", "Display", "Visual", "About")

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> CalculationTab(
                    settings = settings.calculation,
                    onZodiacTypeChanged = { scope.launch { repository.setZodiacType(it) } },
                    onAyanamsaChanged = { scope.launch { repository.setAyanamsa(it) } },
                    onNodeTypeChanged = { scope.launch { repository.setNodeType(it) } },
                    onCenterChanged = { scope.launch { repository.setCenter(it) } },
                    onHouseSystemChanged = { scope.launch { repository.setHouseSystem(it) } },
                    onSpeedChanged = { scope.launch { repository.setSpeedInLongitude(it) } },
                    onEquatorialChanged = { scope.launch { repository.setEquatorialCoordinates(it) } },
                    onTruePositionChanged = { scope.launch { repository.setTruePosition(it) } },
                    onNoGravDeflChanged = { scope.launch { repository.setNoGravitationalDeflection(it) } },
                    onNoAberrationChanged = { scope.launch { repository.setNoAberration(it) } },
                    onJ2000Changed = { scope.launch { repository.setJ2000Equinox(it) } },
                    onNoNutationChanged = { scope.launch { repository.setNoNutation(it) } },
                    onIcrsChanged = { scope.launch { repository.setIcrsFrame(it) } },
                )
                1 -> DisplayTab(
                    settings = settings.display,
                    onBodyToggled = { body, enabled -> scope.launch { repository.setBodyEnabled(body, enabled) } },
                    onAspectToggled = { aspect, enabled -> scope.launch { repository.setAspectEnabled(aspect, enabled) } },
                    onAspectOrbChanged = { aspect, orb -> scope.launch { repository.setAspectOrb(aspect, orb) } },
                )
                2 -> VisualTab(
                    settings = settings.visual,
                    onThemeChanged = { scope.launch { repository.setTheme(it) } },
                    onSymbolStyleChanged = { scope.launch { repository.setSymbolStyle(it) } },
                    onLockAscendantChanged = { scope.launch { repository.setLockAscendant(it) } },
                    onColoredZodiacBandsChanged = { scope.launch { repository.setColoredZodiacBands(it) } },
                    onZodiacOuterChanged = { scope.launch { repository.setZodiacOuterRadius(it) } },
                    onZodiacInnerChanged = { scope.launch { repository.setZodiacInnerRadius(it) } },
                    onHouseOuterChanged = { scope.launch { repository.setHouseOuterRadius(it) } },
                    onHouseInnerChanged = { scope.launch { repository.setHouseInnerRadius(it) } },
                    onBodyRingChanged = { scope.launch { repository.setBodyRingRadius(it) } },
                    onAspectInnerChanged = { scope.launch { repository.setAspectInnerRadius(it) } },
                    onAspectThicknessChanged = { scope.launch { repository.setAspectLineThickness(it) } },
                    onAspectOpacityChanged = { scope.launch { repository.setAspectLineOpacity(it) } },
                    onMajorStyleChanged = { scope.launch { repository.setMajorAspectStyle(it) } },
                    onMinorStyleChanged = { scope.launch { repository.setMinorAspectStyle(it) } },
                    onScaleWidthByOrbChanged = { scope.launch { repository.setScaleWidthByOrb(it) } },
                    onWidthScaleOrbChanged = { scope.launch { repository.setWidthScaleOrb(it) } },
                )
                3 -> AboutTab()
            }
        }
    }
}
