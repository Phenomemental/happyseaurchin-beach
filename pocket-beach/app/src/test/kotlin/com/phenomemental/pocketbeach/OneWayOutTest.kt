package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.model.ModelStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Decision 18 and the model's download: two narrow ways out and nothing else.
 * Only net/HttpsWire.kt (the one post, and whether the phone is connected, so
 * a kept post's "Post now" waits for a connection) and net/ModelFetch.kt (the
 * model's one download) name a network API; nothing hands Android's download
 * manager anything; and the network security config trusts the beach and the
 * download's hosts, and no others.
 */
class OneWayOutTest {
    private val main = File("src/main")
    private val sources = main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.toList()

    private val network = Regex(
        """\bjava\.net\b|\bjavax\.net\b|HttpURLConnection|HttpsURLConnection|\bSocket\b|SocketChannel|DatagramSocket|okhttp|\bktor\b|""" +
            """WebView|\bURL\(|toURL\(|ConnectivityManager|\bVolley\b|\bRetrofit\b|grpc|WebSocket|HttpClient|DownloadManager""",
    )

    private fun read(path: String) = File(main, "kotlin/com/phenomemental/pocketbeach/$path").readText()

    @Test
    fun onlyThePostAndTheDownloadNameANetworkApi() {
        val naming = sources.filter { f -> f.readLines().any { line -> !line.trimStart().startsWith("*") && !line.trimStart().startsWith("//") && network.containsMatchIn(line) } }
        assertEquals(listOf("HttpsWire.kt", "ModelFetch.kt"), naming.map { it.name }.sorted())
    }

    @Test
    fun theDownloadIsTheOnePinnedFileFromTrustedHostsOnly() {
        val users = sources.filter { it.name != "ModelFetch.kt" && it.readText().contains("ModelFetch(") }
        assertEquals("only the store starts the download", listOf("ModelStore.kt"), users.map { it.name })
        val store = read("model/ModelStore.kt")
        assertTrue("one pinned file", ModelStore.URL.startsWith("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/"))
        assertTrue("from the pinned address, through the trusted hosts", store.contains("ModelFetch(URL, HOSTS, SIZE"))
        assertTrue("over Wi-Fi only", store.contains("connection = Unmetered(context)"))
        val fetch = read("net/ModelFetch.kt")
        assertTrue(fetch.contains("instanceFollowRedirects = false"))
        assertTrue(fetch.contains("if (uri.host !in hosts) throw Refused("))
        assertTrue(fetch.contains("""if (uri.scheme != "https") throw Refused("""))
        assertEquals("one connection opened", 1, Regex("""openConnection\(""").findAll(fetch).count())
    }

    @Test
    fun theWireGoesOnlyToThePool() {
        val wire = read("net/HttpsWire.kt")
        assertTrue(wire.contains("PoolPost.BEACH + \"/.well-known/pscale-beach?block=pool:\""))
        assertTrue(wire.contains("require(url.startsWith(PREFIX)"))
        assertTrue(wire.contains("instanceFollowRedirects = false"))
        assertEquals("one connection opened", 1, Regex("""openConnection\(""").findAll(wire).count())
    }

    @Test
    fun noHostIsTrustedButTheBeachAndTheDownloads() {
        val config = File(main, "res/xml/network_security_config.xml").readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        assertTrue(Regex("""<base-config cleartextTrafficPermitted="false">\s*<trust-anchors\s*/>\s*</base-config>""").containsMatchIn(config))
        val trusted = Regex("""<domain[^>]*>([^<]+)</domain>""").findAll(config).map { it.groupValues[1].trim() }.toSet()
        assertEquals(setOf("beach.happyseaurchin.com") + ModelStore.HOSTS, trusted)
        assertEquals("no subdomains, anywhere", trusted.size, Regex("""<domain includeSubdomains="false">""").findAll(config).count())
        assertTrue(!config.contains("cleartextTrafficPermitted=\"true\""))
        val manifest = File(main, "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:networkSecurityConfig="@xml/network_security_config""""))
        assertTrue(!manifest.contains("usesCleartextTraffic=\"true\""))
    }
}
