package de.simon.dankelmann.bluetoothlespam.Datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Preference keys migrated here MUST keep the exact string values they had as
 * `PreferenceManager.getDefaultSharedPreferences` keys (see [ThemeManager] / `preferences.xml`
 * history) — [SettingsRepository]'s migration matches on these literal names.
 */
object SettingsKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
    val BLUR_ENABLED = booleanPreferencesKey("blur_enabled")
    val ALLOW_CUSTOM_SWIFT_PAIR_NAMES = booleanPreferencesKey("allow_custom_swift_pair_names")
    val SPAM_DETECTION_BACKGROUND_ENABLED = booleanPreferencesKey("spam_detection_background_enabled")
    val SPAM_DETECTION_ALERT_ENABLED = booleanPreferencesKey("spam_detection_alert_enabled")
    val BUILT_IN_SEED_VERSION = intPreferencesKey("built_in_seed_version")
    // Automatic spam scheduler (AlarmManager). SCHEDULER_DAYS holds Calendar day-of-week
    // numbers (1=Sunday..7=Saturday) as a comma-separated string, e.g. "2,3,4,5,6" for
    // Mon-Fri; an empty string means daily. SCHEDULER_COLLECTION_ID -1 = kitchen sink (all).
    val SCHEDULER_ENABLED = booleanPreferencesKey("scheduler_enabled")
    val SCHEDULER_HOUR = intPreferencesKey("scheduler_hour")
    val SCHEDULER_MINUTE = intPreferencesKey("scheduler_minute")
    val SCHEDULER_DAYS = stringPreferencesKey("scheduler_days")
    val SCHEDULER_COLLECTION_ID = intPreferencesKey("scheduler_collection_id")

    // TX tuning (tx-tuning feature): persisted TX power / advertising interval / per-target
    // overrides / range-test mode. Read synchronously via [SettingsRepository.current] on the
    // advertisement choke point; defaults keep behavior identical to the hardcoded
    // TX_POWER_HIGH + AdvertisingSetParameters.INTERVAL_MIN the generators use.
    val TX_TUNING_POWER = stringPreferencesKey("tx_tuning_power")
    val TX_TUNING_INTERVAL = intPreferencesKey("tx_tuning_interval")
    val TX_TUNING_PER_TARGET = stringPreferencesKey("tx_tuning_per_target")
    val TX_TUNING_RANGE_TEST = booleanPreferencesKey("tx_tuning_range_test")
}

/**
 * DataStore-backed settings store (plan §2/§11 — theme mode, seed color, dynamic color, blur).
 *
 * `use_legacy_advertising`/advertising-interval/logging settings deliberately stay on
 * `PreferenceManager.getDefaultSharedPreferences` — those are read synchronously on the BLE
 * advertising hot path ([de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeAdvertisementService],
 * [de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers]) and logging already has its own
 * independent SharedPreferences-backed store in `LogFileManager`; moving either onto DataStore's
 * async model was not worth the regression risk for a UI-framework migration.
 *
 * [current] gives synchronous access to the latest snapshot for call sites (e.g. [ThemeManager])
 * that historically read SharedPreferences synchronously — backed by a [StateFlow] that is
 * warmed with a one-time blocking read (which also runs the legacy-value migration, see
 * [migrateFromLegacySharedPreferencesIfNeeded]) in [warmUp], called once from
 * `BleSpamApplication.onCreate` before any UI reads it, so callers never see stale defaults.
 */
class SettingsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(appContext.filesDir, "datastore/specter_settings.preferences_pb") },
    )

    private val initialSnapshot: Preferences by lazy {
        runBlocking { migrateFromLegacySharedPreferencesIfNeeded() }
    }

    val preferencesFlow: StateFlow<Preferences> by lazy {
        dataStore.data.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = initialSnapshot,
        )
    }

    /** Synchronous snapshot for legacy (non-suspend) call sites — see class doc. */
    val current: Preferences get() = preferencesFlow.value

    /**
     * Version of the built-in advertisement-set data this install has already seeded/synced
     * (0 before this feature existed). Compared against [de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers.BUILT_IN_SEED_VERSION]
     * on startup to decide whether to top up the built-in sets. Synchronous, like [current].
     */
    val builtInSeedVersion: Int get() = current[SettingsKeys.BUILT_IN_SEED_VERSION] ?: 0

    /** Forces the one-time blocking load (and migration) to happen up front. Call once at process start. */
    fun warmUp() {
        preferencesFlow
    }

    /**
     * Runs once, the first time this DataStore is ever read: if it has no theme_mode/
     * theme_seed_color of its own yet, carries over whatever a pre-upgrade install had in the
     * classic `PreferenceManager.getDefaultSharedPreferences` file (plan §11 REGRESSION RULE —
     * existing users must not lose these on upgrade). No-op forever after the first real write.
     */
    private suspend fun migrateFromLegacySharedPreferencesIfNeeded(): Preferences {
        val existing = dataStore.data.first()
        if (existing.contains(SettingsKeys.THEME_MODE) || existing.contains(SettingsKeys.THEME_SEED_COLOR)) {
            return existing
        }

        val legacyPrefs = appContext.getSharedPreferences("${appContext.packageName}_preferences", Context.MODE_PRIVATE)
        val legacyThemeMode = legacyPrefs.getString(SettingsKeys.THEME_MODE.name, null)
        val legacySeedColor = if (legacyPrefs.contains(SettingsKeys.THEME_SEED_COLOR.name)) {
            legacyPrefs.getInt(SettingsKeys.THEME_SEED_COLOR.name, 0)
        } else {
            null
        }

        if (legacyThemeMode == null && legacySeedColor == null) return existing

        return dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                legacyThemeMode?.let { this[SettingsKeys.THEME_MODE] = it }
                legacySeedColor?.let { this[SettingsKeys.THEME_SEED_COLOR] = it }
            }
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[SettingsKeys.THEME_MODE] = mode }
    }

    suspend fun setThemeSeedColor(argb: Int) {
        dataStore.edit { it[SettingsKeys.THEME_SEED_COLOR] = argb }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    suspend fun setBlurEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.BLUR_ENABLED] = enabled }
    }

    suspend fun setAllowCustomSwiftPairNamesEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.ALLOW_CUSTOM_SWIFT_PAIR_NAMES] = enabled }
    }

    suspend fun setSpamDetectionBackgroundEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.SPAM_DETECTION_BACKGROUND_ENABLED] = enabled }
    }

    suspend fun setSpamDetectionAlertEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.SPAM_DETECTION_ALERT_ENABLED] = enabled }
    }

    suspend fun setBuiltInSeedVersion(version: Int) {
        dataStore.edit { it[SettingsKeys.BUILT_IN_SEED_VERSION] = version }
    }

    suspend fun setSchedulerEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.SCHEDULER_ENABLED] = enabled }
    }

    suspend fun setSchedulerHour(hour: Int) {
        dataStore.edit { it[SettingsKeys.SCHEDULER_HOUR] = hour }
    }

    suspend fun setSchedulerMinute(minute: Int) {
        dataStore.edit { it[SettingsKeys.SCHEDULER_MINUTE] = minute }
    }

    suspend fun setSchedulerDays(days: String) {
        dataStore.edit { it[SettingsKeys.SCHEDULER_DAYS] = days }
    }

    suspend fun setSchedulerCollectionId(collectionId: Int) {
        dataStore.edit { it[SettingsKeys.SCHEDULER_COLLECTION_ID] = collectionId }
    }

    suspend fun setTxTuningPower(powerLevelName: String) {
        dataStore.edit { it[SettingsKeys.TX_TUNING_POWER] = powerLevelName }
    }

    suspend fun setTxTuningInterval(interval: Int) {
        dataStore.edit { it[SettingsKeys.TX_TUNING_INTERVAL] = interval }
    }

    suspend fun setTxTuningPerTarget(json: String) {
        dataStore.edit { it[SettingsKeys.TX_TUNING_PER_TARGET] = json }
    }

    suspend fun setTxTuningRangeTest(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.TX_TUNING_RANGE_TEST] = enabled }
    }

    /** Fire-and-forget variants for non-suspend call sites (e.g. [ThemeManager]) — launched on
     * this repository's own long-lived [scope] rather than each caller creating its own. */
    fun setThemeModeAsync(mode: String) {
        scope.launch { setThemeMode(mode) }
    }

    fun setThemeSeedColorAsync(argb: Int) {
        scope.launch { setThemeSeedColor(argb) }
    }

    fun setDynamicColorEnabledAsync(enabled: Boolean) {
        scope.launch { setDynamicColorEnabled(enabled) }
    }

    fun setBlurEnabledAsync(enabled: Boolean) {
        scope.launch { setBlurEnabled(enabled) }
    }

    fun setAllowCustomSwiftPairNamesEnabledAsync(enabled: Boolean) {
        scope.launch { setAllowCustomSwiftPairNamesEnabled(enabled) }
    }

    fun setSpamDetectionBackgroundEnabledAsync(enabled: Boolean) {
        scope.launch { setSpamDetectionBackgroundEnabled(enabled) }
    }

    fun setSpamDetectionAlertEnabledAsync(enabled: Boolean) {
        scope.launch { setSpamDetectionAlertEnabled(enabled) }
    }

    fun setSchedulerEnabledAsync(enabled: Boolean) {
        scope.launch { setSchedulerEnabled(enabled) }
    }

    fun setSchedulerHourAsync(hour: Int) {
        scope.launch { setSchedulerHour(hour) }
    }

    fun setSchedulerMinuteAsync(minute: Int) {
        scope.launch { setSchedulerMinute(minute) }
    }

    fun setSchedulerDaysAsync(days: String) {
        scope.launch { setSchedulerDays(days) }
    }

    fun setSchedulerCollectionIdAsync(collectionId: Int) {
        scope.launch { setSchedulerCollectionId(collectionId) }
    }

    fun setTxTuningPowerAsync(powerLevelName: String) {
        scope.launch { setTxTuningPower(powerLevelName) }
    }

    fun setTxTuningIntervalAsync(interval: Int) {
        scope.launch { setTxTuningInterval(interval) }
    }

    fun setTxTuningPerTargetAsync(json: String) {
        scope.launch { setTxTuningPerTarget(json) }
    }

    fun setTxTuningRangeTestAsync(enabled: Boolean) {
        scope.launch { setTxTuningRangeTest(enabled) }
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
