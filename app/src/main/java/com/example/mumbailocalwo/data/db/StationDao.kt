package com.example.mumbailocalwo.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.model.StationDirectionOption

/**
 * Data Access Object for station queries and direction options.
 */
class StationDao(private val db: () -> SQLiteDatabase?) {

    /**
     * Returns all stations ordered by name, with lines aggregated.
     */
    fun getAllStations(): List<Station> {
        val results = mutableListOf<Station>()
        val sql = """
            SELECT s.id, s.code, s.name, s.display_name, s.marathi_name, s.live_code,
                   s.latitude, s.longitude, GROUP_CONCAT(sl.line_code, ',') as lines
            FROM stations s
            LEFT JOIN station_lines sl ON s.id = sl.station_id
            GROUP BY s.id
            ORDER BY s.name ASC
        """.trimIndent()

        db()?.rawQuery(sql, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursorToStation(cursor))
            }
        }
        return results
    }

    /**
     * Search stations by name, code, or Marathi name.
     */
    fun searchStations(query: String): List<Station> {
        val results = mutableListOf<Station>()
        val clean = query.trim().uppercase()
        val wildcard = "%$clean%"

        val sql = """
            SELECT s.id, s.code, s.name, s.display_name, s.marathi_name, s.live_code,
                   s.latitude, s.longitude, GROUP_CONCAT(sl.line_code, ',') as lines
            FROM stations s
            LEFT JOIN station_lines sl ON s.id = sl.station_id
            WHERE UPPER(s.name) LIKE ? OR UPPER(s.code) LIKE ? OR s.marathi_name LIKE ?
            GROUP BY s.id
            ORDER BY
                CASE WHEN UPPER(s.code) = ? THEN 0
                     WHEN UPPER(s.name) LIKE ? THEN 1
                     ELSE 2 END,
                s.name ASC
        """.trimIndent()

        db()?.rawQuery(
            sql,
            arrayOf(wildcard, wildcard, wildcard, clean, "$clean%")
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursorToStation(cursor))
            }
        }
        return results
    }

    /**
     * Get a station by its ID.
     */
    fun getStationById(id: Int): Station? {
        val sql = """
            SELECT s.id, s.code, s.name, s.display_name, s.marathi_name, s.live_code,
                   s.latitude, s.longitude, GROUP_CONCAT(sl.line_code, ',') as lines
            FROM stations s
            LEFT JOIN station_lines sl ON s.id = sl.station_id
            WHERE s.id = ?
            GROUP BY s.id
        """.trimIndent()

        return db()?.rawQuery(sql, arrayOf(id.toString()))?.use { cursor ->
            if (cursor.moveToFirst()) cursorToStation(cursor) else null
        }
    }

    /**
     * Get multiple stations by their IDs, preserving the input order.
     */
    fun getStationsByIds(ids: List<Int>): List<Station> {
        if (ids.isEmpty()) return emptyList()
        val stationMap = mutableMapOf<Int, Station>()
        val placeholders = ids.joinToString(",") { "?" }
        val sql = """
            SELECT s.id, s.code, s.name, s.display_name, s.marathi_name, s.live_code,
                   s.latitude, s.longitude, GROUP_CONCAT(sl.line_code, ',') as lines
            FROM stations s
            LEFT JOIN station_lines sl ON s.id = sl.station_id
            WHERE s.id IN ($placeholders)
            GROUP BY s.id
        """.trimIndent()

        db()?.rawQuery(sql, ids.map { it.toString() }.toTypedArray())?.use { cursor ->
            while (cursor.moveToNext()) {
                val st = cursorToStation(cursor)
                stationMap[st.id] = st
            }
        }
        return ids.mapNotNull { stationMap[it] }
    }

    /**
     * Get direction options available for a specific station (Screen 6: Direction Selector).
     */
    fun getStationDirections(stationId: Int): List<StationDirectionOption> {
        val results = mutableListOf<StationDirectionOption>()
        val sql = """
            SELECT DISTINCT t.line_code, l.name, t.direction, ld.label, ld.short_label, ld.arrow
            FROM stop_events se
            JOIN trains t ON se.train_id = t.id
            JOIN lines l ON t.line_code = l.line_code
            JOIN line_directions ld ON t.line_code = ld.line_code AND t.direction = ld.direction_flag
            WHERE se.station_id = ?
            ORDER BY t.line_code, t.direction
        """.trimIndent()

        db()?.rawQuery(sql, arrayOf(stationId.toString()))?.use { cursor ->
            while (cursor.moveToNext()) {
                results.add(
                    StationDirectionOption(
                        lineCode = cursor.getString(0),
                        lineName = cursor.getString(1),
                        directionFlag = cursor.getString(2),
                        label = cursor.getString(3),
                        shortLabel = cursor.getString(4),
                        arrow = cursor.getString(5)
                    )
                )
            }
        }
        return results
    }

    /**
     * Checks if at least one direct train exists from station A to station B.
     */
    fun hasDirectTrains(fromStationId: Int, toStationId: Int): Boolean {
        val sql = """
            SELECT 1 FROM stop_events se1
            JOIN stop_events se2 ON se1.train_id = se2.train_id
            WHERE se1.station_id = ? AND se2.station_id = ? AND se1.sequence < se2.sequence
            LIMIT 1
        """.trimIndent()

        db()?.rawQuery(sql, arrayOf(fromStationId.toString(), toStationId.toString()))?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    /**
     * Finds an interchange station (like Dadar or Kurla) connecting fromStation to toStation
     * when no direct train exists. Prefers major interchange hubs like Dadar (27) or Kurla (71).
     */
    fun findInterchangeStation(fromStationId: Int, toStationId: Int): Station? {
        val sql = """
            SELECT s.id, s.code, s.name, s.display_name, s.marathi_name, s.live_code,
                   s.latitude, s.longitude, GROUP_CONCAT(sl.line_code, ',') as lines
            FROM stations s
            JOIN station_lines sl ON s.id = sl.station_id
            WHERE EXISTS (
                SELECT 1 FROM stop_events se1
                JOIN stop_events se2 ON se1.train_id = se2.train_id
                WHERE se1.station_id = ? AND se2.station_id = s.id AND se1.sequence < se2.sequence
            )
            AND EXISTS (
                SELECT 1 FROM stop_events se3
                JOIN stop_events se4 ON se3.train_id = se4.train_id
                WHERE se3.station_id = s.id AND se4.station_id = ? AND se3.sequence < se4.sequence
            )
            GROUP BY s.id
            ORDER BY CASE WHEN s.name = 'Dadar' THEN 0 WHEN s.name = 'Kurla' THEN 1 ELSE 2 END, s.name ASC
            LIMIT 1
        """.trimIndent()

        db()?.rawQuery(sql, arrayOf(fromStationId.toString(), toStationId.toString()))?.use { cursor ->
            if (cursor.moveToFirst()) return cursorToStation(cursor)
        }
        return null
    }

    private fun cursorToStation(cursor: Cursor): Station {
        val lineStr = if (cursor.isNull(8)) "" else cursor.getString(8)
        val lineCodes = if (lineStr.isNotBlank()) lineStr.split(",").map { it.trim() }.distinct() else emptyList()

        return Station(
            id = cursor.getInt(0),
            code = cursor.getString(1),
            name = cursor.getString(2),
            displayName = if (cursor.isNull(3)) null else cursor.getString(3),
            marathiName = if (cursor.isNull(4)) null else cursor.getString(4),
            liveCode = if (cursor.isNull(5)) null else cursor.getString(5),
            latitude = if (cursor.isNull(6)) null else cursor.getDouble(6),
            longitude = if (cursor.isNull(7)) null else cursor.getDouble(7),
            lineCodes = lineCodes
        )
    }
}
