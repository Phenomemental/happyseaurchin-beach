package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as Pen
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 0.8.1, a standard chat (spine:pocket-beach 1.11117871364, Matthew, 1
 * November: "I want a standard chat"). The app's look is kept: its ink,
 * paper and crimson, and its type. Laid out as chat apps are:
 * - a slim top bar: the menu on the left (Your blocks, Teaching blocks,
 *   Bench, Save this conversation, Details, Settings, About), her name and
 *   what she wears in the middle, and a new conversation on the right;
 * - the thread, his words as bubbles on the right and hers plain on the
 *   left, each card beneath the reply it belongs to, notices as cards;
 * - one input bar at the foot.
 */
@Composable
fun ChatFrame(
    model: Model,
    nav: Nav,
    title: String,
    wearing: String,
    details: Boolean,
    onSave: (() -> Unit)?,
    input: (@Composable () -> Unit)?,
    scroll: ScrollState,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = LocalInk.current
    val asking = model.app.asking
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(ink.paper).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp).testTag("top-bar"), verticalAlignment = Alignment.CenterVertically) {
            Box {
                RoundMark("Menu", { menu = true }, tag = "menu") { c -> drawMenu(c) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.background(ink.paper)) {
                    fun item(text: String, tag: String, go: () -> Unit) = @Composable {
                        DropdownMenuItem(text = { Text(text, style = Type.body, color = ink.ink) }, onClick = { menu = false; go() }, modifier = Modifier.testTag(tag))
                    }
                    item("Your blocks", "menu-blocks") { nav.back() }()
                    item("Teaching blocks", "menu-teaching") { nav.back() }()
                    item(if (asking.benchRunning) "See the bench" else "Bench", "open-bench") { nav.go(Screen.Bench) }()
                    if (onSave != null) item("Save this conversation", "save-conversation", onSave)()
                    item(if (details) "Details: on" else "Details: off", "details") { model.app.showDetails = !details }()
                    item("Settings", "menu-settings") { nav.go(Screen.Settings) }()
                    item("About", "menu-about") { nav.go(Screen.About) }()
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = Type.name, color = ink.ink, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.testTag("her-name"))
                Text(wearing, style = Type.small, color = ink.soft, maxLines = 1, modifier = Modifier.testTag("wearing"))
            }
            RoundMark("New conversation", { asking.newConversation() }, tag = "new-conversation", enabled = asking.stage !is Asking.Stage.Thinking && !asking.benchRunning) { c -> drawNew(c) }
        }
        Rule()
        LocalSlip.current?.let { Slip(it.text, it.dismiss) }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        if (input != null) input()
    }
}

/** A round mark to tap: drawn, with its words for those who can't see it. */
@Composable
fun RoundMark(words: String, onClick: () -> Unit, tag: String, enabled: Boolean = true, strong: Boolean = false, lit: Boolean = false, draw: DrawScope.(Color) -> Unit) {
    val ink = LocalInk.current
    val fill = if (strong && enabled) ink.crimson else if (lit) ink.wash else ink.paper.copy(alpha = 0f)
    val mark = if (strong && enabled) ink.paper else ink.crimson
    Box(
        Modifier.size(44.dp).alpha(if (enabled) 1f else 0.38f).background(fill, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = words, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = words }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) { draw(mark) }
        // Its words, read by the screen reader and the tests, never shown.
        Text(words, modifier = Modifier.alpha(0f).size(1.dp))
    }
}

private fun DrawScope.pen() = Pen(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Three lines: the menu. */
fun DrawScope.drawMenu(color: Color) {
    for (y in listOf(0.22f, 0.5f, 0.78f)) drawLine(color, Offset(size.width * 0.1f, size.height * y), Offset(size.width * 0.9f, size.height * y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/** A cross: a new conversation. */
fun DrawScope.drawNew(color: Color) {
    drawLine(color, Offset(size.width / 2, size.height * 0.12f), Offset(size.width / 2, size.height * 0.88f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.12f, size.height / 2), Offset(size.width * 0.88f, size.height / 2), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/** A microphone: his voice. */
fun DrawScope.drawMic(color: Color) {
    val w = size.width
    val h = size.height
    drawRoundRect(color, topLeft = Offset(w * 0.35f, h * 0.05f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.55f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f), style = pen())
    val cup = Path().apply {
        moveTo(w * 0.2f, h * 0.45f)
        cubicTo(w * 0.2f, h * 0.85f, w * 0.8f, h * 0.85f, w * 0.8f, h * 0.45f)
    }
    drawPath(cup, color, style = pen())
    drawLine(color, Offset(w / 2, h * 0.8f), Offset(w / 2, h * 0.97f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/** An arrow up: send. */
fun DrawScope.drawSend(color: Color) {
    val arrow = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.48f)
        lineTo(size.width / 2, size.height * 0.14f)
        lineTo(size.width * 0.82f, size.height * 0.48f)
        moveTo(size.width / 2, size.height * 0.14f)
        lineTo(size.width / 2, size.height * 0.9f)
    }
    drawPath(arrow, color, style = pen())
}

/** A square: stop. */
fun DrawScope.drawStop(color: Color) {
    drawRect(color, topLeft = Offset(size.width * 0.22f, size.height * 0.22f), size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.56f))
}
