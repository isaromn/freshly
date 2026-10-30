package com.ecoalacena.app.core

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** Current time as epoch milliseconds, the format used by `PantryItem.expirationDate`. */
@OptIn(ExperimentalTime::class)
fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
