package life.matthewbushell.tideline

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import life.matthewbushell.tideline.seal.Gray
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/*
 * The catch's two words on the phone (spine:tideline 7.16): its edit latch,
 * which proves the right to write, and its reading key, which seals. Asked
 * for once and kept in Android's encrypted storage only: each word is sealed
 * with an AES key that lives in the phone's keystore and cannot be read out
 * of it. Never in the repo, the build or a log.
 */
object Keys {
    private const val PREFS = "keys"
    private const val LATCH = "latch"
    private const val READING = "reading"
    private const val ALIAS = "tideline-words"

    @Volatile
    private var made: ByteArray? = null

    fun save(context: Context, latch: String, reading: String) {
        prefs(context).edit().putString(LATCH, wrap(latch)).putString(READING, wrap(reading)).apply()
        forget()
    }

    /** Both words are kept (without opening them: the screen asks every few seconds). */
    fun present(context: Context): Boolean = prefs(context).let { it.contains(LATCH) && it.contains(READING) }

    fun latch(context: Context): String? = unwrap(prefs(context).getString(LATCH, null))

    private fun reading(context: Context): String? = unwrap(prefs(context).getString(READING, null))

    /** The sealing key, made from the reading key once a run. Slow the first time: call it off the main thread. */
    @Synchronized
    fun sealKey(context: Context): ByteArray? {
        made?.let { return it }
        val word = reading(context) ?: return null
        return Gray.key(word).also { made = it }
    }

    @Synchronized
    private fun forget() {
        made?.fill(0)
        made = null
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun wrap(word: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val b64 = Base64.getEncoder()
        return b64.encodeToString(cipher.iv) + ":" + b64.encodeToString(cipher.doFinal(word.toByteArray(Charsets.UTF_8)))
    }

    private fun unwrap(kept: String?): String? {
        if (kept == null) return null
        return try {
            val (iv, sealed) = kept.split(":", limit = 2).map { Base64.getDecoder().decode(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(sealed), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
