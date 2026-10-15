package de.simon.dankelmann.bluetoothlespam.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/** Route entry point — owns loading and clearing the spam session statistics. */
@Composable
fun StatsRoute() {
    val context = LocalContext.current
    val viewModel: StatsViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    val aggregates by viewModel.aggregates.observeAsState()
    val recentSessions by viewModel.recentSessions.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StatsScreen(
        aggregates = aggregates,
        recentSessions = recentSessions,
        isLoading = isLoading == true,
        onClearClicked = { viewModel.clear(context) },
    )
}
