package life.matthewbushell.tideline.beach

import com.sun.net.httpserver.HttpServer
import org.json.JSONObject
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * The calls' shape, against a stand-in beach on this machine. Against the
 * live one they were tried on trial-millbrook-a 8 (spine:tideline 7.199).
 */
class BeachTest {
    private lateinit var server: HttpServer
    private lateinit var beach: Beach
    private val posted = mutableListOf<Pair<String, JSONObject>>()
    private var answer: Pair<Int, String> = 200 to """{"ok":true,"slot":"3","address":"1.3","node":"1"}"""

    @BeforeTest
    fun start() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/.well-known/pscale-beach") { x ->
            val query = x.requestURI.rawQuery ?: ""
            val (status, body) = if (x.requestMethod == "POST") {
                posted += query to JSONObject(String(x.requestBody.readBytes(), Charsets.UTF_8))
                answer
            } else if (query.contains("spindle=8.3")) {
                200 to """{"_":"sealed","3":"2026-10-25T06:12:01.309Z"}"""
            } else {
                404 to """{"error":"no such block","code":"not_found"}"""
            }
            val bytes = body.toByteArray()
            x.sendResponseHeaders(status, bytes.size.toLong())
            x.responseBody.use { it.write(bytes) }
        }
        server.start()
        beach = Beach("http://127.0.0.1:${server.address.port}")
    }

    @AfterTest
    fun stop() = server.stop(0)

    @Test
    fun `an append beneath a node carries the node, the entry and the latch`() {
        val entry = JSONObject().put("_", "Encrypted (gray); readable only with the author secret.").put("1", "c").put("2", "n")
        assertEquals(Beach.Answer.Landed("1.3"), beach.append("tideline:Phenomemental", "1", entry, "latch"))
        val (query, body) = posted.single()
        assertEquals("block=tideline%3APhenomemental", query)
        assertTrue(body.getBoolean("append"))
        assertEquals("1", body.getString("spindle"))
        assertEquals("latch", body.getString("secret"))
        assertEquals("c", body.getJSONObject("content").getString("1"))
    }

    @Test
    fun `a root append has no spindle, and no secret unless one is given`() {
        answer = 200 to """{"ok":true,"slot":"14"}"""
        assertEquals(Beach.Answer.Landed("14"), beach.appendAtRoot("pool:Phenomemental", JSONObject().put("_", "2 messages"), null))
        val body = posted.single().second
        assertFalse(body.has("spindle"))
        assertFalse(body.has("secret"))
    }

    @Test
    fun `a full node is refused, and says so`() {
        answer = 409 to """{"error":"\"8\" of \"trial-millbrook-a\" is full","code":"node_full"}"""
        val a = beach.append("trial-millbrook-a", "8", JSONObject().put("_", "x"), "latch") as Beach.Answer.Refused
        assertEquals(409, a.status)
        assertEquals("node_full", a.code)
    }

    @Test
    fun `a wrong latch is refused, and a busy beach is only unreached`() {
        answer = 403 to """{"error":"secret does not match","code":"lock_required"}"""
        assertEquals("lock_required", (beach.append("b", "1", JSONObject(), "wrong") as Beach.Answer.Refused).code)
        answer = 429 to """{"error":"taken its fill","code":"cap_appends"}"""
        assertTrue(beach.append("b", "1", JSONObject(), null) is Beach.Answer.Unreached)
        answer = 503 to """{"error":"append contention — retry","code":"append_contention"}"""
        assertTrue(beach.append("b", "1", JSONObject(), null) is Beach.Answer.Unreached)
    }

    @Test
    fun `no beach at all is only unreached`() {
        stop()
        assertTrue(beach.append("b", "1", JSONObject(), "latch") is Beach.Answer.Unreached)
        start()
    }

    @Test
    fun `a read returns the JSON there, or null when there is none`() {
        assertEquals("2026-10-25T06:12:01.309Z", JSONObject(beach.read("trial-millbrook-a", "8.3")!!).getString("3"))
        assertNull(beach.read("trial-millbrook-a", "8.4"))
    }
}
