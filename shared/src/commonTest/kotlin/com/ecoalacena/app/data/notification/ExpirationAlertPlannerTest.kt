package com.ecoalacena.app.data.notification

import com.ecoalacena.app.core.MILLIS_PER_DAY
import com.ecoalacena.app.core.i18n.EnglishStrings
import com.ecoalacena.app.core.i18n.SpanishStrings
import com.ecoalacena.app.testing.TEST_NOW
import com.ecoalacena.app.testing.testItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

// TEST_NOW is 2023-11-14 22:13 UTC.
@OptIn(ExperimentalTime::class)
class ExpirationAlertPlannerTest {

    private val planner = ExpirationAlertPlanner(strings = { EnglishStrings }, timeZone = { TimeZone.UTC })

    @Test
    fun alertsTheChosenDaysBeforeAtTheChosenHour() {
        val milk = testItem("milk", "Whole Milk", expiresInDays = 6) // expires on Nov 20

        val alert = planner.plan(milk, daysBefore = 2, alertHour = 9, nowMillis = TEST_NOW, notifyIfLate = false)

        assertEquals(millisOf(2023, 11, 18, 9, TimeZone.UTC), alert?.triggerAtMillis)
        assertEquals("⚠️ Whole Milk expires in 2 days!", alert?.title)
        assertEquals("Check recipes to use it before it spoils.", alert?.message)
        assertEquals("milk", alert?.itemId)
    }

    @Test
    fun theSameDayAlertSaysToday() {
        val milk = testItem("milk", "Whole Milk", expiresInDays = 6)

        val alert = planner.plan(milk, daysBefore = 0, alertHour = 18, nowMillis = TEST_NOW, notifyIfLate = false)

        assertEquals(millisOf(2023, 11, 20, 18, TimeZone.UTC), alert?.triggerAtMillis)
        assertEquals("⚠️ Whole Milk expires today!", alert?.title)
    }

    @Test
    fun theHourIsInTheUsersTimeZone() {
        val bogota = TimeZone.of("America/Bogota")
        val planner = ExpirationAlertPlanner(strings = { EnglishStrings }, timeZone = { bogota })

        val alert = planner.plan(testItem("milk", expiresInDays = 6), 2, 9, TEST_NOW, notifyIfLate = false)

        // 9:00 in Bogotá (UTC-5) is 14:00 UTC.
        assertEquals(millisOf(2023, 11, 18, 14, TimeZone.UTC), alert?.triggerAtMillis)
    }

    @Test
    fun anEstimatedDateUsesTheDayOnTheUsersClock() {
        val bogota = TimeZone.of("America/Bogota")
        val planner = ExpirationAlertPlanner(strings = { EnglishStrings }, timeZone = { bogota })
        // 22:00 on Nov 14 in Bogotá is already Nov 15 in UTC.
        val now = millisOf(2023, 11, 15, 3, TimeZone.UTC)
        val chicken = testItem("chicken", "Chicken").copy(expirationDate = now + 2 * MILLIS_PER_DAY)

        // It expires on Nov 16 at night in Bogotá: two days before is this morning, so a new item alerts now.
        assertNull(planner.plan(chicken, 2, 9, now, notifyIfLate = false))
        assertEquals(now + ExpirationAlertPlanner.LATE_ALERT_DELAY_MILLIS, planner.plan(chicken, 2, 9, now, true)?.triggerAtMillis)
        assertEquals(millisOf(2023, 11, 15, 9, bogota), planner.plan(chicken, 1, 9, now, false)?.triggerAtMillis)
    }

    @Test
    fun aDatePickedByHandKeepsItsCalendarDay() {
        val bogota = TimeZone.of("America/Bogota")
        val planner = ExpirationAlertPlanner(strings = { EnglishStrings }, timeZone = { bogota })
        val picked = testItem("milk").copy(expirationDate = millisOf(2023, 11, 20, 0, TimeZone.UTC))

        val alert = planner.plan(picked, 0, 9, TEST_NOW, notifyIfLate = false)

        assertEquals(millisOf(2023, 11, 20, 9, bogota), alert?.triggerAtMillis)
    }

    @Test
    fun noAlertWithoutADateOrWhenAlreadyExpired() {
        assertNull(planner.plan(testItem("salt"), 2, 9, TEST_NOW, notifyIfLate = true))
        assertNull(planner.plan(testItem("old", expiresInDays = -1), 2, 9, TEST_NOW, notifyIfLate = true))
    }

    @Test
    fun aPassedAlertIsSkippedUnlessTheFoodIsNew() {
        val yogurt = testItem("yogurt", "Yogurt", expiresInDays = 2) // the 2-days-before alert was this morning

        assertNull(planner.plan(yogurt, 2, 9, TEST_NOW, notifyIfLate = false))

        val late = planner.plan(yogurt, 2, 9, TEST_NOW, notifyIfLate = true)
        assertEquals(TEST_NOW + ExpirationAlertPlanner.LATE_ALERT_DELAY_MILLIS, late?.triggerAtMillis)
        assertEquals("⚠️ Yogurt expires tomorrow!", late?.title)
    }

    @Test
    fun theTextFollowsTheLanguage() {
        val planner = ExpirationAlertPlanner(strings = { SpanishStrings }, timeZone = { TimeZone.UTC })

        val alert = planner.plan(testItem("milk", "Leche entera", expiresInDays = 6), 2, 9, TEST_NOW, false)

        assertEquals("⚠️ ¡Leche entera vence en 2 días!", alert?.title)
        assertEquals("Revisa las recetas para aprovecharlo antes de que se dañe.", alert?.message)
    }

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int, zone: TimeZone): Long =
        LocalDateTime(year, month, day, hour, 0).toInstant(zone).toEpochMilliseconds()
}
