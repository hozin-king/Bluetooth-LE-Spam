package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ServiceData
import java.util.UUID

/**
 * Curated Google Fast Pair device generator for Xiaomi / Redmi.
 *
 * There is no publicly documented native BLE pairing-popup protocol for Xiaomi / Redmi
 * devices, so this generator does NOT claim one. It uses Google Fast Pair -- the real,
 * documented protocol that these vendors' earbuds/headphones actually speak (service UUID
 * 0xFE2C with a 3-byte model ID), exactly like
 * [FastPairDevicesAdvertisementSetGenerator].
 *
 * The model IDs below are a curated subset of [FastPairDevicesAdvertisementSetGenerator]'s
 * genuine device IDs (source: Flipper-XFW/Xtreme-Firmware fastpair.c).
 */
class XiaomiFastPairAdvertisementSetGenerator:IAdvertisementSetGenerator{

    val xiaomiDeviceIds = mapOf(
        "DEEA86" to "Xiaomi Buds 4 Pro",
        "D90617" to "Xiaomi Redmi Buds 4 Active",
        "C8C641" to "Xiaomi Redmi Buds 4 Lite",
        "612907" to "Xiaomi Redmi Buds 4 Lite",
        "16A28E" to "Redmi Buds 6 Active",
        "171098" to "Xiaomi OpenWear Stereo Pro",
        "3A76A0" to "Redmi Buds 6 Active",
        "4B44C2" to "Xiaomi Buds 5",
        "568B56" to "Redmi Buds 5 Pro",
        "5E755B" to "Redmi Buds 5 Pro",
        "6D06EA" to "Redmi Buds 6 Play",
        "78CDD0" to "Xiaomi OpenWear Stereo Pro",
        "7F4AE2" to "Redmi Buds 6 Active",
        "83134D" to "Xiaomi Buds 5",
        "8DC8C1" to "Xiaomi Buds 5",
        "AA3674" to "Redmi Buds 5 Pro",
        "B89C85" to "Xiaomi OpenWear Stereo",
        "BF0AF0" to "Xiaomi OpenWear Stereo",
        "CB12DB" to "Redmi Buds 6 Play",
        "D8B8EA" to "Redmi Buds 6 Play",
        "E114F4" to "Xiaomi OpenWear Stereo Pro",
        "08C750" to "Redmi Buds 4 Lite",
        "095A2B" to "Redmi Buds 4 Lite",
        "0DC039" to "Redmi Buds 6 Basic",
        "0E2818" to "Redmi Buds 5A",
        "11CCFF" to "Redmi Buds 5A",
        "15EC81" to "Redmi Buds 5",
        "1F9924" to "Redmi Buds 6 Lite",
        "293789" to "Xiaomi Watch 2 Pro",
        "3175CB" to "Redmi Buds 4 Active",
        "33EB40" to "Redmi Buds 6",
        "352EE9" to "Xiaomi Watch 2",
        "41328D" to "Redmi Buds 4 Lite",
        "54ABBD" to "Redmi Buds 5",
        "56ED19" to "Redmi Buds 5",
        "56F1A1" to "Redmi Buds 6 Basic",
        "583F90" to "Redmi Buds 6 Basic",
        "67678C" to "Redmi Buds 4 Active",
        "695519" to "Redmi Buds 6 Lite",
        "6A8895" to "Redmi Buds 5C",
        "71C275" to "Redmi Buds 5A",
        "730D44" to "Xiaomi Buds 4 Pro",
        "73D9D4" to "Xiaomi Watch 2 Pro",
        "76E444" to "Redmi Buds 5C",
        "7AA4B1" to "Redmi Buds 4 Lite",
        "7E4021" to "Redmi Buds 6 Basic",
        "826379" to "Redmi Buds 4 Lite",
        "92BCEB" to "Redmi Buds 6",
        "92E10C" to "Redmi Buds 6 Lite",
        "94A9A3" to "Xiaomi Buds 5",
        "9F33D5" to "Redmi Buds 5",
        "A3B280" to "Redmi Buds 5",
        "A45154" to "Redmi Buds 6 Pro",
        "A4D4F5" to "Redmi Buds 6 Pro",
        "AFB46B" to "Xiaomi Watch 2 Pro",
        "B37A5C" to "Xiaomi Buds 3 Star Wars",
        "B51C35" to "Xiaomi Watch 2 Pro",
        "B69E7B" to "Redmi Buds 6 Pro",
        "BD8E97" to "Redmi Buds 6 Lite",
        "BDA989" to "Xiaomi Watch 2",
        "C336A1" to "Redmi Buds 6",
        "CDFA4D" to "Xiaomi Watch 2",
        "D1040B" to "Redmi Buds 6 Active",
        "E37FE1" to "Redmi Buds 4 Active",
        "E922EB" to "Redmi Buds 5",
        "EB1F1F" to "Redmi Buds 5A",
        "FB9B72" to "Redmi Buds 6 Play",
        "FF9A44" to "Redmi Buds 5C",
    )

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: xiaomiDeviceIds

        data.map {

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_XIAOMI
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_XIAOMI
            advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE

            // Advertise Settings
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0

            // Advertising Parameters
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
            advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.primaryPhy = PrimaryPhy.PHY_LE_1M
            advertisementSet.advertisingSetParameters.secondaryPhy = SecondaryPhy.PHY_LE_1M

            // AdvertiseData
            advertisementSet.advertiseData.includeDeviceName = false

            val serviceData = ServiceData()
            serviceData.serviceUuid = serviceUuid
            serviceData.serviceData = StringHelpers.decodeHex(it.key)
            advertisementSet.advertiseData.services.add(serviceData)
            advertisementSet.advertiseData.includeTxPower = true

            // Scan Response
            //advertisementSet.scanResponse.includeTxPower = true

            // General Data
            advertisementSet.title = it.value

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GenericAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }
}
