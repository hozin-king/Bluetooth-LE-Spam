package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * File-backed spam session log (one JSON object per line in `filesDir/spam_stats.jsonl`).
 *
 * Follows the same file-based pattern as [LogFileManager] but deliberately simple: no
 * database, no migrations — sessions are appended and rewritten on close, capped to the
 * last [MAX_LINES] lines so the file can never grow unbounded.
 */
data class SpamSession(
    val id: Long,
    val startTs: Long,
    val endTs: Long?,
    val target: String,
    val setCount: Int,
    val setType: String,
) {
    companion object
}

data class SpamAggregates(
    val totalSessions: Int,
    val totalDurationMs: Long,
    val perTarget: Map<String, Int>,
)

class SpamStatsManager private constructor(appContext: Context) {
    private val context: Context = appContext.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileLock = Any()

    private val statsFile: File
        get() = File(context.filesDir, FILE_NAME)

    /**
     * Records the start of a spam session. Never called on the main thread.
     * @return the id of the created session (a timestamp-based unique id)
     */
    suspend fun recordSessionStart(target: String, setCount: Int, setType: String): Long =
        withContext(Dispatchers.IO) {
            val id = System.currentTimeMillis()
            val session = SpamSession(id, id, null, target, setCount, setType)
            appendSession(session)
            id
        }

    /** Records the end of a spam session. Never called on the main thread. */
    suspend fun recordSessionEnd(id: Long) = withContext(Dispatchers.IO) {
        synchronized(fileLock) {
            val sessions = readSessionsLocked().map {
                if (it.id == id && it.endTs == null) it.copy(endTs = System.currentTimeMillis()) else it
            }
            rewriteSessionsLocked(sessions)
        }
    }

    /** Most recent sessions, newest first. Never called on the main thread. */
    suspend fun getRecentSessions(limit: Int = 20): List<SpamSession> = withContext(Dispatchers.IO) {
        synchronized(fileLock) {
            readSessionsLocked().sortedByDescending { it.startTs }.take(limit)
        }
    }

    /**
     * Aggregate statistics. Durations are summed from closed sessions only — a session that
     * never got an end timestamp contributes to the session count but not to the time total.
     * Never called on the main thread.
     */
    suspend fun getAggregates(): SpamAggregates = withContext(Dispatchers.IO) {
        synchronized(fileLock) {
            val sessions = readSessionsLocked()
            val closedSessions = sessions.filter { it.endTs != null }
            SpamAggregates(
                totalSessions = sessions.size,
                totalDurationMs = closedSessions.sumOf { it.endTs!! - it.startTs },
                perTarget = sessions.groupingBy { it.target }.eachCount(),
            )
        }
    }

    /** Deletes all recorded statistics. Never called on the main thread. */
    suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(fileLock) {
            try {
                if (statsFile.exists()) {
                    statsFile.delete()
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to clear stats file: ${e.message}")
            }
        }
    }

    /**
     * Fire-and-forget hook for the foreground service callbacks: starts a session and reports
     * the generated id via [onStarted]. All work runs on the manager's own IO scope, so the
     * caller stays on whatever thread it is on. Safe to call repeatedly — a previous session
     * id should be closed via [endSessionAsync] before opening the next one.
     */
    fun beginSessionAsync(target: String, setCount: Int, setType: String, onStarted: (Long) -> Unit) {
        ioScope.launch {
            try {
                onStarted(recordSessionStart(target, setCount, setType))
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to record session start: ${e.message}")
            }
        }
    }

    /** Fire-and-forget hook for the foreground service callbacks: closes the given session. */
    fun endSessionAsync(id: Long) {
        ioScope.launch {
            try {
                recordSessionEnd(id)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to record session end: ${e.message}")
            }
        }
    }

    private fun appendSession(session: SpamSession) {
        synchronized(fileLock) {
            try {
                if (!statsFile.exists()) {
                    statsFile.createNewFile()
                }
                statsFile.appendText(session.toJson().toString() + "\n")
                capFileSizeLocked()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to append session: ${e.message}")
            }
        }
    }

    private fun capFileSizeLocked() {
        try {
            val lines = statsFile.readLines()
            if (lines.size > MAX_LINES) {
                statsFile.writeText(lines.takeLast(MAX_LINES).joinToString("\n", postfix = "\n"))
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to cap stats file: ${e.message}")
        }
    }

    private fun readSessionsLocked(): List<SpamSession> {
        if (!statsFile.exists()) {
            return emptyList()
        }
        return try {
            statsFile.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        SpamSession.fromJson(JSONObject(line))
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "Skipping malformed stats line: ${e.message}")
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to read stats file: ${e.message}")
            emptyList()
        }
    }

    private fun rewriteSessionsLocked(sessions: List<SpamSession>) {
        try {
            val capped = sessions.takeLast(MAX_LINES)
            statsFile.writeText(capped.joinToString("\n", postfix = "\n") { it.toJson().toString() })
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to rewrite stats file: ${e.message}")
        }
    }

    companion object {
        private const val LOG_TAG = "SpamStatsManager"
        private const val FILE_NAME = "spam_stats.jsonl"
        private const val MAX_LINES = 500

        @Volatile
        private var instance: SpamStatsManager? = null

        fun getInstance(context: Context): SpamStatsManager {
            return instance ?: synchronized(this) {
                instance ?: SpamStatsManager(context).also { instance = it }
            }
        }
    }
}

private fun SpamSession.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("startTs", startTs)
    if (endTs != null) put("endTs", endTs) else put("endTs", JSONObject.NULL)
    put("target", target)
    put("setCount", setCount)
    put("setType", setType)
}

private fun SpamSession.Companion.fromJson(json: JSONObject): SpamSession = SpamSession(
    id = json.getLong("id"),
    startTs = json.getLong("startTs"),
    endTs = if (json.isNull("endTs")) null else json.getLong("endTs"),
    target = json.optString("target", ""),
    setCount = json.optInt("setCount", 0),
    setType = json.optString("setType", ""),
)
