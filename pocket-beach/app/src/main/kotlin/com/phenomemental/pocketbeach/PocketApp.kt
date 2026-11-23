package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.model.JobPrefs
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.AssetManager
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.bsp.FileStorage
import com.phenomemental.pocketbeach.bsp.FolderStorage
import com.phenomemental.pocketbeach.bsp.Move
import com.phenomemental.pocketbeach.bsp.Private
import com.phenomemental.pocketbeach.bsp.SwitchingStorage
import com.phenomemental.pocketbeach.bsp.RoutedStorage
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
import java.io.File
import java.io.FileNotFoundException

/**
 * The one pocket on this phone. The core (the bsp module) knows nothing of
 * Android: the app hands it a storage for the blocks — files under its private
 * filesDir, as regions/pocket/<name>.json, or, once he has moved them (Sundial
 * 0.11), his folder, Documents/Sundial — and the teaching shelf read from its
 * own assets. The pages the screens show are read and written through it.
 * The companion's model, and the one way out, are held here too.
 */
class PocketApp : Application() {
    /**
     * 0.8: the pocket, and beside it the hidden region (regions/app) the app's
     * own blocks move to once he has hatched: read and written by name as
     * before, never in his map or hers.
     */
    val storage: RoutedStorage by lazy {
        RoutedStorage(switching, privateRegion, Private::routes, openNow = runCatching { Private.open(switching, privateRegion) }.getOrDefault(false))
    }

    // ── Sundial 0.11: where his blocks are kept (spine:pocket-beach 1.111178719942) ──

    /** His blocks in the app's own files, as every version before 0.11 kept them; after the move, the copy kept inside the app. */
    val privatePocket by lazy { FileStorage(filesDir) }

    /** The private region (regions/app): the app's own blocks, and from 0.11 his shell and his history. Never in his folder. */
    val privateRegion by lazy { FileStorage(filesDir, "app") }

    /** The device tests' own folder in place of his (Documents/Sundial-test): no test touches Documents/Sundial. */
    @Volatile var folderForTests: File? = null

