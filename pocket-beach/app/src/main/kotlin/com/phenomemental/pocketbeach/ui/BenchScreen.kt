package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.Region
import kotlinx.coroutines.delay

/** The bench: what it does, the run as it goes, and the block it is kept in. */
@Composable
fun BenchScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val benching = model.app.benching
    benching.changed = { model.changed() }
    val stage = benching.stage
    val view = LocalView.current
    // The screen stays on while the bench runs.
    DisposableEffect(benching.running) {
        view.keepScreenOn = benching.running
        onDispose { view.keepScreenOn = false }
    }
    val busy = model.app.asking.busy

    Page(
        onBack = { nav.back() },
        top = { Text("The bench", style = Type.name, color = ink.ink, maxLines = 1) },
        bottom = {
            when {
                benching.running -> Action("Stop", { benching.stop() }, Modifier.fillMaxWidth(), tag = "stop-bench")
                else -> Action("Run the bench", { benching.start() }, Modifier.fillMaxWidth(), enabled = !busy, strong = true, tag = "run-bench")
            }
        },
    ) {
        Text(
            "Each model on this phone in turn: how fast it loads and answers; the Keep asks, fourteen of them, to the principles (nothing is written " +
                "unless you ask; a new block only when you name it; asked to keep something with no block named, the app asks you where; her name by code; " +
                "her shell's opening line yours alone; one home per fact; a post only when you ask); her four fixed asks; and the telling test. Each is put " +
                "to a scratch copy of a small pocket (garden, notes, an empty Million and the companion's shell), so nothing of yours is written, and nothing is posted.",
            style = Type.body, color = ink.ink,
        )
        Text(
            "Each ask is timed and judged right or wrong; a change to her shell's opening line fails every ask. Then the bench tries the engine itself: what " +
                "this phone's chip and the model file allow. It keeps the screen on.",
            style = Type.small, color = ink.soft,
        )
        Text("The run is kept as one new block, bench- and the date, which you can share from its page.", style = Type.small, color = ink.soft)
        Text(
            "Or, quicker: each model on this phone (Gemma 4 E2B, and E4B once it's here), loaded, a plain answer timed, and your day, " +
                "the telling test written and spoken, sorted a piece at a time as Ask sorts it, with its marks. A few minutes a model.",
            style = Type.small, color = ink.soft,
        )
        if (!benching.running) TextLink("Time each model on your day", { benching.startModels() }, tag = "run-models")
        if (busy && !benching.running) Notice("An Ask is under way. The bench can run once it's over.", tag = "bench-busy")

        when (stage) {
            Benching.Stage.Ready -> {}
            is Benching.Stage.Loading -> {
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(stage) { while (true) { delay(1000); now = System.currentTimeMillis() } }
                Text("Loading the companion… ${((now - stage.since) / 1000).coerceAtLeast(0)} s", style = Type.aside, color = ink.soft, modifier = Modifier.testTag("bench-stage"))
            }
            is Benching.Stage.Asking -> Text("${stage.run}, ask ${stage.ask.n} of ${Bench.ASKS.size}: ${stage.ask.question}", style = Type.aside, color = ink.ink, modifier = Modifier.testTag("bench-stage"))
            is Benching.Stage.Probing -> Text("${stage.what}…", style = Type.aside, color = ink.ink, modifier = Modifier.testTag("bench-stage"))
            is Benching.Stage.Saved -> {
                Done(
                    if (stage.right.isEmpty()) "Kept as ${stage.name}."
                    else stage.right.joinToString("; ") { (way, right) -> "${way.substringBefore(" (")}: $right of ${stage.of} right" } + ". Kept as ${stage.name}.",
                    detail = "Open it to read the run, or share it from its page.", tag = "bench-saved",
                )
                TextLink("Open ${stage.name}", { nav.go(Screen.Block(Region.POCKET, stage.name, "")) }, tag = "open-bench-block")
            }
            is Benching.Stage.Failed -> Notice("The bench stopped: ${stage.why}", tag = "bench-failed")
        }

        if (benching.results.isNotEmpty()) {
            Label("So far")
            Sheet(Modifier.testTag("bench-results")) {
                benching.results.forEachIndexed { i, r ->
                    if (i > 0) Rule()
                    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text((if (r.right) "✓ " else "✗ ") + "${r.ask.n}. ${r.ask.question}", style = Type.small, color = if (r.right) ink.ink else ink.crimson)
                        Text(
                            "${Bench.secs(r.did.timing.totalMs)} s" + (r.did.timing.firstTokenMs?.let { ", first token ${Bench.secs(it)} s" } ?: "") +
                                (if (r.problems.isNotEmpty()) ". " + r.problems.first() else ""),
                            style = Type.small, color = ink.soft,
                        )
                    }
                }
            }
        }
    }
}
