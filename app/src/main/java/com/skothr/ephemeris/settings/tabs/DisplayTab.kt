package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.chart.models.Aspect
import com.skothr.ephemeris.chart.models.CelestialBody
import com.skothr.ephemeris.settings.components.OrbEditDialog
import com.skothr.ephemeris.settings.model.DisplaySettings

@Composable
fun DisplayTab(
    settings: DisplaySettings,
    onBodyToggled: (CelestialBody, Boolean) -> Unit,
    onAspectToggled: (Aspect, Boolean) -> Unit,
    onAspectOrbChanged: (Aspect, Double) -> Unit,
) {
    var editingAspect by remember { mutableStateOf<Aspect?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Bodies section
        SectionLabel("Bodies")

        // 2-column grid for bodies
        val bodies = CelestialBody.entries
        for (i in bodies.indices step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyToggleItem(
                    body = bodies[i],
                    enabled = bodies[i] in settings.enabledBodies,
                    onToggle = { onBodyToggled(bodies[i], it) },
                    modifier = Modifier.weight(1f),
                )
                if (i + 1 < bodies.size) {
                    BodyToggleItem(
                        body = bodies[i + 1],
                        enabled = bodies[i + 1] in settings.enabledBodies,
                        onToggle = { onBodyToggled(bodies[i + 1], it) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Aspects section
        SectionLabel("Aspects")
        Text(
            "Tap orb value to edit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Aspect.entries.forEach { aspect ->
            AspectToggleRow(
                aspect = aspect,
                enabled = aspect in settings.enabledAspects,
                orb = settings.aspectOrbs[aspect] ?: aspect.defaultOrb,
                onToggle = { onAspectToggled(aspect, it) },
                onOrbClick = { editingAspect = aspect },
            )
        }
    }

    // Orb edit dialog
    editingAspect?.let { aspect ->
        OrbEditDialog(
            aspectName = aspect.displayName,
            currentOrb = settings.aspectOrbs[aspect] ?: aspect.defaultOrb,
            onConfirm = { newOrb ->
                onAspectOrbChanged(aspect, newOrb)
                editingAspect = null
            },
            onDismiss = { editingAspect = null },
        )
    }
}

@Composable
private fun BodyToggleItem(
    body: CelestialBody,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${body.symbol} ${body.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun AspectToggleRow(
    aspect: Aspect,
    enabled: Boolean,
    orb: Double,
    onToggle: (Boolean) -> Unit,
    onOrbClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = enabled, onCheckedChange = onToggle)
        Text(
            "${aspect.displayName} (${aspect.angle.toInt()}°)",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "%.1f°".format(orb),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onOrbClick),
        )
    }
}
