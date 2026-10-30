package com.ecoalacena.app.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ecoalacena.app.database.PantryDatabase
import java.io.File

actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val databaseFile = File(System.getProperty("user.home"), ".ecoalacena/$DATABASE_NAME")
        databaseFile.parentFile?.mkdirs()
        val isNewDatabase = !databaseFile.exists()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        if (isNewDatabase) {
            PantryDatabase.Schema.create(driver)
        }
        return driver
    }
}
