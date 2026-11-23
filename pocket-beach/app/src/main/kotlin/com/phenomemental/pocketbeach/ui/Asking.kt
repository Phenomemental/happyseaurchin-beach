package com.phenomemental.pocketbeach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Acts
import com.phenomemental.pocketbeach.bsp.Answer
import com.phenomemental.pocketbeach.bsp.Asked
import com.phenomemental.pocketbeach.bsp.Chats
import com.phenomemental.pocketbeach.bsp.WalkTool
import com.phenomemental.pocketbeach.bsp.Walking
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.Carried
import com.phenomemental.pocketbeach.bsp.Close
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.CookActs
import com.phenomemental.pocketbeach.bsp.Days
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.HerWindow
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Keep
import com.phenomemental.pocketbeach.bsp.parseJson
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.Talking
import com.phenomemental.pocketbeach.bsp.Talks
import com.phenomemental.pocketbeach.model.Job as MindJob
import com.phenomemental.pocketbeach.model.JobSettings
import com.phenomemental.pocketbeach.model.Steps
import com.phenomemental.pocketbeach.model.Mind
import com.phenomemental.pocketbeach.model.Pace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

/**
 * One Ask at a time, from Matthew's words to the entry it leaves in the log.
 * Held by the app, not the screen, so an Ask goes on while he looks
 * elsewhere and is still there when he comes back.
 *
 * 0.4: Ask is a conversation, kept while the Ask screen is open and carrying
 * the last six exchanges; her words stream into the page as she writes them.
 * A line of her reply that plays her turn back is never shown (0.6.1).
 *
 * 0.6.3, a plain chat: [stage] is the turn in hand only, his next line
 * being written or her reply coming. Her reply shows and the box is ready
 * at once. What waits on his answer (a proposal) waits on its own exchange
 * in the thread ([Exchange.waiting]) while he carries on.
 *
 * Sundial 0.12, the plain chat (spine:pocket-beach 1.111178719993): his
 * message walks no recipe and sets out no card; her reply is all that
 * follows his words. Keep today, the greeting and the close still walk his
 * cook.
 */
class Asking(private val app: PocketApp) {
    sealed interface Stage {
        data object Writing : Stage
        class Thinking(val since: Long) : Stage
        /** The companion's answer, and what it proposes, waiting on Yes or Not now; [pace], where and how fast it answered; [firstShown], when her first words showed. */
        class Shown(val asked: Asked, val took: Long, val pace: Pace?, val firstShown: Long? = null) : Stage
        class Declining(val shown: Shown) : Stage
        class Carrying(val shown: Shown) : Stage
        /** The writes landed (or one didn't); the post, if one was offered, waits on its own Yes. */
        class Written(val shown: Shown, val carried: Carried) : Stage
        /** Sundial 0.11: he said yes, but a block it writes changed by another hand since it was shown, so nothing was written: planned again, or let go. */
        class Held(val shown: Shown, val carried: Carried) : Stage
        class Posting(val written: Written, val message: PoolMessage) : Stage
        /** The exchange is over and logged: [note] says how it ended. */
        class Over(val shown: Shown?, val carried: Carried?, val posted: Posted?, val note: String) : Stage
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    /** A job's conversation, kept while Ask is open, and the settings it was opened with. */
    private class Kept(val talker: Mind.Talker, val settings: JobSettings)

    /** Each job's conversation; the jobs whose pocket has changed since their instruction was made. */
    private val kept = java.util.concurrent.ConcurrentHashMap<MindJob, Kept>()
    private val stale: MutableSet<MindJob> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** Whether the two conversations can be kept side by side on this phone; once they can't, each is opened in turn. */
    @Volatile private var together = true
    private val talking = Mutex()

    /** Whether a job's conversation is in use now: for the device tests to say, when a wait times out. */
    val lockedForTests: Boolean get() = talking.isLocked

    /** Her words so far, as they stream in. */
    var live by mutableStateOf("")
        private set

    /**
     * An exchange of the conversation, as the thread shows it: his words, her
     * reply, and what came of it. 0.6.3: what waits on his answer waits here
     * ([waiting]) while he carries on.
     */
    class Exchange(val question: String, val at: Long = System.currentTimeMillis()) {
        /** Sundial 0.12: the files he added to this message, read in as text beneath his words; shown as chips on it. */
        var files: List<Chats.Attached> = emptyList()
            internal set
        /** Sundial 0.13: the blocks his words named, the walker offered her on this turn only; and what she walked, for the chip on her reply. */
        var named: List<String> = emptyList()
            internal set
        var walked: List<String> = emptyList()
            internal set
        /** Her reply, as shown: never a line playing her turn back. */
        var said by mutableStateOf("")
            internal set
        /** How it ended, once it has; empty while it hasn't. */
        var note by mutableStateOf("")
            internal set
        /** Her answer's time and chip, for Details and the log. */
        var time by mutableStateOf("")
            internal set
        /** What came of a proposal; null when there was none. */
        var outcome by mutableStateOf<String?>(null)
            internal set
        /** What waits on his answer beneath her reply: a proposal, or the post after it. */
        var waiting by mutableStateOf<Stage?>(null)
            internal set
        /** How it ended, with what was written and posted, once it has. */
        var over by mutableStateOf<Stage.Over?>(null)
            internal set
        /** The block his words named when she only talked: the app offers to keep them there ("Keep this in Million?"). */
        var keepIn by mutableStateOf<String?>(null)
            internal set
        /** 0.6.1, for the saved conversation: which model and settings answered each job. */
        var jobs = ""
            internal set
        /** The proposal whole, as he saw it; null when there was none. */
        var proposal: String? = null
            internal set
        /** The lines of her reply left out, as her turn played back. */
        var copied: List<String> = emptyList()
            internal set
        /** How long her reply took, where it ran, when her first words showed. */
        internal var took: Long? = null
        internal var pace: Pace? = null
        internal var firstShown: Long? = null
        /** Her turn as the engine had it: his words and hers. */
        internal var talk: List<Pair<String, String>> = emptyList()

        /** The time stamped on his message in her turn, as the beach gives it (HerWindow.stamp), for the workings. */
        var stamp by mutableStateOf<String?>(null)

        /** Sundial 0.10.1: each step of this answer, timed, for the workings. */
        var steps by mutableStateOf<List<String>>(emptyList())

        /** Sundial 0.10.1: a plain line beneath her turn when her reply didn't come, or was stopped; never a blank. */
        var unfinished by mutableStateOf<String?>(null)
            internal set

        /** Sundial 0.10.5: beneath her reply, always, when she ran on the CPU because the GPU couldn't take her ([Pace.SLOWER]). */
        var slower by mutableStateOf<String?>(null)
            internal set
        internal val answeredBy = linkedSetOf<MindJob>()
        internal val jobMs = java.util.concurrent.ConcurrentHashMap<MindJob, Long>()
    }

    /** What waits on his answer, the newest. */
    val waiting: Stage? get() = history.lastOrNull { it.waiting != null }?.waiting

    /** Nothing in hand: her last reply is in, and it is over or waits on his answer. For the device tests. */
    val settled: Boolean get() = stage !is Stage.Thinking && history.lastOrNull()?.let { it.over != null || it.waiting != null } == true

    /** Her reply coming, or something he said yes to being written: the bench waits. */
    val busy: Boolean get() = stage is Stage.Thinking || history.any { it.waiting is Stage.Carrying || it.waiting is Stage.Posting }