    /** His folder: Documents/Sundial on this phone. */
    val folderDir: File
        get() = folderForTests ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER)

    /** Whether his blocks are kept in his folder (after the move), not in the app's own files. */
    val inFolder: Boolean get() = prefs.getBoolean(IN_FOLDER, false)

    /** The device tests say whether Sundial has all-files access, whatever Android says. */
    @Volatile var accessForTests: Boolean? = null

    /** Whether Android lets Sundial reach shared storage: what his folder needs. */
    fun allFilesAccess(): Boolean = accessForTests ?: runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)

    /** Why no write may go now, or null: his blocks are in his folder, and Sundial has lost its access to it. */
    fun barred(): String? = if (inFolder && !allFilesAccess()) BARRED else null

    private val switching by lazy { SwitchingStorage(if (inFolder) FolderStorage(folderDir) else privatePocket, ::barred) }

    /** His folder as the pocket reads it, once his blocks are there; null while they are in the app's own files. */
    val folder: FolderStorage? get() = switching.target as? FolderStorage

    /** What the move would do now, writing nothing: which blocks go, which stay inside the app, and why it can't start, if it can't. */
    fun moveCheck(): Move.Check = Move.check(privatePocket, FolderStorage(folderDir), Private::routes)

    /**
     * The move (spine:pocket-beach 1.111178719933): a whole copy saved, every
     * block copied and checked, and only then the switch, between writes.
     * Blocking: call it off the main thread.
     */
    fun move(): Move.Moved = synchronized(pocket) {
        if (inFolder) return Move.Moved(emptyList(), emptyList(), null, "Your blocks are in Documents/Sundial already.")
        if (!storage.isOpen) return Move.Moved(emptyList(), emptyList(), null, "Your blocks move once you have a shell on this phone, so the app's own blocks stay inside the app.")
        if (!allFilesAccess()) return Move.Moved(emptyList(), emptyList(), null, "Sundial needs all-files access first.")
        val folder = FolderStorage(folderDir)
        val now = System.currentTimeMillis()
        val copy = File(File(filesDir, MOVE_COPIES), "before-the-folder-${com.phenomemental.pocketbeach.bsp.isoString(now).replace(':', '-')}.json")
        val moved = Move.carryOut(privatePocket, privateRegion, folder, Private::routes, storage, copy, now)
        if (moved.ok) {
            prefs.edit().putBoolean(IN_FOLDER, true).commit()
            switching.target = folder
        }
        moved
    }

    /** Whether the copy kept inside the app from before the move still stands. */
    val privateCopyStands: Boolean
        get() = inFolder && File(filesDir, "regions/pocket").list()?.isNotEmpty() == true

    /** Remove the copy kept inside the app, on his word: his blocks stay in his folder. */
    fun removePrivateCopy() {
        if (!inFolder) return
        File(filesDir, "regions/pocket").deleteRecursively()
        File(filesDir, "regions/pocket.meta.json").delete()
    }

    /** The device tests begin with his blocks in the app's own files, as a fresh phone keeps them. */
    fun ownFilesForTests() = synchronized(pocket) {
        prefs.edit().putBoolean(IN_FOLDER, false).putBoolean(FOLDER_OFFERED, true).commit()
        switching.target = privatePocket
    }

    /** Whether Your pocket has offered the folder already: it asks once. */
    var folderOffered: Boolean
        get() = prefs.getBoolean(FOLDER_OFFERED, false)
        set(v) {
            prefs.edit().putBoolean(FOLDER_OFFERED, v).apply()
        }
    val shelf by lazy { Shelf(AssetShelfSource(assets)) }
    /** Rockpool: the app's own procedures as shipped (cook, kernel, spine:p6), read only. */
    val procedures by lazy { Shelf(AssetShelfSource(assets, "procedures/")) }
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

    /** 0.8: his shell on this phone, born or adopted. */
    val hatching by lazy { com.phenomemental.pocketbeach.ui.Hatching(this) }
    val benching by lazy { Benching(this) }

    /** The one speech recogniser the app may hold, made on a tap and let go as soon as listening ends. */
    val speech by lazy { com.phenomemental.pocketbeach.ui.Speech() }

    /** A block another app has just shared in, waiting to be looked at. */
    var inbox by mutableStateOf<Received?>(null)

    /** What carries the one post. The device tests put a wire here that goes nowhere: no test posts to the beach. */
    @Volatile var wire: Wire = HttpsWire

    /** Sundial 0.12: the walker's read of the beach, a GET only, made only while Reach the beach is on. */
    @Volatile var reader: com.phenomemental.pocketbeach.bsp.BeachReader = com.phenomemental.pocketbeach.bsp.BeachReader { HttpsWire.get(it) }

    /** What has been posted already, and where it landed. */
    val posts by lazy { Posts(this) }

    /** What the beach couldn't be reached for, kept for "Post now". */
    val unsent by lazy { Unsent(this, storage) }

    /** Sundial 0.10: today's words, not yet kept (spine:pocket-beach 1.111178719915). */
    val waiting by lazy { WaitingRoom(filesDir) }

    /** Sundial 0.10: where he stands, for the sun. */
    val where by lazy { WhereHeStands(this) }

    /** Sundial 0.10: the notes he has sent to his room. */
    val notesSent by lazy { NotesSent(filesDir) }

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
    fun post(message: PoolMessage): Posted = (ownRoomRefuses(message)?.let { Posted.Refused(it) } ?: PoolPost.send(message, wire)).also { p ->
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

    /**
     * Sundial 0.10: only this phone's own pocket, once hatched, posts, under
     * its own handle, to its own room. Why [message] may not go, or null when
     * it may. Nothing refused here touches the wire.
     */
    private fun ownRoomRefuses(message: PoolMessage): String? {
        if (message.scratch) return PoolPost.SCRATCH
        val own = runCatching { PoolPost.room(storage) }.getOrNull()
            ?: return "Posting waits for your shell: nothing is posted until this phone's pocket is hatched."
        if (own.scratch) return PoolPost.SCRATCH
        if (message.author != own.author || message.pool != own.pool)
            return "This post was made as ${message.author} for pool:${message.pool}; this phone posts only as ${own.author}, to pool:${own.pool}."
        return null
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Decision 13: when Android asks for memory back, the model goes first. Sundial 0.10.1: only when it is really short
        // (TRIM_MEMORY_COMPLETE), never because he looked at another app: the model is loaded once and kept.
        @Suppress("DEPRECATION")
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) gemma.release()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        gemma.release()
    }

    private val prefs by lazy { getSharedPreferences("pocket", MODE_PRIVATE) }

    private val jobs by lazy { mutableStateOf(Job.values().associateWith { JobPrefs.read(prefs, it) }) }

    /** Each job's settings (Settings): Edge's for talking, 0.4's for writing, until he changes them. */
    fun settings(job: Job): JobSettings = jobs.value.getValue(job)

    /**
     * Sundial 0.10.1: a job he never set apart (its settings the app's own)
     * runs on Talk's engine, Talk's model, processor and context, with its own
     * drawing: Write and Sort never load a second model beside her talk's.
     */
    fun followsTalk(job: Job): Boolean = job != Job.TALK && settings(job) == JobSettings.of(job) && settings(Job.TALK).engine != settings(job).engine

    private fun effective(): Map<Job, JobSettings> {
        val talk = settings(Job.TALK)
        return jobs.value.mapValues { (job, s) -> if (followsTalk(job)) s.copy(model = talk.model, processor = talk.processor, context = talk.context) else s }
    }

    /** [job]'s settings as it runs: on the CPU when it asks for the GPU and another job's engine holds it (0.6.2); on Talk's engine when he never set it apart (0.10.1). */
    fun running(job: Job): JobSettings = Gpu.running(effective(), job)

    /** Why [job] runs on the CPU though set to the GPU: set apart from her engine, or the GPU couldn't take it. Null when it runs as set. */
    fun cpuWhy(job: Job): String? = Gpu.why(effective(), job)
        ?: running(job).takeIf { it.processor == "GPU" && mind.gpuFailed(it) }?.let { "the GPU couldn't take it, so it stays on the CPU until Sundial is opened again" }

    fun set(job: Job, s: JobSettings) {
        jobs.value = jobs.value + (job to s)
        JobPrefs.write(prefs, job, s)
    }

    private val details by lazy { mutableStateOf(prefs.getBoolean("showDetails", false)) }

    /** Whether Ask shows its workings under the conversation: her shell, the times, what each ask came to (Ask's Details switch). */
    var showDetails: Boolean
        get() = details.value
        set(on) {
            details.value = on
            prefs.edit().putBoolean("showDetails", on).apply()
        }

    private val reach by lazy { mutableStateOf(prefs.getBoolean("reachBeach", false)) }

    /** Sundial 0.12 (spine:pocket-beach 1.111178719995): whether the walker may read the beach's blocks, read only; off unless he turns it on in Settings. */
    var reachBeach: Boolean
        get() = reach.value
        set(on) {
            reach.value = on
            prefs.edit().putBoolean("reachBeach", on).apply()
        }

    /**
     * Sundial 0.12.1: the walk check's last result on this phone: how many of
     * how many arrived intact, how many arrived altered (as a number, or
     * changed; a call that never came isn't counted), when, and on which model.
     */
    class WalkChecked(val intact: Int, val of: Int, val altered: Int, val atMs: Long, val model: String) {
        /** Every address arrived as sent. */
        val allIntact: Boolean get() = intact == of
    }

    private val walkChecked by lazy {
        mutableStateOf(prefs.getInt("walkCheckOf", 0).takeIf { it > 0 }?.let { WalkChecked(prefs.getInt("walkCheckIntact", 0), it, prefs.getInt("walkCheckAltered", 0), prefs.getLong("walkCheckAt", 0), prefs.getString("walkCheckModel", "") ?: "") })
    }

    /** Sundial 0.12.1 (spine:pocket-beach 1.1111787199993): the walk check's last result, or null before he has run it. */
    var walkCheck: WalkChecked?
        get() = walkChecked.value
        set(c) {
            walkChecked.value = c
            prefs.edit().apply {
                if (c == null) remove("walkCheckOf").remove("walkCheckIntact").remove("walkCheckAltered").remove("walkCheckAt").remove("walkCheckModel")
                else putInt("walkCheckOf", c.of).putInt("walkCheckIntact", c.intact).putInt("walkCheckAltered", c.altered).putLong("walkCheckAt", c.atMs).putString("walkCheckModel", c.model)
            }.apply()
        }

    private val askLog by lazy { mutableStateOf(prefs.getBoolean("keepAskLog", false)) }

    /** 0.6.4: whether each exchange in Ask is kept in the ask log; off unless he turns it on in Settings. */
    var keepAskLog: Boolean
        get() = askLog.value
        set(on) {
            askLog.value = on
            prefs.edit().putBoolean("keepAskLog", on).apply()
        }

    /** When the last copy was saved, kept on the phone beside the blocks (never in a copy). */
    var lastSavedMs: Long?
        get() = prefs.getLong("lastSavedMs", -1L).takeIf { it >= 0 }
        set(ms) {
            prefs.edit().apply { if (ms == null) remove("lastSavedMs") else putLong("lastSavedMs", ms) }.apply()
        }
}

/** Sundial 0.11: his folder's name in Documents, the preferences that say where his blocks are, and what the plain page says. */
const val FOLDER = "Sundial"
private const val IN_FOLDER = "blocksInFolder"
private const val FOLDER_OFFERED = "folderOffered"
private const val MOVE_COPIES = "copies-before-folder"
const val BARRED = "Sundial can't reach Documents/Sundial, where your blocks are kept, so nothing is written until Android lets it again."

/** The teaching shelf as the app bundles it, at the root of its assets. */
class AssetShelfSource(private val assets: AssetManager, private val folder: String = "") : ShelfSource {
    override fun read(file: String): String? = try {
        assets.open(folder + file).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (_: FileNotFoundException) {
        null
    }
}
