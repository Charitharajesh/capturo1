package com.capturo.app.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateTimeUtils {

    // Parse ISO-8601 from backend
    fun parseFromBackend(isoString: String): ZonedDateTime {
        return ZonedDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .withZoneSameInstant(ZoneId.systemDefault())  // convert to user's local timezone
    }

    // Display date: "Apr 30, 2026"
    fun formatDisplayDate(zdt: ZonedDateTime): String {
        return zdt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH))
    }

    // Display time: "2:00 PM"
    fun formatDisplayTime(zdt: ZonedDateTime): String {
        return zdt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    }

    // Display time range: "2:00 PM – 4:00 PM (2 hours)"
    fun formatTimeRange(start: ZonedDateTime, durationHours: Double): String {
        val end = start.plusMinutes((durationHours * 60).toLong())
        val pattern = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        val hours = durationHours.toInt()
        val mins = ((durationHours - hours) * 60).toInt()
        val durationStr = if (mins == 0) "$hours hour${if (hours != 1) "s" else ""}"
                          else "$hours h ${mins} min"
        return "${start.format(pattern)} – ${end.format(pattern)} ($durationStr)"
    }

    // Relative time: "Just now", "5 min ago", "2h ago", "Yesterday", "Apr 28"
    fun formatRelativeTime(isoString: String): String {
        val zdt = parseFromBackend(isoString)
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val diffSeconds = ChronoUnit.SECONDS.between(zdt, now)
        return when {
            diffSeconds < 60           -> "Just now"
            diffSeconds < 3600         -> "${diffSeconds / 60} min ago"
            diffSeconds < 86400        -> "${diffSeconds / 3600}h ago"
            diffSeconds < 172800       -> "Yesterday"
            else                       -> formatDisplayDate(zdt)
        }
    }

    // Send to backend — ALWAYS UTC ISO-8601
    fun toBackendFormat(localDateTime: LocalDateTime): String {
        return localDateTime
            .atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    // Format for booking date input → API: "2026-04-30"
    fun toApiDateFormat(localDate: LocalDate): String {
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)  // "2026-04-30"
    }

    // Format for booking time input → API: "14:00:00"
    fun toApiTimeFormat(hour: Int, minute: Int): String {
        return String.format(Locale.ENGLISH, "%02d:%02d:00", hour, minute)
    }
}
