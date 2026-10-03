package com.example.mumbailocalwo.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences, search history, and recent stations for Wear OS.
 * Stored independently of the SQLite database so timetable updates never erase user history.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mumbai_local_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_RECENT_STATIONS = "recent_stations"
        private const val KEY_LAST_FROM = "last_from_id"
        private const val KEY_LAST_TO = "last_to_id"
        private const val KEY_AUTO_LIVE = "auto_live_enabled"
        private const val KEY_TIMETABLE_VERSION = "timetable_version"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
    }

    var autoLiveEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LIVE, value).apply()

    var lastFromStationId: Long?
        get() {
            val v = prefs.getLong(KEY_LAST_FROM, -1L)
            return if (v != -1L) v else null
        }
        set(value) {
            if (value != null) {
                prefs.edit().putLong(KEY_LAST_FROM, value).apply()
            } else {
                prefs.edit().remove(KEY_LAST_FROM).apply()
            }
        }

    var lastToStationId: Long?
        get() {
            val v = prefs.getLong(KEY_LAST_TO, -1L)
            return if (v != -1L) v else null
        }
        set(value) {
            if (value != null) {
                prefs.edit().putLong(KEY_LAST_TO, value).apply()
            } else {
                prefs.edit().remove(KEY_LAST_TO).apply()
            }
        }

    var timetableVersion: String
        get() = prefs.getString(KEY_TIMETABLE_VERSION, "20260928") ?: "20260928"
        set(value) = prefs.edit().putString(KEY_TIMETABLE_VERSION, value).apply()

    var lastUpdateCheckMillis: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    fun getRecentStationIds(): List<Long> {
        val raw = prefs.getString(KEY_RECENT_STATIONS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun addRecentStation(stationId: Long) {
        val current = getRecentStationIds().toMutableList()
        current.remove(stationId)
        current.add(0, stationId)
        val capped = current.take(5)
        prefs.edit().putString(KEY_RECENT_STATIONS, capped.joinToString(",")).apply()
    }

    fun clearRecents() {
        prefs.edit()
            .remove(KEY_RECENT_STATIONS)
            .remove(KEY_LAST_FROM)
            .remove(KEY_LAST_TO)
            .apply()
    }
}
