package com.phenomemental.pocketbeach.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.BlockSummary
import com.phenomemental.pocketbeach.bsp.Copies
import com.phenomemental.pocketbeach.bsp.Plan
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.ShoreView
import com.phenomemental.pocketbeach.bsp.Stamps
import com.phenomemental.pocketbeach.bsp.parseIsoMs
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShoreScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shore by produceState<ShoreView?>(null, model.version) { value = withContext(Dispatchers.IO) { model.pages.shore() } }
    val stamps by produceState(Stamps(), model.version) { value = withContext(Dispatchers.IO) { model.pages.stamps() } }
    var naming by rememberSaveable { mutableStateOf(false) }
    var restoring by remember { mutableStateOf<Copies.Copy?>(null) }

    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val now = System.currentTimeMillis()
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val text = model.pages.saveCopy()
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    Copies.read(text).blocks.size
                }
            }
            outcome.onSuccess { n ->
                model.saved(now)
                model.say(Screen.Shore, "Saved a copy of ${Words.count(n, "block")}.")
            }.onFailure { model.say(Screen.Shore, "The copy wasn't saved: ${it.message ?: "the file couldn't be written"}.") }
        }
    }
    val openCopy = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { Copies.read(context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }) }
            }
            outcome.onSuccess { restoring = it }.onFailure { model.say(Screen.Shore, it.message ?: "That file couldn't be read.") }
        }
    }

    Page(onBack = null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
            Text("Pocket beach", style = Type.title, color = ink.ink)
            Stroke()
        }
        CopyLine(model.lastSavedMs, Copies.reminderDue(model.lastSavedMs, stamps, System.currentTimeMillis()), Copies.changedSince(model.lastSavedMs, stamps)) {
            saveTo.launch("pocket-beach-copy-${LocalDate.now()}.json")
        }

        val loaded = shore ?: return@Page
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Label("Your blocks", Modifier.weight(1f))
            TextLink("Make a new block", { naming = true }, tag = "new-block")
        }
        val mine = loaded.mine
        Sheet {
            when {
                mine.isEmpty() -> Text(
                    "Nothing here yet. Make a new block to begin: it asks only for a name.",
                    style = Type.aside, color = ink.soft, modifier = Modifier.padding(vertical = 10.dp),
                )
                else -> mine.forEachIndexed { i, b ->
                    if (i > 0) Rule()
                    BlockRow(b) { nav.go(Screen.Block(Region.POCKET, b.name, "")) }
                }
            }
        }

        Label("Teaching blocks", Modifier.padding(top = 6.dp))
        Sheet {
            loaded.teaching.forEachIndexed { i, b ->
                if (i > 0) Rule()
                BlockRow(b) { nav.go(Screen.Block(Region.TEACHING, b.name, "")) }
            }
        }

        Column(Modifier.padding(top = 4.dp)) {
            TextLink("Save a copy", { saveTo.launch("pocket-beach-copy-${LocalDate.now()}.json") }, tag = "save-copy")
            TextLink("Restore a copy", { openCopy.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }, tag = "restore-copy")
            TextLink("About", { nav.go(Screen.About) }, tag = "about")
        }
    }

    if (naming) NewBlockDialog(model, onDone = { name ->
        naming = false
        if (name != null) {
            model.changed()
            nav.go(Screen.Block(Region.POCKET, name, ""))
        }
    })

    restoring?.let { copy ->
        RestoreDialog(copy, mine = shore?.mine?.size ?: 0, onCancel = { restoring = null }) {
            restoring = null
            scope.launch {
                val outcome = withContext(Dispatchers.IO) { runCatching { model.pages.restore(copy) } }
                outcome.onSuccess {
                    parseIsoMs(copy.saved)?.let { saved -> if (saved > (model.lastSavedMs ?: 0)) model.saved(saved) }
                    model.changed()
                    model.say(Screen.Shore, "Restored ${Words.count(copy.blocks.size, "block")} from the copy saved ${Words.at(copy.saved)}.")
                }.onFailure { model.say(Screen.Shore, "Nothing was restored: ${it.message ?: "the blocks couldn't be written"}.") }
            }
        }
    }
}

/** When a copy was last saved, and, once three days have passed with changes since, the reminder. */
@Composable
private fun CopyLine(lastSavedMs: Long?, due: Boolean, changed: Boolean, save: () -> Unit) {
    val ink = LocalInk.current
    val said = when {
        lastSavedMs == null && !changed -> "No copy saved yet. There is nothing to save."
        lastSavedMs == null -> "No copy saved yet. Your blocks are kept only on this phone."
        changed -> "Last copy saved ${Words.at(lastSavedMs)}. Changes since then are only on this phone."
        else -> "Last copy saved ${Words.at(lastSavedMs)}. Nothing has changed since."
    }
    if (due) {
        val days = lastSavedMs?.let { Words.days(it) }
        Notice(
            if (days != null) "It's ${Words.count(days, "day")} since the last copy, and blocks have changed. Save a copy to keep them safe."
            else "Your blocks are kept only on this phone, and no copy has been saved yet.",
            link = "Save a copy" to save,
            tag = "copy-reminder",
        )
    } else {
        Text(said, style = Type.aside, color = ink.soft, modifier = Modifier.testTag("last-saved"))
    }
}

@Composable
private fun BlockRow(b: BlockSummary, onOpen: () -> Unit) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp).testTag("block:${b.name}")) {
        Text(b.name, style = Type.name, color = ink.ink)
        b.opening?.let { Text(it, style = Type.aside, color = ink.soft, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun NewBlockDialog(model: Model, onDone: (String?) -> Unit) {
    val ink = LocalInk.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var plan by remember { mutableStateOf<Plan?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(name) {
        problem = null
        if (name.isBlank()) { plan = null; return@LaunchedEffect }
        delay(150)
        plan = withContext(Dispatchers.IO) { model.pages.planNewBlock(name) }
    }
    PaperDialog(
        "A new block",
        onDismiss = { onDone(null) },
        actions = {
            Action("Not now", { onDone(null) })
            Action("Make it", {
                val p = plan ?: return@Action
                scope.launch {
                    val done = withContext(Dispatchers.IO) { model.pages.carryOut(p) }
                    if (done.ok) onDone(p.block) else problem = done.problem
                }
            }, enabled = plan?.call != null, strong = true, tag = "make-block")
        },
    ) {
        Text("Give it a name. Only the name: the block begins with one opening line, which you can change.", style = Type.small, color = ink.soft)
        PaperField(name, { name = it }, "garden, letters, the year…", singleLine = true, focus = true, tag = "block-name")
        val p = plan
        when {
            problem != null -> Text(problem!!, style = Type.small, color = ink.crimson)
            p?.refusal != null -> Text(p.refusal!!, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("refusal"))
            p != null -> Text("Its opening line will be ${Words.quoted(p.text)}", style = Type.small, color = ink.soft)
        }
    }
}

@Composable
private fun RestoreDialog(copy: Copies.Copy, mine: Int, onCancel: () -> Unit, onRestore: () -> Unit) {
    val ink = LocalInk.current
    PaperDialog(
        "Restore this copy?",
        onDismiss = onCancel,
        actions = {
            Action("Keep my blocks", onCancel)
            Action("Replace my blocks", onRestore, strong = true, tag = "replace-blocks")
        },
    ) {
        Text(
            "The copy was saved ${Words.at(copy.saved)} and holds ${Words.count(copy.blocks.size, "block")}.",
            style = Type.body, color = ink.ink,
        )
        Text(
            "Restoring puts it in place of every block on this phone. The ${Words.count(mine, "block")} here now will be replaced, " +
                "and anything made or changed since the copy was saved will be lost. This can't be undone.",
            style = Type.small, color = ink.crimson,
        )
    }
}
