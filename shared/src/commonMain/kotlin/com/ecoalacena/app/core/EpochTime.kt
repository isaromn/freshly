package com.ecoalacena.app.core

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Whole days from [fromMillis] to [toMillis], rounded towards zero (negative when [toMillis] is
 * earlier). The pantry cards and the expiration alerts both count "days left" with it.
 */
fun wholeDaysBetween(fromMillis: Long, toMillis: Long): Int = ((toMillis - fromMillis) / MILLIS_PER_DAY).toInt()

/** Current time as epoch milliseconds, the format used by `PantryItem.expirationDate`. */
@OptIn(ExperimentalTime::class)
fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
