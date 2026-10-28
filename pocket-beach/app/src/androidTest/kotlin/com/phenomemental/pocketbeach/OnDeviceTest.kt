package com.phenomemental.pocketbeach

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phenomemental.pocketbeach.bsp.BlockStorage
import com.phenomemental.pocketbeach.bsp.Conformance
import com.phenomemental.pocketbeach.bsp.Copies
import com.phenomemental.pocketbeach.bsp.FileStorage
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.Shelf
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
    fun aCopySavedThroughTheFilePickersPathIsRestoredWhole() {
        Fresh.open().close()
        app.pocket.call("""{"agent_id":"pocket","block":"kept","spindle":"1","content":"in the copy"}""")
        val file = File(app.cacheDir, "copy-test.json")
        app.contentResolver.openOutputStream(Uri.fromFile(file), "wt")!!.use { it.write(app.pages.saveCopy().toByteArray(Charsets.UTF_8)) }
        app.pocket.call("""{"agent_id":"pocket","block":"made-later","spindle":"1","content":"not in the copy"}""")

        val copy = Copies.read(app.contentResolver.openInputStream(Uri.fromFile(file))!!.use { it.readBytes().toString(Charsets.UTF_8) })
        app.pages.restore(copy)
        file.delete()
        assertEquals(listOf("kept"), app.pages.shore().mine.map { it.name })
        assertTrue(app.pocket.call("""{"agent_id":"pocket","block":"kept","spindle":"1"}""").contains("in the copy"))
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
        val roots = HashMap<BlockStorage, File>()
        val report = Conformance.replay(
            golden, Shelf(AssetShelfSource(app.assets)),
            newStorage = { File(scratch, System.nanoTime().toString()).let { root -> FileStorage(root).also { roots[it] = root } } },
            release = { roots.remove(it)?.deleteRecursively() },
        )
        scratch.deleteRecursively()
        assertTrue("${report.failures.size} of ${report.calls} calls differ:\n" + report.failures.take(5).joinToString("\n\n"), report.failures.isEmpty())
        assertTrue(report.calls >= 60)
    }
}
