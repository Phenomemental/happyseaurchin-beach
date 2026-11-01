package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
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
        Mark(Modifier.size(96.dp).testTag("mark"))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("Rockpool")
            Text("About", style = Type.title, color = ink.ink)
            Stroke()
        }
        Text(
            "Rockpool keeps pscale blocks on this phone and works on them with a bsp of its own, in the same geometry as the beach, " +
                "with no connection needed. What it does with your words is a block of yours too, your cook: change a step, and it does what the step says.",
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
                        model.say(Screen.About, "There's no browser on this phone to open $BEACH_URL.")
                    }
                },
                style = Type.body,
                tag = "thanks",
            )
        }
    }
}

/**
 * Rockpool's mark, Nine stones (spine:pocket-beach 1.1111787148): on a square
 * of ink with corners of 24 (of 108), a still pool, nine stones on a ring of
 * radius 29 about it, one at the top and every 40 degrees on, no two alike,
 * each turned to follow the ring, and a crimson point at the centre:
 * pscale's ring of nine round a centre, and P6's Presence at the middle.
 */
@Composable
fun Mark(modifier: Modifier = Modifier) {
    val w = listOf(4.4f, 3.8f, 5.0f, 4.1f, 3.6f, 4.7f, 3.9f, 4.5f, 4.2f)
    val h = listOf(3.2f, 2.9f, 3.6f, 3.0f, 2.8f, 3.4f, 3.1f, 3.5f, 2.95f)
    val stone = Color(0xFFECE4D6)
    Canvas(modifier) {
        val u = size.minDimension / 108f
        drawRoundRect(Color(0xFF16130F), size = Size(108 * u, 108 * u), cornerRadius = CornerRadius(24 * u))
        val c = Offset(54 * u, 54 * u)
        drawCircle(Color(0xFF1F2A2C), radius = 22 * u, center = c)
        drawCircle(stone.copy(alpha = 1f / 3), radius = 22 * u, center = c, style = DrawStroke(width = 0.5f * u))
        for (k in 0 until 9) {
            val th = Math.toRadians(-90.0 + 40 * k)
            val at = Offset(c.x + 29 * u * Math.cos(th).toFloat(), c.y + 29 * u * Math.sin(th).toFloat())
            rotate((Math.toDegrees(th) + 90).toFloat(), pivot = at) {
                drawOval(stone, topLeft = Offset(at.x - w[k] / 2 * u, at.y - h[k] / 2 * u), size = Size(w[k] * u, h[k] * u))
            }
        }
        drawCircle(Color(0xFFD9606E), radius = 3.6f * u, center = c)
    }
}
