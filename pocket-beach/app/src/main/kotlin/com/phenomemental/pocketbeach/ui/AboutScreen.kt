package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.phenomemental.pocketbeach.bsp.Js
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.JsStr
import com.phenomemental.pocketbeach.bsp.collectUnderscore
import com.phenomemental.pocketbeach.bsp.jsGet

const val BEACH_URL = "https://beach.happyseaurchin.com"

/**
 * Sundial 0.10, About (spine:pocket-beach 1.1111787197): Sundial's About,
 * reformed for the app, read from words:sundial place 6 (the opening line,
 * its sections, its links); then Rockpool's own at the foot: the version,
 * the licences, who made it, and thanks to David. How the pocket works is
 * said only under Show the workings.
 */
@Composable
fun AboutScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val app = model.app
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    val about = remember(model.version) { jsGet(Words.sundialBlock(app), "6") }
    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            model.say(Screen.About, "There's no browser on this phone to open $url.")
        }
    }
    Page(onBack = { nav.back() }) {
        SundialMark(Modifier.size(96.dp).testTag("mark"))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("Sundial")
            Text("About", style = Type.title, color = ink.ink)
            Stroke()
        }
        textAt(about, "1")?.let { Text(it, style = Type.body, color = ink.ink, modifier = Modifier.testTag("about-opening")) }
        for (k in 2..8) {
            val part = jsGet(about, "$k") as? JsObj ?: continue
            AboutPart(part, "about-$k")
            // A part within a part: If you want more, beneath What it draws on; If you need help now, beneath What it is not.
            (part["2"] as? JsObj)?.let { AboutPart(it, "about-$k-2") }
        }
        val links = (jsGet(about, "9") as? JsObj)?.let { l -> (1..9).mapNotNull { textAt(l, "$it")?.split(" — ", limit = 2)?.takeIf { it.size == 2 } } }.orEmpty()
        val thanks = links.firstOrNull { it[1] == BEACH_URL }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("about-links")) {
            for ((words, url) in links.filter { it != thanks }.map { it[0] to it[1] }) TextLink(words, { open(url) }, tag = "about-link")
        }
        if (app.showDetails) {
            Rule()
            Text(
                "Sundial keeps pscale blocks on this phone and works on them with a bsp of its own, in the same geometry as the beach, " +
                    "with no connection needed. What it does with your words is a block of yours too, your cook: change a step, and it does what the step says.",
                style = Type.body, color = ink.ink, modifier = Modifier.testTag("about-workings"),
            )
            TextLink("Try a call by hand", { nav.go(Screen.Calls) }, tag = "calls")
        }
        Rule(Modifier.padding(top = 8.dp))
        Text(
            "Version $version. Set in Cormorant Garamond by Christian Thalmann, under the SIL Open Font License. " +
                "The teaching blocks are bsp-mcp-server's own, under the MIT licence. Towns from GeoNames (geonames.org), under CC BY 4.0.",
            style = Type.small, color = ink.soft, modifier = Modifier.testTag("about-version"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Made by Matthew Bushell", style = Type.body, color = ink.ink)
            TextLink(thanks?.get(0) ?: "With thanks to David Pinto, who built the ground these pages stand on", { open(BEACH_URL) }, style = Type.body, tag = "thanks")
        }
    }
}

/** One of About's parts: its heading, small and spaced, and its words. */
@Composable
private fun AboutPart(part: JsObj, tag: String) {
    val ink = LocalInk.current
    val heading = (part["_"] as? JsStr)?.value?.substringBefore(",")?.trim() ?: return
    val words = textAt(part, "1") ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag(tag)) {
        Label(heading)
        Text(words, style = Type.body, color = ink.ink)
    }
}

private fun textAt(node: Js?, key: String): String? = when (val n = jsGet(node, key)) {
    is JsStr -> n.value
    null -> null
    else -> collectUnderscore(n)
}
