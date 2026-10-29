package com.phenomemental.pocketbeach.ui

import android.app.ActivityManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Bench
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.Shells
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.model.Gemma
import com.phenomemental.pocketbeach.model.Mind
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * One run of the bench at a time: the twenty fixed asks, each on a fresh
 * scratch pocket (nothing of Matthew's is written, nothing posted), then the
 * engine's probes, then the whole run kept as one new block he can share.
 * Held by the app, so it goes on while he looks elsewhere.
 */
class Benching(private val app: PocketApp) {
    sealed interface Stage {
        data object Ready : Stage
        class Loading(val since: Long) : Stage
        class Asking(val run: String, val ask: Bench.Ask) : Stage
        class Probing(val what: String) : Stage
        /** The run kept as [name]: how many of the twenty each way got right, in order. */
        class Saved(val name: String, val right: List<Pair<String, Int>>, val of: Int) : Stage
        class Failed(val why: String) : Stage
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var chat: Mind.Chat? = null
    private var talker: Mind.Talker? = null

    /** How 0.4's first word was chosen on this run: held by the engine, or free and why. */
    private var route = "not asked"

    companion object {
        const val BEFORE = "0.3 (one JSON plan, held)"
        const val HELD = "0.4, her first word held (route word, actions, streamed)"
        const val FREE = "0.4, her first word free (route word, actions, streamed)"
        const val CPU = "0.4 on the CPU, her first word free (route word, actions, streamed)"
    }

    var stage by mutableStateOf<Stage>(Stage.Ready)
        private set
    val results = mutableStateListOf<Bench.Result>()

    /** Told when the run is kept, so the pages show the new block. */
    var changed: () -> Unit = {}

    val running: Boolean get() = stage is Stage.Loading || stage is Stage.Asking || stage is Stage.Probing

    fun start() {
        if (running) return
        results.clear()
        stage = Stage.Loading(System.currentTimeMillis())
        job = scope.launch {
            try {
                val mind = app.mind
                // Ask's conversation goes first: the bench loads the model both ways, and would cut it off.
                withContext(Dispatchers.IO) { app.asking.letGo() }
                val about = mutableListOf(phone(), "The companion: ${companion(mind)}.")
                val loadMs = withContext(Dispatchers.IO) { mind.warm() }
                about += "Loaded " + (loadMs?.let { "in ${Bench.secs(it)} s" } ?: "already") + (mind.processor?.let { ", on the $it" } ?: "") + "."
                // Before: 0.3's companion, one JSON plan an answer, held to its form.
                for (ask in Bench.ASKS) {
                    stage = Stage.Asking(BEFORE, ask)
                    results += withContext(Dispatchers.IO) {
                        Bench.one(ask, app.shelf, System.currentTimeMillis()) { a, pages, storage -> runBlocking { plan(mind, a, pages, storage) } }
                    }
                }
                val before = results.toList()
                // After: 0.4's, a route word then plain words, reads or actions, streamed; her first word held by the engine, then free.
                val after = listOf(HELD to true, FREE to false).map { (name, held) ->
                    val from = results.size
                    for (ask in Bench.ASKS) {
                        stage = Stage.Asking(name, ask)
                        results += withContext(Dispatchers.IO) {
                            Bench.one(ask, app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE) { a, pages, storage -> runBlocking { talks(mind, a, pages, storage, held) } }
                        }
                    }
                    if (held) about += "0.4's first word, asked to be held: $route."
                    Bench.Run(name, results.drop(from))
                }
                stage = Stage.Probing("Keeping one conversation for three asks")
                val keptGpu = withContext(Dispatchers.IO) { kept(mind) }
                // Then on the CPU alone, beside the GPU: the same asks, free as in Ask, and one conversation kept, where its slower reading falls on the first ask.
                stage = Stage.Probing("Loading the model on the CPU")
                val cpu = try {
                    withContext(Dispatchers.IO) { mind.only("CPU") }
                    val cpuLoad = withContext(Dispatchers.IO) { mind.warm() }
                    about[2] = about[2] + " On the CPU alone: loaded " + (cpuLoad?.let { "in ${Bench.secs(it)} s" } ?: "already") + (mind.processor?.let { ", on the $it" } ?: "") + "."
                    val from = results.size
                    for (ask in Bench.ASKS) {
                        stage = Stage.Asking(CPU, ask)
                        results += withContext(Dispatchers.IO) {
                            Bench.one(ask, app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE) { a, pages, storage -> runBlocking { talks(mind, a, pages, storage, false) } }
                        }
                    }
                    stage = Stage.Probing("Keeping one conversation for three asks, on the CPU")
                    val keptCpu = withContext(Dispatchers.IO) { kept(mind) }
                    about += "One conversation kept for three asks, her first word free as in Ask, her first words after: on the GPU $keptGpu; on the CPU $keptCpu."
                    Bench.Run(CPU, results.drop(from))
                } finally {
                    withContext(Dispatchers.IO + NonCancellable) { runCatching { mind.only(null) } }
                }
                (mind as? Gemma)?.let { gemma ->
                    stage = Stage.Probing("Letting the model go, to try the engine")
                    about += withContext(Dispatchers.IO) { gemma.probes { what -> scope.launch { stage = Stage.Probing(what) } } }
                }
                val now = System.currentTimeMillis()
                val name = "bench-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.UK))
                val runs = listOf(Bench.Run(BEFORE, before)) + after + cpu
                val block = Bench.block(runs, about, now)
                val done = withContext(Dispatchers.IO) { app.pages.carryOut(app.pages.planKeep(name, block)) }
                changed()
                stage = if (done.ok) Stage.Saved(name, runs.map { it.name to it.results.count { r -> r.right } }, Bench.ASKS.size) else Stage.Failed("The run wasn't kept: ${done.problem}")
            } catch (e: CancellationException) {
                stage = Stage.Ready
                throw e
            } catch (e: Exception) {
                stage = Stage.Failed(e.message ?: e::class.java.simpleName)
            }
        }
    }

    /** Forget the last run, as the device tests begin. */
    fun reset() {
        job?.cancel()
        results.clear()
        stage = Stage.Ready
    }

    /** Stop the run; nothing is kept. */
    fun stop() {
        chat?.stop()
        talker?.stop()
        job?.cancel()
        stage = Stage.Ready
    }

    /** Today's companion: one JSON plan an answer, its writes planned on the scratch pocket, timed. */
    private suspend fun plan(mind: Mind, ask: Bench.Ask, pages: com.phenomemental.pocketbeach.bsp.Pages, storage: com.phenomemental.pocketbeach.bsp.MemoryStorage): Bench.Did {
        val now = System.currentTimeMillis()
        val instruction = Asks.instruction(HelloWorld.SHELL, Shells(storage, app.shelf).wear(HelloWorld.SHELL, now).window, Asks.plainWindow(pages, HelloWorld.SHELL), pages)
        val started = System.nanoTime()
        return mind.chat(instruction).use { c ->
            chat = c
            try {
                val asked = Asks.ask(ask.question, c.talk, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal()) { System.currentTimeMillis() }
                val total = (System.nanoTime() - started) / 1_000_000
                val turns = c.pace.turns
                val first = turns.firstOrNull()?.firstTokenSeconds?.let { (it * 1000).toLong() }
                // Today's companion shows nothing until its whole plan is written.
                Bench.didOfPlan(asked, Bench.Timing(first, total, total, asked.exchanges.size, turns.sumOf { it.read }.takeIf { turns.isNotEmpty() }, turns.sumOf { it.wrote }.takeIf { turns.isNotEmpty() }))
            } finally {
                chat = null
            }
        }
    }

    /** 0.4's companion: one conversation per ask (the bench's pocket is fresh each time), her reply streamed, timed to its first token and to her first words shown. */
    private suspend fun talks(mind: Mind, ask: Bench.Ask, pages: com.phenomemental.pocketbeach.bsp.Pages, storage: com.phenomemental.pocketbeach.bsp.MemoryStorage, held: Boolean): Bench.Did {
        val now = System.currentTimeMillis()
        val instruction = Talks.instruction(HelloWorld.SHELL, Shells(storage, app.shelf).wear(HelloWorld.SHELL, now).window, Asks.plainWindow(pages, HelloWorld.SHELL), pages, examplesInText = false)
        val started = System.nanoTime()
        return mind.talker(instruction, Talks.opening(emptyList()), held).use { t ->
            talker = t
            try {
                if (held) route = t.route
                var shown: Long? = null
                val answered = Talks.ask(ask.question, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { System.currentTimeMillis() }) { words ->
                    if (shown == null && words.isNotBlank()) shown = (System.nanoTime() - started) / 1_000_000
                }
                val total = (System.nanoTime() - started) / 1_000_000
                val turns = t.pace.turns
                val first = turns.firstOrNull()?.firstTokenSeconds?.let { (it * 1000).toLong() }
                Bench.didOfTalks(
                    answered,
                    Bench.Timing(first, shown ?: total, total, answered.exchanges.size, turns.sumOf { it.read }.takeIf { turns.isNotEmpty() }, turns.sumOf { it.wrote }.takeIf { turns.isNotEmpty() }),
                    offered = if (answered.route == Talks.Route.TALK) Talks.keepIn(ask.question, pages) else null,
                )
            } finally {
                talker = null
            }
        }
    }

    /** One conversation kept for three asks, as Ask keeps it: how soon her first words come once the instruction has been read. */
    private suspend fun kept(mind: Mind): String {
        val (storage, pages) = Bench.fixture(app.shelf, System.currentTimeMillis(), Talks.SHELL_RECIPE)
        val instruction = Talks.instruction(HelloWorld.SHELL, Shells(storage, app.shelf).wear(HelloWorld.SHELL, System.currentTimeMillis()).window, Asks.plainWindow(pages, HelloWorld.SHELL), pages, examplesInText = false)
        return runCatching {
            mind.talker(instruction, Talks.opening(emptyList())).use { t ->
                val times = listOf("What's your name?", "What can you do?", "Thanks, that's all for now.").map { q ->
                    val started = System.nanoTime()
                    var first: Long? = null
                    Talks.ask(q, t.stream, pages, storage, app.shelf, HelloWorld.SHELL_OPENING, HelloWorld.seal(), { System.currentTimeMillis() }) { words ->
                        if (first == null && words.isNotBlank()) first = (System.nanoTime() - started) / 1_000_000
                    }
                    Bench.secs(first ?: ((System.nanoTime() - started) / 1_000_000)) + " s"
                }
                times.joinToString(", then ")
            }
        }.getOrElse { "(it failed: ${it.message})" }
    }

    private fun phone(): String {
        val memory = ActivityManager.MemoryInfo().also { app.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem
        return "The phone: ${Build.MANUFACTURER} ${Build.MODEL}, ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}, " +
            String.format(Locale.UK, "%.1f", memory / 1e9) + " GB, Android ${Build.VERSION.RELEASE}; Pocket beach ${app.packageManager.getPackageInfo(app.packageName, 0).versionName}."
    }

    private fun companion(mind: Mind): String = when (mind) {
        is Gemma -> "Gemma 4 E2B (revision 6e5c4f1e) on LiteRT-LM 0.18.0, a context of ${Asks.Tuning.MAX_TOKENS} tokens, speculative decoding " +
            (if (Gemma.SPECULATIVE) "on" else "off")
        else -> "a stand-in, as the device tests run it"
    }
}
