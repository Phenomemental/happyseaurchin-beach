package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Mark
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Waiting
import com.phenomemental.pocketbeach.bsp.jsGet
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.10, Today and keeping (spine:pocket-beach 1.111178719911-18), on
 * the phone with a stand-in for her: his words wait in moments.json and Keep
 * today on the dial keeps them in his now in Sundial's form; yesterday's
 * words, not kept, are offered first; a day kept before Sundial is counted
 * by its address; a second handle posts to its own room; the dial's one
 * question follows his tomorrow line; where he stands, chosen from the town
 * list; today's words not yet kept, under Show the workings.
 */
@RunWith(AndroidJUnit4::class)
class SundialDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val wire = NowhereWire()
    private val mind = StandIn()
    private val h = Hatch.MATTHEW_HANDLE

    @Before
    fun standIns() {
        app.wire = wire
        app.mind = mind
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
        app.showDetails = false
    }

    private fun say(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    private fun none(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()

    /** Words on the screen, wherever they stand. */
    private fun sees(text: String, timeoutMs: Long = 10_000) =
        compose.waitUntil(timeoutMs) { compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun hisWordsWaitAndKeepTodayKeepsThemInSundialsForm() {
        Fresh.hatched()
        val today = Keep.dayOf(System.currentTimeMillis())
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.waitFor("own-question", "What’s with you")
            say("Took the dog out with Sam by the river. I felt calm.")
            compose.waitFor("say", timeoutMs = 20_000)
            // (d) His words and their instant wait in the app's own file, never a block, until he keeps today.
            compose.waitUntil(10_000) { app.waiting.of(today).map { it.words } == listOf("Took the dog out with Sam by the river. I felt calm.") }
            assertTrue("nothing written to his now yet", app.storage.read(Days.NOW)?.contains("Took the dog out") != true)
            // Keep today waits while she answers.
            compose.waitUntil(20_000) { app.asking.settled }
            compose.tap("keep-today")
            compose.waitFor("keep-card", timeoutMs = 20_000)
            compose.waitFor("keep-title", "A walk by the river")
            compose.waitFor("keep-places")
            assertTrue("nothing written before his yes", app.storage.read(Days.NOW)?.contains("Took the dog out") != true)
            compose.tap("day-yes")
            compose.waitFor("keep-note", "Kept.", timeoutMs = 20_000)
            val now = app.storage.read(Days.NOW)!!
            // The day's line in Sundial's form at today, his moment at its minute.
            val line = Keep.dayText(com.phenomemental.pocketbeach.bsp.parseJson(now), today)!!
            assertTrue(line, line.startsWith("A walk by the river\nYou took the dog out with Sam by the river."))
            assertTrue(line, line.contains("Tomorrow: ring Jo about the music"))
            assertTrue(now, now.contains("Took the dog out with Sam by the river. I felt calm."))
            assertTrue("no P6", app.storage.read(Days.p6Of(h))?.contains("Took the dog out") != true)
            // (a) Kept: they go from the waiting room.
            assertTrue(app.waiting.all.isEmpty())
            // The day is on the strandline.
            compose.menu("places-days")
            compose.waitFor("days-count", "1 DAY")
            compose.waitFor("strand:${Keep.dayAddress(today)}")
        }
    }

    @Test
    fun yesterdaysWordsNotKeptAreOfferedFirst() {
        Fresh.hatched()
        val today = Keep.dayOf(System.currentTimeMillis())
        val yesterday = today.minusDays(1)
        val noon = yesterday.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        app.waiting.add("Walked the dog in the rain.", noon)
        // Older words are let go as the day turns.
        app.waiting.add("A word from three days ago.", noon - 2 * 86_400_000L)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("yesterday-offer")
            sees("You spoke yesterday and didn't keep it")
            assertTrue("before today's question", none("own-question") && none("greet-question"))
            compose.waitUntil(10_000) { app.waiting.all.map { it.words } == listOf("Walked the dog in the rain.") }
            compose.tap("keep-yesterday")
            compose.waitFor("keep-card", timeoutMs = 20_000)
            compose.waitFor("keep-card-yesterday", "You spoke yesterday")
            compose.tap("day-yes")
            compose.waitFor("keep-note", "Kept.", timeoutMs = 20_000)
            val clock = com.phenomemental.pocketbeach.bsp.parseJson(app.storage.read(Days.NOW)!!)
            // At yesterday's address, not today's.
            assertTrue(Keep.dayText(clock, yesterday)!!.startsWith("A walk by the river"))
            assertTrue(Keep.dayText(clock, today) == null)
            assertTrue(app.waiting.all.isEmpty())
            compose.waitFor("own-question")
        }
    }

    @Test
    fun aDayKeptBeforeSundialIsCountedByItsAddress() {
        Fresh.hatched()
        val today = Keep.dayOf(System.currentTimeMillis())
        val then = Days.dayAddress(today.minusDays(2))
        // A Rockpool day: his telling in his now, two pieces of it in his P6, with no places line.
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Days.voiced(clock, then.take(8), "Took the dog out with Sam. I felt calm.")
        check(app.pages.carryOut(app.pages.planWhole(Days.NOW, clock)).ok)
        val p6 = Days.emptyP6(h)
        (jsGet(jsGet(p6, "1"), "1") as JsObj)["1"] = JsObj.of("_" to str("Took the dog out with Sam."), "2" to str(then), "3" to str("2026-11-01T17:31:00.000Z"))
        (jsGet(jsGet(p6, "1"), "2") as JsObj)["1"] = JsObj.of("_" to str("I felt calm."), "2" to str(then), "3" to str("2026-11-01T17:32:00.000Z"))
        check(app.pages.carryOut(app.pages.planWhole(Days.p6Of(h), p6)).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("places-days")
            compose.waitFor("days-count", "1 DAY")
            compose.waitFor("strand:$then")
            compose.tap("days-resting")
            // Resting where his P6 holds it: what happened and what he felt, one day each; nothing in what matters.
            compose.waitFor("pool:0", "one day")
            compose.waitFor("pool:1", "one day")
            compose.waitFor("pool:3", "no days")
            compose.tap("pool:1")
            compose.waitFor("resting:$then")
            sees("I felt calm")
        }
        // Read, never moved: his P6 stands as it was.
        assertTrue(app.storage.read(Days.p6Of(h))!!.contains("I felt calm."))
    }

    @Test
    fun aSecondHandlePostsToItsOwnRoomUnderItsOwnName() {
        Fresh.hatched("ada-lovelace", JsObj.of("_" to str("Ada Lovelace"), "1" to str("Her heart.")))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("places-room")
            compose.waitFor("room-name", "pool:ada-lovelace, as ada-lovelace")
            compose.onNodeWithTag("room-box").performTextInput("Hello from the phone.")
            compose.tap("room-post")
            compose.waitFor("room-note", "Posted to your room, entry 7", timeoutMs = 20_000)
            assertEquals(1, wire.sent.size)
            val (url, body) = wire.sent.single()
            assertEquals("${PoolPost.BEACH}/.well-known/pscale-beach?block=pool:ada-lovelace", url)
            assertEquals("""{"append":true,"content":{"1":"ada-lovelace","_":"Hello from the phone."}}""", body)
            compose.waitFor("sent-note")
            sees("Hello from the phone.")
            assertEquals("7", app.notesSent.all.single().entry)
        }
    }

    @Test
    fun theDialAsksHerOneQuestionAfterHisTomorrowLine() {
        Fresh.hatched()
        val today = Keep.dayOf(System.currentTimeMillis())
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Keep.dayLine(clock, today.minusDays(1), Mark.write(Mark.Sheet("Phase two", listOf("You worked with David."), "ring Jo about the music", listOf(1, 4))), System.currentTimeMillis() - 86_400_000)
        check(app.pages.carryOut(app.pages.planWhole(Days.NOW, clock)).ok)
        Fresh.ungreeted()
        mind.talkScript += "What came of the call with Jo?"
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("greet-said", "You said: “Tomorrow, ring Jo about the music.”", timeoutMs = 20_000)
            compose.waitFor("greet-question", "What came of the call with Jo?", timeoutMs = 20_000)
            val turn = mind.talkMessages.single()
            assertTrue(turn, turn.contains("he has opened his sundial and hasn't spoken to you yet today"))
            assertTrue(turn, turn.contains("ring Jo about the music"))
            // His answer is his first words today: the question gives way to the conversation.
            say("Jo loved the music.")
            compose.waitFor("say", timeoutMs = 20_000)
            assertEquals(2, mind.talkMessages.size)
            compose.waitUntil(5_000) { none("greet-question") }
        }
        // Asked once a day.
        app.asking.reset()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("top-bar")
            Thread.sleep(1500)
            assertEquals(2, mind.talkMessages.size)
        }
    }

    @Test
    fun whereHeStandsIsATownFromTheListAndHisWaitingWordsShowUnderTheWorkings() {
        Fresh.hatched()
        app.showDetails = true
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("where-ask")
            say("Walked to the river.")
            compose.waitFor("say", timeoutMs = 20_000)
            compose.menu("menu-settings")
            compose.waitFor("where-now", "Not set yet")
            compose.onNodeWithTag("where-search").performTextInput("Northam")
            compose.tap("where-town:Northampton, United Kingdom")
            compose.waitFor("where-now", "Northampton, chosen from the town list")
            val p = app.where.place!!
            assertEquals(52.2, p.lat, 0.1)
            assertEquals(-0.9, p.lon, 0.1)
            compose.waitFor("waiting-said", Waiting.SAID)
            compose.waitFor("waiting-moment")
            sees("Walked to the river.")
        }
        // Set once: the dial asks no more.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            Thread.sleep(1000)
            assertTrue(none("where-ask"))
        }
    }
}
