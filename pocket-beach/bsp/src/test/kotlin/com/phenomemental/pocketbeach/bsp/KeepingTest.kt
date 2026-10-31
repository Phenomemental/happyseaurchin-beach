package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 0.6.4 (Matthew, 31 October), the guards and the gate: the writing check
 * runs only when his words ask; asked to keep with no block named, the app
 * asks him where; a name for her is one line on her shell, by code; every
 * line proposed is his words; no proposal changes her shell's opening line,
 * writes to the app's own blocks, makes a block he didn't name, or posts
 * when he didn't ask.
 */
class KeepingTest {
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private val nowMs = 1_793_178_000_000L

    /** His two messages of 31 October, as the phone heard them, and the "Focus" evening's. */
    private val walk = "Hey Gemma Matthew here I'm just walking along the canal on the way up to the river NEN it's pretty chilly I'm wrapped up warm enough " +
        "there's no rain and I'm just if I'm honest my mind is a wash I want to write this book but also want to make a film and I also want to keep working on the beach " +
        "and create apps and things it's there's a lot in my mind at the moment I know what it's about really it's about somebody watching something or reading something " +
        "or experiencing something that just goes wow you know what you created this my god that's so beautiful you know like poets do you know I can film documentary makers do " +
        "I've got that kind of thing in me and I really want to I want your support Gemma to just sort of keep keep oppressed of the things I say and hopefully we can help shape them over time"
    private val setDown = "Yeah they are buzzing with ideas but there's also a practical side to this as well of started to learn video editing tools of written half of the book " +
        "you know this so it's not just full hardiness and yes I would like you to to set some of this down for me please"
    private val focus = "I'm trying to find fast ways to develop films"

    @Test
    fun theWritingCheckRunsOnlyWhenHisWordsAsk() {
        for (talk in listOf(
            focus, walk, "My plan is to learn to sail next summer.", "I want to write this book", "I want to make a film", "The shopping is on Saturdays from now on.",
            "I want to run a marathon next spring.", "I need to remember to call mum", "What did I note yesterday?", "Good night, talk tomorrow.", "Thanks!",
            "What's in my garden block?", "Can you help me plan my garden for spring?", "I was at the beach today.", "",
        )) assertEquals(emptySet(), Keeping.asks(talk), talk)
        val keep = setOf(Keeping.Ask.KEEP)
        for (asks in listOf(
            setDown, "I've written half of the book. Keep that for me please.", "Keep this: my plan is to learn to sail next summer.", "Can you keep that for me?",
            "Remember to buy stamps.", "Remember that Jo's birthday is on the twelfth.", "Add a line to garden: buy more compost.",
            "In garden, under the autumn line, note that the tulips came from Holland.", "Under the winter line in garden, add: mulch the beds.", "Note in notes: call the plumber.",
            "Please write that down.", "Put it down in notes: the boiler is serviced.", "Save this one.",
        )) assertEquals(keep, Keeping.asks(asks), asks)
        assertEquals(setOf(Keeping.Ask.CHANGE), Keeping.asks("Change the winter line in garden to: Winter: prune the roses and the apple tree."))
        assertEquals(setOf(Keeping.Ask.MAKE), Keeping.asks("Make a new block called recipes."))
        assertEquals(setOf(Keeping.Ask.MAKE), Keeping.asks("Make a block called sailing: learn to sail next summer."))
        assertEquals(setOf(Keeping.Ask.POST), Keeping.asks("Post to my pool: the garden is planted."))
        assertTrue(Keeping.Ask.HELLO in Keeping.asks("Do the hello world. My secret is purple walrus."))
        assertTrue(Keeping.asksToPost("Do the hello world. My secret is purple walrus."))
        assertFalse(Keeping.asksToPost("I was at the beach and the pool was closed."))
    }

    @Test
    fun aNameForHerIsFoundInHisWords() {
        assertEquals("Gemma", Keeping.name("I'd like to call you Gemma"))
        assertEquals("Gemma", Keeping.name(Bench.GEMMA_ASK))
        assertEquals("Robin", Keeping.name("your name is robin"))
        for (none in listOf("I'll call you later", "What should I call you?", "I'll call you back tomorrow.", focus)) assertNull(Keeping.name(none), none)
        assertEquals("sailing", Keeping.made("Make a block called sailing: learn to sail next summer."))
        assertEquals("recipes", Keeping.made("Make a new block called recipes."))
        assertNull(Keeping.made("Keep this: my plan is to learn to sail next summer."))
    }

