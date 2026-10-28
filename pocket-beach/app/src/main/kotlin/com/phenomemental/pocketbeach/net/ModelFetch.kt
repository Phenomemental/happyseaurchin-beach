package com.phenomemental.pocketbeach.net

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/**
 * The model's one download, made by the app itself: the second and last
 * piece of network code beside [HttpsWire], and only for the pinned model
 * file. It starts at Hugging Face's address for that file and follows its
 * redirects one by one, each only to a host in [hosts] and only over HTTPS;
 * the network security config trusts no others. Whatever has come is kept in
 * a part file, and an interrupted download carries on from there with a
 * range request. A broken connection is tried again a few times, waiting a
 * little longer each time; anything else stops it with the real reason.
 * The byte-for-byte check against the model's SHA-256 is made after, by the
 * model store.
 */
class ModelFetch(
    private val start: String,
    private val hosts: Set<String>,
    private val size: Long,
    private val open: Opener = HttpsOpener,
    /** Why not to fetch over the connection the phone has now (a metered one), or null to go ahead. */
    private val connection: () -> String? = { null },
    private val pause: (Long) -> Unit = Thread::sleep,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_TRIES = 6
        private const val CHUNK = 1 shl 20

        fun gb(bytes: Long) = "%.2f GB".format(bytes / 1e9)
    }

    /** One answer from a host: its status, its headers (names in lower case), and the body to read. */
    class Hop(val status: Int, val headers: Map<String, String>, val body: InputStream?, val close: () -> Unit)

    /** Opens [url] with GET, asking for the bytes from [from] on when [from] is more than 0. Failing to connect throws. */
    fun interface Opener {
        fun open(url: String, from: Long): Hop
    }

    /** How far it has come: [note] says what it is doing when that isn't plain fetching. */
    class Progress(val done: Long, val total: Long, val bytesPerSecond: Long, val note: String?)

    sealed class Outcome {
        /** Every byte has come; the part file holds the whole file. */
        data object Complete : Outcome()

        /** Stopped because it was asked to, with [done] bytes kept. */
        class Stopped(val done: Long) : Outcome()

        /** Stopped by what [why] says, with [done] bytes kept to carry on from. */
        class Failed(val why: String, val done: Long) : Outcome()
    }

    /** Not worth trying again: the reason is plain, and the download stops there. */
    private class Refused(why: String) : Exception(why)

    /** Fetch the file into [part], from wherever it has got to. Blocking, on a thread of its own. */
    fun run(part: File, report: (Progress) -> Unit, stop: () -> Boolean): Outcome {
        var tries = 0
        while (true) {
            if (stop()) return Outcome.Stopped(part.length())
            val had = part.length()
            if (had == size) return Outcome.Complete
            if (had > size) part.delete()
            connection()?.let { return Outcome.Failed(it, part.length()) }
            try {
                once(part, report, stop)
                if (stop()) return Outcome.Stopped(part.length())
                if (part.length() == size) return Outcome.Complete
                throw IOException("the connection ended at ${gb(part.length())} of ${gb(size)}")
            } catch (e: Refused) {
                return Outcome.Failed(e.message ?: "It stopped.", part.length())
            } catch (e: IOException) {
                if (part.length() > had) tries = 0
                tries++
                val why = e.message ?: e::class.java.simpleName
                if (tries >= MAX_TRIES) {
                    return Outcome.Failed("The connection broke $tries times without getting further. The last time: $why.", part.length())
                }
                val wait = minOf(30_000L, 2_000L shl (tries - 1))
                report(Progress(part.length(), size, 0, "The connection broke ($why). Trying again in ${wait / 1000} s, from ${gb(part.length())}."))
                var waited = 0L
                while (waited < wait && !stop()) {
                    pause(500)
                    waited += 500
                }
            }
        }
    }

    /** One pass: follow the redirects to the file, then take what comes into [part]. */
    private fun once(part: File, report: (Progress) -> Unit, stop: () -> Boolean) {
        val from = part.length()
        var url = start
        repeat(MAX_REDIRECTS + 1) {
            val uri = try { URI(url) } catch (e: Exception) { throw Refused("Hugging Face gave an address the pocket can't read: $url") }
            if (uri.scheme != "https") throw Refused("Hugging Face sent the download to an address that isn't HTTPS, so it stopped there.")
            if (uri.host !in hosts) throw Refused("Hugging Face sent the download on to ${uri.host}, a host the pocket doesn't trust, so it stopped there.")
            val hop = open.open(url, from)
            try {
                when (hop.status) {
                    301, 302, 303, 307, 308 -> {
                        val next = hop.headers["location"] ?: throw Refused("${uri.host} redirected the download without saying where.")
                        url = uri.resolve(next).toString()
                    }
                    200 -> {
                        hop.headers["content-length"]?.toLongOrNull()?.let { if (it != size) throw Refused("${uri.host} says the file is $it bytes, not the $size the pocket expects.") }
                        take(hop, part, append = false, report, stop)
                        return
                    }
                    206 -> {
                        val range = Regex("""bytes (\d+)-(\d+)/(\d+|\*)""").find(hop.headers["content-range"].orEmpty())
                            ?: throw Refused("${uri.host} sent part of the file without saying which part.")
                        val (first, _, total) = range.destructured
                        if (first.toLong() != from) throw IOException("${uri.host} sent the file from byte $first, not $from")
                        if (total != "*" && total.toLong() != size) throw Refused("${uri.host} says the file is $total bytes, not the $size the pocket expects.")
                        take(hop, part, append = true, report, stop)
                        return
                    }
                    416 -> {
                        // What is kept can't be carried on from: begin again.
                        part.delete()
                        throw IOException("${uri.host} couldn't carry on from ${gb(from)}, so it begins again")
                    }
                    408, 429, in 500..599 -> throw IOException("${uri.host} answered HTTP ${hop.status}${said(hop)}")
                    else -> throw Refused("${uri.host} answered HTTP ${hop.status}${said(hop)}")
                }
            } finally {
                hop.close()
            }
        }
        throw Refused("Hugging Face sent the download through more than $MAX_REDIRECTS redirects, so it stopped.")
    }

    /** The first words of an answer's body, to say why. */
    private fun said(hop: Hop): String {
        val text = runCatching { hop.body?.use { it.readNBytesCompat(300).toString(Charsets.UTF_8) } }.getOrNull()?.trim()?.replace(Regex("\\s+"), " ")
        return if (text.isNullOrEmpty()) "" else ": $text"
    }

    private fun take(hop: Hop, part: File, append: Boolean, report: (Progress) -> Unit, stop: () -> Boolean) {
        val body = hop.body ?: throw IOException("the answer had no body")
        part.parentFile?.mkdirs()
        FileOutputStream(part, append).use { out ->
            val buffer = ByteArray(CHUNK)
            var done = if (append) part.length() else 0L
            var lastReport = 0L
            var windowStart = clock()
            var windowBytes = 0L
            var rate = 0L
            while (!stop()) {
                val n = body.read(buffer)
                if (n < 0) break
                if (done + n > size) throw Refused("More came than the file's $size bytes, so it stopped.")
                out.write(buffer, 0, n)
                done += n
                windowBytes += n
                val now = clock()
                if (now - windowStart >= 1000) {
                    rate = windowBytes * 1000 / (now - windowStart)
                    windowStart = now
                    windowBytes = 0
                }
                if (now - lastReport >= 500) {
                    lastReport = now
                    report(Progress(done, size, rate, null))
                }
            }
            out.fd.sync()
        }
    }
}

