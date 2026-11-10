package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Her sense of when she is (his Pixel, 9 November 2026). Her one
 * conversation stays open all day, and it carried the time only as it
 * opened, so the time went stale: at 22:59 and 23:19 she asked "What's on
 * for today?". Every one of his messages now carries its own time in her
 * turn, as the beach gives it: the day and clock time, the part of the day
 * in the beach's own words, and how long since his last message when that
 * is more than an hour. The conversation is never opened again for it.
 */
class HerTimeTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val london = ZoneId.of("Europe/London")
    private val morning = ZonedDateTime.of(2026, 11, 9, 8, 10, 0, 0, london)
    private val evening = ZonedDateTime.of(2026, 11, 9, 19, 30, 0, 0, london)

    @Test
    fun aConversationOpenedInTheMorningCarriesTheEveningsTimeOnTheEveningTurn() {
        val (_, pages) = Bench.fixture(shelf, morning.toInstant().toEpochMilli(), Talks.SHELL_RECIPE)
        val recipe = "The app: nothing is being kept or set out from this."
        // Opened in the morning, with his first message.
        val opening = HerWindow.opening("Her window.", morning, recipe)
        assertTrue(opening.contains("It is Monday 9 November 2026, 08:10 as this conversation opens."), opening)
        val first = HerWindow.turnKept("I walked Haven this morning", pages, listOf(Cook.Handed(recipe)), recipe, now = morning)
        assertEquals("(It is Monday 9 November 2026, 08:10, morning.)", first.lines().first())
        // The same conversation, still open, in the evening: the evening's time, its part of the day, and how long since his last message.
        val later = HerWindow.turnKept("stiff but free", pages, listOf(Cook.Handed(recipe)), recipe, now = evening, lastMs = morning.toInstant().toEpochMilli())
        assertEquals("(It is Monday 9 November 2026, 19:30, evening. His last message was 11 hours and 20 minutes ago.)", later.lines().first())
        assertFalse(later.contains("08:10"), "never the morning's time on the evening turn")
        assertFalse(later.contains("morning."), later)
        assertTrue(later.endsWith("stiff but free"))
        // Never played back as hers.
        assertTrue(HerWindow.echoLines(later).any { it.startsWith("It is Monday 9 November 2026, 19:30, evening.") })
    }

    @Test
    fun nearMidnightIsNightInTheBeachsOwnWordsAndAGapUnderAnHourIsNotSaid() {
        val at2259 = ZonedDateTime.of(2026, 11, 9, 22, 59, 0, 0, london)
        val at2319 = ZonedDateTime.of(2026, 11, 9, 23, 19, 0, 0, london)
        assertEquals("It is Monday 9 November 2026, 22:59, night.", HerWindow.stamp(at2259))
        assertEquals("It is Monday 9 November 2026, 23:19, night.", HerWindow.stamp(at2319, at2259.toInstant().toEpochMilli()), "twenty minutes is not said")
        // The beach's nine parts, as its clock voices an address.
        assertEquals("deep night", DAY_PARTS[momentToAddress(ZonedDateTime.of(2026, 11, 10, 1, 0, 0, 0, london).toInstant().toEpochMilli())[8] - '1'])
        assertTrue(HerWindow.stamp(ZonedDateTime.of(2026, 11, 9, 14, 0, 0, 0, london)).endsWith(", afternoon."))
    }

    @Test
    fun howLongSinceHisLastMessageIsSaidInHisWords() {
        val hour = 3_600_000L
        assertEquals("an hour and 5 minutes", HerWindow.since(hour + 5 * 60_000L))
        assertEquals("3 hours", HerWindow.since(3 * hour))
        assertEquals("a day and 2 hours", HerWindow.since(26 * hour))
        assertEquals("4 days", HerWindow.since(96 * hour))
        val now = ZonedDateTime.of(2026, 11, 10, 8, 40, 0, 0, london)
        assertEquals("It is Tuesday 10 November 2026, 08:40, morning. His last message was 9 hours and 21 minutes ago.",
            HerWindow.stamp(now, ZonedDateTime.of(2026, 11, 9, 23, 19, 0, 0, london).toInstant().toEpochMilli()))
    }
}
