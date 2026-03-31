package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.chart.models.HouseSystem
import com.skothr.ephemeris.settings.components.SegmentedToggle
import com.skothr.ephemeris.settings.components.SettingsDropdown
import com.skothr.ephemeris.settings.model.*

@Composable
fun CalculationTab(
    settings: CalculationSettings,
    onZodiacTypeChanged: (ZodiacType) -> Unit,
    onAyanamsaChanged: (Ayanamsa) -> Unit,
    onNodeTypeChanged: (NodeType) -> Unit,
    onCenterChanged: (Center) -> Unit,
    onHouseSystemChanged: (HouseSystem) -> Unit,
    onSpeedChanged: (Boolean) -> Unit,
    onEquatorialChanged: (Boolean) -> Unit,
    onTruePositionChanged: (Boolean) -> Unit,
    onNoGravDeflChanged: (Boolean) -> Unit,
    onNoAberrationChanged: (Boolean) -> Unit,
    onJ2000Changed: (Boolean) -> Unit,
    onNoNutationChanged: (Boolean) -> Unit,
    onIcrsChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Zodiac Type
        SectionLabel("Zodiac Type")
        SegmentedToggle(
            options = ZodiacType.entries,
            selected = settings.zodiacType,
            onSelected = onZodiacTypeChanged,
            label = { it.displayName },
        )

        // Ayanamsa (only visible for Sidereal)
        if (settings.zodiacType == ZodiacType.SIDEREAL) {
            SectionLabel("Ayanamsa")
            SettingsDropdown(
                options = Ayanamsa.entries,
                selected = settings.ayanamsa,
                onSelected = onAyanamsaChanged,
                label = { it.displayName },
            )
        }

        // Node Type
        SectionLabel("Node Type")
        SegmentedToggle(
            options = NodeType.entries,
            selected = settings.nodeType,
            onSelected = onNodeTypeChanged,
            label = { it.displayName },
        )

        // Center
        SectionLabel("Center")
        SegmentedToggle(
            options = Center.entries,
            selected = settings.center,
            onSelected = onCenterChanged,
            label = { it.displayName },
        )

        // House System
        SectionLabel("House System")
        SettingsDropdown(
            options = HouseSystem.entries,
            selected = settings.houseSystem,
            onSelected = onHouseSystemChanged,
            label = { it.displayName },
        )

        // Toggle flags
        SectionLabel("Options")
        SettingsToggleRow(
            label = "Speed in longitude",
            checked = settings.speedInLongitude,
            onCheckedChange = onSpeedChanged,
        )
        SettingsToggleRow(
            label = "Equatorial coordinates",
            checked = settings.equatorialCoordinates,
            onCheckedChange = onEquatorialChanged,
        )

        // Corrections
        SectionLabel("Corrections")
        SettingsToggleRow(
            label = "True position",
            checked = settings.truePosition,
            onCheckedChange = onTruePositionChanged,
        )
        SettingsToggleRow(
            label = "No gravitational deflection",
            checked = settings.noGravitationalDeflection,
            onCheckedChange = onNoGravDeflChanged,
        )
        SettingsToggleRow(
            label = "No aberration",
            checked = settings.noAberration,
            onCheckedChange = onNoAberrationChanged,
        )

        // Reference Frame
        SectionLabel("Reference Frame")
        SettingsToggleRow(
            label = "J2000 equinox",
            checked = settings.j2000Equinox,
            onCheckedChange = onJ2000Changed,
        )
        SettingsToggleRow(
            label = "No nutation",
            checked = settings.noNutation,
            onCheckedChange = onNoNutationChanged,
        )
        SettingsToggleRow(
            label = "ICRS frame",
            checked = settings.icrsFrame,
            onCheckedChange = onIcrsChanged,
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
internal fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
