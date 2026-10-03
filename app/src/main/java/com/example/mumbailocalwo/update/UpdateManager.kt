package com.example.mumbailocalwo.update

import android.content.Context
import android.util.Log
import com.example.mumbailocalwo.data.db.TimetableDatabase
import com.example.mumbailocalwo.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class UpdateManager(context: Context) {

    companion object {
        private const val TAG = "UpdateManager"

        @Volatile
        private var INSTANCE: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private val database = TimetableDatabase.getInstance(appContext)
    private val preferences = AppPreferences(appContext)
    private val metadataClient = MetadataClient(appContext)
    private val downloader = DatabaseDownloader(appContext)
    private val validator = DatabaseValidator()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    suspend fun checkForUpdatesAndApply(): Unit = withContext(Dispatchers.IO) {
        val currentVersion = database.getMetadataValue("version")
            ?: preferences.timetableVersion

        _state.value = UpdateState.Checking
        Log.i(TAG, "Starting update check. Local version: $currentVersion")

        // 1. Fetch remote metadata
        val metadataResult = metadataClient.fetchMetadata()
        if (metadataResult.isFailure) {
            val error = metadataResult.exceptionOrNull()
            Log.w(TAG, "Failed to fetch update metadata", error)
            _state.value = UpdateState.NoNetwork(currentVersion)
            return@withContext
        }

        val metadata = metadataResult.getOrThrow()
        preferences.lastUpdateCheckMillis = System.currentTimeMillis()
        Log.i(TAG, "Remote metadata: version=${metadata.version}, schema=${metadata.schemaVersion}")

        // 2. Check version
        val isNewer = isRemoteVersionNewer(metadata.version, currentVersion)
        if (!isNewer) {
            Log.i(TAG, "Timetable is already up to date ($currentVersion)")
            _state.value = UpdateState.UpToDate(currentVersion)
            return@withContext
        }

        // 3. Update found -> Start download
        _state.value = UpdateState.UpdateFound(metadata.version, metadata.databaseSizeBytes)
        val downloadFile = database.getDownloadPath()

        _state.value = UpdateState.Downloading(0, 0, metadata.databaseSizeBytes)
        val downloadResult = downloader.downloadDatabase(downloadFile) { percent, downloaded, total ->
            _state.value = UpdateState.Downloading(percent, downloaded, total)
        }

        if (downloadResult.isFailure) {
            val ex = downloadResult.exceptionOrNull()
            Log.e(TAG, "Database download failed", ex)
            database.cleanupTempFiles()
            _state.value = UpdateState.Error("Download failed: ${ex?.message ?: "Network error"}")
            return@withContext
        }

        // 4. Validate downloaded DB
        _state.value = UpdateState.Validating
        val validationResult = validator.validate(downloadFile, metadata)
        if (validationResult is DatabaseValidator.ValidationResult.Invalid) {
            Log.e(TAG, "Database validation failed: ${validationResult.reason}")
            database.cleanupTempFiles()
            _state.value = UpdateState.Error("Invalid database: ${validationResult.reason}")
            return@withContext
        }

        // 5. Atomic swap
        _state.value = UpdateState.Swapping
        val swapSuccess = database.atomicSwap()
        if (!swapSuccess) {
            Log.e(TAG, "Atomic database swap failed")
            database.cleanupTempFiles()
            _state.value = UpdateState.Error("Failed to apply new database to storage")
            return@withContext
        }

        // 6. Success!
        preferences.timetableVersion = metadata.version
        Log.i(TAG, "Timetable successfully updated to version ${metadata.version}")
        _state.value = UpdateState.Success(metadata.version)
    }

    private fun isRemoteVersionNewer(remote: String, local: String): Boolean {
        val rNum = remote.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val lNum = local.filter { it.isDigit() }.toLongOrNull() ?: 0L
        return rNum > lNum
    }

    fun resetState() {
        _state.value = UpdateState.Idle
    }
}
