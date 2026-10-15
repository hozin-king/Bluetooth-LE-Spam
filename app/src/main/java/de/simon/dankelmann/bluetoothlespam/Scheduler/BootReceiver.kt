package de.simon.dankelmann.bluetoothlespam.Scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Alarms don't survive a reboot, so re-arm the scheduler (no-op when it's disabled —
 * [SchedulerManager.schedule] cancels the alarm in that case).
 */
class BootReceiver : BroadcastReceiver() {

    private val logTag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val pendingResult = goAsync()
        Thread {
            try {
                // schedule() reads the DataStore snapshot; off the main thread because the
                // first read after boot does a blocking load.
                SchedulerManager.schedule(context)
            } catch (e: Exception) {
                Log.e(logTag, "Failed to re-schedule after boot: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}
