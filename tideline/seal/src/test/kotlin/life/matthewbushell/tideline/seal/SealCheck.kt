package life.matthewbushell.tideline.seal

import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

/*
 * The seal checked against the live beach (stage 6.2), by hand, on a trial
 * block and never the real catch:
 *
 *   echo '<a test line>' | TIDELINE_READING_KEY=<a trial word> ./gradlew -q :seal:sealCheck --args=seal
 *     prints the envelope, to be written to the trial block and read back
 *     through the Pscale tool with the same word as enc_secret.
 *   TIDELINE_READING_KEY=<a trial word> ./gradlew -q :seal:sealCheck --args="open <field 1> <field 2>"
 *     opens an envelope the tool sealed with that word.
 *
 * The word comes from the environment only, so it stays out of the shell's
 * history and the process list. The line comes in on stdin and everything
 * goes out as UTF-8 bytes, because a JVM under a POSIX locale reads its
 * arguments as ASCII and would seal '?' for every letter beyond it.
 */
fun main(args: Array<String>) {
    val word = System.getenv("TIDELINE_READING_KEY")
    if (word.isNullOrEmpty()) fail("set TIDELINE_READING_KEY to the trial's reading key")
    val salt = System.getenv("TIDELINE_SALT") ?: Gray.BEACH
    val out = PrintStream(FileOutputStream(FileDescriptor.out), true, "UTF-8")
    when (args.firstOrNull()) {
        "seal" -> {
            val line = String(System.`in`.readBytes(), Charsets.UTF_8).trimEnd('\n')
            if (line.isEmpty()) fail("give the test line on stdin")
            out.println(Gray.seal(line, Gray.key(word, salt)).toJson())
        }
        "open" -> {
            if (args.size != 3) fail("open <field 1> <field 2>")
            out.println(Gray.open(Gray.Envelope(args[1], args[2]), Gray.key(word, salt)) ?: "does not open")
        }
        else -> fail("seal (line on stdin) | open <field 1> <field 2>")
    }
}

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}
