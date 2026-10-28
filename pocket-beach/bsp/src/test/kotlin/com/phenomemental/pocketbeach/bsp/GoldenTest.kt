package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/** The golden replay on the JVM — every call of conformance/golden/golden.json must match the reference. */
class GoldenTest {
    private val golden = File(System.getProperty("pocket.golden") ?: "../conformance/golden/golden.json")
    private val shelfDir = File(System.getProperty("pocket.shelf") ?: "../shelf")

    @Test
    fun everyCallMatchesTheReference() {
        assertTrue(golden.isFile, "no golden file at $golden — run conformance/generate.sh")
        val shelf = Shelf(File(shelfDir, "index.json").readText()) { f -> File(shelfDir, f).takeIf { it.isFile }?.readText() }
        val report = Conformance.replay(golden.readText(), shelf) { Files.createTempDirectory("pocket-golden").toFile() }
        println("golden: ${report.cases} cases, ${report.calls} calls, ${report.failures.size} mismatches")
        if (report.failures.isNotEmpty()) {
            fail("${report.failures.size} of ${report.calls} calls differ from the reference:\n\n" + report.failures.take(12).joinToString("\n\n"))
        }
        assertTrue(report.calls >= 60, "the corpus must hold at least 60 calls")
    }
}
