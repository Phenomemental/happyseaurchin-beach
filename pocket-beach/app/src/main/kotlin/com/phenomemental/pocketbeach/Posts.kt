package com.phenomemental.pocketbeach

import android.content.Context
import com.phenomemental.pocketbeach.bsp.PoolMessage
import java.security.MessageDigest

/**
 * Every message the pocket has posted, and the entry it landed at, kept on
 * the phone beside the blocks: so the same message is never sent twice
 * without Matthew asking for it again on purpose.
 */
class Posts(context: Context) {
    private val prefs = context.getSharedPreferences("posts", Context.MODE_PRIVATE)

    /** The entry [message] landed at, if it was posted already. */
    fun landed(message: PoolMessage): String? = prefs.getString(key(message), null)

    fun record(message: PoolMessage, entry: String) {
        prefs.edit().putString(key(message), entry).apply()
    }

    private fun key(m: PoolMessage): String =
        MessageDigest.getInstance("SHA-256").digest("${m.pool}\n${m.message}".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