private fun InputStream.readNBytesCompat(most: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (out.size() < most) {
        val n = read(buffer, 0, minOf(buffer.size, most - out.size()))
        if (n < 0) break
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}

/** Wi-Fi only: a reason not to fetch over a metered connection (mobile data), or null on an unmetered one. */
class Unmetered(private val context: android.content.Context) : () -> String? {
    override fun invoke(): String? {
        val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            ?: return "The phone has no connection. Connect to Wi-Fi, then carry on."
        return if (caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) null
        else "The phone is on a metered connection, such as mobile data. The model is fetched on Wi-Fi only; connect to Wi-Fi, then carry on."
    }
}

/** The real connection: HTTPS only, no redirect followed by itself, nothing cached. */
object HttpsOpener : ModelFetch.Opener {
    override fun open(url: String, from: Long): ModelFetch.Hop {
        val connection = URI(url).toURL().openConnection() as HttpsURLConnection
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty("User-Agent", "PocketBeach")
        if (from > 0) connection.setRequestProperty("Range", "bytes=$from-")
        val status = connection.responseCode
        val headers = connection.headerFields.entries.filter { it.key != null }.associate { it.key.lowercase() to (it.value.lastOrNull() ?: "") }
        val body = runCatching { if (status >= 400) connection.errorStream else connection.inputStream }.getOrNull()
        return ModelFetch.Hop(status, headers, body) { connection.disconnect() }
    }
}
