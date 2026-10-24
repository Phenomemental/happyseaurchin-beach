package life.matthewbushell.tideline.seal

import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.util.Arrays as Bc
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.SecureRandom
import java.util.Base64

/*
 * The beach's gray seal, self mode, ported from bsp-mcp's src/keys.ts
 * (deriveKeypair, selfEncrypt, selfDecrypt) and nothing invented (the plan at
 * spine:tideline 7.13). A line sealed here opens through the Pscale tool, and
 * one sealed through the tool opens here:
 *
 *   key   Argon2id, version 0x13, over the reading key's UTF-8 (64 MB, 3
 *         passes, parallelism 1, 64 bytes), salted with the UTF-8 of the name
 *         the beach is addressed by, padded with noughts to 8 if shorter; the
 *         first 32 bytes are the key (keys.ts takes them as an X25519 secret
 *         key and uses them as they are).
 *   seal  NaCl secretbox (XSalsa20-Poly1305) with a fresh random 24-byte
 *         nonce: the first 32 bytes of the stream key Poly1305, the rest
 *         encrypt the text, and the box is the 16-byte tag then the
 *         ciphertext. Both travel as standard base64.
 *   shape { "_": "Encrypted (gray); readable only with the author secret.",
 *           "1": <ciphertext>, "2": <nonce>, "9": { "_": "gray", "1": "self" } }
 *
 * The tool salts with the agent_id its caller passed, character for
 * character, so a line sealed under BEACH opens only for a reader who
 * addresses the beach as exactly that.
 *
 * The reading key is passed in for one call and kept nowhere here; the key
 * it makes is slow to make on purpose, so the app makes it once and holds
 * it in memory, never the word.
 */
object Gray {
    const val BEACH = "https://beach.happyseaurchin.com"
    const val NOTE = "Encrypted (gray); readable only with the author secret."

    const val KEY_BYTES = 32
    const val NONCE_BYTES = 24
    private const val TAG_BYTES = 16

    private val random = SecureRandom()

    /** One sealed line as it stands on the beach. */
    class Envelope(val ciphertext: String, val nonce: String, val mode: String = "self") {
        fun toJson(): String =
            "{\"_\":\"$NOTE\",\"1\":\"$ciphertext\",\"2\":\"$nonce\",\"9\":{\"_\":\"gray\",\"1\":\"${mode}\"}}"
    }

    /** The key a reading key makes under a salt. Slow: make it once and keep it. */
    fun key(readingKey: String, salt: String = BEACH): ByteArray {
        val s = if (salt.length >= 8) salt else salt.padEnd(8, '\u0000')
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3)
            .withMemoryAsKB(65536)
            .withParallelism(1)
            .withSalt(utf8(s))
            .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val hash = ByteArray(64)
        val word = utf8(readingKey)
        generator.generateBytes(word, hash)
        val key = hash.copyOfRange(0, KEY_BYTES)
        hash.fill(0)
        word.fill(0)
        return key
    }

    /** Seal a line under a key made by [key]. */
    fun seal(text: String, key: ByteArray): Envelope {
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        return Envelope(b64(secretbox(utf8(text), nonce, key)), b64(nonce))
    }

    /** Open a sealed line, or null when the key is not its key or the envelope was changed. */
    fun open(envelope: Envelope, key: ByteArray): String? {
        if (envelope.mode != "self") return null
        val box: ByteArray
        val nonce: ByteArray
        try {
            box = Base64.getDecoder().decode(envelope.ciphertext)
            nonce = Base64.getDecoder().decode(envelope.nonce)
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (nonce.size != NONCE_BYTES) return null
        val plain = secretboxOpen(box, nonce, key) ?: return null
        return String(plain, Charsets.UTF_8)
    }

    /** NaCl crypto_secretbox, as tweetnacl's nacl.secretbox. */
    fun secretbox(message: ByteArray, nonce: ByteArray, key: ByteArray): ByteArray {
        val stream = stream(nonce, key)
        val macKey = ByteArray(32)
        stream.processBytes(ByteArray(32), 0, 32, macKey, 0)
        val out = ByteArray(TAG_BYTES + message.size)
        stream.processBytes(message, 0, message.size, out, TAG_BYTES)
        tag(macKey, out, TAG_BYTES, message.size).copyInto(out, 0)
        macKey.fill(0)
        return out
    }

    /** NaCl crypto_secretbox_open, as tweetnacl's nacl.secretbox.open. */
    fun secretboxOpen(box: ByteArray, nonce: ByteArray, key: ByteArray): ByteArray? {
        if (box.size < TAG_BYTES) return null
        val stream = stream(nonce, key)
        val macKey = ByteArray(32)
        stream.processBytes(ByteArray(32), 0, 32, macKey, 0)
        val expected = tag(macKey, box, TAG_BYTES, box.size - TAG_BYTES)
        macKey.fill(0)
        if (!Bc.constantTimeAreEqual(TAG_BYTES, expected, 0, box, 0)) return null
        val plain = ByteArray(box.size - TAG_BYTES)
        stream.processBytes(box, TAG_BYTES, plain.size, plain, 0)
        return plain
    }

    private fun stream(nonce: ByteArray, key: ByteArray): XSalsa20Engine {
        require(key.size == KEY_BYTES) { "a gray key is 32 bytes" }
        require(nonce.size == NONCE_BYTES) { "a gray nonce is 24 bytes" }
        return XSalsa20Engine().apply { init(true, ParametersWithIV(KeyParameter(key), nonce)) }
    }

    private fun tag(macKey: ByteArray, data: ByteArray, offset: Int, length: Int): ByteArray {
        val mac = Poly1305()
        mac.init(KeyParameter(macKey))
        mac.update(data, offset, length)
        return ByteArray(TAG_BYTES).also { mac.doFinal(it, 0) }
    }

    // UTF-8 as the browser's TextEncoder (and so tweetnacl and hash-wasm) makes
    // it: an unpaired surrogate becomes U+FFFD, where Java's own would write '?'.
    private fun utf8(text: String): ByteArray {
        val encoder = Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .replaceWith(byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte()))
        val buffer: ByteBuffer = encoder.encode(CharBuffer.wrap(text))
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
