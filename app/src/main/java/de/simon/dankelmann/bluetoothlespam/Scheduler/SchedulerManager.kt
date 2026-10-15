package de.simon.dankelmann.bluetoothlespam.Scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import java.util.Calendar

/**
 * Owns the one-shot exact [AlarmManager] alarm that drives the automatic spam scheduler.
 *
 * Exact alarms are one-shot, so [SchedulerReceiver] calls [schedule] again after every fire
 * to arm the next occurrence; [BootReceiver] re-arms after a reboot. On API 31+, when the
 * user hasn't granted exact-alarm access, we fall back to [AlarmManager.setAndAllowWhileIdle]
 * (inexact) instead of silently failing.
 */
object SchedulerManager {
    private const val LOG_TAG = "SchedulerManager"
    private const val REQUEST_CODE = 9001

    /** Scheduler collection id that means "kitchen sink" (all spam types), see [SettingsKeys.SCHEDULER_COLLECTION_ID]. */
    const val KITCHEN_SINK_COLLECTION_ID = -1
    const val DEFAULT_HOUR = 9
    const val DEFAULT_MINUTE = 0

    /**
     * Days of week as [Calendar] day-of-week numbers (1=Sunday..7=Saturday); an empty set
     * (and an empty/missing preference value) means "daily".
     */
    fun parseDays(value: String?): Set<Int> =
        value.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()

    fun daysToString(days: Set<Int>): String =
        days.filter { it in 1..7 }.sorted().joinToString(",")

    /** Next trigger time (epoch millis) for the given schedule, strictly after now. */
    fun nextTriggerMillis(hour: Int, minute: Int, days: Set<Int>): Long {
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis > now && (days.isEmpty() || candidate.get(Calendar.DAY_OF_WEEK) in days)) {
                return candidate.timeInMillis
            }
        }
        // Unreachable: the empty-days ("daily") case always matches within 8 days.
        return now + 24L * 60 * 60 * 1000
    }

    /** Whether the next scheduled run can use an exact alarm on this device. */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun schedulerPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SchedulerReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Arms the alarm for the next occurrence; cancels it when the scheduler is disabled. */
    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val repo = SettingsRepository.getInstance(appContext)
        val prefs = repo.current
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val enabled = prefs[SettingsKeys.SCHEDULER_ENABLED] ?: false
        if (!enabled) {
            alarmManager.cancel(schedulerPendingIntent(appContext))
            Log.d(LOG_TAG, "Scheduler disabled, alarm cancelled")
            return
        }

        val hour = prefs[SettingsKeys.SCHEDULER_HOUR] ?: DEFAULT_HOUR
        val minute = prefs[SettingsKeys.SCHEDULER_MINUTE] ?: DEFAULT_MINUTE
        val days = parseDays(prefs[SettingsKeys.SCHEDULER_DAYS])
        val triggerAt = nextTriggerMillis(hour, minute, days)
        val pendingIntent = schedulerPendingIntent(appContext)

        if (canScheduleExactAlarms(appContext)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            Log.d(LOG_TAG, "Exact alarm scheduled for $triggerAt")
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            Log.w(LOG_TAG, "Exact alarms not granted, falling back to inexact alarm for $triggerAt")
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(schedulerPendingIntent(appContext))
    }
}
