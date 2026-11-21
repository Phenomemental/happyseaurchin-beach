package com.phenomemental.pocketbeach

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phenomemental.pocketbeach.bsp.Copies
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.folderFileNameOf
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sundial 0.11 on the phone (spine:pocket-beach 1.111178719932 to
 * 1.111178719943, 1.111178719942): the move from Settings, refused on a
 * clash; one plain page without the permission; an outside change noticed
 * and a write over it held; a file that isn't a block listed and left; a
 * copy restored on the folder. In the tests' own folder, Documents/
 * Sundial-test, never his.
 */
@RunWith(AndroidJUnit4::class)
class FolderDeviceTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = Fresh.app
    private val h = Hatch.MATTHEW_HANDLE
    private val folder get() = Fresh.testFolder

    /** All-files access, given as Android's settings give it. */
    @Before
    fun access() {
        shell("appops set --uid ${app.packageName} MANAGE_EXTERNAL_STORAGE allow")
        check(android.os.Environment.isExternalStorageManager()) { "the emulator gave Sundial all-files access" }
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use { it.readBytes().toString(Charsets.UTF_8) }

    /** A hatched pocket with a garden block of his. */
    private fun hatchedWithGarden() {
        Fresh.hatched()
        check(app.pages.carryOut(app.pages.planNewBlock("garden")).ok)
        check(app.pages.carryOut(app.pages.planAdd("garden", "", "plant the bulbs")).ok)
    }

    private fun none(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isEmpty()

    private fun moved() {
        val m = app.move()
        check(m.ok) { m.problem!! }
    }

    @Test
    fun theMoveFromSettingsCopiesChecksAndSwitches() {
        hatchedWithGarden()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-settings")
            compose.waitFor("kept-inside")
            compose.tap("keep-in-folder")
            compose.waitFor("move-what", timeoutMs = 15_000)
            val names = app.storage.names().sorted()
            val texts = names.associateWith { app.storage.read(it) }
            compose.tap("move-them")
            compose.waitFor("move-said", "Moved", timeoutMs = 20_000)
            assertTrue(app.inFolder)
            assertEquals(names, app.storage.names().sorted())
            for (n in names) assertEquals(n, texts[n], app.storage.read(n))
            val files = folder.list()!!.toSet()
            assertTrue(files.toString(), "garden.json" in files && folderFileNameOf(Hatch.passportOf(h)) in files)
            assertFalse("his shell stays inside the app", folderFileNameOf(Hatch.shellOf(h)) in files)
            assertFalse("his history stays inside the app", folderFileNameOf(Hatch.historyOf(h)) in files)
            assertFalse("her safety stays inside the app", folderFileNameOf(Talking.SAFETY_BLOCK) in files)
            assertNotNull(app.privateRegion.read(Hatch.shellOf(h)))
            assertTrue("the copy inside the app stays", app.privateCopyStands)
            compose.waitFor("kept-in-folder")
            compose.tap("remove-private-copy")
            compose.tap("remove-private-copy-yes")
            compose.waitUntil(10_000) { !app.privateCopyStands }
            assertEquals(texts["garden"], app.storage.read("garden"))
        }
    }

    @Test
    fun aNameTakenInTheFolderStopsTheMoveAndIsListed() {
        hatchedWithGarden()
        folder.mkdirs()
        File(folder, "Garden.json").writeText("""{"_":"Garden, on the laptop"}""")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-settings")
            compose.tap("keep-in-folder")
            compose.waitFor("move-refusal", "Garden.json (for garden)", timeoutMs = 15_000)
            assertFalse(app.inFolder)
            assertEquals(setOf("Garden.json"), folder.list()!!.toSet())
        }
    }

    @Test
    fun withoutTheAccessOnePlainPageAndNoWrites() {
        hatchedWithGarden()
        moved()
        val garden = File(folder, "garden.json").readText()
        app.accessForTests = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitFor("barred")
            assertTrue("never an empty pocket: nothing of the app shows", none("menu") && none("question") && none("block:garden"))
            assertFalse(app.pages.carryOut(app.pages.planAdd("garden", "", "written while barred")).ok)
            assertEquals(garden, File(folder, "garden.json").readText())
            // Given back, Sundial opens as ever on its return.
            app.accessForTests = true
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitFor("menu", timeoutMs = 15_000)
            assertTrue(none("barred"))
        }
    }

    @Test
    fun aChangeMadeOutsideIsNoticedAndHisWriteHeldThenPlannedAgain() {
        hatchedWithGarden()
        moved()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-blocks")
            compose.tap("block:garden")
            compose.tap("line:1")
            compose.tap("change")
            compose.waitFor("line-text")
            compose.onNodeWithTag("line-text").performTextReplacement("plant the tulips")
            // The laptop's edit arrives by the sync: the same line, other words.
            val block = parseJson(File(folder, "garden.json").readText()) as JsObj
            block["1"] = str("plant the bulbs, done on the laptop")
            File(folder, "garden.json").writeText(stringify(block)!!)
            compose.waitFor("held", timeoutMs = 15_000)
            compose.tap("held-again")
            compose.waitUntil(10_000) { none("held") }
            compose.waitFor("line-text")
            compose.tap("save")
            compose.waitFor("standing", "plant the tulips", timeoutMs = 10_000)
            assertTrue(File(folder, "garden.json").readText().contains("plant the tulips"))
        }
    }

    @Test
    fun aSaveOverAChangeMadeOutsideIsHeldAndLetGo() {
        hatchedWithGarden()
        moved()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-blocks")
            compose.tap("block:garden")
            compose.tap("line:1")
            compose.tap("change")
            compose.waitFor("line-text")
            compose.onNodeWithTag("line-text").performTextReplacement("plant the tulips")
            val outside = """{"_":"garden at pocket.","1":"plant the bulbs, done on the laptop"}"""
            File(folder, "garden.json").writeText(outside)
            compose.tap("save")
            compose.waitFor("held", timeoutMs = 15_000)
            assertEquals("nothing was written over it", outside, File(folder, "garden.json").readText())
            compose.tap("held-let-go")
            compose.waitFor("standing", "plant the bulbs, done on the laptop", timeoutMs = 10_000)
        }
    }

    @Test
    fun aFileThatIsntABlockIsListedAndLeftAsItIs() {
        hatchedWithGarden()
        moved()
        File(folder, "broken.json").writeText("{\"_\": \"half writ")
        File(folder, "notes.md").writeText("# mine")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-blocks")
            compose.waitFor("set-aside:broken.json", "isn't readable JSON")
            compose.waitFor("block:garden")
            assertEquals("{\"_\": \"half writ", File(folder, "broken.json").readText())
            assertEquals("# mine", File(folder, "notes.md").readText())
        }
    }

    /** "Restoring a copy is tried once on the new folder before release" (spine:pocket-beach 1.111178719933). */
    @Test
    fun aCopyRestoredOnTheFolderPutsEveryBlockBack() {
        hatchedWithGarden()
        moved()
        File(folder, "notes.md").writeText("# mine")
        val names = app.storage.names().sorted()
        val texts = names.associateWith { app.storage.read(it) }
        val copy = Copies.read(app.pages.saveCopy())
        check(app.pages.carryOut(app.pages.planAdd("garden", "", "written after the copy")).ok)
        check(app.pages.carryOut(app.pages.planNewBlock("extra")).ok)
        app.pages.restore(copy)
        assertEquals(names, app.storage.names().sorted())
        for (n in names) assertEquals(n, texts[n], app.storage.read(n))
        assertNull(app.storage.read("extra"))
        assertFalse(File(folder, "extra.json").exists())
        assertEquals("# mine", File(folder, "notes.md").readText())
    }

    @Test
    fun yourPocketOffersTheFolderOnce() {
        hatchedWithGarden()
        app.folderOffered = false
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.menu("menu-blocks")
            compose.waitFor("folder-offer")
            compose.tap("folder-offer-not-now")
            compose.waitUntil(5_000) { none("folder-offer") }
        }
        assertTrue(app.folderOffered)
    }
}
