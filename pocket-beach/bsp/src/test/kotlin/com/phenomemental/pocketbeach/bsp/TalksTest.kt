package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 0.4's asking: the route word, what can be shown while she writes, and her instruction. */
class TalksTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))

    @Test
    fun theFirstWordIsTheRouteAndAnythingElseIsTalk() {
        assertEquals(Talks.Route.TALK to "I'm the pocket companion.", Talks.route("talk\nI'm the pocket companion."))
        assertEquals(Talks.Route.ACT to "add garden: compost", Talks.route("  Act\nadd garden: compost"))
        assertEquals(Talks.Route.READ to "read garden", Talks.route("**read**\nread garden"))
        assertEquals(Talks.Route.TALK to "Hello there.", Talks.route("talk: Hello there."))
        assertEquals(Talks.Route.TALK to "Actually, I can't say.", Talks.route("Actually, I can't say."), "a word that begins like a route isn't one")
        assertEquals(Talks.Route.TALK to "Sure, here's a plan.", Talks.route("Sure, here's a plan."))
    }

    /** Her replies as she wrote them on the laptop: a few words, then the route she meant. */
    @Test
    fun aReplyIsReadWhole() {
        val act = Talks.reply("talk\nI will add the line to the garden block.\nact\nadd garden: buy more compost")
        assertEquals(Talks.Route.ACT, act.route)
        assertEquals("I will add the line to the garden block.", act.words)
        assertEquals("add garden: buy more compost", act.rest)
        val stray = Talks.reply("talk\nYes, there is.\nread garden\n<block>Winter: prune the roses</block>\ntalk\nIt says prune the roses.")
        assertEquals(Talks.Route.READ, stray.route, "a read line among her words is a read")
        assertEquals("read garden", stray.rest)
        val imagined = Talks.reply("read\n<garden>\nThe garden, through the year\n</garden>")
        assertEquals(Talks.Route.READ, imagined.route)
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        assertEquals(listOf(Acts.Action.Read("garden", null)), Talks.reads(imagined.rest, "Read me the autumn line in garden.", pages), "what she imagined isn't read; the block named is")
        assertEquals(listOf(Acts.Action.Read("garden", "autumn")), Talks.reads("read garden \"autumn\"\nread nowhere", "", pages))
        assertEquals(listOf(Acts.Action.Read("whetstone", null)), Talks.reads("read whetstone", "", pages), "a teaching block")
        assertEquals(listOf(Acts.Action.Read("sea at night", null)), Talks.reads("read sea at night", "Tell me about the sea at night.", pages), "a block that isn't there is read, and says so")
        assertEquals(Talks.Route.TALK, Talks.reply("talk\nYou're welcome.\nact\n").route, "an act with nothing after it is talk")
        assertEquals("You're welcome.", Talks.reply("talk\nYou're welcome.\nact\n").words)
    }

    @Test
    fun whatHeTellsThatNamesABlockIsOfferedForKeeping() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        assertEquals("Million", Talks.keepIn("I'm going to make a million pound by creating tech that changes the world, before the end of 2027.", pages), "whatever the capitals")
        assertEquals("garden", Talks.keepIn("The garden gate needs a new latch.", pages))
        assertNull(Talks.keepIn("What's in my garden block?", pages), "a question")
        assertNull(Talks.keepIn("Add to garden: compost.", pages), "an ask to write, hers to plan")
        assertNull(Talks.keepIn("Good morning! The garden looks lovely.", pages), "a greeting")
        assertNull(Talks.keepIn("I'm going sailing.", pages), "no block named")
        assertNull(Talks.keepIn("My notes on the garden are a mess.", pages), "two blocks named: not the app's to choose")
    }

    @Test
    fun theSecretIsWhatHeSaysItIs() {
        assertEquals("purple walrus", Talks.secretIn("Do the hello world. My secret is purple walrus."))
        assertEquals("otters hold hands", Talks.secretIn("Do the hello world again. My secret is otters hold hands."))
        assertEquals("blue heron", Talks.secretIn("hello world, secret: \"blue heron\""))
        assertNull(Talks.secretIn("Do the hello world."))
    }

    @Test
    fun nothingShowsUntilTheRouteWordIsPast() {
        assertNull(Talks.shown("ta"))
        assertNull(Talks.shown("talk"))
        assertEquals("I'm", Talks.shown("talk\nI'm"))
        assertEquals("Sure, here", Talks.shown("Sure, here"))
        assertEquals("I'll add it.", Talks.shown("talk\nI'll add it.\nac"), "a route word half written")
        assertEquals("I'll add it.", Talks.shown("talk\nI'll add it.\nact\nadd garden: x"))
        assertEquals("Yes.", Talks.shown("talk\nYes.\nread garden\nSomething"))
    }

    @Test
    fun theInstructionNamesLinesByTheirWords() {
        val (_, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        val text = Talks.instruction(HelloWorld.SHELL, "pocket-companion", Asks.plainWindow(pages, HelloWorld.SHELL), pages)
        assertContains(text, "- garden, opening \"The garden, through the year\", 2 lines:\n  \"Autumn: plant the bulbs\"\n  \"Winter: prune the roses\"")
        assertFalse(Regex("""\n  1: """).containsMatchIn(text), "no addresses")
        assertContains(text, "add <block> under \"<the words of a line>\": <line>")
        assertContains(text, "Greetings, thanks and questions are talk, never act.")
        assertFalse(text.contains("is already in the pocket"))
        // 0.6.4: no worked examples, no keeping of what he only tells, a new block only when he names it.
        assertFalse(text.contains("Robin") || text.contains("running") || text.contains("What's your name?"), "no examples")
        assertFalse(text.contains("worth keeping"))
        assertContains(text, "Make a new block only when he names it, by his name for it.")
        assertContains(text, "- Million, opening \"Million at pocket.\", 0 lines")
        val lean = Talks.lean(HelloWorld.SHELL, pages)
        assertFalse(lean.contains("Robin") || lean.contains("worth keeping") || lean.contains("read <block>"))
        assertContains(lean, "Each line you write is Matthew's own words, as he said them")
        assertEquals(emptyList(), Talks.opening(emptyList(), "Gemma"), "no worked examples as first turns")
    }

    @Test
    fun theShellsHelloWorldInActionsIsAnActThatComesOutRight() {
        val (storage, pages) = Bench.fixture(shelf, 1_793_178_000_000L)
        val answer = Talks.SHELL_RECIPE.substringAfter("changing nothing else: ").substringBefore(". (Each").replace(" / ", "\n").replace(HelloWorld.SECRET_SLOT, Bench.SECRET)
        val (route, rest) = Talks.route(answer)
        assertEquals(Talks.Route.ACT, route)
        val planned = Acts.propose("", Acts.parse(rest), storage, shelf, HelloWorld.SHELL_OPENING, "0".repeat(32), 1_793_178_000_000L)
        val carried = Asks.carryOut(planned.proposal!!, pages, storage, HelloWorld.SHELL_OPENING)
        assertEquals(emptyList(), HelloWorld.check(storage.read(HelloWorld.BLOCK), HelloWorld.SHELL_OPENING, "0".repeat(32)))
        assertEquals(HelloWorld.messageFor(storage.read(HelloWorld.BLOCK)), carried.post!!.message)

        // Asked again, she is told the one change to the secret, by its words.
        val again = Talks.again(pages)!!
        val redo = again.substringAfter("answer ").substringBefore(", writing").replace(" / ", "\n").replace(HelloWorld.SECRET_SLOT, "orange walrus")
        val second = Acts.propose("", Acts.parse(Talks.route(redo).second), storage, shelf, HelloWorld.SHELL_OPENING, "f".repeat(32), 1_793_178_100_000L)
        assertEquals(listOf(false, true, true), second.proposal!!.writes.map { it.step.byApp })
        Asks.carryOut(second.proposal!!, pages, storage, HelloWorld.SHELL_OPENING)
        assertEquals(emptyList(), HelloWorld.check(storage.read(HelloWorld.BLOCK), HelloWorld.SHELL_OPENING, "f".repeat(32)))
    }
}

