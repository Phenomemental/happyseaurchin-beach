package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.Kind
import com.phenomemental.pocketbeach.bsp.Line
import com.phenomemental.pocketbeach.bsp.PositionView
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.stringify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where a position's back step leads: the line above it, or the block's opening. */
private fun PositionView.up(): String = trail.lastOrNull()?.address ?: ""

@Composable
fun BlockScreen(model: Model, nav: Nav, screen: Screen.Block) {
    val ink = LocalInk.current
    val view by produceState<PositionView?>(null, screen, model.version) {
        value = withContext(Dispatchers.IO) { model.pages.position(screen.region, screen.name, screen.address) }
    }
    fun standAt(address: String) = nav.replace(screen.copy(address = address))
    val v = view
    BackHandler(enabled = screen.address.isNotEmpty()) { standAt(v?.up() ?: "") }

    val mine = screen.region == Region.POCKET
    Page(
        onBack = { if (screen.address.isEmpty()) nav.back() else standAt(v?.up() ?: "") },
        top = { Trail(screen.name, v) { standAt(it) } },
        bottom = if (!mine || v == null) null else ({
            Action(if (v.text == null) "Write this line" else "Change this line", {
                nav.go(Screen.Write(Kind.CHANGE, screen.name, screen.address))
            }, Modifier.weight(1f), tag = "change")
            Action("Add beneath", { nav.go(Screen.Write(Kind.ADD, screen.name, screen.address)) }, Modifier.weight(1f),
                enabled = !(v.full && screen.address.isNotEmpty()), tag = "add")
        }),
    ) {
        if (v == null) return@Page
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label(
                when {
                    screen.address.isEmpty() && mine -> "Your block"
                    screen.address.isEmpty() -> "Teaching block, read only"
                    else -> listOfNotNull("at ${v.address}", Words.at(v.stamp)?.let { "added $it" }).joinToString(" · ")
                },
            )
            val text = v.text
            Text(
                text ?: "Nothing is written here yet.",
                style = when {
                    text == null -> Type.aside
                    text.length <= 90 -> Type.heading
                    else -> Type.body
                },
                color = if (text == null) ink.soft else ink.ink,
                modifier = Modifier.testTag("standing"),
            )
            Stroke()
        }

        if (screen.address.isEmpty()) {
            if (v.wrapped == 1) Notice(
                "This block has wrapped. When its opening held nine lines, the tenth moved those nine one step beneath the opening line, " +
                    "to 01 to 09, and began the next nine at 11. Its lines gather in nines from here.",
                tag = "wrapped",
            )
            if (v.wrapped > 1) Notice(
                "This block has wrapped ${Words.number(v.wrapped)} times. Each time its opening was full, everything moved one step " +
                    "beneath the opening line and a new set began.",
                tag = "wrapped",
            )
            if (v.full && mine) Notice(
                if (v.wrapped == 0) "Nine lines at the opening. The next one you add wraps the block."
                else "The opening is full. The next line you add wraps the block again.",
                tag = "full",
            )
            if (mine) v.owed.forEach { owed ->
                Notice(
                    "A summary is owed at ${owed.at}: one line there that sums up ${Words.span(owed.over)}.",
                    link = "Write it" to { nav.go(Screen.Write(Kind.CHANGE, screen.name, owed.at)) },
                    tag = "owed",
                )
            }
        } else if (v.full) {
            Notice("Every place beneath this line is taken, so nothing more can be added here. Only a block's opening line wraps past nine.", tag = "full")
        }

        Sheet(Modifier.testTag("lines")) {
            if (v.lines.isEmpty()) {
                Text(
                    if (screen.address.isEmpty()) "No lines yet." else "Nothing beneath this line yet.",
                    style = Type.aside, color = ink.soft, modifier = Modifier.padding(vertical = 10.dp),
                )
            }
            var group: String? = null
            v.lines.forEach { line ->
                val g = line.address.dropLast(1)
                if (group != null && g != group) Break()
                group = g
                LineRow(line, highlighted = model.landed == screen.name to line.address) { standAt(line.address) }
            }
        }

        if (screen.address.isEmpty()) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            fun whole(): String? = when (screen.region) {
                Region.POCKET -> model.app.storage.read(screen.name)
                Region.TEACHING -> model.app.shelf.load(screen.name)?.let { stringify(it) }
            }
            // The whole block as a file, where Matthew picks, to keep or attach: a share copied from the menu came through cut short (0.4.1's bench).
            val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            val text = whole() ?: error("it isn't there any more")
                            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        }
                    }
                    outcome.onSuccess { model.say(screen, "Saved ${screen.name} as a file, whole.") }
                        .onFailure { model.say(screen, "${screen.name} wasn't saved: ${it.message ?: "the file couldn't be written"}.") }
                }
            }
            TextLink("Share this block", {
                val text = whole() ?: return@TextLink
                // Decision 19: out through Android's share menu, by Matthew's tap, to whichever app he picks.
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .putExtra(Intent.EXTRA_SUBJECT, screen.name)
                    .putExtra(Intent.EXTRA_TITLE, screen.name)
                try {
                    context.startActivity(Intent.createChooser(send, "Share ${screen.name}"))
                } catch (_: ActivityNotFoundException) {
                    model.say(screen, "There's no app on this phone to share it with.")
                }
            }, tag = "share")
            TextLink("Save this block as a file", { saveTo.launch("${screen.name}.json") }, tag = "save-file")
        }
    }

    LaunchedEffect(model.landed) {
        if (model.landed != null) {
            delay(2500)
            model.landed = null
        }
    }
}

/** The way back up: the block's name, then each line above where you stand. */
@Composable
private fun Trail(name: String, view: PositionView?, standAt: (String) -> Unit) {
    val ink = LocalInk.current
    val atRoot = view == null || view.address.isEmpty()
    Text(name, style = Type.name, color = if (atRoot) ink.ink else ink.crimson, maxLines = 1,
        modifier = Modifier.clickable(enabled = !atRoot) { standAt("") }.padding(vertical = 8.dp).testTag("trail:"))
    view?.trail?.forEach { step ->
        Text("  ›  ", style = Type.small, color = ink.soft)
        Text(step.text?.let { if (it.length > 24) it.take(23).trimEnd() + "…" else it } ?: step.address, style = Type.small,
            color = ink.crimson, maxLines = 1, modifier = Modifier.clickable { standAt(step.address) }.padding(vertical = 8.dp).testTag("trail:${step.address}"))
    }
}

@Composable
private fun LineRow(line: Line, highlighted: Boolean, onOpen: () -> Unit) {
    val ink = LocalInk.current
    Row(
        Modifier.fillMaxWidth().background(if (highlighted) ink.wash else ink.sheet).clickable(onClick = onOpen)
            .padding(vertical = 10.dp).testTag("line:${line.address}"),
        verticalAlignment = Alignment.Top,
    ) {
        Text(line.address, style = Type.address, color = ink.soft, modifier = Modifier.width(52.dp))
        Column(Modifier.weight(1f)) {
            Text(line.text ?: "Nothing written here.", style = if (line.text == null) Type.aside else Type.body,
                color = if (line.text == null) ink.soft else ink.ink)
            if (line.beneath > 0) Text("${Words.number(line.beneath)} beneath", style = Type.small, color = ink.soft)
        }
        Spacer(Modifier.width(8.dp))
        Text("›", style = Type.body, color = ink.soft)
    }
}

/** A break between nines, as a book marks a break between sections. */
@Composable
private fun Break() {
    Text("·   ·   ·", style = Type.small, color = LocalInk.current.soft, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), maxLines = 1, overflow = TextOverflow.Clip)
}
