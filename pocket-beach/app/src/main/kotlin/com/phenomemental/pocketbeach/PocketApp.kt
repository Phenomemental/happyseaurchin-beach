package com.phenomemental.pocketbeach

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.AssetManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.bsp.FileStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.ShelfSource
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Wire
import com.phenomemental.pocketbeach.model.Gemma
import com.phenomemental.pocketbeach.model.Gpu
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.ModelStore
import com.phenomemental.pocketbeach.net.Connection
import com.phenomemental.pocketbeach.net.HttpsWire
import com.phenomemental.pocketbeach.ui.Asking
import com.phenomemental.pocketbeach.ui.Benching
import com.phenomemental.pocketbeach.ui.Received
import java.io.FileNotFoundException

/**
 * The one pocket on this phone. The core (the bsp module) knows nothing of
 * Android: the app hands it a storage for the blocks — files under its private
 * filesDir, as regions/pocket/<name>.json — and the teaching shelf read from
 * its own assets. The pages the screens show are read and written through it.
 * The companion's model, and the one way out, are held here too.
 */
class PocketApp : Application() {
    val storage by lazy { FileStorage(filesDir) }
    val shelf by lazy { Shelf(AssetShelfSource(assets)) }
    val pocket: PocketBsp by lazy { PocketBsp(storage, shelf) }
    val pages: Pages by lazy { Pages(storage, shelf, pocket) }
    val models by lazy { ModelStore(this) }

    /** Gemma 4 E4B, the bigger: fetched only when Matthew asks for it in Settings. */
    val e4b by lazy { ModelStore(this, ModelStore.E4B) }

    /** The store of the model named [name] (Gemma 4 E2B or E4B). */
    fun store(name: String): ModelStore = if (name == ModelStore.E4B.name) e4b else models

    val gemma by lazy { Gemma({ name -> store(name) }, cacheDir.path, applicationInfo.nativeLibraryDir) }

    /** What the Ask screen asks: Gemma. The device tests put a stand-in here, since no test can run the model. */
    @Volatile private var standIn: Mind? = null
    var mind: Mind
        get() = standIn ?: gemma
        set(value) { standIn = value }
    val asking by lazy { Asking(this) }
    val benching by lazy { Benching(this) }

    /** The one speech recogniser the app may hold, made on a tap and let go as soon as listening ends. */
    val speech by lazy { com.phenomemental.pocketbeach.ui.Speech() }

    /** A block another app has just shared in, waiting to be looked at. */
    var inbox by mutableStateOf<Received?>(null)

    /** What carries the one post. The device tests put a wire here that goes nowhere: no test posts to the beach. */
    @Volatile var wire: Wire = HttpsWire

    /** What has been posted already, and where it landed. */
    val posts by lazy { Posts(this) }

    /** What the beach couldn't be reached for, kept for "Post now". */
    val unsent by lazy { Unsent(this) }

    /** Whether the phone has a connection, as Android says: only so "Post now" waits for one. */
    private var connected by mutableStateOf(false)

    /** The device tests say the connection is up or down, whatever Android says. */
    var connectedForTests by mutableStateOf<Boolean?>(null)

    val online: Boolean get() = connectedForTests ?: connected

    override fun onCreate() {
        super.onCreate()
        runCatching { Connection.watch(this) { connected = it } }
    }

    /**
     * Send [message] once, as Matthew confirmed it: where it landed is kept,
     * and if the beach couldn't be reached the message is kept instead, to go
     * only when he taps "Post now". Blocking: call it off the main thread.
     */
    fun post(message: PoolMessage): Posted = PoolPost.send(message, wire).also { p ->
        val now = System.currentTimeMillis()
        when (p) {
            is Posted.Landed -> {
                posts.record(message, p.slot)
                unsent.drop(message)
            }
            is Posted.Unreached -> unsent.keep(message, p.why, p.mayHaveLanded, now)
            is Posted.Refused -> unsent.note(message, p.why, now)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Decision 13: when Android asks for memory back, the model goes first.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) gemma.release()
    }

    private val prefs by lazy { getSharedPreferences("pocket", MODE_PRIVATE) }

    private val jobs by lazy { mutableStateOf(Job.values().associateWith { JobSettings.read(prefs, it) }) }

    /** Each job's settings (Settings): Edge's for talking, 0.4's for writing, until he changes them. */
    fun settings(job: Job): JobSettings = jobs.value.getValue(job)

    /** [job]'s settings as it runs: on the CPU when it asks for the GPU and another job's engine holds it (0.6.2). */
    fun running(job: Job): JobSettings = Gpu.running(jobs.value, job)

    /** Why [job] runs on the CPU though set to the GPU: another job's engine holds it, or the GPU failed on one of its answers. Null when it runs as set. */
    fun cpuWhy(job: Job): String? = Gpu.why(jobs.value, job)
        ?: running(job).takeIf { it.processor == "GPU" && mind.gpuFailed(it) }?.let { "the GPU failed on one of its answers, so it stays on the CPU until the app starts again" }

    fun set(job: Job, s: JobSettings) {
        jobs.value = jobs.value + (job to s)
        JobSettings.write(prefs, job, s)
    }

    private val details by lazy { mutableStateOf(prefs.getBoolean("showDetails", false)) }

    /** Whether Ask shows its workings under the conversation: her shell, the times, what each ask came to (Ask's Details switch). */
    var showDetails: Boolean
        get() = details.value
        set(on) {
            details.value = on
            prefs.edit().putBoolean("showDetails", on).apply()
        }

    /** When the last copy was saved, kept on the phone beside the blocks (never in a copy). */
    var lastSavedMs: Long?
        get() = prefs.getLong("lastSavedMs", -1L).takeIf { it >= 0 }
        set(ms) {
            prefs.edit().apply { if (ms == null) remove("lastSavedMs") else putLong("lastSavedMs", ms) }.apply()
        }
}

/** The teaching shelf as the app bundles it, at the root of its assets. */
class AssetShelfSource(private val assets: AssetManager) : ShelfSource {
    override fun read(file: String): String? = try {
        assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (_: FileNotFoundException) {
        null
    }
}
