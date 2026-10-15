package de.simon.dankelmann.bluetoothlespam.Helpers

import android.bluetooth.le.AdvertisingSetParameters as FrameworkAdvertisingSetParameters
import androidx.datastore.preferences.core.Preferences
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseData
import de.simon.dankelmann.bluetoothlespam.Models.AdvertiseSettings
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisingSetParameters
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import de.simon.dankelmann.bluetoothlespam.Models.PeriodicAdvertisingParameters
import de.simon.dankelmann.bluetoothlespam.Models.ServiceData
import org.json.JSONObject

/**
 * Single place where persisted TX-tuning settings are applied to an [AdvertisementSet] model
 * before it is handed to the Android BLE framework.
 *
 * [applyTuning] is pure: it never mutates the incoming set (the generators own their sets and
 * the queue handler may re-use them), it returns a copy with the effective TX power level
 * (per-target override wins over the global level) written to BOTH the legacy
 * [AdvertiseSettings] model and the modern [AdvertisingSetParameters] model, and the
 * persisted advertising interval written to [AdvertisingSetParameters.interval].
 *
 * Defaults resolve to exactly what the generators hardcode today (TX_POWER_HIGH +
 * [FrameworkAdvertisingSetParameters.INTERVAL_MIN]), so behavior is unchanged until the user
 * touches the "TX & Interval" preferences section.
 */
object AdvertisementTuningHelper {

    /** Preset advertising-interval values exposed by the preferences slider (units of 0.625 ms). */
    val INTERVAL_LOW_LATENCY = FrameworkAdvertisingSetParameters.INTERVAL_MIN // ~100 ms
    const val INTERVAL_BALANCED = 1600 // ~1 s
    const val INTERVAL_LOW_POWER = 16000 // ~10 s

    val intervalPresets = listOf(INTERVAL_LOW_LATENCY, INTERVAL_BALANCED, INTERVAL_LOW_POWER)

    fun applyTuning(set: AdvertisementSet, settings: Preferences): AdvertisementSet {
        val perTarget = parsePerTargetMap(settings[SettingsKeys.TX_TUNING_PER_TARGET] ?: "{}")
        val level = perTarget[set.target] ?: txPowerLevelFromName(settings[SettingsKeys.TX_TUNING_POWER])
        val interval = settings[SettingsKeys.TX_TUNING_INTERVAL]
            ?: FrameworkAdvertisingSetParameters.INTERVAL_MIN

        val tuned = copyAdvertisementSet(set)
        tuned.advertiseSettings.txPowerLevel = level
        tuned.advertisingSetParameters.txPowerLevel = level
        tuned.advertisingSetParameters.interval = interval
        return tuned
    }

    /** Resolves the global TX power level; unknown/missing values fall back to TX_POWER_HIGH. */
    fun txPowerLevelFromName(name: String?): TxPowerLevel {
        return try {
            if (name.isNullOrEmpty()) TxPowerLevel.TX_POWER_HIGH else TxPowerLevel.valueOf(name)
        } catch (_: IllegalArgumentException) {
            TxPowerLevel.TX_POWER_HIGH
        }
    }

