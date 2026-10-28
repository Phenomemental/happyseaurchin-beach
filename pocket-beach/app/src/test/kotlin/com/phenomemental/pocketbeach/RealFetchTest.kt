package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.model.ModelStore
import com.phenomemental.pocketbeach.net.ModelFetch
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * The real download, run only when asked (POCKET_REAL_FETCH=1), never in CI:
 * the pinned model from Hugging Face through its real redirect, stopped part
 * way, carried on with a range request, and checked whole against its SHA-256.
 */
class RealFetchTest {
    @Test
    fun theRealModelComesWholeAcrossAStop() {
        assumeTrue(System.getenv("POCKET_REAL_FETCH") == "1")
        val part = File(Files.createTempDirectory("realfetch").toFile(), "model.part")
        try {
            val notes = mutableListOf<String>()
            val fetch = ModelFetch(ModelStore.URL, ModelStore.HOSTS, ModelStore.SIZE)
            val first = fetch.run(part, { p -> p.note?.let(notes::add) }, { part.length() >= 300_000_000L })
            println("first pass: $first at ${part.length()} bytes; notes: $notes")
            assertEquals(ModelFetch.Outcome.Stopped::class, first::class)
            val second = fetch.run(part, { p -> p.note?.let(notes::add) }, { false })
            println("second pass: $second at ${part.length()} bytes; notes: $notes")
            assertEquals(ModelFetch.Outcome.Complete, second)
            val digest = MessageDigest.getInstance("SHA-256")
            part.inputStream().buffered(1 shl 20).use { input ->
                val buffer = ByteArray(1 shl 20)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            assertEquals(ModelStore.SHA256, digest.digest().joinToString("") { "%02x".format(it) })
        } finally {
            part.parentFile?.deleteRecursively()
        }
    }
}
