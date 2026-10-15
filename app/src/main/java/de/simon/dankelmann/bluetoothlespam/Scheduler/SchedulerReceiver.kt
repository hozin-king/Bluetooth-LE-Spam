package de.simon.dankelmann.bluetoothlespam.Scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.builtInCollectionDefinitions
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import java.util.Calendar

/**
 * Fires when the scheduled alarm goes off: starts advertising with the configured
 * collection, then re-arms [SchedulerManager] for the next occurrence (exact alarms are
 * one-shot).
 *
 * The trigger flow mirrors what the UI does when a user starts advertising by hand:
 * the domain collection is fully built from the database (sets included — freshly loaded
 * sets default to checked), handed to the queue handler, and activated, exactly like the
 * Quick Start / Advertisement Collection flows do.
 */
class SchedulerReceiver : BroadcastReceiver() {

    private val logTag = "SchedulerReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Thread {
            // Set once the main-thread post takes over finishing the pending result; the
            // finally block finishes it for every early-return / exception path instead.
            var mainThreadTookOver = false
            try {
                val appContext = context.applicationContext
                val repo = SettingsRepository.getInstance(appContext)
                val prefs = repo.current

                val enabled = prefs[SettingsKeys.SCHEDULER_ENABLED] ?: false
                if (!enabled) {
                    Log.d(logTag, "Alarm fired but scheduler is disabled, ignoring")
                    return@Thread
                }

                val days = SchedulerManager.parseDays(prefs[SettingsKeys.SCHEDULER_DAYS])
                val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                if (days.isNotEmpty() && today !in days) {
                    Log.d(logTag, "Alarm fired on an unscheduled day, ignoring")
                    return@Thread
                }

                val collectionId = prefs[SettingsKeys.SCHEDULER_COLLECTION_ID]
                    ?: SchedulerManager.KITCHEN_SINK_COLLECTION_ID
                val collection = loadCollection(appContext, collectionId)
                if (collection == null) {
                    Log.w(logTag, "Could not load scheduler collection id=$collectionId, skipping run")
                    return@Thread
                }

                // queueHandler.activate() is what the UI calls on the main thread (it may
                // show a Toast when nothing is selected), so hop back to main for it.
                Handler(Looper.getMainLooper()).post {
                    try {
                        val app = appContext as BleSpamApplication
                        app.queueHandler.apply {
                            deactivate(appContext)
                            if (collectionId == SchedulerManager.KITCHEN_SINK_COLLECTION_ID) {
                                // Same as tapping the "Kitchen Sink" card in the collections tab.
                                setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM)
                            }
                            setAdvertisementSetCollection(collection)
                            activate(appContext)
                        }
                    } catch (e: Exception) {
                        Log.e(logTag, "Failed to start scheduled advertising: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
                mainThreadTookOver = true
            } catch (e: Exception) {
                Log.e(logTag, "Scheduler run failed: ${e.message}")
            } finally {
                if (!mainThreadTookOver) {
                    pendingResult.finish()
                }
                // Always re-arm (or cancel when disabled) so exactly one future alarm exists.
                try {
                    SchedulerManager.schedule(context)
                } catch (e: Exception) {
                    Log.e(logTag, "Failed to re-schedule: ${e.message}")
                }
            }
        }.start()
    }

    /**
     * Builds the full domain collection (sets included) from the database — the same shape
     * the UI passes to the queue handler, but loaded synchronously since there's no screen
     * to show a loading skeleton on.
     */
    private fun loadCollection(context: Context, collectionId: Int): AdvertisementSetCollection? {
        if (collectionId == SchedulerManager.KITCHEN_SINK_COLLECTION_ID) {
            return buildKitchenSinkCollection(context)
        }
        val database = AppDatabase.getInstance()
        val withLists = database.advertisementSetCollectionDao().getCollectionWithLists(collectionId)
            ?: return null
        val collection = DatabaseHelpers.buildAdvertisementSetCollectionSkeletonFromEntity(withLists)
        withLists.lists.forEachIndexed { index, listEntity ->
            collection.advertisementSetLists[index].advertisementSets =
                DatabaseHelpers.getAllAdvertisementSetsForList(listEntity.id).toMutableList()
        }
        return collection
    }

    /** Same content the "Kitchen Sink Collection" card starts: every type, random queue mode. */
    private fun buildKitchenSinkCollection(context: Context): AdvertisementSetCollection {
        val definition = builtInCollectionDefinitions.firstOrNull { it.title == "Kitchen Sink Collection" }
        val collection = AdvertisementSetCollection()
        collection.title = definition?.title ?: "Kitchen Sink Collection"
        definition?.types?.forEach { type ->
            val list = AdvertisementSetList()
            list.title = "${context.getString(type.stringResId())} List"
            list.advertisementSets = DatabaseHelpers.getAllAdvertisementSetsForType(type).toMutableList()
            collection.advertisementSetLists.add(list)
        }
        return collection
    }
}