    /**
     * Parses the per-target override map stored as JSON
     * (`{"ADVERTISEMENT_TARGET_IOS":"TX_POWER_LOW", ...}`). Malformed JSON or unknown enum
     * names degrade to an empty map instead of crashing.
     */
    fun parsePerTargetMap(json: String): Map<AdvertisementTarget, TxPowerLevel> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<AdvertisementTarget, TxPowerLevel>()
            for (target in AdvertisementTarget.entries) {
                if (obj.has(target.name)) {
                    val levelName = obj.optString(target.name, "")
                    if (levelName.isNotEmpty()) {
                        try {
                            map[target] = TxPowerLevel.valueOf(levelName)
                        } catch (_: IllegalArgumentException) {
                            // Skip unknown levels; keep the rest of the map usable.
                        }
                    }
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun serializePerTargetMap(map: Map<AdvertisementTarget, TxPowerLevel>): String {
        val obj = JSONObject()
        map.forEach { (target, level) -> obj.put(target.name, level.name) }
        return obj.toString()
    }

    /** Snaps an arbitrary stored interval to the nearest slider preset (for the UI). */
    fun nearestPresetIndex(interval: Int): Int {
        return intervalPresets.indices.minByOrNull { kotlin.math.abs(intervalPresets[it] - interval) } ?: 0
    }

    // --- Deep copies so the caller's set is never mutated -------------------------------

    private fun copyAdvertisementSet(set: AdvertisementSet): AdvertisementSet {
        val copy = AdvertisementSet()
        copy.id = set.id
        copy.title = set.title
        copy.target = set.target
        copy.type = set.type
        copy.duration = set.duration
        copy.maxExtendedAdvertisingEvents = set.maxExtendedAdvertisingEvents
        copy.range = set.range

        copy.advertiseSettings = copyAdvertiseSettings(set.advertiseSettings)
        copy.advertisingSetParameters = copyAdvertisingSetParameters(set.advertisingSetParameters)
        copy.advertiseData = copyAdvertiseData(set.advertiseData)
        copy.scanResponse = set.scanResponse?.let { copyAdvertiseData(it) }
        copy.periodicAdvertisingParameters = set.periodicAdvertisingParameters?.let {
            copyPeriodicAdvertisingParameters(it)
        }
        copy.periodicAdvertiseData = set.periodicAdvertiseData?.let { copyAdvertiseData(it) }

        // Callbacks are deliberately NOT copied: the advertisement service installs its own
        // callback on the tuned set after tuning.

        copy.currentlyAdvertising = set.currentlyAdvertising
        copy.advertisementState = set.advertisementState
        copy.isChecked = set.isChecked
        return copy
    }

    private fun copyAdvertiseSettings(settings: AdvertiseSettings): AdvertiseSettings {
        val copy = AdvertiseSettings()
        copy.id = settings.id
        copy.advertiseMode = settings.advertiseMode
        copy.txPowerLevel = settings.txPowerLevel
        copy.connectable = settings.connectable
        copy.timeout = settings.timeout
        return copy
    }

    private fun copyAdvertisingSetParameters(params: AdvertisingSetParameters): AdvertisingSetParameters {
        val copy = AdvertisingSetParameters()
        copy.id = params.id
        copy.legacyMode = params.legacyMode
        copy.interval = params.interval
        copy.txPowerLevel = params.txPowerLevel
        copy.includeTxPowerLevel = params.includeTxPowerLevel
        copy.primaryPhy = params.primaryPhy
        copy.secondaryPhy = params.secondaryPhy
        copy.scanable = params.scanable
        copy.connectable = params.connectable
        copy.anonymous = params.anonymous
        return copy
    }

    private fun copyAdvertiseData(data: AdvertiseData): AdvertiseData {
        val copy = AdvertiseData()
        copy.id = data.id
        copy.includeDeviceName = data.includeDeviceName
        copy.includeTxPower = data.includeTxPower
        data.manufacturerData.forEach { entry ->
            val entryCopy = ManufacturerSpecificData()
            entryCopy.id = entry.id
            entryCopy.manufacturerId = entry.manufacturerId
            entryCopy.manufacturerSpecificData = entry.manufacturerSpecificData
            copy.manufacturerData.add(entryCopy)
        }
        data.services.forEach { entry ->
            val entryCopy = ServiceData()
            entryCopy.id = entry.id
            entryCopy.serviceUuid = entry.serviceUuid
            entryCopy.serviceData = entry.serviceData
            copy.services.add(entryCopy)
        }
        return copy
    }

    private fun copyPeriodicAdvertisingParameters(params: PeriodicAdvertisingParameters): PeriodicAdvertisingParameters {
        val copy = PeriodicAdvertisingParameters()
        copy.id = params.id
        copy.includeTxPowerLevel = params.includeTxPowerLevel
        copy.interval = params.interval
        return copy
    }
}
