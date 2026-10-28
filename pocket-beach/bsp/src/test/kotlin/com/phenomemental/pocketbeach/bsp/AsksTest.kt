package com.phenomemental.pocketbeach.bsp

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the companion proposes, the hello world, and the one post, on a pocket held in memory and a wire that goes nowhere. */
class AsksTest {
    private val storage = MemoryStorage()
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))
    private var now = 1_793_178_000_000
    private val bsp = PocketBsp(storage, shelf) { now }
    private val pages = Pages(storage, shelf, bsp) { now }
    private val opening = HelloWorld.SHELL_OPENING
    private val seal = "0123456789abcdef0123456789abcdef"

    /** An answer as the companion gives the hello world, prose around it as a small model may leave. */
    private val helloAnswer = """
        Here is my answer:
        ```json
        {"say": "I'll make hello-world and offer the post.",
         "steps": [
           {"do": "make", "block": "hello-world"},
           {"do": "add", "block": "hello-world", "at": "", "text": "{{shell}}"},
           {"do": "add", "block": "hello-world", "at": "", "text": "Secret: purple walrus"},
           {"do": "add", "block": "hello-world", "at": "", "text": "Seal: {{seal}}"},
           {"do": "add", "block": "hello-world", "at": "", "text": "Fingerprint: {{fingerprint}}"}
         ],
         "post": {"pool": "Phenomemental", "message": "Hello world from the pocket. Shell: “{{shell}}” Secret: purple walrus. Seal: {{seal}}. Fingerprint: {{fingerprint}}. Block: {{address}}."}}
        ```
    """.trimIndent()

    @Test
    fun anAnswerIsReadLenientlyAndCheckedStrictly() {
        val a = Asks.parse(helloAnswer)
        assertEquals(5, a.steps.size)
        assertEquals(Act.MAKE, a.steps[0].act)
        assertEquals("{{shell}}", a.steps[1].text)
        assertEquals("Phenomemental", a.post!!.pool)

        assertNull(Asks.parse("""{"say":"Nothing to do.","steps":[],"post":{"pool":"","message":""}}""").post)
        assertNull(Asks.parse("""{"say":"Nothing to do.","steps":[],"post":{"pool":"None","message":""}}""").post)
        assertNull(Asks.parse("""{"say":"Nothing to do.","steps":[],"post":{"pool":"Phenomemental"}}""").post)
        assertEquals(Act.READ, Asks.parse("""{"say":"Looking.","steps":[{"do":"Read","block":"pocket/garden","at":1}]}""").steps[0].act)
        assertEquals("garden", Asks.parse("""{"say":"x","steps":[{"do":"read","block":"pocket/garden"}]}""").steps[0].block)
        assertEquals("1", Asks.parse("""{"say":"x","steps":[{"do":"read","block":"garden","at":1}]}""").steps[0].at)

        for (bad in listOf(
            "no json here",
            "{not json}",
            """{"say":"x","steps":[{"do":"delete","block":"garden"}]}""",
            """{"say":"x","steps":[{"do":"add","block":"garden"}]}""",
            """{"say":"x","steps":[{"do":"add","text":"a line"}]}""",
            """{"say":"x","steps":"make garden"}""",
            """{"say":"","steps":[]}""",
            """{"say":"x","steps":[{"do":"add","block":"garden","at":"","text":"one\ntwo"}]}""",
            """{"say":"x","steps":[{"do":"add","block":"hello-world","at":"","text":"Secret: HIS SECRET"}]}""",
            """{"say":"x","steps":[],"post":{"pool":"Phenomemental","message":"Secret: HIS SECRET."}}""",
        )) assertFailsWith<Asks.Unreadable>(bad) { Asks.parse(bad) }
    }

    @Test
    fun theHelloWorldIsShownFirstThenWrittenAsShown() {
        val answer = Asks.parse(helloAnswer)
        val shown = Asks.propose(answer, storage, shelf, opening, seal, now)
        assertNull(shown.refusal, shown.refusal)
        assertTrue(shown.asks)
        assertEquals(listOf("", "1", "2", "3", "4"), shown.writes.map { it.plan.landsAt })
        assertEquals(opening, shown.writes[1].text)
        assertEquals("Seal: $seal", shown.writes[3].text)
        assertEquals("Fingerprint: {{fingerprint}}", shown.writes[4].shown, "a fingerprint is known only once its line is written")
        assertFalse(pages.exists(HelloWorld.BLOCK), "showing writes nothing")

        now += 5000
        val carried = Asks.carryOut(shown, pages, storage, opening)
        assertNull(carried.problem, carried.problem)
        assertEquals(5, carried.landed.size)
        assertEquals(HelloWorld.BLOCK, carried.made)
        val stored = storage.read(HelloWorld.BLOCK)
        assertEquals(emptyList(), HelloWorld.check(stored, opening, seal))

        val post = assertNotNull(carried.post)
        val print = collectUnderscore((parseJson(stored!!) as JsObj)["4"])!!.removePrefix("Fingerprint: ")
        assertEquals(print, carried.landed[4].print)
        assertEquals(shown.post!!.message.replace(shown.writes[4].print!!, print), post.message, "the post goes as it was shown, with the fingerprint the block holds")
        assertEquals(emptyList(), HelloWorld.checkMessage(post.message, opening, seal, print))
        assertEquals("Hello world from the pocket. Shell: “$opening” Secret: purple walrus. Seal: $seal. Fingerprint: $print. Block: pocket/hello-world.", post.message)
    }

    @Test
    fun nothingIsWrittenWhenThePocketMovedSinceItWasShown() {
        val shown = Asks.propose(Asks.parse(helloAnswer), storage, shelf, opening, seal, now)
        pages.carryOut(pages.planNewBlock(HelloWorld.BLOCK))
        val carried = Asks.carryOut(shown, pages, storage, opening)
        assertNotNull(carried.problem)
        assertContains(carried.problem!!, "Nothing was written")
        assertTrue(carried.landed.isEmpty())
        assertNull(carried.post)
        assertEquals("""{"_":"hello-world at pocket."}""", storage.read(HelloWorld.BLOCK))
    }

    @Test
    fun aLineThatWouldLandElsewhereStopsTheRest() {
        pages.carryOut(pages.planNewBlock("garden"))
        pages.carryOut(pages.planAdd("garden", "", "autumn"))
        val answer = Asks.parse("""{"say":"x","steps":[
            {"do":"add","block":"garden","at":"1","text":"bulbs"},
            {"do":"add","block":"garden","at":"","text":"winter"}]}""")
        val shown = Asks.propose(answer, storage, shelf, opening, seal, now)
        assertNull(shown.refusal)
        assertEquals(listOf("1.1", "2"), shown.writes.map { it.plan.landsAt })
        pages.carryOut(pages.planAdd("garden", "", "spring"))
        val carried = Asks.carryOut(shown, pages, storage, opening)
        assertEquals(1, carried.landed.size, "the first still lands where shown")
        assertContains(carried.problem!!, "The writes before it landed")
    }

    @Test
    fun whatCantGoAheadIsSentBackOnceThenShown() {
        val replies = ArrayDeque(listOf(
            """{"say":"x","steps":[{"do":"add","block":"hello-world","at":"","text":"a line"}]}""",
            """{"say":"x","steps":[{"do":"make","block":"hello-world","at":"","text":""},{"do":"add","block":"hello-world","at":"","text":"a line"}]}""",
        ))
        val asked = Asks.ask("Make hello-world with a line", { replies.removeFirst() }, pages, storage, shelf, opening, seal) { now }
        assertEquals(2, asked.exchanges.size)
        assertContains(asked.exchanges[1].first, "That can't go ahead: Step 1: There's no block called hello-world. A block is made with a make step before lines are added to it.")
        assertNull(asked.proposal!!.refusal)
        assertEquals(listOf(false, false, true, true), asked.proposal!!.writes.map { it.step.byApp }, "a write to hello-world gets the app's seal and fingerprint after it")

        // Asked for the hello world with one already there, it's told how to do it again.
        HelloWorld.makeByHand(pages, storage, opening, "first", seal)
        val recipe = HelloWorld.SHELL_RECIPE.substringAfter("changing nothing else: ").replace("HIS SECRET", "second")
        val redo = ArrayDeque(listOf(recipe, recipe))
        val twice = Asks.ask("Do the hello world, secret: second", { redo.removeFirst() }, pages, storage, shelf, opening, seal) { now }
        assertContains(twice.exchanges[1].first, "There's already a block called hello-world. To do the hello world again, change its Secret line instead")

        val stubborn = ArrayDeque(List(2) { """{"say":"x","steps":[{"do":"add","block":"nowhere","at":"","text":"a line"}]}""" })
        val refused = Asks.ask("Add a line to nowhere", { stubborn.removeFirst() }, pages, storage, shelf, opening, seal) { now }
        assertEquals(2, refused.exchanges.size, "sent back once, not again")
        assertContains(refused.proposal!!.refusal!!, "There's no block called nowhere")
    }

    @Test
    fun whatCantGoAheadIsSaidPlainly() {
        fun refusal(json: String) = Asks.propose(Asks.parse(json), storage, shelf, opening, seal, now).refusal
        assertContains(refusal("""{"say":"x","steps":[{"do":"add","block":"nowhere","text":"a line"}]}""")!!, "There's no block called nowhere")
        assertContains(refusal("""{"say":"x","steps":[{"do":"make","block":"g"},{"do":"add","block":"g","text":"{{secret}}"}]}""")!!, "{{secret}}")
        assertContains(refusal("""{"say":"x","steps":[{"do":"make","block":"sed:x"}]}""")!!, "Step 1")
        assertContains(refusal("""{"say":"x","steps":[{"do":"add","block":"whetstone","text":"mine now"}]}""")!!, "There's no block called whetstone")
        assertTrue(storage.names().isEmpty())

        val elsewhere = Asks.propose(Asks.parse("""{"say":"x","steps":[],"post":{"pool":"someone-else","message":"hi"}}"""), storage, shelf, opening, seal, now)
        assertNull(elsewhere.post)
        assertContains(elsewhere.postRefusal!!, "only to pool:Phenomemental")
        val gaps = Asks.propose(Asks.parse("""{"say":"x","steps":[],"post":{"pool":"Phenomemental","message":"Block: {{address}}"}}"""), storage, shelf, opening, seal, now)
        assertNull(gaps.post)
        assertFalse(gaps.asks)
    }

    @Test
    fun aMakeThatGivesTheOpeningLineIsTwoWrites() {
        val shown = Asks.propose(Asks.parse("""{"say":"x","steps":[{"do":"make","block":"notes","text":"My notes"}]}"""), storage, shelf, opening, seal, now)
        assertEquals(listOf(Kind.NEW_BLOCK, Kind.CHANGE), shown.writes.map { it.plan.kind })
        Asks.carryOut(shown, pages, storage, opening)
        assertEquals("My notes", pages.position(Region.POCKET, "notes")!!.text)
    }

    @Test
    fun readsFindThePocketThenTheShelf() {
        pages.carryOut(pages.planNewBlock("garden"))
        pages.carryOut(pages.planAdd("garden", "", "autumn"))
        val found = Asks.read(Asks.parse("""{"say":"x","steps":[{"do":"read","block":"garden"},{"do":"read","block":"whetstone","at":"1"},{"do":"read","block":"nowhere"}]}""").reads, pages)
        assertContains(found, "pocket/garden, its opening line: \"garden at pocket.\"\n  1: \"autumn\"")
        assertContains(found, "the teaching block whetstone, the line at 1")
        assertContains(found, "There's no block called nowhere")
    }

    @Test
    fun everyAskLeavesAnEntryInTheLog() {
        for (i in 1..10) {
            now += 1000
            val done = Asks.log(pages, Asks.entry("question $i", "Proposed nothing.", "Nothing to say yes to.", "Took 1.2 s."))
            assertTrue(done.ok, done.problem)
        }
        val log = pages.position(Region.POCKET, Asks.LOG)!!
        assertEquals(Asks.LOG_OPENING, log.text)
        assertEquals(1, log.wrapped, "the tenth entry wraps the log, as any block wraps")
        val first = pages.position(Region.POCKET, Asks.LOG, "01")!!
        assertEquals("Asked: question 1", first.text)
        assertEquals(isoString(1_793_178_001_000), first.stamp, "the beach stamps each entry's arrival at field 3")
        assertEquals(listOf("Proposed nothing.", "Nothing to say yes to.", "Took 1.2 s."), first.lines.map { it.text })
    }

    @Test
    fun theLogKeepsTheCallsProposed() {
        val shown = Asks.propose(Asks.parse(helloAnswer), storage, shelf, opening, seal, now)
        val said = Asks.proposed(shown)
        assertContains(said, "Proposed: bsp({\"agent_id\":\"pocket\",\"block\":\"hello-world\"")
        assertContains(said, "Offered to post to pool:Phenomemental: Hello world from the pocket.")
        assertEquals("Proposed nothing.", Asks.proposed(null))
    }

    @Test
    fun aPostIsOfferedOnlyWhenMatthewAsksForOne() {
        pages.carryOut(pages.planNewBlock("garden"))
        val replies = ArrayDeque(listOf(
            """{"say":"Reading.","steps":[{"do":"read","block":"garden","at":"","text":""}],"post":{"pool":"Phenomemental","message":"Done!"}}""",
            """{"say":"It holds only its opening line.","steps":[]}""",
        ))
        val asked = Asks.ask("What's in my garden block?", { replies.removeFirst() }, pages, storage, shelf, opening, seal) { now }
        assertEquals(2, asked.exchanges.size, "with the unasked post left out, the read runs and it answers again")
        assertContains(asked.found.single(), "pocket/garden")
        assertEquals("Done!", asked.unasked!!.message)
        assertNull(asked.answer!!.post)
        assertFalse(asked.proposal!!.asks)
        assertContains(Asks.proposed(asked.proposal, asked.unasked), "that wasn't asked for, so it was left out")

        // Reads it has already been shown end the asking, rather than going round again.
        val again = ArrayDeque(List(3) { """{"say":"It holds its opening line.","steps":[{"do":"read","block":"garden","at":"","text":""}]}""" })
        val read = Asks.ask("What's in garden?", { again.removeFirst() }, pages, storage, shelf, opening, seal) { now }
        assertEquals(2, read.exchanges.size)
        assertEquals("It holds its opening line.", read.answer!!.say)

        assertTrue(Asks.asksForPost("Do the hello world. My secret is x."))
        assertTrue(Asks.asksForPost("please post this to my pool"))
        assertTrue(Asks.asksForPost("hello-world please"))
        assertFalse(Asks.asksForPost("Add a line to garden: buy more compost."))
        assertFalse(Asks.asksForPost("What's the composition of my notes?"))
    }

    @Test
    fun theInstructionCarriesTheShellAndThePocket() {
        pages.carryOut(pages.planNewBlock("garden"))
        pages.carryOut(pages.planAdd("garden", "", "autumn"))
        val text = Asks.instruction(HelloWorld.SHELL, "pocket-companion: \"$opening\"", Asks.plainWindow(pages, "garden"), pages)
        assertContains(text, "You wear the shell pocket-companion")
        assertContains(text, opening)
        assertContains(text, "- garden, its opening line: \"garden at pocket.\"\n  1: \"autumn\"")
        assertContains(text, "The teaching blocks, read only: ")
        assertContains(text, "whetstone")
        assertContains(text, "{{fingerprint}}")
        assertContains(text, "The shell's lines, plainly:\ngarden: \"garden at pocket.\"")
        assertNull(HelloWorld.again(pages))
        assertFalse(text.contains("is already in the pocket"), "the hello world again is spoken of only when there is one")
    }

    // ── the hello world by hand ──

    @Test
    fun theHelloWorldByHandHoldsItsFourLines() {
        val made = HelloWorld.makeByHand(pages, storage, opening, "  purple walrus ", seal)
        assertEquals(emptyList(), HelloWorld.check(made.stored, opening, seal))
        assertTrue(HelloWorld.verify(made.stored))
        val block = parseJson(made.stored) as JsObj
        assertEquals(listOf(opening, "Secret: purple walrus", "Seal: $seal", "Fingerprint: ${made.fingerprint}"), (1..4).map { collectUnderscore(block["$it"]) })
        assertEquals(16, made.fingerprint.length)

        val tampered = made.stored.replace("purple", "orange")
        assertFalse(HelloWorld.verify(tampered))
        assertEquals(listOf("The last line isn't a fingerprint that matches the block."), HelloWorld.check(tampered, opening, seal))
        assertEquals(listOf("No line holds the seal the app made."), HelloWorld.check(made.stored, opening, "f".repeat(32)))

        assertEquals(HelloWorld.message(opening, "purple walrus", seal, made.fingerprint), HelloWorld.messageFor(made.stored))
        assertNull(HelloWorld.messageFor(tampered))

        assertFailsWith<IllegalArgumentException> { HelloWorld.makeByHand(pages, storage, opening, "again", seal) }
        assertFailsWith<IllegalArgumentException> { HelloWorld.makeByHand(Pages(MemoryStorage(), shelf, bsp), storage, opening, " ", seal) }
    }

    @Test
    fun theCompanionsShellIsMadeByATap() {
        HelloWorld.makeShell(pages)
        val shell = pages.position(Region.POCKET, HelloWorld.SHELL)!!
        assertEquals(HelloWorld.SHELL_OPENING, shell.text)
        assertEquals(listOf(HelloWorld.SHELL_RECIPE), shell.lines.map { it.text })
        assertFailsWith<IllegalArgumentException> { HelloWorld.makeShell(pages) }
        // The shell's own answer, its secret written in, is a hello world that checks out.
        val answer = HelloWorld.SHELL_RECIPE.substringAfter("changing nothing else: ").replace("HIS SECRET", "purple walrus")
        val shown = Asks.propose(Asks.parse(answer), storage, shelf, opening, seal, now)
        assertTrue(shown.writes.none { it.step.byApp }, "the shell's own answer leaves the app nothing to add")
        val carried = Asks.carryOut(shown, pages, storage, opening)
        assertNull(carried.problem)
        assertEquals(emptyList(), HelloWorld.check(storage.read(HelloWorld.BLOCK), opening, seal))
        assertEquals(HelloWorld.messageFor(storage.read(HelloWorld.BLOCK)), carried.post!!.message)
        assertContains(carried.post!!.message, "Secret: purple walrus.")

        // Asked again with the block there, the companion is told to change the secret instead; the app does the rest.
        val told = HelloWorld.again(pages)!!
        assertContains(Asks.instruction(HelloWorld.SHELL, "", Asks.plainWindow(pages, HelloWorld.SHELL), pages), told)
        val step = told.substringAfter("changing its Secret line, ").substringBefore(", writing the secret").replace("HIS SECRET", "orange walrus")
        assertEquals("""{"do": "change", "block": "hello-world", "at": "2", "text": "Secret: orange walrus"}""", step)
        val again = """{"say": "Again.", "steps": [$step], "post": {"pool": "Phenomemental", "message": "Hello world"}}"""
        val newSeal = "f".repeat(32)
        val redo = Asks.propose(Asks.parse(again), storage, shelf, opening, newSeal, now)
        assertNull(redo.refusal, redo.refusal)
        assertEquals(listOf(false, true, true), redo.writes.map { it.step.byApp })
        val done = Asks.carryOut(redo, pages, storage, opening)
        assertNull(done.problem, done.problem)
        assertEquals(emptyList(), HelloWorld.check(storage.read(HelloWorld.BLOCK), opening, newSeal))
        assertContains(done.post!!.message, "Secret: orange walrus.")
    }

    @Test
    fun anOldHelloWorldIsPostedWithTheSealAndFingerprintOfThisAsk() {
        val old = HelloWorld.makeByHand(pages, storage, opening, "the old secret", "a".repeat(32))
        now += 60_000
        // As the companion answered on the phone: the secret line replaced, and a post copying the old seal and fingerprint.
        val answer = Asks.parse("""{"say":"I'll change the secret and post it.",
            "steps":[{"do":"change","block":"hello-world","at":"2","text":"Secret: the new secret"}],
            "post":{"pool":"Phenomemental","message":"Hello world from the pocket. Shell: “$opening” Secret: the new secret. Seal: ${old.seal}. Fingerprint: ${old.fingerprint}. Block: pocket/hello-world."}}""")
        val shown = Asks.propose(answer, storage, shelf, opening, seal, now)
        assertNull(shown.refusal, shown.refusal)
        assertEquals(listOf("2" to false, "3" to true, "4" to true), shown.writes.map { it.plan.landsAt to it.step.byApp })
        assertEquals("Seal: $seal", shown.writes[1].text)
        assertEquals("Fingerprint: {{fingerprint}}", shown.writes[2].shown)
        val offered = shown.post!!.message
        assertFalse(offered.contains(old.seal) || offered.contains(old.fingerprint), "the post shown never carries the earlier run's seal or fingerprint")
        assertContains(offered, "Seal: $seal.")
        assertEquals(HelloWorld.SHELL_OPENING, storage.read(HelloWorld.BLOCK)!!.let { collectUnderscore((parseJson(it) as JsObj)["1"]) }, "showing writes nothing")

        now += 5000
        val carried = Asks.carryOut(shown, pages, storage, opening)
        assertNull(carried.problem, carried.problem)
        val stored = storage.read(HelloWorld.BLOCK)!!
        assertEquals(emptyList(), HelloWorld.check(stored, opening, seal), "the block as written holds this Ask's seal, and a fingerprint that matches it")
        val print = collectUnderscore((parseJson(stored) as JsObj)["4"])!!.removePrefix("Fingerprint: ")
        assertTrue(print != old.fingerprint)
        assertEquals(HelloWorld.message(opening, "the new secret", seal, print), carried.post!!.message, "the post is the block's own, made once it was written")
        assertContains(Asks.proposed(shown), "Fingerprint: {{fingerprint}}.", message = "the log keeps the fingerprint as the gap the write filled")
    }

    @Test
    fun aHelloWorldPostAloneRefreshesTheBlockFirst() {
        val old = HelloWorld.makeByHand(pages, storage, opening, "kept secret", "a".repeat(32))
        now += 60_000
        val shown = Asks.propose(Asks.parse("""{"say":"Posting it.","steps":[],"post":{"pool":"Phenomemental","message":"Hello world"}}"""), storage, shelf, opening, seal, now)
        assertEquals(listOf("3", "4"), shown.writes.map { it.plan.landsAt })
        assertTrue(shown.writes.all { it.step.byApp })
        val carried = Asks.carryOut(shown, pages, storage, opening)
        val stored = storage.read(HelloWorld.BLOCK)!!
        assertEquals(emptyList(), HelloWorld.check(stored, opening, seal))
        assertEquals(HelloWorld.messageFor(stored), carried.post!!.message)
        assertContains(carried.post!!.message, "Secret: kept secret.")
        assertFalse(carried.post!!.message.contains(old.fingerprint))

        // With no hello-world at all there is nothing true to post.
        storage.delete(HelloWorld.BLOCK)
        val none = Asks.propose(Asks.parse("""{"say":"x","steps":[],"post":{"pool":"Phenomemental","message":"Hello world"}}"""), storage, shelf, opening, seal, now)
        assertNull(none.post)
        assertContains(none.postRefusal!!, "There's no hello-world block")
    }

    @Test
    fun aFingerprintTheCompanionMakesUpIsReplaced() {
        val answer = Asks.parse("""{"say":"x","steps":[
            {"do":"make","block":"hello-world","at":"","text":""},
            {"do":"add","block":"hello-world","at":"","text":"{{shell}}"},
            {"do":"add","block":"hello-world","at":"","text":"Secret: s"},
            {"do":"add","block":"hello-world","at":"","text":"Fingerprint: 1234abcd1234abcd"}],
            "post":{"pool":"Phenomemental","message":"Hello world"}}""")
        val shown = Asks.propose(answer, storage, shelf, opening, seal, now)
        assertNull(shown.refusal, shown.refusal)
        // No seal line, so the app adds one beneath the made-up fingerprint, then a fingerprint of its own last.
        assertEquals(listOf("4" to "Seal: $seal", "5" to "Fingerprint: {{fingerprint}}"), shown.writes.filter { it.step.byApp }.map { it.plan.landsAt to it.shown })
        val carried = Asks.carryOut(shown, pages, storage, opening)
        assertEquals(emptyList(), HelloWorld.check(storage.read(HelloWorld.BLOCK), opening, seal))
        assertEquals(HelloWorld.messageFor(storage.read(HelloWorld.BLOCK)), carried.post!!.message)
    }

    @Test
    fun aFingerprintWrittenOverTheLastLineMatchesTheBlock() {
        val made = HelloWorld.makeByHand(pages, storage, opening, "s", seal)
        val without = parseJson(made.stored) as JsObj
        without.remove("4")
        assertEquals(HelloWorld.fingerprint(stringify(without)!!), HelloWorld.fingerprintWithout(made.stored, "4"))
        assertEquals(HelloWorld.fingerprint(made.stored), HelloWorld.fingerprintWithout(made.stored, ""))
        assertTrue(HelloWorld.isHelloPost("Hello world"))
        assertTrue(HelloWorld.isHelloPost("Seal: {{seal}}"))
        assertFalse(HelloWorld.isHelloPost("Good morning, pool."))
    }

    @Test
    fun aBlockFromElsewhereIsKeptOnlyAsItCame() {
        val received = parseJson("""{"_":"Notes from a friend","1":"one","2":{"_":"two","1":"two point one"}}""")
        val plan = pages.planKeep("friend", received)
        assertNull(plan.refusal, plan.refusal)
        assertFalse(pages.exists("friend"), "planning keeps nothing")
        assertTrue(pages.carryOut(plan).ok)
        assertTrue(deepEqual(received, parseJson(storage.read("friend")!!)))
        assertEquals("There's already a block called friend.", pages.planKeep("friend", received).refusal)
        assertEquals("Only a block (a JSON object) can be kept.", pages.planKeep("list", parseJson("[1,2]")).refusal)
        assertNotNull(pages.planKeep("sed:x", received).refusal)
    }

    @Test
    fun sealsAreRandomAndHex() {
        val seals = (1..50).map { HelloWorld.seal() }.toSet()
        assertEquals(50, seals.size)
        assertTrue(seals.all { Regex("^[0-9a-f]{32}$").matches(it) })
        assertEquals("Hello world from the pocket. Shell: “Pocket companion.” Secret: s. Seal: a. Fingerprint: b. Block: pocket/hello-world.", HelloWorld.message("Pocket companion.", "s", "a", "b"))
        assertTrue(HelloWorld.recipeIsOlder("Hello world: when Matthew asks for the hello world, answer with exactly these steps (an older form)"))
        assertFalse(HelloWorld.recipeIsOlder(HelloWorld.SHELL_RECIPE))
        assertFalse(HelloWorld.recipeIsOlder("A line of Matthew's own"))
    }

    // ── the one post ──

    @Test
    fun thePostIsOneAppendToTheFixedBeach() {
        val m = PoolPost.prepare("pool:Phenomemental", "  Hello world.  ")
        assertEquals("https://beach.happyseaurchin.com/.well-known/pscale-beach?block=pool:Phenomemental", m.url)
        assertEquals("""{"append":true,"content":{"1":"Phenomemental","_":"Hello world."}}""", m.body, "keyed as JavaScript keys it, digits first")
        assertFailsWith<IllegalArgumentException> { PoolPost.prepare("Elsewhere", "hi") }
        assertFailsWith<IllegalArgumentException> { PoolPost.prepare("Phenomemental", " ") }
        assertFailsWith<IllegalArgumentException> { PoolPost.prepare("Phenomemental", "x".repeat(PoolPost.MAX_CHARS + 1)) }

        val sent = mutableListOf<Pair<String, String>>()
        fun wire(status: Int, text: String) = Wire { url, body -> sent += url to body; WireReply(status, text) }

        val landed = assertIs<Posted.Landed>(PoolPost.send(m, wire(200, """{"slot":"7","block":"pool:Phenomemental"}""")))
        assertEquals("7", landed.slot)
        assertEquals(listOf(m.url to m.body), sent)

        assertContains(assertIs<Posted.Refused>(PoolPost.send(m, wire(403, """{"error":"lock required","code":"lock_required"}"""))).why, "now needs a key")
        assertContains(assertIs<Posted.Refused>(PoolPost.send(m, wire(429, """{"error":"pool is full for today"}"""))).why, "pool is full for today")
        assertContains(assertIs<Posted.Refused>(PoolPost.send(m, wire(500, "oops"))).why, "(500)")
        assertContains(assertIs<Posted.Refused>(PoolPost.send(m, wire(200, "{}"))).why, "without saying where")
        assertContains(assertIs<Posted.Refused>(PoolPost.send(m) { _, _ -> throw java.io.IOException("no route") }).why, "couldn't be reached")
        assertEquals(5, sent.size, "one request each, never retried")
    }
}
