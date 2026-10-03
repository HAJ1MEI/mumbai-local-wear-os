package com.example.mumbailocalwo.update

/**
 * State machine for the timetable OTA update process.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
    data class UpdateFound(val newVersion: String, val sizeBytes: Long) : UpdateState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateState()
    object Validating : UpdateState()
    object Swapping : UpdateState()
    data class Success(val version: String) : UpdateState()
    data class NoNetwork(val currentVersion: String) : UpdateState()
    data class Error(val message: String, val canRetry: Boolean = true) : UpdateState()
}
