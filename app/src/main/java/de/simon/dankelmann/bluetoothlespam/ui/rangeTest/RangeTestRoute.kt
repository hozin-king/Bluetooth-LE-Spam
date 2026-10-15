package de.simon.dankelmann.bluetoothlespam.ui.rangeTest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId
import de.simon.dankelmann.bluetoothlespam.Helpers.AdvertisementTuningHelper
import de.simon.dankelmann.bluetoothlespam.R

/** Route entry point — owns the business logic for the Range Test screen. */
@Composable
fun RangeTestRoute() {
    val context = LocalContext.current
    val app = context.applicationContext as BleSpamApplication
    val viewModel: RangeTestViewModel = viewModel()

    LaunchedEffect(app) {
        viewModel.observeSettings(app)
    }

    val txPower by viewModel.txPower.collectAsState()
    val interval by viewModel.interval.collectAsState()
    val rangeTestEnabled by viewModel.rangeTestEnabled.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    val intervalLabelRes = when (AdvertisementTuningHelper.nearestPresetIndex(interval)) {
        1 -> R.string.tx_interval_balanced
        2 -> R.string.tx_interval_low_power
        else -> R.string.tx_interval_low_latency
    }

    RangeTestScreen(
        txPowerLabel = stringResource(txPower.toStringId()),
        intervalLabel = stringResource(intervalLabelRes),
        intervalRaw = interval,
        rangeTestEnabled = rangeTestEnabled,
        isAdvertising = isAdvertising,
        elapsedSeconds = elapsedSeconds,
        statusText = statusText,
        onStart = { viewModel.start(app) },
        onStop = { viewModel.stop(app) },
    )
}
