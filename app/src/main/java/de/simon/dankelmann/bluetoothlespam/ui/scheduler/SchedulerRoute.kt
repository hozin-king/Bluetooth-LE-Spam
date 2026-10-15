package de.simon.dankelmann.bluetoothlespam.ui.scheduler

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Route entry point — [SchedulerViewModel] owns all state and persistence; the screen gets
 * refreshed on ON_RESUME so it picks up changes made elsewhere (e.g. the collection being
 * deleted while this screen was in the back stack).
 */
@Composable
fun SchedulerRoute() {
    val viewModel: SchedulerViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SchedulerScreen(
        enabled = viewModel.enabled,
        onEnabledChanged = viewModel::onEnabledChanged,
        hour = viewModel.hour,
        minute = viewModel.minute,
        onTimeSelected = viewModel::onTimeSelected,
        days = viewModel.days,
        onDayToggled = viewModel::onDayToggled,
        collections = viewModel.collections,
        selectedCollectionId = viewModel.collectionId,
        onCollectionSelected = viewModel::onCollectionSelected,
        nextRunText = viewModel.nextRunText,
        exactAlarmsGranted = viewModel.exactAlarmsGranted,
    )
}
