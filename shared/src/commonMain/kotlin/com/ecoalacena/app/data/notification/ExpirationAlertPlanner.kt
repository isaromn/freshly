package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.civilDateOf
import com.ecoalacena.app.core.i18n.AppStrings
import com.ecoalacena.app.core.i18n.currentStrings
import com.ecoalacena.app.core.wholeDaysBetween
import com.ecoalacena.app.domain.model.PantryItem
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** One expiration alert, ready for the platform to schedule. */
data class ExpirationAlert(
    val itemId: String,
    val triggerAtMillis: Long,
    val title: String,
    val message: String,
)

/**
 * Decides when the alert of a pantry item arrives and what it says. Shared by every platform, so
 * Android, iOS and desktop alert at the same moment with the same words.
 *
 * The alert arrives on the expiration day minus the lead days, at the chosen hour of the user's
 * own time zone. A date picked by hand is stored as midnight UTC and means that calendar day; an
 * estimated date is an exact moment, whose day is the one on the user's clock.
 */
class ExpirationAlertPlanner(
    private val strings: () -> AppStrings = ::currentStrings,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

    /** The alert of [item], or `null` when it should get none. See [ExpirationNotifier.scheduleExpirationAlert]. */
    @OptIn(ExperimentalTime::class)
    fun plan(
        item: PantryItem,
        daysBefore: Int,
        alertHour: Int,
        nowMillis: Long,
        notifyIfLate: Boolean,
    ): ExpirationAlert? {
        val expiration = item.expirationDate ?: return null
        val zone = timeZone()
        val expirationDay = if (expiration.mod(MILLIS_PER_DAY) == 0L) {
            civilDateOf(expiration).let { LocalDate(it.year, it.month, it.day) }
        } else {
            Instant.fromEpochMilliseconds(expiration).toLocalDateTime(zone).date
        }
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).date
        if (expirationDay < today) return null

        val alertDay = expirationDay.minus(daysBefore.coerceAtLeast(0), DateTimeUnit.DAY)
        val alertTime = LocalTime(hour = alertHour.coerceIn(0, 23), minute = 0)
        var triggerAt = LocalDateTime(alertDay, alertTime).toInstant(zone).toEpochMilliseconds()
        if (triggerAt <= nowMillis) {
            if (!notifyIfLate) return null
            triggerAt = nowMillis + LATE_ALERT_DELAY_MILLIS
        }

        // Counted like the pantry card, so the alert and the card the user opens say the same.
        val daysLeft = wholeDaysBetween(triggerAt, expiration).coerceAtLeast(0)
        val texts = strings()
        return ExpirationAlert(
            itemId = item.id,
            triggerAtMillis = triggerAt,
            title = texts.expirationAlertTitle(item.name, daysLeft),
            message = texts.expirationAlertBody,
        )
    }

    companion object {
        /** A late alert arrives a few seconds after the food is saved, not in the middle of it. */
        const val LATE_ALERT_DELAY_MILLIS = 5_000L
    }
}
