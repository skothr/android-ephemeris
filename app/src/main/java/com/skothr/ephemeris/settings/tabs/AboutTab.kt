package com.skothr.ephemeris.settings.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skothr.ephemeris.BuildConfig

@Composable
fun AboutTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // App info
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Ephemeris",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Description
        Text(
            text = "A precise astrological chart calculator powered by the Swiss Ephemeris library.",
            style = MaterialTheme.typography.bodyLarge,
        )

        // Licenses
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Licenses")

            LicenseItem(
                name = "Ephemeris",
                license = "GNU Affero General Public License v3 (AGPL-3.0)",
            )
            LicenseItem(
                name = "Swiss Ephemeris",
                license = "GNU Affero General Public License (AGPL)",
                copyright = "Astrodienst AG, Switzerland",
            )
            LicenseItem(
                name = "Astronomicon Font",
                license = "SIL Open Font License 1.1",
            )
        }
    }
}

@Composable
private fun LicenseItem(
    name: String,
    license: String,
    copyright: String? = null,
) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (copyright != null) {
            Text(
                text = copyright,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = license,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
