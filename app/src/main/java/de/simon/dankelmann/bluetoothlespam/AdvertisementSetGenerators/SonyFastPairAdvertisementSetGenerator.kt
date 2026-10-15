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
 * Curated Google Fast Pair device generator for Sony.
 *
 * There is no publicly documented native BLE pairing-popup protocol for Sony
 * devices, so this generator does NOT claim one. It uses Google Fast Pair -- the real,
 * documented protocol that these vendors' earbuds/headphones actually speak (service UUID
 * 0xFE2C with a 3-byte model ID), exactly like
 * [FastPairDevicesAdvertisementSetGenerator].
 *
 * The model IDs below are a curated subset of [FastPairDevicesAdvertisementSetGenerator]'s
 * genuine device IDs (source: Flipper-XFW/Xtreme-Firmware fastpair.c).
 */
class SonyFastPairAdvertisementSetGenerator:IAdvertisementSetGenerator{

    val sonyDeviceIds = mapOf(
        "00C95C" to "Sony WF-1000X",
        "01C95C" to "Sony WF-1000X",
        "5CC900" to "Sony WF-1000X",
        "5CC901" to "Sony WF-1000X",
        "5CC938" to "Sony WF-1000XM3",
        "5CC939" to "Sony WF-1000XM3",
        "5CC93A" to "Sony WF-1000XM3",
        "5CC93B" to "Sony WF-1000XM3",
        "2D7A23" to "Sony WF-1000XM4",
        "1EC95C" to "Sony WF-SP700N",
        "1FC95C" to "Sony WF-SP700N",
        "20C95C" to "Sony WF-SP700N",
        "5CC91E" to "Sony WF-SP700N",
        "5CC91F" to "Sony WF-SP700N",
        "5CC920" to "Sony WF-SP700N",
        "5CC921" to "Sony WF-SP700N",
        "5CC922" to "Sony WF-SP700N",
        "5CC923" to "Sony WF-SP700N",
        "5CC924" to "Sony WF-SP700N",
        "5CC925" to "Sony WF-SP700N",
        "5CC926" to "Sony WF-SP700N",
        "5CC927" to "Sony WF-SP700N",
        "02C95C" to "Sony WH-1000XM2",
        "03C95C" to "Sony WH-1000XM2",
        "06C95C" to "Sony WH-1000XM2",
        "07C95C" to "Sony WH-1000XM2",
        "5CC902" to "Sony WH-1000XM2",
        "5CC903" to "Sony WH-1000XM2",
        "5CC906" to "Sony WH-1000XM2",
        "5CC907" to "Sony WH-1000XM2",
        "0DC95C" to "Sony WH-1000XM3",
        "5CC90A" to "Sony WH-1000XM3",
        "5CC90B" to "Sony WH-1000XM3",
        "5CC90C" to "Sony WH-1000XM3",
        "5CC90D" to "Sony WH-1000XM3",
        "706908" to "Sony WH-1000XM3",
        "837980" to "Sony WH-1000XM3",
        "5CC932" to "Sony WH-CH700N",
        "5CC933" to "Sony WH-CH700N",
        "5CC934" to "Sony WH-CH700N",
        "5CC935" to "Sony WH-CH700N",
        "5CC936" to "Sony WH-CH700N",
        "5CC937" to "Sony WH-CH700N",
        "5CC928" to "Sony WH-H900N",
        "5CC929" to "Sony WH-H900N",
        "5CC92A" to "Sony WH-H900N",
        "5CC92B" to "Sony WH-H900N",
        "5CC92C" to "Sony WH-H900N",
        "5CC92D" to "Sony WH-H900N",
        "5CC92E" to "Sony WH-H900N",
        "5CC92F" to "Sony WH-H900N",
        "5CC930" to "Sony WH-H900N",
        "5CC931" to "Sony WH-H900N",
        "5CC93C" to "Sony WH-XB700",
        "5CC93D" to "Sony WH-XB700",
        "5CC93E" to "Sony WH-XB700",
        "5CC93F" to "Sony WH-XB700",
        "5CC940" to "Sony WH-XB900N",
        "5CC941" to "Sony WH-XB900N",
        "5CC942" to "Sony WH-XB900N",
        "5CC943" to "Sony WH-XB900N",
        "5CC944" to "Sony WH-XB900N",
        "5CC945" to "Sony WH-XB900N",
        "05C95C" to "Sony WI-1000X",
        "04C95C" to "Sony WI-1000X",
        "5CC904" to "Sony WI-1000X",
        "5CC905" to "Sony WI-1000X",
        "5CC908" to "Sony WI-1000X",
        "5CC909" to "Sony WI-1000X",
        "575836" to "Sony WI-1000X",
        "641372" to "Sony WI-1000X",
        "0EC95C" to "Sony WI-C600N",
        "5CC90E" to "Sony WI-C600N",
        "5CC90F" to "Sony WI-C600N",
        "5CC910" to "Sony WI-C600N",
        "5CC911" to "Sony WI-C600N",
        "5CC912" to "Sony WI-C600N",
        "5CC913" to "Sony WI-C600N",
        "5CC914" to "Sony WI-SP600N",
        "5CC915" to "Sony WI-SP600N",
        "5CC916" to "Sony WI-SP600N",
        "5CC917" to "Sony WI-SP600N",
        "5CC918" to "Sony WI-SP600N",
        "5CC919" to "Sony WI-SP600N",
        "5CC91A" to "Sony WI-SP600N",
        "5CC91B" to "Sony WI-SP600N",
        "5CC91C" to "Sony WI-SP600N",
        "5CC91D" to "Sony WI-SP600N",
        "D446A7" to "Sony XM5",
        "0AC95C" to "Sony WH-1000XM3",
        "0BC95C" to "Sony WH-1000XM3",
        "0CC95C" to "Sony WH-1000XM3",
        "0FC95C" to "Sony WI-C600N",
        "10C95C" to "Sony WI-C600N",
        "11C95C" to "Sony WI-C600N",
        "14C95C" to "Sony WI-SP600N",
        "15C95C" to "Sony WI-SP600N",
        "16C95C" to "Sony WI-SP600N",
        "21C95C" to "Sony WF-SP700N",
        "22C95C" to "Sony WF-SP700N",
        "23C95C" to "Sony WF-SP700N",
        "38C95C" to "Sony WF-1000XM3",
        "39C95C" to "Sony WF-1000XM3",
        "3AC95C" to "Sony WF-1000XM3",
        "08C95C" to "Sony WI-1000X",
        "09C95C" to "Sony WI-1000X",
        "12C95C" to "Sony WI-C600N",
        "13C95C" to "Sony WI-C600N",
        "17C95C" to "Sony WI-SP600N",
        "18C95C" to "Sony WI-SP600N",
        "19C95C" to "Sony WI-SP600N",
        "1AC95C" to "Sony WI-SP600N",
        "1BC95C" to "Sony WI-SP600N",
        "1CC95C" to "Sony WI-SP600N",
        "1DC95C" to "Sony WI-SP600N",
        "24C95C" to "Sony WF-SP700N",
        "25C95C" to "Sony WF-SP700N",
        "26C95C" to "Sony WF-SP700N",
        "27C95C" to "Sony WF-SP700N",
        "3BC95C" to "Sony WF-1000XM3",
        "3CC95C" to "Sony WH-XB700",
        "3DC95C" to "Sony WH-XB700",
        "3EC95C" to "Sony WH-XB700",
        "3FC95C" to "Sony WH-XB700",
        "40C95C" to "Sony WH-XB900N",
        "41C95C" to "Sony WH-XB900N",
        "42C95C" to "Sony WH-XB900N",
        "43C95C" to "Sony WH-XB900N",
        "44C95C" to "Sony WH-XB900N",
        "45C95C" to "Sony WH-XB900N",
    )

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: sonyDeviceIds

        data.map {

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_SONY
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_SONY
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
