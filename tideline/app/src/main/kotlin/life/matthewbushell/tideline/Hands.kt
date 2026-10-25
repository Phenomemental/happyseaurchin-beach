package life.matthewbushell.tideline

import android.content.Context
import life.matthewbushell.tideline.seal.Gray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/*
 * The phone's hands (spine:tideline 1): a catch is sealed the moment it is
 * caught, put in the queue, and the landing worker is told. Without the two
 * keys nothing is sealed or kept, and the catch stays on the screen only.
 */
object Hands {
    private val one = Executors.newSingleThreadExecutor()

    fun take(context: Context, catches: List<Catch>) {
        if (catches.isEmpty()) return
        val app = context.applicationContext
        one.execute {
            // A failure here must not take the listener down with it, and says
            // nothing: the catch stays on the screen, and the next one tries again.
            try {
                val key = Keys.sealKey(app) ?: return@execute
                val queue = queue(app)
                var added = false
                for (c in catches) {
                    val line = JSONObject(Gray.seal(Lines.message(c), key).toJson())
                    if (queue.add(Queue.markOf(c, key), c.app, line)) added = true
                }
                if (added) LandWorker.start(app)
            } catch (e: Exception) {
                // Nothing is logged: whatever went wrong was holding a message.
            }
        }
    }

    fun queue(context: Context) = Queue(File(context.applicationContext.noBackupFilesDir, "queue"))

    fun roomNote(context: Context) = RoomNote(File(context.applicationContext.noBackupFilesDir, "room-note.json"))
}
