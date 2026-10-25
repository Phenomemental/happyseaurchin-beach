package life.matthewbushell.tideline.beach

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/*
 * The phone's calls to the beach, the same ones the Pscale tool makes
 * (bsp-mcp src/pscale-wire.ts; spine:tideline 7.14), over the beach's own
 * endpoint and nothing else:
 *
 *   read    GET  <beach>/.well-known/pscale-beach?block=<name>&spindle=<address>
 *   append  POST <beach>/.well-known/pscale-beach?block=<name>
 *           { "append": true, "spindle": <node>, "content": <entry>, "secret": <latch> }
 *
 * With a spindle the beach lands the entry in the next free slot beneath that
 * node and answers with the landed address. It stamps an object entry with
 * its arrival time at position 3.
 *
 * Plain Kotlin on Java's own HTTP client, so the phone and any computer run
 * the same calls. org.json is Android's own; elsewhere the build supplies it.
 */
class Beach(val origin: String = ORIGIN, private val userAgent: String = "Tideline") {
    companion object {
        const val ORIGIN = "https://beach.happyseaurchin.com"
    }

    /** What the beach answered: the landed address, or why it refused. */
    sealed class Answer {
        data class Landed(val address: String) : Answer()
        /** The beach said no (a lock, a full node, a bad shape): retrying will not help. */
        data class Refused(val status: Int, val code: String?, val reason: String) : Answer()
        /** The beach could not be reached, or was briefly unwell: try again later. */
        data class Unreached(val reason: String) : Answer()
    }

    /** Append [entry] (a JSON object) beneath [node] of [block]. */
    fun append(block: String, node: String, entry: JSONObject, secret: String?): Answer {
        val body = JSONObject()
            .put("append", true)
            .put("spindle", node)
            .put("content", entry)
        if (secret != null) body.put("secret", secret)
        return post(block, body)
    }

    /** Append [entry] at the root of [block], as a pool takes its notes. */
    fun appendAtRoot(block: String, entry: JSONObject, secret: String?): Answer {
        val body = JSONObject().put("append", true).put("content", entry)
        if (secret != null) body.put("secret", secret)
        return post(block, body)
    }

    /** The JSON at [spindle] of [block], or null when there is nothing there. */
    fun read(block: String, spindle: String?): String? {
        val query = "block=" + enc(block) + (if (spindle.isNullOrEmpty()) "" else "&spindle=" + enc(spindle))
        val c = open(query)
        try {
            c.requestMethod = "GET"
            val status = c.responseCode
            if (status == 404) return null
            if (status >= 400) throw IOException("the beach answered $status")
            return c.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
        } finally {
            c.disconnect()
        }
    }

    private fun post(block: String, body: JSONObject): Answer {
        val c = try {
            open("block=" + enc(block))
        } catch (e: IOException) {
            return Answer.Unreached(e.javaClass.simpleName)
        }
        try {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = c.responseCode
            val text = (if (status >= 400) c.errorStream else c.inputStream)?.use { String(it.readBytes(), Charsets.UTF_8) } ?: ""
            val json = try { JSONObject(text) } catch (e: Exception) { null }
            if (status in 200..299) {
                val address = json?.optString("address", "")?.takeIf { it.isNotEmpty() } ?: json?.optString("slot", "") ?: ""
                return Answer.Landed(address)
            }
            val reason = json?.optString("error", "")?.takeIf { it.isNotEmpty() } ?: "the beach answered $status"
            val code = json?.optString("code", "")?.takeIf { it.isNotEmpty() }
            // 429 (a cap for the minute) and 5xx pass; anything else is the beach's answer.
            return if (status == 429 || status >= 500) Answer.Unreached(reason) else Answer.Refused(status, code, reason)
        } catch (e: IOException) {
            // Only the kind of failure travels: the request carried a secret
            // and sealed text, and neither belongs in a message about it.
            return Answer.Unreached(e.javaClass.simpleName)
        } finally {
            c.disconnect()
        }
    }

    private fun open(query: String): HttpURLConnection {
        val c = URI("$origin/.well-known/pscale-beach?$query").toURL().openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.useCaches = false
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("User-Agent", userAgent)
        return c
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
