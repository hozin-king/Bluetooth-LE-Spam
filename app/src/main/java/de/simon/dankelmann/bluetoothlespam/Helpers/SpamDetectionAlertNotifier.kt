package de.simon.dankelmann.bluetoothlespam.Helpers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Enums.stringRes
import de.simon.dankelmann.bluetoothlespam.MainActivity
import de.simon.dankelmann.bluetoothlespam.Navigation.SpecterDestinations
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R

/**
 * Posts standalone "Spam detected: <type>" alert notifications (feature/spam-detection).
 *
 * This is separate from the foreground service's ongoing scan notification: these are
 * one-shot alerts that deep-link into the spam detector screen via
 * [MainActivity.EXTRA_DESTINATION]. Rate limiting keeps them from spamming the notification
 * shade: at most one alert per 60 seconds per [SpamPackageType].
 */
object SpamDetectionAlertNotifier {

    private const val CHANNEL_ID = "BluetoothLeSpamDetectionAlerts"
    private const val NOTIFICATION_ID_BASE = 3000
    private const val REQUEST_CODE_OPEN_SPAM_DETECTOR = 1003
    private const val RATE_LIMIT_MILLIS = 60_000L

    private val _lastNotifiedAt = mutableMapOf<SpamPackageType, Long>()

    /**
     * Posts the alert if the per-type rate limit allows it. Returns true when a notification
     * was actually posted.
     */
    fun notifyIfAllowed(context: Context, spamPackageType: SpamPackageType): Boolean {
        if (spamPackageType == SpamPackageType.UNKNOWN) return false

        val now = System.currentTimeMillis()
        synchronized(_lastNotifiedAt) {
            val last = _lastNotifiedAt[spamPackageType] ?: 0L
            if (now - last < RATE_LIMIT_MILLIS) return false
            _lastNotifiedAt[spamPackageType] = now
        }

        // Android 13+ silently drops notifications without POST_NOTIFICATIONS; check up front
        // so the rate-limit timestamp above isn't burned on a notification that never shows.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionCheck.checkPermission(Manifest.permission.POST_NOTIFICATIONS, context)
        ) {
            synchronized(_lastNotifiedAt) { _lastNotifiedAt.remove(spamPackageType) }
            return false
        }

        createNotificationChannel(context)

        val typeLabel = context.getString(spamPackageType.stringRes())
        val targetIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_DESTINATION, SpecterDestinations.SPAM_DETECTOR)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_SPAM_DETECTOR,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.bluetooth)
            .setContentTitle(context.getString(R.string.sd_notification_title, typeLabel))
            .setContentText(context.getString(R.string.sd_notification_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setChannelId(CHANNEL_ID)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_BASE + spamPackageType.ordinal, notification)
        return true
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.sd_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
