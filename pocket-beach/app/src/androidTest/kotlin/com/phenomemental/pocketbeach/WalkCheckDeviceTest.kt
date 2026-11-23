package com.phenomemental.pocketbeach

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phenomemental.pocketbeach.net.HttpsWire
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.12.1, the check on his phone (spine:pocket-beach 1.1111787199993):
 * twenty walk calls on the GPU with known addresses, and how many arrived
 * intact, shown in Settings and kept for 0.13.
 */
@RunWith(AndroidJUnit4::class)
class WalkCheckDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val mind = StandIn()

    @Before
    fun standIns() {
        app.wire = NowhereWire()
        app.mind = mind
        app.walkCheck = null
    }

    @After
    fun backAsTheyWere() {
        app.wire = HttpsWire
        app.mind = app.gemma
        app.walkCheck = null
    }

    private fun check() {
        compose.menu("menu-settings")
        compose.tap("walk-check")
        compose.waitFor("walk-check-result", timeoutMs = 20_000)
    }

    @Test
    fun twentyAddressesSentAsTextAllArriveIntact() {
        Fresh.hatched()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            check()
            compose.waitFor("walk-check-result", "20 of 20 arrived intact.")
            assertEquals(20, mind.toolSettings.size)
            assertTrue("each on the GPU", mind.toolSettings.all { it.processor == "GPU" })
            assertTrue(compose.onAllNodes(hasTestTag("walk-check-altered"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
            assertEquals(20, app.walkCheck!!.intact)
            assertEquals(0, app.walkCheck!!.altered)
            assertTrue(app.walkCheck!!.allIntact)
        }
    }

    @Test
    fun addressesArrivingAsNumbersAreCountedAltered() {
        Fresh.hatched()
        mind.toolNumbers = true
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitFor("dial")
            check()
            // Sent as numbers, every address is counted altered, the long one losing nothing here but its kind.
            compose.waitFor("walk-check-result", "0 of 20 arrived intact.")
            compose.waitFor("walk-check-altered", "1.111178719993 arrived as a number")
            assertEquals(0, app.walkCheck!!.intact)
            assertEquals(20, app.walkCheck!!.altered)
            assertTrue(!app.walkCheck!!.allIntact)
        }
    }
}
