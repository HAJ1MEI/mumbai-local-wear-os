package com.example.mumbailocalwo.data.model

/**
 * Represents a single stop event in a train's schedule.
 */
data class StopEvent(
    val id: Int,
    val trainId: Int,
    val stationId: Int,
    val sequence: Int,
    val minutesFromMidnight: Int,
    val arrival: String?,   // HH:MM or null for first stop
    val departure: String?, // HH:MM or null for last stop
    val platform: String?,
    val doorSide: String?,  // L, R, B, or null
    // Joined fields
    val stationCode: String? = null,
    val stationName: String? = null
) {
    /** Display time: either departure or arrival. */
    val displayTime: String
        get() = departure ?: arrival ?: minutesToTimeString(minutesFromMidnight)

    /** Door side display text. */
    val doorSideDisplay: String?
        get() = when (doorSide) {
            "L" -> "Left"
            "R" -> "Right"
            "B" -> "Both"
            else -> null
        }

    companion object {
        fun minutesToTimeString(minutes: Int): String {
            val h = minutes / 60
            val m = minutes % 60
            return "%02d:%02d".format(h, m)
        }
    }
}
