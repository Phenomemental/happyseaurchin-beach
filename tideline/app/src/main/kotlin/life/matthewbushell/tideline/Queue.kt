package life.matthewbushell.tideline

import org.json.JSONObject
import java.io.File
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/*
 * The catches waiting on the phone until the beach takes them (spine:tideline
 * 7.12 b), so nothing is lost offline and nothing is sent twice. Only sealed
 * lines are written here: each file holds a line's envelope and the name of
 * its app, for the room's count, and nothing else. A message's mark (its
 * conversation, text and time) never reaches the disc as itself: files are
 * named by a keyed hash of it, made with the sealing key, so even the name
 * of a file says nothing without that key.
 */
class Queue(private val dir: File) {
    private val landed = File(dir, "landed")

    init {
        dir.mkdirs()
    }

    /** One waiting line: its mark's hash, the app it came from, and the envelope. */
    class Waiting(val file: File, val mark: String, val app: String, val line: JSONObject)

    /** Put a sealed line in the queue. False when that message is waiting or has landed already. */
    @Synchronized
    fun add(mark: String, app: String, line: JSONObject): Boolean {
        val file = File(dir, "$mark.json")
        if (file.exists() || mark in landedMarks()) return false
        val part = File(dir, "$mark.part")
        part.writeText(JSONObject().put("app", app).put("line", line).toString())
        return part.renameTo(file)
    }

    /** Oldest first. */
    @Synchronized
    fun waiting(): List<Waiting> = (dir.listFiles { f -> f.name.endsWith(".json") } ?: emptyArray())
        .sortedWith(compareBy({ it.lastModified() }, { it.name }))
        .mapNotNull { f ->
            try {
                val j = JSONObject(f.readText())
                Waiting(f, f.name.removeSuffix(".json"), j.getString("app"), j.getJSONObject("line"))
            } catch (e: Exception) {
                null
            }
        }

    @Synchronized
    fun size(): Int = dir.listFiles { f -> f.name.endsWith(".json") }?.size ?: 0

    /** The beach has it: forget the line, remember its mark. */
    @Synchronized
    fun landed(w: Waiting) {
        val marks = (landedMarks() + w.mark).takeLast(KEEP_MARKS)
        landed.writeText(marks.joinToString("\n"))
        w.file.delete()
    }

    private fun landedMarks(): List<String> =
        if (landed.exists()) landed.readLines().filter { it.isNotBlank() } else emptyList()

    companion object {
        private const val KEEP_MARKS = 2000

        /** A message's mark as the queue knows it: HMAC-SHA256 under the sealing key. */
        fun markOf(catch: Catch, sealKey: ByteArray): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(sealKey, "HmacSHA256"))
            return mac.doFinal(catch.mark.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
    }
}
