package com.phenomemental.pocketbeach

import android.app.Application
import android.content.res.AssetManager
import com.phenomemental.pocketbeach.bsp.FileStorage
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.Shelf
import com.phenomemental.pocketbeach.bsp.ShelfSource
import java.io.FileNotFoundException

/**
 * The one pocket on this phone. The core (the bsp module) knows nothing of
 * Android: the app hands it a storage for the blocks — files under its private
 * filesDir, as regions/pocket/<name>.json — and the teaching shelf read from
 * its own assets.
 */
class PocketApp : Application() {
    val pocket: PocketBsp by lazy { PocketBsp(FileStorage(filesDir), Shelf(AssetShelfSource(assets))) }
}

/** The teaching shelf as the app bundles it, at the root of its assets. */
class AssetShelfSource(private val assets: AssetManager) : ShelfSource {
    override fun read(file: String): String? = try {
        assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } catch (_: FileNotFoundException) {
        null
    }
}
