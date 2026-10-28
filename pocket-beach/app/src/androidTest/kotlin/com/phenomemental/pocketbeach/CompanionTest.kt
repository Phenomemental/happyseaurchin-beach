package com.phenomemental.pocketbeach

import android.content.Intent
import android.security.NetworkSecurityPolicy
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talk
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.Wire
import com.phenomemental.pocketbeach.bsp.WireReply
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.ModelStore
import com.phenomemental.pocketbeach.model.Pace
import java.io.File
import com.phenomemental.pocketbeach.net.HttpsWire
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
 * in actions. Anything else it answers in plain words.
 */
class StandIn : Mind {
    var instruction = ""
    /** The earlier exchanges each 0.4 conversation was opened with. */
    val histories = mutableListOf<List<Pair<String, String>>>()
    override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean): Mind.Talker {
        this.instruction = instruction
        histories += history
        return object : Mind.Talker {
            override val stream = Talks.Stream { message, piece ->
                val reply = talk(message)
                reply.chunked(4).forEach(piece)
                reply
            }
            override val pace = Pace(processor, null, null)
            override fun stop() {}
            override fun close() {}
        }
    }

    /** Replies to give first, in order, before answering as above. */
    val script = ArrayDeque<String>()

    fun talk(message: String): String {
        script.removeFirstOrNull()?.let { return it }
        if (!message.contains("hello world", ignoreCase = true)) return "talk\nI'm a stand-in for the companion."
        val secret = message.substringAfter("secret is ", "").trim().removeSuffix(".")
        val again = instruction.substringAfter("don't make it: answer ", "").substringBefore(", writing the secret", "")
        val lines = again.ifEmpty { instruction.substringAfter("changing nothing else: ", "").substringBefore(". (Each", "") }
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
        compose.tap("ask-companion")
        compose.waitFor("make-shell")
        compose.tap("Make the companion's shell")
        compose.waitFor("wearing", "Wearing pocket-companion")
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
            assertTrue(mind.instruction.contains("hello-world is already in the pocket"))
            assertTrue("in the action language", mind.instruction.contains("act / change hello-world \"Secret: an old secret\""))
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

            compose.tap("back")
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
            compose.tap("open-bench")
            compose.tap("run-bench")
            compose.waitFor("bench-saved", "Kept as bench-", timeoutMs = 120_000)
            val made = app.storage.names().toSet() - before
            assertEquals("one new block, the run", 1, made.size)
            assertTrue(made.single().startsWith("bench-"))
            assertEquals("nothing else of the pocket changed", before, app.storage.names().toSet() - made)
            assertFalse(app.pages.exists(HelloWorld.BLOCK))
            assertTrue("nothing posted", wire.sent.isEmpty())
            val results = app.benching.results
            assertEquals("twenty asks before, twenty after with her first word held, twenty with it free", 60, results.size)
            assertTrue("the stand-in's hello world is right, before", results[19].right)
            assertTrue("and after, held", results[39].right)
            assertTrue("and free", results.last().right)
            val run = app.pages.position(Region.POCKET, made.single())!!
            assertTrue(run.text!!.startsWith("Bench, "))
            assertEquals(4, run.lines.size)
            assertTrue(run.lines[0].text!!.startsWith("0.3 "))
            assertTrue(run.lines[1].text!!.startsWith("0.4, her first word held"))
            assertTrue(run.lines[2].text!!.startsWith("0.4, her first word free"))
            assertEquals("How it was run", run.lines[3].text)
            compose.tap("open-bench-block")
            compose.waitFor("standing", "Bench, ")
        }
    }

    @Test
    fun askIsAConversation() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.pages.carryOut(app.pages.planNewBlock("garden"))
        app.pages.carryOut(app.pages.planAdd("garden", "", "Autumn: plant the bulbs"))
        app.pages.carryOut(app.pages.planAdd("garden", "", "Winter: prune the roses"))
        mind.script += listOf("talk\nI'm the pocket companion.", "act\nadd garden under \"the\": mulch the beds")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            compose.waitFor("question")
            compose.onNodeWithTag("question").performTextInput("What's your name?")
            compose.tap("ask")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "first words after")
            compose.onNodeWithTag("question").performTextClearance()
            compose.onNodeWithTag("question").performTextInput("Under the line with the, note: mulch the beds.")
            compose.tap("ask")
            // The quote matches both lines: Matthew says which before anything is planned.
            compose.waitFor("which-line", "2 lines say that")
            assertEquals("the first exchange is shown above", 1, compose.onAllNodes(hasTestTag("earlier"), useUnmergedTree = true).fetchSemanticsNodes().size)
            compose.tap("choose:2")
            compose.waitFor("writes")
            compose.waitFor("yes")
            assertFalse("nothing written before yes", app.pages.position(Region.POCKET, "garden", "2")!!.lines.isNotEmpty())
            compose.tap("yes")
            compose.waitUntil(10_000) { app.pages.position(Region.POCKET, "garden", "2.1")?.text == "mulch the beds" }
            assertEquals("one conversation for both asks, opened with the examples", 1, mind.histories.size)
            assertEquals(Talks.EXAMPLES, mind.histories.single())
        }
    }

    private fun asked(question: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextClearance()
        compose.onNodeWithTag("question").performTextInput(question)
        compose.tap("ask")
    }

    @Test
    fun timingsShowUnderEveryAnswerWhenOnAndAreAlwaysLogged() {
        Fresh.clear()
        HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
        app.pages.carryOut(app.pages.planNewBlock("garden"))
        app.pages.carryOut(app.pages.planAdd("garden", "", "Autumn: plant the bulbs"))
        mind.script += listOf(
            "talk\nI'm the pocket companion.",
            "read\nread garden",
            "talk\nIt says: Autumn: plant the bulbs.",
            "act\nadd garden: buy more compost",
            "talk\nYou're welcome.",
        )
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("ask-companion")
            // On (the default): talk, read and act each show the time and where she ran.
            asked("What's your name?")
            compose.waitFor("over", "She talked.")
            compose.waitFor("took", "on the stand-in")
            asked("What's in my garden block?")
            compose.waitFor("over", "She read 1 block, then answered.")
            compose.waitFor("took", "on the stand-in")
            asked("Add to garden: buy more compost.")
            compose.waitFor("writes")
            compose.waitFor("took", "on the stand-in")
            compose.tap("not-now")
            compose.tap("just-not-now")
            compose.waitFor("over", "Matthew said not now.")

            // Off: nothing extra under any answer, the earlier ones too.
            app.showTimings = false
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("took"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            asked("Thanks.")
            compose.waitFor("over", "She talked.")
            assertTrue(compose.onAllNodes(hasTestTag("took"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertTrue(compose.onAllNodes(hasText("Took ", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        }
        // The ask log keeps every answer's time, shown or not.
        val log = app.pages.position(Region.POCKET, Asks.LOG)!!
        assertEquals(4, log.lines.size)
        for (entry in log.lines) assertTrue(app.pages.position(Region.POCKET, Asks.LOG, entry.address)!!.lines.any { it.text?.startsWith("Took ") == true })
        app.showTimings = true
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
            assertEquals(1, wire.sent.size)
            compose.tap("hello-post-again")
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
