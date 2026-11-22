package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.Chats
import com.phenomemental.pocketbeach.bsp.OutlineLine
import com.phenomemental.pocketbeach.bsp.Plan
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Seen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Sundial 0.12, the editor (spine:pocket-beach 1.111178719994): one of his
 * blocks in pscale structure, the opening line at the top and each part
 * indented by its depth, its place faint in the margin. His words are his to
 * change in place; adding, moving or removing a part is set out first and
 * written only on his yes. Every write carries the block as he saw it, so
 * 0.11's guard holds it when another hand has changed the block meanwhile.
 */

/** What the line he stands on is doing: shown, its words being changed, a part being added beneath, or waiting for where to move it. */
private enum class Doing { NOTHING, WORDS, ADD, MOVE }

/** A change set out for his yes: the plan, and what it does in plain words. */
private class SetOut(val plan: Plan, val said: String)

@Composable
fun EditorScreen(model: Model, nav: Nav, name: String) {
    val ink = LocalInk.current
    val pages = model.pages
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    // The block as he sees it, and its stored text then, carried with every write he makes from here.
    val loaded by produceState<Pair<List<OutlineLine>, Seen>?>(null, name, model.version, reload) {
        value = withContext(Dispatchers.IO) { runCatching { pages.outline(Region.POCKET, name)?.let { it to pages.seen(name) } }.getOrNull() }
    }
    // A clock (his now, his days) is walked a day at a time, as before: the editor hands it to its own page.
    val clock by produceState<Boolean?>(null, name) {
        value = withContext(Dispatchers.IO) { runCatching { pages.position(Region.POCKET, name)?.let { com.phenomemental.pocketbeach.bsp.Clock.isClock(name, it.floor, it.text) } == true }.getOrDefault(false) }
    }
    androidx.compose.runtime.LaunchedEffect(clock) { if (clock == true) nav.replace(Screen.Block(Region.POCKET, name, "")) }
    var at by remember { mutableStateOf<String?>(null) }
    var doing by remember { mutableStateOf(Doing.NOTHING) }
    var words by remember { mutableStateOf("") }
    var setOut by remember { mutableStateOf<SetOut?>(null) }
    var note by remember { mutableStateOf<String?>(null) }

    fun stand(address: String?, text: String?) {
        at = address
        doing = Doing.NOTHING
        words = text.orEmpty()
        setOut = null
    }

    /** Carry out [plan] with the block as he saw it; held, the block is shown again as it now stands. */
    fun write(plan: Plan, seen: Seen, done: String) {
        scope.launch {
            val d = withContext(Dispatchers.IO) { runCatching { pages.carryOut(plan, seen) }.getOrNull() }
            note = when {
                d == null -> "Nothing was written: the block couldn't be read."
                d.ok -> done
                d.held -> "The block changed while it was open here, so nothing was written. It's shown as it stands now: set it out again."
                else -> "Nothing was written: ${d.problem ?: "the write didn't land as shown"}."
            }
            if (d?.ok == true) model.changed() else reload++
            stand(null, null)
        }
    }

    fun quoted(text: String?) = text?.let { Words.quoted(it, 60) } ?: "an empty place"

    Page(
        onBack = { nav.back() },
        top = { Text(name, style = Type.name, color = ink.ink, maxLines = 1, modifier = Modifier.testTag("editor-name")) },
        end = {
            TextLink("Walk it", { nav.go(Screen.Block(Region.POCKET, name, "")) }, tag = "walk-block")
            TextLink("bsp", { nav.go(Screen.Walker(false, name)) }, tag = "walk-bsp")
        },
    ) {
        if (clock != false) return@Page
        val (lines, seen) = loaded ?: return@Page
        Text(
            "Tap a line to change its words. Adding, moving or removing a part is set out first, and written only when you say yes.",
            style = Type.small, color = ink.soft,
        )
        note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("editor-note")) }
        if (doing == Doing.MOVE) {
            val from = at
            Text("Tap the line it should stand beneath.", style = Type.aside, color = ink.crimson, modifier = Modifier.testTag("move-ask"))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                TextLink("Beneath the opening line", {
                    if (from != null) {
                        val p = pages.planMove(name, from, "")
                        if (p.refusal != null) note = p.refusal else setOut = SetOut(p, "Move $from, ${quoted(lines.firstOrNull { it.address == from }?.text)}, " +
                            "with ${Words.count(pages.linesBeneath(name, from), "line")} beneath it, to stand beneath the opening line, at ${pages.moveLandsAt(name, from, "")}.")
                        doing = Doing.NOTHING
                    }
                }, tag = "move-to-opening")
                TextLink("Not now", { doing = Doing.NOTHING }, tag = "move-not-now")
            }
        }
        Sheet(Modifier.testTag("outline")) {
            for (l in lines) {
                val here = at == l.address
                Row(
                    Modifier.fillMaxWidth().background(if (here) ink.wash else ink.sheet).clickable {
                        if (doing == Doing.MOVE) {
                            val from = at ?: return@clickable
                            val p = pages.planMove(name, from, l.address)
                            if (p.refusal != null) note = p.refusal else setOut = SetOut(p, "Move $from, ${quoted(lines.firstOrNull { it.address == from }?.text)}, " +
                                "with ${Words.count(pages.linesBeneath(name, from), "line")} beneath it, to stand beneath ${l.address.ifEmpty { "the opening line" }}, at ${pages.moveLandsAt(name, from, l.address)}.")
                            doing = Doing.NOTHING
                        } else {
                            note = null
                            stand(l.address, l.text)
                        }
                    }.padding(vertical = 8.dp).testTag("outline-line:${l.address}"),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Its place, faint in the margin; the line indented by its depth.
                    Text(l.address, style = Type.address, color = ink.soft.copy(alpha = 0.55f), modifier = Modifier.width(48.dp))
                    Text(
                        l.text ?: "Nothing written here.",
                        style = when {
                            l.text == null -> Type.aside
                            l.depth == 0 -> Type.heading
                            else -> Type.body
                        },
                        color = if (l.text == null) ink.soft else ink.ink,
                        modifier = Modifier.weight(1f).padding(start = (maxOf(0, l.depth - 1) * 18).dp),
                    )
                }
                if (here && setOut == null) Column(Modifier.padding(start = 48.dp, top = 6.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (doing) {
                        Doing.WORDS -> {
                            PaperField(words, { words = it }, "Its words", focus = true, tag = "edit-field")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Action("Keep these words", {
                                    val p = pages.planChange(name, l.address, words.trim())
                                    if (p.refusal != null) note = p.refusal else write(p, seen, "Kept.")
                                }, Modifier.weight(1f), enabled = words.isNotBlank(), strong = true, tag = "keep-words")
                                Action("Not now", { doing = Doing.NOTHING }, Modifier.weight(1f), tag = "words-not-now")
                            }
                        }
                        Doing.ADD -> {
                            PaperField(words, { words = it }, "The new part's words", focus = true, tag = "part-field")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Action("Set it out", {
                                    val p = pages.planAdd(name, l.address, words.trim())
                                    if (p.refusal != null) note = p.refusal
                                    else setOut = SetOut(p, "Add ${Words.quoted(words.trim(), 60)} beneath ${l.address.ifEmpty { "the opening line" }}, at ${p.landsAt}" +
                                        (if (p.wraps) ". The block wraps to make room: its lines move one step beneath the opening line." else "."))
                                }, Modifier.weight(1f), enabled = words.isNotBlank(), strong = true, tag = "set-out-part")
                                Action("Not now", { doing = Doing.NOTHING }, Modifier.weight(1f), tag = "part-not-now")
                            }
                        }
                        // Two rows, so every action fits a phone's width.
                        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                TextLink("Change its words", { words = l.text.orEmpty(); doing = Doing.WORDS }, tag = "change-words")
                                TextLink("Add a part beneath", { words = ""; doing = Doing.ADD }, tag = "add-part")
                            }
                            if (l.address.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                TextLink("Move", { note = null; doing = Doing.MOVE }, tag = "move-part")
                                TextLink("Remove", {
                                    val p = pages.planRemove(name, l.address)
                                    if (p.refusal != null) note = p.refusal
                                    else setOut = SetOut(p, "Remove ${l.address}, ${quoted(l.text)}" +
                                        pages.linesBeneath(name, l.address).let { n -> if (n == 0) "." else ", and the ${Words.count(n, "line")} beneath it." })
                                }, tag = "remove-part")
                            }
                        }
                    }
                }
                if (here) setOut?.let { s ->
                    Column(
                        Modifier.fillMaxWidth().padding(start = 48.dp, top = 6.dp, bottom = 10.dp).testTag("change-card"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SelectionContainer { Text(s.said, style = Type.body, color = ink.ink, modifier = Modifier.testTag("change-said")) }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Action("Yes", { write(s.plan, seen, "Done.") }, Modifier.weight(1f), strong = true, tag = "change-yes")
                            Action("Not now", { setOut = null }, Modifier.weight(1f), tag = "change-not-now")
                        }
                    }
                }
            }
        }
        if (lines.size >= 600) Text("Only the first 600 lines are shown here. Walk it to reach the rest.", style = Type.small, color = ink.soft)
    }
}

/** Sundial 0.12: a text file in his Sundial folder, opened as plain text, read only. */
@Composable
fun TextFileScreen(model: Model, nav: Nav, name: String) {
    val ink = LocalInk.current
    val app = model.app
    val text by produceState<String?>(null, name, model.version) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                Chats.textFiles(app.folderDir).firstOrNull { it.name == name }?.readText()
            }.getOrNull() ?: ""
        }
    }
    Page(onBack = { nav.back() }, top = { Text(name, style = Type.name, color = ink.ink, maxLines = 1) }) {
        val t = text ?: return@Page
        Label("A text file in your Sundial folder, read only")
        if (t.isEmpty()) Text("This file is empty, or it couldn't be read.", style = Type.aside, color = ink.soft)
        else SelectionContainer { Text(t, style = Type.body, color = ink.ink, modifier = Modifier.testTag("text-file")) }
    }
}
