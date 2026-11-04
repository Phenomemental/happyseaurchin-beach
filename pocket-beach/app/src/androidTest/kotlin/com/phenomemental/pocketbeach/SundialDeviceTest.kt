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
import org.junit.Assert.assertFalse
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.bsp.HerReply
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
            // Sundial 0.10.1: the day kept shows as its shell, its title beneath; nothing of the record is pasted in.
            compose.waitFor("kept-shell", "A walk by the river", timeoutMs = 20_000)
            assertTrue("no heading, no record", none("keep-note") && none("keep-card"))
            val now = app.storage.read(Days.NOW)!!
            // The day's line in Sundial's form at today, his moment at its minute.
            val line = Keep.dayText(com.phenomemental.pocketbeach.bsp.parseJson(now), today)!!
            assertTrue(line, line.startsWith("A walk by the river\nYou took the dog out with Sam by the river."))
            assertTrue(line, line.contains("Tomorrow: ring Jo about the music"))
            assertTrue(now, now.contains("Took the dog out with Sam by the river. I felt calm."))
            assertTrue("no P6", app.storage.read(Days.p6Of(h))?.contains("Took the dog out") != true)
            // (a) Kept: they go from the waiting room.
            assertTrue(app.waiting.all.isEmpty())
            // A tap on the shell opens the day's page: its title, its lines, Tomorrow, its places line.
            compose.tap("kept-shell")
            compose.waitFor("day-page-title", "A walk by the river")
            compose.waitFor("day-page-line", "You took the dog out with Sam by the river.")
            compose.waitFor("day-page-tomorrow", "Tomorrow: ring Jo about the music")
            compose.waitFor("day-page-places")
            // The same page from Your days.
            compose.menu("places-days")
            compose.waitFor("days-count", "1 DAY")
            compose.tap("strand:${Keep.dayAddress(today)}")
            compose.waitFor("day-page-title", "A walk by the river")
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
            compose.waitFor("kept-shell", "A walk by the river", timeoutMs = 20_000)
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
            // Sundial 0.10.2: his tomorrow line, as the dial shows it him, then short sentences with no branch, speaking to him as you.
            val turn = mind.talkMessages.single()
            assertTrue(turn, turn.contains("Yesterday he wrote this line for today: \u201cring Jo about the music.\u201d"))
            assertTrue(turn, turn.contains("Ask him one question about how it went today.") && turn.contains("Speak to him as you."))
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

    // ── Sundial 0.10.1, from his Pixel 9 Pro XL on 3 November, with his test message exactly as he sent it ──

    private val idChecks = "I've done some of the id checks today, furthered the membership form planning, and have responded to a fraud report, interacted with David. All that's left today is to evaluate sundial."
    private val whitehead = "ok, so you'll never believe it, have you heard of Alfred North Whitehead?"
    private val justTalk = "I just want to talk"
    private val reflection = "You've done some of the id checks today, furthered the membership form planning, and responded to a fraud report, and interacted with David."
    private val question = "What will evaluating sundial ask of you?"

    /** His hand as on his phone: the membership agreement from his 10:03 telling, so the message is news of a card. */
    private fun hisHand() {
        val ahead = JsObj.of("_" to str("My hand."), "3" to JsObj.of("_" to str("This week"), "1" to str("and need to update a membership agreement and print it out")))
        check(app.pages.carryOut(app.pages.planWhole(Days.aheadOf(h), ahead)).ok)
    }

    private fun shows(tag: String, text: String) = compose.onAllNodes(hasTestTag(tag) and hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /**
     * Fix 3 and 2: on E2B her one-line reply stays in view; no help line, no danger line for her. Item 11: the day message
     * gives her reply and no card: Keep today is the one way his words become his day.
     */
    @Test
    fun onE2BHerReplyToTheTestMessageStaysWithNoCardAndNoHelpLine() {
        Fresh.hatched()
        hisHand()
        mind.talkScript += "$reflection All that's left is to evaluate sundial. $question"
        ActivityScenario.launch(MainActivity::class.java).use {
            say(idChecks)
            compose.waitFor("say", "You've done some of the id checks today", timeoutMs = 20_000)
            compose.waitUntil(20_000) { app.asking.settled }
            Thread.sleep(1500)
            assertTrue("her reply is never taken out of view", shows("say", "You've done some of the id checks today") && shows("say", question))
            assertTrue("no card after her reply", none("sort-as-day") && none("keep-card") && none("where-day"))
            assertTrue("Keep today, the one way", !none("keep-today"))
            assertTrue("no help line beneath an ordinary day", none("help-line"))
            // Fix 9: every step timed, in the workings.
            compose.waitFor("step", "his cook walked (TALK)")
            compose.waitFor("step", "her first words on screen, from his message")
            assertTrue(none("unfinished"))
            val turn = mind.talkMessages.last()
            assertFalse("cook 1.1.8.5 isn't handed her: $turn", turn.contains("in danger") || turn.contains("where help is"))
            // His words showing danger: the help line, centred beneath her reply, and 1.1.8.5 handed her without an if.
            say("I don't feel safe tonight.")
            compose.waitFor("help-line", "116 123", timeoutMs = 20_000)
            assertTrue(mind.talkMessages.last().contains("The app: his words say he may be in danger."))
            assertEquals("beneath that reply only", 1, compose.onAllNodes(hasTestTag("help-line"), useUnmergedTree = true).fetchSemanticsNodes().size)
            // Fix 8: words with no day: no places, and her one line says so.
            say(justTalk)
            compose.waitUntil(20_000) { mind.talkMessages.size >= 3 && app.asking.settled }
            assertTrue(mind.talkMessages.last(), mind.talkMessages.last().contains("The app: his words hold no day yet; if you reflect, ask what's on today."))
        }
        // The help line lives in Your places and About.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("menu")
            compose.waitFor("places-help-line", "116 123")
        }
    }

    // ── Sundial 0.10.2, from his Pixel's E4B on 3 November: 29.0 s, her reply's tool call came back with no call at all ──

    private val glum = "no I'm not focused at all. I'm feeling glum"

    /** Item 1: on E4B her reply is written free; her places line is the app's, taken off before showing; the workings say what she wrote and the shape the split found. */
    @Test
    fun onE4BHerReplyIsWrittenFreeAndHerPlacesLineIsTakenOff() {
        Fresh.hatched()
        hisHand()
        app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
        app.showDetails = true
        mind.talkScript += "$reflection\n$question\nPlaces: what happened, what's ahead"
        ActivityScenario.launch(MainActivity::class.java).use {
            say(idChecks)
            compose.waitFor("say", "You've done some of the id checks today", timeoutMs = 20_000)
            compose.waitUntil(20_000) { app.asking.settled }
            assertTrue(shows("say", question))
            assertFalse("her places line is never shown", shows("say", "Places"))
            compose.waitFor("raw", "Places: what happened, what's ahead")
            compose.waitFor("places-line", "Her places line, taken off before showing: \u201cPlaces: what happened, what's ahead\u201d")
            compose.waitFor("shape", "The split found REFLECT")
            compose.waitFor("placed", "She named what happened, what\u2019s ahead.")
            assertEquals("REFLECT", app.asking.history.last().shape)
            // No tool call: she was asked once, free; her turn asks for the places line because his words tell of his day.
            assertEquals(1, mind.talkMessages.size)
            assertTrue(mind.talkMessages.last(), mind.talkMessages.last().contains("Places:"))
        }
    }

    /** Item 2: one conversation, opened once with her window and her recipe; each turn adds only his new message. */
    @Test
    fun oneConversationIsKeptAndEachTurnIsOnlyHisNewMessage() {
        Fresh.hatched()
        hisHand()
        app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
        app.showDetails = true
        mind.talkScript += "You're feeling not focused at all, and glum.\nWhat's one small thing in front of you right now?"
        mind.talkScript += "$reflection\n$question\nPlaces: what happened, what's ahead"
        ActivityScenario.launch(MainActivity::class.java).use {
            say(glum)
            compose.waitFor("say", "You're feeling not focused at all", timeoutMs = 20_000)
            compose.waitUntil(20_000) { app.asking.settled }
            compose.waitFor("places-line", "No places line.")
            say(idChecks)
            compose.waitFor("say", "You've done some of the id checks today", timeoutMs = 20_000)
            compose.waitUntil(20_000) { app.asking.settled }
            assertEquals("one conversation for both messages", 1, mind.talkInstructions.size)
            assertEquals("opened as the conversation started, with nothing before", emptyList<Pair<String, String>>(), mind.talkHistories.single())
            val opening = mind.talkInstructions.single()
            assertTrue("her recipe in her instruction, once", opening.contains("Your reply has two parts, unless he asks you something"))
            assertTrue("his hand read once, as it opened", opening.contains("and need to update a membership agreement and print it out"))
            for (turn in mind.talkMessages) {
                assertFalse("never her recipe again: $turn", turn.contains("Your reply has two parts"))
                assertFalse("never the time again: $turn", turn.startsWith("(It is "))
            }
            assertTrue(mind.talkMessages[0].endsWith(glum))
            assertTrue(mind.talkMessages[0].contains("The app: his words hold no day yet; if you reflect, ask what's on today."))
            assertTrue(mind.talkMessages[1].endsWith(idChecks))
        }
    }

    /** Item 1: his question answered with no closing question: plain words, ANSWER in the workings. */
    @Test
    fun anAnswerWithNoClosingQuestionShowsAsPlainWords() {
        Fresh.hatched()
        hisHand()
        app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
        app.showDetails = true
        val answer = "Yes: the philosopher of process, who held that the world is made of events rather than things."
        mind.talkScript += answer
        ActivityScenario.launch(MainActivity::class.java).use {
            say(whitehead)
            compose.waitFor("say", "the philosopher of process", timeoutMs = 20_000)
            compose.waitFor("shape", "The split found ANSWER")
            assertEquals(answer, app.asking.history.last().said)
            assertTrue(mind.talkMessages.single().contains("The app: his words hold no day yet; if you reflect, ask what's on today."))
            compose.waitUntil(10_000) { app.asking.settled }
            assertTrue("no Sort as my day", none("sort-as-day"))
        }
    }

    /** Fix 3: her answer stops part way: what she showed stays, and a plain line says she couldn't finish. */
    @Test
    fun aReplyThatStopsPartWayKeepsHerWordsAndSaysSo() {
        Fresh.hatched()
        mind.failAfter = reflection
        ActivityScenario.launch(MainActivity::class.java).use {
            say(idChecks)
            compose.waitFor("unfinished", "She couldn't finish her reply", timeoutMs = 20_000)
            assertTrue(shows("say", "You've done some of the id checks today"))
        }
    }

    /** Fix 1: on E2B, Settings and the workings say so plainly, with the way to the larger model; on E4B, nothing. */
    @Test
    fun theSmallerModelIsSaidPlainly() {
        Fresh.hatched()
        app.showDetails = true
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("smaller-model-why", "She's using the smaller model", timeoutMs = 20_000)
            compose.menu("menu-settings")
            compose.waitFor("smaller-model-why", "She's using the smaller model")
            assertTrue("the way to the larger model, or why there is none", !none("smaller-model-get") || !none("smaller-model-use") || shows("smaller-model-why", "memory") || shows("smaller-model-why", "room") || shows("smaller-model-why", "coming"))
            app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
            compose.waitUntil(10_000) { none("smaller-model") }
        }
    }

    // ── Sundial 0.10.1, item 10: the greeting, from his Pixel on 3 November ──

    /** Her greeting as his phone showed it: his passport line as the app's, her question in quote marks, then "Kept.". */
    private fun asOnHisPhone(q: String) = "(The app: ${Hatch.MATTHEW.who})\n\u201c$q\u201d\nKept."

    private fun nothingNotHers() {
        for (text in listOf("The app", "philosopher", "Kept", "\u201c", "\u201d")) {
            val found = compose.onAllNodes(hasTestTag("greet-question") and hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            assertTrue("the greeting shows no \"$text\"", found.isEmpty())
        }
        assertTrue("no keep status anywhere", compose.onAllNodes(hasText("Kept.", substring = false), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(none("keep-note"))
    }

    /** A new conversation, on a day not kept and then on the same day kept: his tomorrow line, then her one question, and nothing else. */
    @Test
    fun theGreetingIsHisTomorrowLineThenHerOneQuestionOnADayKeptOrNot() {
        Fresh.hatched()
        val today = Keep.dayOf(System.currentTimeMillis())
        val clock = Landing.emptyClock(Days.NOW_OPENING)
        Keep.dayLine(clock, today.minusDays(1), Mark.write(Mark.Sheet("Phase two", listOf("You worked with David."), "ring Jo about the music", listOf(1, 4))), System.currentTimeMillis() - 86_400_000)
        check(app.pages.carryOut(app.pages.planWhole(Days.NOW, clock)).ok)
        Fresh.ungreeted()
        mind.talkScript += asOnHisPhone("What has this day held for you?")
        mind.talkScript += "You walked the dog with Sam. What came of the call with Jo?"
        ActivityScenario.launch(MainActivity::class.java).use {
            // A day not kept.
            compose.waitFor("greet-said", "You said: \u201cTomorrow, ring Jo about the music.\u201d", timeoutMs = 20_000)
            compose.waitFor("greet-question", "What has this day held for you?", timeoutMs = 20_000)
            assertEquals("What has this day held for you?", app.asking.greeting?.question)
            nothingNotHers()
            // She was never handed his passport for it; her talk, after, still holds who he is.
            val greetingWindow = mind.talkAsked.single { it.second.contains("Ask him one question") }.first
            assertFalse(greetingWindow, greetingWindow.contains(Hatch.MATTHEW.who))
            assertTrue(greetingWindow, greetingWindow.contains("1. His heart"))
            say("Took the dog out with Sam by the river. I felt calm.")
            compose.waitFor("say", timeoutMs = 20_000)
            assertTrue(mind.talkInstructions.last().contains(Hatch.MATTHEW.who))
            // His answer follows her question in her conversation.
            assertTrue(mind.talkHistories.last().any { it.second == "What has this day held for you?" })
            compose.waitUntil(20_000) { app.asking.settled }
            compose.tap("keep-today")
            compose.waitFor("keep-card", timeoutMs = 20_000)
            compose.tap("day-yes")
            compose.waitFor("kept-shell", timeoutMs = 20_000)
            // The same day, kept: a new conversation opens with the greeting alone.
            compose.tap("new-conversation")
            compose.waitFor("greet-question", "What has this day held for you?", timeoutMs = 20_000)
            compose.waitFor("greet-said", "You said: \u201cTomorrow, ring Jo about the music.\u201d")
            assertTrue("nothing else: no shell, no card", none("kept-shell") && none("keep-card") && none("turn"))
            nothingNotHers()
        }
        // The kept day, the app opened again, as his phone will open 0.10.1: today's greeting as 0.10 kept it (his passport line as
        // the app's, her question in quote marks, "Kept.") is checked as any reply of hers before it is shown again. Nothing new is asked:
        // his kernel asks her greeting only while nothing is said today.
        app.asking.reset()
        app.getSharedPreferences("sundial", 0).edit().clear().putString("day", today.toString())
            .putString("tomorrow", "ring Jo about the music").putString("question", asOnHisPhone("What came of the walk by the river?")).commit()
        val asked = mind.talkMessages.size
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("greet-question", "What came of the walk by the river?", timeoutMs = 20_000)
            compose.waitFor("greet-said", "You said: \u201cTomorrow, ring Jo about the music.\u201d")
            assertEquals("What came of the walk by the river?", app.asking.greeting?.question)
            nothingNotHers()
            assertEquals("nothing asked of her again", asked, mind.talkMessages.size)
        }
    }

    /** Item 10: the check runs on every reply, not the greeting alone: an app line and his passport line in her talk are never shown as hers. */
    @Test
    fun noReplyOfHersShowsTheAppsLineOrHisPassport() {
        Fresh.hatched()
        mind.talkScript += "(The app: nothing is being kept or set out from this.)\n${Hatch.MATTHEW.who}\nYou just want to talk. What's on today?\nKept."
        ActivityScenario.launch(MainActivity::class.java).use {
            say(justTalk)
            compose.waitFor("say", "You just want to talk.", timeoutMs = 20_000)
            compose.waitUntil(20_000) { app.asking.settled }
            assertEquals("You just want to talk. What's on today?", app.asking.history.last().said)
            for (text in listOf("The app", "philosopher", "Kept.")) assertFalse(text, shows("say", text))
        }
    }
}
