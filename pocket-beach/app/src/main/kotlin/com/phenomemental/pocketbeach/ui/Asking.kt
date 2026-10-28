package com.phenomemental.pocketbeach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Asked
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Carried
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Shells
import com.phenomemental.pocketbeach.model.Mind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One Ask at a time, from Matthew's words to the entry it leaves in the log.
 * Held by the app, not the screen, so an Ask goes on while he looks
 * elsewhere and is still there when he comes back.
 */
class Asking(private val app: PocketApp) {
    sealed interface Stage {
        data object Writing : Stage
        class Thinking(val since: Long) : Stage
        /** The companion's answer, and what it proposes, waiting on Yes or Not now. */
        class Shown(val asked: Asked, val took: Long) : Stage
        class Declining(val shown: Shown) : Stage
        class Carrying(val shown: Shown) : Stage
        /** The writes landed (or one didn't); the post, if one was offered, waits on its own Yes. */
        class Written(val shown: Shown, val carried: Carried) : Stage
        class Posting(val written: Written, val message: PoolMessage) : Stage
        /** The Ask is over and logged: [note] says how it ended. */
        class Over(val shown: Shown?, val carried: Carried?, val posted: Posted?, val note: String) : Stage
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var chat: Mind.Chat? = null

    /** Told whenever the Ask changes the pocket, so the pages show it. */
    var changed: () -> Unit = {}

    var question by mutableStateOf("")
    var stage by mutableStateOf<Stage>(Stage.Writing)
        private set
    private var asked: String = ""

    /** The shell's opening line as it stands in the pocket. */
    fun shellOpening(): String = app.pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    fun ask() {
        val q = question.trim()
        if (q.isEmpty() || stage !is Stage.Writing && stage !is Stage.Over) return
        asked = q
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch {
            val started = System.nanoTime()
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val instruction = Asks.instruction(HelloWorld.SHELL, Companion.window(app), Asks.plainWindow(app.pages, HelloWorld.SHELL), app.pages)
                    app.mind.chat(instruction).use { c ->
                        chat = c
                        try {
                            Asks.ask(q, c.talk, app.pages, app.storage, app.shelf, shellOpening(), HelloWorld.seal()) { System.currentTimeMillis() }
                        } finally {
                            chat = null
                        }
                    }
                }
            }
            val took = (System.nanoTime() - started) / 1_000_000
            outcome.onFailure { e ->
                if (stage is Stage.Thinking) end(null, null, null, "The companion couldn't answer: ${e.message ?: e::class.simpleName}.", took)
            }.onSuccess { a ->
                val shown = Stage.Shown(a, took)
                when {
                    a.problem != null -> end(shown, null, null, "No answer could be read: ${a.problem}", took)
                    a.proposal?.asks != true -> end(shown, null, null, a.proposal?.refusal?.let { "It couldn't go ahead: $it" } ?: "Nothing to say yes to.", took)
                    else -> stage = shown
                }
            }
        }
    }

    fun stop() {
        chat?.stop()
        job?.cancel()
        end(null, null, null, "Stopped before the companion answered.", null)
    }

    fun notNow() {
        val s = stage as? Stage.Shown ?: return
        stage = Stage.Declining(s)
    }

    fun because(reason: String?) {
        val d = stage as? Stage.Declining ?: return
        end(d.shown, null, null, if (reason == null) "Matthew said not now." else "Matthew said not now: $reason.", d.shown.took)
    }

    fun back() {
        val d = stage as? Stage.Declining ?: return
        stage = d.shown
    }

    fun yes() {
        val s = stage as? Stage.Shown ?: return
        val proposal = s.asked.proposal ?: return
        stage = Stage.Carrying(s)
        scope.launch {
            val carried = withContext(Dispatchers.IO) { Asks.carryOut(proposal, app.pages, app.storage, shellOpening()) }
            changed()
            val written = Stage.Written(s, carried)
            when {
                carried.problem != null -> end(s, carried, null, "Matthew said yes. ${carried.problem}", s.took)
                carried.post == null -> end(s, carried, null, "Matthew said yes; " + landed(carried) + (carried.postRefusal?.let { " The post couldn't be offered: $it" } ?: ""), s.took)
                else -> stage = written
            }
        }
    }

    fun post() {
        val w = stage as? Stage.Written ?: return
        val message = w.carried.post ?: return
        stage = Stage.Posting(w, message)
        scope.launch {
            val posted = withContext(Dispatchers.IO) { app.post(message) }
            val said = when (posted) {
                is Posted.Landed -> "He said yes to the post: it landed at entry ${posted.slot} of pool:${posted.pool}."
                is Posted.Refused -> "He said yes to the post, but it didn't go: ${posted.why}"
            }
            end(w.shown, w.carried, posted, "Matthew said yes; " + landed(w.carried) + " " + said, w.shown.took)
        }
    }

    fun dontPost() {
        val w = stage as? Stage.Written ?: return
        end(w.shown, w.carried, null, "Matthew said yes; " + landed(w.carried) + " He said not now to the post.", w.shown.took)
    }

    /** Forget the Ask in hand without logging it, as the device tests begin. */
    fun reset() {
        job?.cancel()
        question = ""
        stage = Stage.Writing
    }

    /** Begin again, the last Ask's words kept to change or clear. */
    fun again() {
        if (stage is Stage.Over) stage = Stage.Writing
    }

    private fun landed(c: Carried): String =
        c.landed.groupBy { it.step.block }.entries.joinToString("; ") { (block, writes) ->
            val at = writes.mapNotNull { it.plan.landsAt?.takeIf { a -> a.isNotEmpty() } }
            if (at.isEmpty()) "$block was written" else "$block was written at ${at.joinToString(", ")}"
        }.ifEmpty { "nothing was written" } + "."

    private fun end(shown: Stage.Shown?, carried: Carried?, posted: Posted?, note: String, took: Long?) {
        stage = Stage.Over(shown, carried, posted, note)
        val exchanges = shown?.asked?.exchanges?.size ?: 0
        val time = took?.let { "Took %.1f s to answer".format(it / 1000.0) + (app.mind.processor?.let { p -> ", on the $p" } ?: "") + ", in ${Words.count(exchanges, "exchange")}." }
            ?: "Stopped."
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { Asks.log(app.pages, Asks.entry(asked, Asks.proposed(shown?.asked?.proposal, shown?.asked?.unasked), note, time)) } }
            changed()
        }
    }

    /** What the companion is shown of the shell it wears: the shell put on as the play door puts it on, offline. */
    object Companion {
        fun window(app: PocketApp): String = Shells(app.storage, app.shelf).wear(HelloWorld.SHELL, System.currentTimeMillis()).window
    }
}
