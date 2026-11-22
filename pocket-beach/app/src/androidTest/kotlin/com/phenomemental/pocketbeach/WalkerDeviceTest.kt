package com.phenomemental.pocketbeach

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.bsp.BeachReader
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Reach
import com.phenomemental.pocketbeach.bsp.WireReply
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.12, the walker (spine:pocket-beach 1.111178719995): a block of
 * his walked as the beach's bsp() prints it; the beach asked nothing until he
 * turns Reach the beach on; then a block of the beach's read, walked here,
 * and copied into his pocket on his yes, nothing written to the beach.
 */
@RunWith(AndroidJUnit4::class)
class WalkerDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val wire = NowhereWire()
    private val asked = mutableListOf<String>()
    private val demo = JsObj.of("_" to str("A demo spine."), "1" to str("The first decision."), "2" to JsObj.of("_" to str("The second."), "1" to str("Its reason.")))

    @Before
    fun standIns() {
        app.wire = wire
        app.reader = BeachReader { url ->
            asked += url
            when (url) {
                Reach.indexUrl() -> WireReply(200, """{"_":"URL surface.","blocks":["lighthouse","spine:demo"]}""")
                Reach.blockUrl("spine:demo") -> WireReply(200, stringify(demo)!!)
                else -> WireReply(404, "{}")
            }
        }
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.reader = BeachReader { HttpsWire.get(it) }
        app.reachBeach = false
    }

    private fun walk(name: String, spindle: String = "") {
        compose.waitFor("walk-block-name")
        compose.onNodeWithTag("walk-block-name").performTextReplacement(name)
        compose.onNodeWithTag("walk-spindle").performTextReplacement(spindle)
        compose.tap("walk-go")
    }

    @Test
    fun aBlockOfHisIsWalkedAsTheBeachPrintsIt() {
        Fresh.clear()
        check(app.pages.carryOut(app.pages.planKeep("orchard", JsObj.of("_" to str("The orchard."), "1" to JsObj.of("_" to str("Autumn"), "1" to str("pick the apples"))))).ok)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("open-walker")
            walk("orchard")
            compose.waitFor("walk-out", "[disc @ pscale 0")
            // A walk to an address, no pscale: the path down to it, as the beach's bsp walks (its children need a pscale).
            walk("orchard", "1.1")
            compose.waitFor("walk-out", "pick the apples")
            val config = compose.onNodeWithTag("walk-out", useUnmergedTree = true).fetchSemanticsNode().config
            val shown = config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString("") { t -> t.text }
            assertEquals(Reach.walkPocket(app.pocket, "orchard", "1.1", null), shown)
            assertTrue("his walk asks nothing of the beach", asked.isEmpty())
        }
    }

    @Test
    fun theBeachIsReadOnlyOnceHeTurnsReachOnAndABlockIsCopiedOnHisYes() {
        Fresh.clear()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.tap("open-walker")
            compose.tap("walk-beach")
            compose.waitFor("walk-note", "Reach the beach is off")
            assertTrue("off: nothing asked of the beach", asked.isEmpty())
            // On, in Settings.
            compose.tap("back")
            compose.tap("settings")
            compose.tap("reach-beach")
            compose.waitUntil(5_000) { app.reachBeach }
            compose.tap("back")
            compose.tap("open-walker")
            compose.tap("walk-beach")
            compose.tap("walk-list")
            compose.waitFor("walk-name:spine:demo")
            compose.tap("walk-name:spine:demo")
            compose.tap("walk-go")
            compose.waitFor("walk-out", "The second.")
            assertEquals(listOf(Reach.indexUrl(), Reach.blockUrl("spine:demo")), asked.toList())
            // Walked again at an address: read once, walked here.
            compose.onNodeWithTag("walk-spindle").performTextInput("2.1")
            compose.tap("walk-go")
            compose.waitFor("walk-out", "Its reason.")
            assertEquals("the block was read once", 2, asked.size)
            // Copied into his pocket on his yes.
            compose.tap("copy-block")
            compose.waitFor("copy-card")
            assertTrue("nothing written before his yes", !app.pages.exists("spine:demo"))
            compose.tap("copy-yes")
            compose.waitFor("walk-note", "Copied into your pocket as spine:demo.")
            assertTrue("copied whole", com.phenomemental.pocketbeach.bsp.deepEqual(demo, com.phenomemental.pocketbeach.bsp.parseJson(app.storage.read("spine:demo")!!)))
            // A name already in his pocket is refused, and named.
            compose.tap("copy-block")
            compose.tap("copy-yes")
            compose.waitFor("walk-note", "There's already a block called spine:demo.")
            assertTrue("nothing written to the beach", wire.sent.isEmpty())
        }
    }
}
