package com.phenomemental.pocketbeach.net

import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Wire
import com.phenomemental.pocketbeach.bsp.WireReply
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/**
 * The app's one way out (decision 18), and the only code in it that opens a
 * connection: one POST of one message to a pool on the beach, made only after
 * Matthew has tapped to confirm it. It takes no address but the beach's pool
 * append, follows no redirect, keeps no cookie or cache, and reads back at
 * most a short answer. The network security config trusts no certificate for
 * any host but the beach, so nothing else could get out over TLS, and plain
 * HTTP is refused everywhere.
 *
 * The model's one download is made by Android's download manager on the
 * app's behalf, outside this process; nothing in the app reads from the
 * network.
 */
object HttpsWire : Wire {
    private const val PREFIX = PoolPost.BEACH + "/.well-known/pscale-beach?block=pool:"
    private const val MOST = 64 * 1024

    override fun post(url: String, body: String): WireReply {
        require(url.startsWith(PREFIX) && url.indexOf('&') < 0 && url.indexOf('#') < 0) { "The pocket posts only to a pool on the beach." }
        val connection = URI(url).toURL().openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            val bytes = body.toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status >= 400) connection.errorStream else connection.inputStream
            val text = stream?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (out.size() < MOST) {
                    val n = input.read(buffer, 0, minOf(buffer.size, MOST - out.size()))
                    if (n < 0) break
                    out.write(buffer, 0, n)
                }
                out.toString(Charsets.UTF_8.name())
            }.orEmpty()
            return WireReply(status, text)
        } finally {
            connection.disconnect()
        }
    }
}
