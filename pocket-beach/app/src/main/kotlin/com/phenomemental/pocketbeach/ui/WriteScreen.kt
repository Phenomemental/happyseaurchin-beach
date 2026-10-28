package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.Done
import com.phenomemental.pocketbeach.bsp.Kind
import com.phenomemental.pocketbeach.bsp.Plan
import com.phenomemental.pocketbeach.bsp.PositionView
import com.phenomemental.pocketbeach.bsp.Region
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Change this line" or "Add beneath", from wherever Matthew stands; where it will land is shown before anything is saved. */
@Composable
fun WriteScreen(model: Model, nav: Nav, screen: Screen.Write) {
    val ink = LocalInk.current
    val scope = rememberCoroutineScope()
    val here by produceState<PositionView?>(null, screen) {
        value = withContext(Dispatchers.IO) { model.pages.position(Region.POCKET, screen.block, screen.address) }
    }
    var text by rememberSaveable(screen) { mutableStateOf<String?>(null) }
    LaunchedEffect(here) {
        val h = here
        if (text == null && h != null) text = if (screen.kind == Kind.CHANGE) h.text.orEmpty() else ""
    }
    val typed = text.orEmpty()
    val current = here?.text
    val unchanged = screen.kind == Kind.CHANGE && typed == current.orEmpty()

    // Planned afresh as the words change: with what is typed, or with a stand-in
    // before anything is, so the landing shows from the start.
    val plan by produceState<Plan?>(null, screen, typed, model.version) {
        if (value != null) delay(200)
        value = withContext(Dispatchers.IO) {
            val line = if (typed.isBlank() || unchanged) "…" else typed
            if (screen.kind == Kind.ADD) model.pages.planAdd(screen.block, screen.address, line)
            else model.pages.planChange(screen.block, screen.address, line)
        }
    }
    var done by remember(screen) { mutableStateOf<Done?>(null) }
    var saving by remember(screen) { mutableStateOf(false) }
    val p = plan
    val ready = p != null && p.call != null && p.text == typed && typed.isNotBlank() && !unchanged

    Page(
        onBack = { nav.back() },
        top = {
            Text(screen.block, style = Type.name, color = ink.ink, maxLines = 1)
            here?.trail?.forEach { Text("  ›  " + Words.quoted(it.text, 24), style = Type.small, color = ink.soft, maxLines = 1) }
            if (screen.address.isNotEmpty() && here != null) Text("  ›  " + Words.quoted(current, 24), style = Type.small, color = ink.soft, maxLines = 1)
        },
        bottom = {
            Action("Cancel", { nav.back() }, Modifier.weight(1f), tag = "cancel")
            Action(
                "Save",
                {
                    val chosen = p ?: return@Action
                    saving = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { model.pages.carryOut(chosen) }
                        saving = false
                        if (!result.ok) {
                            done = result
                            return@launch
                        }
                        model.changed()
                        model.landed = screen.block to (result.landsAt ?: "")
                        val returnTo = nav.stack.getOrNull(nav.stack.size - 2) ?: Screen.Shore
                        model.say(
                            returnTo,
                            when {
                                chosen.kind == Kind.CHANGE -> "Saved."
                                result.wraps -> "Added at ${result.landsAt}. The block wrapped: the lines at its opening now stand one step beneath the opening line."
                                else -> "Added at ${result.landsAt}."
                            },
                        )
                        nav.back()
                    }
                },
                Modifier.weight(1f),
                enabled = ready && !saving,
                strong = true,
                tag = "save",
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label(
                when {
                    screen.kind == Kind.ADD -> "Add beneath"
                    current == null -> "Write this line"
                    else -> "Change this line"
                },
            )
            Text(
                when {
                    screen.kind == Kind.ADD && screen.address.isEmpty() -> "Beneath the opening line of ${screen.block}"
                    screen.kind == Kind.ADD -> "Beneath ${Words.quoted(current)}"
                    screen.address.isEmpty() -> "The opening line of ${screen.block}"
                    else -> "The line at ${screen.address}"
                },
                style = Type.heading, color = ink.ink,
            )
            Stroke()
        }
        // Where the line lands stands above the field, so the keyboard never hides it.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label("Where it lands")
            val refusal = p?.refusal?.takeIf { screen.kind == Kind.ADD || (!unchanged && typed.isNotBlank()) }
            when {
                p == null -> Text(" ", style = Type.small)
                refusal != null -> Text(refusal, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("refusal"))
                else -> Text(
                    Words.landing(screen.kind, screen.block, screen.address, p.landsAt, p.wraps, current),
                    style = Type.small, color = ink.ink, modifier = Modifier.testTag("landing"),
                )
            }
        }
        if (screen.kind == Kind.CHANGE && current != null) {
            Text("Now it reads ${Words.quoted(current, 200)}", style = Type.aside, color = ink.soft)
        }
        PaperField(
            typed, { text = it; done = null },
            if (screen.kind == Kind.ADD) "The new line" else "The line as it should read",
            focus = true, tag = "line-text",
        )
        Speak { words -> text = if (typed.isBlank()) words else typed.trimEnd() + " " + words; done = null }
        if (ready) {
            Label("The call")
            SelectionContainer { Text(Words.call(p), style = Type.call, color = ink.soft, modifier = Modifier.testTag("call")) }
        }
        done?.let { d ->
            Notice(d.problem ?: "It wasn't saved.", tag = "problem")
            if (d.reply.isNotEmpty()) SelectionContainer { Text(d.reply, style = Type.call, color = ink.soft) }
        }
    }
}
