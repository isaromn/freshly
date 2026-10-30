package com.ecoalacena.app.presentation.util

import kotlin.math.abs
import kotlin.math.roundToLong

/** Formats [value] with a fixed number of [decimals], using a dot as separator. */
fun formatDecimal(value: Double, decimals: Int = 1): String {
    var factor = 1L
    repeat(decimals) { factor *= 10 }

    val scaled = (value * factor).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    val integerPart = absolute / factor

    if (decimals == 0) return "$sign$integerPart"

    val fraction = (absolute % factor).toString().padStart(decimals, '0')
    return "$sign$integerPart.$fraction"
}

/** Formats a quantity without trailing zeros: `2.0` -> "2", `0.5` -> "0.5". */
fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        formatDecimal(value, 2).trimEnd('0').trimEnd('.')
    }

// Units and day counts are language dependent: use `AppStrings.unitShort(unit)` and `AppStrings.days(count)`.

/** Groups digits in thousands, e.g. `45000` -> "45.000" (Colombian format). */
fun formatGrouped(value: Long, separator: Char = '.'): String {
    val grouped = abs(value)
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(separator.toString())
        .reversed()

    return if (value < 0) "-$grouped" else grouped
}
