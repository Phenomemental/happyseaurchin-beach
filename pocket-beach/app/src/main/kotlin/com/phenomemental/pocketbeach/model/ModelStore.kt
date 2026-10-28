package com.phenomemental.pocketbeach.model

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.StatFs
import java.io.File
import java.security.MessageDigest

/**
 * The companion's model on this phone: Gemma 4 E2B as LiteRT-LM packs it,
 * fetched once, from one pinned file on Hugging Face, by Android's download
 * manager over Wi-Fi only, then checked byte for byte against its SHA-256
 * before it is ever loaded. Decision 13: the download is offered only when the
 * phone has the memory to run it, and when it hasn't, that is said plainly.
 */
class ModelStore(private val context: Context) {
    companion object {
        const val URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/gemma-4-E2B-it.litertlm?download=true"
        const val FILE = "gemma-4-E2B-it.litertlm"
        const val SIZE = 2_588_147_712L
        const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"

        /** Google's own gallery app asks 8 GB of a phone for this model; a phone sold with 8 GB tells apps a little less. */
        const val MEMORY_NEEDED = 7_000_000_000L

        /** Room to leave free beyond the model itself. */
        const val SPARE = 500_000_000L

        fun gb(bytes: Long) = "%.1f GB".format(bytes / 1e9)
    }

    sealed interface State {
        class TooLittleMemory(val has: Long) : State
        class TooLittleRoom(val free: Long) : State
        data object Absent : State
        class Fetching(val done: Long, val waitingForWifi: Boolean) : State
        data object Fetched : State
        data object Checking : State
        class Failed(val why: String) : State
        data object Ready : State
    }

    private val dir get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "models")
    val file: File get() = File(dir, FILE)
    private val part get() = File(dir, "$FILE.part")
    private val prefs = context.getSharedPreferences("model", Context.MODE_PRIVATE)
    private val downloads get() = context.getSystemService(DownloadManager::class.java)

    @Volatile private var checking = false

    fun memory(): Long = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem

    fun state(): State {
        if (prefs.getBoolean("checked", false) && file.length() == SIZE) return State.Ready
        if (checking) return State.Checking
        prefs.getString("failed", null)?.let { return State.Failed(it) }
        val id = prefs.getLong("download", -1L)
        if (id >= 0) {
            downloads.query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (c.moveToFirst()) {
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    return when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> State.Fetched
                        DownloadManager.STATUS_FAILED -> {
                            forget()
                            fail("The download stopped (Android's download manager gave reason $reason). Try again on Wi-Fi.")
                        }
                        DownloadManager.STATUS_PAUSED -> State.Fetching(
                            done,
                            waitingForWifi = reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI || reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK,
                        )
                        else -> State.Fetching(done, waitingForWifi = false)
                    }
                }
            }
            forget()
        }
        val has = memory()
        if (has < MEMORY_NEEDED) return State.TooLittleMemory(has)
        val free = StatFs(dir.apply { mkdirs() }.path).availableBytes
        if (free < SIZE + SPARE) return State.TooLittleRoom(free)
        return State.Absent
    }

    /** Ask Android's download manager to fetch the model, over Wi-Fi only. */
    fun fetch() {
        prefs.edit().remove("failed").apply()
        dir.mkdirs()
        part.delete()
        val request = DownloadManager.Request(Uri.parse(URL))
            .setTitle("The companion's model")
            .setDescription("Gemma 4 E2B for Pocket beach, ${gb(SIZE)}")
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(part))
        prefs.edit().putLong("download", downloads.enqueue(request)).apply()
    }

    /** Stop fetching, and remove what has come so far. */
    fun stop() {
        prefs.getLong("download", -1L).takeIf { it >= 0 }?.let { downloads.remove(it) }
        forget()
        part.delete()
    }

    /** Once fetched: check every byte against the pinned SHA-256, and only then keep it as the model. Slow; call it off the main thread. */
    fun check(): State {
        checking = true
        try {
            prefs.getLong("download", -1L).takeIf { it >= 0 }?.let { prefs.edit().remove("download").apply() }
            if (part.length() != SIZE) return failFetch("The download came to ${gb(part.length())}, not ${gb(SIZE)}, so it was set aside.")
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
            if (sha != SHA256) return failFetch("The download didn't match the model's fingerprint, so it was set aside. Try again.")
            if (!part.renameTo(file)) return failFetch("The model couldn't be put in place.")
            prefs.edit().putBoolean("checked", true).apply()
            return State.Ready
        } finally {
            checking = false
        }
    }

    /** Remove the model from the phone. */
    fun remove() {
        stop()
        prefs.edit().remove("checked").remove("failed").apply()
        file.delete()
    }

    fun dismissFailure() { prefs.edit().remove("failed").apply() }

    private fun forget() { prefs.edit().remove("download").apply() }

    private fun fail(why: String): State {
        prefs.edit().putString("failed", why).apply()
        return State.Failed(why)
    }

    private fun failFetch(why: String): State {
        part.delete()
        return fail(why)
    }
}
