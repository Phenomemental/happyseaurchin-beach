package com.phenomemental.pocketbeach.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.phenomemental.pocketbeach.model.FetchService
import com.phenomemental.pocketbeach.model.ModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobSettings
import java.util.Locale

/**
 * The app's settings: for each of the companion's jobs, Edge's settings for
 * a model (0.5's Stage 1): the model, the processor, the context, then how
 * her replies are drawn: temperature, top-k, top-p and the longest reply.
 * 0.6: Gemma 4 E4B is fetched here, and is then a choice for each job.
 */
@Composable
fun SettingsScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    Page(onBack = { nav.back() }, top = { Text("Settings", style = Type.name, color = ink.ink, maxLines = 1) }) {
        Text(
            "Each of the companion's jobs has Edge's settings of its own. Jobs set to the same model, processor and context share one engine; " +
                "a job set apart loads an engine of its own beside it, and both stay in the phone's memory while Ask is open. " +
                "The phone runs one engine on its GPU at a time: Talk's. A job set to the GPU on another model or context runs on the CPU. " +
                "A change is used from the next thing you say in Ask.",
            style = Type.small, color = ink.soft,
        )
        val e4b by produceState<ModelStore.State?>(null) {
            while (true) {
                val st = withContext(Dispatchers.IO) { app.e4b.state() }
                value = st
                delay(if (st is ModelStore.State.Fetching) 1000 else 3000)
            }
        }
        // The models a job can be set to: those on this phone.
        val ready = JobSettings.MODELS.filter { it == JobSettings.GEMMA_E2B || it == JobSettings.GEMMA_E4B && e4b is ModelStore.State.Ready }
        BiggerModel(app, e4b)
        for (job in Job.values()) JobPart(job, app.settings(job), ready, app.cpuWhy(job)) { app.set(job, it) }
        SafetyWords(model, nav)
        AskLog(model)
        Rule()
        TextLink("Run the bench", { nav.go(Screen.Bench) }, tag = "settings-bench")
        Text("The bench asks fixed things on a scratch pocket and keeps what she said and how fast, to set beside Edge.", style = Type.small, color = ink.soft)
    }
}

/**
 * 0.6.2, part three: the words that say he isn't safe. When anything he
 * says holds one, her instruction for that turn carries the safety line and
 * no other. They are his to change once kept as a block, which is written
 * only when he taps; until then the app's first words are used. 0.6.3:
 * and the app shows its note beneath her reply, whatever she says; the
 * note is kept with the words, his to change. Words kept before 0.6.3 have
 * no note until he taps to add it; till then the app's own note shows.
 */
@Composable
private fun SafetyWords(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    val scope = rememberCoroutineScope()
    var kept by remember { mutableStateOf(app.pages.exists(Talking.SAFETY_BLOCK)) }
    var noted by remember { mutableStateOf(Talking.keepsNote(app.pages)) }
    var problem by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().testTag("safety"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Safety words", style = Type.heading, color = ink.ink)
        Text(
            "When anything you say holds one of these, she is told you've said you aren't safe: to say plainly she's worried for you, and urge you " +
                "to reach someone you trust, Samaritans on 116 123 at any hour, or 999 if you're in danger now. And beneath her reply, whatever she says, " +
                "the app shows this note:",
            style = Type.small, color = ink.soft,
        )
        Text(Talking.safetyNote(app.pages), style = Type.body, color = ink.ink, modifier = Modifier.testTag("safety-note-words"))
        Text("The words:", style = Type.small, color = ink.soft)
        Text(Talking.safetyWords(app.pages).joinToString(", ") { "\u201c$it\u201d" }, style = Type.body, color = ink.ink, modifier = Modifier.testTag("safety-words"))
        if (kept) TextLink("Change them in your ${Talking.SAFETY_BLOCK} block", { nav.go(Screen.Block(Region.POCKET, Talking.SAFETY_BLOCK, "")) }, tag = "open-safety-words")
        else TextLink("Keep the words and the note as a block you can change", {
            scope.launch {
                val done = withContext(Dispatchers.IO) { app.pages.carryOut(app.pages.planKeep(Talking.SAFETY_BLOCK, Talking.safetyBlock())) }
                if (done.ok) { kept = true; noted = true; model.changed() } else problem = "They weren't kept: ${done.problem}"
            }
        }, tag = "keep-safety-words")
        // Kept before 0.6.3: the note isn't in his block yet, so it's the app's until he adds it.
        if (kept && !noted) TextLink("Add the note to your ${Talking.SAFETY_BLOCK} block, to change it", {
            scope.launch {
                val why = withContext(Dispatchers.IO) { Talking.addNote(app.pages) }
                if (why == null) { noted = true; model.changed() } else problem = "The note wasn't added: $why"
            }
        }, tag = "add-safety-note")
        problem?.let { Text(it, style = Type.small, color = ink.soft, modifier = Modifier.testTag("safety-problem")) }
    }
}

