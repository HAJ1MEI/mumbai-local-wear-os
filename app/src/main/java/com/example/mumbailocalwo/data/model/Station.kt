package com.example.mumbailocalwo.data.model

/**
 * Represents a railway station with its associated line codes.
 */
data class Station(
    val id: Int,
    val code: String,
    val name: String,
    val displayName: String? = null,
    val marathiName: String? = null,
    val liveCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lineCodes: List<String> = emptyList()
)

/**
 * Represents a direction choice for a given station and line.
 */
data class StationDirectionOption(
    val lineCode: String,
    val lineName: String,
    val directionFlag: String,
    val label: String,
    val shortLabel: String,
    val arrow: String
)