    @Test
    fun whatHeAsksToKeepIsHisOwnWords() {
        assertEquals("my plan is to learn to sail next summer.", Keeping.keptWords("Keep this: my plan is to learn to sail next summer."))
        assertEquals("I've written half of the book.", Keeping.keptWords("I've written half of the book. Keep that for me please."))
        assertEquals("buy stamps.", Keeping.keptWords("Remember to buy stamps."))
        assertEquals("Jo's birthday is on the twelfth.", Keeping.keptWords("Remember that Jo's birthday is on the twelfth."))
        val walked = Keeping.keptWords(setDown)
        assertTrue(walked.startsWith("Yeah they are buzzing with ideas") && walked.endsWith("so it's not just full hardiness"), walked)
        assertEquals("I've started editing the film.", Keeping.keptWords("Keep that.", listOf("Hello.", "I've started editing the film.")))
        for (line in listOf("my plan is to learn to sail next summer", "learn to sail", "LEARN TO SAIL NEXT SUMMER.")) assertTrue(Keeping.hisWords(line, listOf("Keep this: my plan is to learn to sail next summer.")), line)
        for (line in listOf("Plan: learn to sail", "Focus", "I plan to learn to sail", "")) assertFalse(Keeping.hisWords(line, listOf("Keep this: my plan is to learn to sail next summer.")), line)
        assertTrue(Keeping.hisWords("I've written half of the book", listOf("I’ve written half of the book.")), "his apostrophes")
        assertTrue(Keeping.hisWords("the river and the canal", listOf("Down by the river", "and the canal, chilly")), "across his messages")
    }

