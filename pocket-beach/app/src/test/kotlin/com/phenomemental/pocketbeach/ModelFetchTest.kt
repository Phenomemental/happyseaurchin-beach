package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.net.ModelFetch
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The model's downloader against a pretend Hugging Face: redirects, resuming, breaks, refusals. */
class ModelFetchTest {
    private val file = Random(7).nextBytes(5000)
    private val start = "https://huggingface.co/m/resolve/r/model.litertlm?download=true"
    private val cdn = "https://us.aws.cdn.hf.co/xet-bridge-us/abc?sig=1"
    private val hosts = setOf("huggingface.co", "us.aws.cdn.hf.co")
    private val part = File(Files.createTempDirectory("fetch").toFile(), "model.part")

    /** What each request asked: the address and where it asked to begin. */
    private val asked = mutableListOf<Pair<String, Long>>()

    /** A pretend Hugging Face: the resolve address redirects to the content server, which serves [file] honouring ranges. */
    private fun hf(
        cdnStatus: (from: Long) -> Int = { if (it > 0) 206 else 200 },
        body: (from: Long) -> InputStream = { ByteArrayInputStream(file, it.toInt(), file.size - it.toInt()) },
        redirect: String = cdn,
    ) = ModelFetch.Opener { url, from ->
        asked += url to from
        when {
            url.startsWith("https://huggingface.co/") -> ModelFetch.Hop(302, mapOf("location" to redirect), null) {}
            url.startsWith("https://us.aws.cdn.hf.co/") -> {
                val status = cdnStatus(from)
                val headers = when (status) {
                    206 -> mapOf("content-range" to "bytes $from-${file.size - 1}/${file.size}", "content-length" to "${file.size - from}")
                    200 -> mapOf("content-length" to "${file.size}")
                    else -> emptyMap()
                }
                val content = when (status) {
                    200 -> body(0)
                    206 -> body(from)
                    else -> ByteArrayInputStream("Not found here".toByteArray())
                }
                ModelFetch.Hop(status, headers, content) {}
            }
            else -> error("nothing at $url")
        }
    }

    private fun fetch(open: ModelFetch.Opener, connection: () -> String? = { null }, stop: () -> Boolean = { false }, notes: MutableList<String> = mutableListOf()) =
        ModelFetch(start, hosts, file.size.toLong(), open, connection, pause = {}).run(part, { p -> p.note?.let(notes::add) }, stop)

    @Test
    fun itFollowsTheRedirectAndFetchesTheWholeFile() {
        assertEquals(ModelFetch.Outcome.Complete, fetch(hf()))
        assertArrayEquals(file, part.readBytes())
        assertEquals(listOf(start to 0L, cdn to 0L), asked)
    }

    @Test
    fun itCarriesOnFromWhatIsKept() {
        part.writeBytes(file.copyOf(1234))
        assertEquals(ModelFetch.Outcome.Complete, fetch(hf()))
        assertArrayEquals(file, part.readBytes())
        assertEquals("the range is asked of every hop", listOf(start to 1234L, cdn to 1234L), asked)
    }

    @Test
    fun aServerThatIgnoresTheRangeIsTakenFromTheStart() {
        part.writeBytes(ByteArray(1234) { 9 })
        assertEquals(ModelFetch.Outcome.Complete, fetch(hf(cdnStatus = { 200 })))
        assertArrayEquals(file, part.readBytes())
    }

    @Test
    fun aBrokenConnectionCarriesOnWhereItBroke() {
        var first = true
        val breaking = hf(body = { from ->
            val rest = ByteArrayInputStream(file, from.toInt(), file.size - from.toInt())
            if (!first) rest else object : InputStream() {
                var sent = 0
                override fun read(): Int = throw UnsupportedOperationException()
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    if (sent >= 3000) { first = false; throw IOException("Connection reset") }
                    val n = rest.read(b, off, minOf(len, 3000 - sent))
                    sent += n
                    return n
                }
            }
        })
        val notes = mutableListOf<String>()
        assertEquals(ModelFetch.Outcome.Complete, fetch(breaking, notes = notes))
        assertArrayEquals(file, part.readBytes())
        assertEquals(3000L, asked.last().second)
        assertTrue(notes.single(), notes.single().startsWith("The connection broke (Connection reset). Trying again in 2 s"))
    }

    @Test
    fun anUntrustedHostStopsItAndIsNamed() {
        val out = fetch(hf(redirect = "https://elsewhere.example.com/model"))
        val failed = out as ModelFetch.Outcome.Failed
        assertEquals("Hugging Face sent the download on to elsewhere.example.com, a host the pocket doesn't trust, so it stopped there.", failed.why)
        assertEquals("nothing was asked of the untrusted host", listOf(start to 0L), asked)
        val plain = fetch(hf(redirect = "http://us.aws.cdn.hf.co/model")) as ModelFetch.Outcome.Failed
        assertTrue(plain.why.contains("isn't HTTPS"))
    }

    @Test
    fun anHttpErrorIsSaidAsTheServerSaidIt() {
        val failed = fetch(hf(cdnStatus = { 404 })) as ModelFetch.Outcome.Failed
        assertEquals("us.aws.cdn.hf.co answered HTTP 404: Not found here", failed.why)
        val busy = fetch(hf(cdnStatus = { 503 })) as ModelFetch.Outcome.Failed
        assertTrue(busy.why, busy.why.startsWith("The connection broke ${ModelFetch.MAX_TRIES} times without getting further. The last time: us.aws.cdn.hf.co answered HTTP 503"))
    }

    @Test
    fun aWrongSizeIsRefused() {
        val wrong = ModelFetch.Opener { url, _ ->
            if (url == start) ModelFetch.Hop(302, mapOf("location" to cdn), null) {}
            else ModelFetch.Hop(200, mapOf("content-length" to "99"), ByteArrayInputStream(ByteArray(99))) {}
        }
        val failed = fetch(wrong) as ModelFetch.Outcome.Failed
        assertEquals("us.aws.cdn.hf.co says the file is 99 bytes, not the ${file.size} the pocket expects.", failed.why)
    }

    @Test
    fun stoppingKeepsWhatHasCome() {
        var reads = 0
        val out = ModelFetch(start, hosts, file.size.toLong(), hf(body = { from ->
            object : InputStream() {
                val rest = ByteArrayInputStream(file, from.toInt(), file.size - from.toInt())
                override fun read(): Int = throw UnsupportedOperationException()
                override fun read(b: ByteArray, off: Int, len: Int): Int { reads++; return rest.read(b, off, minOf(len, 1000)) }
            }
        }), pause = {}).run(part, {}, { reads >= 2 })
        assertTrue(out is ModelFetch.Outcome.Stopped)
        assertEquals(2000L, part.length())
        assertArrayEquals(file.copyOf(2000), part.readBytes())
    }

    @Test
    fun aMeteredConnectionIsNotUsed() {
        val failed = fetch(hf(), connection = { "The phone is on a metered connection." }) as ModelFetch.Outcome.Failed
        assertEquals("The phone is on a metered connection.", failed.why)
        assertTrue(asked.isEmpty())
    }
}
