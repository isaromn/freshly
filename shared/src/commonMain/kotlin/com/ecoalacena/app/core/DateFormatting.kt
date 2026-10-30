package com.ecoalacena.app.core

import com.ecoalacena.app.core.i18n.AppLanguage

/** A calendar date in the UTC time zone. */
data class CivilDate(val year: Int, val month: Int, val day: Int)

/**
 * Converts epoch milliseconds to a calendar date in UTC (proleptic Gregorian calendar).
 * The date picker also works in UTC, so a picked day always shows the same date.
 */
fun civilDateOf(epochMillis: Long): CivilDate {
    // Days since 1970-01-01 -> civil date, using Howard Hinnant's algorithm.
    val days = epochMillis.floorDiv(MILLIS_PER_DAY)
    val shifted = days + 719_468
    val era = (if (shifted >= 0) shifted else shifted - 146_096) / 146_097
    val dayOfEra = shifted - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthIndex = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthIndex + 2) / 5 + 1
    val month = if (monthIndex < 10) monthIndex + 3 else monthIndex - 9
    val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0

    return CivilDate(year = year.toInt(), month = month.toInt(), day = day.toInt())
}

/** `27/10/2026` in Spanish, `10/27/2026` in English. */
fun formatDate(epochMillis: Long, language: AppLanguage): String {
    val date = civilDateOf(epochMillis)
    val day = date.day.toString().padStart(2, '0')
    val month = date.month.toString().padStart(2, '0')

    return when (language) {
        AppLanguage.ES -> "$day/$month/${date.year}"
        AppLanguage.EN -> "$month/$day/${date.year}"
    }
}

/** Midnight (UTC) at the start of the day that contains [epochMillis]. */
fun startOfDayUtc(epochMillis: Long): Long = epochMillis - epochMillis.mod(MILLIS_PER_DAY)
