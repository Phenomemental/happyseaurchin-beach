package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.BringAcross
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.Reach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Sundial 0.13.5, bring my blocks across: with Reach the beach on, the
 * beach's blocks listed, those named with his handle first, each with a tick
 * box. The ticked ones are read from the beach (read only), set out on one
 * card, new or in place of his, and copied in on one yes, after one whole
 * copy of his pocket is saved. What came across, and anything refused with
 * why, is said beneath. Nothing is written to the beach.
 *
 * 0.13.6: the beach holds thousands of blocks, so the list shows his, and of
 * the rest only what his search finds; "Bring N across" stands at the foot of
 * the screen, always in reach; the card and the report stand at the top.
 */

@Composable
fun BringAcrossScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var names by remember { mutableStateOf<List<String>?>(null) }
    var handle by remember { mutableStateOf<String?>(null) }
    var ticked by remember { mutableStateOf(setOf<String>()) }
    var find by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var setOut by remember { mutableStateOf<List<BringAcross.Read>?>(null) }
    var report by remember { mutableStateOf<BringAcross.Report?>(null) }
    val all = names
    // The card and the report stand at the top: brought into view as they come.
    LaunchedEffect(setOut != null, report) { if (setOut != null || report != null) scroll.animateScrollTo(0) }

    val bring = {
        busy = true
        report = null
        scope.launch {
            // Read only: each ticked block asked of the beach, before anything is set out.
            val order = all.orEmpty()
            setOut = withContext(Dispatchers.IO) { ticked.toList().sortedBy { order.indexOf(it) }.map { n -> readFromBeach(app, n) } }
            busy = false
        }
        Unit
    }

    // Always in reach, however long the list: the foot of the screen doesn't scroll.
    val foot: @Composable RowScope.() -> Unit = {
        Action(
            if (busy) "Reading the beach…" else "Bring ${ticked.size} across", bring, Modifier.weight(1f),
            enabled = ticked.isNotEmpty() && !busy, strong = true, tag = "bring-go",
        )
    }
    Page(
        onBack = { nav.back() },
        top = { Text("Bring my blocks across", style = Type.name, color = ink.ink) },
        scroll = scroll,
        bottom = foot.takeIf { app.reachBeach && all != null && setOut == null },
    ) {
        if (!app.reachBeach) {
            Text("Reach the beach is off. Turn it on in Settings to bring your blocks across from the beach, read only.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("bring-off"))
            TextLink("Settings", { nav.go(Screen.Settings) }, tag = "bring-to-settings")
            return@Page
        }
        LaunchedEffect(Unit) {
            val (index, h) = withContext(Dispatchers.IO) {
                runCatching { app.reader.get(Reach.indexUrl()) }.getOrNull() to runCatching { Hatch.hatched(app.storage) }.getOrNull()
            }
            handle = h
            val listed = index?.takeIf { it.status in 200..299 }?.let { Reach.names(it.text) }.orEmpty()
            if (listed.isEmpty()) note = "The beach's list of blocks couldn't be read." else names = BringAcross.ordered(listed, h)
        }
        val reads = setOut
        if (reads != null) {
            // The one card, alone on the screen until he answers.
            BringCard(model, reads, onNo = { setOut = null }, onDone = { r -> setOut = null; ticked = emptySet(); report = r })
            return@Page
        }
        report?.let { r -> BringReport(r) }
        Text(
            "Tick the blocks to bring into your pocket: each comes across whole, as a new block or in place of yours. " +
                "A whole copy of your pocket is saved first, and Your pocket can put it back. Nothing is written to the beach.",
            style = Type.small, color = ink.soft,
        )
        note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("bring-note")) }
        if (all == null && note == null) Text("Reading the beach's list…", style = Type.small, color = ink.soft)
        if (all != null) {
            PaperField(find, { find = it }, "Find a block", singleLine = true, tag = "bring-find")
            val h = handle
            val shown = remember(all, h, find, ticked) { BringAcross.shown(all, h, find, ticked) }
            val yours = remember(all, h) { all.filter { BringAcross.isHis(it, h) } }
            val toggle = { n: String -> ticked = if (n in ticked) ticked - n else ticked + n; report = null }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (h == null) {
                    Text(
                        "Your handle isn't known on this phone, so your blocks aren't picked out: find them by name.",
                        style = Type.small, color = ink.soft, modifier = Modifier.testTag("bring-no-handle"),
                    )
                } else {
                    Text("Yours (${counted(yours.size)})", style = Type.heading, color = ink.ink, modifier = Modifier.padding(top = 8.dp).testTag("bring-yours"))
                    if (yours.isEmpty()) Text("None of the beach's blocks is named with $h.", style = Type.small, color = ink.soft)
                    else {
                        val allTicked = ticked.containsAll(yours)
                        TextLink(if (allTicked) "Untick all yours" else "Tick all yours", {
                            ticked = if (allTicked) ticked - yours.toSet() else ticked + yours
                            report = null
                        }, tag = "bring-tick-yours")
                    }
                    for (n in shown.his) BringRow(n, n in ticked, app.pages.exists(n)) { toggle(n) }
                }
                Text(
                    "The rest of the beach (${counted(shown.restCount)})", style = Type.heading, color = ink.ink,
                    modifier = Modifier.padding(top = 12.dp).testTag("bring-rest"),
                )
                for (n in shown.rest) BringRow(n, n in ticked, app.pages.exists(n)) { toggle(n) }
                val q = find.trim()
                when {
                    q.isEmpty() -> Text("Type in Find a block to look among them.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("bring-rest-hidden"))
                    shown.rest.none { it.contains(q, ignoreCase = true) } -> Text("None of the rest of the beach matches \u201c$q\u201d.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("bring-none"))
                    shown.more > 0 -> Text("And ${counted(shown.more)} more: type more of the name.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("bring-more"))
                }
            }
        }
    }
}

