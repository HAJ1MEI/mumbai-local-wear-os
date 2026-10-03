package com.example.mumbailocalwo.update.model

/**
 * Metadata model matching metadata.json published by the timetable generation pipeline.
 */
data class UpdateMetadata(
    val version: String,
    val schemaVersion: Int,
    val generatedAt: String?,
    val trainCount: Int,
    val stationCount: Int,
    val stopEventCount: Int,
    val databaseSizeBytes: Long,
    val sha256: String? = null
)
