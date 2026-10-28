package com.phenomemental.pocketbeach

import android.app.Application
import android.content.res.AssetManager
import com.phenomemental.pocketbeach.bsp.FileStorage
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.ShelfSource
import java.io.FileNotFoundException

/**
 * The one pocket on this phone. The core (the bsp module) knows nothing of
 * Android: the app hands it a storage for the blocks — files under its private
 * filesDir, as regions/pocket/<name>.json — and the teaching shelf read from
 * its own assets. The pages the screens show are read and written through it.
 */
class PocketApp : Application() {
    private val storage by lazy { FileStorage(filesDir) }
    private val shelf by lazy { Shelf(AssetShelfSource(assets)) }
    val pocket: PocketBsp by lazy { PocketBsp(storage, shelf) }
    val pages: Pages by lazy { Pages(storage, shelf, pocket) }

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
