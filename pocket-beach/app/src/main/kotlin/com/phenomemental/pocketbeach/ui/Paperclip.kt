package com.phenomemental.pocketbeach.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.phenomemental.pocketbeach.bsp.Chats
import com.phenomemental.pocketbeach.bsp.Region
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Sundial 0.12, the plain chat's paperclip (spine:pocket-beach 1.111178719993):
 * files from his Sundial folder, a block or a text file, one or several, read
 * into the conversation as text and shown as a chip on his message; a
 * conversation or a reply shared out as text; his older conversations listed.
 */

/** A paperclip: a file added to his message. */
fun DrawScope.drawClip(color: Color) {
    val w = size.width
    val h = size.height
    val clip = Path().apply {
        moveTo(w * 0.62f, h * 0.30f)
        lineTo(w * 0.62f, h * 0.72f)
        cubicTo(w * 0.62f, h * 0.92f, w * 0.30f, h * 0.92f, w * 0.30f, h * 0.72f)
        lineTo(w * 0.30f, h * 0.22f)
        cubicTo(w * 0.30f, h * 0.06f, w * 0.78f, h * 0.06f, w * 0.78f, h * 0.22f)
        lineTo(w * 0.78f, h * 0.66f)
    }
    drawPath(clip, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    drawLine(color, Offset(w * 0.46f, h * 0.30f), Offset(w * 0.46f, h * 0.68f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}

/** The files on a message, each a chip with its name; with [remove], each can be taken off before he sends. */
@Composable
fun FileChips(files: List<Chats.Attached>, remove: ((Chats.Attached) -> Unit)? = null, alignEnd: Boolean = false) {
    if (files.isEmpty()) return
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
            for (f in files) Row(
                Modifier.widthIn(max = 320.dp).background(ink.sheet, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp).testTag("file-chip"),
                verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Sundial 0.13.6: the name, then what was given beneath it, each on its own line and as wide as the chip,
                // so a long name doesn't squeeze the words beside it into a column of letters.
                Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text((if (f.block) "▤ " else "▢ ") + f.name, style = Type.small, color = ink.ink, modifier = Modifier.testTag("file-chip-name"))
                    // Sundial 0.13.5: talk about this, with its shape; Sundial 0.13.6: from the editor, its top lines as bsp() reads them.
                    if (f.view) Text(if (f.walked) "your walk of it, and its shape" else "its top lines and its shape", style = Type.small, color = ink.soft, modifier = Modifier.testTag("file-chip-view"))
                    // Sundial 0.13.3: a block his words named, read by Sundial before she answered. Sundial 0.13.6: its top lines, as bsp() reads them.
                    else if (f.byApp) Text("read by Sundial: its top lines", style = Type.small, color = ink.soft, modifier = Modifier.testTag("file-chip-read"))
                    // Too big for her window whole: she has its root and each part's opening line, and the chip says so.
                    else if (f.block && !f.whole) Text("its opening lines only", style = Type.small, color = ink.soft, modifier = Modifier.testTag("file-chip-partial"))
                }
                if (remove != null) Text(
                    "×", style = Type.body, color = ink.crimson,
                    modifier = Modifier.clickable { remove(f) }.padding(horizontal = 4.dp).testTag("file-chip-remove"),
                )
            }
        }
    }
}

