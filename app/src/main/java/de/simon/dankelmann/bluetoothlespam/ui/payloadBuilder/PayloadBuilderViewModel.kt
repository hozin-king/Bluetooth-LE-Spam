package de.simon.dankelmann.bluetoothlespam.ui.payloadBuilder

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.CustomPayloadAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

/** Validation failures of the raw payload hex input. */
enum class PayloadHexError {
    EMPTY,
    ODD_LENGTH,
    INVALID_CHARS,
    TOO_LONG,
}

/** Holds the payload builder form state (LiveData style, like [AdvertisementViewModel]). */
class PayloadBuilderViewModel : ViewModel() {

    companion object {
        const val DEFAULT_TITLE = CustomPayloadAdvertisementSetGenerator.DEFAULT_TITLE
        const val DEFAULT_MANUFACTURER_ID = "76" // Apple company ID
    }

    val title = MutableLiveData(DEFAULT_TITLE)
    val manufacturerId = MutableLiveData(DEFAULT_MANUFACTURER_ID)
    val payloadHex = MutableLiveData("")
    val txPowerLevel = MutableLiveData(TxPowerLevel.TX_POWER_HIGH)

    val isSaving = MutableLiveData(false)
    val saveMessageResId = MutableLiveData<Int?>(null)

    fun parsedManufacturerId(): Int? =
        manufacturerId.value?.trim()?.toIntOrNull()?.takeIf { it in 0..65535 }

    fun isManufacturerIdValid(): Boolean = parsedManufacturerId() != null

    fun payloadHexError(): PayloadHexError? {
        val hex = payloadHex.value?.trim().orEmpty()
        return when {
            hex.isEmpty() -> PayloadHexError.EMPTY
            hex.length % 2 != 0 -> PayloadHexError.ODD_LENGTH
            !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } -> PayloadHexError.INVALID_CHARS
            hex.length / 2 > CustomPayloadAdvertisementSetGenerator.MAX_LEGACY_PAYLOAD_BYTES -> PayloadHexError.TOO_LONG
            else -> null
        }
    }

    fun payloadByteCount(): Int = payloadHex.value?.trim()?.length?.div(2) ?: 0

    fun isInputValid(): Boolean = isManufacturerIdValid() && payloadHexError() == null

    fun buildInputData(): Map<String, String> = mapOf(
        CustomPayloadAdvertisementSetGenerator.KEY_MANUFACTURER_ID to (manufacturerId.value?.trim() ?: ""),
        CustomPayloadAdvertisementSetGenerator.KEY_PAYLOAD_HEX to (payloadHex.value?.trim() ?: ""),
        CustomPayloadAdvertisementSetGenerator.KEY_TITLE to (title.value?.trim() ?: ""),
        CustomPayloadAdvertisementSetGenerator.KEY_TX_POWER_LEVEL to (txPowerLevel.value?.name ?: TxPowerLevel.TX_POWER_HIGH.name),
    )

    /** Builds the advertisement set via the generator, or null when the input is invalid. */
    fun buildAdvertisementSet(): AdvertisementSet? =
        CustomPayloadAdvertisementSetGenerator().getAdvertisementSets(buildInputData()).firstOrNull()
}
