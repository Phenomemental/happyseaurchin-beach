package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.Unsent
import com.phenomemental.pocketbeach.bsp.Posted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Posts Matthew said yes to that the beach couldn't be reached for, each with
 * "Post now". It can be tapped only while the phone has a connection, and a
 * kept post goes only on that tap: coming back online sends nothing.
 * [onPosted] tells the screen it stands on when one lands.
 */
@Composable
fun Waiting(model: Model, onPosted: () -> Unit = {}) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    val kept = app.unsent.all
    var sending by remember { mutableStateOf(false) }
    var landed by remember { mutableStateOf<Posted.Landed?>(null) }
    var discarding by remember { mutableStateOf<Unsent.Kept?>(null) }

    landed?.let { Done("Posted to your pool at entry ${it.slot}.", detail = "pool:${it.pool} on beach.happyseaurchin.com", tag = "posted-later") }
    if (kept.isEmpty()) return
    Label("Waiting to be posted")
    kept.forEach { k ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("waiting")) {
            Sheet { SelectionContainer { Text(k.message.message, style = Type.body, color = ink.ink, modifier = Modifier.padding(vertical = 8.dp)) } }
            Text("Not posted ${Words.at(k.atMs)}. ${k.why}", style = Type.small, color = ink.soft)
            if (k.mayHaveLanded) Text(
                "It may be on the beach already. Look at pool:${k.message.pool} before posting it again, or it may land twice.",
                style = Type.small, color = ink.crimson, modifier = Modifier.testTag("may-have-landed"),
            )
            Text(
                if (app.online) "It goes to pool:${k.message.pool}, under the name ${k.message.author}, only when you tap Post now."
                else "Waiting for a connection. Post now can be tapped once the phone is back online; nothing goes by itself.",
                style = Type.small, color = if (app.online) ink.soft else ink.crimson, modifier = Modifier.testTag("waiting-why"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Action("Post now", {
                    sending = true
                    scope.launch {
                        val posted = withContext(Dispatchers.IO) { app.post(k.message) }
                        sending = false
                        if (posted is Posted.Landed) {
                            landed = posted
                            onPosted()
                        }
                    }
                }, enabled = app.online && !sending, strong = true, tag = "post-now")
                TextLink("Discard", { discarding = k }, tag = "discard-waiting")
            }
        }
    }

    discarding?.let { k ->
        PaperDialog(
            "Discard this post?",
            onDismiss = { discarding = null },
            actions = {
                Action("Keep it", { discarding = null })
                Action("Discard it", {
                    discarding = null
                    scope.launch { withContext(Dispatchers.IO) { app.unsent.drop(k.message) } }
                }, strong = true, tag = "discard-confirm")
            },
        ) {
            Text(k.message.message, style = Type.body, color = ink.ink)
            Text("It won't be posted, and isn't kept anywhere else. The writes it came with stay as they are.", style = Type.small, color = ink.crimson)
        }
    }
}
