package com.ecoalacena.app.data.local

import app.cash.sqldelight.db.QueryResult
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
        val schema = PantryDatabase.Schema
        if (isNewDatabase) {
            schema.create(driver)
            driver.setUserVersion(schema.version)
        } else {
            // Databases created before the schema had versions report 0: they are version 1.
            val current = driver.userVersion().coerceAtLeast(1)
            if (current < schema.version) {
                schema.migrate(driver, current, schema.version)
                driver.setUserVersion(schema.version)
            }
        }
        return driver
    }
}

/** The schema version stored in the SQLite file (`PRAGMA user_version`). */
private fun SqlDriver.userVersion(): Long =
    executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0,
    ).value

private fun SqlDriver.setUserVersion(version: Long) {
    execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}
