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
import com.phenomemental.pocketbeach.model.Gemma
import com.phenomemental.pocketbeach.model.Mind
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
        class Asking(val ask: Bench.Ask) : Stage
        class Probing(val what: String) : Stage
        class Saved(val name: String, val right: Int, val of: Int) : Stage
        class Failed(val why: String) : Stage
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var chat: Mind.Chat? = null

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
                val about = mutableListOf(phone(), "The companion: ${companion(mind)}.")
                val loadMs = withContext(Dispatchers.IO) { mind.warm() }
                about += "Loaded " + (loadMs?.let { "in ${Bench.secs(it)} s" } ?: "already") + (mind.processor?.let { ", on the $it" } ?: "") + "."
                for (ask in Bench.ASKS) {
                    stage = Stage.Asking(ask)
                    results += withContext(Dispatchers.IO) {
                        Bench.one(ask, app.shelf, System.currentTimeMillis()) { a, pages, storage -> runBlocking { plan(mind, a, pages, storage) } }
                    }
                }
                (mind as? Gemma)?.let { gemma ->
                    stage = Stage.Probing("Letting the model go, to try the engine")
                    about += withContext(Dispatchers.IO) { gemma.probes { what -> scope.launch { stage = Stage.Probing(what) } } }
                }
                val now = System.currentTimeMillis()
                val name = "bench-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.UK))
                val done = withContext(Dispatchers.IO) { app.pages.carryOut(app.pages.planKeep(name, Bench.block(results, about, now))) }
                changed()
                stage = if (done.ok) Stage.Saved(name, results.count { it.right }, results.size) else Stage.Failed("The run wasn't kept: ${done.problem}")
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

    private fun phone(): String {
        val memory = ActivityManager.MemoryInfo().also { app.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.totalMem
        return "The phone: ${Build.MANUFACTURER} ${Build.MODEL}, ${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}, " +
            String.format(Locale.UK, "%.1f", memory / 1e9) + " GB, Android ${Build.VERSION.RELEASE}; Pocket beach ${app.packageManager.getPackageInfo(app.packageName, 0).versionName}."
    }

    private fun companion(mind: Mind): String = when (mind) {
        is Gemma -> "Gemma 4 E2B (revision 6e5c4f1e) on LiteRT-LM 0.18.0, as 0.3 asks it: one JSON plan an answer, held to its form, a context of ${Asks.Tuning.MAX_TOKENS} tokens"
        else -> "a stand-in, as the device tests run it"
    }
}