    /**
     * 0.7: her conversation is kept while Ask is open, her window read once
     * (spine:pocket-beach 1.1111781). It is opened afresh on a new day, after
     * [TURNS] messages (so it never outgrows her context, 8,192 tokens by default), and whenever
     * the pocket changes by a write.
     */
    private var windowDay: java.time.LocalDate? = null
    private var talkTurns = 0

    /**
     * 0.8, the close (spine:pocket-beach 1.1111787133): what this conversation
     * was, for the one history entry the app appends when it closes (a new
     * day, six messages, a write, as 0.7 reopens it, or Ask left).
     */
    private class Conversation {
        val said = mutableListOf<String>()
        val read = mutableListOf("her window")
        val written = mutableListOf<String>()
    }
    private var conversation = Conversation()

    /**
     * Rockpool: what his cook's walker needs of the app, for Keep today, the
     * greeting and the close. His day and a whisper are carried out by the
     * app's own path, through the guards; his day is written whole.
     */
    private inner class AppWorld(override val handle: String, override val before: List<String>, private val day: java.time.LocalDate? = null) : Cook.World {
        override val pages get() = app.pages
        override val storage get() = app.storage
        override val shelf get() = app.procedures
        override val now: ZonedDateTime get() = ZonedDateTime.now()
        override val big: Boolean get() = app.running(MindJob.TALK).model == JobSettings.GEMMA_E4B
        /** Sundial 0.10: the beach's day (UTC), one today everywhere a day is meant (spine:pocket-beach 1.111178719915); yesterday's when the card offers it first. */
        override val keepDay: java.time.LocalDate get() = day ?: Keep.dayOf(System.currentTimeMillis())
        override fun moments(): List<Keep.Moment> = momentsOf(keepDay)
        override fun cards(): Map<String, String> = aheadCards(handle)
        override fun write(card: Cook.Card): String? =
            if (card is Cook.Card.Day || card is Cook.Card.Whisper) CookActs.carryOut(card, handle, app.pages, app.storage, app.shelf, System.currentTimeMillis()) else null
        override fun send(words: String): String? = null
        override fun close(): String? = closing?.let { t ->
            runCatching { Close.plan(t, app.pages, app.storage)?.let { plan -> app.pages.carryOut(plan).let { if (it.ok) null else it.problem ?: "it didn't land" } } }.getOrElse { it.message }
        }
        var closing: Close.Talk? = null
    }

    /**
     * Sundial 0.10: his moments of [day], each with its instant: those kept
     * in his now and those waiting for Keep today, each once.
     */
    fun momentsOf(day: java.time.LocalDate): List<Keep.Moment> {
        val kept = runCatching {
            val clock = app.storage.read(Days.NOW)?.let(::parseJson)
            Keep.days(clock, null, day).lastOrNull { it.date == day }?.moments.orEmpty().mapNotNull { k -> k.atMs?.let { Keep.Moment(k.words, it) } }
        }.getOrDefault(emptyList())
        return (kept + app.waiting.of(day)).distinctBy { it.atMs to it.words }.sortedBy { it.atMs }
    }

    /** His cards on his ahead, by pile and place, for "news of a card". */
    private fun aheadCards(handle: String): Map<String, String> = runCatching {
        val a = app.storage.read(Days.aheadOf(handle))?.let(::parseJson) as? JsObj ?: return@runCatching emptyMap<String, String>()
        buildMap { for (p in 1..9) (a["$p"] as? JsObj)?.let { pile -> for (k in 1..9) pile["$k"]?.let { c -> com.phenomemental.pocketbeach.bsp.collectUnderscore(c) ?: (c as? com.phenomemental.pocketbeach.bsp.JsStr)?.value }?.takeIf { it.isNotBlank() }?.let { put("$p.$k", it) } } }
    }.getOrDefault(emptyMap())

    /** Rockpool: her choices for the rings the walk waits on, each a short call through the engine's tool call, under the same lock as her talk. */
    private suspend fun serveHer(w: Cook.Walk) {
        while (true) {
            // Sundial 0.10: her placing, a list through the tool call; her sheet for the day, through the tool call on E4B, else free.
            when (val wait = w.waiting) {
                is Cook.Wait.HerMany -> {
                    if (wait.moments.isNotEmpty()) {
                        // Sundial 0.10.3: each moment keeps the places she named in her reply to it; only those she hasn't placed are
                        // asked, all at once, in free text the app reads (on his Pixel on 4 November the places tool call took 10.4 s).
                        val ask = wait.unplaced
                        val placed = if (ask.isEmpty()) emptyMap() else runCatching {
                            val started = System.nanoTime()
                            val a = talking.withLock { app.mind.ask(com.phenomemental.pocketbeach.bsp.Placing.instruction(wait.question, wait.places), com.phenomemental.pocketbeach.bsp.Placing.material(ask), app.running(MindJob.TALK)) }
                            com.phenomemental.pocketbeach.bsp.Placing.read(a.text, ask).also {
                                app.mind.steps?.add("the places, ${Words.count(ask.size, "moment")} she hadn't placed asked in free text, ${it.size} placed", Steps.since(started))
                            }
                        }.getOrNull()
                        w.herEach(placed)
                        continue
                    }
                    val ring = wait.places.mapIndexed { i, p -> "${i + 1} $p" }.joinToString("\n")
                    val instruction = "Name every place of this ring his words plainly touch, one or more, never ranked: ${wait.question}.\n$ring"
                    val chosen = runCatching { talking.withLock { app.mind.chooseMany(instruction, wait.about, wait.places.size, app.running(MindJob.TALK)) } }.getOrNull()
                    w.herMany(chosen)
                    continue
                }
                is Cook.Wait.HerSheet -> {
                    // Sundial 0.10.3: written free and read by the app, never through the tool call: on the laptop's E4B free was as fast or
                    // faster (18.8 s against 30.8 s; 15.0 s against 15.5 s), and the tool's own description said "the second person" over his line.
                    val started = System.nanoTime()
                    val sheet = runCatching { talking.withLock { app.mind.sheet(wait.line, wait.material, app.running(MindJob.TALK), tool = false) } }.getOrNull()
                    app.mind.steps?.add("the card's sheet, written free" + (if (sheet == null) ": no title came back" else ""), Steps.since(started))
                    w.sheet(sheet)
                    continue
                }
                else -> {}
            }
            val her = w.waiting as? Cook.Wait.Her ?: return
            val choices = her.rings.map { r ->
                val ring = r.places.mapIndexed { i, p -> "${i + 1} $p" }.joinToString("\n")
                val question = Regex("""THE [A-Z][A-Z ]*[A-Z]""").replace(her.question) { "his words" }
                val instruction = "Choose $question, from this ring:\n$ring"
                runCatching { talking.withLock { app.mind.choose(instruction, r.about.ifBlank { w.carried["THE MESSAGE"]?.toString() ?: "" }, r.places.size, app.running(MindJob.TALK)) } }.getOrNull()
            }
            w.her(choices)
        }
    }

    /**
     * The instant of his message before [atMs]: the last in this run of the
     * app, else the last still waiting for Keep today (said in a run before);
     * null when there is none.
     */
    private fun lastMessageBefore(atMs: Long): Long? =
        (history.map { it.at } + runCatching { app.waiting.all.map { it.atMs } }.getOrDefault(emptyList())).filter { it < atMs }.maxOrNull()

