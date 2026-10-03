package com.example.mumbailocalwo.presentation.util

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

object ServiceDayUtil {
    val KOLKATA_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    fun getNowKolkata(): ZonedDateTime {
        return ZonedDateTime.now(KOLKATA_ZONE)
    }

    /**
     * Service day runs 03:00 to 02:59.
     * Before 03:00, the service day is yesterday.
     */
    fun getServiceDayOfWeek(now: ZonedDateTime = getNowKolkata()): DayOfWeek {
        return if (now.hour < 3) {
            now.minusDays(1).dayOfWeek
        } else {
            now.dayOfWeek
        }
    }

    fun getDayAbbreviation(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "MON"
            DayOfWeek.TUESDAY -> "TUE"
            DayOfWeek.WEDNESDAY -> "WED"
            DayOfWeek.THURSDAY -> "THU"
            DayOfWeek.FRIDAY -> "FRI"
            DayOfWeek.SATURDAY -> "SAT"
            DayOfWeek.SUNDAY -> "SUN"
        }
    }

    fun runsToday(operatingDays: String, now: ZonedDateTime = getNowKolkata()): Boolean {
        val currentServiceDay = getDayAbbreviation(getServiceDayOfWeek(now))
        return operatingDays.split(",").any { it.trim().equals(currentServiceDay, ignoreCase = true) }
    }

    fun isUpcoming(departureMinutes: Int, dayOffset: Int = 0, now: ZonedDateTime = getNowKolkata()): Boolean {
        val curMinutes = now.hour * 60 + now.minute
        val effectiveDepMinutes = departureMinutes + (dayOffset * 1440)
        return (effectiveDepMinutes + 2) >= curMinutes
    }

    fun formatCountdown(departureMinutes: Int, dayOffset: Int = 0, now: ZonedDateTime = getNowKolkata()): String? {
        val curMinutes = now.hour * 60 + now.minute
        val diff = (departureMinutes + dayOffset * 1440) - curMinutes
        return when {
            diff in -2..0 -> "now"
            diff in 1..59 -> "in $diff min"
            diff in 60..360 -> {
                val h = diff / 60
                val m = diff % 60
                if (m > 0) "in ${h} h ${m} min" else "in ${h} h"
            }
            else -> null
        }
    }
}
