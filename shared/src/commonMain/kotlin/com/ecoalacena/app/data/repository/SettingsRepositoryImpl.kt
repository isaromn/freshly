package com.ecoalacena.app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ecoalacena.app.database.PantryDatabase
import com.ecoalacena.app.domain.model.AlertLead
import com.ecoalacena.app.domain.model.NotificationSettings
import com.ecoalacena.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Stores the settings as key/value rows in the local SQLDelight database. */
class SettingsRepositoryImpl(
    private val database: PantryDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SettingsRepository {

    private val queries = database.pantryDatabaseQueries

    override fun observeNotificationSettings(): Flow<NotificationSettings> =
        queries.selectSettings()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.associate { it.settingKey to it.settingValue }.toNotificationSettings() }
            .distinctUntilChanged()

    override suspend fun saveNotificationSettings(settings: NotificationSettings) {
        withContext(dispatcher) {
            database.transaction {
                queries.upsertSetting(KEY_ENABLED, settings.enabled.toString())
                queries.upsertSetting(KEY_LEAD, settings.lead.name)
                queries.upsertSetting(KEY_ALERT_HOUR, settings.alertHour.toString())
                queries.upsertSetting(KEY_HOUSEHOLD, settings.householdUpdates.toString())
            }
        }
    }

    private companion object {
        const val KEY_ENABLED = "notifications.enabled"
        const val KEY_LEAD = "notifications.lead"
        const val KEY_ALERT_HOUR = "notifications.alertHour"
        const val KEY_HOUSEHOLD = "notifications.household"
    }

    /** Missing or unreadable values keep their default. */
    private fun Map<String, String>.toNotificationSettings(): NotificationSettings {
        val defaults = NotificationSettings()
        return NotificationSettings(
            enabled = get(KEY_ENABLED)?.toBooleanStrictOrNull() ?: defaults.enabled,
            lead = AlertLead.entries.firstOrNull { it.name == get(KEY_LEAD) } ?: defaults.lead,
            alertHour = get(KEY_ALERT_HOUR)?.toIntOrNull()?.takeIf { it in 0..23 } ?: defaults.alertHour,
            householdUpdates = get(KEY_HOUSEHOLD)?.toBooleanStrictOrNull() ?: defaults.householdUpdates,
        )
    }
}
