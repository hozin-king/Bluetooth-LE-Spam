package de.simon.dankelmann.bluetoothlespam.ui.rangeTest

import android.bluetooth.le.AdvertisingSetParameters as FrameworkAdvertisingSetParameters
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.AdvertisementTuningHelper
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import de.simon.dankelmann.bluetoothlespam.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val LOG_TAG = "RangeTestViewModel"

/**
 * Advertises a single, clearly-labeled static beacon ("Range Test Beacon", Apple manufacturer
 * ID 76) through the normal advertisement path ([BleSpamApplication.advertisementService]), so
 * the persisted TX power / interval tuning applies exactly like it would for any generator set.
 *
 * Starting requires the "Range test mode" safety gate ([SettingsKeys.TX_TUNING_RANGE_TEST])
 * from Preferences › TX & Interval to be enabled.
 */
class RangeTestViewModel : ViewModel() {

    val txPower: StateFlow<TxPowerLevel> = MutableStateFlow(TxPowerLevel.TX_POWER_HIGH)
    val interval: StateFlow<Int> = MutableStateFlow(FrameworkAdvertisingSetParameters.INTERVAL_MIN)
    val rangeTestEnabled: StateFlow<Boolean> = MutableStateFlow(false)

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private var tickerJob: Job? = null
    private var startRealtimeMs = 0L
    private var boundApp: BleSpamApplication? = null

    private val serviceCallback = object : IAdvertisementServiceCallback {
        override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
            _isAdvertising.value = true
        }

        override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
            _isAdvertising.value = true
        }

        override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
            _statusText.value = "Failed: ${advertisementError.name}"
            stopInternal()
        }

        override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
            stopInternal()
        }
    }

    /** Binds the ViewModel to the app's persisted TX tuning; call once from the route. */
    fun observeSettings(app: BleSpamApplication) {
        boundApp = app
        viewModelScope.launch {
            SettingsRepository.getInstance(app).preferencesFlow
                .map { prefs ->
                    Triple(
                        AdvertisementTuningHelper.txPowerLevelFromName(prefs[SettingsKeys.TX_TUNING_POWER]),
                        prefs[SettingsKeys.TX_TUNING_INTERVAL]
                            ?: FrameworkAdvertisingSetParameters.INTERVAL_MIN,
                        prefs[SettingsKeys.TX_TUNING_RANGE_TEST] ?: false,
                    )
                }
                .collect { (power, intervalValue, enabled) ->
                    (txPower as MutableStateFlow).value = power
                    (this@RangeTestViewModel.interval as MutableStateFlow).value = intervalValue
                    (rangeTestEnabled as MutableStateFlow).value = enabled
                }
        }
    }

    fun start(app: BleSpamApplication) {
        if (_isAdvertising.value) return
        if (!rangeTestEnabled.value) return

        app.advertisementService.addAdvertisementServiceCallback(serviceCallback)
        val beaconSet = buildRangeTestSet(app)
        Log.d(LOG_TAG, "Starting range test beacon")
        app.advertisementService.startAdvertisement(beaconSet)
        _isAdvertising.value = true
        _statusText.value = ""
        startRealtimeMs = SystemClock.elapsedRealtime()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _elapsedSeconds.value = (SystemClock.elapsedRealtime() - startRealtimeMs) / 1000
                delay(1000)
            }
        }
    }

    fun stop(app: BleSpamApplication) {
        if (!_isAdvertising.value) return
        app.advertisementService.stopAdvertisement()
        app.advertisementService.removeAdvertisementServiceCallback(serviceCallback)
        stopInternal()
    }

    private fun stopInternal() {
        tickerJob?.cancel()
        tickerJob = null
        _isAdvertising.value = false
        _elapsedSeconds.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        boundApp?.removeAdvertisementServiceCallback(serviceCallback)
    }

    /**
     * Harmless static iBeacon-style payload: Apple company ID (0x004C = 76), type 0x02/length
     * 0x15, a fixed "RANGE-TEST-BEACON" UUID so scanners can identify it. Fits the 31-byte
     * legacy payload (29 bytes manufacturer data), so no device name / TX-power AD structures
     * are included.
     */
    private fun buildRangeTestSet(app: BleSpamApplication): AdvertisementSet {
        val set = AdvertisementSet()
        set.title = app.getString(R.string.tx_range_test_beacon_name)
        set.target = AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED

        set.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
        set.advertiseSettings.connectable = false
        set.advertiseSettings.timeout = 0

        set.advertisingSetParameters.legacyMode = true
        set.advertisingSetParameters.interval = interval.value
        set.advertisingSetParameters.txPowerLevel = txPower.value
        set.advertisingSetParameters.scanable = false
        set.advertisingSetParameters.connectable = false

        set.advertiseData.includeDeviceName = false
        set.advertiseData.includeTxPower = false

        val uuid = "RANGE-TEST-BEACON".toByteArray(Charsets.US_ASCII) // 16 bytes
        val payload = byteArrayOf(0x02, 0x15) +
            uuid +
            byteArrayOf(0x00, 0x01, 0x00, 0x01, 0xC5.toByte()) // major, minor, measured power

        val manufacturerSpecificData = ManufacturerSpecificData()
        manufacturerSpecificData.manufacturerId = 76 // 0x004c == Apple
        manufacturerSpecificData.manufacturerSpecificData = payload
        set.advertiseData.manufacturerData.add(manufacturerSpecificData)

        return set
    }
}
