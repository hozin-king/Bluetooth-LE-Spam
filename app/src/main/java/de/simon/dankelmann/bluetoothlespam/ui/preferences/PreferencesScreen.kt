package de.simon.dankelmann.bluetoothlespam.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Enums.toStringId
import de.simon.dankelmann.bluetoothlespam.Helpers.AdvertisementTuningHelper
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModeOption
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeModePicker
import de.simon.dankelmann.bluetoothlespam.ui.theme.ThemeSwatchPicker

/**
 * Hand-rolled replacement for `PreferenceFragmentCompat` (plan §5/§11 — no Compose equivalent
 * of the classic Preference widget system exists, so every row is a plain composable).
 *
 * Theme mode/seed color/dynamic-color/blur are DataStore-backed (plan §11); use-legacy-advertising,
 * advertising interval, and logging stay on their existing stores (SharedPreferences /
 * `LogFileManager`'s own store respectively) — see [de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository]'s
 * doc for why those weren't folded into DataStore too.
 */
@Composable
fun PreferencesScreen(
    themeMode: ThemeModeOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    seedColorArgb: Int,
    onSeedColorSelected: (Int) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorEnabledChanged: (Boolean) -> Unit,
    blurEnabled: Boolean,
    onBlurEnabledChanged: (Boolean) -> Unit,
    allowCustomSwiftPairNames: Boolean,
    onAllowCustomSwiftPairNamesChanged: (Boolean) -> Unit,
    useLegacyAdvertising: Boolean,
    onUseLegacyAdvertisingChanged: (Boolean) -> Unit,
    advertisingIntervalMs: String,
    onAdvertisingIntervalChanged: (String) -> Unit,
    onTxPowerClicked: () -> Unit,
    txPowerLevel: TxPowerLevel,
    onTxPowerLevelSelected: (TxPowerLevel) -> Unit,
    txInterval: Int,
    onTxIntervalSelected: (Int) -> Unit,
    txPerTarget: Map<AdvertisementTarget, TxPowerLevel>,
    onTxPerTargetSelected: (AdvertisementTarget, TxPowerLevel?) -> Unit,
    rangeTestEnabled: Boolean,
    onRangeTestEnabledChanged: (Boolean) -> Unit,
    onOpenRangeTestClicked: () -> Unit,
    spamDetectionBackgroundEnabled: Boolean,
    onSpamDetectionBackgroundEnabledChanged: (Boolean) -> Unit,
    spamDetectionAlertEnabled: Boolean,
    onSpamDetectionAlertEnabledChanged: (Boolean) -> Unit,
    loggingEnabled: Boolean,
    onLoggingEnabledChanged: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        // Top/bottom padding include SpecterTopAppBarClearance/FloatingNavBarClearance so the
        // first/last card scrolls clear of the floating title bar and nav pill instead of
        // staying stuck underneath either one.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + SpecterTopAppBarClearance,
            bottom = 16.dp + FloatingNavBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader("Appearance") }
        item {
            // Both pickers are now bordered pills (ThemeSwatchPicker matches ThemeModePicker's
            // own 28.dp/outlineVariant treatment) -- 8dp read as too tight once there were two
            // visible borders stacked instead of one, so the gap between them is a bit wider.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeModePicker(selected = themeMode, onSelected = onThemeModeSelected)
                ThemeSwatchPicker(selectedSeedColor = seedColorArgb, onSeedColorSelected = onSeedColorSelected)
            }
        }
        item {
            SwitchRow(
                title = "Dynamic Color",
                summary = "Follow Material You colors derived from your wallpaper or picked swatch",
                checked = dynamicColorEnabled,
                onCheckedChange = onDynamicColorEnabledChanged,
                icon = rememberVectorPainter(Icons.Filled.Palette),
            )
        }
        item {
            SwitchRow(
                title = "Blur Effects",
                summary = "Frosted-glass backdrop behind the floating navigation bar",
                checked = blurEnabled,
                onCheckedChange = onBlurEnabledChanged,
                icon = rememberVectorPainter(Icons.Filled.BlurOn),
            )
        }

        item { SectionHeader("Advertising Settings") }
        item {
            SwitchRow(
                title = "Use Legacy Advertising",
                summary = "Recommended for maximum device compatibility",
                checked = useLegacyAdvertising,
                onCheckedChange = onUseLegacyAdvertisingChanged,
                icon = painterResource(R.drawable.ic_settings_bluetooth),
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_duration),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    OutlinedTextField(
                        value = advertisingIntervalMs,
                        // Number keyboard is a soft-keyboard hint only -- it doesn't block a
                        // hardware keyboard or paste, so digits are filtered explicitly here
                        // (equivalent to the old EditTextPreference's android:digits filter).
                        onValueChange = { input -> onAdvertisingIntervalChanged(input.filter(Char::isDigit)) },
                        label = { Text("Advertisement Duration (ms)") },
                        supportingText = { Text("Recommended: 1000 - 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                    )
                }
            }
        }
        item {
            ClickRow(title = "Set TX Power", onClick = onTxPowerClicked, iconRes = R.drawable.ic_tx_power)
        }

        item { SectionHeader(stringResource(R.string.tx_section_title)) }
        item {
            // Global TX power — persisted to TX_TUNING_POWER and applied at the advertisement
            // choke point to every advertisement set (per-target overrides win over this).
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TxPowerDropdown(
                        label = stringResource(R.string.tx_power_title),
                        summary = stringResource(R.string.tx_power_summary),
                        selected = txPowerLevel,
                        allowDefault = false,
                        onSelected = { level -> level?.let(onTxPowerLevelSelected) },
                    )
                }
            }
        }
        item {
            // Advertising interval — discrete presets mapped onto INTERVAL_MIN..INTERVAL_MAX.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = stringResource(R.string.tx_interval_title), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.tx_interval_summary), style = MaterialTheme.typography.bodySmall)
                    IntervalSliderRow(interval = txInterval, onIntervalSelected = onTxIntervalSelected)
                }
            }
        }
        item {
            // One row per target; "Default" removes the override so the global level applies.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.tx_per_target_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.tx_per_target_summary),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    AdvertisementTarget.entries.forEach { target ->
                        TxPowerDropdown(
                            label = targetDisplayName(target),
                            summary = null,
                            selected = txPerTarget[target],
                            allowDefault = true,
                            onSelected = { level -> onTxPerTargetSelected(target, level) },
                        )
                    }
                }
            }
        }
        item {
            SwitchRow(
                title = stringResource(R.string.tx_range_test_title),
                summary = stringResource(R.string.tx_range_test_summary),
                checked = rangeTestEnabled,
                onCheckedChange = onRangeTestEnabledChanged,
                icon = painterResource(R.drawable.ic_settings_bluetooth),
            )
        }
        item {
            ClickRow(title = stringResource(R.string.tx_open_range_test), onClick = onOpenRangeTestClicked, iconRes = R.drawable.ic_tx_power)
        }

        item { SectionHeader("Swift Pair") }
        item {
            SwitchRow(
                title = "Allow Custom Swift Pair Names",
                summary = "Shows an edit button on each Swift Pair entry to set the advertised device name",
                checked = allowCustomSwiftPairNames,
                onCheckedChange = onAllowCustomSwiftPairNamesChanged,
            )
        }

        item { SectionHeader("Spam Detector Settings") }
        item {
            SwitchRow(
                title = "Detect Spam in Background",
                summary = "Keep spam detection running once the app leaves the foreground — requires background location access",
                checked = spamDetectionBackgroundEnabled,
                onCheckedChange = onSpamDetectionBackgroundEnabledChanged,
                icon = painterResource(R.drawable.ic_location_searching),
            )
        }
        item {
            SwitchRow(
                title = stringResource(R.string.sd_pref_title),
                summary = stringResource(R.string.sd_pref_summary),
                checked = spamDetectionAlertEnabled,
                onCheckedChange = onSpamDetectionAlertEnabledChanged,
                icon = painterResource(R.drawable.bluetooth),
            )
        }

        item { SectionHeader("Debug Settings") }
        item {
            SwitchRow(
                title = "Enable Logging",
                summary = "Write advertisement/scan activity to a log file",
                checked = loggingEnabled,
                onCheckedChange = onLoggingEnabledChanged,
                icon = painterResource(R.drawable.ic_settings_logging),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Painter? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    // padding BEFORE size: chaining size().padding() would fix the outer box at
                    // 28dp and then let padding eat into that same box from the inside, shrinking
                    // the rendered icon to ~12dp -- this order applies the gap outside the icon.
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = summary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ClickRow(title: String, onClick: () -> Unit, iconRes: Int? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    // padding BEFORE size: chaining size().padding() would fix the outer box at
                    // 28dp and then let padding eat into that same box from the inside, shrinking
                    // the rendered icon to ~12dp -- this order applies the gap outside the icon.
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(28.dp),
                )
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Dropdown for a TX power level. With [allowDefault], "Default (follow global)" maps to null. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxPowerDropdown(
    label: String,
    summary: String?,
    selected: TxPowerLevel?,
    allowDefault: Boolean,
    onSelected: (TxPowerLevel?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        if (summary != null) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(text = summary, style = MaterialTheme.typography.bodySmall)
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = if (selected == null) stringResource(R.string.tx_default)
                else stringResource(selected.toStringId()),
                onValueChange = {},
                readOnly = true,
                label = if (summary == null) {
                    { Text(label) }
                } else {
                    null
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                if (allowDefault) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tx_default)) },
                        onClick = { onSelected(null); expanded = false },
                    )
                }
                TxPowerLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(stringResource(level.toStringId())) },
                        onClick = { onSelected(level); expanded = false },
                    )
                }
            }
        }
    }
}