    /** 0.8: the handle his shell on this phone is of, once he has hatched; null before. */
    fun hatched(): String? = runCatching { Hatch.hatched(app.storage) }.getOrNull()

    /**
     * 0.8: the conversation closes: one entry in his history, its opening a
     * plain concern line, what was read and written beneath, written by the
     * app. Nothing when nothing was said, or before the hatch.
     */
    private fun closeConversation() {
        val c = conversation
        conversation = Conversation()
        if (c.said.isEmpty()) return
        val talk = Close.Talk(c.said.toList(), c.read.toList(), c.written.toList())
        // Rockpool: the close is his cook's CLOSE, as his kernel names it.
        hatched()?.let { h ->
            runCatching {
                val loaded = Cook.load(h, app.storage, app.procedures)
                val world = AppWorld(h, emptyList()).also { it.closing = talk }
                Cook.Walk(loaded.parsed, world, "").start(loaded.kernel.close ?: "7")
            }.onFailure { android.util.Log.w("Asking", "The close's history entry couldn't be written.", it) }
            return
        }
        runCatching {
            Close.plan(talk, app.pages, app.storage)?.let { plan ->
                val done = app.pages.carryOut(plan)
                if (!done.ok) android.util.Log.w("Asking", "The close's history entry didn't land: ${done.problem}")
            }
        }.onFailure { android.util.Log.w("Asking", "The close's history entry couldn't be written.", it) }
    }

    /**
     * Sundial 0.10.3 (spine:pocket-beach 1.111178719929): one yes's own entry
     * in his history, written as it lands, never left for a conversation to
     * close. Off the main thread.
     */
    private fun recordYes(what: String, written: List<String>, refused: List<String>) {
        val h = hatched() ?: return
        Close.recordYes(h, what, written, refused, app.pages)?.let { android.util.Log.w("Asking", "A yes's history entry didn't land: $it") }
    }

    /** 0.8: he has just hatched: both conversations are opened afresh, hers wearing his shell. */
    fun hatchedNow() {
        conversation = Conversation()
        stale += MindJob.values()
        // Sundial 0.12: her model is loaded only once he has hatched.
        opened()
    }

    /** The app's copies of his blocks as she last saw them: what tells her what's changed since. The app's own file, never a block. */
    private val copies get() = java.io.File(app.filesDir, COPIES)

    /** The exchanges so far, oldest first. */
    val history = mutableStateListOf<Exchange>()

    /** Each job's last six exchanges as they went, carried into its conversation opened afresh. */
    private val context = MindJob.values().associateWith { ArrayDeque<Pair<String, String>>() }

    /** Told whenever the Ask changes the pocket, so the pages show it. */
    var changed: () -> Unit = {}

    var question by mutableStateOf("")
    var stage by mutableStateOf<Stage>(Stage.Writing)
        private set

    /** The shell's opening line as it stands in the pocket. */
    fun shellOpening(): String = app.pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING

    /** While the bench runs, Ask waits: the bench loads the model its own ways, and a conversation open here would be cut off. */
    val benchRunning: Boolean get() = app.benching.running

    /** How many times Ask has been opened: a close asked for before the latest opening lets nothing go. */
    @Volatile private var openings = 0

