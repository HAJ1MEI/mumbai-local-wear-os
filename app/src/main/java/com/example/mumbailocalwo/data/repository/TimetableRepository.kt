package com.example.mumbailocalwo.data.repository

import android.content.Context
import com.example.mumbailocalwo.data.db.StationDao
import com.example.mumbailocalwo.data.db.TimetableDatabase
import com.example.mumbailocalwo.data.db.TrainDao
import com.example.mumbailocalwo.data.model.BoardingTrain
import com.example.mumbailocalwo.data.model.SearchResult
import com.example.mumbailocalwo.data.model.Station
import com.example.mumbailocalwo.data.model.StationDirectionOption
import com.example.mumbailocalwo.data.model.StopEvent
import com.example.mumbailocalwo.data.model.Train
import com.example.mumbailocalwo.data.prefs.AppPreferences
import com.example.mumbailocalwo.live.LiveClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main repository for timetable data queries, live tracking, and user preferences.
 * All DB operations are suspend functions running on Dispatchers.IO.
 */
class TimetableRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: TimetableRepository? = null

        fun getInstance(context: Context): TimetableRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimetableRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val database = TimetableDatabase.getInstance(context)
    private val stationDao = StationDao { database.getReadableDatabase() }
    private val trainDao = TrainDao { database.getReadableDatabase() }

    val preferences = AppPreferences(context)
    val liveClient = LiveClient(context)

    // ---- Station queries ----

    suspend fun getAllStations(): List<Station> = withContext(Dispatchers.IO) {
        stationDao.getAllStations()
    }

    suspend fun searchStations(query: String): List<Station> = withContext(Dispatchers.IO) {
        if (query.isBlank()) stationDao.getAllStations()
        else stationDao.searchStations(query)
    }

    suspend fun getStationById(id: Int): Station? = withContext(Dispatchers.IO) {
        stationDao.getStationById(id)
    }

    suspend fun getStationsByIds(ids: List<Int>): List<Station> = withContext(Dispatchers.IO) {
        stationDao.getStationsByIds(ids)
    }

    suspend fun getStationDirections(stationId: Int): List<StationDirectionOption> =
        withContext(Dispatchers.IO) {
            stationDao.getStationDirections(stationId)
        }

    suspend fun hasDirectTrains(fromStationId: Int, toStationId: Int): Boolean =
        withContext(Dispatchers.IO) {
            stationDao.hasDirectTrains(fromStationId, toStationId)
        }

    suspend fun findInterchangeStation(fromStationId: Int, toStationId: Int): Station? =
        withContext(Dispatchers.IO) {
            stationDao.findInterchangeStation(fromStationId, toStationId)
        }

    // ---- Train queries ----

    suspend fun findDirectTrains(
        fromStationId: Int,
        toStationId: Int
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        trainDao.findDirectTrains(fromStationId, toStationId)
    }

    suspend fun getBoardingTrains(
        stationId: Int,
        lineCode: String,
        direction: String
    ): List<BoardingTrain> = withContext(Dispatchers.IO) {
        trainDao.getBoardingTrains(stationId, lineCode, direction)
    }

    suspend fun getTrainById(trainId: Int): Train? = withContext(Dispatchers.IO) {
        trainDao.getTrainById(trainId)
    }

    suspend fun getTrainStops(trainId: Int): List<StopEvent> = withContext(Dispatchers.IO) {
        trainDao.getTrainStops(trainId)
    }

    // ---- Metadata ----

    suspend fun getTimetableMetadata(): Map<String, String> = withContext(Dispatchers.IO) {
        trainDao.getTimetableMetadata()
    }

    suspend fun getTrainCount(): Int = withContext(Dispatchers.IO) {
        getTimetableMetadata()["trainCount"]?.toIntOrNull() ?: 0
    }

    suspend fun getStationCount(): Int = withContext(Dispatchers.IO) {
        getTimetableMetadata()["stationCount"]?.toIntOrNull() ?: 0
    }

    suspend fun getTimetableVersion(): String = withContext(Dispatchers.IO) {
        database.getMetadataValue("version") ?: "20260928"
    }

    fun getDatabaseInstance(): TimetableDatabase = database
}
