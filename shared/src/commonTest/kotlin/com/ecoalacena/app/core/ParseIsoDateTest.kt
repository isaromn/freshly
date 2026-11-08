package com.ecoalacena.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseIsoDateTest {

    @Test
    fun parsesDatesBackAndForth() {
        listOf("1970-01-01", "2000-02-29", "2026-11-08", "2024-12-31").forEach { text ->
            val millis = parseIsoDate(text)!!
            val date = civilDateOf(millis)
            assertEquals(text, "${date.year}-${date.month.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}")
            assertEquals(0L, millis % MILLIS_PER_DAY)
        }
    }

    @Test
    fun rejectsTextThatIsNotARealDate() {
        assertNull(parseIsoDate("2026-02-31"))
        assertNull(parseIsoDate("2026-13-01"))
        assertNull(parseIsoDate("31/10/2026"))
        assertNull(parseIsoDate(""))
    }
}