/** 0.6.4: whether each exchange in Ask is kept in the ask log; off unless he turns it on. The saved conversation keeps everything either way. */
@Composable
private fun AskLog(model: Model) {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().testTag("ask-log-part"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Keep an ask log", style = Type.heading, color = ink.ink, modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = model.app.keepAskLog,
                onCheckedChange = { model.app.keepAskLog = it },
                colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = ink.crimson),
                modifier = Modifier.testTag("ask-log"),
            )
        }
        Text(
            "When on, each exchange in Ask is written to your ask-log block: what you said, what was proposed, and how it ended. Off, nothing is written " +
                "unless you ask; \u201cSave this conversation\u201d keeps everything either way.",
            style = Type.small, color = ink.soft,
        )
    }
}

@Composable
private fun JobPart(job: Job, s: JobSettings, models: List<String>, cpuWhy: String?, set: (JobSettings) -> Unit) {
    val ink = LocalInk.current
    val tag = "job-${job.name.lowercase()}"
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text(job.title, style = Type.heading, color = ink.ink)
        Text(job.about, style = Type.small, color = ink.soft)
        val choices = (models + s.model).distinct().sortedBy { JobSettings.MODELS.indexOf(it) }
        Setting(
            "Model", s.model, tag = "$tag-model",
            hint = if (choices.size == 1) "The one on this phone. Get Gemma 4 E4B above to choose it." else "E2B, the quicker; E4B, the bigger.",
            less = step(choices, s.model, -1)?.let { m -> { set(s.copy(model = m)) } },
            more = step(choices, s.model, 1)?.let { m -> { set(s.copy(model = m)) } },
        )
        Setting(
            "Processor", if (s.processor == "GPU") "GPU, else the CPU" else "CPU", tag = "$tag-processor",
            // 0.6.2: said plainly when it runs on the CPU though set to the GPU.
            hint = cpuWhy?.let { "Runs on the CPU: $it." },
            less = step(JobSettings.PROCESSORS, s.processor, -1)?.let { p -> { set(s.copy(processor = p)) } },
            more = step(JobSettings.PROCESSORS, s.processor, 1)?.let { p -> { set(s.copy(processor = p)) } },
        )
        Setting(
            "Context", "${count(s.context)} tokens", tag = "$tag-context", hint = "Her instruction, the conversation and her reply, together.",
            less = step(JobSettings.CONTEXTS, s.context, -1)?.let { c -> { set(s.copy(context = c)) } },
            more = step(JobSettings.CONTEXTS, s.context, 1)?.let { c -> { set(s.copy(context = c)) } },
        )
        Setting(
            "Temperature", two(s.temperature), tag = "$tag-temperature", hint = if (s.greedy) "With top-k 1 or temperature 0, the likeliest word each time." else null,
            less = (s.temperature > 0.0).then { set(s.copy(temperature = JobSettings.round(maxOf(0.0, s.temperature - 0.1)))) },
            more = (s.temperature < JobSettings.TEMPERATURE_MAX).then { set(s.copy(temperature = JobSettings.round(minOf(JobSettings.TEMPERATURE_MAX, s.temperature + 0.1)))) },
        )
        Setting(
            "Top-k", "${s.topK}", tag = "$tag-topk",
            less = (s.topK > 1).then { set(s.copy(topK = TOP_KS.lastOrNull { it < s.topK } ?: 1)) },
            more = (s.topK < JobSettings.TOP_K_MAX).then { set(s.copy(topK = TOP_KS.firstOrNull { it > s.topK } ?: JobSettings.TOP_K_MAX)) },
        )
        Setting(
            "Top-p", two(s.topP), tag = "$tag-topp",
            less = (s.topP > 0.05).then { set(s.copy(topP = JobSettings.round(maxOf(0.05, s.topP - 0.05)))) },
            more = (s.topP < 1.0).then { set(s.copy(topP = JobSettings.round(minOf(1.0, s.topP + 0.05)))) },
        )
        Setting(
            "Longest reply", "${count(s.longestReply)} tokens", tag = "$tag-reply",
            less = step(JobSettings.REPLIES, s.longestReply, -1)?.let { r -> { set(s.copy(longestReply = r)) } },
            more = step(JobSettings.REPLIES, s.longestReply, 1)?.let { r -> { set(s.copy(longestReply = r)) } },
        )
        val first = JobSettings.of(job)
        val firstName = when (job) {
            Job.TALK -> "Edge's settings"
            Job.WRITE -> "0.4's settings"
            Job.SORT -> "the laptop's settings"
        }
        if (s != first) TextLink("Back to $firstName", { set(first) }, tag = "$tag-reset")
        else Text(
            when (job) {
                Job.TALK -> "Edge's settings for Gemma 4 E2B."
                Job.WRITE -> "0.4's settings: the same words propose the same lines."
                Job.SORT -> "The laptop's settings for Stage 4: the likeliest word, a word or two a piece."
            },
            style = Type.small, color = ink.soft,
        )
    }
}

