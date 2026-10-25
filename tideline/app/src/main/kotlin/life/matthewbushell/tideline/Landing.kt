package life.matthewbushell.tideline

import life.matthewbushell.tideline.beach.Beach
import org.json.JSONObject

/*
 * Lands what is waiting, oldest first, at the root of the catch (spine:tideline
 * 4.2, 7.14). Stops at the first line the beach does not take: unreached, it
 * is tried again later; refused, it waits on the phone and the screen says
 * why, since trying again would only be refused again.
 */
class Landing(private val queue: Queue, private val append: (JSONObject) -> Beach.Answer) {
    /** How a drain ended; [apps] names the app of each line that landed on the way, for the room's count. */
    sealed class Outcome(val apps: List<String>) {
        /** Everything waiting has landed. */
        class Landed(apps: List<String>) : Outcome(apps)
        class Unreached(apps: List<String>, val reason: String) : Outcome(apps)
        class Refused(apps: List<String>, val reason: String) : Outcome(apps)
    }

    fun drain(): Outcome {
        val apps = mutableListOf<String>()
        for (w in queue.waiting()) {
            when (val a = append(w.line)) {
                is Beach.Answer.Landed -> {
                    queue.landed(w)
                    apps += w.app
                }
                is Beach.Answer.Unreached -> return Outcome.Unreached(apps, a.reason)
                is Beach.Answer.Refused -> return Outcome.Refused(apps, a.reason)
            }
        }
        return Outcome.Landed(apps)
    }
}
