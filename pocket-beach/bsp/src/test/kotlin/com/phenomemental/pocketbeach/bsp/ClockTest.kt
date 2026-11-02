package com.phenomemental.pocketbeach.bsp

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 0.9.1: the clock on the Block screen (spine:pocket-beach 1.1111787164). */
class ClockTest {
    private val today = LocalDate.of(2026, 11, 2)

    @Test
    fun aDayIsAFullWidthAddressAndBack() {
        assertEquals("2026421200", Clock.dayOf(today))
        assertEquals(today, Clock.dateOf("2026421200"))
        for (n in 0L..400L) today.plusDays(n).let { d -> assertEquals(d, Clock.dateOf(Clock.dayOf(d)), d.toString()) }
        assertNull(Clock.dateOf("2026421000"), "a week names no day")
        assertNull(Clock.dateOf("20264212"), "eight digits alone are never a day's address")
    }

    @Test
    fun itSaysTheDayAsHeWould() {
        assertEquals("Today, Monday 2 November", Clock.said(today, today))
        assertEquals("Yesterday, Sunday 1 November", Clock.said(today.minusDays(1), today))
        assertEquals("Tuesday 10 November", Clock.said(today.plusDays(8), today))
        assertEquals(listOf("2026000000", "2026400000", "2026420000", "2026421000"), Clock.trail("2026421200"))
        assertEquals(listOf("2026", "October to December 2026", "November 2026", "1 to 7 November"), Clock.trail("2026421200").map { Clock.rungSaid(it, today) })
    }

    @Test
    fun hisNowIsAClockByItsNameOrItsOpening() {
        assertTrue(Clock.isClock(Days.NOW, 10, Landing.NOW_OPENING), "his now, as 0.6 opened it")
        assertTrue(Clock.isClock("diary", 10, Days.NOW_OPENING))
        assertFalse(Clock.isClock("garden", 1, "The garden"))
        assertFalse(Clock.isClock("diary", 10, "Ten deep, but no clock"))
    }

    @Test
    fun hisNowStandsAtTodayAndADayHeWritesLandsAtItsDate() {
        val shelf = Shelf(DirShelfSource(java.io.File(System.getProperty("pocket.shelf") ?: "../shelf")))
        val (_, pages) = Bench.fixture(shelf, 1793620000000L, Talks.SHELL_RECIPE)
        val (clock, _) = Landing.withDay(null, Days.dayAddress(today), "Walked the dog.", emptyMap())
        check(pages.carryOut(pages.planWhole(Days.NOW, clock)).ok)
        assertEquals("Walked the dog.", pages.position(Region.POCKET, Days.NOW, Clock.dayOf(today))!!.text)
        val tomorrow = Clock.dayOf(today.plusDays(1))
        val done = pages.carryOut(pages.planChange(Days.NOW, tomorrow, "Ring Jo."))
        assertTrue(done.ok, done.problem)
        assertEquals("Ring Jo.", pages.position(Region.POCKET, Days.NOW, tomorrow)!!.text)
        assertEquals("Walked the dog.", pages.position(Region.POCKET, Days.NOW, Clock.dayOf(today))!!.text, "today's line where it was")
        assertEquals(10, pages.floorOf(Days.NOW), "no supernest")
    }
}
