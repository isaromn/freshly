package com.ecoalacena.app.domain.model

/** How long before the expiration day the alert of an item arrives. */
enum class AlertLead(val daysBefore: Int) {
    SAME_DAY(0),
    ONE_DAY(1),
    TWO_DAYS(2),
}

/**
 * The notification preferences of the user.
 *
 * @property enabled turns every notification of the app on or off.
 * @property lead how many days before an item expires its alert arrives.
 * @property alertHour hour of the day (0–23, local time) at which expiration alerts arrive.
 * @property householdUpdates alerts when something runs out, so the household does not buy it twice.
 */
data class NotificationSettings(
    val enabled: Boolean = true,
    val lead: AlertLead = AlertLead.TWO_DAYS,
    val alertHour: Int = DEFAULT_ALERT_HOUR,
    val householdUpdates: Boolean = true,
) {
    companion object {
        const val DEFAULT_ALERT_HOUR = 9

        /** The hours offered in the settings screen. */
        val ALERT_HOURS = listOf(8, 9, 12, 18, 20)
    }
}
