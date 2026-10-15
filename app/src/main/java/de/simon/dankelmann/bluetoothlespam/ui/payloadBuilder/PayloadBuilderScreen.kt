package de.simon.dankelmann.bluetoothlespam.ui.payloadBuilder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.CustomPayloadAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance

/**
 * Stateless form for building a custom BLE advertisement payload: name, decimal manufacturer
 * ID, raw hex payload and TX power. All validation errors are reported live as the user types.
 */
@Composable
fun PayloadBuilderScreen(
    title: String,
    onTitleChanged: (String) -> Unit,
    manufacturerId: String,
    manufacturerIdError: Boolean,
    onManufacturerIdChanged: (String) -> Unit,
    payloadHex: String,
    payloadHexError: PayloadHexError?,
    payloadByteCount: Int,
    onPayloadHexChanged: (String) -> Unit,
    txPowerLevel: TxPowerLevel,
    onTxPowerLevelChanged: (TxPowerLevel) -> Unit,
    canSubmit: Boolean,
    isSaving: Boolean,
    onAdvertiseNowClicked: () -> Unit,
    onSaveClicked: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Top clearance keeps content clear of the floating title bar (see MainActivity).
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp + SpecterTopAppBarClearance, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChanged,
                    label = { Text(stringResource(R.string.pb_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = manufacturerId,
                    onValueChange = onManufacturerIdChanged,
                    label = { Text(stringResource(R.string.pb_manufacturer_id_label)) },
                    isError = manufacturerIdError,
                    supportingText = {
                        if (manufacturerIdError) {
                            Text(stringResource(R.string.pb_manufacturer_id_error))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = payloadHex,
                    onValueChange = onPayloadHexChanged,
                    label = { Text(stringResource(R.string.pb_payload_hex_label)) },
                    isError = payloadHexError != null,
                    supportingText = {
                        when (payloadHexError) {
                            PayloadHexError.EMPTY -> Text(stringResource(R.string.pb_payload_hex_error_empty))
                            PayloadHexError.ODD_LENGTH -> Text(stringResource(R.string.pb_payload_hex_error_odd_length))
                            PayloadHexError.INVALID_CHARS -> Text(stringResource(R.string.pb_payload_hex_error_invalid_chars))
                            PayloadHexError.TOO_LONG -> Text(stringResource(R.string.pb_payload_hex_error_too_long))
                            null -> Text(
                                stringResource(
                                    R.string.pb_payload_bytes,
                                    payloadByteCount,
                                    CustomPayloadAdvertisementSetGenerator.MAX_LEGACY_PAYLOAD_BYTES,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                TxPowerDropdown(
                    selected = txPowerLevel,
                    onSelected = onTxPowerLevelChanged,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onAdvertiseNowClicked,
                        enabled = canSubmit && !isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.pb_advertise_now))
                    }
                    OutlinedButton(
                        onClick = onSaveClicked,
                        enabled = canSubmit && !isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.pb_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun TxPowerDropdown(
    selected: TxPowerLevel,
    onSelected: (TxPowerLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Read-only text field opens the menu on tap (no ExposedDropdownMenuBox in use yet).
        OutlinedTextField(
            value = stringResource(selected.toStringId()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.pb_tx_power_label)) },
            trailingIcon = { Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TxPowerLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(stringResource(level.toStringId())) },
                    onClick = {
                        onSelected(level)
                        expanded = false
                    },
                )
            }
        }
    }
}
