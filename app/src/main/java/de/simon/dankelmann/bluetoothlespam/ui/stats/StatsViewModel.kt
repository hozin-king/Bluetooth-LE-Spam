package de.simon.dankelmann.bluetoothlespam.ui.stats

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.simon.dankelmann.bluetoothlespam.Helpers.SpamAggregates
import de.simon.dankelmann.bluetoothlespam.Helpers.SpamSession
import de.simon.dankelmann.bluetoothlespam.Helpers.SpamStatsManager
import kotlinx.coroutines.launch

class StatsViewModel : ViewModel() {
    val aggregates = MutableLiveData<SpamAggregates>(SpamAggregates(0, 0L, emptyMap()))
    val recentSessions = MutableLiveData<List<SpamSession>>(emptyList())
    val isLoading = MutableLiveData<Boolean>(false)

    fun load(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            isLoading.value = true
            val manager = SpamStatsManager.getInstance(appContext)
            aggregates.value = manager.getAggregates()
            recentSessions.value = manager.getRecentSessions()
            isLoading.value = false
        }
    }

    fun clear(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            SpamStatsManager.getInstance(appContext).clear()
            load(appContext)
        }
    }
}