/** Share [text] out to another app, as plain text. */
fun shareText(context: Context, text: String, subject: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    context.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** A file chosen with Android's picker, read as text: its name and its words; null when it can't be read. */
private fun readPicked(context: Context, uri: Uri): Chats.Attached? = runCatching {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "a file"
    val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return null
    // A block chosen from elsewhere is read as his blocks are: readable text, never JSON.
    Chats.picked(name, text, (context.applicationContext as com.phenomemental.pocketbeach.PocketApp).shelf)
}.getOrNull()

/**
 * The paperclip's sheet: his blocks, and the text files in his Sundial
 * folder when Sundial can reach it, each added on a tap; and Android's picker
 * for a text file from anywhere else. Nothing is written: they are read.
 */
@Composable
fun AttachSheet(model: Model, onDone: () -> Unit) {
    val ink = LocalInk.current
    val app = model.app
    val asking = app.asking
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blocks by produceState(emptyList<com.phenomemental.pocketbeach.bsp.BlockSummary>(), model.version) {
        value = withContext(Dispatchers.IO) { runCatching { app.pages.shore().mine }.getOrDefault(emptyList()) }
    }
    val texts by produceState(emptyList<java.io.File>(), model.version) {
        value = withContext(Dispatchers.IO) { if (app.inFolder && app.allFilesAccess()) runCatching { Chats.textFiles(app.folderDir) }.getOrDefault(emptyList()) else emptyList() }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch {
            val read = withContext(Dispatchers.IO) { uris.mapNotNull { readPicked(context, it) } }
            read.forEach(asking::attach)
        }
    }
    Dialog(onDismissRequest = onDone) {
        Column(
            Modifier.fillMaxWidth().background(ink.paper, RoundedCornerShape(16.dp)).padding(18.dp).testTag("attach-sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Add a file", style = Type.heading, color = ink.ink)
            Text("It is read into the conversation as text, and nothing is written. A block too big to read whole is given as its opening lines.", style = Type.small, color = ink.soft)
            FileChips(asking.attached.toList(), remove = { asking.attached.remove(it) })
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (blocks.isNotEmpty()) Label("Your blocks")
                for (b in blocks) Row(
                    Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            val read = withContext(Dispatchers.IO) { runCatching { Chats.blockFile(app.pages, Region.POCKET, b.name) }.getOrNull() }
                            if (read != null) asking.attach(read)
                        }
                    }.padding(vertical = 7.dp).testTag("attach-block:${b.name}"),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = Type.body, color = ink.ink)
                        b.opening?.let { Text(Words.quoted(it, 90), style = Type.small, color = ink.soft) }
                    }
                }
                if (texts.isNotEmpty()) Label("Text files in your Sundial folder", Modifier.padding(top = 8.dp))
                for (f in texts) Row(
                    Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            val text = withContext(Dispatchers.IO) { runCatching { f.readText() }.getOrNull() }
                            if (text != null) asking.attach(Chats.Attached(f.name, text, block = false))
                        }
                    }.padding(vertical = 7.dp).testTag("attach-text:${f.name}"),
                ) { Text(f.name, style = Type.body, color = ink.ink) }
            }
            TextLink("Choose a text file from elsewhere", { pick.launch(arrayOf("text/*")) }, tag = "attach-pick")
            Action("Done", onDone, Modifier.fillMaxWidth(), strong = true, tag = "attach-done")
        }
    }
}

/** Sundial 0.12: his older conversations, newest first, each by its day and his first words; a tap opens it. */
@Composable
fun TalksScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val talks by produceState(emptyList<Chats.Kept>(), model.version) { value = withContext(Dispatchers.IO) { model.app.asking.keptTalks() } }
    Page(onBack = { nav.back() }) {
        Text("Your conversations", style = Type.title, color = ink.ink)
        Stroke()
        if (talks.isEmpty()) Text("None kept yet. Each conversation is kept here once she has answered.", style = Type.aside, color = ink.soft, modifier = Modifier.testTag("talks-none"))
        Sheet {
            talks.forEachIndexed { i, t ->
                if (i > 0) Rule()
                Column(Modifier.fillMaxWidth().clickable { nav.go(Screen.Talk(t.startedMs)) }.padding(vertical = 8.dp).testTag("talk:${t.startedMs}")) {
                    Text(Words.at(t.startedMs), style = Type.small, color = ink.soft)
                    Text(t.opening, style = Type.body, color = ink.ink)
                }
            }
        }
    }
}

/** Sundial 0.12: one older conversation, as it went, to read or to share out as text. */
@Composable
fun TalkScreen(model: Model, nav: Nav, startedMs: Long) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val talk by produceState<Chats.Kept?>(null, model.version, startedMs) {
        value = withContext(Dispatchers.IO) { model.app.asking.keptTalks().firstOrNull { it.startedMs == startedMs } }
    }
    Page(onBack = { nav.back() }) {
        val t = talk ?: return@Page
        Text(Words.at(t.startedMs), style = Type.title, color = ink.ink)
        TextLink("Share this conversation", { shareText(context, Chats.asText(t), "A conversation from Sundial") }, tag = "talk-share")
        Stroke()
        for (s in t.exchanges) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp).testTag("talk-exchange")) {
                FileChips(s.files, alignEnd = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(s.his, style = Type.body, color = ink.ink, modifier = Modifier.widthIn(max = 300.dp).background(ink.wash, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 9.dp))
                }
                if (s.hers.isNotBlank()) Text(s.hers, style = Type.body, color = ink.ink)
            }
        }
    }
}
