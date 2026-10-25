package life.matthewbushell.tideline

import life.matthewbushell.tideline.beach.Beach
import life.matthewbushell.tideline.seal.Gray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/*
 * The phone's whole path for one message, from catch to sealed line to queue
 * to landing, against a trial block on the live beach and never the catch.
 * It runs only when a trial's words are in the environment, so a build
 * elsewhere skips it:
 *
 *   TIDELINE_READING_KEY=<trial word> TIDELINE_TRIAL_LATCH=<trial latch> TIDELINE_TRIAL_BLOCK=trial-tideline \
 *     ./gradlew :app:testDebugUnitTest --tests '*LiveLandingTest*'
 */
class LiveLandingTest {
    @Test
    fun `a caught message lands sealed at a trial root`() {
        val word = System.getenv("TIDELINE_READING_KEY")
        val latch = System.getenv("TIDELINE_TRIAL_LATCH")
        val block = System.getenv("TIDELINE_TRIAL_BLOCK")
        assumeTrue(!word.isNullOrEmpty() && !latch.isNullOrEmpty() && !block.isNullOrEmpty())

        val caught = Catch(
            "WhatsApp", "A test sender", "Tideline trial",
            "Landing check from the phone's own path, 25 October 2026 — ça va 🌊",
            System.currentTimeMillis(), "com.whatsapp|trial",
        )
        val key = Gray.key(word)
        val queue = Queue(Files.createTempDirectory("tideline-live").toFile())
        queue.add(Queue.markOf(caught, key), caught.app, JSONObject(Gray.seal(Lines.message(caught), key).toJson()))
        val beach = Beach()
        val outcome = Landing(queue) { beach.appendAtRoot(block, it, latch) }.drain()
        assertTrue(outcome is Landing.Outcome.Landed, outcome.toString())
        assertTrue(queue.size() == 0)
        println("landed at the root of $block: ${outcome.apps}")
    }
}
