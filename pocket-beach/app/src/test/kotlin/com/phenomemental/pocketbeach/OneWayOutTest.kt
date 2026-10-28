package com.phenomemental.pocketbeach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Decision 18: one narrow way out. Only net/HttpsWire.kt names a network
 * API, only model/ModelStore.kt hands Android's download manager a download,
 * and the network security config trusts no host but the beach.
 */
class OneWayOutTest {
    private val main = File("src/main")
    private val sources = main.walkTopDown().filter { it.isFile && it.extension in setOf("kt", "java") }.toList()

    private val network = Regex(
        """\bjava\.net\b|\bjavax\.net\b|HttpURLConnection|HttpsURLConnection|\bSocket\b|SocketChannel|DatagramSocket|okhttp|\bktor\b|""" +
            """WebView|\bURL\(|toURL\(|ConnectivityManager|\bVolley\b|\bRetrofit\b|grpc|WebSocket|HttpClient""",
    )

    @Test
    fun onlyTheWireNamesANetworkApi() {
        val naming = sources.filter { f -> f.readLines().any { line -> !line.trimStart().startsWith("*") && !line.trimStart().startsWith("//") && network.containsMatchIn(line) } }
        assertEquals(listOf("HttpsWire.kt"), naming.map { it.name })
    }

    @Test
    fun onlyTheModelStoreDownloads() {
        val naming = sources.filter { it.readText().contains("DownloadManager") }
        assertEquals(listOf("ModelStore.kt"), naming.map { it.name })
        val store = File(main, "kotlin/com/phenomemental/pocketbeach/model/ModelStore.kt").readText()
        assertTrue("one pinned file", store.contains("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/"))
        assertTrue("over Wi-Fi only", store.contains("setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)"))
        assertEquals(1, Regex("""\.enqueue\(""").findAll(store).count())
    }

    @Test
    fun theWireGoesOnlyToThePool() {
        val wire = File(main, "kotlin/com/phenomemental/pocketbeach/net/HttpsWire.kt").readText()
        assertTrue(wire.contains("PoolPost.BEACH + \"/.well-known/pscale-beach?block=pool:\""))
        assertTrue(wire.contains("require(url.startsWith(PREFIX)"))
        assertTrue(wire.contains("instanceFollowRedirects = false"))
        assertEquals("one connection opened", 1, Regex("""openConnection\(""").findAll(wire).count())
    }

    @Test
    fun noHostIsTrustedButTheBeach() {
        val config = File(main, "res/xml/network_security_config.xml").readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        assertTrue(Regex("""<base-config cleartextTrafficPermitted="false">\s*<trust-anchors\s*/>\s*</base-config>""").containsMatchIn(config))
        assertEquals(listOf("beach.happyseaurchin.com"), Regex("""<domain[^>]*>([^<]+)</domain>""").findAll(config).map { it.groupValues[1].trim() }.toList())
        assertTrue(config.contains("""<domain includeSubdomains="false">"""))
        assertTrue(!config.contains("cleartextTrafficPermitted=\"true\""))
        val manifest = File(main, "AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:networkSecurityConfig="@xml/network_security_config""""))
        assertTrue(!manifest.contains("usesCleartextTraffic=\"true\""))
    }
}
