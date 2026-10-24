package life.matthewbushell.tideline.seal

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
 * The port checked against bsp-mcp's own src/keys.ts. Every expected value
 * here was made by that file, unchanged, with its pinned tweetnacl 1.0.3 and
 * hash-wasm 4.12.0 (24 October 2026, the source at bsp-mcp-server 8884f2c).
 * The word is a test word for these checks and opens nothing on the beach.
 */
class GrayTest {
    private val word = "tideline-test-word"
    private val beachKey by lazy { Gray.key(word) }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    private fun unb64(text: String) = Base64.getDecoder().decode(text)

    @Test
    fun `the key is the one keys_ts derives`() {
        assertEquals("ee350d9a172a77e4ffd9bf3d0b8f1a65dd6f06befeb5ec3188bd99b91a3e7f9e", hex(beachKey))
        assertEquals("ee350d9a172a77e4ffd9bf3d0b8f1a65dd6f06befeb5ec3188bd99b91a3e7f9e", hex(Gray.key(word, Gray.BEACH)))
    }

    @Test
    fun `a short salt is padded with noughts to eight, as keys_ts pads it`() {
        assertEquals("37381230664e45b7a2330fc67c9131832bd347476f91bbc7ebe261f8c724eb9e", hex(Gray.key(word, "Matt")))
    }

    @Test
    fun `a word and salt beyond ascii make the same key`() {
        assertEquals("cd21850bb02de32a38514b3b9f6050e78332b26eb4874305f8fcf4bbeca8e58e", hex(Gray.key("é🌊 word", "Phenomemental")))
    }

    @Test
    fun `secretbox matches tweetnacl byte for byte`() {
        val nonce = ByteArray(24) { it.toByte() }
        val cases = listOf(
            "" to "gil1E5QLDlx3UKCQwlw7Nw==",
            "A line for the seal." to "wvHU4214aSWGSNQ2NWYknWMBH5scKpQt3npZh/BhwBZ+SYMh",
            "Ça va? 🌊 — tideline\nsecond line" to "xCFNLj4YqgCQUJO9B0WpUeGmEtIELotrQZf1ebjmYPE7XIZr9FAtlCOGaU5Z1VUCQYIQwgI=",
        )
        for ((text, box) in cases) {
            assertEquals(box, Base64.getEncoder().encodeToString(Gray.secretbox(text.toByteArray(Charsets.UTF_8), nonce, beachKey)), text)
            assertEquals(text, String(Gray.secretboxOpen(unb64(box), nonce, beachKey)!!, Charsets.UTF_8))
        }
    }

    @Test
    fun `an unpaired surrogate is sealed as the browser encodes it`() {
        // keys.ts seals what TextEncoder makes of the text: U+FFFD for a lone half.
        val nonce = ByteArray(24) { it.toByte() }
        val envelope = Gray.Envelope("r1KE4N5dlZrzRZy6+uYxT0POzE8Q", Base64.getEncoder().encodeToString(nonce))
        assertEquals("a�b", Gray.open(envelope, beachKey))
    }

    @Test
    fun `lines sealed by keys_ts selfEncrypt open here`() {
        val sealed = listOf(
            Triple("Sealed by keys.ts selfEncrypt.", "DDfWRmpuuH9j37UHc6T4aB2qyXGXylL3esopK0B9QsoT5aIYYlRN/R38AB7ysg==", "+k5qgP+FCKjTm/l6YLgs0IqQyqnhbe2o"),
            Triple("Ça va? 🌊 — tideline", "3pW2g16q+RX5S3v36qJSXgflAKudSDcjMRUbBKteVH5ERZFoFq+YofU=", "IavxvI9o2hooqbyS4ULG9mSU3YSEHWd1"),
        )
        for ((text, ciphertext, nonce) in sealed) {
            assertEquals(text, Gray.open(Gray.Envelope(ciphertext, nonce), beachKey))
        }
    }

    @Test
    fun `a line sealed here opens here, and only with its own key and salt`() {
        val line = "Ça va? 🌊 — a line for the seal."
        val envelope = Gray.seal(line, beachKey)
        assertEquals(line, Gray.open(envelope, beachKey))
        assertEquals(24, unb64(envelope.nonce).size)
        assertNull(Gray.open(envelope, Gray.key("another-word")))
        assertNull(Gray.open(envelope, Gray.key(word, "Phenomemental")))
        assertNull(Gray.open(envelope, Gray.key(word, Gray.BEACH + "/")))
    }

    @Test
    fun `the same line sealed twice gives two envelopes`() {
        val a = Gray.seal("the same line", beachKey)
        val b = Gray.seal("the same line", beachKey)
        assertNotEquals(a.nonce, b.nonce)
        assertNotEquals(a.ciphertext, b.ciphertext)
    }

    @Test
    fun `a changed envelope does not open`() {
        val envelope = Gray.seal("a line for the seal", beachKey)
        val box = unb64(envelope.ciphertext)
        for (i in box.indices) {
            val changed = box.copyOf().also { it[i] = (it[i].toInt() xor 1).toByte() }
            assertNull(Gray.open(Gray.Envelope(Base64.getEncoder().encodeToString(changed), envelope.nonce), beachKey))
        }
        assertNull(Gray.open(Gray.Envelope(envelope.ciphertext, Gray.seal("x", beachKey).nonce), beachKey))
        assertNull(Gray.open(Gray.Envelope("not base64!", envelope.nonce), beachKey))
        assertNull(Gray.open(Gray.Envelope("", envelope.nonce), beachKey))
        assertNull(Gray.open(Gray.Envelope(envelope.ciphertext, envelope.nonce, "grain"), beachKey))
    }

    @Test
    fun `the envelope has the beach's gray shape and none of the line`() {
        val json = Gray.seal("Lizards on the wall", beachKey).toJson()
        assertTrue(json.startsWith("{\"_\":\"Encrypted (gray); readable only with the author secret.\",\"1\":\""))
        assertTrue(json.endsWith("\"9\":{\"_\":\"gray\",\"1\":\"self\"}}"))
        assertTrue("Lizards" !in json)
    }
}
