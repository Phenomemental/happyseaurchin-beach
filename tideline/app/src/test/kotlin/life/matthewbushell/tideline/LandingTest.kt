package life.matthewbushell.tideline

import life.matthewbushell.tideline.beach.Beach
import life.matthewbushell.tideline.seal.Gray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/* Stage 6.4 on the phone's side: the sealed line, the queue, the landing, the count note. */
class LandingTest {
    private val dir: File = Files.createTempDirectory("tideline").toFile()
    private val key = ByteArray(32) { (it * 7).toByte() }
    private fun catch(text: String = "Are you in tonight?", app: String = "WhatsApp", time: Long = 1_792_913_465_000) =
        Catch(app, "Alice", null, text, time, "com.whatsapp|447700900123@s.whatsapp.net")

    @AfterTest
    fun clean() {
        dir.deleteRecursively()
    }

    @Test
    fun `the sealed line names its kind and carries the message as caught`() {
        val j = JSONObject(Lines.message(catch(), ZoneId.of("Europe/London")))
        assertEquals("message", j.getString("kind"))
        assertEquals("WhatsApp", j.getString("app"))
        assertEquals("Alice", j.getString("sender"))
        assertTrue(j.isNull("conversation"))
        assertEquals("Are you in tonight?", j.getString("text"))
        assertEquals("2026-10-25T07:31:05Z", j.getString("time"))
        assertEquals("com.whatsapp|447700900123@s.whatsapp.net", j.getString("key"))
        val summer = JSONObject(Lines.message(catch(time = 1_786_536_000_000), ZoneId.of("Europe/London")))
        assertEquals("2026-08-12T13:00+01:00", summer.getString("time"))
    }

    @Test
    fun `a sealed line opens to the same words`() {
        val words = Lines.message(catch("Ça va? 🌊"))
        val envelope = Gray.seal(words, key)
        assertEquals(words, Gray.open(envelope, key))
        assertFalse("Ça va" in envelope.toJson())
    }

    @Test
    fun `a message is queued once, and once landed is never queued again`() {
        val q = Queue(dir)
        val mark = Queue.markOf(catch(), key)
        assertEquals(64, mark.length)
        assertTrue(q.add(mark, "WhatsApp", JSONObject().put("_", "sealed")))
        assertFalse(q.add(mark, "WhatsApp", JSONObject().put("_", "sealed")))
        assertEquals(1, q.size())
        q.landed(q.waiting().single())
        assertEquals(0, q.size())
        assertFalse(Queue(dir).add(mark, "WhatsApp", JSONObject().put("_", "sealed")))
        assertNotEquals(mark, Queue.markOf(catch("Another message"), key))
        assertNotEquals(mark, Queue.markOf(catch(), ByteArray(32)))
    }

    @Test
    fun `nothing of the message is written to the queue`() {
        val q = Queue(dir)
        val c = catch("Meet me at the station at six")
        q.add(Queue.markOf(c, key), c.app, JSONObject(Gray.seal(Lines.message(c), key).toJson()))
        val onDisc = dir.walk().filter { it.isFile }.joinToString("\n") { it.name + "\n" + it.readText() }
        assertFalse("station" in onDisc)
        assertFalse("Alice" in onDisc)
        assertTrue("WhatsApp" in onDisc)
    }

    @Test
    fun `landing takes everything waiting, oldest first`() {
        val q = Queue(dir)
        q.add("a".repeat(64), "WhatsApp", JSONObject().put("_", "first"))
        File(dir, "a".repeat(64) + ".json").setLastModified(1_000)
        q.add("b".repeat(64), "Signal", JSONObject().put("_", "second"))
        val sent = mutableListOf<String>()
        val outcome = Landing(q) { sent += it.getString("_"); Beach.Answer.Landed("1") }.drain()
        assertTrue(outcome is Landing.Outcome.Landed)
        assertEquals(listOf("first", "second"), sent)
        assertEquals(listOf("WhatsApp", "Signal"), outcome.apps)
        assertEquals(0, q.size())
    }

    @Test
    fun `a refusal stops the landing and keeps what is left`() {
        val q = Queue(dir)
        q.add("a".repeat(64), "WhatsApp", JSONObject().put("_", "first"))
        File(dir, "a".repeat(64) + ".json").setLastModified(1_000)
        q.add("b".repeat(64), "Signal", JSONObject().put("_", "second"))
        var n = 0
        val outcome = Landing(q) { if (n++ == 0) Beach.Answer.Landed("1") else Beach.Answer.Refused(403, "lock_required", "secret does not match") }.drain()
        assertTrue(outcome is Landing.Outcome.Refused)
        assertEquals("secret does not match", outcome.reason)
        assertEquals(listOf("WhatsApp"), outcome.apps)
        assertEquals(1, q.size())
    }

    @Test
    fun `no beach keeps everything for later`() {
        val q = Queue(dir)
        q.add("a".repeat(64), "WhatsApp", JSONObject().put("_", "first"))
        val outcome = Landing(q) { Beach.Answer.Unreached("UnknownHostException") }.drain()
        assertTrue(outcome is Landing.Outcome.Unreached)
        assertEquals(1, q.size())
    }

    @Test
    fun `the count note counts by app, with no names and no words`() {
        assertEquals("Tideline: 1 new message (WhatsApp).", RoomNote.text(mapOf("WhatsApp" to 1)))
        assertEquals("Tideline: 2 new messages (Signal).", RoomNote.text(mapOf("Signal" to 2)))
        assertEquals("Tideline: 3 new messages (2 WhatsApp, 1 Signal).", RoomNote.text(mapOf("Signal" to 1, "WhatsApp" to 2)))
        val e = RoomNote.entry(mapOf("Signal" to 1))
        assertEquals("tideline", e.getString("1"))
        assertEquals("", e.getString("2"))
        assertFalse(e.has("3"))
    }

    @Test
    fun `at most one count note every ten minutes, and counts wait for the next`() {
        val note = RoomNote(File(dir, "room-note.json"))
        assertNull(note.dueIn(0))
        note.add(listOf("WhatsApp"))
        assertEquals(0, note.dueIn(1_000_000))
        note.sent(1_000_000)
        assertNull(note.dueIn(1_000_001))
        note.add(listOf("Signal", "Signal"))
        assertEquals(RoomNote.EVERY_MILLIS - 60_000, note.dueIn(1_060_000))
        assertEquals(mapOf("Signal" to 2), RoomNote(File(dir, "room-note.json")).counts())
        assertEquals(0, note.dueIn(1_000_000 + RoomNote.EVERY_MILLIS))
    }
}
