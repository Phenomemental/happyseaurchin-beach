package com.phenomemental.pocketbeach.model

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import com.phenomemental.pocketbeach.net.ModelFetch
import com.phenomemental.pocketbeach.net.Unmetered
import java.io.File
import java.security.MessageDigest

/**
 * The companion's model on this phone: Gemma 4 E2B as LiteRT-LM packs it,
 * fetched once, from one pinned file on Hugging Face, by the app's own
 * downloader ([ModelFetch], run by [FetchService] while Matthew is away from
 * the screen), then checked byte for byte against its SHA-256 before it is
 * ever loaded. Decision 13: the download is offered only when the phone has
 * the memory to run it, and when it hasn't, that is said plainly.
 */
class ModelStore(private val context: Context) {
    companion object {
        const val URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm?download=true"
        const val FILE = "gemma-4-E2B-it.litertlm"
        const val SIZE = 2_588_147_712L
        const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"

        /**
         * The hosts the download may pass through, and no others: Hugging Face,
         * which answers with a redirect, and the content servers it redirects a
         * Xet-stored file like this one to. us.aws.cdn.hf.co is where it went
         * on 28 October 2026; the other two are Hugging Face's own for the same
         * files. network_security_config.xml trusts exactly these, and the beach.
         */
        val HOSTS = setOf("huggingface.co", "us.aws.cdn.hf.co", "us.gcp.cdn.hf.co", "cas-bridge.xethub.hf.co")

        /** Google's own gallery app asks 8 GB of a phone for this model; a phone sold with 8 GB tells apps a little less. */
        const val MEMORY_NEEDED = 7_000_000_000L

        /** Room to leave free beyond the model itself. */
        const val SPARE = 500_000_000L

        fun gb(bytes: Long) = "%.1f GB".format(bytes / 1e9)
    }

    sealed interface State {
        class TooLittleMemory(val has: Long) : State
        class TooLittleRoom(val free: Long, val needs: Long) : State
        data object Absent : State
        /** Fetching now: [done] of [SIZE], at [bytesPerSecond]; [note] says what it is doing when it isn't plain fetching. */
        class Fetching(val done: Long, val bytesPerSecond: Long, val note: String?) : State
        /** Part of it is on the phone, and nothing is fetching: it carries on from [done]. */
        class Paused(val done: Long) : State
        data object Checking : State
        /** It stopped for [why], with [done] bytes kept to carry on from. */
        class Failed(val why: String, val done: Long) : State
        data object Ready : State
    }

    private val dir get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "models")
    val file: File get() = File(dir, FILE)
    private val part get() = File(dir, "$FILE.part")
    private val prefs = context.getSharedPreferences("model", Context.MODE_PRIVATE)

    @Volatile private var checking = false
    @Volatile private var progress: ModelFetch.Progress? = null
    @Volatile private var stopping = false

    /** Whether the download is running now. */
    val fetching: Boolean get() = progress != null

    init {
        // 0.3 asked Android's download manager, which failed on this download; what it left is set aside.
        if (prefs.contains("download")) {
            prefs.edit().remove("download").remove("asked").apply()
            part.delete()
        }
    }

    fun memory(): Long = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem

    fun state(): State {
        if (prefs.getBoolean("checked", false) && file.length() == SIZE) return State.Ready
        if (checking) return State.Checking
        progress?.let { return State.Fetching(it.done, it.bytesPerSecond, it.note) }
        val have = part.length()
        prefs.getString("failed", null)?.let { return State.Failed(it, have) }
        if (have > 0) return State.Paused(have)
        val has = memory()
        if (has < MEMORY_NEEDED) return State.TooLittleMemory(has)
        val free = StatFs(dir.apply { mkdirs() }.path).availableBytes
        val needs = SIZE - have + SPARE
        if (free < needs) return State.TooLittleRoom(free, needs)
        return State.Absent
    }

    /**
     * Fetch the model, carrying on from whatever is on the phone, then check
     * it. Blocking: [FetchService] runs it on a thread of its own, telling
     * [report] how far it has come.
     */
    fun fetchNow(report: (ModelFetch.Progress) -> Unit) {
        if (fetching) return
        stopping = false
        prefs.edit().remove("failed").apply()
        dir.mkdirs()
        val start = ModelFetch.Progress(part.length(), SIZE, 0, "Connecting to Hugging Face…")
        progress = start
        report(start)
        try {
            val outcome = ModelFetch(URL, HOSTS, SIZE, connection = Unmetered(context)).run(part, { p -> progress = p; report(p) }, { stopping })
            when (outcome) {
                ModelFetch.Outcome.Complete -> {
                    progress = null
                    check()
                }
                is ModelFetch.Outcome.Stopped -> {}
                is ModelFetch.Outcome.Failed -> prefs.edit().putString("failed", outcome.why).apply()
            }
        } catch (e: Exception) {
            prefs.edit().putString("failed", "The download stopped: ${e.message ?: e::class.java.simpleName}").apply()
        } finally {
            progress = null
        }
    }

    /** Stop fetching; what has come is kept, to carry on from. */
    fun stop() {
        stopping = true
    }

    /** Remove what has come so far, to begin again from nothing. */
    fun discard() {
        stopping = true
        prefs.edit().remove("failed").apply()
        part.delete()
    }

    /** Once fetched: check every byte against the pinned SHA-256, and only then keep it as the model. Slow; call it off the main thread. */
    fun check(): State {
        checking = true
        try {
            if (part.length() != SIZE) return fail("The download came to ${gb(part.length())}, not ${gb(SIZE)}.")
            val digest = MessageDigest.getInstance("SHA-256")
            part.inputStream().buffered(1 shl 20).use { input ->
                val buffer = ByteArray(1 shl 20)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            if (sha != SHA256) {
                part.delete()
                return fail("The download didn't match the model's SHA-256 (it came to ${sha.take(12)}…, not ${SHA256.take(12)}…), so it was set aside. Get it again.")
            }
            if (!part.renameTo(file)) return fail("The model couldn't be put in place.")
            prefs.edit().putBoolean("checked", true).apply()
            return State.Ready
        } finally {
            checking = false
        }
    }

    /** Remove the model from the phone. */
    fun remove() {
        discard()
        prefs.edit().remove("checked").apply()
        file.delete()
    }

    /** Forget a failure; what was kept stays, to carry on from. */
    fun dismissFailure() {
        prefs.edit().remove("failed").apply()
    }

    private fun fail(why: String): State {
        prefs.edit().putString("failed", why).apply()
        return State.Failed(why, part.length())
    }
}
