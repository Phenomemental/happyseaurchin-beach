package com.phenomemental.pocketbeach

import android.content.Intent
import android.security.NetworkSecurityPolicy
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Landing
import com.phenomemental.pocketbeach.bsp.Telling
import com.phenomemental.pocketbeach.bsp.Whens
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talk
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.Wire
import com.phenomemental.pocketbeach.bsp.WireReply
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.ModelStore
import com.phenomemental.pocketbeach.model.Pace
import java.io.File
import com.phenomemental.pocketbeach.net.HttpsWire
import com.phenomemental.pocketbeach.ui.Asking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A wire whose beach can't be reached, as on a phone with no connection: each try is counted, and nothing goes. */
class UnreachableWire : Wire {
    var tries = 0
    override fun post(url: String, body: String): WireReply {
        tries++
        throw java.net.UnknownHostException("Unable to resolve host \"beach.happyseaurchin.com\": No address associated with hostname")
    }
}

/** A wire that goes nowhere: it keeps what would have been sent and answers as the beach does. No test posts to the beach. */
class NowhereWire : Wire {
    val sent = mutableListOf<Pair<String, String>>()
    override fun post(url: String, body: String): WireReply {
        sent += url to body
        return WireReply(200, """{"slot":"7","block":"pool:Phenomemental"}""")
    }
}

/**
 * A stand-in for the companion, since no test can run the model: it answers
 * the hello world as the shell describes it, or, with a hello-world already
 * in the pocket, as its instruction then says; 0.3's way in a JSON plan, 0.4's
 * in actions. Anything else it answers in plain words. 0.5: a conversation
 * of the Talk job (its instruction has no route words) only talks.
 */
class StandIn : Mind {
    /** The Write job's instruction, as 0.4's was. */
    var instruction = ""
    /** The Talk job's instruction. */
    var talkInstruction = ""
    /** The earlier exchanges each Write conversation (0.4's) was opened with. */
    val histories = mutableListOf<List<Pair<String, String>>>()
    /** The earlier exchanges each Talk conversation was opened with, and the settings it was drawn by. */
    val talkHistories = mutableListOf<List<Pair<String, String>>>()
    val talkSettings = mutableListOf<JobSettings>()
    /** 0.6.2: the settings each Write conversation was run on. */
    val writeSettings = mutableListOf<JobSettings>()
    /** Every message the Talk job was given, and how many the Write job was, and its last. */
    val talkMessages = mutableListOf<String>()
    var writeMessages = 0
    var writeMessage = ""

    override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean, settings: JobSettings): Mind.Talker {
        // 0.6.4: the writing check answers "in one of two ways" (it was three before reads went).
        val talking = !instruction.contains("Answer in one of")
        if (talking) {
            talkInstruction = instruction
            talkHistories += history
            talkSettings += settings
        } else {
            this.instruction = instruction
            histories += history
            writeSettings += settings
        }
        val gpuFails = talking && gpuFailsNext.also { gpuFailsNext = false }
        val paced = Pace(processor, null, null)
        // 0.6.3: a Write answer held back until stopped or let go, as a slow one on the phone; stopped part way, it says nothing.
        var holding: java.util.concurrent.CountDownLatch? = null
        var stopped = false
        return object : Mind.Talker {
            override val stream = Talks.Stream { message, piece ->
                // 0.6.2: as Gemma does when an answer fails on the GPU and she answers on the CPU.
                if (gpuFails) paced.gpuFailed()
                val reply = if (talking) {
                    talkMessages += message
                    talkScript.removeFirstOrNull() ?: "I'm a stand-in for the companion, talking."
                } else {
                    writeMessages++
                    writeMessage = message
                    writeHold?.let { h -> writeHold = null; holding = h; h.await(10, java.util.concurrent.TimeUnit.SECONDS); holding = null }
                    if (stopped) { stopped = false; "talk\n" } else talk(message)
                }
                reply.chunked(4).forEach(piece)
                reply
            }
            override val pace = paced
            override fun stop() { holding?.let { stopped = true; it.countDown() } }
            override fun close() {}
        }
    }

    /** 0.6.3: the next Write answer is held back until it is stopped. */
    @Volatile var writeHold: java.util.concurrent.CountDownLatch? = null

    /** 0.6.2: the next Talk conversation's answer fails on the GPU, and is answered on the CPU. */
    var gpuFailsNext = false

    /** The Write job's replies to give first, in order, before answering as above. */
    val script = ArrayDeque<String>()

    /** The Talk job's replies to give first, in order, before its plain one. */
    val talkScript = ArrayDeque<String>()

    /** 0.6: what the Sort job was asked, a piece at a time, and the instruction each piece came with. */
    val sortAsks = mutableListOf<Pair<String?, String>>()

    /** Her word for each piece of his day; "happened" for a piece not here. A card is about a piece when [about] says so. */
    val sortWords = mutableMapOf<String, String>()
    var about: (String) -> Boolean = { false }

    override suspend fun ask(instruction: String?, message: String, settings: JobSettings): Mind.Answer {
        sortAsks += instruction to message
        val text = when {
            message.startsWith("A card on Matthew's hand says") -> if (about(message)) "yes" else "no"
            // 0.6.1: a rung takes one of his pieces, by its number, or none.
            message.startsWith("Matthew's rungs above") -> "3.1: 1\n3.4: none"
            else -> sortWords[message.substringAfter("The piece: \u201c").substringBeforeLast("\u201d")] ?: "happened"
        }
        return Mind.Answer(text, 0, null, processor, null, null)
    }

    fun talk(message: String): String {
        script.removeFirstOrNull()?.let { return it }
        if (!message.contains("hello world", ignoreCase = true)) return "talk\nI'm a stand-in for the companion."
        val secret = message.substringAfter("secret is ", "").trim().removeSuffix(".")
        // 0.6.2: the recipe, and what to do when a hello-world is already in the pocket, come with the message that asks for it.
        val told = instruction + "\n" + message
        val again = told.substringAfter("don't make it: answer ", "").substringBefore(", writing the secret", "")
        val lines = again.ifEmpty { told.substringAfter("changing nothing else: ", "").substringBefore(". (Each", "") }
        return lines.replace(" / ", "\n").replace(HelloWorld.SECRET_SLOT, secret)
    }
    override fun state(): ModelStore.State = ModelStore.State.Ready
    override val loaded = true
    override val processor = "stand-in"
    override suspend fun chat(instruction: String): Mind.Chat {
        this.instruction = instruction
        return object : Mind.Chat {
            override val talk = Talk { message -> answer(message) }
            override val pace = Pace(processor, null, null)
            override fun stop() {}
            override fun close() {}
        }
    }

    fun answer(message: String): String {
        val secret = message.substringAfter("secret is ", "").trim().removeSuffix(".")
        val again = instruction.substringAfter("answer with one step changing its Secret line, ", "").substringBefore(", writing the secret", "")
        val steps = if (again.isNotEmpty()) listOf(again.replace("HIS SECRET", secret))
        else listOf("""{"do":"make","block":"hello-world","at":"","text":""}""") +
            HelloWorld.SHELL_LINES.map { """{"do":"add","block":"hello-world","at":"","text":"${it.replace("HIS SECRET", secret)}"}""" }
        return """{"say":"I'll write hello-world and offer the post.","steps":[${steps.joinToString(",")}],""" +
            """"post":{"pool":"Phenomemental","message":"${HelloWorld.SHELL_POST}"}}"""
    }
}

