package com.example.mumbailocalwo.live

import com.example.mumbailocalwo.live.model.LiveKind
import com.example.mumbailocalwo.live.model.LiveStatus
import java.util.Locale

object LiveStatusParser {

    private val DELAY_LATE_REGEX = Regex("(\\d+)\\s*min\\s*Late", RegexOption.IGNORE_CASE)
    private val DELAY_EARLY_REGEX = Regex("(\\d+)\\s*min\\s*Early", RegexOption.IGNORE_CASE)

    fun parse(raw: String): LiveStatus {
        val trimmed = raw.trim()
        val lessAccurate = trimmed.contains("Less Accurate", ignoreCase = true)
        val cleanStr = trimmed
            .replace(Regex("\\[\\s*\\d+\\s*min\\s*ago\\s*\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(Less Accurate\\)", RegexOption.IGNORE_CASE), "")
            .trim()

        if (cleanStr.contains("Cancellation Reported", ignoreCase = true) || cleanStr.contains("Cancelled", ignoreCase = true)) {
            return LiveStatus(
                raw = trimmed,
                kind = LiveKind.CANCELLED,
                location = null,
                delayMinutes = null,
                lessAccurate = lessAccurate
            )
        }

        // Delay parsing
        var delayMinutes: Int? = null
        val lateMatch = DELAY_LATE_REGEX.find(cleanStr)
        if (lateMatch != null) {
            delayMinutes = lateMatch.groupValues[1].toIntOrNull()
        } else {
            val earlyMatch = DELAY_EARLY_REGEX.find(cleanStr)
            if (earlyMatch != null) {
                delayMinutes = -(earlyMatch.groupValues[1].toIntOrNull() ?: 0)
            } else if (cleanStr.contains("On Time", ignoreCase = true)) {
                delayMinutes = 0
            }
        }

        // Kind and Location parsing
        var kind = LiveKind.UNKNOWN
        var location: String? = null

        when {
            cleanStr.startsWith("At ", ignoreCase = true) -> {
                kind = LiveKind.AT
                val part = cleanStr.substring(3).split(",")[0].trim()
                location = titleCase(part)
            }
            cleanStr.startsWith("Rake at ", ignoreCase = true) -> {
                kind = LiveKind.AT
                val part = cleanStr.substring(8).split(",")[0].trim()
                location = titleCase(part)
            }
            cleanStr.startsWith("Between ", ignoreCase = true) -> {
                kind = LiveKind.BETWEEN
                val part = cleanStr.substring(8).split(",")[0].trim()
                location = formatBetween(part)
            }
            cleanStr.startsWith("Reaching ", ignoreCase = true) -> {
                kind = LiveKind.REACHING
                val part = cleanStr.substring(9).split(",")[0].trim()
                location = titleCase(part)
            }
            cleanStr.startsWith("Arriving ", ignoreCase = true) -> {
                kind = LiveKind.REACHING
                val part = cleanStr.substring(9).split(",")[0].trim()
                location = titleCase(part)
            }
            cleanStr.startsWith("Crossed ", ignoreCase = true) -> {
                kind = LiveKind.CROSSED
                val part = cleanStr.substring(8).split(",")[0].trim()
                location = titleCase(part)
            }
            cleanStr.startsWith("Past ", ignoreCase = true) -> {
                kind = LiveKind.CROSSED
                val part = cleanStr.substring(5).split(",")[0].trim()
                location = titleCase(part)
            }
            delayMinutes == 0 -> {
                kind = LiveKind.ON_TIME
            }
        }

        return LiveStatus(
            raw = trimmed,
            kind = kind,
            location = location,
            delayMinutes = delayMinutes,
            lessAccurate = lessAccurate
        )
    }

    private fun titleCase(text: String): String {
        return text.lowercase(Locale.ROOT).split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    private fun formatBetween(text: String): String {
        val parts = text.split("-")
        return parts.joinToString(" - ") { titleCase(it.trim()) }
    }
}
