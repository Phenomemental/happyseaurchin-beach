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
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.ModelStore
import com.phenomemental.pocketbeach.net.HttpsWire
import com.phenomemental.pocketbeach.ui.Asking
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
    val gemma by lazy { Gemma(models, cacheDir.path) }

    /** What the Ask screen asks: Gemma. The device tests put a stand-in here, since no test can run the model. */
    @Volatile private var standIn: Mind? = null
    var mind: Mind
        get() = standIn ?: gemma
        set(value) { standIn = value }
    val asking by lazy { Asking(this) }

    /** A block another app has just shared in, waiting to be looked at. */
    var inbox by mutableStateOf<Received?>(null)

    /** What carries the one post. The device tests put a wire here that goes nowhere: no test posts to the beach. */
    @Volatile var wire: Wire = HttpsWire

    /** What has been posted already, and where it landed. */
    val posts by lazy { Posts(this) }

    /** Send [message] once, as Matthew confirmed it, and keep where it landed. Blocking: call it off the main thread. */
    fun post(message: PoolMessage): Posted =
        PoolPost.send(message, wire).also { if (it is Posted.Landed) posts.record(message, it.slot) }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Decision 13: when Android asks for memory back, the model goes first.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) gemma.release()
    }

    private val prefs by lazy { getSharedPreferences("pocket", MODE_PRIVATE) }

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
