package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

const val BEACH_URL = "https://beach.happyseaurchin.com"

@Composable
fun AboutScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    Page(onBack = { nav.back() }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("Pocket beach")
            Text("About", style = Type.title, color = ink.ink)
            Stroke()
        }
        Text(
            "Pocket beach keeps pscale blocks on this phone and works on them with a bsp of its own, in the same geometry as the beach, " +
                "with no connection needed. It begins empty: every block in it is one you made.",
            style = Type.body, color = ink.ink,
        )
        TextLink("Try a call by hand", { nav.go(Screen.Calls) }, tag = "calls")
        Text(
            "Version $version. Set in Cormorant Garamond by Christian Thalmann, under the SIL Open Font License. " +
                "The teaching blocks are bsp-mcp-server's own, under the MIT licence.",
            style = Type.small, color = ink.soft,
        )
        Rule(Modifier.padding(top = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Made by Matthew Bushell", style = Type.body, color = ink.ink)
            TextLink(
                "With thanks to David Pinto, who built the ground these pages stand on",
                {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BEACH_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: ActivityNotFoundException) {
                        model.slip = "There's no browser on this phone to open $BEACH_URL."
                    }
                },
                style = Type.body,
                tag = "thanks",
            )
        }
    }
}
