package com.phenomemental.pocketbeach

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.bsp.collectUnderscore
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import com.phenomemental.pocketbeach.net.HttpsWire
import com.phenomemental.pocketbeach.ui.Hatching
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 0.8, the hatch on the phone (spine:pocket-beach 1.1111787133), with a
 * stand-in for the model: born at the welcome, or his passport adopted from
 * a file; a whole copy saved first; her window the nine places of his shell;
 * "Keep that" set out for his stash; and the close's one history entry.
 */
@RunWith(AndroidJUnit4::class)
class HatchDeviceTest {
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

    private fun block(name: String): JsObj = parseJson(app.storage.read(name)!!) as JsObj

    private fun answer(words: String) {
        compose.waitFor("question")
        compose.onNodeWithTag("question").performTextInput(words)
        compose.tap("ask")
    }

    @Test
    fun theWelcomeBearsAShellOfFourAndSheWearsIt() {
        Fresh.open().use {
            compose.tap("ask-companion")
            compose.waitFor("hatch-opening", Hatching.OPENING)
            compose.tap("hatch-new")
            compose.waitFor("hatch-question", "A handle")
            answer("two words")
            compose.waitFor("hatch-note", "one word")
            compose.waitFor("hatch-question", "A handle")
            answer("ana")
            compose.waitFor("hatch-question", "Who are you")
            answer("Ana — a gardener in Leeds.")
            compose.waitFor("hatch-question", "Your heart")
            answer("Grow what feeds people.")
            compose.waitFor("hatch-question", "How would you like her to meet you")
            answer("Plainly, and briefly.")
            compose.waitFor("hatch-yes-passport")
            assertFalse("nothing written before his yes", app.pages.exists("passport:ana"))
            compose.tap("hatch-yes-passport")
            compose.tap("hatch-make")
            compose.waitFor("hatch-done", "shell:ana", timeoutMs = 20_000)
            for (n in Hatch.fourOf("ana")) assertTrue("$n made", app.pages.exists(n))
            assertEquals("ana — Ana — a gardener in Leeds.", collectUnderscore(block("passport:ana")))
            assertTrue("a whole copy saved first", File(app.filesDir, Hatching.COPIES).listFiles()!!.any { it.name.startsWith("before-the-hatch-") })
            // She wears his shell: her window is its nine places.
            answer("Hello")
            compose.waitFor("say")
            assertTrue(mind.talkInstruction, mind.talkInstruction.contains("1. His heart: “Grow what feeds people.”"))
            assertTrue(mind.talkInstruction, mind.talkInstruction.contains("8. ${Hatch.WAY}"))
        }
    }

    @Test
    fun hisPassportAdoptedFromAFileKeepsHisBlocksAndKeepThatGoesToHisStash() {
        Fresh.open().use {
            // A 0.7 pocket: her shell named, a block of his.
            HelloWorld.makeShell(app.pages, Talks.SHELL_RECIPE)
            check(app.pages.carryOut(app.pages.planAdd(HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
            check(app.pages.carryOut(app.pages.planNewBlock("garden")).ok)
            compose.tap("ask-companion")
            compose.waitFor("hatch-opening")
            // The file picker can't be driven: the file, as Ask reads it.
            val passport = JsObj.of("_" to str("Matthew Bushell — digital social innovator."), "1" to str("The heart."))
            app.hatching.read(stringify(JsObj.of("passport:Phenomemental" to passport))!!, "passport-Phenomemental.json")
            compose.waitFor("hatch-read-at", "From passport-Phenomemental.json")
            compose.waitFor("hatch-three", Hatch.MATTHEW.heart)
            compose.tap("hatch-adopt")
            compose.waitFor("hatch-done", "shell:Phenomemental", timeoutMs = 20_000)
            assertEquals(stringify(passport), stringify(block("passport:Phenomemental")))
            val shell = block("shell:Phenomemental")
            assertEquals(str("Matthew calls me Gemma."), (shell[Hatch.HER] as JsObj)["1"])
            assertTrue("his garden listed", stringify(shell["3"])!!.contains("\"garden\""))
            assertNotNull("her shell kept, in the hidden region", File(app.filesDir, "regions/app").listFiles()!!.firstOrNull { it.name.startsWith("pocket-companion") })
            assertTrue("his garden kept", app.pages.exists("garden"))
            // Keep that, with no block named: set out for his stash, his yes.
            answer("I've been making huge progress with my film making.")
            compose.waitFor("say")
            assertTrue(mind.talkInstruction, mind.talkInstruction.contains("1. His heart: “${Hatch.MATTHEW.heart}”"))
            answer("Keep that for me please")
            compose.waitFor("yes", timeoutMs = 20_000)
            compose.tap("yes")
            compose.waitUntil(20_000) { stringify(block("stash:Phenomemental"))!!.contains("huge progress") }
            assertTrue(mind.talkMessages.last(), mind.talkMessages.last().contains("It's below, for your yes."))
            // Leaving Ask closes the conversation: one history entry, by the app.
            compose.tap("back")
            compose.waitUntil(20_000) { block("history:Phenomemental").has("1") }
            val entry = block("history:Phenomemental")["1"] as JsObj
            assertTrue(stringify(entry)!!, collectUnderscore(entry)!!.startsWith("Talked with him: 2 messages, from “I've been making huge progress"))
            assertTrue(stringify(entry)!!, stringify(entry["2"])!!.contains("stash:Phenomemental"))
        }
    }
}
