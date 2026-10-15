package de.simon.dankelmann.bluetoothlespam.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.simon.dankelmann.bluetoothlespam.Helpers.SpamAggregates
import de.simon.dankelmann.bluetoothlespam.Helpers.SpamSession
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.ui.theme.FloatingNavBarClearance
import de.simon.dankelmann.bluetoothlespam.ui.theme.SpecterTopAppBarClearance
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Spam session statistics screen — cards for the summary, per-target breakdown and history. */
@Composable
fun StatsScreen(
    aggregates: SpamAggregates?,
    recentSessions: List<SpamSession>,
    isLoading: Boolean,
    onClearClicked: () -> Unit,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Top/bottom padding include SpecterTopAppBarClearance/FloatingNavBarClearance so the
        // first/last card scrolls clear of the floating title bar and nav pill instead of
        // staying stuck underneath either one.
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp + SpecterTopAppBarClearance,
            bottom = 16.dp + FloatingNavBarClearance,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SummaryCard(
                totalSessions = aggregates?.totalSessions ?: 0,
                totalDurationMs = aggregates?.totalDurationMs ?: 0L,
            )
        }

        item {
            PerTargetCard(perTarget = aggregates?.perTarget ?: emptyMap())
        }

        item {
            SectionHeader(title = stringResource(R.string.stats_recent_title), count = recentSessions.size)
        }

        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (recentSessions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_no_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(recentSessions, key = { it.id }) { session ->
                SessionRow(session = session)
            }
        }

        item {
            Button(
                onClick = { showClearConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && (aggregates?.totalSessions ?: 0) > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(text = stringResource(R.string.stats_clear))
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(text = stringResource(R.string.stats_clear_title)) },
            text = { Text(text = stringResource(R.string.stats_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearClicked()
                    },
                ) {
                    Text(text = stringResource(R.string.stats_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(text = stringResource(R.string.stats_cancel))
                }
            },
        )
    }
}

@Composable
private fun SummaryCard(totalSessions: Int, totalDurationMs: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_summary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            StatRow(
                label = stringResource(R.string.stats_total_sessions),
                value = totalSessions.toString(),
            )
            StatRow(
                label = stringResource(R.string.stats_total_spam_time),
                value = formatDuration(totalDurationMs),
            )
        }
    }
}

@Composable
private fun PerTargetCard(perTarget: Map<String, Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_per_target_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (perTarget.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                perTarget.entries
                    .sortedByDescending { it.value }
                    .forEach { (target, count) ->
                        StatRow(label = target.prettyName(), value = count.toString())
                    }
            }
        }
    }
}

@Composable
private fun SessionRow(session: SpamSession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = session.target.prettyName(),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = session.sessionDuration(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = session.sessionStartText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Turns e.g. ADVERTISEMENT_TARGET_ANDROID into a readable label. */
private fun String.prettyName(): String =
    removePrefix("ADVERTISEMENT_TARGET_")
        .removePrefix("ADVERTISEMENT_TYPE_")
        .lowercase(Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

@Composable
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}${stringResource(R.string.stats_hours_short)} " +
            "${minutes}${stringResource(R.string.stats_minutes_short)} " +
            "${seconds}${stringResource(R.string.stats_seconds_short)}"
        minutes > 0 -> "${minutes}${stringResource(R.string.stats_minutes_short)} " +
            "${seconds}${stringResource(R.string.stats_seconds_short)}"
        else -> "${seconds}${stringResource(R.string.stats_seconds_short)}"
    }.trim()
}

@Composable
private fun SpamSession.sessionDuration(): String {
    return if (endTs != null) {
        formatDuration(endTs - startTs)
    } else {
        stringResource(R.string.stats_open_session)
    }
}

private fun SpamSession.sessionStartText(): String {
    val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
    return formatter.format(Date(startTs))
}
