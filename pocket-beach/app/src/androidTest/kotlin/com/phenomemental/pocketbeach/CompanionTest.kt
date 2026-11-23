package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.model.JobPrefs
import com.phenomemental.pocketbeach.model.ModelState
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
    /** Sundial 0.10.1: every instruction a Talk conversation was opened with, the greeting's among them. */
    val talkInstructions = mutableListOf<String>()
    /** Sundial 0.10.1: each Talk message with the instruction of the conversation it was given in. */
    val talkAsked = mutableListOf<Pair<String, String>>()
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
            talkInstructions += instruction
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
                // Sundial 0.10.1: an answer that stops part way, her words so far given first.
                if (talking) failAfter?.let { f -> failAfter = null; talkMessages += message; f.chunked(4).forEach(piece); throw RuntimeException("The engine stopped part way.") }
                val reply = if (talking) {
                    talkMessages += message
                    talkAsked += instruction to message
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

    /** Sundial 0.10.1: the next Talk answer gives these words, then stops part way. */
    @Volatile var failAfter: String? = null

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

    /** Sundial 0.10: the day's sheet she draws for the keep card, in plain lines. */
    var sheet = "A walk by the river\nYou took the dog out with Sam by the river.\nYou felt calm.\nTomorrow: ring Jo about the music"

    /** Her word for each piece of his day; "happened" for a piece not here. A card is about a piece when [about] says so. */
    val sortWords = mutableMapOf<String, String>()
    var about: (String) -> Boolean = { false }

    override suspend fun ask(instruction: String?, message: String, settings: JobSettings): Mind.Answer {
        sortAsks += instruction to message
        val text = when {
            // Sundial 0.10: the day's sheet, in plain lines (cook 6.1.5 on E2B).
            instruction?.contains("draw the day's sheet") == true -> sheet
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
    override fun state(): ModelState = ModelState.Ready

    /** Sundial 0.12.1: the walk check's calls: each address she is asked for comes back as text, or, with [toolNumbers], as a JSON number (as the GPU report had it). */
    var toolNumbers = false
    val toolSettings = java.util.concurrent.CopyOnWriteArrayList<JobSettings>()
    override suspend fun callTool(instruction: String, message: String, toolJson: String, settings: JobSettings): Mind.Called? {
        toolSettings += settings
        val address = Regex("""at (\S+) in my""").find(message)?.groupValues?.get(1) ?: return null
        return Mind.Called("bsp", mapOf("block" to "walk-check", "spindle" to (if (toolNumbers) address.toDouble() else address)), "")
    }

    /** Sundial 0.12.1: the settings each warm was asked for, in order. */
    val warmed = java.util.concurrent.CopyOnWriteArrayList<JobSettings>()
    override suspend fun warm(settings: JobSettings): Long? { warmed += settings; return null }
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

    private fun log() = app.pages.position(Region.POCKET, Asks.LOG)

    /**
     * Hatched: a post to his room the beach couldn't receive is kept, and goes
     * only on a tap. Sundial 0.12: posted from Your room's box, the chat
     * setting out no card (spine:pocket-beach 1.111178719993).
     */
    @Test
    fun aPostTheBeachCouldntReceiveIsKeptAndGoesOnlyOnATap() {
        val away = UnreachableWire()
        Fresh.hatched()
        app.wire = away
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("places-room")
            compose.waitFor("room-box")
            compose.onNodeWithTag("room-box").performTextInput("Back on Tuesday")
            compose.tap("room-post")
            compose.waitFor("room-note", "couldn't be reached")
            assertEquals(1, away.tries)
            val kept = app.unsent.all.single()
            assertTrue(kept.message.message.contains("Back on Tuesday"))
            assertEquals("Phenomemental", kept.message.author)
            assertFalse(kept.mayHaveLanded)
            compose.menu("menu-blocks")
            compose.waitFor("waiting")

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
            compose.waitFor("waiting")
            compose.tap("post-now")
            compose.waitFor("posted-later", "Posted to your pool at entry 7")
            assertEquals(1, wire.sent.size)
            assertEquals("${PoolPost.BEACH}/.well-known/pscale-beach?block=pool:Phenomemental", wire.sent.single().first)
            assertTrue(wire.sent.single().second.contains("Back on Tuesday"))
            assertTrue("no longer kept", app.unsent.all.isEmpty())
            assertEquals("7", app.posts.landed(kept.message))
        }
    }

    @Test
    fun aPostThatMayHaveLandedSaysSoAndCanBeDiscarded() {
        Fresh.clear()
        val message = PoolPost.prepare(HelloWorld.POOL, "A message whose answer never came.", com.phenomemental.pocketbeach.bsp.Room(com.phenomemental.pocketbeach.bsp.Hatch.MATTHEW_HANDLE, HelloWorld.POOL))
        var touched = 0
        app.wire = Wire { _, _ -> touched++; throw com.phenomemental.pocketbeach.bsp.NoAnswer("Read timed out") }
        // Sundial 0.10: before a hatch nothing is posted, and the wire isn't touched.
        assertTrue(app.post(message) is com.phenomemental.pocketbeach.bsp.Posted.Refused)
        assertEquals(0, touched)
        assertTrue(app.unsent.all.isEmpty())
        Fresh.hatched()
        // Hatched: a post made in a scratch pocket (the bench's) never leaves, nor one made as another handle for another room.
        val (bench, _) = Bench.fixture(app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE)
        val scratch = PoolPost.prepare(HelloWorld.POOL, "From the bench.", PoolPost.room(bench))
        assertEquals(PoolPost.SCRATCH, (app.post(scratch) as com.phenomemental.pocketbeach.bsp.Posted.Refused).why)
        val another = PoolPost.prepare("pool:ada-lovelace", "As someone else.", com.phenomemental.pocketbeach.bsp.Room("ada-lovelace", "ada-lovelace"))
        assertTrue(app.post(another) is com.phenomemental.pocketbeach.bsp.Posted.Refused)
        assertEquals(0, touched)
        // His own, from his own room: it goes, and no answer comes back.
        app.post(message)
        assertEquals(1, touched)
        assertTrue(app.unsent.all.single().mayHaveLanded)
        app.wire = wire
        ActivityScenario.launch(MainActivity::class.java).use {
            // Hatched, the app opens on Today; kept posts wait on Your pocket.
            compose.menu("menu-blocks")
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
            assertTrue(asks.joinToString("\n"), asks.single { it.startsWith("Keep 10.") }.let { it.startsWith("Keep 10. right") && it.contains("He calls me Gemma.") })
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
        // Sundial 0.12: the safety check is taken out, so no note shows beneath her reply to ask 24.
        assertFalse(lines.joinToString("\n"), lines.any { it.contains("The note beneath her reply") })
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
            compose.waitFor("say", "Yes: under one of the two lines")
            assertEquals("the first exchange is shown above, in the thread", 2, compose.onAllNodes(hasTestTag("turn"), useUnmergedTree = true).fetchSemanticsNodes().size)
            // Sundial 0.12, the plain chat: nothing is set out from his words, and nothing written.
            compose.waitUntil(10_000) { app.asking.settled }
            assertTrue("no which-line, no proposal", compose.onAllNodes(hasTestTag("which-line"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodes(hasTestTag("writes"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertTrue("garden as it stood", app.pages.position(Region.POCKET, "garden", "2")!!.lines.isEmpty())
            assertEquals("no Write conversation", 0, mind.histories.size)
            // 0.7: hers is kept between his messages, her window read once.
            assertEquals("one Talk conversation, kept between his messages", listOf(emptyList<Pair<String, String>>()), mind.talkHistories)
            // Sundial 0.12, Gemma released: her turn is the time and his words, no line of the app's.
            assertTrue("her turn carries the time and his words alone", mind.talkMessages.last().startsWith("(It is ") && !mind.talkMessages.last().contains("(The app: "))
        }
    }

    /**
     * Ask closes and opens as the model and the shell come ready, and the two
     * run on other threads in either order (CI, twice): the close must never
     * shut the conversation just opened. Sundial 0.12: before the hatch,
     * nothing is opened or warmed as Ask opens (spine:pocket-beach
     * 1.111178719992); hers opens with his message.
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
                Thread.sleep(300)
                assertEquals("nothing opened before the hatch as Ask opens (round ${it + 1})", writes, mind.histories.size)
                assertEquals("hers isn't opened until he speaks (round ${it + 1})", before, mind.talkHistories.size)
                main { asking.question = "Hello."; asking.ask() }
                compose.waitUntil(5_000) { asking.settled }
                assertEquals("one Talk conversation, for his message (round ${it + 1})", before + 1, mind.talkHistories.size)
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
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            // Details on: each answer shows the time and where she ran.
            asked("What's your name?")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "on the stand-in")
            asked("What does the whetstone say?")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "on the stand-in")
            // Sundial 0.12, Gemma released: nothing is read into her turn for her; the time, then his words.
            assertFalse(mind.talkMessages.last(), mind.talkMessages.last().contains("What the app read for you"))
            assertTrue("his words close her turn", mind.talkMessages.last().endsWith("\nWhat does the whetstone say?"))
            // Sundial 0.12, the plain chat: asked to keep, she talks, and nothing is set out.
            asked("Add to garden: buy more compost.")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "on the stand-in")
            assertTrue(compose.onAllNodes(hasTestTag("writes"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

            // Off: only the conversation.
            app.showDetails = false
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("took"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            asked("Thanks.")
            compose.waitUntil(10_000) { app.asking.settled }
            compose.waitFor("say", "I'm a stand-in for the companion, talking.")
            assertTrue(compose.onAllNodes(hasTestTag("took"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertTrue(compose.onAllNodes(hasText("Took ", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
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
        mind.talkScript += listOf("Good morning, Matthew. What's on your mind?", "A marathon! What's drawing you to it?", "A block called running sounds good; you can make it in Your pocket.")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            asked("Good morning")
            compose.waitFor("say", "Good morning, Matthew.")
            // Sundial 0.12, Gemma released (spine:pocket-beach 1.111178719991): her turn is the time, then his words;
            // her window is the time alone, before the hatch as after: no friend line, no "what she can do", no shell.
            assertEquals("(It is ", mind.talkMessages.single().take(7))
            assertTrue(mind.talkMessages.single(), mind.talkMessages.single().endsWith(")\nGood morning"))
            assertFalse(mind.talkMessages.single(), mind.talkMessages.single().contains("(The app: "))
            assertTrue(mind.talkInstruction, Regex("""It is \w+ \d+ \w+ \d{4}, \d\d:\d\d as this conversation opens\.""").matches(mind.talkInstruction))
            assertFalse(mind.talkInstruction, mind.talkInstruction.contains(HelloWorld.SHELL_OPENING))
            assertFalse("her instruction has no route words", mind.talkInstruction.contains("talk:"))
            assertEquals("a greeting asks no write", 0, mind.writeMessages)
            // 0.6.4: something told is not kept unasked; he names the block.
            asked("I want to run a marathon next spring.")
            assertEquals("told, not asked: no writing check", 0, mind.writeMessages)
            assertTrue("nothing waits on him", app.asking.history.last().waiting == null)
            asked("Make a block called running: I want to run a marathon next spring.")
            compose.waitFor("say", "A block called running sounds good")
            // Sundial 0.12, the plain chat: nothing set out, the Write job not asked, no block made.
            assertEquals(0, mind.writeMessages)
            assertTrue("she talked as Edge does", mind.talkSettings.all { it == JobSettings.of(Job.TALK) })
            assertFalse(app.pages.exists("running"))
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
        assertFalse("Sundial 0.12: no line of the app's in her turn", t.contains("The app's line to her"))
        assertTrue(t, t.contains("Left out of her reply, as her turn played back: "))
        assertTrue(t, t.contains("Companion: What would you like to keep in your blocks?"))
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
            assertEquals("kept on the phone", 40, JobPrefs.read(app.getSharedPreferences("pocket", 0), Job.TALK).topK)
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
     * 0.6.2: never two engines on the GPU. Talk's holds it; a job set to the GPU on another model runs on the CPU, and Settings and Ask say so.
     * Sundial 0.10.1 (fix 9): a job he never set apart runs on Talk's own engine, so no second model loads; one he set apart is as 0.6.2 has it.
     */
    @Test
    fun oneEngineOnTheGpuAndAJobMovedOffItSaysSo() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.set(Job.TALK, JobSettings.of(Job.TALK).copy(model = JobSettings.GEMMA_E4B))
        try {
            assertEquals("GPU", app.running(Job.TALK).processor)
            // Never set apart: on Talk's engine, E4B on the GPU, with nothing to say.
            for (job in listOf(Job.WRITE, Job.SORT)) {
                assertEquals(JobSettings.GEMMA_E4B, app.running(job).model)
                assertEquals("GPU", app.running(job).processor)
                assertEquals(null, app.cpuWhy(job))
            }
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("settings")
                compose.waitFor("job-write-follows", "Runs on her talk's model, Gemma 4 E4B")
            }
            // Write set apart (its own context): on E2B, which can't share the GPU with Talk's E4B, so on the CPU, and said.
            app.set(Job.WRITE, JobSettings.of(Job.WRITE).copy(context = 4096))
            assertEquals("CPU", app.running(Job.WRITE).processor)
            assertEquals("Talk's Gemma 4 E4B holds the GPU, and the phone runs one engine on it at a time", app.cpuWhy(Job.WRITE))
            assertEquals("GPU", app.running(Job.SORT).processor)
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.tap("settings")
                compose.waitFor("job-write-processor-hint", "Runs on the CPU: Talk's Gemma 4 E4B holds the GPU")
                assertTrue("Talk runs as set", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("job-talk-processor-hint")).fetchSemanticsNodes().isEmpty())
                compose.tap("back")
                compose.tap("ask-companion")
                asked("Hello.")
                compose.waitUntil(10_000) { mind.talkSettings.isNotEmpty() }
                // Sundial 0.12: the plain chat asks no Write job; hers runs on the GPU as set.
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

    /**
     * Sundial 0.12, the plain chat (spine:pocket-beach 1.111178719993): his
     * words asking to change, make, keep or post are talked with, and nothing
     * is set out, written or sent; the Write job isn't asked. Her shell's
     * opening line, the app's own blocks and his are left as they stand.
     */
    @Test
    fun theChatWritesNothingAndSetsNothingOut() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        check(app.pages.carryOut(app.pages.planNewBlock("garden")).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            for (words in listOf(
                "Change your opening line to: I am Gemma.",
                "Make a block called now: buy more compost",
                "Keep this in garden: buy more compost.",
                "Post this to my room: \"Back on Tuesday\"",
            )) {
                asked(words)
                compose.waitUntil(10_000) { app.asking.settled }
                assertEquals(words, null, app.asking.history.last().waiting)
            }
            assertTrue("no card beneath her replies", compose.onAllNodes(hasTestTag("yes"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertTrue("no proposal", compose.onAllNodes(hasTestTag("writes"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        }
        assertEquals("the Write job isn't asked", 0, mind.writeMessages)
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
    fun theHelloWorldByHandOnTheTestScreen() {
        // Sundial 0.10: posted from the shell he hatched as, to his room.
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-about")
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
