package com.example.mumbailocalwo.update

import android.content.Context
import com.example.mumbailocalwo.net.NetworkGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DatabaseDownloader(context: Context) {

    private val networkGate = NetworkGate(context.applicationContext)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DEFAULT_DB_URL =
            "https://raw.githubusercontent.com/HAJ1MEI/mumbai-local-wear-os/main/timetable/mumbai-watch.db"
        private const val USER_AGENT = "MumbaiLocalWatch/1.0 (Wear OS)"
    }

    suspend fun downloadDatabase(
        destinationFile: File,
        url: String = DEFAULT_DB_URL,
        onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            networkGate.withNetwork {
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withNetwork Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }

                    val body = response.body
                        ?: return@withNetwork Result.failure(Exception("Response body is null"))

                    val totalBytes = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(destinationFile)

                    var bytesDownloaded = 0L
                    val buffer = ByteArray(8192)
                    var lastPercent = -1

                    outputStream.use { out ->
                        inputStream.use { input ->
                            while (coroutineContext.isActive) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                out.write(buffer, 0, read)
                                bytesDownloaded += read

                                if (totalBytes > 0) {
                                    val percent = ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        onProgress(percent, bytesDownloaded, totalBytes)
                                    }
                                }
                            }
                        }
                    }

                    if (!coroutineContext.isActive) {
                        destinationFile.delete()
                        return@withNetwork Result.failure(Exception("Download cancelled"))
                    }

                    onProgress(100, bytesDownloaded, bytesDownloaded)
                    Result.success(destinationFile)
                }
            }
        } catch (e: Exception) {
            destinationFile.delete()
            Result.failure(e)
        }
    }
}
