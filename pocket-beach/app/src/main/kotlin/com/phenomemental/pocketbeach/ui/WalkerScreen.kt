package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Reach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Sundial 0.12, the walker (spine:pocket-beach 1.111178719995): a block
 * walked as the beach's bsp() walks it, point, walk or disc, its text printed
 * as the beach prints it. His own blocks always; with Reach the beach on, the
 * beach's blocks too, read over HTTPS, read only. A block of the beach's he
 * wants is copied into his pocket, whole, on his yes. Nothing is written to
 * the beach.
 */

/** A block read from the beach, held while he walks it. */
private class Read(val name: String, val block: JsObj)

@Composable
fun WalkerScreen(model: Model, nav: Nav, screen: Screen.Walker) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    var beach by rememberSaveable { mutableStateOf(screen.beach && app.reachBeach) }
    var name by rememberSaveable { mutableStateOf(screen.block) }
    var spindle by rememberSaveable { mutableStateOf("") }
    var pscale by rememberSaveable { mutableStateOf("") }
    var out by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    var read by remember { mutableStateOf<Read?>(null) }
    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var copying by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    /** One GET of the beach, refused before it is made while Reach the beach is off. */
    suspend fun get(url: String) = withContext(Dispatchers.IO) {
        check(app.reachBeach) { "Reach the beach is off." }
        app.reader.get(url)
    }

    fun walk() {
        val block = name.trim()
        if (block.isEmpty() || busy) return
        val attention = pscale.trim().takeIf { it.isNotEmpty() }?.let { it.toDoubleOrNull() ?: run { note = "The pscale is a number, like 0 or -1."; return } }
        note = null
        busy = true
        scope.launch {
            val text = runCatching {
                if (!beach) withContext(Dispatchers.IO) {
                    if (!app.pages.exists(block)) error("There's no block called $block in your pocket.")
                    Reach.walkPocket(app.pocket, block, spindle, attention)
                } else {
                    val held = read?.takeIf { it.name == block } ?: run {
                        val reply = get(Reach.blockUrl(block))
                        if (reply.status == 404) error("The beach has no block called $block.")
                        if (reply.status !in 200..299) error("The beach answered ${reply.status}.")
                        Read(block, Reach.blockOf(reply.text) ?: error("The beach's answer for $block isn't a block."))
                    }
                    read = held
                    withContext(Dispatchers.Default) { Reach.walk(app.shelf, block, held.block, spindle, attention) }
                }
            }
            busy = false
            text.onSuccess { out = it }.onFailure { out = null; note = it.message ?: "It couldn't be read." }
        }
    }

    Page(onBack = { nav.back() }, top = { Text("The walker", style = Type.name, color = ink.ink) }) {
        Text(
            "A block walked as the beach's bsp() walks it. With no address and no pscale, its disc; an address walks to it; a pscale picks the rung.",
            style = Type.small, color = ink.soft,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TextLink(if (!beach) "▪ Your pocket" else "Your pocket", { beach = false; out = null; note = null }, tag = "walk-pocket")
            TextLink(if (beach) "▪ The beach" else "The beach", {
                if (!app.reachBeach) note = "Reach the beach is off. Turn it on in Settings to walk the beach's blocks, read only."
                else { beach = true; out = null; note = null }
            }, tag = "walk-beach")
        }
        PaperField(name, { name = it; read = null }, if (beach) "A block of the beach, like spine:pocket-beach" else "One of your blocks", singleLine = true, tag = "walk-block-name")
        if (beach && names.isEmpty()) TextLink("List the beach's blocks", {
            scope.launch {
                runCatching { get(Reach.indexUrl()) }.onSuccess { r -> names = Reach.names(r.text); if (names.isEmpty()) note = "The beach's index couldn't be read." }
                    .onFailure { note = it.message ?: "The beach couldn't be reached." }
            }
        }, tag = "walk-list")
        if (beach && names.isNotEmpty()) {
            val q = name.trim()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (n in names.filter { q.isEmpty() || it.contains(q, ignoreCase = true) }.take(12)) Text(
                    n, style = Type.small, color = ink.crimson,
                    modifier = Modifier.fillMaxWidth().clickable { name = n; read = null }.padding(vertical = 4.dp).testTag("walk-name:$n"),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PaperField(spindle, { spindle = it }, "Address, like 1.2", Modifier.weight(1f), singleLine = true, tag = "walk-spindle")
            PaperField(pscale, { pscale = it }, "Pscale", Modifier.weight(0.6f), singleLine = true, tag = "walk-pscale")
        }
        Action(if (busy) "Walking…" else "Walk", { walk() }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && !busy, strong = true, tag = "walk-go")
        note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("walk-note")) }
        out?.let { text ->
            Sheet {
                SelectionContainer(Modifier.horizontalScroll(rememberScrollState())) {
                    Text(text, style = Type.call, color = ink.ink, modifier = Modifier.padding(vertical = 8.dp).testTag("walk-out"))
                }
            }
            val r = read
            if (beach && r != null) {
                if (!copying) TextLink("Copy this block into your pocket", { copying = true }, tag = "copy-block")
                else Column(Modifier.testTag("copy-card"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Copy ${r.name} from the beach into your pocket, whole, as a block of your own? Nothing is written to the beach.", style = Type.body, color = ink.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Action("Yes", {
                            scope.launch {
                                val done = withContext(Dispatchers.IO) {
                                    val plan = app.pages.planKeep(r.name, r.block)
                                    plan.refusal?.let { return@withContext it }
                                    val d = app.pages.carryOut(plan)
                                    if (d.ok) null else d.problem ?: "It didn't land as shown."
                                }
                                copying = false
                                if (done == null) {
                                    model.changed()
                                    note = "Copied into your pocket as ${r.name}."
                                } else note = "Nothing was copied: $done"
                            }
                        }, Modifier.weight(1f), strong = true, tag = "copy-yes")
                        Action("Not now", { copying = false }, Modifier.weight(1f), tag = "copy-not-now")
                    }
                }
            }
        }
    }
}
