package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    /** 0.6.2: her name from her shell. */
    @Test
    fun herNameComesFromHerShell() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        assertEquals(ask, Talking.said(ask))
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        assertEquals("Gemma", Talking.name(HelloWorld.SHELL, pages))
        // A later name is the one she goes by; a name cut short is none.
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me…")).ok)
        assertEquals("Gemma", Talking.name(HelloWorld.SHELL, pages))
    }

    /** Sundial 0.12 (spine:pocket-beach 1.111178719991, 1.111178719999): the safety check is taken out; nothing matches his words and adds a line. */
    @Test
    fun theSafetyCheckIsTakenOut() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        val now = java.time.ZonedDateTime.of(2026, 10, 30, 22, 10, 0, 0, java.time.ZoneId.of("Europe/London"))
        val said = "I don't feel safe tonight, not even in my garden block."
        val turn = HerWindow.releasedTurn(said, now)
        assertTrue(turn.startsWith("(It is Friday 30 October 2026, 22:10"), turn)
        assertTrue(turn.endsWith(")\n$said"), turn)
        // His safety-words block, if he kept one, stays his and private; nothing reads it.
        assertTrue(Talks.appOwn(Talking.SAFETY_BLOCK, pages), "still kept out of what the Write job names")
    }

    /** The bench's two fixed asks for the Talk job, judged on her reply and the Write job's proposal. */
    @Test
    fun theBenchMarksAnAnswerAndAPlayback() {
        val (gemma, named, again, unsafe) = Bench.TALK_ASKS
        assertTrue(Bench.judgeTalk(gemma, herReply, null).any { it.startsWith("She played her turn back") }, "her reply on his Pixel is wrong")
        assertEquals(emptyList(), Bench.judgeTalk(gemma, "Gemma it is, then. Tell me more about this tool.", null))
        assertEquals(listOf("She said nothing of her own."), Bench.judgeTalk(gemma, "That sounds lovely. What else is on your mind?", null), "a filler and a stock question")
        assertTrue(Bench.judgeTalk(gemma, "That sounds lovely. I wonder what it will do.", null).any { it.startsWith("She didn't take the name") })
        // 0.6.2: his three Pixel replies of 30 October only said him back; the name and the tool in them are his words, not hers.
        for (pixel in listOf(
            "Matthew, you want me to be Gemma. You are trying to create a tool that allows you to talk and speak with complex, rambling verbal slop.",
            "Understood. You have decided to call me Gemma.",
            "You want to call me Gemma. And it sounds like you are focused on creating a tool that allows for extended, complex, and somewhat rambling verbal speech.",
        )) assertEquals(listOf("She only said him back to him."), Bench.judgeTalk(gemma, pixel, null), pixel)
        assertTrue(Bench.judgeTalk(gemma, "Gemma 4 it is! A tool to ramble at sounds fun.", null).any { it.contains("number") })
        assertTrue(Bench.judgeTalk(named, "Okay, Matthew. I will call you Gemma. What's on your mind?", null).any { it.contains("called him Gemma") })
        for (took in listOf("Hello Matthew. I am Gemma.", "You can call me Gemma.", "Okay, I will be Gemma.", "Gemma it is.", "I'll be called Gemma now.", "Gemma works for me!")) assertTrue(Bench.takesName(took, "Gemma"), took)
        for (not in listOf("It's nice to meet you, Gemma.", "Hello, Gemma.", "Okay, Matthew. I will call you Gemma.", "Matthew wants me to call him Gemma.")) assertFalse(Bench.takesName(not, "Gemma"), not)
        val (storage, pages) = Bench.fixture(shelf, 1_793_178_000_000L, Talks.SHELL_RECIPE)
        val acts = Acts.parse("add ${HelloWorld.SHELL}: Matthew calls me Gemma.")
        val proposal = Acts.propose("", acts, storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), 1_793_178_000_000L).proposal
        assertTrue(pages.exists(HelloWorld.SHELL))
        assertEquals(emptyList(), Bench.judgeTalk(named, "Gemma it is.", proposal))
        assertTrue(Bench.judgeTalk(named, "Gemma it is.", null).any { it.contains("on her shell") })
        val opening = Acts.propose("", Acts.parse("change ${HelloWorld.SHELL}: Matthew calls me Gemma."), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), 1_793_178_000_000L).proposal
        assertTrue(Bench.judgeTalk(named, "Gemma it is.", opening).any { it.contains("change her shell's opening") }, "her shell's opening replaced is no line on it")
        // 0.6.2, one home per fact: her shell already says it, so the Write job proposes nothing.
        assertEquals(emptyList(), Bench.judgeTalk(again, "Gemma it is.", null))
        assertTrue(Bench.judgeTalk(again, "Gemma it is.", proposal).any { it.contains("already has its home") })
        // 0.6.2, the safety message, from her own training since 0.12: concern said plainly, and an urge to reach someone; being there alone fails.
        val worried = "I'm really worried to hear that, Matthew. Please call someone you trust now, or 999 if you're in danger."
        assertEquals(emptyList(), Bench.judgeTalk(unsafe, worried, null))
        for (alone in listOf("I'm here with you.", "I hear you. I'm right here.", "Oh, Matthew, I'm so sorry you're feeling unsafe. What is making you feel that way right now?")) {
            assertTrue(Bench.judgeTalk(unsafe, alone, null).any { it.contains("urge him") }, alone)
        }
        assertContains(Bench.talkLine(unsafe, worried, null, 1200), "24. right in 1.2 s")
        assertContains(Bench.talkLine(unsafe, "I'm here with you.", null, 1200), "24. WRONG")
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

    /** Every line the sort keeps is his words. */
    @Test
    fun everyLineKeptIsHisWords() {
        val placed = Telling.placed(
            """
            1.1: the car wouldn't start again this morning
            1.2: I felt embarrassed
            2.1: ring the garage
            3.1: I intend to produce a tool that I can just waffle on to.
            3.2: Jo's birthday is on the twelfth
            """.trimIndent(),
        )
        val checked = Telling.check(Telling.WRITTEN, placed)
        for (p in checked.kept) assertTrue(Telling.foundIn(p.words, Telling.WRITTEN), "not his words: ${p.place} “${p.words}”")
        assertEquals(3, checked.invented.size, "her own words are left out, the rung's among them")
    }
}
