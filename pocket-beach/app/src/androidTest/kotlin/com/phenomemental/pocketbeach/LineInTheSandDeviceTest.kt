package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.Js
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.collectUnderscore
import com.phenomemental.pocketbeach.bsp.parseJson
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
 * Sundial 0.10.3, THE LINE IN THE SAND (spine:pocket-beach 1.11117871992),
 * on the phone with a stand-in for her: the keep card shows each moment with
 * the places she marked in her reply, and she isn't asked again; his day and
 * his short answers take the routes cook 1.1.8.3 says; he takes a card off
 * his hand himself; and every yes, Keep yesterday's among them, has its own
 * entry in his history as it is written.
 */
@RunWith(AndroidJUnit4::class)
class LineInTheSandDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val mind = StandIn()
    private val h = Hatch.MATTHEW_HANDLE

    @Before
    fun standIns() {
        app.wire = NowhereWire()
        app.mind = mind
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
    }

    private fun say(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    /** His history's entries, oldest first, each its opening line. */
    private fun history(): List<String> {
        val out = mutableListOf<String>()
        fun walk(n: Js?) {
            val o = n as? JsObj ?: return
            (o["_"] as? JsObj)?.let(::walk)
            for (k in 1..9) (o["$k"] as? JsObj)?.let { e -> collectUnderscore(e)?.let { out += it } }
        }
        walk(app.storage.read(Hatch.historyOf(h))?.let(::parseJson))
        return out
    }

    /** A day of his: three sentences and forty words, as cook 1.1.8.3 ships. */
    private val hisDay = "Took the dog out with Sam by the river this morning and the water was high after all the rain. " +
        "I felt calm for the first time in a while, which surprised me. Then I spent an hour on the membership form and made good progress with it."

    @Test
    fun test7_theCardShowsHerPlacesForEachMomentAndSheIsNotAskedAgain() {
        Fresh.hatched()
        mind.talkScript += "You took the dog out with Sam by the river, and you felt calm. What did you notice on the way back?\n\nPlaces: what happened, what he felt"
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say(hisDay)
            compose.waitUntil(20_000) { app.waiting.all.any { it.places.isNotEmpty() } }
            assertEquals(listOf(listOf(1, 2)), app.waiting.all.map { it.places })
            compose.waitUntil(20_000) { app.asking.settled }
            compose.tap("keep-today")
            compose.waitFor("keep-card", timeoutMs = 20_000)
            compose.waitFor("keep-marked", "She's marked where each moment sits. Tap a colour only to change it.")
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasTestTag("keep-moment-place:0:1") and hasContentDescription(", on", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue("her places came from her reply: she wasn't asked again", mind.sortAsks.none { it.first?.contains("for each of his moments") == true })
            val before = history().size
            compose.tap("day-yes")
            compose.waitFor("kept-shell", "A walk by the river", timeoutMs = 20_000)
            val line = Keep.dayText(parseJson(app.storage.read(Days.NOW)!!), Keep.dayOf(System.currentTimeMillis()))!!
            assertTrue(line, !line.contains("In your own words.") && !line.contains("What’s ahead."))
            compose.waitUntil(10_000) { history().size == before + 1 }
            assertTrue(history().last(), history().last().startsWith("On his yes: his day of"))
        }
    }

    @Test
    fun tests1and2_hisDayAndHisShortAnswerTakeTheRoutesTheSentenceSays() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say(hisDay)
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
            assertTrue(mind.talkMessages.last(), mind.talkMessages.last().contains("Places:"))
            say("stiff but free")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.size == 2 }
            assertTrue(mind.talkMessages.last(), !mind.talkMessages.last().contains("Places:"))
        }
    }

    @Test
    fun test9_heTakesACardOffHisHandHimself() {
        Fresh.hatched()
        val ahead = Days.emptyAhead(h)
        (ahead["3"] as JsObj)["1"] = JsObj.of("_" to str("I need to finish the membership form"))
        (ahead["3"] as JsObj)["2"] = JsObj.of("_" to str("I need to reply to Jo"))
        check(app.pages.carryOut(app.pages.planWhole(Days.aheadOf(h), ahead)).ok)
        val before = history().size
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("places-hand")
            compose.waitFor("hand-card", "I need to finish the membership form")
            compose.tap("hand-off:3.1")
            compose.tap("hand-take-off")
            compose.waitUntil(10_000) { Days.handOf(app.storage.read(Days.aheadOf(h))?.let(::parseJson) as? JsObj) == mapOf("3.1" to "I need to reply to Jo") }
            compose.waitUntil(10_000) { history().size == before + 1 }
            assertEquals("On his yes: a card taken off his hand.", history().last())
        }
    }

    @Test
    fun test9_keepYesterdayOnTheDialHasItsOwnHistoryEntry() {
        Fresh.hatched()
        val yesterday = Keep.dayOf(System.currentTimeMillis()).minusDays(1)
        app.waiting.add("Walked the dog in the rain.", yesterday.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli())
        val before = history().size
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("yesterday-offer")
            compose.tap("keep-yesterday")
            compose.waitFor("keep-card", timeoutMs = 20_000)
            compose.tap("day-yes")
            compose.waitFor("kept-shell", timeoutMs = 20_000)
            // On his Pixel on 4 November his ahead was written at 07:27 by this tap with no entry: no conversation had closed.
            compose.waitUntil(10_000) { history().size == before + 1 }
            assertTrue(history().last(), history().last().startsWith("On his yes: his day of"))
        }
    }

    // ── Sundial 0.10.4 ──

    /** His three lines in his own wording, his SHEET step without its last sentence, as his phone held them after 0.10.3. */
    private val hisTalk = "Hand her this line: “The app: talk with me as yourself, warmly, in a few sentences. Speak to him as you, never by his name.”"
    private val hisNoDay = "Hand her this line: “The app: this message is not news of my day. If I seem to be struggling, ask gently whether I'd like to sit with it a moment.”"
    private val hisSheet = "The app: I am keeping today. From my words today, draw my day's sheet in the first person, the whole day and not one thread."

    @Test
    fun test9_hisCookIsBroughtUpAndHisLinesStayHisAndRun() {
        Fresh.hatched()
        val shipped = app.procedures.load(com.phenomemental.pocketbeach.bsp.Cook.SHIPPED_COOK) as JsObj
        val his = parseJson(com.phenomemental.pocketbeach.bsp.stringify(shipped)!!) as JsObj
        fun at(vararg k: String) = k.fold<String, Js?>(his) { n, x -> com.phenomemental.pocketbeach.bsp.jsGet(n, x) } as JsObj
        at("1", "1", "8")["1"] = str(hisTalk)
        at("1", "1", "8", "3", "2")["1"] = str(hisNoDay)
        val sheet = com.phenomemental.pocketbeach.bsp.Cook.places(shipped).getValue("6.1.5")
        val inside = Regex("“(.+?)” On E2B").find(sheet)!!.groupValues[1]
        at("6", "1")["5"] = str(sheet.replace(inside, hisSheet).replace(" Carry it as THE SHEET.", ""))
        check(app.pages.carryOut(app.pages.planWhole(com.phenomemental.pocketbeach.bsp.Cook.cookOf(h), his)).ok)
        val kernel = app.procedures.load(com.phenomemental.pocketbeach.bsp.Cook.SHIPPED_KERNEL) as JsObj
        check(app.pages.carryOut(app.pages.planWhole(com.phenomemental.pocketbeach.bsp.Cook.kernelOf(h), parseJson(com.phenomemental.pocketbeach.bsp.stringify(kernel)!!) as JsObj)).ok)
        java.io.File(app.filesDir, "shipped-${com.phenomemental.pocketbeach.bsp.Cook.SHIPPED_COOK}.json").writeText(com.phenomemental.pocketbeach.bsp.stringify(shipped)!!)
        java.io.File(app.filesDir, "shipped-${com.phenomemental.pocketbeach.bsp.Cook.SHIPPED_KERNEL}.json").writeText(com.phenomemental.pocketbeach.bsp.stringify(kernel)!!)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("corrected", "cook 6.1.5: your own words kept, with the app's form words added around them", timeoutMs = 20_000)
            val now = com.phenomemental.pocketbeach.bsp.Cook.places(parseJson(app.storage.read(com.phenomemental.pocketbeach.bsp.Cook.cookOf(h))!!))
            assertEquals(hisTalk, now["1.1.8.1"])
            assertEquals(hisNoDay, now["1.1.8.3.2.1"])
            assertTrue(now["6.1.5"]!!, now["6.1.5"]!!.contains("“$hisSheet”") && now["6.1.5"]!!.endsWith("Carry it as THE SHEET."))
            compose.waitFor("dial")
            say("stiff but free")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkMessages.isNotEmpty() }
            assertTrue("his TALK line is her recipe", mind.talkInstructions.last().contains("The app: talk with me as yourself, warmly, in a few sentences."))
            assertTrue("his no-day line is handed her", mind.talkMessages.last().contains("If I seem to be struggling, ask gently whether I'd like to sit with it a moment."))
        }
    }

    @Test
    fun test3_aCardFromAnEarlierDayReachesHerWithItsDayNeverAsTodays() {
        Fresh.hatched()
        val stale = "These are the things I need to get done around the membership work and the LERO proposal. First the agreement, then the minutes, then the emails."
        val ahead = Days.emptyAhead(h)
        (ahead["2"] as JsObj)["1"] = JsObj.of("_" to str(stale), "3" to str("2026-11-02T10:02"))
        (ahead["2"] as JsObj)["2"] = JsObj.of("_" to str("I'm going to finish the membership form"), "3" to str(com.phenomemental.pocketbeach.bsp.isoString(System.currentTimeMillis())))
        check(app.pages.carryOut(app.pages.planWhole(Days.aheadOf(h), ahead)).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            say("What have I got in front of me?")
            compose.waitUntil(20_000) { app.asking.settled && mind.talkInstructions.isNotEmpty() }
            val window = mind.talkInstructions.last()
            assertTrue(window, window.contains("From earlier days, not today's: “These are the things I need to get done around the membership work and the LERO proposal.” (from Monday 2 November"))
            assertTrue(window, window.contains("Today: “I'm going to finish the membership form”"))
            assertTrue("shortened to its first sentence", !window.contains("First the agreement"))
        }
    }
}
