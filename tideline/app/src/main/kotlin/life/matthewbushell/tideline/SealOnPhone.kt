package life.matthewbushell.tideline

import android.os.SystemClock
import life.matthewbushell.tideline.seal.Gray
import java.util.Base64

/*
 * The seal checked on the phone itself, once a run, on a test word that opens
 * nothing: the key and a sealed line must come out exactly as bsp-mcp's
 * keys.ts makes them (the values the seal module's tests hold), and a line
 * must seal and open again. It also times the key, which the plan wants
 * measured on the Pixel (spine:tideline 7.19, "the key is slow on purpose").
 */
object SealOnPhone {
    class Result(val passed: Boolean, val keyMillis: Long)

    private const val WORD = "tideline-test-word"
    private const val KEY = "ee350d9a172a77e4ffd9bf3d0b8f1a65dd6f06befeb5ec3188bd99b91a3e7f9e"
    private const val BOX = "wvHU4214aSWGSNQ2NWYknWMBH5scKpQt3npZh/BhwBZ+SYMh"

    @Volatile
    var result: Result? = null
        private set

    /** Slow (it makes a key): call off the main thread. */
    fun run(): Result {
        result?.let { return it }
        val started = SystemClock.elapsedRealtime()
        val key = Gray.key(WORD)
        val millis = SystemClock.elapsedRealtime() - started
        val nonce = ByteArray(Gray.NONCE_BYTES) { it.toByte() }
        val box = Base64.getEncoder().encodeToString(Gray.secretbox("A line for the seal.".toByteArray(), nonce, key))
        val line = "Ça va? 🌊 a line sealed and opened on the phone"
        val passed = key.joinToString("") { "%02x".format(it) } == KEY &&
            box == BOX &&
            Gray.open(Gray.seal(line, key), key) == line
        return Result(passed, millis).also { result = it }
    }
}
