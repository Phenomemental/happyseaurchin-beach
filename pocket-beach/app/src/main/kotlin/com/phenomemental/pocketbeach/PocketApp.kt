package com.phenomemental.pocketbeach

import android.app.Application
import android.content.res.AssetManager
import com.phenomemental.pocketbeach.bsp.PocketBsp
import com.phenomemental.pocketbeach.bsp.Shelf
import java.io.FileNotFoundException

/** The one pocket on this phone: blocks under filesDir/regions/pocket, the teaching shelf from the app's assets. */
class PocketApp : Application() {
    val pocket: PocketBsp by lazy { PocketBsp(filesDir, shelfFrom(assets)) }

    companion object {
        fun shelfFrom(assets: AssetManager): Shelf {
            fun read(name: String): String? = try {
                assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: FileNotFoundException) {
                null
            }
            return Shelf(read("index.json") ?: error("the teaching shelf is missing from the app")) { read(it) }
        }
    }
}
