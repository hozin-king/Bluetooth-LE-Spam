package de.simon.dankelmann.bluetoothlespam.ui.scheduler

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Scheduler.SchedulerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** A collection entry shown in the scheduler's dropdown (id from the database entity). */
data class SchedulerCollectionOption(val id: Int, val title: String)

/**
 * Holds the scheduler screen state and persists changes through [SettingsRepository]
 * (suspend setters, so [SchedulerManager.schedule]/[SchedulerManager.cancel] always see
 * the just-written values — the fire-and-forget Async variants would race them).
 */
class SchedulerViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val repository = SettingsRepository.getInstance(context)

    var enabled by mutableStateOf(false)
        private set
    var hour by mutableIntStateOf(SchedulerManager.DEFAULT_HOUR)
        private set
    var minute by mutableIntStateOf(SchedulerManager.DEFAULT_MINUTE)
        private set

    /** Selected days as [Calendar] day-of-week numbers (1=Sunday..7=Saturday); empty = daily. */
    var days by mutableStateOf<Set<Int>>(emptySet())
        private set

    var collectionId by mutableIntStateOf(SchedulerManager.KITCHEN_SINK_COLLECTION_ID)
        private set

    var collections by mutableStateOf<List<SchedulerCollectionOption>>(emptyList())
        private set

    var nextRunText by mutableStateOf("")
        private set

    var exactAlarmsGranted by mutableStateOf(true)
        private set

    init {
        refresh()
    }

    fun refresh() {
        val prefs = repository.current
        enabled = prefs[SettingsKeys.SCHEDULER_ENABLED] ?: false
        hour = prefs[SettingsKeys.SCHEDULER_HOUR] ?: SchedulerManager.DEFAULT_HOUR
        minute = prefs[SettingsKeys.SCHEDULER_MINUTE] ?: SchedulerManager.DEFAULT_MINUTE
        days = SchedulerManager.parseDays(prefs[SettingsKeys.SCHEDULER_DAYS])
        collectionId = prefs[SettingsKeys.SCHEDULER_COLLECTION_ID]
            ?: SchedulerManager.KITCHEN_SINK_COLLECTION_ID
        updateNextRunText()

        viewModelScope.launch(Dispatchers.IO) {
            val options = mutableListOf(
                SchedulerCollectionOption(
                    SchedulerManager.KITCHEN_SINK_COLLECTION_ID,
                    context.getString(R.string.sched_collection_kitchen_sink),
                ),
            )
            AppDatabase.getInstance().advertisementSetCollectionDao().getAll().forEach { entity ->
                options.add(SchedulerCollectionOption(entity.id, entity.title))
            }
            withContext(Dispatchers.Main) {
                collections = options
            }
        }
    }

    fun onEnabledChanged(value: Boolean) {
        viewModelScope.launch {
            repository.setSchedulerEnabled(value)
            enabled = value
            if (value) {
                SchedulerManager.schedule(context)
            } else {
                SchedulerManager.cancel(context)
            }
            updateNextRunText()
        }
    }

    fun onTimeSelected(newHour: Int, newMinute: Int) {
        viewModelScope.launch {
            repository.setSchedulerHour(newHour)
            repository.setSchedulerMinute(newMinute)
            hour = newHour
            minute = newMinute
            SchedulerManager.schedule(context)
            updateNextRunText()
        }
    }

    fun onDayToggled(dayOfWeek: Int) {
        viewModelScope.launch {
            val newDays = if (dayOfWeek in days) days - dayOfWeek else days + dayOfWeek
            repository.setSchedulerDays(SchedulerManager.daysToString(newDays))
            days = newDays
            SchedulerManager.schedule(context)
            updateNextRunText()
        }
    }

    fun onCollectionSelected(id: Int) {
        viewModelScope.launch {
            repository.setSchedulerCollectionId(id)
            collectionId = id
            SchedulerManager.schedule(context)
            updateNextRunText()
        }
    }

    private fun updateNextRunText() {
        exactAlarmsGranted = SchedulerManager.canScheduleExactAlarms(context)
        nextRunText = if (!enabled) {
            context.getString(R.string.sched_next_run_disabled)
        } else {
            val trigger = SchedulerManager.nextTriggerMillis(hour, minute, days)
            context.getString(
                R.string.sched_next_run,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(trigger)),
            )
        }
    }
}
