package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.model.Job
import com.phenomemental.pocketbeach.model.JobSettings
import java.util.Locale

/**
 * The app's settings: for each of the companion's jobs, Edge's settings for
 * a model (0.5's Stage 1): the model, the processor, the context, then how
 * her replies are drawn: temperature, top-k, top-p and the longest reply.
 */
@Composable
fun SettingsScreen(model: Model, nav: Nav) {
    val ink = LocalInk.current
    val app = model.app
    Page(onBack = { nav.back() }, top = { Text("Settings", style = Type.name, color = ink.ink, maxLines = 1) }) {
        Text(
            "Each of the companion's jobs has Edge's settings of its own. Jobs set to the same model, processor and context share one engine; " +
                "a job set apart loads an engine of its own beside it, and both stay in the phone's memory while Ask is open. " +
                "A change is used from the next thing you say in Ask.",
            style = Type.small, color = ink.soft,
        )
        for (job in Job.values()) JobPart(job, app.settings(job)) { app.set(job, it) }
        Rule()
        TextLink("Run the bench", { nav.go(Screen.Bench) }, tag = "settings-bench")
        Text("The bench asks fixed things on a scratch pocket and keeps what she said and how fast, to set beside Edge.", style = Type.small, color = ink.soft)
    }
}

@Composable
private fun JobPart(job: Job, s: JobSettings, set: (JobSettings) -> Unit) {
    val ink = LocalInk.current
    val tag = "job-${job.name.lowercase()}"
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text(job.title, style = Type.heading, color = ink.ink)
        Text(job.about, style = Type.small, color = ink.soft)
        Setting(
            "Model", s.model, tag = "$tag-model", hint = "The one on this phone. Gemma 4 E4B comes with Stage 5.",
            less = step(JobSettings.MODELS, s.model, -1)?.let { m -> { set(s.copy(model = m)) } },
            more = step(JobSettings.MODELS, s.model, 1)?.let { m -> { set(s.copy(model = m)) } },
        )
        Setting(
            "Processor", if (s.processor == "GPU") "GPU, else the CPU" else "CPU", tag = "$tag-processor",
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
        if (s != first) TextLink(if (job == Job.TALK) "Back to Edge's settings" else "Back to 0.4's settings", { set(first) }, tag = "$tag-reset")
        else Text(if (job == Job.TALK) "Edge's settings for Gemma 4 E2B." else "0.4's settings: the same words propose the same lines.", style = Type.small, color = ink.soft)
    }
}

/** A setting and its value, a step less or more on either side (none where it can go no further). */
@Composable
private fun Setting(name: String, value: String, tag: String, hint: String? = null, less: (() -> Unit)?, more: (() -> Unit)?) {
    val ink = LocalInk.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, style = Type.body, color = ink.ink)
            if (hint != null) Text(hint, style = Type.small, color = ink.soft)
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
