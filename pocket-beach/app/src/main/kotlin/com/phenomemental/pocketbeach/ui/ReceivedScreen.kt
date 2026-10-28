package com.phenomemental.pocketbeach.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.phenomemental.pocketbeach.bsp.Js
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.bspRead
import com.phenomemental.pocketbeach.bsp.collectUnderscore
import com.phenomemental.pocketbeach.bsp.parseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A block shared in from another app (decision 19): data only, until Matthew keeps it. */
class Received(val text: String, val subject: String?) {
    /** The block, or why it can't be read as one. */
    val block: Result<JsObj> = runCatching {
        require(text.length <= MOST) { "It is too large for the pocket: more than a megabyte." }
        val start = text.indexOf('{')
        require(start >= 0) { "It holds no JSON object." }
        parseJson(text.substring(start, text.lastIndexOf('}') + 1)) as? JsObj ?: throw IllegalArgumentException("It isn't a JSON object.")
    }

    companion object {
        const val MOST = 1_000_000

        /** What another app shared with the pocket, read from the intent it sent. */
        fun from(context: Context, intent: Intent): Received? {
            if (intent.action != Intent.ACTION_SEND) return null
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: intent.getStringExtra(Intent.EXTRA_TITLE)
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { return Received(it, subject) }
            @Suppress("DEPRECATION")
            val uri = (if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            else intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri) ?: return null
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readNBytesCompat(MOST + 1)
                    bytes.toString(Charsets.UTF_8)
                }
            }.getOrNull() ?: return null
            return Received(text, subject ?: uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".json"))
        }

        private fun java.io.InputStream.readNBytesCompat(most: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (out.size() < most) {
                val n = read(buffer, 0, minOf(buffer.size, most - out.size()))
                if (n < 0) break
                out.write(buffer, 0, n)
            }
            return out.toByteArray()
        }
    }
}

@Composable
fun ReceivedScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val scope = rememberCoroutineScope()
    val received = model.app.inbox
    var name by rememberSaveable { mutableStateOf(received?.subject?.trim()?.take(120).orEmpty()) }
    var done by remember { mutableStateOf<Done?>(null) }
    val block = received?.block?.getOrNull()
    val refusal by produceState<String?>(null, name, block, model.version) {
        value = if (block == null || name.isBlank()) null else withContext(Dispatchers.IO) { model.pages.planKeep(name, block).refusal }
    }

    Page(
        onBack = { model.app.inbox = null; nav.back() },
        top = { Text("Received", style = Type.name, color = ink.ink, maxLines = 1) },
        bottom = if (block == null) null else ({
            Action("Let it go", { model.app.inbox = null; nav.back() }, Modifier.weight(1f), tag = "let-go")
            Action(
                "Keep it as a block",
                {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { model.pages.carryOut(model.pages.planKeep(name, block)) }
                        if (!result.ok) { done = result; return@launch }
                        val kept = name.trim()
                        model.app.inbox = null
                        model.changed()
                        nav.replace(Screen.Block(Region.POCKET, kept, ""))
                        model.say(Screen.Block(Region.POCKET, kept, ""), "Kept as a block of yours. It is in no shell.")
                    }
                },
                Modifier.weight(1f),
                enabled = name.isNotBlank() && refusal == null,
                strong = true,
                tag = "keep",
            )
        }),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("From another app")
            Text(if (block != null) "A block, as data" else "Something that isn't a block", style = Type.title.copy(fontSize = Type.heading.fontSize, lineHeight = Type.heading.lineHeight), color = ink.ink)
            Stroke()
        }
        Text(
            "It is shown here as data: nothing in it is followed, and it is kept only if you say so. Keeping it makes a new block of yours; " +
                "it joins no shell unless you choose that yourself.",
            style = Type.small, color = ink.soft,
        )
        if (received == null) {
            Text("Nothing is waiting.", style = Type.aside, color = ink.soft)
            return@Page
        }
        received.block.exceptionOrNull()?.let { e ->
            Notice("This can't be read as a pscale block. ${e.message}", tag = "unreadable")
            SelectionContainer { Text(received.text.take(2000), style = Type.call, color = ink.soft) }
            return@Page
        }
        val b = block!!
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Label("Its opening line")
            Text(collectUnderscore(b) ?: "(none)", style = Type.heading, color = ink.ink, modifier = Modifier.testTag("received-opening"))
        }
        val lines = remember(b) { runCatching { bspRead(b as Js, null, 0).discEntries.orEmpty() }.getOrDefault(emptyList()).filter { !it.address.all { c -> c == '0' || c == '.' } } }
        Sheet(Modifier.testTag("received-lines")) {
            if (lines.isEmpty()) Text("No lines beneath it.", style = Type.aside, color = ink.soft, modifier = Modifier.padding(vertical = 8.dp))
            lines.forEachIndexed { i, l ->
                if (i > 0) Rule()
                Text("${l.address}  ${l.content ?: "(empty)"}", style = Type.small, color = ink.ink, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
        Label("Keep it as")
        PaperField(name, { name = it; done = null }, "A name for the block", singleLine = true, tag = "keep-name")
        refusal?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("keep-refusal")) }
        done?.let { Notice(it.problem ?: "It wasn't kept.", tag = "problem") }
    }
}