/** 0.4's ask, with her replies written by hand and streamed a few letters at a time. */
class TalksAskTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val now = 1_793_178_000_000L

    private fun stream(vararg replies: String): Talks.Stream {
        val queue = ArrayDeque(replies.toList())
        return Talks.Stream { _, piece ->
            val reply = queue.removeFirst()
            reply.chunked(3).forEach(piece)
            reply
        }
    }

    private fun ask(question: String, s: Talks.Stream, seen: MutableList<String> = mutableListOf()): Talks.Answered {
        val (storage, pages) = Bench.fixture(shelf, now)
        return Talks.ask(question, s, pages, storage, shelf, HelloWorld.SHELL_OPENING, "0".repeat(32), { now }) { seen += it }
    }

    @Test
    fun talkIsStreamedWithoutItsRouteWord() {
        val seen = mutableListOf<String>()
        val a = ask("What's your name?", stream("talk\nI'm the pocket companion."), seen)
        assertEquals(Talks.Route.TALK, a.route)
        assertEquals("I'm the pocket companion.", a.said)
        assertEquals("I'm the pocket companion.", seen.last())
        kotlin.test.assertTrue(seen.none { it.startsWith("talk") })
    }

    @Test
    fun aReadIsRunThenAnsweredInPlainWords() {
        val a = ask("What's in my garden block?", stream("read\nread garden", "talk\nIt holds the autumn and winter lines."))
        assertEquals(Talks.Route.READ, a.route)
        assertContains(a.found.single(), "Autumn: plant the bulbs")
        assertEquals("It holds the autumn and winter lines.", a.said)
        assertEquals(2, a.exchanges.size)
    }

    @Test
    fun anActIsPlannedOrSentBackOnce() {
        val a = ask("Under the autumn line in garden, note the tulips.", stream("act\nadd garden under \"Autumn: plant the bulbs\": the tulips came from Holland"))
        assertEquals(listOf("1.1"), a.planned!!.proposal!!.writes.map { it.plan.landsAt })
        val again = ask("Note it.", stream("act\nadd garden under \"spring\": x", "act\nadd garden: the tulips came from Holland"))
        assertEquals(listOf("3"), again.planned!!.proposal!!.writes.map { it.plan.landsAt })
        assertContains(again.exchanges[1].first, "No line in garden says “spring”.")
        val stubborn = ask("Note it.", stream("act\nplease add it", "act\nplease add it"))
        assertContains(stubborn.problem!!, "couldn't be read")
        val unsure = ask("Plan my garden", stream("Sure! Here's a plan: water, weed, wait."))
        assertEquals(Talks.Route.TALK, unsure.route, "when unsure, talk")
        val said = ask("Add compost to garden.", stream("talk\nI'll add it.\nact\nadd garden: buy more compost"))
        assertEquals("I'll add it.", said.said, "her words before an act are what she says with it")
        assertEquals(listOf("3"), said.planned!!.proposal!!.writes.map { it.plan.landsAt })
    }

    @Test
    fun aReadOfNothingIsStillAnswered() {
        val a = ask("Tell me about the sea at night.", stream("read\nread sea at night", "talk\nThe sea at night is dark and calm."))
        assertEquals("The sea at night is dark and calm.", a.said)
        assertContains(a.exchanges[1].first, "There's no block called sea at night")
    }

    @Test
    fun howHerLastActEndedIsToldHerFirst() {
        val heard = mutableListOf<String>()
        val s = Talks.Stream { message, _ -> heard += message; "talk\nNoted." }
        val (storage, pages) = Bench.fixture(shelf, now)
        Talks.ask("And now?", s, pages, storage, shelf, HelloWorld.SHELL_OPENING, "0".repeat(32), { now }, before = "Matthew said not now: wrong block.")
        assertEquals("(Matthew said not now: wrong block.)\nMatthew says: And now?", heard.single())
    }

    /** 0.6.2, one home per fact: her shell carries "Matthew calls me Gemma.", as his Pixel's did on 30 October. */
    private fun askNamed(question: String, s: Talks.Stream): Talks.Answered {
        val (storage, pages) = Bench.fixture(shelf, now)
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        return Talks.ask(question, s, pages, storage, shelf, HelloWorld.SHELL_OPENING, "0".repeat(32), { now })
    }

    @Test
    fun aNameHerShellAlreadyCarriesIsNotProposedAgain() {
        for (written in listOf("add pocket-companion: Matthew calls me Gemma.", "add pocket-companion: matthew calls me gemma", "add pocket-companion under \"Matthew calls me Gemma.\": Matthew calls me Gemma")) {
            val a = askNamed("I'd like to call you Gemma", stream("act\n$written"))
            assertEquals(Talks.Route.TALK, a.route, written)
            kotlin.test.assertTrue(a.planned!!.nothing, written)
            kotlin.test.assertTrue(a.planned!!.dropped.single().contains("already says"), written)
        }
    }

    @Test
    fun aLineCutShortIsNotProposed() {
        for (written in listOf("add pocket-companion: Matthew calls me…", "add pocket-companion: Matthew calls me...", "add pocket-companion: Matthew calls me <the name>.")) {
            val a = askNamed("I'd like to call you Robin", stream("act\n$written"))
            assertEquals(Talks.Route.TALK, a.route, written)
            kotlin.test.assertTrue(a.planned!!.dropped.single().contains("cut short"), written)
        }
    }

    @Test
    fun aNewNameIsStillProposed() {
        val a = askNamed("I'd like to call you Robin", stream("act\nadd pocket-companion: Matthew calls me Robin."))
        assertEquals(Talks.Route.ACT, a.route)
        assertEquals(listOf("Matthew calls me Robin."), a.planned!!.proposal!!.writes.map { it.text })
    }

    @Test
    fun onlyTheLineAlreadyThereIsLetBe() {
        val a = ask("Keep both", stream("act\nadd garden: Autumn: plant the bulbs\nadd garden: Spring: sow the peas"))
        assertEquals(Talks.Route.ACT, a.route)
        assertEquals(listOf("Spring: sow the peas"), a.planned!!.proposal!!.writes.map { it.text })
        assertEquals(1, a.planned!!.dropped.size)
    }

    @Test
    fun aQuoteMatchingTwoLinesIsLeftForMatthew() {
        val a = ask("Under the line with 'the', add a note.", stream("act\nadd garden under \"the\": a note"))
        val choice = a.planned!!.choose!!
        assertEquals(2, choice.lines.size)
        assertEquals(listOf(Acts.Action.Add("garden", "the", "a note")), a.actions)
    }
}
