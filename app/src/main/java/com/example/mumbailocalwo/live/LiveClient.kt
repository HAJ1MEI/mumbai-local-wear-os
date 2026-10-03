package com.example.mumbailocalwo.live

import android.content.Context
import com.example.mumbailocalwo.live.model.LiveStatus
import com.example.mumbailocalwo.live.model.SingleTrainLiveInfo
import com.example.mumbailocalwo.net.NetworkGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LiveClient(context: Context) {

    private val networkGate = NetworkGate(context.applicationContext)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        const val LIVE_BASE_URL = "https://livetrain.mobond.com/mtracker/getalllivetrains"
        const val LIVE_TRAIN_URL = "https://livetrain.mobond.com/mtracker/getlivetraininfo"

        val LIVE_SUPPORTED_LINES = setOf("C", "W", "H")

        private const val USER_AGENT =
            "Dalvik/2.1.0 (Linux; U; Android 14; Pixel Build/UP1A.231005.007)"
    }

    fun isLineSupported(lineCode: String): Boolean {
        return LIVE_SUPPORTED_LINES.contains(lineCode.uppercase())
    }

    suspend fun fetchAllLiveTrains(
        lineCode: String,
        stationCode: String
    ): Result<Map<String, LiveStatus>> = withContext(Dispatchers.IO) {
        val cleanLine = lineCode.uppercase()
        val cleanStation = stationCode.uppercase().trim()

        if (!isLineSupported(cleanLine)) {
            return@withContext Result.failure(IllegalArgumentException("Line $cleanLine does not support live tracking"))
        }

        try {
            val url = "$LIVE_BASE_URL?l=$cleanLine&s=$cleanStation"
            android.util.Log.d("LiveClient", "Fetching live trains: $url")

            networkGate.withNetwork {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    android.util.Log.d("LiveClient", "Response code: ${response.code} for $url")
                    if (!response.isSuccessful) {
                        android.util.Log.e("LiveClient", "HTTP error ${response.code}: ${response.message}")
                        return@withNetwork Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val bodyString = decodeBody(response)
                    if (bodyString.isBlank()) {
                        return@withNetwork Result.failure(Exception("Empty body"))
                    }

                    val json = JSONObject(bodyString)
                    val result = mutableMapOf<String, LiveStatus>()
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val trainNumber = keys.next()
                        val rawStatus = json.optString(trainNumber)
                        if (rawStatus.isNotBlank()) {
                            result[trainNumber] = LiveStatusParser.parse(rawStatus)
                        }
                    }

                    // Save to in-memory cache
                    LiveCache.saveBulk(cleanLine, cleanStation, result)
                    android.util.Log.d("LiveClient", "Successfully parsed ${result.size} live trains for $cleanLine-$cleanStation")
                    Result.success(result)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveClient", "Failed to fetch live trains for $cleanLine-$cleanStation", e)
            Result.failure(e)
        }
    }

    suspend fun fetchSingleTrainInfo(trainNumber: String): Result<SingleTrainLiveInfo> =
        withContext(Dispatchers.IO) {
            try {
                networkGate.withNetwork {
                    val url = "$LIVE_TRAIN_URL?tn=$trainNumber"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .get()
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withNetwork Result.failure(Exception("HTTP ${response.code}"))
                        }
                        val bodyString = decodeBody(response)
                        if (bodyString.isBlank()) {
                            return@withNetwork Result.failure(Exception("Empty body"))
                        }

                        val json = JSONObject(bodyString)
                        val posObj = json.optJSONObject("position")
                        val message = posObj?.optString("msg")
                        val station = posObj?.optString("s")
                        val delay = if (posObj != null && posObj.has("d")) posObj.optInt("d") else null
                        val accuracy = posObj?.optBoolean("a", true) ?: true
                        val passengerCount = json.optInt("pc", 0)
                        val cancellationReports = json.optInt("cc", 0)
                        val isMoving = json.optBoolean("mv", false)
                        val lastUpdate = json.optLong("t", System.currentTimeMillis())

                        val info = SingleTrainLiveInfo(
                            trainNumber = trainNumber,
                            message = message,
                            station = station,
                            delayMinutes = delay,
                            isAccurate = accuracy,
                            passengerCount = passengerCount,
                            cancellationReports = cancellationReports,
                            isMoving = isMoving,
                            lastUpdateEpochMillis = lastUpdate
                        )

                        LiveCache.saveSingleTrainInfo(info)
                        Result.success(info)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun decodeBody(response: okhttp3.Response): String {
        val body = response.body ?: return ""
        val bytes = body.bytes()
        if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            return java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes))
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }
        return String(bytes, Charsets.UTF_8)
    }
}
