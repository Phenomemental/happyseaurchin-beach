package com.phenomemental.pocketbeach.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Copies
import com.phenomemental.pocketbeach.bsp.Hatch
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.isoString
import java.io.File

/**
 * 0.8, the hatch (spine:pocket-beach 1.1111787133), as Ask carries it: his
 * shell on this phone, born or adopted, before she wears it.
 *
 * - Born: until she can talk, the app's own words ask "Hello. Are you new
 *   here, or do you have a passport from the beach?" with I'm new and I
 *   have a passport. New, the welcome asks his handle and his three lines,
 *   one at a time in the chat's own box, then sets out a passport whole for
 *   his yes; then Make my shell.
 * - Adopted: a passport file, shared in or chosen in Ask, shown as his
 *   places in lines with when it was read; his three lines (for Matthew,
 *   the ones drafted with Claude from his passport, his words only, for his
 *   yes); then Adopt this as my shell on this phone. By his handle on the
 *   beach waits for the switch.
 *
 * Before anything changes, the app saves a whole copy of the pocket in its
 * own files, which the Shore can bring back.
 */
class Hatching(private val app: PocketApp) {
    sealed interface Step {
        /** The app's own words ask whether he is new, or has a passport. */
        data object Opening : Step
        /** The welcome: [asked] is the question in hand (0 his handle, 1 who he is, 2 his heart, 3 how he'd be met); [answers] so far. */
        class Welcome(val asked: Int, val answers: List<String>, val adopting: Adopting? = null) : Step
        /** Born: his passport set out whole, and his three lines, for his yes; [yes] once he has said it to the passport. */
        class Born(val handle: String, val passport: JsObj, val three: Hatch.Three, val yes: Boolean = false) : Step
        /** Adopted: his passport as read from [from] at [at], his handle, and his three lines once they stand. */
        class Adopting(val read: Hatch.Read, val from: String, val at: Long, val handle: String, val three: Hatch.Three?) : Step
        data object Working : Step
        class Done(val handle: String, val copy: String, val hidden: List<String>) : Step
        class Failed(val why: String, val back: Step) : Step
    }

    var step by mutableStateOf<Step>(Step.Opening)
        private set

    /** The handle he has hatched as, or null before. */
    var hatched by mutableStateOf<String?>(null)
        private set

    /** Whether the hatch has been looked for since the app started. */
    var known by mutableStateOf(false)
        private set

    /** What went wrong with the last answer, in plain words: shown under the question. */
    var note by mutableStateOf<String?>(null)
        private set

    /** Look again at whether he has hatched. Called off the main thread. */
    fun refresh() {
        hatched = runCatching { Hatch.hatched(app.storage) }.getOrNull()
        known = true
        if (hatched != null) correct()
    }

    /** 0.8.1: what was corrected in a shell 0.8 adopted, said once in the thread; null when nothing was. */
    var corrected by mutableStateOf<String?>(null)
        private set

    /**
     * 0.8.1 (spine:pocket-beach 1.1111787136 (a), (b)): a shell 0.8 adopted
     * is corrected once: its opening, which the template filled as locked,
     * said truly; his third line, if it still holds the draft he changed.
     * A whole copy of the pocket is saved first. Called off the main thread.
     */
    private fun correct() {
        val plans = runCatching { Hatch.corrections(app.pages, app.storage) }.getOrDefault(emptyList())
        if (plans.isEmpty()) return
        val copy = runCatching { saveCopy(System.currentTimeMillis()) }.getOrNull() ?: return
        val done = plans.map { app.pages.carryOut(it) }
        corrected = if (done.all { it.ok })
            "Your shell is corrected: " + plans.joinToString("; ") { if (it.address.isEmpty()) "its opening now says truly that it isn't locked" else "her window's third place now reads as you changed it" } +
                ". A whole copy of the pocket was saved first (${copy.name})."
        else "Your shell couldn't all be corrected: " + done.filter { !it.ok }.joinToString("; ") { it.problem ?: it.reply } + ". A whole copy was saved first (${copy.name})."
        app.asking.hatchedNow()
    }

    /** The question the welcome asks now, in the app's own words. */
    val question: String?
        get() = (step as? Step.Welcome)?.let { QUESTIONS[it.asked] }

    /** The welcome takes the chat's box: his answer goes here, not to her. */
    val takesTheBox: Boolean get() = step is Step.Welcome

    fun new() {
        note = null
        step = Step.Welcome(0, emptyList())
    }

    fun back() {
        note = null
        step = Step.Opening
    }

    /** His answer to the welcome's question: kept, and the next asked; false when it isn't one. Called on the main thread. */
    fun answer(said: String): Boolean {
        val w = step as? Step.Welcome ?: return false
        val a = said.trim()
        if (w.asked == 0) Hatch.handleRefusal(a)?.let { note = it; return true }
        if (a.isEmpty()) { note = "Write it in your own words first."; return true }
        note = null
        val answers = w.answers + a
        if (w.asked < 3) {
            step = Step.Welcome(w.asked + 1, answers, w.adopting)
            return true
        }
        val ad = w.adopting
        step = if (ad != null) {
            val (who, heart, met) = answers.takeLast(3)
            Step.Adopting(ad.read, ad.from, ad.at, ad.handle, Hatch.Three(heart = heart, who = who, met = met))
        } else {
            val (handle, who, heart, met) = answers
            Step.Born(handle, Hatch.bornPassport(handle, who, System.currentTimeMillis()), Hatch.Three(heart = heart, who = who, met = met))
        }
        return true
    }

