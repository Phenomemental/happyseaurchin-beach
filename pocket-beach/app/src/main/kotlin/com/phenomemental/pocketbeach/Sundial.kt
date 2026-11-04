package com.phenomemental.pocketbeach

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.bsp.JsArr
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.JsStr
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.Waiting
import com.phenomemental.pocketbeach.bsp.isoString
import com.phenomemental.pocketbeach.bsp.parseIsoMs
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringifyPretty
import java.io.File
import java.time.LocalDate

/**
 * Sundial 0.10, today's moments before they are kept (spine:pocket-beach
 * 1.111178719915): the app's own file, moments.json, never a block. Each
 * message of his and its instant go here as he speaks, so the dial's ring
 * shows them at once and they survive the app closing; Keep today writes
 * them to his now and clears them (a); a new day lets older go (b); Show the
 * workings shows it, and a whole copy carries it (c); his words only (d).
 */
class WaitingRoom(private val dir: File) {
    private val file get() = File(dir, Waiting.FILE)

    /** Every moment waiting, oldest first. */
    var all by mutableStateOf(load())
        private set

    private fun load(): List<Keep.Moment> = Waiting.read(runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull())

    @Synchronized private fun save(ms: List<Keep.Moment>) {
        all = ms
        if (ms.isEmpty()) runCatching { file.delete() } else runCatching { file.writeText(Waiting.write(ms)) }
    }

    /** His words said at [atMs], waiting for Keep today. */
    @Synchronized fun add(words: String, atMs: Long) = save(Waiting.add(all, words, atMs))

    /** Sundial 0.10.3: the places she named for the moment said at [atMs], from her reply's places line: kept with it for Keep today. */
    @Synchronized fun place(atMs: Long, places: List<Int>) { if (places.isNotEmpty()) save(Waiting.placed(all, atMs, places)) }

    /** Those waiting from [day], the beach's day. */
    fun of(day: LocalDate): List<Keep.Moment> = Waiting.of(all, day)

    /** (a) [day] kept: its moments are in his now; they go from here. */
    @Synchronized fun kept(day: LocalDate) = save(Waiting.kept(all, day))

    /** (b) A new day: today's and yesterday's stay, older are let go. */
    @Synchronized fun letGo(today: LocalDate) = save(Waiting.letGo(all, today))

    /** (b) The day before [today], when its moments still wait. */
    fun yesterday(today: LocalDate): LocalDate? = Waiting.yesterdayWaiting(all, today)

    /** (c) The file as a whole copy carries it; null when nothing waits. */
    fun text(): String? = all.takeIf { it.isNotEmpty() }?.let { Waiting.write(it) }

    /** (c) Put back what a restored copy carried. */
    @Synchronized fun restore(text: String?) = save(Waiting.read(text))

    /** For the device tests: nothing waiting. */
    @Synchronized fun clear() = save(emptyList())
}

/**
 * Sundial 0.10, where he stands (spine:pocket-beach 1.111178719914): set once,
 * roughly, kept on the phone and never sent; from the phone's rough location
 * on his permission, or a town chosen from the list shipped with the app
 * (GeoNames towns of 15,000 or more, CC BY 4.0). The passport isn't consulted.
 */
class WhereHeStands(context: Context) {
    private val prefs = context.getSharedPreferences("where", Context.MODE_PRIVATE)

    /** A place: its name as he'd say it, and where it is, to a tenth of a degree from the phone and a town's own from the list. */
    data class Place(val name: String, val lat: Double, val lon: Double, val from: String)

    var place by mutableStateOf(load())
        private set

    private fun load(): Place? {
        val name = prefs.getString("name", null) ?: return null
        if (!prefs.contains("lat")) return null
        return Place(name, prefs.getFloat("lat", 0f).toDouble(), prefs.getFloat("lon", 0f).toDouble(), prefs.getString("from", "town") ?: "town")
    }

    fun set(p: Place?) {
        place = p
        val e = prefs.edit()
        if (p == null) e.clear() else e.putString("name", p.name).putFloat("lat", p.lat.toFloat()).putFloat("lon", p.lon.toFloat()).putString("from", p.from)
        e.apply()
    }

    /** Whether he has been asked for the phone's rough location yet (asked once). */
    var asked: Boolean
        get() = prefs.getBoolean("asked", false)
        set(v) { prefs.edit().putBoolean("asked", v).apply() }

    companion object {
        /** A fix rounded to a tenth of a degree: roughly where, never exactly. */
        fun rounded(lat: Double, lon: Double): Pair<Double, Double> = Math.round(lat * 10) / 10.0 to Math.round(lon * 10) / 10.0
    }
}

/** Sundial 0.10, the towns shipped with the app (assets/towns.tsv): name, country, latitude, longitude, largest first. */
object Towns {
    data class Town(val name: String, val country: String, val lat: Double, val lon: Double)

    @Volatile private var loaded: List<Town>? = null

    fun all(context: Context): List<Town> = loaded ?: synchronized(this) {
        loaded ?: context.assets.open("towns.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { l ->
                val p = l.split('\t')
                if (p.size < 4) null else Town(p[0], p[1], p[2].toDoubleOrNull() ?: return@mapNotNull null, p[3].toDoubleOrNull() ?: return@mapNotNull null)
            }.toList()
        }.also { loaded = it }
    }

    /** The towns whose name begins with [typed], largest first, at most [n]; then those that hold it. */
    fun find(context: Context, typed: String, n: Int = 12): List<Town> {
        val t = typed.trim().lowercase()
        if (t.length < 2) return emptyList()
        val all = all(context)
        val starts = all.asSequence().filter { it.name.lowercase().startsWith(t) }.take(n).toList()
        if (starts.size >= n) return starts
        return starts + all.asSequence().filter { it !in starts && it.name.lowercase().contains(t) }.take(n - starts.size).toList()
    }
}

/**
 * Sundial 0.10, Your room (spine:pocket-beach 1.111178719918): the notes he
 * has sent from the phone, newest first, with their words, time and entry,
 * kept in the app's own file, never a block.
 */
class NotesSent(private val dir: File) {
    data class Note(val words: String, val atMs: Long, val entry: String)

    private val file get() = File(dir, "notes-sent.json")

    var all by mutableStateOf(load())
        private set

    private fun load(): List<Note> = runCatching {
        val arr = (parseJson(file.readText()) as JsObj)["notes"] as JsArr
        arr.items.mapNotNull { e ->
            val o = e as? JsObj ?: return@mapNotNull null
            Note((o["words"] as? JsStr)?.value ?: return@mapNotNull null, (o["at"] as? JsStr)?.value?.let(::parseIsoMs) ?: 0L, (o["entry"] as? JsStr)?.value.orEmpty())
        }.sortedByDescending { it.atMs }
    }.getOrDefault(emptyList())

    @Synchronized fun add(words: String, atMs: Long, entry: String) {
        val notes = (listOf(Note(words, atMs, entry)) + all).sortedByDescending { it.atMs }
        all = notes
        runCatching {
            file.writeText(stringifyPretty(JsObj.of(
                "_" to str("The notes he has sent to his room from the phone, newest first: the app's own record, never a block."),
                "notes" to JsArr(notes.map { JsObj.of("words" to str(it.words), "at" to str(isoString(it.atMs)), "entry" to str(it.entry)) as com.phenomemental.pocketbeach.bsp.Js? }.toMutableList()),
            )) + "\n")
        }
    }

    @Synchronized fun clear() {
        all = emptyList()
        runCatching { file.delete() }
    }
}
