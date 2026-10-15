package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData

/**
 * Builds a legacy-mode [AdvertisementSet] from a user-supplied manufacturer ID and raw
 * manufacturer-specific payload. Used by the custom payload builder UI.
 *
 * Expected [inputData] keys:
 * - `"manufacturerId"`: decimal string, 0..65535 (e.g. "76" for Apple)
 * - `"payloadHex"`: even-length hex string holding the raw manufacturer payload bytes
 * - `"title"`: optional, defaults to [DEFAULT_TITLE] when blank
 * - `"txPowerLevel"`: optional [TxPowerLevel] enum name, defaults to TX_POWER_HIGH
 *
 * Returns an empty list on any invalid input and never throws.
 */
class CustomPayloadAdvertisementSetGenerator : IAdvertisementSetGenerator {

    companion object {
        const val KEY_MANUFACTURER_ID = "manufacturerId"
        const val KEY_PAYLOAD_HEX = "payloadHex"
        const val KEY_TITLE = "title"
        const val KEY_TX_POWER_LEVEL = "txPowerLevel"

        const val DEFAULT_TITLE = "Custom Payload"

        /**
         * A legacy advertising packet is 31 bytes; the manufacturer-specific AD structure
         * needs 2 bytes type/length framing plus the 2-byte company ID, leaving at most
         * this many bytes for the actual payload.
         */
        const val MAX_LEGACY_PAYLOAD_BYTES = 27

        private val HEX_REGEX = Regex("^[0-9a-fA-F]+$")
    }

    private val _logTag = "CustomPayloadAdvertisementSetGenerator"

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        if (inputData == null) {
            return emptyList()
        }

        // Manufacturer ID must be a decimal int in the 16-bit company-ID range
        val manufacturerId = inputData[KEY_MANUFACTURER_ID]?.trim()?.toIntOrNull()
        if (manufacturerId == null || manufacturerId !in 0..65535) {
            return emptyList()
        }

        // Hex validation: non-empty, even length, hex chars only, fits a legacy packet
        val payloadHex = inputData[KEY_PAYLOAD_HEX]?.trim()
        if (payloadHex.isNullOrEmpty() ||
            payloadHex.length % 2 != 0 ||
            !HEX_REGEX.matches(payloadHex) ||
            payloadHex.length / 2 > MAX_LEGACY_PAYLOAD_BYTES
        ) {
            return emptyList()
        }

        // decodeHex throws on odd length / non-hex chars; guarded above, but never throw
        val payload = try {
            StringHelpers.decodeHex(payloadHex)
        } catch (e: Exception) {
            Log.d(_logTag, "Invalid payload hex: ${e.message}")
            return emptyList()
        }

        val title = inputData[KEY_TITLE]?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE

        val txPowerLevel = try {
            inputData[KEY_TX_POWER_LEVEL]?.let { TxPowerLevel.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            Log.d(_logTag, "Unknown txPowerLevel '${inputData[KEY_TX_POWER_LEVEL]}', defaulting to TX_POWER_HIGH")
            null
        } ?: TxPowerLevel.TX_POWER_HIGH

        val advertisementSet = AdvertisementSet()
        advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED
        advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED
        advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN

        // Advertise Settings
        advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
        advertisementSet.advertiseSettings.txPowerLevel = txPowerLevel
        advertisementSet.advertiseSettings.connectable = false
        advertisementSet.advertiseSettings.timeout = 0

        // Advertising Parameters (legacy mode)
        advertisementSet.advertisingSetParameters.legacyMode = true
        advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
        advertisementSet.advertisingSetParameters.txPowerLevel = txPowerLevel

        // AdvertiseData: manufacturer-specific data only, no device name or TX power
        advertisementSet.advertiseData.includeDeviceName = false
        advertisementSet.advertiseData.includeTxPower = false

        val manufacturerSpecificData = ManufacturerSpecificData()
        manufacturerSpecificData.manufacturerId = manufacturerId
        manufacturerSpecificData.manufacturerSpecificData = payload
        advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)

        // General Data
        advertisementSet.title = title

        // Callbacks
        advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
        advertisementSet.advertisingCallback = GenericAdvertisingCallback()

        return listOf(advertisementSet)
    }
}