    /**
     * Sundial 0.12.1 (spine:pocket-beach 1.1111787199992): Sundial has opened.
     * Once he has hatched, her talk mind is loaded now, the model his talk
     * runs on, so his first reply is as quick as the rest; before the hatch
     * nothing is loaded. Called as the app's window is made.
     */
    fun sundialOpened() {
        if (benchRunning) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                talking.withLock {
                    if (hatched() == null) return@withLock
                    val settings = app.running(MindJob.TALK)
                    if (app.mind.state(settings.model) !is com.phenomemental.pocketbeach.model.ModelState.Ready) return@withLock
                    app.mind.warm(settings)
                }
            }
        }
    }

    /** Ask is open: load her model now, while Matthew writes (already loaded when Sundial opened, after the hatch). */
    fun opened() {
        openings++
        if (benchRunning) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                talking.withLock {
                    // Sundial 0.12 (spine:pocket-beach 1.111178719992): the welcome is the app's own; no model is loaded before he has hatched.
                    if (hatched() == null) return@withLock
                    app.mind.warm(app.running(MindJob.TALK))
                    // Sundial 0.10.2: her one conversation opens with his first message (or her greeting): opening one is instant, and the
                    // engine reads her window at the first message either way.
                }
            }
        }
    }

    /** Let the conversations go now, before the bench loads the model its own ways. */
    suspend fun letGo() {
        talking.withLock { closeAll() }
    }

    /** Close both jobs' conversations. Called holding [talking], or as Ask is reset. */
    /** The close of the Ask last left, still writing his history perhaps. */
    @Volatile private var closing: Job? = null

    /** For the device tests: wait for the close of the Ask last left, so the next test's pocket is its own. */
    fun awaitClosing() {
        kotlinx.coroutines.runBlocking { closing?.join() }
    }

    private fun closeAll() {
        kept.values.forEach { runCatching { it.talker.close() } }
        kept.clear()
    }

    /**
     * The pocket changed by a write: a conversation whose instruction shows it
     * is opened afresh at the next ask. Sundial 0.10.2: once he has hatched,
     * never hers: her one conversation opens again only when her recipe, the
     * model or the day changes.
     */
    private fun pocketChanged() {
        stale += MindJob.values().filter { it != MindJob.TALK || hatched() == null }
    }

    /**
     * Ask is closed: let the conversation go (the model goes after a few idle
     * minutes). A close and an opening run on other threads in either order
     * (as when Ask is left and opened again at once): a close is let go once
     * Ask has been opened since, or it would shut the conversation just
     * opened, to be opened again at the first ask.
     */
    fun closed() {
        if (stage is Stage.Thinking) return
        val at = openings
        closing = scope.launch(Dispatchers.IO) {
            talking.withLock {
                if (openings != at) return@withLock
                // 0.8: Ask left: the conversation closes into his history.
                closeConversation()
                // Sundial 0.10.2: once he has hatched, her one conversation stays open while he looks elsewhere in the app; it is the
                // one he comes back to, opened again only when her recipe, the model or the day changes (or at New conversation).
                if (hatched() != null) kept.keys.filter { it != MindJob.TALK }.forEach { job -> kept.remove(job)?.let { runCatching { it.talker.close() } } }
                else closeAll()
            }
        }
    }

    /**
     * [job]'s conversation, opened afresh if the pocket or the job's settings
     * have changed since it was. Should the phone not open it beside the
     * other's, the other is let go, and from then on each is opened in turn.
     * Called holding [talking].
     */
    private suspend fun talker(job: MindJob): Mind.Talker {
        val settings = app.running(job)
        kept[job]?.let { k ->
            if (job !in stale && k.settings == settings) return k.talker
            runCatching { k.talker.close() }
            kept.remove(job)
        }
        if (!together) kept.keys.filter { it != job }.forEach { other -> kept.remove(other)?.let { runCatching { it.talker.close() } } }
        val open = suspend {
            when (job) {
                // Sundial 0.12: only her conversation is kept here; the bench asks the Write and Sort jobs its own way.
                MindJob.SORT, MindJob.WRITE -> error("Only her conversation is kept in Ask.")
                // 0.6.1: the date, the time and her shell in her instruction, opened afresh for each message; the earlier exchanges his words and hers.
                // 0.8: the conversation before hers closes into his history.
                MindJob.TALK -> {
                    closeConversation()
                    val now = ZonedDateTime.now()
                    windowDay = now.toLocalDate()
                    talkTurns = 0
                    val h = hatched()
                    // Sundial 0.12, Gemma released (spine:pocket-beach 1.111178719991): her window is the time alone, before the hatch and after;
                    // no heart, no three lines, no recipe opens her conversation.
                    app.mind.talker(HerWindow.released(now), context.getValue(job).toList(), settings = settings)
                }
            }
        }
        val t = try {
            open()
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException || kept.isEmpty()) throw e
            android.util.Log.w("Asking", "The two conversations couldn't be kept side by side; each is opened in turn from now on.", e)
            together = false
            closeAll()
            open()
        }
        kept[job] = Kept(t, settings)
        stale.remove(job)
        return t
    }

    /**
     * He says the next thing: the box empties, her reply streams in, and the
     * box is ready again as soon as it has. What was offered under her last
     * reply lapses; a proposal waiting stays, for his answer.
     */
    fun ask() {
        val q = question.trim()
        if (q.isEmpty() || benchRunning || stage is Stage.Thinking) return
        // 0.8: the welcome asks in the chat's own box; his answer is the hatch's, not hers.
        if (app.hatching.takesTheBox) {
            if (app.hatching.answer(q)) question = ""
            return
        }
        question = ""
        // Sundial 0.10.1: the bring-up line goes once he sends a message.
        app.hatching.dismissNotice()
        history.lastOrNull()?.keepIn = null
        val e = Exchange(q)
        e.files = attached.toList()
        attached.clear()
        history += e
        live = ""
        e.answeredBy += MindJob.TALK
        stage = Stage.Thinking(System.currentTimeMillis())
        job = scope.launch {
            val started = System.nanoTime()
            var pace: Pace? = null
            var firstShown: Long? = null
            val h = withContext(Dispatchers.IO) { hatched() }
            // Sundial 0.10: his words and their instant wait for Keep today, before his cook walks them (spine:pocket-beach 1.111178719915).
            if (h != null) runCatching { app.waiting.add(q, e.at) }
            // Sundial 0.12, the plain chat (spine:pocket-beach 1.111178719993): his message walks no recipe and sets out no card;
            // the recipes and skills wait for 0.13. Her reply is all that follows his words.
            val readied = app.mind.steps?.take().orEmpty().map { "before his message, $it" }
            val walkSteps = readied + app.mind.steps?.take().orEmpty()
            // Her reply, streamed as she writes it.
            val talked = withContext(Dispatchers.IO) {
                runCatching {
                    talking.withLock {
                        val now = ZonedDateTime.now()
                        // 0.7: her conversation is kept; opened afresh on a new day, or when it has run long.
                        // Sundial 0.10.2: once he has hatched, one conversation, opened again only when the recipe, the model or the day changes,
                        // or when it nears its context; the model's change is talker's own (its settings).
                        if (windowDay != now.toLocalDate()) stale += MindJob.TALK
                        if (h == null && talkTurns >= TURNS) stale += MindJob.TALK
                        kept[MindJob.TALK]?.talker?.tokens()?.let { held -> if (held > app.running(MindJob.TALK).context * 3 / 4) stale += MindJob.TALK }
                        val t = talker(MindJob.TALK)
                        val before = t.pace.turns.size
                        val first = history.size == 1
                        conversation.said += q
                        conversation.read += e.files.map { "his ${it.name}" }
                        // Sundial 0.10.2: in her one conversation, only his new message and what is new with it; her recipe is in her instruction.
                        // Every one of his messages carries its own time, as the beach gives it; her conversation is never opened again for it.
                        val saidAt = java.time.Instant.ofEpochMilli(e.at).atZone(now.zone)
                        val last = lastMessageBefore(e.at)
                        e.stamp = HerWindow.stamp(saidAt, last)
                        // Sundial 0.12, Gemma released: her turn is the time and his words, and nothing added to shape how she answers.
                        val message = HerWindow.releasedTurn(Chats.withFiles(q, e.files), saidAt, last)
                        val echo = HerWindow.echoLines(message)
                        // Sundial 0.12: the echo filter is given only the app's own lines (the time it stamps on his turn); his words are hers to say.
                        val opening = ""
                        val words = StringBuilder()
                        val hisWords = false
                        val handed = emptyList<String>()
                        // Never shown: a line of hers that plays her turn back. Never taken out of view: a line once shown (Sundial 0.10.1).
                        val shown = Talking.Shown(q, opening, echo, hisWords, handed)
                        // Sundial 0.10.2: her reply written free, streamed as she writes it. Her places line, if she gives one, is never shown.
                        val piece: (String) -> Unit = { p ->
                            words.append(p)
                            val now = shown.of(words.toString())
                            if (firstShown == null && now.isNotBlank()) firstShown = (System.nanoTime() - started) / 1_000_000
                            live = now
                        }
                        // Sundial 0.13 (spine:pocket-beach 1.1111787199994): when his words name a block of his, on this turn only she is
                        // given the walker as one native tool, thinking off, its words naming the blocks his words named (the only blocks
                        // it takes); the app walks what she asks and hands her readable text.
                        e.named = runCatching { Walking.named(q, app.pages.shore().mine.map { it.name }, h) }.getOrDefault(emptyList())
                        val walks = mutableListOf<Walking.Walked>()
                        val reply = if (e.named.isEmpty()) t.stream.say(message, piece)
                        else t.sayWithTool(message, WalkTool.forBlocks(e.named), onCpu = false, call = { c ->
                            Walking.walk(app.pages, c.arguments, e.named, h).also { walks += it }.text
                        }, piece = piece).reply
                        e.walked = walks.map { it.said }
                        conversation.read += walks.filter { it.found }.map { "his ${it.chip}" }
                        pace = t.pace.copy(if (first) t.pace.loadMs else null).also { p ->
                            p.turns.addAll(t.pace.turns.drop(before))
                            app.cpuWhy(MindJob.TALK)?.takeIf { Pace.GPU_FAILED !in p.notes }?.let { p.notes += "Talk ran on the CPU: $it." }
                        }
                        // 0.6.2: the GPU failed her answer and that engine was closed: every conversation on it is opened again, on the CPU.
                        if (Pace.GPU_FAILED in t.pace.notes) stale += MindJob.values()
                        talkTurns++
                        // She has now seen his blocks as they stand.
                        runCatching { copies.writeText(HerWindow.copies(app.pages, app.storage)) }
                        e.copied = Talking.copied(reply, q, opening, echo, hisWords)
                        // 0.6.1 holds in a kept conversation: her turn played back is never given her again, so it is opened afresh with what was shown.
                        // Sundial 0.10.2: once he has hatched, her one conversation stays; what she played back is never shown.
                        if (e.copied.isNotEmpty() && h == null) stale += MindJob.TALK
                        e.jobMs[MindJob.TALK] = (System.nanoTime() - started) / 1_000_000
                        val whole = shown.of(reply, over = true)
                        // A short reply shows whole at its end: its first words are shown then.
                        if (firstShown == null && whole.isNotBlank()) firstShown = (System.nanoTime() - started) / 1_000_000
                        message to whole
                    }
                }
            }
            if (stage !is Stage.Thinking) return@launch
            e.took = (System.nanoTime() - started) / 1_000_000
            e.pace = pace
            e.slower = pace?.slower
            e.firstShown = firstShown
            val (message, said) = talked.getOrElse { ex ->
                // Sundial 0.10.1: what she had shown stays in view, and a plain line says she couldn't finish: never a blank.
                if (live.isNotBlank()) e.said = live
                e.unfinished = UNFINISHED
                live = ""
                stage = Stage.Writing
                finish(e, null, "The companion couldn't answer: ${ex.message ?: ex::class.simpleName}.")
                return@launch
            }
            // Sundial 0.10.1: never less than she showed while it came; and a reply with no words is said plainly, never a blank.
            e.said = said.ifBlank { live }
            if (e.said.isBlank()) e.unfinished = UNFINISHED
            // Sundial 0.10.1: each step timed, then the whole from his message to her first words on screen.
            e.steps = walkSteps + app.mind.steps?.take().orEmpty() + listOfNotNull(
                firstShown?.let { "her first words on screen, from his message: " + Steps.secs(it) },
                "her reply whole, from his message: " + Steps.secs(e.took ?: 0),
            )
            e.talk = listOf(message to said)
            e.time = timeLine(e, pace, e.talk.size)
            live = ""
            // What she said, never her turn played back, is what she is given again: his words and hers.
            remember(MindJob.TALK, Talking.said(q), said.ifBlank { "…" })
            keepTalk()
            // Sundial 0.12, the plain chat: nothing is set out beneath her reply; the box is ready.
            stage = Stage.Writing
            answered(e)
        }
    }

    // ── Sundial 0.10: Keep today, the greeting, and the day before ──

    /**
     * Sundial 0.10.1: a day just kept, as the conversation shows it: its
     * shell (the coil in the day's colours, as sundial.html folds the sheet
     * into it) and its title beneath; a tap opens the day's page.
     */
    data class KeptShell(val day: java.time.LocalDate, val title: String, val places: List<Int>) {
        val address: String get() = Keep.dayAddress(day)
    }

    private fun shellOf(card: Days.DayCard): KeptShell = KeptShell(
        card.day,
        card.sheet?.title?.takeIf { it.isNotBlank() } ?: card.his?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
            ?: card.day.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.UK)),
        card.places,
    )

    /** Sundial 0.10.1: the day kept last on the dial, shown as its shell; null when none this run. */
    var keptShell by mutableStateOf<KeptShell?>(null)
        private set

    /** The keep card Keep today set out (kernel KEEP), waiting on Keep it or Not now: its walk and the card as he has changed it. */
    class KeepCard(val walk: Cook.Walk, val card: Days.DayCard, val fellBack: String? = null)

    /** The keep card set out on Today, if one waits. */
    var keepCard by mutableStateOf<KeepCard?>(null)
        private set

    /** Keep today's walk is drafting the card, or Keep it is writing it. */
    var keeping by mutableStateOf(false)
        private set

    /** What came of the last keep, in plain words ("Kept."), or why nothing was set out. */
    var keepNote by mutableStateOf<String?>(null)
        private set

    /** The beach's day last kept in this run of the app: the dial blooms it. */
    var lastKept by mutableStateOf<java.time.LocalDate?>(null)
        private set

    /**
     * Sundial 0.10: his tap of Keep today (or of the day before, offered
     * first): his kernel's KEEP TODAY event runs his cook's 6.1, her placing
     * and her sheet served as she is asked, and the keep card set out for
     * his yes. Nothing is written until Keep it.
     */
    fun keepToday(day: java.time.LocalDate = Keep.dayOf(System.currentTimeMillis())) {
        if (keeping || benchRunning || stage is Stage.Thinking) return
        val h = hatched() ?: return
        keeping = true
        keepNote = null
        keepCard = null
        scope.launch {
            val made = withContext(Dispatchers.IO) {
                runCatching {
                    val loaded = Cook.load(h, app.storage, app.procedures)
                    val (parsed, place) = loaded.walkFor(Cook.KEEP) ?: (loaded.parsed to "6.1")
                    val w = Cook.Walk(parsed, AppWorld(h, emptyList(), day), "")
                    w.start(place)
                    serveHer(w)
                    w to (loaded.note ?: loaded.fellBack[Cook.KEEP])
                }
            }
            made.onSuccess { (w, why) ->
                val card = ((w.waiting as? Cook.Wait.Him)?.card as? Cook.Card.Day)?.landing
                if (card == null) keepNote = "Nothing is set out: you haven't said anything of this day yet."
                else keepCard = KeepCard(w, card, why)
            }.onFailure { keepNote = "Your day couldn't be set out: ${it.message ?: it::class.simpleName}." }
            keeping = false
        }
    }

    /** Sundial 0.10.3: the cards a keep skipped as already on his hand, said plainly; null when none. */
    private fun alreadySaid(card: Days.DayCard): String? = card.lines.filter { it.already }.map { "“${it.words}”" }.takeIf { it.isNotEmpty() }?.let {
        "${it.joinToString(" and ")} ${if (it.size == 1) "was" else "were"} already on your hand, so not written again."
    }

    /** Sundial 0.10.3: his tap of a place's colour beside his [i]th moment on the keep card: off if it was on, on if it was off. */
    fun placeKeepMoment(i: Int, n: Int) { keepCard?.let { k -> keepCard = KeepCard(k.walk, k.card.placeMoment(i, n), k.fellBack) } }

    /** A place's dot tapped on the keep card. */
    fun placeKeep(n: Int) { keepCard?.let { k -> keepCard = KeepCard(k.walk, k.card.place(n), k.fellBack) } }

    /** A line of his hand, a booking or a rung, ticked or unticked on the keep card. */
    fun tickKeep(i: Int) { keepCard?.let { k -> keepCard = KeepCard(k.walk, k.card.tick(i), k.fellBack) } }

    /** Keep it: the walk goes on from the card, "On his yes, write it."; his moments of that day leave the waiting room (a). */
    fun keepIt() {
        val k = keepCard ?: return
        if (keeping) return
        keeping = true
        scope.launch {
            val wrote = withContext(Dispatchers.IO) {
                runCatching { k.walk.changed(Cook.Card.Day(k.card)); k.walk.yes() }
                k.walk.trace.lastOrNull { it.did.startsWith("wrote it") || it.did.startsWith("couldn't write it") }?.did
            }
            if (wrote == null || wrote.startsWith("wrote it")) {
                app.waiting.kept(k.card.day)
                lastKept = k.card.day
                // Sundial 0.10.1: the day's shell and its title, never the record or a heading.
                keptShell = shellOf(k.card)
                // Sundial 0.10.3: a card on his hand already is said so, never a failure.
                keepNote = alreadySaid(k.card)
                conversation.written += "his day: ${k.card.describe()}"
                pocketChanged()
            } else {
                // Sundial 0.10.3: what landed stands; his moments stay waiting, so Keep today again adds only what is missing.
                keepNote = "Not all of your day could be written: ${wrote.removePrefix("couldn't write it: ")}. Your moments are still waiting; " +
                    "Keep today again writes only what is missing."
                pocketChanged()
            }
            keepCard = null
            keeping = false
            changed()
        }
    }

    /** Not now: nothing is written; his moments still wait. */
    fun notNowKeep() {
        keepCard?.walk?.let { w -> runCatching { w.notNow() } }
        keepCard = null
    }

    /** Sundial 0.10: her one question as the dial opens (kernel THE DIAL OPENED, his cook's 6.3), with his tomorrow line before it. */
    data class Greeting(val day: java.time.LocalDate, val tomorrow: String?, val question: String?)

    var greeting by mutableStateOf<Greeting?>(null)
        private set

    @Volatile private var greetingNow = false

    private val greeted get() = app.getSharedPreferences("sundial", android.content.Context.MODE_PRIVATE)

    /**
     * The dial opened: when he has said nothing today and no day before waits
     * to be kept, she asks her one question about today, in her kept
     * conversation, so his answer follows it. Asked once a day; until she is
     * ready Sundial's own question stands.
     */
    fun greet() {
        val today = Keep.dayOf(System.currentTimeMillis())
        val day = today.toString()
        if (greeting?.day == today) return
        greeted.getString("day", null)?.takeIf { it == day }?.let {
            // Sundial 0.10.1: a greeting kept from before is checked as any reply of hers is, before it is shown again.
            greeting = Greeting(today, greeted.getString("tomorrow", null), greeted.getString("question", null)?.let { q -> Talking.oneQuestion(q) })
            return
        }
        if (greetingNow || benchRunning || stage is Stage.Thinking) return
        val h = hatched() ?: return
        if (momentsOf(today).isNotEmpty() || app.waiting.yesterday(today) != null || history.any { Keep.dayOf(it.at) == today }) return
        greetingNow = true
        scope.launch {
            val got = withContext(Dispatchers.IO) {
                runCatching {
                    val loaded = Cook.load(h, app.storage, app.procedures)
                    val (parsed, place) = loaded.walkFor(Cook.GREET) ?: return@runCatching null
                    val world = AppWorld(h, emptyList(), today)
                    val w = Cook.Walk(parsed, world, "")
                    w.start(place)
                    val tomorrow = Keep.tomorrowOf(Keep.lastDay(world.kept(), today))
                    val question = talking.withLock {
                        // Sundial 0.10.1 (item 10): her greeting is asked in a conversation of its own, her window without who he is:
                        // she is never handed his passport for it. On his Pixel on 3 November she copied that line out as her greeting.
                        val now = ZonedDateTime.now()
                        kept.remove(MindJob.TALK)?.let { runCatching { it.talker.close() } }
                        // Sundial 0.12: her window the time alone, as in her conversation.
                        val window = HerWindow.released(now)
                        val message = HerWindow.turnHanded("", now, app.pages, w.handed, lastMs = lastMessageBefore(now.toInstant().toEpochMilli()))
                        // Never shown as hers: the app's own lines (his last day, the greeting's line), "Kept.".
                        val handed = w.handed.flatMap { listOf(it.line) + it.material }
                        val t = app.mind.talker(window, emptyList(), settings = app.running(MindJob.TALK))
                        try {
                            // Sundial 0.10.2: her greeting written free, as every reply of hers is: no tool call.
                            val said = t.stream.say(message) { }
                            // The greeting is his tomorrow line (the app's), then her one question: nothing else.
                            // Sundial 0.10.2: the example in her line is a frame for her question, not a line she plays back: six of its words
                            // in a row ("How did you get on with") are hers to use; eight of anything she was handed never are.
                            Talking.oneQuestion(said, HerWindow.echoLines(message).filter { it.startsWith("It is ") }, handed)
                                // Her conversation holds the question he answers, as it was shown.
                                ?.also { remember(MindJob.TALK, message, it) }
                        } finally {
                            runCatching { t.close() }
                        }
                    }
                    Greeting(today, tomorrow, question)
                }.getOrNull()
            }
            greetingNow = false
            if (got != null) {
                greeting = got
                greeted.edit().putString("day", day).putString("tomorrow", got.tomorrow).putString("question", got.question).apply()
            }
        }
    }

    /** Her reply is in: Sundial 0.12, the plain chat, she talked, and the exchange ends with her words. */
    private fun answered(e: Exchange) {
        val shown = Stage.Shown(asAsked(Talks.Answered(Talks.Route.TALK, e.said, emptyList(), null, null, e.talk)), e.took ?: 0, e.pace, e.firstShown)
        // What he told her names one of his blocks: the app offers to keep it there, while it's the last thing said.
        if (e === history.lastOrNull()) e.keepIn = Talks.keepIn(e.question, app.pages)
        finish(e, shown, "She talked.")
    }

    /** Matthew tapped "Keep this in …?": his words, as he said them, planned as a line added to that block, for Yes. */
    fun keep(e: Exchange? = history.lastOrNull()) {
        val block = e?.keepIn ?: return
        e.keepIn = null
        byApp(e, listOf(Acts.Action.Add(block, null, e.question)), "It couldn't be kept in $block")
    }

    /**
     * A proposal the app makes itself from his words (0.6.4), planned on a
     * copy, for his Yes. Shown as her talk was timed: the app's offer is made
     * in no time.
     */
    private fun byApp(e: Exchange, actions: List<Acts.Action>, couldnt: String) {
        scope.launch {
            val planned = withContext(Dispatchers.IO) {
                Acts.propose("", actions, app.storage, app.shelf, shellOpening(), HelloWorld.seal(), System.currentTimeMillis())
            }
            val a = Talks.Answered(Talks.Route.ACT, "", emptyList(), planned, planned.problem, e.talk, actions)
            val shown = Stage.Shown(asAsked(a), e.took ?: 0, e.pace, e.firstShown)
            when {
                planned.problem != null -> finish(e, shown, "$couldnt: ${planned.problem}")
                planned.proposal?.asks != true -> finish(e, shown, planned.proposal?.refusal?.let { "$couldnt: $it" } ?: "Nothing to say yes to.")
                else -> e.waiting = shown
            }
        }
    }

    /** The exchange whose [waiting] is of [kind], the newest. */
    private inline fun <reified T : Stage> waitingOn(): Exchange? = history.lastOrNull { it.waiting is T }

    /** A job's exchange, kept as context for its next conversation; only the last six. */
    private fun remember(job: MindJob, message: String, reply: String) {
        if (reply.isBlank()) return
        val c = context.getValue(job)
        c.addLast(message to reply)
        while (c.size > 6) c.removeFirst()
    }

    /** 0.4's answer in the shape the proposal's pages show: her words, what she read, the writes and the post. */
    private fun asAsked(a: Talks.Answered): Asked {
        val p = a.planned?.proposal
        return Asked(a.exchanges, a.found, Answer(a.said, p?.answer?.steps.orEmpty(), p?.answer?.post), p, a.problem)
    }

    /** Stop her reply. */
    fun stop() {
        if (stage !is Stage.Thinking) return
        kept.values.forEach { it.talker.stop() }
        // A conversation stopped part way can't be asked again: the next ask opens it afresh.
        stale += MindJob.values()
        job?.cancel()
        // Sundial 0.10.1: the words she had shown stay in view when he stops her.
        history.lastOrNull()?.takeIf { it.said.isEmpty() && live.isNotBlank() }?.let { it.said = live; it.unfinished = STOPPED }
        live = ""
        stage = Stage.Writing
        history.lastOrNull()?.let { e ->
            if (e.over == null && e.waiting == null && e.said.isEmpty()) finish(e, null, "Stopped before the companion answered.")
        }
    }

    fun notNow(e: Exchange? = waitingOn<Stage.Shown>()) {
        val s = e?.waiting as? Stage.Shown ?: return
        e.waiting = Stage.Declining(s)
    }

    fun because(reason: String?, e: Exchange? = waitingOn<Stage.Declining>()) {
        val d = e?.waiting as? Stage.Declining ?: return
        finish(e, d.shown, if (reason == null) "Matthew said not now." else "Matthew said not now: $reason.")
    }

    fun back(e: Exchange? = waitingOn<Stage.Declining>()) {
        val d = e?.waiting as? Stage.Declining ?: return
        e.waiting = d.shown
    }

    fun yes(e: Exchange? = waitingOn<Stage.Shown>()) {
        val s = e?.waiting as? Stage.Shown ?: return
        val proposal = s.asked.proposal ?: return
        e.waiting = Stage.Carrying(s)
        scope.launch {
            val carried = withContext(Dispatchers.IO) {
                Asks.carryOut(proposal, app.pages, app.storage, shellOpening()).also { c ->
                    recordYes("what was set out beneath her reply to “${Cook.shortly(e.question)}”",
                        c.landed.map { w -> "${w.plan.block}${w.plan.landsAt?.takeIf { it.isNotEmpty() }?.let { " at $it" }.orEmpty()}: \u201c${Cook.shortly(w.text ?: w.plan.text)}\u201d" },
                        listOfNotNull(c.problem))
                }
            }
            // Sundial 0.11: held, nothing was written; it waits on him, planned again on this version or let go.
            if (carried.held.isNotEmpty()) {
                e.waiting = Stage.Held(s, carried)
                changed()
                return@launch
            }
            // Her instructions show the pocket as it stood; the next ask opens the conversations afresh.
            if (carried.landed.isNotEmpty()) pocketChanged()
            for (w in carried.landed) conversation.written += "${w.plan.block}${w.plan.landsAt?.takeIf { it.isNotEmpty() }?.let { " at $it" }.orEmpty()}: \u201c${(w.text ?: w.plan.text).take(120)}\u201d"
            changed()
            when {
                carried.problem != null -> finish(e, s, "Matthew said yes. ${carried.problem}", carried)
                carried.post == null -> finish(e, s, "Matthew said yes; " + landed(carried) + (carried.postRefusal?.let { " The post couldn't be offered: $it" } ?: ""), carried)
                else -> e.waiting = Stage.Written(s, carried)
            }
        }
    }

    /** Sundial 0.11: a held proposal planned again on the pocket as it stands now, and shown again for his yes. */
    fun planAgain(e: Exchange? = waitingOn<Stage.Held>()) {
        val h = e?.waiting as? Stage.Held ?: return
        val asked = h.shown.asked
        val answer = asked.answer ?: return
        val seal = asked.proposal?.seal ?: HelloWorld.seal()
        scope.launch {
            val again = withContext(Dispatchers.IO) { Asks.propose(answer, app.storage, app.shelf, shellOpening(), seal, System.currentTimeMillis()) }
            e.waiting = Stage.Shown(Asked(asked.exchanges, asked.found, answer, again, null, asked.unasked), h.shown.took, h.shown.pace, h.shown.firstShown)
            changed()
        }
    }

    /** Sundial 0.11: a held proposal let go: nothing is written. */
    fun letHeldGo(e: Exchange? = waitingOn<Stage.Held>()) {
        val h = e?.waiting as? Stage.Held ?: return
        finish(e, h.shown, "Matthew said yes, but ${h.carried.held.joinToString(", ")} had changed since it was set out, so nothing was written; he let it go.", h.carried)
    }

    fun post(e: Exchange? = waitingOn<Stage.Written>()) {
        val w = e?.waiting as? Stage.Written ?: return
        val message = w.carried.post ?: return
        e.waiting = Stage.Posting(w, message)
        scope.launch {
            val posted = withContext(Dispatchers.IO) { app.post(message) }
            val said = when (posted) {
                is Posted.Landed -> "He said yes to the post: it landed at entry ${posted.slot} of pool:${posted.pool}."
                is Posted.Refused -> "He said yes to the post, but it didn't go: ${posted.why}"
                is Posted.Unreached -> "He said yes to the post, but it didn't go: ${posted.why} It is kept, to be posted only when he taps Post now."
            }
            finish(e, w.shown, "Matthew said yes; " + landed(w.carried) + " " + said, w.carried, posted)
        }
    }

    fun dontPost(e: Exchange? = waitingOn<Stage.Written>()) {
        val w = e?.waiting as? Stage.Written ?: return
        finish(e, w.shown, "Matthew said yes; " + landed(w.carried) + " He said not now to the post.", w.carried)
    }

    /**
     * 0.8.1: a new conversation, as a chat app starts one: the one in hand
     * closes into his history, and the thread begins empty. Nothing waiting
     * on his answer is written.
     */
    fun newConversation() {
        if (stage is Stage.Thinking || benchRunning) return
        question = ""
        live = ""
        stage = Stage.Writing
        history.clear()
        context.values.forEach { it.clear() }
        // Sundial 0.10.1 (item 10): the new conversation opens with the greeting alone: his tomorrow line, then her one question.
        keptShell = null
        keepNote = null
        scope.launch(Dispatchers.IO) {
            talking.withLock {
                closeConversation()
                closeAll()
            }
        }
    }

    /** Sundial 0.12: the files he has added to his next message, shown as chips above the box. */
    val attached = mutableStateListOf<Chats.Attached>()

    /** Sundial 0.12: add a file to his next message, once by its name. */
    fun attach(file: Chats.Attached) {
        if (attached.none { it.name == file.name }) attached += file
    }

    /** Sundial 0.12: his kept conversations, newest first (the app's own files, never blocks). */
    fun keptTalks(): List<Chats.Kept> = runCatching { Chats.all(java.io.File(app.filesDir, TALKS)) }.getOrDefault(emptyList())

    /** Sundial 0.12: the conversation in hand, as kept and shared. */
    fun talkInHand(): Chats.Kept? = history.filter { it.said.isNotBlank() }.takeIf { it.isNotEmpty() }?.let { ex ->
        Chats.Kept(ex.first().at, ex.map { Chats.Said(it.question, it.files, it.said, it.at) })
    }

    /** Sundial 0.12: the conversation in hand kept inside the app after each reply, for the list of older ones. */
    private fun keepTalk() {
        val kept = talkInHand() ?: return
        runCatching { Chats.keep(java.io.File(app.filesDir, TALKS), kept) }
    }

    /** Forget the Ask in hand and the conversation without logging them, as the device tests begin. */
    fun reset() {
        job?.cancel()
        // Sundial 0.12: the files waiting for his next message go with the Ask in hand.
        attached.clear()
        question = ""
        live = ""
        stage = Stage.Writing
        history.clear()
        context.values.forEach { it.clear() }
        // Sundial 0.10: the keep card, and the dial's question as this app run last had it.
        keepCard = null
        keepNote = null
        lastKept = null
        keptShell = null
        greeting = null
        conversation = Conversation()
        together = true
        val all = kept.values.map { it.talker }
        kept.clear()
        stale += MindJob.values()
        scope.launch(Dispatchers.IO) { all.forEach { runCatching { it.close() } } }
    }

    /**
     * The conversation so far as a file of plain text, to keep or send: each
     * exchange, when he said it, her reply, and how it ended and how long it
     * took; first, the settings her jobs ran on. 0.6.3: what still waits on
     * his answer.
     */
    fun transcript(): String = buildString {
        val day = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.UK)
        val time = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.UK)
        fun at(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
        appendLine("Sundial: a conversation in Today, ${day.format(at(history.firstOrNull()?.at ?: System.currentTimeMillis()))}")
        val h = hatched()
        if (h != null) appendLine("Wearing ${Hatch.shellOf(h)}: her window is the time alone.") else appendLine("Wearing ${HelloWorld.SHELL}: \"${shellOpening()}\"")
        for (j in MindJob.values()) appendLine("${j.title}: " + settingsLine(app.running(j)) + app.cpuWhy(j)?.let { " On the CPU: $it." }.orEmpty())
        for (e in history) {
            appendLine()
            appendLine("${time.format(at(e.at))} Matthew: ${e.question}")
            appendLine("Companion: ${e.said.ifBlank { "(nothing of hers shown)" }}")
            e.stamp?.let { appendLine("  The time she was given: $it") }
            e.steps.forEach { appendLine("  Step: $it") }
            e.unfinished?.let { appendLine("  $it") }
            e.slower?.let { appendLine("  $it") }
            if (e.copied.isNotEmpty()) appendLine("  Left out of her reply, as her turn played back: " + e.copied.joinToString(" / "))
            if (e.named.isNotEmpty()) appendLine("  His words named ${e.named.joinToString()}: the walker offered; she " + (e.walked.joinToString("; ").ifEmpty { "walked nothing" }) + ".")
            if (e.jobs.isNotEmpty()) appendLine("  Answered by: ${e.jobs}")
            if (e.time.isNotEmpty()) appendLine("  Times: ${e.time}")
            e.proposal?.let { appendLine("  Proposed:"); appendLine(it.prependIndent("  ")) }
            e.waiting?.let { w -> appendLine("  Waiting on his answer: " + waitingLine(w)) }
            if (e.note.isNotEmpty()) appendLine("  How it ended: ${e.note}")
        }
    }

    /** What waits on his answer, in a line for the saved conversation. */
    private fun waitingLine(w: Stage): String = when (w) {
        is Stage.Shown -> "a proposal.\n" + (describe(w.asked.proposal) ?: "")
        is Stage.Declining -> "why not now.\n" + (describe(w.shown.asked.proposal) ?: "")
        is Stage.Written -> "the post, after the writes: " + landed(w.carried)
        else -> "it was being carried out."
    }.trimEnd()

    /** The name the conversation's file is offered under. */
    fun fileName(): String =
        "ask-" + java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", java.util.Locale.UK).format(java.time.LocalDateTime.now()) + ".txt"

    private fun settingsLine(s: JobSettings): String =
        "${s.model}, ${if (s.processor == "GPU") "the GPU, else the CPU" else "the CPU"}, a context of ${s.context} tokens, " +
            (if (s.greedy) "the likeliest word each time" else "temperature %.2f, top-k %d, top-p %.2f".format(java.util.Locale.UK, s.temperature, s.topK, s.topP)) +
            ", replies of up to ${s.longestReply} tokens."

    private fun landed(c: Carried): String =
        c.landed.groupBy { it.step.block }.entries.joinToString("; ") { (block, writes) ->
            // 0.6.3: where, in words; never by a line's number.
            val beneath = writes.mapNotNull { w -> w.standing?.takeIf { w.plan.kind == com.phenomemental.pocketbeach.bsp.Kind.ADD && w.plan.address.isNotEmpty() } }
            if (beneath.isEmpty()) "$block was written" else "$block was written, beneath " + beneath.joinToString(" and ") { Words.quoted(it) }
        }.ifEmpty { "nothing was written" } + "."

    /** A proposal's writes and post, a line each, as the saved conversation and the log keep them. */
    private fun describe(p: com.phenomemental.pocketbeach.bsp.Proposal?): String? = p?.let {
        it.writes.joinToString("\n") { w -> "  " + Words.landing(w.plan.kind, w.plan.block, w.plan.address, w.plan.landsAt, w.plan.wraps, w.standing) + (w.shown ?: w.plan.text).takeIf { t -> t.isNotBlank() }?.let { t -> " “$t”" }.orEmpty() } +
            (it.post?.let { post -> "\n  Then a post to pool:${post.pool}" }.orEmpty()) + (it.refusal?.let { r -> "\n  It couldn't go ahead: $r" }.orEmpty())
    }

    /** Her answer's time and chip: how long her reply took, where it ran, when her first words showed, and the engine's counts. */
    private fun timeLine(e: Exchange, pace: Pace?, exchanges: Int): String = e.took?.let {
        "Took %.1f s to answer".format(it / 1000.0) + (pace?.where?.takeIf(String::isNotEmpty)?.let { w -> ", $w" } ?: "") +
            (e.firstShown?.let { f -> ", first words after %.1f s".format(f / 1000.0) } ?: "") +
            ", in ${Words.count(exchanges, "exchange")}." + (pace?.detail()?.let { d -> " $d" } ?: "")
    } ?: "Stopped."

    /** [e] is over: how it ended, what was written and posted, its times; logged. */
    private fun finish(e: Exchange, shown: Stage.Shown?, note: String, carried: Carried? = null, posted: Posted? = null) {
        e.waiting = null
        e.over = Stage.Over(shown, carried, posted, note)
        e.note = note
        val time = timeLine(e, shown?.pace ?: e.pace, shown?.asked?.exchanges?.size ?: e.talk.size)
        e.time = time
        val proposedHere = shown?.asked?.proposal != null || shown?.asked?.unasked != null
        e.outcome = if (proposedHere) note else null
        e.jobs = e.answeredBy.joinToString("; ") { j -> "${j.title}" + (e.jobMs[j]?.let { " (%.1f s)".format(java.util.Locale.UK, it / 1000.0) }.orEmpty()) + ": " + settingsLine(app.running(j)) + app.cpuWhy(j)?.let { " On the CPU: $it." }.orEmpty() }
        e.proposal = describe(shown?.asked?.proposal)?.ifEmpty { null }
        scope.launch {
            // 0.6.4: the ask log only when he has turned it on.
            if (app.keepAskLog) withContext(Dispatchers.IO) { runCatching { Asks.log(app.pages, Asks.entry(e.question, Asks.proposed(shown?.asked?.proposal, shown?.asked?.unasked), note, time)) } }
            changed()
        }
    }

    /** What the companion is shown of the shell it wears: the shell put on as the play door puts it on, offline. */
    companion object {
        /** Sundial 0.12: where his conversations are kept, inside the app. */
        const val TALKS = "talks"

        /** 0.7: how many messages her kept conversation carries before it is opened afresh with the last six exchanges. */
        const val TURNS = 6

        /** The app's copies of his blocks as she last saw them, beside the pocket. */
        const val COPIES = "her-copies.json"

        /** Sundial 0.10.1: said plainly beneath her turn when her reply didn't come, or came with no words; never a blank. */
        const val UNFINISHED = "She couldn't finish her reply. Say it again, or carry on."

        /** Sundial 0.10.1: said beneath what she had written when he stopped her. */
        const val STOPPED = "You stopped her before she finished."
    }
}