@RunWith(AndroidJUnit4::class)
class CompanionTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val wire = NowhereWire()
    private val mind = StandIn()

    @Before
    fun standIns() {
        app.wire = wire
        app.mind = mind
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
        app.connectedForTests = null
    }

    private fun askTheCompanion() {
        // 0.8: her own shell is no longer offered in Ask (the hatch is how a new person comes in); made here as a 0.7 pocket has it.
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        compose.tap("ask-companion")
        compose.waitFor("hatch-opening")
        compose.waitFor("wearing", "wearing pocket-companion")
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput("Do the hello world. My secret is purple walrus.")
        compose.tap("ask")
        compose.waitFor("writes")
        compose.waitFor("offered-post")
    }

    private fun log() = app.pages.position(Region.POCKET, Asks.LOG)

    @Test
    fun theHelloWorldWithTheCompanionEndsInOnePost() {
        Fresh.open().use {
            askTheCompanion()
            assertTrue(mind.instruction.contains(HelloWorld.SHELL_OPENING))
            assertEquals(5, compose.onAllNodes(hasTestTag("write"), useUnmergedTree = true).fetchSemanticsNodes().size)
            assertFalse("showing writes nothing", app.pages.exists(HelloWorld.BLOCK))
            compose.tap("yes")
            compose.waitFor("hello-checks", "checks out")
            compose.waitFor("post-message", "Secret: purple walrus")
            assertTrue("nothing goes until the post is confirmed", wire.sent.isEmpty())
            assertEquals(emptyList<String>(), HelloWorld.check(app.storage.read(HelloWorld.BLOCK), HelloWorld.SHELL_OPENING, sealOf()))
            compose.tap("post")
            compose.waitFor("posted", "Posted to your pool at entry 7")
            assertEquals(1, wire.sent.size)
            val (url, body) = wire.sent.single()
            assertEquals("${PoolPost.BEACH}/.well-known/pscale-beach?block=pool:Phenomemental", url)
            assertTrue(body.contains("Secret: purple walrus") && body.contains(sealOf()) && body.contains("pocket/hello-world") && body.contains("\"1\":\"Phenomemental\""))
            assertTrue("the post carries the shell's opening line", body.contains(HelloWorld.SHELL_OPENING))
            compose.waitUntil(10_000) { log()?.lines?.isNotEmpty() == true }
            val entry = app.pages.position(Region.POCKET, Asks.LOG, log()!!.lines.first().address)!!
            assertEquals("Asked: Do the hello world. My secret is purple walrus.", entry.text)
            assertTrue(entry.lines[1].text!!.contains("it landed at entry 7 of pool:Phenomemental"))
        }
    }

    @Test
    fun anOldHelloWorldIsPostedWithTheSealAndFingerprintOfThisAsk() {
        Fresh.clear()
        val old = HelloWorld.makeByHand(app.pages, app.storage, HelloWorld.SHELL_OPENING, "an old secret")
        ActivityScenario.launch(MainActivity::class.java).use {
            askTheCompanion()
            // 0.6.2: the recipe, and the hello-world already in the pocket, come with the message that asks for it.
            assertFalse(mind.instruction.contains("hello-world is already in the pocket"))
            assertTrue(mind.writeMessage, mind.writeMessage.contains("hello-world is already in the pocket"))
            assertTrue("in the action language", mind.writeMessage.contains("act / change hello-world \"Secret: an old secret\""))
            assertEquals(3, compose.onAllNodes(hasTestTag("write"), useUnmergedTree = true).fetchSemanticsNodes().size)
            assertEquals("the seal and the fingerprint are the app's own writes, shown", 2, compose.onAllNodes(hasTestTag("by-app"), useUnmergedTree = true).fetchSemanticsNodes().size)
            compose.waitFor("took", "on the stand-in")
            compose.tap("yes")
            compose.waitFor("hello-checks", "checks out")
            val stored = app.storage.read(HelloWorld.BLOCK)!!
            val seal = sealOf()
            assertTrue(seal != old.seal)
            compose.waitFor("post-message", "Secret: purple walrus")
            compose.tap("post")
            compose.waitFor("posted", "Posted to your pool at entry 7")
            val body = wire.sent.single().second
            assertTrue("the post is the block's own, as written", body.contains(HelloWorld.messageFor(stored)!!))
            assertTrue(body.contains(seal))
            assertFalse("never the earlier run's seal or fingerprint", body.contains(old.seal) || body.contains(old.fingerprint))
        }
    }

    @Test
    fun aPostTheBeachCouldntReceiveIsKeptAndGoesOnlyOnATap() {
        val away = UnreachableWire()
        app.wire = away
        Fresh.open().use {
            askTheCompanion()
            compose.tap("yes")
            compose.waitFor("post-message", "Secret: purple walrus")
            compose.tap("post")
            compose.waitFor("not-posted", "couldn't be reached")
            compose.waitFor("waiting")
            assertEquals(1, away.tries)
            val kept = app.unsent.all.single()
            assertTrue(kept.message.message.contains("Secret: purple walrus"))
            assertFalse(kept.mayHaveLanded)

            // Offline, Post now waits; back online, still nothing goes until the tap.
            app.connectedForTests = false
            compose.waitFor("waiting-why", "Waiting for a connection")
            compose.onNodeWithTag("post-now").assertIsNotEnabled()
            app.wire = wire
            app.connectedForTests = true
            compose.waitFor("waiting-why", "only when you tap Post now")
            compose.waitForIdle()
            Thread.sleep(1500)
            assertTrue("coming back online sends nothing", wire.sent.isEmpty())

            compose.menu("menu-blocks")
            compose.waitFor("waiting")
            compose.tap("post-now")
            compose.waitFor("posted-later", "Posted to your pool at entry 7")
            assertEquals(1, wire.sent.size)
            assertTrue(wire.sent.single().second.contains(HelloWorld.messageFor(app.storage.read(HelloWorld.BLOCK))!!))
            assertTrue("no longer kept", app.unsent.all.isEmpty())
            assertEquals("7", app.posts.landed(kept.message))
            compose.waitUntil(10_000) { log()?.lines?.isNotEmpty() == true }
            val entry = app.pages.position(Region.POCKET, Asks.LOG, log()!!.lines.first().address)!!
            assertTrue(entry.lines[1].text!!.contains("kept, to be posted only when he taps Post now"))
        }
    }

    @Test
    fun aPostThatMayHaveLandedSaysSoAndCanBeDiscarded() {
        Fresh.clear()
        val message = PoolPost.prepare(HelloWorld.POOL, "A message whose answer never came.")
        app.wire = Wire { _, _ -> throw com.phenomemental.pocketbeach.bsp.NoAnswer("Read timed out") }
        app.post(message)
        assertTrue(app.unsent.all.single().mayHaveLanded)
        app.wire = wire
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("waiting")
            compose.waitFor("may-have-landed", "may be on the beach already")
            compose.tap("discard-waiting")
            compose.tap("discard-confirm")
            compose.waitUntil(10_000) { app.unsent.all.isEmpty() }
            assertTrue(wire.sent.isEmpty())
        }
    }

    @Test
    fun theBenchRunsOnAScratchPocketAndKeepsOneBlock() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages)
        app.pages.carryOut(app.pages.planNewBlock("mine"))
        val before = app.storage.names().toSet()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            compose.menu("open-bench")
            compose.tap("run-bench")
            compose.waitFor("bench-saved", "Kept as bench-", timeoutMs = 120_000)
            val made = app.storage.names().toSet() - before
            assertEquals("one new block, the run", 1, made.size)
            assertTrue(made.single().startsWith("bench-"))
            assertEquals("nothing else of the pocket changed", before, app.storage.names().toSet() - made)
            assertFalse(app.pages.exists(HelloWorld.BLOCK))
            assertTrue("nothing posted", wire.sent.isEmpty())
            // 0.6.4: rewritten to the principles: how it was run, the Keep asks first, then each model; 0.3's and 0.4's runs are gone.
            val run = app.pages.position(Region.POCKET, made.single())!!
            assertTrue(run.text!!, run.text!!.startsWith("Bench, ") && run.text!!.contains("the Keep asks"))
            assertEquals(listOf("How it was run", "The Keep asks, each model: what the app does with his words, by the principles", "Each model on this phone: its speed, the Talk job, and the telling test"), run.lines.map { it.text })
            val keeps = app.pages.position(Region.POCKET, made.single(), run.lines[1].address)!!
            assertEquals(listOf("Gemma 4 E2B", "Gemma 4 E4B"), keeps.lines.map { it.text!!.substringBefore(":") })
            val e2b = app.pages.position(Region.POCKET, made.single(), keeps.lines[0].address)!!
            val asks = e2b.lines.flatMap { g -> app.pages.position(Region.POCKET, made.single(), g.address)!!.lines.mapNotNull { it.text } }
            assertEquals(Bench.KEEP_ASKS.size, asks.size)
            assertTrue(asks.joinToString("\n"), asks.first().startsWith("Keep 1. right") && asks.first().contains("She only talks"))
            assertTrue(asks.joinToString("\n"), asks.single { it.startsWith("Keep 3.") }.let { it.startsWith("Keep 3. right") && it.contains("The app asks where") })
            assertTrue(asks.joinToString("\n"), asks.single { it.startsWith("Keep 10.") }.let { it.startsWith("Keep 10. right") && it.contains("Matthew calls me Gemma.") })
            compose.tap("open-bench-block")
            compose.waitFor("standing", "Bench, ")
        }
    }

    /** 0.6: the quicker run, each model on the phone, its speed and the telling test, kept as a block of its own. */
    @Test
    fun eachModelIsTimedOnTheTellingTest() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages)
        val before = app.storage.names().toSet()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            compose.menu("open-bench")
            compose.tap("run-models")
            compose.waitFor("bench-saved", "Kept as bench-models-", timeoutMs = 60_000)
        }
        val made = (app.storage.names().toSet() - before).single()
        // 0.6.4: the Keep asks first, then each model.
        assertEquals("The Keep asks, each model: what the app does with his words, by the principles", app.pages.position(Region.POCKET, made, "2")!!.text)
        val models = app.pages.position(Region.POCKET, made, "3")!!
        assertEquals("Each model on this phone: its speed, the Talk job, and the telling test", models.text)
        assertEquals("each model an entry of its own", listOf("Gemma 4 E2B", "Gemma 4 E4B"), models.lines.map { it.text })
        val lines = models.lines.flatMap { m -> app.pages.position(Region.POCKET, made, m.address)!!.lines.mapNotNull { it.text } }
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E2B, the telling test, written: ") && it.contains(" of 11 marks") })
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E4B, the telling test, spoken: ") })
        // 0.6.1: his message of 29 October each way, and the Talk job's fixed asks; 0.6.2: the name already on her shell, and the safety message.
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E2B, the Talk job's fixed ask 21. ") })
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E4B, the Talk job's fixed ask 22. ") && it.contains("The Write job proposed") })
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E2B, the Talk job's fixed ask 23. ") && it.contains("The Write job proposed") })
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E4B, the Talk job's fixed ask 24. ") && it.contains("urge him") })
        // 0.6.3: ask 24 says the note showed beneath her reply.
        assertTrue(lines.joinToString("\n"), lines.any { it.startsWith("Gemma 4 E2B, the Talk job's fixed ask 24. ") && it.contains("The note beneath her reply: “If you're not safe right now") })
        assertTrue("each piece asked on its own", mind.sortAsks.size > Telling.cut(Telling.WRITTEN).size)
        assertTrue("nothing posted", wire.sent.isEmpty())
        assertFalse("nothing of his written but the run", app.pages.exists(Landing.NOW) || app.pages.exists(Landing.TELLING))
    }

    @Test
    fun askIsAConversation() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.pages.carryOut(app.pages.planNewBlock("garden"))
        app.pages.carryOut(app.pages.planAdd("garden", "", "Autumn: plant the bulbs"))
        app.pages.carryOut(app.pages.planAdd("garden", "", "Winter: prune the roses"))
        mind.talkScript += listOf("I'm the pocket companion.", "Yes: under one of the two lines that say that.")
        mind.script += listOf("act\nadd garden under \"the\": mulch the beds")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            compose.waitFor("question")
            compose.onNodeWithTag("question").performTextInput("What's your name?")
            compose.tap("ask")
            compose.waitFor("over", "She talked.")
            compose.waitFor("say", "I'm the pocket companion.")
            compose.waitFor("took", "first words after")
            assertEquals("a question asks no write: the Write job isn't asked", 0, mind.writeMessages)
            compose.onNodeWithTag("question").performTextClearance()
            compose.onNodeWithTag("question").performTextInput("In garden, under the line with the, note: mulch the beds.")
            compose.tap("ask")
            // The quote matches both lines: Matthew says which before anything is planned.
            compose.waitFor("which-line", "2 lines say that")
            compose.waitFor("say", "Yes: under one of the two lines")
            assertEquals("the first exchange is shown above, in the thread", 2, compose.onAllNodes(hasTestTag("turn"), useUnmergedTree = true).fetchSemanticsNodes().size)
            compose.tap("choose:2")
            compose.waitFor("writes")
            compose.waitFor("yes")
            assertFalse("nothing written before yes", app.pages.position(Region.POCKET, "garden", "2")!!.lines.isNotEmpty())
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, "garden", "2.1")?.text == "mulch the beds" }
            assertEquals("one Write conversation", 1, mind.histories.size)
            assertEquals("0.6.4: opened with no worked examples", emptyList<Pair<String, String>>(), mind.histories.single())
            // 0.7: hers is kept between his messages, her window read once.
            assertEquals("one Talk conversation, kept between his messages", listOf(emptyList<Pair<String, String>>()), mind.talkHistories)
            assertTrue("her turn carries the time and the app's line", mind.talkMessages.last().startsWith("(It is ") && mind.talkMessages.last().contains("(The app: "))
        }
    }

    /**
     * Ask closes and opens as the model and the shell come ready, and the two
     * run on other threads in either order (CI, twice): the close must never
     * shut the conversation just opened, to be read again at the first ask.
     * 0.6.1: that is the Write job's; hers is opened at each message.
     */
    @Test
    fun askOpenedAsItClosesKeepsTheConversationForTheFirstAsk() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        val asking = app.asking
        fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
        ActivityScenario.launch(MainActivity::class.java).use {
            repeat(10) {
                val before = mind.talkHistories.size
                val writes = mind.histories.size
                main { asking.reset(); asking.closed(); asking.opened() }
                compose.waitUntil(5_000) { mind.histories.size == writes + 1 }
                Thread.sleep(300)
                assertEquals("hers isn't opened until he speaks (round ${it + 1})", before, mind.talkHistories.size)
                main { asking.question = "Add to garden: compost."; asking.ask() }
                compose.waitUntil(5_000) { asking.settled }
                assertEquals("one Talk conversation, for his message (round ${it + 1})", before + 1, mind.talkHistories.size)
                assertEquals("one Write conversation, opened as Ask opened and kept for the first ask (round ${it + 1})", writes + 1, mind.histories.size)
            }
        }
    }

    /** He says [question]; 0.6.3: the thread keeps every turn, so the wait is for her reply to this one (the Write job may still be looking). */
    private fun asked(question: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextClearance()
        compose.onNodeWithTag("question").performTextInput(question)
        val before = app.asking.history.size
        compose.tap("ask")
        compose.waitUntil(10_000) { app.asking.history.size > before && app.asking.stage !is Asking.Stage.Thinking }
        assertEquals("the box empties when he sends", "", app.asking.question)
    }

    @Test
    fun detailsShowTheWorkingsUnderEveryAnswerAndTheLogKeepsThemAlways() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.pages.carryOut(app.pages.planNewBlock("garden"))
        app.pages.carryOut(app.pages.planAdd("garden", "", "Autumn: plant the bulbs"))
        mind.script += listOf("act\nadd garden: buy more compost")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            // Details on: each answer shows the time and where she ran.
            asked("What's your name?")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "on the stand-in")
            asked("What does the whetstone say?")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "on the stand-in")
            assertTrue("0.7: a teaching block his words name is read for her, in her turn", mind.talkMessages.last().contains("What the app read for you"))
            assertTrue("his words close her turn", mind.talkMessages.last().endsWith("\nWhat does the whetstone say?"))
            asked("Add to garden: buy more compost.")
            compose.waitFor("writes")
            compose.waitFor("took", "on the stand-in")
            compose.tap("not-now")
            compose.tap("just-not-now")
            compose.waitFor("over", "Matthew said not now.")

            // Off: only the conversation, and what came of a proposal.
            app.showDetails = false
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("took"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            asked("Thanks.")
            compose.waitUntil(10_000) { app.asking.settled }
            compose.waitFor("say", "I'm a stand-in for the companion, talking.")
            assertTrue(compose.onAllNodes(hasTestTag("took"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertTrue(compose.onAllNodes(hasText("Took ", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertTrue("what came of the proposal stays", compose.onAllNodes(hasText("Matthew said not now.", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
            assertTrue(compose.onAllNodes(hasText("She talked.", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        }
        // The ask log keeps every answer's time, shown or not.
        val log = app.pages.position(Region.POCKET, Asks.LOG)!!
        assertEquals(4, log.lines.size)
        for (entry in log.lines) assertTrue(app.pages.position(Region.POCKET, Asks.LOG, entry.address)!!.lines.any { it.text?.startsWith("Took ") == true })
        app.showDetails = true
    }

    /** 0.5's Stage 2: every message gets a reply; 0.6.1: his turn is his words alone, the date, the time and her shell in her instruction; the conversation saved whole. */
    @Test
    fun sheTalksToEveryMessageAndTheConversationCanBeSaved() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        mind.talkScript += listOf("Good morning, Matthew. What's on your mind?", "I'd keep that in a new block, running; say yes below if it's right.")
        mind.script += listOf("act\nnew running: I want to run a marathon next spring.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("Good morning")
            compose.waitFor("say", "Good morning, Matthew.")
            // 0.7: her turn is the time and the app's line, then his words; her window holds no time.
            assertTrue(mind.talkMessages.single(), mind.talkMessages.single().endsWith("\nGood morning"))
            assertTrue(mind.talkMessages.single(), mind.talkMessages.single().startsWith("(It is "))
            assertTrue(mind.talkMessages.single(), mind.talkMessages.single().contains("(The app: nothing is being kept or set out from this. Just talk with him.)"))
            assertTrue(mind.talkInstruction, mind.talkInstruction.contains(com.phenomemental.pocketbeach.bsp.HerWindow.CAN_DO))
            // 0.6.2: the friend line (C), her name not yet given; her shell's opening line has left her instruction.
            assertTrue(mind.talkInstruction, mind.talkInstruction.startsWith("You are Matthew's companion on his phone. Talk with him as a friend would"))
            assertTrue(mind.talkInstruction, mind.talkInstruction.contains("If he gives you a name, take it gladly as yours."))
            // 0.6.3: C changed is hers.
            assertTrue(mind.talkInstruction, mind.talkInstruction.contains(com.phenomemental.pocketbeach.bsp.Talking.MEET) && mind.talkInstruction.contains(com.phenomemental.pocketbeach.bsp.Talking.KEEPING))
            assertFalse(mind.talkInstruction, mind.talkInstruction.contains(HelloWorld.SHELL_OPENING))
            assertFalse("her instruction has no route words", mind.talkInstruction.contains("talk:"))
            assertEquals("a greeting asks no write", 0, mind.writeMessages)
            // 0.6.4: something told is not kept unasked; he names the block.
            asked("I want to run a marathon next spring.")
            assertEquals("told, not asked: no writing check", 0, mind.writeMessages)
            assertTrue("nothing waits on him", app.asking.history.last().waiting == null)
            asked("Make a block called running: I want to run a marathon next spring.")
            compose.waitFor("say", "I'd keep that in a new block, running")
            compose.waitFor("writes")
            assertEquals(1, mind.writeMessages)
            assertTrue("she talked as Edge does", mind.talkSettings.all { it == JobSettings.of(Job.TALK) })
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.exists("running") }
            compose.tap("menu")
            compose.waitFor("save-conversation")
        }
        val t = app.asking.transcript()
        assertTrue(t, t.contains("Matthew: Good morning"))
        assertTrue(t, t.contains("Companion: Good morning, Matthew. What's on your mind?"))
        assertTrue(t, t.contains("Matthew: I want to run a marathon next spring."))
        assertTrue(t, Regex("""Talk \(\d+\.\d s\): Gemma 4 E2B""").containsMatchIn(t))
        assertTrue(t, t.contains("temperature 1.00, top-k 64, top-p 0.95"))
    }

    /**
     * 0.6, Stage 4: his day, told in Ask, is sorted a piece at a time and
     * shown as one proposal; a line unticked is left out, a line moved lands
     * where he put it and is kept as an example she is shown the next time
     * she sorts. 0.6.1: only when he taps "Sort as my day" under her reply.
     */
    @Test
    fun hisDayIsSortedWhenHeAsksAndHisMovesAreKeptForNextTime() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        mind.talkScript += listOf("That sounds like a hard morning, Matthew.", "Good: Monday, then.")
        mind.sortWords += mapOf("Felt rotten about the morning" to "felt", "The thing I keep coming back to is the kids seeing me calm" to "matters")
        mind.about = { it.contains("Rang the garage") }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked(Telling.WRITTEN)
            compose.waitFor("say", "That sounds like a hard morning")
            compose.waitFor("sort-as-day")
            assertEquals("nothing sorted until he asks", 0, mind.sortAsks.size)
            // 0.6.4: his day told, not asked to keep: no writing check.
            assertEquals(0, mind.writeMessages)
            compose.tap("sort-as-day")
            compose.waitFor("day-yes", timeoutMs = 30_000)
            // 0.6.4: every block his Yes will write, named before he taps it.
            compose.waitFor("day-writes", "now, your days on the clock")
            assertEquals("each piece asked on its own, then the rungs", Telling.cut(Telling.WRITTEN, Landing.names(app.pages)).size + 1, mind.sortAsks.size)
            assertFalse("nothing written before yes", app.pages.exists(Landing.NOW))
            val day = app.asking.waiting as Asking.Stage.Day
            val car = day.lines.indexOfFirst { it.words == "the car wouldn't start again this morning" }
            val sam = day.lines.indexOfFirst { it.words == "that I'm always the one apologising" }
            compose.tap("day-tick:$car")
            compose.tap("day-move:$sam")
            compose.tap("day-to:1.3")
            compose.waitUntil(5_000) { (app.asking.waiting as? Asking.Stage.Day)?.lines?.get(sam)?.place == "1.3" }
            compose.tap("day-yes")
            compose.waitUntil(10_000) { app.pages.exists(Landing.NOW) }
            compose.waitFor("over", "kept as an example for her next time")
        }
        val day = Landing.dayAddress(System.currentTimeMillis())
        assertEquals(Telling.WRITTEN, app.pages.position(Region.POCKET, Landing.NOW, day)?.text)
        val now = app.storage.read(Landing.NOW)!!
        assertFalse("an unticked line isn't written", now.contains("\"the car wouldn't start again this morning\""))
        assertEquals(listOf("that I'm always the one apologising" to "thought"), Landing.examples(parseJson(app.storage.read(Landing.EXAMPLES)!!)))
        assertTrue("his time words, to change", app.pages.exists(Whens.BLOCK))
        val hand = Telling.cards(parseJson(app.storage.read(Landing.TELLING)!!) as JsObj)
        assertTrue(hand.values.toString(), hand.values.any { it.contains("ringing the garage tomorrow") })

        // The second telling: news of the garage card, and her instruction carries his move.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            val before = mind.sortAsks.size
            asked(Telling.SECOND)
            compose.tap("sort-as-day")
            compose.waitFor("day-yes", timeoutMs = 30_000)
            assertTrue(mind.sortAsks.drop(before).first().first!!.contains("Matthew placed these himself:\n\u201cthat I'm always the one apologising\u201d: thought"))
            compose.tap("day-yes")
            compose.waitUntil(10_000) { Telling.cards(parseJson(app.storage.read(Landing.TELLING)!!) as JsObj).values.any { it.contains("coming Monday") } }
        }
        // 0.6.3: the saved conversation says when "Sort as my day" was offered under her reply, and whether he tapped it.
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked(Telling.WRITTEN)
            compose.waitFor("sort-as-day")
            asked("Never mind, another time.")
            assertTrue("offered under her last reply only", compose.onAllNodes(hasTestTag("sort-as-day"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        }
        val saved = app.asking.transcript()
        assertTrue(saved, Regex("""At \d\d:\d\d, "Sort as my day" was offered under her reply; he tapped it at \d\d:\d\d\.""").containsMatchIn(saved))
        assertTrue(saved, saved.contains("\"Sort as my day\" was offered under her reply; he didn't tap it."))
        val hand2 = Telling.cards(parseJson(app.storage.read(Landing.TELLING)!!) as JsObj)
        assertEquals("the card updated, not doubled", 1, hand2.values.count { it.contains("garage") })
        assertEquals(Telling.WRITTEN + "\n\n" + Telling.SECOND, app.pages.position(Region.POCKET, Landing.NOW, day)?.text)
    }

    /**
     * 0.6.1, from his first test of 0.6 (29 October): her turn played back is
     * never shown, nor given her again; his message, one run of his words,
     * isn't offered as a day.
     */
    @Test
    fun herTurnPlayedBackIsNeverShown() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        mind.talkScript += listOf(
            "Shell: \"${HelloWorld.SHELL_OPENING}\"\nIt's Thursday, October 29, 2026, 17:37.\nMatthew: ${Bench.GEMMA_ASK}\nWhat would you like to keep in your blocks?",
            "Gemma it is.",
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked(Bench.GEMMA_ASK)
            compose.waitUntil(10_000) { app.asking.settled }
            compose.waitFor("say", "What would you like to keep in your blocks?")
            for (copy in listOf("Shell:", "It's Thursday", "Matthew: Hey")) {
                assertTrue(copy, compose.onAllNodes(hasText(copy, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            }
            assertTrue("one run of his words is no day", compose.onAllNodes(hasTestTag("sort-as-day"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            if (app.asking.waiting is Asking.Stage.Shown) compose.tap("not-now").also { compose.tap("just-not-now") }
            asked("Gemma it is, then.")
            compose.waitFor("say", "Gemma it is.")
            assertEquals("her turn played back isn't given her again", "What would you like to keep in your blocks?", mind.talkHistories.last().single().second)
        }
        val t = app.asking.transcript()
        assertTrue(t, t.contains("The app's line to her: The app: "))
        assertTrue(t, t.contains("Left out of her reply, as her turn played back: "))
        assertTrue(t, t.contains("Companion: What would you like to keep in your blocks?"))
    }

    /**
     * 0.6.1: he taps a line of his day to put right a word the phone
     * misheard; his correction lands as his words, and the saved
     * conversation carries the proposal whole.
     */
    @Test
    fun heCorrectsAMisheardWordAndTheConversationCarriesTheProposalWhole() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        val told = Telling.WRITTEN.replace("Felt rotten about the morning", "Felt rotten about the mourning")
        mind.talkScript += listOf("That sounds like a hard morning, Matthew.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked(told)
            // The Write job looks first, behind the chat, so the saved conversation carries its answer too.
            compose.waitUntil(10_000) { !app.asking.checking }
            compose.tap("sort-as-day")
            compose.waitFor("day-yes", timeoutMs = 30_000)
            val day = app.asking.waiting as Asking.Stage.Day
            val i = day.lines.indexOfFirst { it.words.contains("mourning") }
            assertTrue(day.lines.joinToString("\n") { it.words }, i >= 0)
            compose.tap("day-words:$i")
            compose.waitFor("day-correct-field")
            compose.onNodeWithTag("day-correct-field").performTextClearance()
            compose.onNodeWithTag("day-correct-field").performTextInput(day.lines[i].words.replace("mourning", "morning"))
            compose.tap("day-correct-keep")
            compose.waitUntil(5_000) { (app.asking.waiting as? Asking.Stage.Day)?.lines?.get(i)?.corrected == true }
            compose.waitFor("day-words:$i", "about the morning")
            compose.tap("day-yes")
            compose.waitUntil(10_000) { app.pages.exists(Landing.NOW) }
        }
        val now = app.storage.read(Landing.NOW)!!
        assertTrue("his correction lands as his words", now.contains("Felt rotten about the morning"))
        assertFalse(now.contains("mourning"))
        val t = app.asking.transcript()
        assertEquals("his day continues the exchange it sorts: his words once", 1, Regex("Matthew: Right, so the car").findAll(t).count())
        // 0.6.4: his day told, not asked to keep: no writing check.
        assertFalse(t, t.contains("; Write ("))
        for (part in listOf("Answered by: Talk (", "; Sort (", "Proposed:", "[x] ", "(corrected by him; heard", "His answer: Yes", "How it ended: Matthew said yes to his day")) {
            assertTrue("$part in\n$t", t.contains(part))
        }
    }

    /** 0.5's Stage 1: a job's settings, changed in Settings, are what its next conversation is opened with. */
    @Test
    fun aJobsSettingsAreKeptAndUsedFromTheNextAsk() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("settings")
            compose.waitFor("job-talk-temperature", "1.00")
            compose.tap("job-talk-temperature-less")
            compose.waitFor("job-talk-temperature", "0.90")
            compose.tap("job-talk-topk-less")
            compose.waitFor("job-talk-topk", "40")
            compose.waitFor("job-talk-reset")
            compose.waitFor("job-write-temperature", "0.00")
            compose.onNodeWithTag("job-write-temperature-less").assertIsNotEnabled()
            assertEquals(0.9, app.settings(Job.TALK).temperature, 1e-9)
            assertEquals(40, app.settings(Job.TALK).topK)
            assertEquals("kept on the phone", 40, JobSettings.read(app.getSharedPreferences("pocket", 0), Job.TALK).topK)
            compose.tap("back")
            compose.tap("ask-companion")
            asked("Hello")
            compose.waitUntil(10_000) { app.asking.settled }
            assertEquals(0.9, mind.talkSettings.last().temperature, 1e-9)
            assertEquals(40, mind.talkSettings.last().topK)
            compose.menu("menu-blocks")
            compose.tap("settings")
            compose.tap("job-talk-reset")
            compose.waitFor("job-talk-temperature", "1.00")
        }
        assertEquals(JobSettings.of(Job.TALK), app.settings(Job.TALK))
    }

    /**
     * 0.6.3, a plain chat: her reply shows and the box is ready at once; the
     * Write job looks after, behind the chat, gives way when he says the next
     * thing, and looks again after her reply; its proposal waits beneath her
     * first reply while he carries on, until he answers it.
     */
    @Test
    fun writeNeverHoldsUpTheChatAndItsProposalWaitsInTheThread() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planNewBlock("book")).ok)
        mind.talkScript += listOf("Half a book is real work already.", "Resolve is a good place to start.")
        mind.script += listOf("act\nadd book: I've written half of the book.")
        mind.writeHold = java.util.concurrent.CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("I've written half of the book. Keep that for me please.")
            compose.waitFor("say", "Half a book is real work already.")
            // The Write job is looking, and the box is ready: he carries on.
            compose.waitFor("checking")
            compose.waitUntil(5_000) { mind.writeMessages == 1 }
            compose.onNodeWithTag("question").performTextInput("Note in book: I'm learning Resolve too.")
            compose.onNodeWithTag("ask").assertIsEnabled()
            compose.tap("ask")
            compose.waitFor("say", "Resolve is a good place to start.")
            // It gave way to her reply, looked again, and its proposal waits beneath her first reply.
            compose.waitUntil(10_000) { app.asking.settled }
            val (first, second) = app.asking.history
            assertTrue("the proposal waits on the first exchange", first.waiting is Asking.Stage.Shown)
            assertEquals("She talked.", second.note)
            assertEquals("stopped once, then asked again, then the second", 3, mind.writeMessages)
            compose.waitFor("writes")
            // 0.6.3: where it goes, in words; the bsp line only with Details on.
            compose.waitUntil(5_000) { compose.onAllNodes(hasText("A new line in your book block.")).fetchSemanticsNodes().isNotEmpty() }
            assertTrue("no line's number", compose.onAllNodes(hasText("A new line at", substring = true)).fetchSemanticsNodes().isEmpty())
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("call") and hasText("bsp(agent_id: \"pocket\", block: \"book\"", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            app.showDetails = false
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("call"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            compose.waitFor("yes")
            app.showDetails = true
            assertTrue("nothing written before yes", app.pages.position(Region.POCKET, "book")!!.lines.isEmpty())
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, "book")!!.lines.any { it.text == "I've written half of the book." } }
            compose.waitFor("over", "Matthew said yes")
        }
        val t = app.asking.transcript()
        assertTrue(t, t.contains("Matthew: I've written half of the book. Keep that for me please."))
        assertTrue(t, t.contains("How it ended: Matthew said yes; book was written"))
    }

    /** 0.6.2, part three: when his words say he isn't safe, the safety line is her instruction's only line that turn; his safety words are kept as a block on his tap. */
    @Test
    fun theSafetyLineComesWithHisWordsAndTheWordsAreHisToChange() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        mind.talkScript += listOf("I'm worried about you. Please reach someone you trust right now, or Samaritans on 116 123.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("I don't feel safe tonight.")
            compose.waitFor("say", "I'm worried about you.")
            assertTrue(mind.talkMessages.last(), mind.talkMessages.last().contains("(" + com.phenomemental.pocketbeach.bsp.Talking.SAFETY + ")"))
            compose.menu("menu-blocks")
            compose.tap("settings")
            compose.waitFor("safety-words", "don't feel safe")
            assertFalse("nothing written until he taps", app.pages.exists(com.phenomemental.pocketbeach.bsp.Talking.SAFETY_BLOCK))
            compose.tap("keep-safety-words")
            compose.waitUntil(10_000) { app.pages.exists(com.phenomemental.pocketbeach.bsp.Talking.SAFETY_BLOCK) }
            compose.waitFor("open-safety-words")
            compose.waitFor("safety-note-words", "If you're not safe right now")
        }
        assertEquals(com.phenomemental.pocketbeach.bsp.Talking.SAFETY_WORDS, com.phenomemental.pocketbeach.bsp.Talking.safetyWords(app.pages))
        assertTrue("the note is kept with his words", com.phenomemental.pocketbeach.bsp.Talking.keepsNote(app.pages))
    }

    /**
     * 0.6.3 (Matthew, 31 October): when his safety words match, the app's own
     * note shows beneath her reply, whatever she says; none on a turn that
     * doesn't need it; his own note once he changes it; and words kept before
     * 0.6.3 get the note on his tap. The saved conversation says it showed.
     */
    @Test
    fun theSafetyNoteShowsBeneathHerReplyWhateverSheSays() {
        val talking = com.phenomemental.pocketbeach.bsp.Talking
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        mind.talkScript += listOf("Okay.", "Glad to hear it.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("I don't feel safe tonight.")
            compose.waitFor("say", "Okay.")
            compose.waitFor("safety-note", talking.SAFETY_NOTE)
            assertTrue("0.7: the safety line rides in her turn", mind.talkMessages.last().contains("(" + talking.SAFETY + ")"))
            asked("Had a good day after all.")
            compose.waitFor("say", "Glad to hear it.")
            assertFalse("no safety line on a turn that doesn't need it", mind.talkMessages.last().contains(talking.SAFETY))
            assertEquals("one note, beneath the reply that needed it", 1, compose.onAllNodes(hasTestTag("safety-note")).fetchSemanticsNodes().size)
            assertEquals(talking.SAFETY_NOTE, app.asking.history.first().safety)
            assertEquals(null, app.asking.history.last().safety)
        }
        val t = app.asking.transcript()
        assertTrue(t, t.contains("The safety note, shown beneath her reply: \"If you're not safe right now: call someone you trust, Samaritans on 116 123 (free, any hour), or 999 if you're in danger.\""))
        assertEquals("once", 1, Regex("The safety note, shown").findAll(t).count())

        // His own note, changed in his block, is the one shown.
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planKeep(talking.SAFETY_BLOCK, talking.safetyBlock())).ok)
        val heading = app.pages.position(Region.POCKET, talking.SAFETY_BLOCK)!!.lines.single { it.text == talking.NOTE_HEADING }
        val line = app.pages.position(Region.POCKET, talking.SAFETY_BLOCK, heading.address)!!.lines.single()
        check(app.pages.carryOut(app.pages.planChange(talking.SAFETY_BLOCK, line.address, "Ring Jo, or Samaritans on 116 123.")).ok)
        mind.talkScript += listOf("I hear you.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("I can't go on like this.")
            compose.waitFor("say", "I hear you.")
            compose.waitFor("safety-note", "Ring Jo, or Samaritans on 116 123.")
        }

        // Words kept before 0.6.3 have no note: the app's own shows, and Settings adds it to his block on his tap.
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planKeep(talking.SAFETY_BLOCK, talking.safetyBlock().also { it.remove("${talking.SAFETY_GROUPS.size + 1}") })).ok)
        assertFalse(talking.keepsNote(app.pages))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("settings")
            compose.waitFor("safety-note-words", "If you're not safe right now")
            compose.tap("add-safety-note")
            compose.waitUntil(10_000) { talking.keepsNote(app.pages) }
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("add-safety-note"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        }
        assertEquals(talking.SAFETY_NOTE, talking.safetyNote(app.pages))
        assertEquals(talking.SAFETY_WORDS, talking.safetyWords(app.pages))
    }

    /** 0.6.2: never two engines on the GPU. Talk's holds it; a job set to the GPU on another model runs on the CPU, and Settings and Ask say so. */
    @Test
    fun oneEngineOnTheGpuAndAJobMovedOffItSaysSo() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
        try {
            assertEquals("GPU", app.running(Job.TALK).processor)
            assertEquals("CPU", app.running(Job.WRITE).processor)
            assertEquals("CPU", app.running(Job.SORT).processor)
            assertEquals("Talk's Gemma 4 E4B holds the GPU, and the phone runs one engine on it at a time", app.cpuWhy(Job.WRITE))
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("settings")
                compose.waitFor("job-write-processor-hint", "Runs on the CPU: Talk's Gemma 4 E4B holds the GPU")
                compose.waitFor("job-sort-processor-hint", "Runs on the CPU")
                assertTrue("Talk runs as set", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("job-talk-processor-hint")).fetchSemanticsNodes().isEmpty())
                compose.tap("back")
                compose.tap("ask-companion")
                asked("Make a block called stamps.")
                compose.waitUntil(10_000) { mind.writeSettings.isNotEmpty() && mind.talkSettings.isNotEmpty() }
                assertEquals("the Write job ran on the CPU", "CPU", mind.writeSettings.last().processor)
                assertEquals("hers on the GPU", "GPU", mind.talkSettings.last().processor)
            }
            // An answer that failed on the GPU, answered on the CPU, says so under her reply: in its time line with Details on, a line of its own with them off.
            for (details in listOf(true, false)) {
                app.showDetails = details
                mind.gpuFailsNext = true
                val before = mind.talkMessages.size
                ActivityScenario.launch(MainActivity::class.java).use {
                    compose.tap("ask-companion")
                    asked("Hello")
                    compose.waitUntil(10_000) { mind.talkMessages.size > before }
                    compose.waitFor(if (details) "took" else "gpu-failed", com.phenomemental.pocketbeach.model.Pace.GPU_FAILED)
                }
            }
            app.showDetails = true
            // All three on one model share the GPU's one engine.
            app.set(Job.WRITE, JobSettings.of(Job.WRITE).copy(model = JobSettings.GEMMA_E4B))
            assertEquals("GPU", app.running(Job.WRITE).processor)
            assertEquals(null, app.cpuWhy(Job.WRITE))
        } finally {
            app.showDetails = true
            app.set(Job.TALK, JobSettings.of(Job.TALK))
            app.set(Job.WRITE, JobSettings.of(Job.WRITE))
        }
    }

    @Test
    fun whatHeTellsIsOfferedForKeepingInTheBlockItNames() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.pages.carryOut(app.pages.planNewBlock("Million"))
        val goal = "I'm going to make a million pound by creating tech that changes the world, before the end of 2027."
        // She only chats; the app offers to keep it in Million. Then she keeps a line herself, naming the block in small letters.
        mind.script += listOf("act\nadd million: by selling the first app in 2026")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked(goal)
            compose.waitFor("over", "She talked.")
            compose.waitFor("keep-in", "Keep this in Million?")
            compose.tap("keep-in")
            compose.waitFor("writes")
            assertTrue("nothing written before yes", app.pages.position(Region.POCKET, "Million")!!.lines.isEmpty())
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, "Million")?.lines?.firstOrNull()?.text == goal }

            asked("Add to Million: by selling the first app in 2026.")
            compose.waitFor("writes")
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, "Million")?.lines?.getOrNull(1)?.text == "by selling the first app in 2026" }
            assertFalse("never a second block in other capitals", app.pages.exists("million"))
        }
    }

    /**
     * 0.6.4, the "Focus" case (31 October): something he tells proposes
     * nothing and runs no writing check; asked to keep it with no block
     * named, the app asks him where, among his blocks and his day, and never
     * makes a block.
     */
    @Test
    fun somethingToldProposesNothingAndAskedToKeepTheAppAsksWhere() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planNewBlock("films")).ok)
        val before = app.storage.names().toSet()
        mind.talkScript += listOf("Fast ways sound good. What have you tried?", "It's below, for your yes.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("I'm trying to find fast ways to develop films")
            compose.waitFor("say", "Fast ways sound good.")
            assertEquals("no writing check", 0, mind.writeMessages)
            assertTrue("nothing waits on him", app.asking.history.last().waiting == null)
            asked("Keep that for me please.")
            compose.waitFor("where-words", "I'm trying to find fast ways to develop films")
            compose.waitFor("where:films")
            compose.waitFor("where-day")
            assertEquals("the model doesn't choose", 0, mind.writeMessages)
            compose.tap("where:films")
            compose.waitFor("writes")
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, "films")!!.lines.any { it.text == "I'm trying to find fast ways to develop films" } }
        }
        // 0.7: her heart and her ways are made by the app the first time she talks, his to change; never a block she proposed.
        assertEquals("never a new block", before + Asks.LOG + com.phenomemental.pocketbeach.bsp.HerWindow.HERS, app.storage.names().toSet())
        assertTrue(app.asking.transcript(), app.asking.transcript().contains("Matthew: Keep that for me please."))
    }

    /** 0.6.4: a name he gives her is one line on her shell, by code; given again, nothing. */
    @Test
    fun aNameForHerIsProposedByCode() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        mind.talkScript += listOf("Gemma it is.", "Still Gemma.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("I'd like to call you Gemma")
            compose.waitFor("writes")
            assertEquals("no writing check", 0, mind.writeMessages)
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, HelloWorld.SHELL)!!.lines.any { it.text == "Matthew calls me Gemma." } }
            asked("I'd like to call you Gemma")
            compose.waitFor("say", "Still Gemma.")
            compose.waitUntil(5_000) { app.asking.settled }
            assertTrue("one home: nothing proposed again", app.asking.history.last().waiting == null)
        }
        assertEquals(1, app.pages.position(Region.POCKET, HelloWorld.SHELL)!!.lines.count { it.text == "Matthew calls me Gemma." })
    }

    /** 0.6.4, the guards in code: her shell's opening line, the app's own blocks, a block he didn't name, words not his, a post he didn't ask for: never proposed. */
    @Test
    fun theGuardsLetNothingByThatBreaksThePrinciples() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planNewBlock("garden")).ok)
        mind.script += listOf(
            "act\nchange ${HelloWorld.SHELL}: I am Gemma.",
            "act\nadd now: buy more compost",
            "act\nadd garden: Compost, more of it\npost: buy more compost",
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("Change your opening line to: I am Gemma.")
            compose.waitFor("over", "Nothing proposed: her shell's opening line is his to change")
            asked("Make a block called now: buy more compost")
            compose.waitFor("over", "Nothing proposed: now is the app's own")
            asked("Keep this in garden: buy more compost.")
            compose.waitUntil(10_000) { app.asking.history.last().note.isNotEmpty() }
            assertTrue(app.asking.history.last().note, app.asking.history.last().note.contains("isn't his words"))
        }
        assertEquals(HelloWorld.SHELL_OPENING, app.pages.position(Region.POCKET, HelloWorld.SHELL)!!.text)
        assertFalse(app.pages.exists(Landing.NOW))
        assertTrue(app.pages.position(Region.POCKET, "garden")!!.lines.isEmpty())
        assertTrue("nothing posted", wire.sent.isEmpty())
    }

    /** 0.6.4: the ask log is written only when he turns it on in Settings; off, nothing is. */
    @Test
    fun theAskLogIsWrittenOnlyWhenHeTurnsItOn() {
        Fresh.clear()
        app.keepAskLog = false
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("Hello")
            compose.waitUntil(5_000) { app.asking.settled }
            assertFalse("off: no ask log", app.pages.exists(Asks.LOG))
            compose.menu("menu-blocks")
            compose.tap("settings")
            compose.tap("ask-log")
            compose.waitUntil(5_000) { app.keepAskLog }
            compose.tap("back")
            compose.tap("ask-companion")
            asked("Hello again")
            compose.waitUntil(10_000) { app.pages.exists(Asks.LOG) }
        }
    }

    @Test
    fun notNowWritesNothingAndLogsWhy() {
        Fresh.open().use {
            askTheCompanion()
            compose.tap("not-now")
            compose.tap("wrong-block")
            compose.waitFor("over", "not now: wrong block")
            assertFalse(app.pages.exists(HelloWorld.BLOCK))
            assertTrue(wire.sent.isEmpty())
            compose.waitUntil(10_000) { log()?.lines?.isNotEmpty() == true }
            val entry = app.pages.position(Region.POCKET, Asks.LOG, log()!!.lines.first().address)!!
            assertEquals("Matthew said not now: wrong block.", entry.lines[1].text)
        }
    }

    @Test
    fun theHelloWorldByHandOnTheTestScreen() {
        Fresh.open().use {
            compose.tap("about")
            compose.tap("calls")
            compose.waitFor("hello-secret")
            compose.onNodeWithTag("hello-secret").performTextInput("otters hold hands")
            compose.tap("hello-make")
            compose.waitFor("hello-made", "hello-world made")
            assertTrue(HelloWorld.verify(app.storage.read(HelloWorld.BLOCK)!!))
            compose.waitFor("hello-preview", HelloWorld.SHELL_OPENING)
            compose.tap("hello-post")
            compose.waitFor("hello-message", "Secret: otters hold hands")
            assertTrue(wire.sent.isEmpty())
            compose.tap("hello-confirm")
            compose.waitFor("hello-said", "Posted to your pool at entry 7")
            assertEquals(1, wire.sent.size)
            assertTrue(wire.sent.single().second.contains(HelloWorld.messageFor(app.storage.read(HelloWorld.BLOCK))!!))

            // Once posted, the same message goes again only on purpose.
            compose.onNodeWithTag("hello-post").assertIsNotEnabled().assertTextContains("Posted at entry 7")
            compose.tap("hello-post-again")
            compose.waitFor("hello-message")
            compose.tap("hello-not-now")
            // The dialog is gone before the link beneath it is tapped again.
            compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("hello-confirm"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            assertEquals(1, wire.sent.size)
            compose.tap("hello-post-again")
            compose.waitFor("hello-message")
            compose.tap("hello-confirm")
            compose.waitUntil(10_000) { wire.sent.size == 2 }
        }
    }

    @Test
    fun aBlockSharedInIsDataUntilKept() {
        Fresh.clear()
        val send = Intent(Intent.ACTION_SEND).setClass(app, MainActivity::class.java).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, """{"_":"Notes from a friend","1":"one","2":"two"}""")
            .putExtra(Intent.EXTRA_SUBJECT, "friend")
        ActivityScenario.launch<MainActivity>(send).use {
            compose.waitFor("received-opening", "Notes from a friend")
            assertFalse("shown, not kept", app.pages.exists("friend"))
            compose.tap("keep")
            compose.waitFor("standing", "Notes from a friend")
            assertTrue(app.pages.exists("friend"))
        }
    }

    @Test
    fun theModelIsOfferedOnlyWhereItCanRun() {
        app.mind = app.gemma
        Fresh.open().use {
            compose.tap("ask-companion")
            compose.waitUntil(15_000) {
                listOf("too-little-memory", "too-little-room", "get-model").any { compose.onAllNodes(hasTestTag(it), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            }
            val enough = app.models.memory() >= ModelStore.MEMORY_NEEDED
            val offered = compose.onAllNodes(hasTestTag("get-model"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            if (!enough) assertFalse("never offered without the memory", offered)
            // Nothing is fetched: no test taps "Get the model".
        }
    }

    @Test
    fun aDownloadKeptPartWayIsShownAndCanBeDiscarded() {
        app.mind = app.gemma
        Fresh.clear()
        val part = File(app.getExternalFilesDir(null), "models/${ModelStore.FILE}.part").apply { parentFile!!.mkdirs(); writeBytes(ByteArray(4096)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            compose.waitFor("paused", "Paused at 0.0 GB of 2.6 GB")
            // "Carry on" would start the real download, so no test taps it.
            compose.tap("discard")
            compose.waitUntil(10_000) { !part.exists() }
        }
    }

    @Test
    fun aFailureIsShownWithItsRealReason() {
        app.mind = app.gemma
        Fresh.clear()
        app.getSharedPreferences("model", 0).edit().putString("failed", "us.aws.cdn.hf.co answered HTTP 403: Forbidden").commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            compose.waitFor("model-failed", "us.aws.cdn.hf.co answered HTTP 403: Forbidden")
        }
        Fresh.clear()
    }

    @Test
    fun plainHttpIsRefusedEverywhere() {
        val policy = NetworkSecurityPolicy.getInstance()
        assertFalse(policy.isCleartextTrafficPermitted)
        assertFalse(policy.isCleartextTrafficPermitted("beach.happyseaurchin.com"))
        assertFalse(policy.isCleartextTrafficPermitted("example.com"))
    }

    private fun sealOf(): String =
        app.pages.position(Region.POCKET, HelloWorld.BLOCK)!!.lines.mapNotNull { it.text }.first { it.startsWith("Seal: ") }.removePrefix("Seal: ")
}
