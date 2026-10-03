package com.example.mumbailocalwo.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Manages the timetable SQLite database lifecycle.
 *
 * On first launch, copies the bundled initial database from assets.
 * Supports atomic replacement for OTA updates.
 */
class TimetableDatabase private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TimetableDatabase"
        private const val DB_DIR = "timetable"
        private const val DB_NAME = "mumbai.db"
        private const val DB_DOWNLOAD = "mumbai.db.download"
        private const val DB_OLD = "mumbai.db.old"
        private const val ASSET_PATH = "initial/mumbai-watch.db"

        @Volatile
        private var INSTANCE: TimetableDatabase? = null

        fun getInstance(context: Context): TimetableDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimetableDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private var database: SQLiteDatabase? = null
    private val dbDir: File = File(context.filesDir, DB_DIR)
    private val dbFile: File = File(dbDir, DB_NAME)

    init {
        dbDir.mkdirs()
        ensureDatabase()
    }

    fun databaseFileExists(): Boolean = dbFile.exists() && dbFile.length() > 0L

    /**
     * Ensures a valid database exists with all required tables. Copies from assets if needed.
     */
    private fun ensureDatabase() {
        if (!dbFile.exists() || dbFile.length() == 0L || !isSchemaValid(dbFile)) {
            Log.i(TAG, "No valid or current DB found, copying from assets...")
            copyFromAssets()
        }
        openDatabase()
    }

    private fun isSchemaValid(file: File): Boolean {
        return try {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            ).use { tempDb ->
                tempDb.rawQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('station_lines', 'line_directions', 'trains')",
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getInt(0) == 3
                    } else false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Copies the bundled initial database from APK assets to internal storage atomically.
     */
    private fun copyFromAssets() {
        val tmpFile = File(dbDir, "mumbai.db.tmp")
        try {
            context.assets.open(ASSET_PATH).use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            if (dbFile.exists()) {
                dbFile.delete()
            }
            tmpFile.renameTo(dbFile)
            Log.i(TAG, "Initial DB copied: ${dbFile.length()} bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy initial DB from assets", e)
            tmpFile.delete()
        }
    }

    /**
     * Opens the database for reading.
     */
    private fun openDatabase() {
        try {
            database?.close()
            database = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
            Log.i(TAG, "Database opened: ${dbFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open database", e)
        }
    }

    /**
     * Returns the open database for queries.
     */
    fun getReadableDatabase(): SQLiteDatabase? {
        if (database == null || !database!!.isOpen) {
            openDatabase()
        }
        return database
    }

    /**
     * Returns the path for downloading a new database file.
     */
    fun getDownloadPath(): File = File(dbDir, DB_DOWNLOAD)

    /**
     * Atomically replaces the current database with a validated download.
     *
     * Steps:
     * 1. Close current DB
     * 2. Rename current → .old
     * 3. Rename download → current
     * 4. Open new DB
     * 5. Delete .old
     */
    fun atomicSwap(): Boolean {
        val downloadFile = File(dbDir, DB_DOWNLOAD)
        val oldFile = File(dbDir, DB_OLD)

        if (!downloadFile.exists()) {
            Log.e(TAG, "Download file does not exist")
            return false
        }

        try {
            // Close current connection
            database?.close()
            database = null

            // Rename current → old
            if (dbFile.exists()) {
                if (!dbFile.renameTo(oldFile)) {
                    Log.e(TAG, "Failed to rename current DB to old")
                    openDatabase() // Re-open existing
                    return false
                }
            }

            // Rename download → current
            if (!downloadFile.renameTo(dbFile)) {
                Log.e(TAG, "Failed to rename download to current")
                // Try to restore old
                if (oldFile.exists()) {
                    oldFile.renameTo(dbFile)
                }
                openDatabase()
                return false
            }

            // Open new DB
            openDatabase()

            // Clean up old DB
            if (oldFile.exists()) {
                oldFile.delete()
            }

            Log.i(TAG, "Atomic swap completed successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Atomic swap failed", e)
            // Try recovery
            if (!dbFile.exists() && oldFile.exists()) {
                oldFile.renameTo(dbFile)
            }
            openDatabase()
            return false
        }
    }

    /**
     * Cleans up stale temporary files.
     */
    fun cleanupTempFiles() {
        File(dbDir, DB_DOWNLOAD).takeIf { it.exists() }?.delete()
        File(dbDir, DB_OLD).takeIf { it.exists() }?.delete()
    }

    /**
     * Returns the current database file size in bytes.
     */
    fun getDatabaseSize(): Long = if (dbFile.exists()) dbFile.length() else 0L

    /**
     * Returns a metadata value from the metadata table.
     */
    fun getMetadataValue(key: String): String? {
        return try {
            getReadableDatabase()?.rawQuery(
                "SELECT value FROM metadata WHERE key = ?",
                arrayOf(key)
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata key: $key", e)
            null
        }
    }

    fun close() {
        database?.close()
        database = null
    }
}
