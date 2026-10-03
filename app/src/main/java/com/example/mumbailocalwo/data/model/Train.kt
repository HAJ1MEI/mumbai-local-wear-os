package com.example.mumbailocalwo.data.model

/**
 * Represents a train with its core attributes (without stop details).
 */
data class Train(
    val id: Int,
    val trainNumber: String?,
    val trainName: String?,
    val line: String,
    val lineCode: String,
    val direction: String,
    val sourceStationId: Int,
    val destStationId: Int,
    val trainType: String,
    val isAC: Boolean = false,
    val is15Car: Boolean = false,
    val isLadiesSpecial: Boolean = false,
    val operatingDays: String, // comma-separated: MON,TUE,...
    val departureTime: String?,
    val arrivalTime: String?,
    val departureMinutes: Int?,
    val arrivalMinutes: Int?,
    val stopsCount: Int = 0,
    // Joined fields (populated by queries)
    val sourceStationName: String? = null,
    val destStationName: String? = null
) {
    /** Returns display-friendly line name. */
    val lineDisplayName: String
        get() = when (line) {
            "CENTRAL" -> "Central"
            "WESTERN" -> "Western"
            "HARBOUR" -> "Harbour"
            "TRANS_HARBOUR" -> "Trans-Harbour"
            "URAN" -> "Uran"
            "DIVA_VASAI_PANVEL" -> "Diva-Vasai-Panvel"
            else -> line
        }

    /** Returns display-friendly train type. */
    val trainTypeDisplay: String
        get() = when (trainType) {
            "FAST" -> "Fast"
            "SLOW" -> "Slow"
            "SEMI_FAST" -> "Semi-Fast"
            "EXPRESS" -> "Express"
            "MEMU" -> "MEMU"
            "PASSENGER" -> "Passenger"
            "SHUTTLE" -> "Shuttle"
            else -> trainType
        }

    /** Returns the operating days as a list. */
    val operatingDaysList: List<String>
        get() = operatingDays.split(",").filter { it.isNotBlank() }

    /** Check if train runs on a specific day. */
    fun runsOn(day: String): Boolean = day.uppercase() in operatingDaysList
}
