package com.phenomemental.pocketbeach.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.phenomemental.pocketbeach.bsp.NoAnswer
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Wire
import com.phenomemental.pocketbeach.bsp.WireReply
import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/**
 * The app's one way out (decision 18): one POST of one message to a pool on
 * the beach, made only after Matthew has tapped to confirm it. It takes no
 * address but the beach's pool append, follows no redirect, keeps no cookie
 * or cache, and reads back at most a short answer. Plain HTTP is refused
 * everywhere. The model's one download (`ModelFetch.kt`) is the only other
 * code that opens a connection.
 *
 * Once the whole message has gone, a failure to hear back is [NoAnswer]: it
 * may have landed, and Matthew is told so.
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
            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw NoAnswer(e.message ?: e::class.java.simpleName, e)
            }
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

/**
 * Whether the phone has a connection that reaches the internet, as Android
 * says: told to [changed] now and whenever it changes, on the main thread.
 * Nothing is sent because it changes; it says only when "Post now" can be
 * tapped.
 */
object Connection {
    fun watch(context: Context, changed: (Boolean) -> Unit) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        fun up(caps: NetworkCapabilities?) = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        changed(up(connectivity.getNetworkCapabilities(connectivity.activeNetwork)))
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = changed(up(caps))
            override fun onLost(network: Network) = changed(false)
        }, Handler(Looper.getMainLooper()))
    }
}
