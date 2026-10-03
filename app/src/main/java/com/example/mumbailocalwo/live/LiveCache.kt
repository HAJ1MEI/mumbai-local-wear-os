package com.example.mumbailocalwo.live

import com.example.mumbailocalwo.live.model.LiveStatus
import com.example.mumbailocalwo.live.model.SingleTrainLiveInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory process-level cache for live train tracking statuses.
 * Retains responses with timestamps and provides fast lookup for details screens.
 */
object LiveCache {

    private val bulkCache = ConcurrentHashMap<String, BulkSnapshot>()
    private val singleTrainCache = ConcurrentHashMap<String, SingleTrainLiveInfo>()
    private val trainToStatusIndex = ConcurrentHashMap<String, LiveStatus>()

    data class BulkSnapshot(
        val key: String,
        val statuses: Map<String, LiveStatus>,
        val fetchedAtMillis: Long = System.currentTimeMillis()
    )

    fun saveBulk(lineCode: String, stationCode: String, statuses: Map<String, LiveStatus>) {
        val key = "${lineCode.uppercase()}:${stationCode.uppercase()}"
        bulkCache[key] = BulkSnapshot(key, statuses)
        // Also index each individual train number for instant lookup on details screen
        for ((trainNumber, status) in statuses) {
            trainToStatusIndex[trainNumber] = status
        }
    }

    fun getBulk(lineCode: String, stationCode: String): BulkSnapshot? {
        val key = "${lineCode.uppercase()}:${stationCode.uppercase()}"
        return bulkCache[key]
    }

    fun getStatusForTrain(trainNumber: String): LiveStatus? {
        return trainToStatusIndex[trainNumber]
    }

    fun saveSingleTrainInfo(info: SingleTrainLiveInfo) {
        singleTrainCache[info.trainNumber] = info
    }

    fun getSingleTrainInfo(trainNumber: String): SingleTrainLiveInfo? {
        return singleTrainCache[trainNumber]
    }

    fun clear() {
        bulkCache.clear()
        singleTrainCache.clear()
        trainToStatusIndex.clear()
    }
}
