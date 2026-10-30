package com.ecoalacena.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ecoalacena.app.database.PantryDatabase

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(PantryDatabase.Schema, DATABASE_NAME)
}
