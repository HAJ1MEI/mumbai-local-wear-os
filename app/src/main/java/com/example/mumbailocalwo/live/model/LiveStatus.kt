package com.example.mumbailocalwo.live.model

enum class LiveKind {
    AT,
    BETWEEN,
    REACHING,
    CROSSED,
    ON_TIME,
    CANCELLED,
    UNKNOWN
}

/**
 * Parsed live tracking status for a train from m-Indicator bulk endpoint.
 */
data class LiveStatus(
    val raw: String,
    val kind: LiveKind,
    val location: String?,
    val delayMinutes: Int?,
    val lessAccurate: Boolean,
    val fetchedAtMillis: Long = System.currentTimeMillis()
) {
    val isCancelled: Boolean get() = kind == LiveKind.CANCELLED

    val isStale: Boolean
        get() = (System.currentTimeMillis() - fetchedAtMillis) > 15 * 60 * 1000

    /**
     * Formats chip text with delay and train location per user requirement:
     * e.g., "● 5 min late · At Diva" or "● On time · At CSMT" or "● 8 min late · Bet. Thane - Diva"
     */
    fun getChipText(): String {
        if (isCancelled) return "⚠ Cancellation reported"

        val delayText = when {
            delayMinutes != null && delayMinutes == 0 -> "On time"
            delayMinutes != null && delayMinutes < 0 -> "${-delayMinutes} min early"
            delayMinutes != null && delayMinutes > 0 -> {
                val acc = if (lessAccurate) " ~" else ""
                "$delayMinutes min late$acc"
            }
            else -> null
        }

        val locText = when {
            location.isNullOrBlank() -> null
            kind == LiveKind.AT -> "At $location"
            kind == LiveKind.BETWEEN -> "Bet. $location"
            kind == LiveKind.REACHING -> "Arr. $location"
            kind == LiveKind.CROSSED -> "Past $location"
            else -> location
        }

        return when {
            delayText != null && locText != null -> "● $delayText · $locText"
            delayText != null -> "● $delayText"
            locText != null -> "● $locText"
            raw.isNotBlank() -> raw.take(28)
            else -> "Live"
        }
    }
}

/**
 * Detailed live tracking data from single-train endpoint (getlivetraininfo).
 */
data class SingleTrainLiveInfo(
    val trainNumber: String,
    val message: String?,
    val station: String?,
    val delayMinutes: Int?,
    val isAccurate: Boolean,
    val passengerCount: Int,
    val cancellationReports: Int,
    val isMoving: Boolean,
    val lastUpdateEpochMillis: Long,
    val fetchedAtMillis: Long = System.currentTimeMillis()
)
