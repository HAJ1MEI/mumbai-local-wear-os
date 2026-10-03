package com.example.mumbailocalwo.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.mumbailocalwo.data.model.BoardingTrain
import com.example.mumbailocalwo.data.model.SearchResult
import com.example.mumbailocalwo.data.model.StopEvent
import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.presentation.util.ServiceDayUtil

/**
 * Data Access Object for train and stop event queries.
 */
class TrainDao(private val db: () -> SQLiteDatabase?) {

    /**
     * Find direct trains from station A to station B for the entire service day.
     * Evaluates runsToday, upcoming status, and identifies the next train.
     */
    fun findDirectTrains(
        fromStationId: Int,
        toStationId: Int
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        val sql = """
            SELECT
              t.id, t.train_number, t.train_name, t.line, t.line_code,
              t.direction, t.source_station_id, t.dest_station_id,
              t.train_type, t.is_ac, t.is_15_car, t.is_ladies_special,
              t.operating_days, t.departure_time, t.arrival_time,
              t.departure_minutes, t.arrival_minutes, t.stops_count,
              src_st.name AS source_name, dst_st.name AS dest_name,
              se_from.minutes_from_midnight AS dep_mins,
              se_to.minutes_from_midnight AS arr_mins,
              se_from.departure AS dep_time,
              se_to.arrival AS arr_time,
              se_from.platform AS from_pf,
              se_from.door_side AS from_door,
              se_to.platform AS to_pf,
              se_to.door_side AS to_door,
              se_from.sequence AS from_seq,
              se_to.sequence AS to_seq,
              se_from.day_offset AS dep_day_offset
            FROM stop_events se_from
            JOIN stop_events se_to ON se_from.train_id = se_to.train_id
              AND se_to.sequence > se_from.sequence
            JOIN trains t ON t.id = se_from.train_id
            LEFT JOIN stations src_st ON src_st.id = t.source_station_id
            LEFT JOIN stations dst_st ON dst_st.id = t.dest_station_id
            WHERE se_from.station_id = ?
              AND se_to.station_id = ?
            ORDER BY se_from.day_offset ASC, se_from.minutes_from_midnight ASC
        """.trimIndent()

        db()?.rawQuery(sql, arrayOf(fromStationId.toString(), toStationId.toString()))?.use { cursor ->
            while (cursor.moveToNext()) {
                val train = cursorToTrain(cursor)
                val depMins = cursor.getInt(cursor.getColumnIndexOrThrow("dep_mins"))
                val arrMins = cursor.getInt(cursor.getColumnIndexOrThrow("arr_mins"))
                val dayOffset = cursor.getInt(cursor.getColumnIndexOrThrow("dep_day_offset"))
                var duration = arrMins - depMins
                if (duration < 0) duration += 1440

                val runsToday = ServiceDayUtil.runsToday(train.operatingDays)
                val isUpcoming = ServiceDayUtil.isUpcoming(depMins, dayOffset)
                val countdown = if (isUpcoming) ServiceDayUtil.formatCountdown(depMins, dayOffset) else null

                results.add(
                    SearchResult(
                        train = train,
                        departureTime = cursor.getString(cursor.getColumnIndexOrThrow("dep_time")) ?: "",
                        arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arr_time")) ?: "",
                        departureMinutes = depMins,
                        arrivalMinutes = arrMins,
                        durationMinutes = duration,
                        fromPlatform = cursor.getString(cursor.getColumnIndexOrThrow("from_pf")),
                        fromDoorSide = cursor.getString(cursor.getColumnIndexOrThrow("from_door")),
                        toPlatform = cursor.getString(cursor.getColumnIndexOrThrow("to_pf")),
                        toDoorSide = cursor.getString(cursor.getColumnIndexOrThrow("to_door")),
                        intermediateStops = cursor.getInt(cursor.getColumnIndexOrThrow("to_seq")) -
                                cursor.getInt(cursor.getColumnIndexOrThrow("from_seq")) - 1,
                        isRunningToday = runsToday,
                        isPast = !isUpcoming,
                        isNext = false,
                        countdownText = countdown,
                        dayOffset = dayOffset
                    )
                )
            }
        }

        // Mark the first upcoming train that runs today as isNext
        val nextIdx = results.indexOfFirst { !it.isPast && it.isRunningToday }
        if (nextIdx != -1) {
            results[nextIdx] = results[nextIdx].copy(isNext = true)
        }

        return results
    }

