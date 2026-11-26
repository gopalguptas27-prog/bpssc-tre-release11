package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Single source of truth for Indian Standard Time (IST — Asia/Kolkata, UTC+5:30).
 * Ensures every live clock, timestamp display, scheduled release parser, and report boundary
 * strictly uses Asia/Kolkata regardless of the user's device or server timezone.
 */
object IstClock {
    const val IST_TIME_ZONE_ID = "Asia/Kolkata"

    val istTimeZone: TimeZone
        get() = TimeZone.getTimeZone(IST_TIME_ZONE_ID)

    val timeZone: TimeZone
        get() = istTimeZone

    fun newDateFormat(pattern: String, locale: Locale = Locale.US): SimpleDateFormat =
        SimpleDateFormat(pattern, locale).apply {
            timeZone = istTimeZone
        }

    fun formatNow(pattern: String, locale: Locale = Locale.US): String =
        newDateFormat(pattern, locale).format(Date())

    fun formatDate(date: Date, pattern: String, locale: Locale = Locale.US): String =
        newDateFormat(pattern, locale).format(date)

    fun formatEpochMillis(epochMillis: Long, pattern: String, locale: Locale = Locale.US): String =
        formatDate(Date(epochMillis), pattern, locale)

    fun formatTimeWithSeconds(epochMillis: Long = System.currentTimeMillis()): String =
        formatEpochMillis(epochMillis, "hh:mm:ss a 'IST'")

    fun formatTimeOnly(epochMillis: Long = System.currentTimeMillis()): String =
        formatEpochMillis(epochMillis, "hh:mm a 'IST'")

    fun formatDateTime(epochMillis: Long = System.currentTimeMillis()): String =
        formatEpochMillis(epochMillis, "dd MMM yyyy, hh:mm a 'IST'")

    fun parse(raw: String, pattern: String, locale: Locale = Locale.US): Date? =
        runCatching { newDateFormat(pattern, locale).parse(raw) }.getOrNull()

    fun calendar(epochMillis: Long = System.currentTimeMillis()): Calendar =
        Calendar.getInstance(istTimeZone).apply {
            timeInMillis = epochMillis
        }

    fun todayIstKey(nowMillis: Long = System.currentTimeMillis()): String =
        formatEpochMillis(nowMillis, "yyyy-MM-dd")

    fun offsetIstKey(daysOffset: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val cal = calendar(nowMillis)
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return formatDate(cal.time, "yyyy-MM-dd")
    }

    fun formatIstKeyReadable(dateKey: String): String {
        val parsed = parse(dateKey.trim(), "yyyy-MM-dd") ?: return dateKey
        return formatDate(parsed, "dd MMM yyyy")
    }
}
