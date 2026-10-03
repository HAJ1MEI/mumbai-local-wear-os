package com.example.mumbailocalwo.update

import android.database.sqlite.SQLiteDatabase
import com.example.mumbailocalwo.update.model.UpdateMetadata
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class DatabaseValidator {

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        private val REQUIRED_TABLES = listOf(
            "stations",
            "trains",
            "stop_events",
            "station_lines",
            "line_directions",
            "metadata"
        )
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    fun validate(file: File, metadata: UpdateMetadata? = null): ValidationResult {
        if (!file.exists() || file.length() == 0L) {
            return ValidationResult.Invalid("Downloaded database file does not exist or is empty")
        }

        // 1. Schema version check
        if (metadata != null && metadata.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
            return ValidationResult.Invalid(
                "Database schema version ${metadata.schemaVersion} is newer than supported ($SUPPORTED_SCHEMA_VERSION). Please update the app."
            )
        }

        // 2. SHA256 verification if provided
        if (metadata?.sha256 != null && metadata.sha256.isNotBlank()) {
            val calculatedSha = calculateSha256(file)
            if (!calculatedSha.equals(metadata.sha256.trim(), ignoreCase = true)) {
                return ValidationResult.Invalid(
                    "SHA256 checksum mismatch. Expected: ${metadata.sha256}, calculated: $calculatedSha"
                )
            }
        }

        // 3. Open SQLite and run PRAGMA integrity_check
        var tempDb: SQLiteDatabase? = null
        try {
            tempDb = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )

            // PRAGMA integrity_check
            tempDb.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                    val status = if (cursor.count > 0) cursor.getString(0) else "unknown"
                    return ValidationResult.Invalid("SQLite integrity check failed: $status")
                }
            }

            // 4. Required tables check
            val existingTables = mutableSetOf<String>()
            tempDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    existingTables.add(cursor.getString(0))
                }
            }

            for (table in REQUIRED_TABLES) {
                if (!existingTables.contains(table)) {
                    return ValidationResult.Invalid("Missing required table: $table")
                }
            }

            // 5. Minimum row counts check
            val stationCount = getRowCount(tempDb, "stations")
            if (stationCount < 100) {
                return ValidationResult.Invalid("Insufficient stations count: $stationCount")
            }

            val trainCount = getRowCount(tempDb, "trains")
            if (trainCount < 500) {
                return ValidationResult.Invalid("Insufficient trains count: $trainCount")
            }

            val stopEventCount = getRowCount(tempDb, "stop_events")
            if (stopEventCount < 1000) {
                return ValidationResult.Invalid("Insufficient stop_events count: $stopEventCount")
            }

            return ValidationResult.Valid
        } catch (e: Exception) {
            return ValidationResult.Invalid("Validation exception: ${e.message}")
        } finally {
            tempDb?.close()
        }
    }

    private fun getRowCount(db: SQLiteDatabase, table: String): Int {
        return try {
            db.rawQuery("SELECT count(*) FROM $table", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
