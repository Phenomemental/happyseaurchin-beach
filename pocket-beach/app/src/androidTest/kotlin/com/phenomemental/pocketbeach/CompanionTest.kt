package com.phenomemental.pocketbeach

import android.content.Intent
import android.security.NetworkSecurityPolicy
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talk
import com.phenomemental.pocketbeach.bsp.Wire
import com.phenomemental.pocketbeach.bsp.WireReply
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.ModelStore
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

/** A wire that goes nowhere: it keeps what would have been sent and answers as the beach does. No test posts to the beach. */
class NowhereWire : Wire {
    val sent = mutableListOf<Pair<String, String>>()
    override fun post(url: String, body: String): WireReply {
        sent += url to body
        return WireReply(200, """{"slot":"7","block":"pool:Phenomemental"}""")
    }
}

/** A stand-in for the companion, since no test can run the model: it answers the hello world as the shell describes it. */
class StandIn : Mind {
    var instruction = ""
    override fun state(): ModelStore.State = ModelStore.State.Ready
    override val loaded = true
    override val processor = "stand-in"
    override suspend fun chat(instruction: String): Mind.Chat {
        this.instruction = instruction
        return object : Mind.Chat {
            override val talk = Talk { message -> answer(message) }
            override fun stop() {}
            override fun close() {}
        }
    }

    fun answer(message: String): String {
        val secret = message.substringAfter("secret is ", "").trim().removeSuffix(".")
        val steps = listOf("""{"do":"make","block":"hello-world","at":"","text":""}""") +
            HelloWorld.SHELL_LINES.map { """{"do":"add","block":"hello-world","at":"","text":"${it.replace("HIS SECRET", secret)}"}""" }
        return """{"say":"I'll make hello-world and offer the post.","steps":[${steps.joinToString(",")}],""" +
            """"post":{"pool":"Phenomemental","message":"${HelloWorld.SHELL_POST.replace("HIS SECRET", secret)}"}}"""
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
            compose.onNodeWithTag("hello-post", useUnmergedTree = true).assertIsNotEnabled().assertTextContains("Posted at entry 7")
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
