package life.matthewbushell.tideline.beach

import life.matthewbushell.tideline.seal.Gray
import org.json.JSONObject
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

/*
 * The app's beach calls tried against a trial block, by hand, never the real
 * catch (spine:tideline 6.4):
 *
 *   TIDELINE_READING_KEY=<trial word> TIDELINE_TRIAL_LATCH=<trial latch> \
 *     ./gradlew -q :beach:beachCheck --args="<block> <node> <how many>"
 *
 * seals that many numbered test lines with the app's seal and appends each
 * beneath <node> with the app's own call, printing what the beach answered.
 * A <node> of "root" appends at the block's root, as the catch takes its
 * lines. Both words come from the environment, never the command line.
 */
fun main(args: Array<String>) {
    val out = PrintStream(FileOutputStream(FileDescriptor.out), true, "UTF-8")
    val word = System.getenv("TIDELINE_READING_KEY")
    val latch = System.getenv("TIDELINE_TRIAL_LATCH")
    if (word.isNullOrEmpty() || latch.isNullOrEmpty()) fail("set TIDELINE_READING_KEY and TIDELINE_TRIAL_LATCH")
    if (args.size != 3) fail("<block> <node> <how many>")
    val (block, node, count) = Triple(args[0], args[1], args[2].toInt())
    val key = Gray.key(word)
    val beach = Beach()
    for (n in 1..count) {
        val line = "Tideline landing check $n, 25 October 2026: sealed and landed by the app’s own calls — ça va 🌊"
        val entry = JSONObject(Gray.seal(line, key).toJson())
        val answer = if (node == "root") beach.appendAtRoot(block, entry, latch) else beach.append(block, node, entry, latch)
        out.println("$n: $answer")
    }
}

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}
