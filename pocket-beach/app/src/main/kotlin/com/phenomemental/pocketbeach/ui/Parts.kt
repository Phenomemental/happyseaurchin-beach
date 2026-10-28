package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** The passing message the page shows under its trail, and how to put it away. */
class SlipNote(val text: String, val dismiss: () -> Unit)

val LocalSlip = staticCompositionLocalOf<SlipNote?> { null }

/** A page of the app: paper to the edges, the back mark and the trail at the top, actions at the foot above the keyboard. */
@Composable
fun Page(
    onBack: (() -> Unit)?,
    top: (@Composable RowScope.() -> Unit)? = null,
    bottom: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxSize().background(ink.paper).windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (onBack != null || top != null) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 4.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) BackMark(onBack) else Spacer(Modifier.width(18.dp))
                if (top != null) Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, content = top)
            }
        }
        LocalSlip.current?.let { Slip(it.text, it.dismiss) }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
        if (bottom != null) {
            Rule()
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = bottom)
        }
    }
}

@Composable
private fun BackMark(onBack: () -> Unit) {
    val ink = LocalInk.current
    Box(
        Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).testTag("back"),
        contentAlignment = Alignment.Center,
    ) { Text("‹", style = Type.title, color = ink.crimson) }
}

/** The framed sheet a block's lines are set on: a hairline frame inside a ruled one, like a plate in a book. */
@Composable
fun Sheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ink = LocalInk.current
    Box(modifier.fillMaxWidth().background(ink.sheet).border(1.dp, ink.rule).padding(4.dp).border(0.5.dp, ink.rule)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(LocalInk.current.rule))
}

/** The short crimson stroke beneath a heading. */
@Composable
fun Stroke() {
    Box(Modifier.width(44.dp).height(2.dp).background(LocalInk.current.crimson))
}

/** A small label, set wide in capitals. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Type.label, color = LocalInk.current.soft, modifier = modifier)
}

/** A button in the book's manner: crimson words in a crimson frame, or crimson filled for the one that saves. */
@Composable
fun Action(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    strong: Boolean = false,
    tag: String = text,
) {
    val ink = LocalInk.current
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .background(if (strong) ink.crimson else ink.paper, shape)
            .border(BorderStroke(1.dp, ink.crimson), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.action, color = if (strong) ink.paper else ink.crimson, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** A link in running text: crimson, underlined. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: TextStyle = Type.small, tag: String = text) {
    Text(
        text,
        style = style,
        color = LocalInk.current.crimson,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp).testTag(tag),
    )
}

/** Something to know about where you stand, with a crimson rule beside it and, if it asks for something, the link to do it. */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier, link: Pair<String, () -> Unit>? = null, tag: String = "notice") {
    val ink = LocalInk.current
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).background(ink.wash).semantics(mergeDescendants = true) {}.testTag(tag)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(ink.crimson))
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(text, style = Type.small, color = ink.ink)
            if (link != null) TextLink(link.first, link.second)
        }
    }
}

/** A field to write a line in, framed like the sheet, in the block's own type. */
@Composable
fun PaperField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    focus: Boolean = false,
    tag: String = "field",
) {
    val ink = LocalInk.current
    val requester = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = Type.body.copy(color = ink.ink),
        cursorBrush = SolidColor(ink.crimson),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth().focusRequester(requester).testTag(tag),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().background(ink.sheet).border(1.dp, ink.rule).padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (value.isEmpty()) Text(placeholder, style = Type.aside, color = ink.soft)
                inner()
            }
        },
    )
    if (focus) LaunchedEffect(Unit) { requester.requestFocus() }
}

/** A question asked on a sheet of paper, with its answers at the foot. */
@Composable
fun PaperDialog(title: String, onDismiss: () -> Unit, actions: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val ink = LocalInk.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(ink.paper).border(1.dp, ink.rule).padding(4.dp).border(0.5.dp, ink.rule).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = Type.heading, color = ink.ink)
            Stroke()
            content()
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), content = actions)
        }
    }
}

/** A passing message, set in the page beneath its trail so it never covers anything to tap. */
@Composable
fun Slip(text: String, onDismiss: () -> Unit) {
    val ink = LocalInk.current
    Box(
        Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth()
            .background(ink.ink).clickable(onClick = onDismiss).padding(horizontal = 16.dp, vertical = 12.dp).testTag("slip"),
    ) {
        Text(text, style = Type.small, color = ink.paper, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}
