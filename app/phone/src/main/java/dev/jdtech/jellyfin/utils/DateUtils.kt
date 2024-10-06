package dev.jdtech.jellyfin.utils

import java.text.DateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatDateTimeToUserPreference(date: LocalDateTime): String {
        val systemTimeZone = ZoneId.systemDefault()
        val instant = date.atZone(systemTimeZone).toInstant()
        val dateAsDate = Date.from(instant)

        val dateFormatter = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
        val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())

        val formattedDate = dateFormatter.format(dateAsDate)
        val formattedTime = timeFormatter.format(dateAsDate)

        return "$formattedDate $formattedTime"
    }
}