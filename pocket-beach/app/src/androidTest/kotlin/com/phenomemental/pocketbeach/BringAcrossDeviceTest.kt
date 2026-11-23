package com.phenomemental.pocketbeach

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.BeachReader
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Reach
import com.phenomemental.pocketbeach.bsp.WireReply
import com.phenomemental.pocketbeach.bsp.deepEqual
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.13.5, bring my blocks across: with Reach the beach on, the
 * beach's blocks listed, his first, each with a tick box; the ticked ones
 * copied in on one yes, new or replacing, one whole copy of his pocket saved
 * before; what came across, and anything refused with why, said beneath.
 */
@RunWith(AndroidJUnit4::class)
class BringAcrossDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val wire = NowhereWire()
    private val asked = mutableListOf<String>()
    private val passport = JsObj.of("_" to str("Matthew Bushell, as the beach has it."), "5" to str("How to work with me."))
    private val shell = JsObj.of("_" to str("Phenomemental's shell on the beach."), "1" to str("Faces."))
    private val demo = JsObj.of("_" to str("A demo spine."), "1" to str("The first decision."))

    @Before
    fun standIns() {
        app.wire = wire
        app.reader = BeachReader { url ->
            asked += url
            when (url) {
                Reach.indexUrl() -> WireReply(200, """{"_":"URL surface.","blocks":["lighthouse","spine:demo","passport:Phenomemental","broken","shell:Phenomemental"]}""")
                Reach.blockUrl("passport:Phenomemental") -> WireReply(200, stringify(passport)!!)
                Reach.blockUrl("shell:Phenomemental") -> WireReply(200, stringify(shell)!!)
                Reach.blockUrl("spine:demo") -> WireReply(200, stringify(demo)!!)
                Reach.blockUrl("broken") -> WireReply(200, "not a block")
                else -> WireReply(500, "{}")
            }
        }
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.reader = BeachReader { HttpsWire.get(it) }
        app.reachBeach = false
    }

    @Test
    fun theTickedBlocksComeAcrossOnOneYesAndWhatIsRefusedSaysWhy() {
        Fresh.hatched()
        app.reachBeach = true
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("menu-settings")
            compose.tap("to-bring-across")
            // His blocks first, by his handle.
            compose.waitFor("bring-row:passport:Phenomemental")
            compose.waitFor("bring-row:lighthouse")
            for (n in listOf("passport:Phenomemental", "shell:Phenomemental", "spine:demo", "broken")) compose.tap("tick:$n")
            compose.tap("bring-go")
            // Read only, then set out on one card: his shell's changes named before his yes.
            compose.waitFor("bring-card-said", "Bring 3 into your pocket: 1 new, 2 in place of yours.")
            compose.waitFor("bring-shell-changes", "These places of your shell will change:")
            assertFalse("nothing written before his yes", app.pages.exists("spine:demo"))
            assertTrue(app.beachCopies().isEmpty())
            compose.tap("bring-yes")
            compose.waitFor("bring-report")
            compose.waitFor("came:passport:Phenomemental", "passport:Phenomemental, replacing yours")
            compose.waitFor("came:shell:Phenomemental", "shell:Phenomemental, replacing yours")
            compose.waitFor("came:spine:demo", "spine:demo, new")
            compose.waitFor("refused:broken", "broken: the beach's answer isn't a block")
            assertTrue(deepEqual(demo, app.pages.whole("spine:demo")!!))
            assertTrue(deepEqual(passport, app.pages.whole("passport:Phenomemental")!!))
            assertTrue(deepEqual(shell, app.pages.whole("shell:Phenomemental")!!))
            assertEquals("one whole copy saved first", 1, app.beachCopies().size)
            assertTrue("nothing written to the beach", wire.sent.isEmpty())
            assertFalse("lighthouse, unticked, never asked for", asked.contains(Reach.blockUrl("lighthouse")))
        }
    }

    @Test
    fun withReachTheBeachOffThereIsNoWayInAndNothingIsAsked() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            compose.menu("menu-settings")
            compose.waitFor("reach-part")
            assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasTestTag("to-bring-across"), useUnmergedTree = true).fetchSemanticsNodes().size)
            assertTrue("nothing asked of the beach", asked.isEmpty())
        }
    }
}
