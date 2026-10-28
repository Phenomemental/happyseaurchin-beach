package com.phenomemental.pocketbeach

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phenomemental.pocketbeach.bsp.Conformance
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.JsObj
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The pocket on a real Android runtime: the shelf from the app's own assets, blocks in its own files. */
@RunWith(AndroidJUnit4::class)
class OnDeviceTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PocketApp

    @Test
    fun theWhetstoneComesFromTheShelf() {
        val reply = app.pocket.call("""{"agent_id":"pscale","block":"whetstone"}""")
        assertTrue(reply, reply.startsWith("[disc @ pscale 0 (depth 1)]"))
        assertTrue(reply, reply.contains("[2]: Selection-shape derivation"))
        assertTrue(reply, reply.contains("the whole block is 40810 characters"))
    }

    @Test
    fun aLineWrittenIsReadBackFromItsOwnFile() {
        val name = "device-test-${System.nanoTime()}"
        val wrote = app.pocket.call("""{"agent_id":"pocket","block":"$name","spindle":"1","content":"plant the bulbs"}""")
        assertTrue(wrote, wrote.contains("this write created \"$name\""))
        val read = app.pocket.call("""{"agent_id":"pocket","block":"$name","spindle":"1"}""")
        assertTrue(read, read.contains("plant the bulbs"))
        val file = File(app.filesDir, "regions/pocket/$name.json")
        assertTrue("stored at ${file.path}", file.isFile)
        assertEquals("plant the bulbs", ((parseJson(file.readText()) as JsObj)["1"] as com.phenomemental.pocketbeach.bsp.JsStr).value)
        file.delete()
    }

    @Test
    fun theBeachIsOutOfReach() {
        assertEquals(PocketBsp.BEACH_OUT_OF_REACH, app.pocket.call("""{"agent_id":"https://beach.happyseaurchin.com","block":"marks"}"""))
    }

    @Test
    fun everyGoldenCallMatchesOnTheDevice() {
        val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
        val golden = testAssets.open("golden.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val scratch = File(app.cacheDir, "golden")
        val report = Conformance.replay(golden, PocketApp.shelfFrom(app.assets)) {
            File(scratch, System.nanoTime().toString()).also { it.mkdirs() }
        }
        scratch.deleteRecursively()
        assertTrue("${report.failures.size} of ${report.calls} calls differ:\n" + report.failures.take(5).joinToString("\n\n"), report.failures.isEmpty())
        assertTrue(report.calls >= 60)
    }
}
