package com.example.mumbailocalwo.data.model

/**
 * Result of a point-to-point journey search (trains going from station A to station B).
 */
data class SearchResult(
    val train: Train,
    val departureTime: String,
    val arrivalTime: String,
    val departureMinutes: Int,
    val arrivalMinutes: Int,
    val durationMinutes: Int,
    val fromPlatform: String?,
    val fromDoorSide: String?,
    val toPlatform: String?,
    val toDoorSide: String?,
    val intermediateStops: Int,
    val isRunningToday: Boolean = true,
    val isPast: Boolean = false,
    val isNext: Boolean = false,
    val countdownText: String? = null,
    val dayOffset: Int = 0
)

/**
 * Result of a station board query (trains departing from a station in a specific direction).
 */
data class BoardingTrain(
    val train: Train,
    val departureTime: String,
    val departureMinutes: Int,
    val platform: String?,
    val doorSide: String?,
    val destinationName: String,
    val isRunningToday: Boolean = true,
    val isPast: Boolean = false,
    val isNext: Boolean = false,
    val countdownText: String? = null,
    val dayOffset: Int = 0,
    val isStartingHere: Boolean = false
)
