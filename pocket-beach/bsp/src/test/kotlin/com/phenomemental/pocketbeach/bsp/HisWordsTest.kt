package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 0.6.1, from his first test of 0.6 on the Pixel (29 October, 17:37): every
 * line he is shown is his words, nothing invented, nothing dropped; her reply
 * never plays her turn back; the phone cuts his speech by his sentences and
 * never before a name it knows.
 */
class HisWordsTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))

    /** What he said, as the phone heard it ("torque" is "talk"). */
    private val ask = Bench.GEMMA_ASK

    /** Her reply on his Pixel, verbatim. */
    private val herReply = """
        Shell: "Pocket companion. I live on this phone, read freely, and write only when Matthew says yes."
        It's Thursday, October 29, 2026, 17:37.
        Matthew: $ask
        What would you like to keep in your blocks?
    """.trimIndent()

    @Test
    fun herReplyNeverPlaysHerTurnBack() {
        val copied = Talking.copied(herReply, ask, HelloWorld.SHELL_OPENING)
        assertEquals(3, copied.size, copied.joinToString("\n"))
        assertEquals("What would you like to keep in your blocks?", Talking.unechoed(herReply, ask, HelloWorld.SHELL_OPENING))
        val answer = "Gemma it is, then. A tool you can just talk at, and it keeps up with you: that's what I'm here for."
        assertEquals(emptyList(), Talking.copied(answer, ask, HelloWorld.SHELL_OPENING), "an answer is no copy")
        assertEquals(answer, Talking.unechoed(answer, ask, HelloWorld.SHELL_OPENING))
        assertTrue(Talking.copied("It is Thursday 29 October 2026, 17:37.", ask, HelloWorld.SHELL_OPENING).isNotEmpty())
        assertTrue(Talking.copied("I live on your phone, read freely, and write only when you say yes.", ask, HelloWorld.SHELL_OPENING).isNotEmpty(), "her shell in other words is still her shell")
        assertEquals(emptyList(), Talking.copied("Matthew, you want to call me Gemma? Okay, I am Gemma.", ask, HelloWorld.SHELL_OPENING), "speaking to him by name is no copy")
        assertEquals(emptyList(), Talking.copied("Good morning, Matthew. What's on your mind?", "Good morning", HelloWorld.SHELL_OPENING), "a greeting returned is no copy")
        assertEquals(listOf("Good morning."), Talking.copied("Good morning.\nHow are you?", "Good morning", HelloWorld.SHELL_OPENING), "his few words alone on a line are")
    }

    @Test
    fun herTimeAndShellAreInHerInstructionAndHisTurnIsHisWordsAlone() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        val now = java.time.ZonedDateTime.of(2026, 10, 29, 17, 37, 0, 0, java.time.ZoneId.of("Europe/London"))
        val i = Talking.instruction(HelloWorld.SHELL, pages, now)
        assertContains(i, "It is Thursday 29 October 2026, 17:37. You wear the shell ${HelloWorld.SHELL}; it says \"${HelloWorld.SHELL_OPENING}\"")
        assertFalse(i.contains("Each of his messages comes with"), "0.6's line that had her open with the date and her shell")
        assertFalse(i.contains("Hello world:"), "the hello world line is the Write job's")
        assertFalse(i.contains("The blocks in the pocket"), "the pocket set out had her answer about her blocks, not him")
        assertFalse(i.contains("What the app read for you"), "his words name no block")
        assertEquals(ask, Talking.said(ask))
        // A block his words name is read for her.
        val named = Talking.instruction(HelloWorld.SHELL, pages, now, "What's in my garden block?")
        assertContains(named, "What the app read for you, of the blocks his words name:")
        assertContains(named, "garden")
        assertContains(Talking.instruction(HelloWorld.SHELL, pages, now, "What blocks do I have?"), "The blocks in the pocket", message = "asked about his blocks, she is shown them")
    }

    /** The bench's two fixed asks for the Talk job, judged on her reply and the Write job's proposal. */
    @Test
    fun theBenchMarksAnAnswerAndAPlayback() {
        val (gemma, named) = Bench.TALK_ASKS
        assertTrue(Bench.judgeTalk(gemma, herReply, null).any { it.startsWith("She played her turn back") }, "her reply on his Pixel is wrong")
        assertEquals(emptyList(), Bench.judgeTalk(gemma, "Gemma it is, then. Tell me more about this tool.", null))
        assertTrue(Bench.judgeTalk(gemma, "That sounds lovely. What else is on your mind?", null).any { it.startsWith("She didn't answer him") })
        for (took in listOf("Hello Matthew. I am Gemma.", "You can call me Gemma.", "Okay, I will be Gemma.", "Gemma it is.", "I'll be called Gemma now.")) assertTrue(Bench.takesName(took, "Gemma"), took)
        for (not in listOf("It's nice to meet you, Gemma.", "Hello, Gemma.", "Okay, Matthew. I will call you Gemma.", "Matthew wants me to call him Gemma.")) assertFalse(Bench.takesName(not, "Gemma"), not)
        val (storage, pages) = Bench.fixture(shelf, 1_793_178_000_000L, Talks.SHELL_RECIPE)
        val acts = Acts.parse("add ${HelloWorld.SHELL}: Matthew calls me Gemma.")
        val proposal = Acts.propose("", acts, storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), 1_793_178_000_000L).proposal
        assertTrue(pages.exists(HelloWorld.SHELL))
        assertEquals(emptyList(), Bench.judgeTalk(named, "Gemma it is.", proposal))
        assertTrue(Bench.judgeTalk(named, "Gemma it is.", null).any { it.contains("on her shell") })
        val opening = Acts.propose("", Acts.parse("change ${HelloWorld.SHELL}: Matthew calls me Gemma."), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), 1_793_178_000_000L).proposal
        assertTrue(Bench.judgeTalk(named, "Gemma it is.", opening).any { it.contains("change her shell's opening") }, "her shell's opening replaced is no line on it")
    }

    /** E2B's Write job wrote the shell line with no way word before it: an act all the same; plain words stay talk. */
    @Test
    fun actionsAloneAreAnAct() {
        val named = Talks.reply("add ${HelloWorld.SHELL}: Matthew calls me Gemma")
        assertEquals(Talks.Route.ACT, named.route)
        assertEquals("add ${HelloWorld.SHELL}: Matthew calls me Gemma", named.rest)
        assertEquals(Talks.Route.TALK, Talks.reply("It's nice to meet you, Gemma. That sounds like an interesting project.").route)
        assertEquals(Talks.Route.READ, Talks.reply("read garden").route, "a read alone is still a read")
    }

    @Test
    fun hisSpeechIsCutNeverBeforeANameAndNoPieceIsUnderThreeWords() {
        assertEquals(
            listOf(
                "Hey I'm quite happy to call you Gemma I like Gemma",
                "one of the main things that I'm trying to produce is a tool that I can just waffle on to torque and speak",
                "highly complex verbal slop really",
            ),
            Telling.cut(ask, setOf("gemma")),
        )
        for (said in listOf(ask, Telling.WRITTEN, Telling.SPOKEN, Telling.SECOND)) for (piece in Telling.cut(said, setOf("gemma"))) {
            val counted = Telling.plain(piece).split(' ').count { it.isNotEmpty() && it !in setOf("and", "but", "so", "anyway", "right", "well") }
            assertTrue(counted >= 3, "under three words: “$piece”")
            assertTrue(Telling.foundIn(piece, said), "not his words: “$piece”")
        }
        assertFalse(Landing.isTelling(ask, emptyMap(), setOf("gemma")), "his message is one run of his words, not a day")
    }

    @Test
    fun thePocketKnowsHerNameHisBlocksAndTheNamesInThem() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        pages.carryOut(pages.planNewBlock("friends"))
        pages.carryOut(pages.planAdd("friends", "", "Lunch with Jo and Sam on Friday"))
        val names = Landing.names(pages)
        for (n in listOf("gemma", "garden", "million", "friends", "jo", "sam", "friday")) assertTrue(n in names, "$n in $names")
        assertFalse("lunch" in names, "a line's first word is no name")
    }

    @Test
    fun withHisMessageNoRungChanges() {
        // As she judged it on his Pixel: what he felt, then what he made of it.
        val judged = mapOf("Hey I'm quite happy to call you Gemma I like Gemma" to "felt")
        val (placed, steps) = Telling.sorted(ask, Telling.BENCH_DAY, Whens.first(), Telling.shape(), { judged[it] ?: "thought" }, { _, _ -> false }, setOf("gemma"))
        assertEquals(emptyMap(), Telling.rungChoices(steps), "nothing he said bears on a rung")
        var asked = false
        assertEquals(emptyList(), Telling.rungsFor(Telling.shape(), steps) { asked = true; "3.1: 1" })
        assertFalse(asked, "she isn't asked about a rung nothing bears on")
        assertTrue(placed.none { it.place in Telling.RUNGS })
    }

    @Test
    fun aRungIsOneOfHisPiecesLiftedOrLeftAsItStood() {
        val words = Telling.cut(Telling.WRITTEN).associateWith { "happened" }
        val (_, steps) = Telling.sorted(Telling.WRITTEN, Telling.BENCH_DAY, Whens.first(), Telling.shape(), { words.getValue(it) }, { _, _ -> false })
        val choices = Telling.rungChoices(steps)
        assertEquals(listOf("Dentist on Thursday", "I've decided I'm ringing the garage tomorrow"), choices["3.1"], "this week and tomorrow reach this week's rung, in his order")
        assertTrue("Before the frost comes I want the garden shut down properly for once" in choices["3.3"].orEmpty())
        val asked = Telling.rungsAsk(Telling.shape(), choices)
        assertContains(asked, "or none to leave the rung as it stands")
        // She lifts one, leaves one, and names a piece one rung wasn't offered: only the piece lifted lands.
        val rungs = Telling.rungLines("3.1: 1\n3.2: none\n3.3: 2", choices)
        assertEquals(listOf(Telling.Placed("3.1", null, "Dentist on Thursday")), rungs)
        assertTrue(rungs.all { Telling.foundIn(it.words, Telling.WRITTEN) })
    }

    /** Every line a proposal shows is his words; his own corrections are the one exception. */
    @Test
    fun everyLineShownIsHisWordsOrHisCorrection() {
        val placed = Telling.placed(
            """
            1.1: the car wouldn't start again this morning
            1.2: I felt embarrassed
            2.1: ring the garage
            3.1: I intend to produce a tool that I can just waffle on to.
            3.2: Jo's birthday is on the twelfth
            """.trimIndent(),
        )
        val sorting = Landing.sorting(Telling.WRITTEN, placed)
        for (line in sorting.lines.filter { it.place != null }) assertTrue(Telling.foundIn(line.words, Telling.WRITTEN), "not his words: ${line.place} “${line.words}”")
        assertEquals(3, sorting.invented.size, "her own words are left out, the rung's among them")
        val corrected = Landing.sorting(ask, Telling.placed("1.3: a tool that I can just waffle on to torque and speak")).lines.first().correctedTo("a tool that I can just waffle on to talk and speak")
        assertTrue(corrected.corrected)
        assertFalse(Telling.foundIn(corrected.words, ask), "his correction isn't what the phone heard")
    }

    @Test
    fun hisCorrectionOfAMisheardWordLandsAsHisWords() {
        val storage = MemoryStorage()
        val nowMs = Instant.parse("2026-10-29T17:37:00Z").toEpochMilli()
        val pages = Pages(storage, shelf, PocketBsp(storage, shelf) { nowMs }) { nowMs }
        val heard = "one of the main things that I'm trying to produce is a tool that I can just waffle on to torque and speak"
        val line = Landing.Line("1.3", null, heard).correctedTo(heard.replace("torque", "talk"))
        val landed = Landing.landed(listOf(line), ask, nowMs) { n -> storage.read(n)?.let(::parseJson) }
        for (p in Landing.plans(landed, pages)) assertTrue(pages.carryOut(p).ok)
        val day = pages.position(Region.POCKET, Landing.NOW, Landing.dayAddress(nowMs))!!.text!!
        assertContains(day, "waffle on to talk and speak")
        assertFalse(day.contains("torque"), "the day's line carries his correction")
        assertContains(storage.read(Landing.NOW)!!, "waffle on to talk and speak")
    }
}