    @Test
    fun theWayForEachMessage() {
        val (_, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        assertIs<Keeping.Way.Talk>(Keeping.way(focus, emptyList(), pages))
        assertIs<Keeping.Way.Talk>(Keeping.way("My plan is to learn to sail next summer.", emptyList(), pages))
        val where = Keeping.way("Keep this: my plan is to learn to sail next summer.", emptyList(), pages)
        assertIs<Keeping.Way.Where>(where)
        assertEquals("my plan is to learn to sail next summer.", where.words)
        assertEquals(setOf(Keeping.Ask.KEEP), assertIs<Keeping.Way.Write>(Keeping.way("Keep this in garden: buy more compost.", emptyList(), pages)).asks)
        // A block he names that isn't there is no block: the app asks where.
        assertIs<Keeping.Way.Where>(Keeping.way("Keep this in sailing: learn to sail.", emptyList(), pages))
        assertIs<Keeping.Way.Write>(Keeping.way("Make a block called sailing: learn to sail next summer.", emptyList(), pages))
        val name = assertIs<Keeping.Way.Name>(Keeping.way("I'd like to call you Gemma", emptyList(), pages))
        assertEquals("Matthew calls me Gemma.", name.line)
        check(pages.carryOut(pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
        assertIs<Keeping.Way.Talk>(Keeping.way("I'd like to call you Gemma", emptyList(), pages), "her shell already holds it")
        // The app's own blocks are never his to name for keeping.
        check(pages.carryOut(pages.planKeep(Talking.SAFETY_BLOCK, Talking.safetyBlock())).ok)
        assertEquals(emptyList(), Keeping.named("Keep this in safety-words: ok", pages))
    }

    private fun propose(said: String, his: List<String>, answer: String, pages: Pages, storage: MemoryStorage): Acts.Planned =
        Acts.propose("", Acts.parse(answer), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), nowMs, secret = Talks.secretIn(said), guard = Acts.Guard(his, said))

    @Test
    fun theGuardsInCode() {
        val (storage, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        val keep = "Keep this in garden: buy more compost."
        // His words, to a block that stands: proposed.
        val ok = propose(keep, listOf(keep), "add garden: buy more compost", pages, storage)
        assertEquals(listOf("buy more compost"), ok.proposal!!.writes.map { it.plan.text })
        // Not his words: never proposed.
        val reworded = propose(keep, listOf(keep), "add garden: Compost: buy more", pages, storage)
        assertTrue(reworded.nothing, reworded.dropped.toString())
        assertTrue(reworded.dropped.single().contains("isn't his words"))
        // Her words never count as his.
        assertTrue(propose(keep, listOf(keep), "add garden: what a lovely garden", pages, storage).nothing)
        // Her shell's opening line: never, named or quoted.
        val shell = "Change your opening line to: I am Gemma."
        assertTrue(propose(shell, listOf(shell), "change ${HelloWorld.SHELL}: I am Gemma.", pages, storage).dropped.single().contains("his to change"))
        assertTrue(propose(shell, listOf(shell), "change ${HelloWorld.SHELL} \"${HelloWorld.SHELL_OPENING}\": I am Gemma.", pages, storage).dropped.single().contains("his to change"))
        // The app's own blocks, standing or not.
        for (own in listOf(Landing.NOW, Landing.TELLING, Asks.LOG, Whens.BLOCK, Landing.EXAMPLES, Talking.SAFETY_BLOCK)) {
            val said = "Make a block called $own: buy more compost"
            assertTrue(propose(said, listOf(said), "new $own: buy more compost", pages, storage).dropped.single().contains("the app's own"), own)
        }
        // A new block only when he names it.
        val sail = "Keep this: my plan is to learn to sail next summer."
        assertTrue(propose(sail, listOf(sail), "new sailing: my plan is to learn to sail next summer", pages, storage).dropped.single().contains("didn't ask for a block called sailing"))
        val made = "Make a block called sailing: learn to sail next summer."
        val named = propose(made, listOf(made), "new sailing: learn to sail next summer", pages, storage)
        assertEquals("sailing", named.proposal!!.writes.first().plan.block)
        // A post only when he asks for one.
        val noPost = propose(keep, listOf(keep), "add garden: buy more compost\npost: buy more compost", pages, storage)
        assertNull(noPost.proposal!!.post)
        assertTrue("he didn't ask to post" in noPost.dropped)
        val post = "Post to my pool: the garden is planted."
        assertTrue(propose(post, listOf(post), "post: the garden is planted", pages, storage).proposal!!.post != null)
        // The hello world is the recipe's own, and asks for its post.
        val hello = "Do the hello world. My secret is purple walrus."
        val hw = propose(hello, listOf(hello), "new hello-world\nadd hello-world: Secret: ${HelloWorld.SECRET_SLOT}\npost: ${HelloWorld.SHELL_POST}", pages, storage)
        assertTrue(hw.proposal!!.asks, hw.problem ?: hw.dropped.toString())
        assertTrue(hw.proposal!!.post != null)
    }

    @Test
    fun keepThisInNeverOffersTheAppsOwnBlocks() {
        val (_, pages) = Bench.fixture(shelf, nowMs, Talks.SHELL_RECIPE)
        check(pages.carryOut(pages.planKeep(Landing.TELLING, Telling.shape())).ok)
        assertNull(Talks.keepIn("I was telling Jo about the garden and the notes", pages)?.takeIf { it == Landing.TELLING })
        assertNull(Talks.keepIn("I was telling Jo about it", pages))
        assertEquals("garden", Talks.keepIn("The garden needs netting before June.", pages))
    }

    @Test
    fun hisDayIsWrittenOnceThoughSortedTwice() {
        val six = mapOf(Telling.DAY[0] to listOf("Rang the garage first thing"))
        val (once, _) = Landing.withDay(null, Landing.dayAddress(nowMs), "Rang the garage first thing.", six)
        val (twice, _) = Landing.withDay(once, Landing.dayAddress(nowMs), "Rang the garage first thing.", six)
        assertEquals(stringify(once), stringify(twice))
    }

    /** 0.6.4: the bench's Keep asks, judged by the principles; a change to her shell's opening line fails every ask. */
    @Test
    fun theBenchsKeepAsksJudgeByThePrinciples() {
        fun ask(n: Int) = Bench.KEEP_ASKS.single { it.n == n }
        fun run(n: Int, answer: String?, guarded: Boolean = true): List<String> {
            val a = ask(n)
            val (storage, pages) = Bench.keepFixture(a, shelf, nowMs)
            val way = Keeping.way(a.question, emptyList(), pages)
            val planned = if (answer == null) Bench.byApp(way, storage, shelf, nowMs) else
                Acts.propose("", Acts.parse(answer), storage, shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), nowMs, secret = Talks.secretIn(a.question), guard = if (guarded) Acts.Guard(listOf(a.question), a.question) else null)
            return Bench.judgeKeep(a, way, planned)
        }
        assertEquals(emptyList(), run(1, null), "told, not asked: she only talks")
        assertEquals(emptyList(), run(2, null), "the Focus case")
        assertEquals(emptyList(), run(3, null), "the app asks where")
        assertEquals(emptyList(), run(4, null))
        assertEquals(emptyList(), run(5, "new sailing: learn to sail next summer"))
        assertEquals(emptyList(), run(6, "add garden: buy more compost"))
        assertEquals(emptyList(), run(7, "add garden under \"Autumn: plant the bulbs\": the tulips came from Holland"))
        assertEquals(emptyList(), run(10, null), "her name by code")
        assertEquals(emptyList(), run(11, null), "her name already on her shell")
        assertEquals(emptyList(), run(12, "change ${HelloWorld.SHELL}: I am Gemma, and I keep his words."), "guarded: nothing proposed")
        assertTrue(run(12, "change ${HelloWorld.SHELL}: I am Gemma, and I keep his words.", guarded = false).any { it.contains("shell's opening line") }, "unguarded, it fails")
        assertTrue(run(6, "new compost: buy more compost", guarded = false).any { it.contains("didn't name") })
        assertTrue(run(6, "add garden: Compost, more of it", guarded = false).any { it.contains("isn't his words") })
        assertTrue(run(13, "post: the garden is planted").isEmpty())
        // E4B copied the grammar's "<block>": his words name one block, so that is the block.
        val (_, garden) = Bench.keepFixture(ask(7), shelf, nowMs)
        assertEquals("add garden under \"Autumn: plant the bulbs\": tulips came from Holland", Talks.blockNamed("add <block> under \"Autumn: plant the bulbs\": tulips came from Holland", ask(7).question, garden))
        assertEquals("add <block>: x", Talks.blockNamed("add <block>: x", "Keep this: x", garden), "no block named: left as it is")
        assertTrue(Bench.keepLine(ask(3), Keeping.way(ask(3).question, emptyList(), Bench.keepFixture(ask(3), shelf, nowMs).second), null, 10).startsWith("Keep 3. right in 0.0 s"))
    }
}