/** Gemma 4 E4B: offered when the phone has the memory, fetched on a tap, checked byte for byte, then a choice for each job. */
@Composable
private fun BiggerModel(app: com.phenomemental.pocketbeach.PocketApp, state: ModelStore.State?) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val size = ModelStore.gb(ModelStore.E4B.size)
    fun act(block: () -> Unit) { scope.launch(Dispatchers.IO) { block() } }
    val asked = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { FetchService.start(context, ModelStore.E4B) }
    fun fetch() {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            asked.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FetchService.start(context, ModelStore.E4B)
        }
    }
    Column(Modifier.fillMaxWidth().testTag("e4b"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Gemma 4 E4B", style = Type.heading, color = ink.ink)
        when (state) {
            null -> {}
            ModelStore.State.Ready -> Text("On this phone, checked: a choice for each job below.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("e4b-ready"))
            is ModelStore.State.TooLittleMemory -> Text(
                "This phone has ${ModelStore.gb(state.has)} of memory, and E4B needs a phone with 12 GB, so it isn't offered here.",
                style = Type.small, color = ink.soft, modifier = Modifier.testTag("e4b-too-little-memory"),
            )
            is ModelStore.State.TooLittleRoom -> Text(
                "E4B needs ${ModelStore.gb(state.needs)} of room and this phone has ${ModelStore.gb(state.free)} free.",
                style = Type.small, color = ink.soft, modifier = Modifier.testTag("e4b-too-little-room"),
            )
            ModelStore.State.Absent -> {
                Text(
                    "The bigger Gemma: slower, and on the laptop it sorted your day a little better. One download of $size from Hugging Face, " +
                        "over Wi-Fi only, checked byte for byte before it is used. Gemma 4 E2B stays as it is.",
                    style = Type.small, color = ink.soft,
                )
                Action("Get Gemma 4 E4B, $size over Wi-Fi", { fetch() }, Modifier.fillMaxWidth(), tag = "get-e4b")
            }
            is ModelStore.State.Fetching -> {
                Text(
                    "${ModelStore.gb(state.done)} of $size (${state.done * 100 / ModelStore.E4B.size}%)" +
                        if (state.bytesPerSecond > 0) ", at %.1f MB/s".format(state.bytesPerSecond / 1e6) else "",
                    style = Type.body, color = ink.ink, modifier = Modifier.testTag("e4b-fetching"),
                )
                state.note?.let { Text(it, style = Type.small, color = ink.crimson) }
                TextLink("Stop fetching", { act { app.e4b.stop() } }, tag = "e4b-stop")
            }
            is ModelStore.State.Paused -> {
                Text("Paused at ${ModelStore.gb(state.done)} of $size. It carries on from there.", style = Type.body, color = ink.ink, modifier = Modifier.testTag("e4b-paused"))
                Action("Carry on", { fetch() }, Modifier.fillMaxWidth(), tag = "e4b-carry-on")
                TextLink("Discard what has come", { act { app.e4b.discard() } }, tag = "e4b-discard")
            }
            ModelStore.State.Checking -> Text("Checking the download, byte for byte…", style = Type.aside, color = ink.soft)
            is ModelStore.State.Failed -> {
                Notice(state.why, tag = "e4b-failed")
                Action(if (state.done > 0) "Carry on from ${ModelStore.gb(state.done)}" else "Try again", { act { app.e4b.dismissFailure() }; fetch() }, Modifier.fillMaxWidth(), tag = "e4b-try-again")
            }
        }
    }
}

/** A setting and its value, a step less or more on either side (none where it can go no further). */
@Composable
private fun Setting(name: String, value: String, tag: String, hint: String? = null, less: (() -> Unit)?, more: (() -> Unit)?) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, style = Type.body, color = ink.ink)
            if (hint != null) Text(hint, style = Type.small, color = ink.soft, modifier = Modifier.testTag("$tag-hint"))
        }
        Action("−", { less?.invoke() }, enabled = less != null, tag = "$tag-less")
        Text(value, style = Type.small, color = ink.ink, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 84.dp).testTag(tag))
        Action("+", { more?.invoke() }, enabled = more != null, tag = "$tag-more")
    }
}

/** The steps top-k is moved through. */
private val TOP_KS = listOf(1, 2, 5, 10, 20, 32, 40, 64, 100, 128)

/** The value [by] steps along [values] from [now], or null at either end. */
private fun <T> step(values: List<T>, now: T, by: Int): T? = values.getOrNull(values.indexOf(now) + by)

private fun Boolean.then(action: () -> Unit): (() -> Unit)? = if (this) action else null

private fun two(x: Double) = String.format(Locale.UK, "%.2f", x)

private fun count(n: Int) = String.format(Locale.UK, "%,d", n)