/** Discrete 3-step slider over the interval presets (INTERVAL_MIN..INTERVAL_MAX range). */
@Composable
private fun IntervalSliderRow(interval: Int, onIntervalSelected: (Int) -> Unit) {
    var index by remember(interval) { mutableIntStateOf(AdvertisementTuningHelper.nearestPresetIndex(interval)) }
    val presets = AdvertisementTuningHelper.intervalPresets

    Text(
        text = intervalLabel(index),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(
        value = index.toFloat(),
        valueRange = 0f..(presets.size - 1).toFloat(),
        steps = presets.size - 2,
        onValueChange = { newValue ->
            val newIndex = newValue.toInt()
            if (newIndex != index) {
                index = newIndex
                onIntervalSelected(presets[newIndex])
            }
        },
    )
}

@Composable
private fun intervalLabel(index: Int): String {
    return when (index) {
        1 -> stringResource(R.string.tx_interval_balanced)
        2 -> stringResource(R.string.tx_interval_low_power)
        else -> stringResource(R.string.tx_interval_low_latency)
    }
}

/** "ADVERTISEMENT_TARGET_KITCHEN_SINK" -> "Kitchen Sink". */
private fun targetDisplayName(target: AdvertisementTarget): String {
    return target.name
        .removePrefix("ADVERTISEMENT_TARGET_")
        .split("_")
        .joinToString(" ") { part -> part.lowercase().replaceFirstChar(Char::uppercaseChar) }
}