    /** Yes to the passport set out whole: then Make my shell. */
    fun yesToPassport() {
        val b = step as? Step.Born ?: return
        step = Step.Born(b.handle, b.passport, b.three, yes = true)
    }

    /** A passport file, shared in or chosen: read as it stands, data and never instructions. Called off the main thread. */
    fun read(text: String, fileName: String?) {
        note = null
        step = try {
            val r = Hatch.readPassport(text, fileName)
            val handle = r.handle ?: ""
            Step.Adopting(r, fileName ?: "a file", System.currentTimeMillis(), handle, if (handle == Hatch.MATTHEW_HANDLE) Hatch.MATTHEW else null)
        } catch (e: Copies.Unreadable) {
            Step.Failed(e.message ?: "That file isn't a passport.", Step.Opening)
        }
    }

    /** The handle the adopted passport is kept under, as he writes it. */
    fun handle(typed: String) {
        val a = step as? Step.Adopting ?: return
        val h = typed.trim()
        step = Step.Adopting(a.read, a.from, a.at, h, if (a.three == null && h == Hatch.MATTHEW_HANDLE) Hatch.MATTHEW else a.three)
    }

    /** He'd rather write his three lines himself: the welcome asks them, after his handle. */
    fun ownLines() {
        val a = step as? Step.Adopting ?: return
        Hatch.handleRefusal(a.handle)?.let { note = it; return }
        note = null
        step = Step.Welcome(1, listOf(a.handle), Step.Adopting(a.read, a.from, a.at, a.handle, null))
    }

    /**
     * Make his shell: a whole copy of the pocket saved first, in the app's
     * own files; then the four blocks written as planned and the hidden
     * region opened. Called off the main thread.
     */
    fun make(): Step {
        val s = step
        val (handle, passport, three, how) = when (s) {
            is Step.Born -> if (!s.yes) return s else Quad(s.handle, s.passport, s.three, "Born on this phone at the welcome, at ${isoString(System.currentTimeMillis())}.")
            is Step.Adopting -> {
                val three = s.three ?: return s
                Quad(s.handle, s.read.block, three, "Adopted on this phone from ${s.from}, read at ${isoString(s.at)}.")
            }
            else -> return s
        }
        step = Step.Working
        val result = runCatching {
            val nowMs = System.currentTimeMillis()
            val planned = Hatch.plan(handle, passport, three, app.pages, app.storage, nowMs, how)
            if (!planned.ok) return@runCatching Step.Failed(planned.refusal ?: "His shell couldn't be planned.", s)
            val copy = saveCopy(nowMs)
            val done = Hatch.carryOut(planned, app.pages, app.storage)
            if (done.problem != null) Step.Failed("${done.problem} A whole copy of the pocket was saved first (${copy.name}); the Shore can bring it back.", s)
            else Step.Done(handle, copy.name, done.hidden)
        }.getOrElse { Step.Failed("His shell couldn't be made: ${it.message ?: it::class.simpleName}.", s) }
        step = result
        refresh()
        if (result is Step.Done) app.asking.hatchedNow()
        return result
    }

    /** Forget the hatch in hand, as the device tests begin. */
    fun reset() {
        step = Step.Opening
        note = null
        corrected = null
        hatched = null
        known = false
    }

    /** Leave a failure, back to where it came from. */
    fun again() {
        (step as? Step.Failed)?.let { step = it.back }
    }

    /** The whole pocket, saved before the hatch changes anything: the app's own file, never a block. */
    private fun saveCopy(nowMs: Long): File {
        val dir = File(app.filesDir, COPIES).apply { mkdirs() }
        val file = File(dir, "before-the-hatch-${isoString(nowMs).replace(":", "-")}.json")
        file.writeText(app.pages.saveCopy(), Charsets.UTF_8)
        return file
    }

    /** The copies saved before a hatch, newest first. */
    fun copies(): List<File> = (File(app.filesDir, COPIES).listFiles() ?: emptyArray()).filter { it.isFile && it.name.endsWith(".json") }.sortedByDescending { it.name }

    private data class Quad(val handle: String, val passport: JsObj, val three: Hatch.Three, val how: String)

    companion object {
        /** The app's own words, before she can talk. */
        const val OPENING = "Hello. Are you new here, or do you have a passport from the beach?"

        const val COPIES = "copies"

        val QUESTIONS = listOf(
            "A handle for your shell: one word, as the beach writes names (a letter first, then letters, digits, - or _).",
            "Who are you, in a line of your own words? It opens your passport.",
            "Your heart: one line she holds first, in your own words.",
            "How would you like her to meet you? A line in your own words.",
        )
    }
}
