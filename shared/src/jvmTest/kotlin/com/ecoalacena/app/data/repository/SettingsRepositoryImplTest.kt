package com.ecoalacena.app.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.NotificationSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SettingsRepositoryImplTest {

    private val repository = SettingsRepositoryImpl(
        database = PantryDatabase(
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PantryDatabase.Schema.create(it) },
        ),
        dispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun startsWithTheDefaults() = runTest {
        assertEquals(NotificationSettings(), repository.observeNotificationSettings().first())
    }

    @Test
    fun savedSettingsAreReadBack() = runTest {
        val chosen = NotificationSettings(enabled = false, lead = AlertLead.SAME_DAY, alertHour = 20, householdUpdates = false)

        repository.saveNotificationSettings(chosen)

        assertEquals(chosen, repository.observeNotificationSettings().first())
    }
}
