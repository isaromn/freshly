package com.ecoalacena.app.data.local

import app.cash.sqldelight.db.SqlDriver

internal const val DATABASE_NAME = "pantry.db"

/**
 * Creates the platform-specific [SqlDriver] used by SQLDelight.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
