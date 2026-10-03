package com.example.mumbailocalwo.update

import android.content.Context
import com.example.mumbailocalwo.net.NetworkGate
import com.example.mumbailocalwo.update.model.UpdateMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MetadataClient(context: Context) {

    private val networkGate = NetworkGate(context.applicationContext)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DEFAULT_METADATA_URL =
            "https://raw.githubusercontent.com/HAJ1MEI/mumbai-local-wear-os/main/timetable/metadata.json"
        private const val USER_AGENT = "MumbaiLocalWatch/1.0 (Wear OS)"
    }

    suspend fun fetchMetadata(url: String = DEFAULT_METADATA_URL): Result<UpdateMetadata> =
        withContext(Dispatchers.IO) {
            try {
                networkGate.withNetwork {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .header("Cache-Control", "no-cache")
                        .get()
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withNetwork Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                        }
                        val body = response.body?.string()
                            ?: return@withNetwork Result.failure(Exception("Empty metadata response"))

                        val json = JSONObject(body)
                        val metadata = UpdateMetadata(
                            version = json.getString("version"),
                            schemaVersion = json.getInt("schemaVersion"),
                            generatedAt = if (json.has("generatedAt")) json.getString("generatedAt") else null,
                            trainCount = json.optInt("trainCount", 0),
                            stationCount = json.optInt("stationCount", 0),
                            stopEventCount = json.optInt("stopEventCount", 0),
                            databaseSizeBytes = json.optLong("databaseSizeBytes", 0L),
                            sha256 = if (json.has("sha256")) json.getString("sha256") else null
                        )
                        Result.success(metadata)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