    /**
     * Get all trains stopping at a station in a specific direction on a specific line.
     * Excludes trains that terminate at this station (no onward departure).
     */
    fun getBoardingTrains(
        stationId: Int,
        lineCode: String,
        direction: String
    ): List<BoardingTrain> {
        val results = mutableListOf<BoardingTrain>()
        val sql = """
            SELECT
              t.id, t.train_number, t.train_name, t.line, t.line_code,
              t.direction, t.source_station_id, t.dest_station_id,
              t.train_type, t.is_ac, t.is_15_car, t.is_ladies_special,
              t.operating_days, t.departure_time, t.arrival_time,
              t.departure_minutes, t.arrival_minutes, t.stops_count,
              src_st.name AS source_name, dst_st.name AS dest_name,
              se.minutes_from_midnight, se.day_offset, se.departure,
              se.platform, se.door_side, se.sequence
            FROM stop_events se
            JOIN trains t ON t.id = se.train_id
            LEFT JOIN stations src_st ON src_st.id = t.source_station_id
            LEFT JOIN stations dst_st ON dst_st.id = t.dest_station_id
            WHERE se.station_id = ?
              AND t.line_code = ?
              AND t.direction = ?
              AND se.sequence < (t.stops_count - 1)
            ORDER BY se.day_offset ASC, se.minutes_from_midnight ASC
        """.trimIndent()

        db()?.rawQuery(sql, arrayOf(stationId.toString(), lineCode, direction))?.use { cursor ->
            while (cursor.moveToNext()) {
                val train = cursorToTrain(cursor)
                val depMins = cursor.getInt(cursor.getColumnIndexOrThrow("minutes_from_midnight"))
                val dayOffset = cursor.getInt(cursor.getColumnIndexOrThrow("day_offset"))
                val depTime = cursor.getString(cursor.getColumnIndexOrThrow("departure")) ?: ""
                val pf = cursor.getString(cursor.getColumnIndexOrThrow("platform"))
                val door = cursor.getString(cursor.getColumnIndexOrThrow("door_side"))
                val destName = cursor.getString(cursor.getColumnIndexOrThrow("dest_name")) ?: ""
                val seq = cursor.getInt(cursor.getColumnIndexOrThrow("sequence"))

                val runsToday = ServiceDayUtil.runsToday(train.operatingDays)
                val isUpcoming = ServiceDayUtil.isUpcoming(depMins, dayOffset)
                val countdown = if (isUpcoming) ServiceDayUtil.formatCountdown(depMins, dayOffset) else null

                results.add(
                    BoardingTrain(
                        train = train,
                        departureTime = depTime,
                        departureMinutes = depMins,
                        platform = pf,
                        doorSide = door,
                        destinationName = destName,
                        isRunningToday = runsToday,
                        isPast = !isUpcoming,
                        isNext = false,
                        countdownText = countdown,
                        dayOffset = dayOffset,
                        isStartingHere = seq == 0
                    )
                )
            }
        }

        val nextIdx = results.indexOfFirst { !it.isPast && it.isRunningToday }
        if (nextIdx != -1) {
            results[nextIdx] = results[nextIdx].copy(isNext = true)
        }

        return results
    }

    /**
     * Get a single train by ID with station names.
     */
    fun getTrainById(trainId: Int): Train? {
        return db()?.rawQuery(
            """SELECT
                 t.id, t.train_number, t.train_name, t.line, t.line_code,
                 t.direction, t.source_station_id, t.dest_station_id,
                 t.train_type, t.is_ac, t.is_15_car, t.is_ladies_special,
                 t.operating_days, t.departure_time, t.arrival_time,
                 t.departure_minutes, t.arrival_minutes, t.stops_count,
                 src.name AS source_name, dst.name AS dest_name
               FROM trains t
               LEFT JOIN stations src ON src.id = t.source_station_id
               LEFT JOIN stations dst ON dst.id = t.dest_station_id
               WHERE t.id = ?""",
            arrayOf(trainId.toString())
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursorToTrain(cursor) else null
        }
    }

    /**
     * Get stop schedule timeline for a train in sequence order.
     */
    fun getTrainStops(trainId: Int): List<StopEvent> {
        val results = mutableListOf<StopEvent>()
        val sql = """
            SELECT
              se.id, se.train_id, se.station_id, se.sequence,
              se.minutes_from_midnight, se.arrival, se.departure,
              se.platform, se.door_side,
              s.code, s.name, se.day_offset
            FROM stop_events se
            JOIN stations s ON s.id = se.station_id
            WHERE se.train_id = ?
            ORDER BY se.sequence ASC
        """.trimIndent()

        db()?.rawQuery(sql, arrayOf(trainId.toString()))?.use { cursor ->
            while (cursor.moveToNext()) {
                results.add(
                    StopEvent(
                        id = cursor.getInt(0),
                        trainId = cursor.getInt(1),
                        stationId = cursor.getInt(2),
                        sequence = cursor.getInt(3),
                        minutesFromMidnight = cursor.getInt(4),
                        arrival = if (cursor.isNull(5)) null else cursor.getString(5),
                        departure = if (cursor.isNull(6)) null else cursor.getString(6),
                        platform = if (cursor.isNull(7)) null else cursor.getString(7),
                        doorSide = if (cursor.isNull(8)) null else cursor.getString(8),
                        stationCode = cursor.getString(9),
                        stationName = cursor.getString(10)
                    )
                )
            }
        }
        return results
    }

    /**
     * Read metadata key-value table.
     */
    fun getTimetableMetadata(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        db()?.rawQuery("SELECT key, value FROM metadata", null)?.use { cursor ->
            while (cursor.moveToNext()) {
                map[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return map
    }

    private fun cursorToTrain(cursor: Cursor): Train {
        return Train(
            id = cursor.getInt(0),
            trainNumber = if (cursor.isNull(1)) null else cursor.getString(1),
            trainName = if (cursor.isNull(2)) null else cursor.getString(2),
            line = cursor.getString(3),
            lineCode = cursor.getString(4),
            direction = cursor.getString(5),
            sourceStationId = cursor.getInt(6),
            destStationId = cursor.getInt(7),
            trainType = cursor.getString(8),
            isAC = cursor.getInt(9) == 1,
            is15Car = cursor.getInt(10) == 1,
            isLadiesSpecial = cursor.getInt(11) == 1,
            operatingDays = cursor.getString(12),
            departureTime = if (cursor.isNull(13)) null else cursor.getString(13),
            arrivalTime = if (cursor.isNull(14)) null else cursor.getString(14),
            departureMinutes = if (cursor.isNull(15)) null else cursor.getInt(15),
            arrivalMinutes = if (cursor.isNull(16)) null else cursor.getInt(16),
            stopsCount = cursor.getInt(17),
            sourceStationName = if (cursor.columnCount > 18 && !cursor.isNull(18)) cursor.getString(18) else null,
            destStationName = if (cursor.columnCount > 19 && !cursor.isNull(19)) cursor.getString(19) else null
        )
    }
}
