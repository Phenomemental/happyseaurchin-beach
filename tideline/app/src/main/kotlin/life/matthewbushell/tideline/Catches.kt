package life.matthewbushell.tideline

import java.util.concurrent.CopyOnWriteArraySet

/*
 * What the phone has caught since Tideline started, held in memory only. In
 * stage 6.3 nothing is written down and nothing leaves: the text of a message
 * is never put anywhere unsealed (spine:tideline 4.4, 7.18), so the list goes
 * when the app does.
 */
object Catches {
    private const val KEEP = 100
    private const val MARKS = 1000

    private val caught = ArrayDeque<Catch>()
    private val marks = LinkedHashSet<String>()
    private val watchers = CopyOnWriteArraySet<() -> Unit>()

    /** True while Android has the listener connected. */
    @Volatile
    var connected = false
        set(value) {
            field = value
            tell()
        }

    /** Keep a catch, unless the same message was caught already. */
    fun add(catch: Catch): Boolean {
        synchronized(this) {
            if (!marks.add(catch.mark)) return false
            if (marks.size > MARKS) marks.remove(marks.first())
            caught.addFirst(catch)
            if (caught.size > KEEP) caught.removeLast()
        }
        tell()
        return true
    }

    /** Newest first. */
    fun all(): List<Catch> = synchronized(this) { caught.toList() }

    fun watch(watcher: () -> Unit) = watchers.add(watcher)

    fun unwatch(watcher: () -> Unit) = watchers.remove(watcher)

    private fun tell() = watchers.forEach { it() }
}
