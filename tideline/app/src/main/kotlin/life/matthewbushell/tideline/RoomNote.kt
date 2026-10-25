package life.matthewbushell.tideline

import org.json.JSONObject
import java.io.File

/*
 * The count note in Matthew's room (spine:tideline 2.1, 3.5): one line for
 * what has landed, at most one every ten minutes, counts by app only. No
 * names and no words: they are other people's. Counts landed inside the ten
 * minutes wait here and go with the next note.
 */
class RoomNote(private val file: File) {
    companion object {
        const val EVERY_MILLIS = 10 * 60 * 1000L

        /** "Tideline: 3 new messages (2 WhatsApp, 1 Signal)."; from one app alone, "Tideline: 2 new messages (Signal)." */
        fun text(counts: Map<String, Int>): String {
            val total = counts.values.sum()
            val parts = counts.keys.singleOrNull()
                ?: counts.entries.sortedWith(compareBy({ -it.value }, { it.key })).joinToString(", ") { "${it.value} ${it.key}" }
            return "Tideline: $total new message${if (total == 1) "" else "s"} ($parts)."
        }

        /** The note as a room takes a contribution: {_, 1: who, 2: where (none), 4: face}; the beach dates it at 3. */
        fun entry(counts: Map<String, Int>): JSONObject =
            JSONObject().put("_", text(counts)).put("1", Places.VOICE).put("2", "").put("4", "observer")
    }

    @Synchronized
    fun add(apps: List<String>) {
        if (apps.isEmpty()) return
        val counts = counts().toMutableMap()
        apps.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        save(counts, last())
    }

    @Synchronized
    fun counts(): Map<String, Int> {
        val j = read().optJSONObject("counts") ?: return emptyMap()
        return j.keys().asSequence().associateWith { j.getInt(it) }
    }

    /** When the last note went, in milliseconds; 0 for never. */
    @Synchronized
    fun last(): Long = read().optLong("last", 0)

    /** How long until a note may go: 0 when one may go now, or null when there is nothing to say. */
    @Synchronized
    fun dueIn(now: Long): Long? {
        if (counts().isEmpty()) return null
        return (last() + EVERY_MILLIS - now).coerceAtLeast(0)
    }

    /** The note went: start counting afresh. */
    @Synchronized
    fun sent(now: Long) = save(emptyMap(), now)

    private fun read(): JSONObject = try {
        if (file.exists()) JSONObject(file.readText()) else JSONObject()
    } catch (e: Exception) {
        JSONObject()
    }

    private fun save(counts: Map<String, Int>, last: Long) {
        file.parentFile?.mkdirs()
        val part = File(file.path + ".part")
        part.writeText(JSONObject().put("counts", JSONObject(counts)).put("last", last).toString())
        part.renameTo(file)
    }
}
