package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The golden replay on the JVM — every call of conformance/golden/golden.json
 * must match the reference, with the blocks in files and again with them in
 * memory: the core answers the same whatever storage stands beneath it.
 */
class GoldenTest {
    private val golden = File(System.getProperty("pocket.golden") ?: "../conformance/golden/golden.json")
    private val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.shelf") ?: "../shelf")))

    private fun check(label: String, report: Conformance.Report) {
        println("golden ($label): ${report.cases} cases, ${report.calls} calls, ${report.failures.size} mismatches")
        if (report.failures.isNotEmpty()) {
            fail("$label: ${report.failures.size} of ${report.calls} calls differ from the reference:\n\n" + report.failures.take(12).joinToString("\n\n"))
        }
        assertTrue(report.calls >= 60, "the corpus must hold at least 60 calls")
    }

    @Test
    fun everyCallMatchesTheReferenceInFiles() {
        assertTrue(golden.isFile, "no golden file at $golden — run conformance/generate.sh")
        val roots = HashMap<BlockStorage, File>()
        val report = Conformance.replay(
            golden.readText(Charsets.UTF_8), shelf,
            newStorage = { Files.createTempDirectory("pocket-golden").toFile().let { root -> FileStorage(root).also { roots[it] = root } } },
            release = { roots.remove(it)?.deleteRecursively() },
        )
        check("files", report)
    }

    @Test
    fun everyCallMatchesTheReferenceInMemory() {
        check("memory", Conformance.replay(golden.readText(Charsets.UTF_8), shelf, newStorage = { MemoryStorage() }))
    }
}