/** One block of the beach with its tick box, and whether he has one of that name. */
@Composable
private fun BringRow(name: String, ticked: Boolean, his: Boolean, toggle: () -> Unit) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth().clickable { toggle() }.testTag("bring-row:$name"), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = ticked, onCheckedChange = { toggle() }, colors = CheckboxDefaults.colors(checkedColor = ink.crimson), modifier = Modifier.testTag("tick:$name"))
        Column(Modifier.weight(1f)) {
            Text(name, style = Type.body, color = ink.ink)
            if (his) Text("You have one: it would be replaced.", style = Type.small, color = ink.soft)
        }
    }
}

/** A count as he reads it: 2,542. */
private fun counted(n: Int): String = String.format(java.util.Locale.ROOT, "%,d", n)

/** One block read from the beach, read only: the block, or why it couldn't be. */
private fun readFromBeach(app: com.phenomemental.pocketbeach.PocketApp, name: String): BringAcross.Read = runCatching {
    val reply = app.reader.get(Reach.blockUrl(name))
    when {
        reply.status == 404 -> BringAcross.Read(name, null, "the beach has no block called $name")
        reply.status !in 200..299 -> BringAcross.Read(name, null, "the beach answered ${reply.status}")
        else -> Reach.blockOf(reply.text)?.let { BringAcross.Read(name, it) } ?: BringAcross.Read(name, null, "the beach's answer isn't a block")
    }
}.getOrElse { BringAcross.Read(name, null, "the beach couldn't be reached: ${it.message ?: it::class.simpleName}") }

/** The one card: what would come across, new or in place of his, his shell's places that would change, and one yes. */
@Composable
private fun BringCard(model: Model, reads: List<BringAcross.Read>, onNo: () -> Unit, onDone: (BringAcross.Report) -> Unit) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    val readable = reads.filter { it.block != null }
    val replacing = readable.filter { app.pages.exists(it.name) }
    val new = readable.size - replacing.size
    Column(Modifier.fillMaxWidth().testTag("bring-card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Rule()
        Text(
            "Bring ${readable.size} into your pocket: $new new, ${replacing.size} in place of yours. A whole copy of your pocket is saved first. Nothing is written to the beach.",
            style = Type.body, color = ink.ink, modifier = Modifier.testTag("bring-card-said"),
        )
        for (r in readable) Text("${r.name}: " + if (r in replacing) "in place of yours" else "new", style = Type.small, color = ink.soft)
        // His shell: which places will change, before he says yes.
        for (r in replacing.filter { it.name.startsWith("shell:") }) {
            val changes = BringAcross.changes(app.pages.whole(r.name), r.block)
            Text(changesSaid(changes), style = Type.small, color = ink.crimson, modifier = Modifier.testTag("bring-shell-changes"))
        }
        for (r in reads.filter { it.block == null }) Text("${r.name} can't come across: ${r.why}.", style = Type.small, color = ink.crimson)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Action("Yes, bring them", {
                scope.launch {
                    val report = withContext(Dispatchers.IO) {
                        if (readable.isNotEmpty()) app.saveCopyBeforeTheBeach()
                        BringAcross.bring(app.pages, reads)
                    }
                    model.changed()
                    onDone(report)
                }
            }, Modifier.weight(1f), strong = true, tag = "bring-yes")
            Action("Not now", onNo, Modifier.weight(1f), tag = "bring-not-now")
        }
    }
}

/** What came across, and anything refused with why. */
@Composable
private fun BringReport(r: BringAcross.Report) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().testTag("bring-report"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text(if (r.came.isEmpty()) "Nothing came across." else "Came across:", style = Type.heading, color = ink.ink)
        for (c in r.came) Text(c.said, style = Type.body, color = ink.ink, modifier = Modifier.testTag("came:${c.name}"))
        if (r.came.isNotEmpty()) Text("A whole copy of your pocket was saved first: Your pocket can put it back.", style = Type.small, color = ink.soft)
        if (r.refused.isNotEmpty()) {
            Text("Refused:", style = Type.heading, color = ink.ink)
            for (x in r.refused) Text(x.said, style = Type.body, color = ink.crimson, modifier = Modifier.testTag("refused:${x.name}"))
        }
    }
}
