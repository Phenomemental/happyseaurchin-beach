package com.phenomemental.pocketbeach

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.bsp.JsBool
import com.phenomemental.pocketbeach.bsp.JsNum
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.JsStr
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import java.security.MessageDigest

/**
 * Messages Matthew said yes to that the beach couldn't be reached for, kept
 * on the phone until he posts them with "Post now" or discards them. Nothing
 * here sends anything: a kept message goes only on his tap.
 */
class Unsent(context: Context) {
    /** A kept message, when it last failed and why; [mayHaveLanded] when it went but no answer came back. */
    class Kept(val message: PoolMessage, val atMs: Long, val why: String, val mayHaveLanded: Boolean)

    private val prefs = context.getSharedPreferences("unsent", Context.MODE_PRIVATE)

    /** Every kept message, oldest first. */
    var all by mutableStateOf(load())
        private set

    /** Keep [message], or note its latest failure if it is kept already. */
    fun keep(message: PoolMessage, why: String, mayHaveLanded: Boolean, atMs: Long) {
        val entry = JsObj.of(
            "pool" to str(message.pool),
            "message" to str(message.message),
            "why" to str(why),
            "may" to JsBool(mayHaveLanded),
            "at" to JsNum(atMs.toDouble()),
        )
        prefs.edit().putString(key(message), stringify(entry)).commit()
        all = load()
    }

    /** A kept message's latest failure, when the beach answered but took nothing; a message not kept stays not kept. */
    fun note(message: PoolMessage, why: String, atMs: Long) {
        val kept = all.firstOrNull { key(it.message) == key(message) } ?: return
        keep(message, why, kept.mayHaveLanded, atMs)
    }

    fun drop(message: PoolMessage) {
        if (!prefs.contains(key(message))) return
        prefs.edit().remove(key(message)).commit()
        all = load()
    }

    /** Forget every kept message, as the device tests begin. */
    fun clear() {
        prefs.edit().clear().commit()
        all = emptyList()
    }

    private fun load(): List<Kept> = prefs.all.values.mapNotNull { v ->
        val o = (v as? String)?.let { runCatching { parseJson(it) as? JsObj }.getOrNull() } ?: return@mapNotNull null
        val message = runCatching { PoolPost.prepare((o["pool"] as JsStr).value, (o["message"] as JsStr).value) }.getOrNull() ?: return@mapNotNull null
        Kept(message, ((o["at"] as? JsNum)?.value ?: 0.0).toLong(), (o["why"] as? JsStr)?.value.orEmpty(), (o["may"] as? JsBool)?.value == true)
    }.sortedBy { it.atMs }

    private fun key(m: PoolMessage): String =
        MessageDigest.getInstance("SHA-256").digest("${m.pool}\n${m.message}".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
