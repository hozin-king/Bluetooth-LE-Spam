package de.simon.dankelmann.bluetoothlespam.ui.rangeTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance

/**
 * Range test screen: shows the effective TX power + advertising interval (as persisted in
 * Preferences › TX & Interval), a live elapsed timer, and a Start/Stop button that advertises
 * a single clearly-labeled static beacon ("Range Test Beacon", Apple manufacturer ID 76)
 * through the normal advertisement path.
 */
@Composable
fun RangeTestScreen(
    txPowerLabel: String,
    intervalLabel: String,
    intervalRaw: Int,
    rangeTestEnabled: Boolean,
    isAdvertising: Boolean,
    elapsedSeconds: Long,
    statusText: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + SpecterTopAppBarClearance,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tx_range_test_beacon_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isAdvertising) stringResource(R.string.tx_range_test_status_advertising)
                        else stringResource(R.string.tx_range_test_status_idle),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAdvertising) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${stringResource(R.string.tx_range_test_elapsed)}: ${formatElapsed(elapsedSeconds)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (statusText.isNotEmpty()) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tx_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${stringResource(R.string.tx_power_title)}: $txPowerLabel",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${stringResource(R.string.tx_interval_title)}: $intervalLabel ($intervalRaw × 0.625 ms)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            if (!rangeTestEnabled) {
                Text(
                    text = stringResource(R.string.tx_range_test_disabled_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Button(
                onClick = { if (isAdvertising) onStop() else onStart() },
                enabled = if (isAdvertising) true else rangeTestEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isAdvertising) stringResource(R.string.tx_range_test_stop)
                    else stringResource(R.string.tx_range_test_start),
                )
            }
        }
    }
}

private fun formatElapsed(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
