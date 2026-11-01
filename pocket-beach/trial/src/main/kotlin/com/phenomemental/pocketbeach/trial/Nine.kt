@file:OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)

package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.OpenApiTool
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.ai.edge.litertlm.tool
import com.phenomemental.pocketbeach.bsp.BlockStorage
import com.phenomemental.pocketbeach.bsp.Cook
import com.phenomemental.pocketbeach.bsp.CookActs
import com.phenomemental.pocketbeach.bsp.DirShelfSource
import com.phenomemental.pocketbeach.bsp.Pages
import com.phenomemental.pocketbeach.bsp.Shelf
import java.io.File
import java.time.ZonedDateTime

/**
 * Rockpool on the bench: the blocks the phone runs (assets/procedures, the
 * same files the app packs), walked by the same walker; her rings through the
 * engine's own tool call, as Gemma.choose makes them on the phone.
 */
internal val PROCEDURES: Shelf by lazy {
    val shelf = Shelf(DirShelfSource(File(System.getProperty("pocket.procedures") ?: "assets/procedures")))
    // The bench runs the blocks the phone runs: a shipped cook this walker can't read whole is a bench out of step with them, never a run.
    val refused = Cook.parse(shelf.load(Cook.SHIPPED_COOK)).refused
    check(refused.isEmpty()) { "The shipped cook doesn't read whole with this build, at ${refused.joinToString("; ")}: rebuild the bench from the tree." }
    shelf
}

internal class BenchWorld(override val pages: Pages, override val storage: BlockStorage, override val now: ZonedDateTime, override val big: Boolean, override val before: List<String>) : Cook.World {
    override val handle = H
    override val shelf: Shelf = PROCEDURES
    override fun write(card: Cook.Card): String? = CookActs.carryOut(card, H, pages, storage, PROCEDURES, now.toInstant().toEpochMilli())
}

/** Her choice from one ring, through the tool call: the place's number, or null when her answer isn't whole. */
internal fun choose(engine: Engine, question: String, ring: Cook.Ring, message: String, seed: Int): Int? {
    val places = ring.places
    val t = object : OpenApiTool {
        override fun getToolDescriptionJsonString() = """{"name":"choose","description":"Choose the one place in the ring.","parameters":{"type":"object","properties":{"place":{"type":"integer","description":"The place's number in the ring, 1 to ${places.size}"}},"required":["place"]}}"""
        override fun execute(p0: String): String = "unused"
    }
    val q = Regex("""THE [A-Z][A-Z ]*[A-Z]""").replace(question) { "his words" }
    val instruction = "Choose $q, from this ring:\n" + places.mapIndexed { i, p -> "${i + 1} $p" }.joinToString("\n") + "\nCall choose with the place."
    return engine.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of(instruction), tools = listOf(tool(t)), automaticToolCalling = false,
            samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed), maxOutputToken = 60,
            thinkingConfig = ThinkingConfig(enableThinking = false),
        ),
    ).use { c ->
        val m = runCatching { c.sendMessage(ring.about.ifBlank { message }) }.getOrNull()
        m?.toolCalls?.firstOrNull { it.name == "choose" }?.arguments?.get("place")?.toString()?.toDoubleOrNull()?.toInt()?.takeIf { it in 1..places.size }
    }
}

/** His message walked from his cook (the shipped one, as a pocket just brought up has it), her rings served. */
internal fun walkFor(engine: Engine, pages: Pages, storage: BlockStorage, now: ZonedDateTime, before: List<String>, said: String, seed: Int): Cook.Walk {
    val loaded = Cook.load(H, storage, PROCEDURES)
    val w = Cook.Walk(loaded.parsed, BenchWorld(pages, storage, now, BIG, before), said).start(loaded.kernel.message ?: "1")
    while (true) {
        val her = w.waiting as? Cook.Wait.Her ?: break
        w.her(her.rings.map { r -> choose(engine, her.question, r, said, seed) })
    }
    return w
}

/** What a walk did, for the judged file: the trace, and what it set out beneath her reply. */
internal fun walkNote(w: Cook.Walk): String = buildString {
    appendLine("His cook, step by step:")
    for (t in w.trace) appendLine("- $t")
    val card = (w.waiting as? Cook.Wait.Him)?.card
    append("Set out beneath her reply: " + when (card) {
        null -> if (w.writingCheck) "nothing yet; the writing check looks after her reply" else "nothing"
        is Cook.Card.Keep -> "the keeping of “${card.words}” in ${card.block}, for his yes"
        is Cook.Card.Post -> "a post of his own words, “${card.words}”, for his tap, with no model call"
        is Cook.Card.New -> "a new block called ${card.block}, its opening “${card.words}”, for his yes"
        is Cook.Card.Name -> "her name, ${card.name}, in his shell, for his yes"
        is Cook.Card.Offer -> "the card “${card.title}”, for his tap"
        is Cook.Card.Ring -> "the ring of his places, for him to choose: " + card.places.joinToString(", ")
        is Cook.Card.Day -> "his day: " + card.landing.describe()
    })
}

// ── point 6's proofs on the bench, and the cases a fresh model walks beside the walker ──

private fun benchPocket(shelf: Shelf, nowMs: Long, passport: com.phenomemental.pocketbeach.bsp.JsObj? = null): Pair<com.phenomemental.pocketbeach.bsp.MemoryStorage, Pages> {
    val (storage, pages) = com.phenomemental.pocketbeach.bsp.Bench.fixture(shelf, nowMs, com.phenomemental.pocketbeach.bsp.Talks.SHELL_RECIPE)
    check(pages.carryOut(pages.planAdd(com.phenomemental.pocketbeach.bsp.HelloWorld.SHELL, "", "Matthew calls me Gemma.")).ok)
    if (passport != null) hatched(storage, pages, nowMs, passport) else hatched(storage, pages, nowMs)
    for (b in listOf("films", "book")) check(pages.carryOut(pages.planNewBlock(b)).ok)
    check(pages.carryOut(pages.planAdd("films", "", "The pilot: my first episode, rendering now.")).ok)
    return storage to pages
}

private fun edited(edit: (com.phenomemental.pocketbeach.bsp.JsObj) -> Unit): Cook.Parsed {
    val b = com.phenomemental.pocketbeach.bsp.parseJson(com.phenomemental.pocketbeach.bsp.stringify(PROCEDURES.load(Cook.SHIPPED_COOK))!!) as com.phenomemental.pocketbeach.bsp.JsObj
    edit(b)
    return Cook.parse(b)
}

private fun node(b: com.phenomemental.pocketbeach.bsp.Js?, vararg keys: String) = keys.fold(b) { n, k -> com.phenomemental.pocketbeach.bsp.jsGet(n, k) }

/** The three proofs of point 6 on the bench pocket, each a block edited and the behaviour changed, no code changed. */
fun proofs(shelf: Shelf, out: File) {
    val now = ZonedDateTime.of(2026, 11, 1, 17, 30, 0, 0, java.time.ZoneId.of("Europe/London"))
    val ms = now.toInstant().toEpochMilli()
    val shipped = Cook.parse(PROCEDURES.load(Cook.SHIPPED_COOK))
    val w = StringBuilder("# Rockpool on the bench: point 6's proofs\n\nThe blocks the phone runs (assets/procedures), on a pocket hatched as his, walked with no model; where a step is hers, the ring she is offered is shown.\n")
    fun walk(cook: Cook.Parsed, said: String, before: List<String> = emptyList(), big: Boolean = true): Cook.Walk {
        val (s, p) = benchPocket(shelf, ms)
        return Cook.Walk(cook, BenchWorld(p, s, now, big, before), said).start("1")
    }
    fun trace(k: Cook.Walk) = k.trace.joinToString("\n") { "    $it" }
    fun card(k: Cook.Walk) = walkNote(k).lines().last()

    val said = "Send a message to my room: back on Tuesday"
    val withoutSend = edited { b -> (b["9"] as com.phenomemental.pocketbeach.bsp.JsObj)["2"] = com.phenomemental.pocketbeach.bsp.str(com.phenomemental.pocketbeach.bsp.jsString(node(b, "9", "2")).replace("send … to my room, ", "")) }
    val a1 = walk(withoutSend, said)
    val a2 = walk(shipped, said)
    w.appendLine("\n## (a) A cue line edited\n\nHis words: “$said”.\n\n**With “send … to my room” taken out of cook 9.2:** the answer is ${a1.answer}.\n\n${trace(a1)}\n\n${card(a1)}\n\n**With it, as shipped:** the answer is ${a2.answer}.\n\n${trace(a2)}\n\n${card(a2)}")

    val keep = "Keep that for me please"
    val before = listOf("Jo's doing the music for the pilot.")
    val b1 = walk(shipped, keep, before)
    val moved = edited { b ->
        val ring = node(b, "2", "5", "2", "1") as com.phenomemental.pocketbeach.bsp.JsObj
        ring["1"] = com.phenomemental.pocketbeach.bsp.str("films"); ring["2"] = com.phenomemental.pocketbeach.bsp.str("book"); ring["3"] = com.phenomemental.pocketbeach.bsp.str("his stash"); ring["4"] = com.phenomemental.pocketbeach.bsp.str("his other blocks, in his manifest's order")
    }
    val b2 = walk(moved, keep, before)
    fun ring(k: Cook.Walk) = (k.waiting as? Cook.Wait.Her)?.rings?.firstOrNull()?.places?.mapIndexed { i, p -> "${i + 1} $p" }?.joinToString(", ") ?: "(no ring)"
    val r1 = ring(b1)
    val r2 = ring(b2)
    w.appendLine("\n## (b) The KEEP ring reordered\n\nHis words: \u201c$keep\u201d, after \u201c${before[0]}\u201d.\n\n**As shipped, his stash first:** the ring she is offered is $r1.\n\n**His stash moved to third, after films and book (cook 2.5.2.1 rewritten):** $r2.")

    val post = "Post this message to my room: \"Back on Tuesday\""
    val c1 = walk(shipped, post)
    val rewritten = edited { b -> (b["3"] as com.phenomemental.pocketbeach.bsp.JsObj)["3"] = com.phenomemental.pocketbeach.bsp.str("Hand her this line: “The app: his words are set out beneath for his tap. Say only: “It's there for your tap.””") }
    val c2 = walk(rewritten, post)
    w.appendLine("\n## (c) A step rewritten\n\nHis words: “$post”. Cook 3.3, the line POST hands her, rewritten.\n\n**As shipped:** she is handed “${c1.handed.single().line}”\n\n${trace(c1)}\n\n**Rewritten:** she is handed “${c2.handed.single().line}”\n\n${trace(c2)}")
    out.writeText(w.toString())
    println(w)
}

/**
 * The cases a fresh model walks beside the walker (spine:code-as-blocks 1.3):
 * each his message, what he said before, the pocket as it stands, and the
 * choices that are hers and the taps that are his, given to both; the
 * walker's trace beside them. Written as JSON for the kernel to read.
 */
fun kernelCases(shelf: Shelf, passport: com.phenomemental.pocketbeach.bsp.JsObj, out: File) {
    val now = ZonedDateTime.of(2026, 11, 1, 17, 30, 0, 0, java.time.ZoneId.of("Europe/London"))
    val ms = now.toInstant().toEpochMilli()
    class Case(val name: String, val start: String, val said: String, val before: List<String>, val her: List<List<Int?>>, val yes: Boolean)
    val cases = listOf(
        Case("keep", "1", "Keep that for me please", listOf("I've been making huge progress with my film making."), listOf(listOf(1)), true),
        Case("post", "1", "Post this message to my room: \"Back on Tuesday\"", emptyList(), emptyList(), true),
        Case("post, no words", "1", "Send a post to my room.", emptyList(), emptyList(), false),
        Case("make", "1", "Make a block called sailing: knots I'm learning", emptyList(), emptyList(), true),
        Case("read", "1", "What does my passport say about where things go?", emptyList(), emptyList(), false),
        Case("her name", "1", "Your name is Ada", emptyList(), emptyList(), true),
        Case("talk", "1", "Hello Gemma, how are you this evening?", emptyList(), emptyList(), false),
        Case("tell my day", "6", "Took the dog out with Sam this afternoon. I felt calm for the first time this week. I need to ring Jo tomorrow about the music. This month is all about the pilot.", emptyList(), listOf(listOf(1, 2, 5, 4)), true),
    )
    val arr = com.phenomemental.pocketbeach.bsp.JsArr(mutableListOf())
    for (c in cases) {
        val (s, p) = benchPocket(shelf, ms, passport)
        val pocket = com.phenomemental.pocketbeach.bsp.JsObj.of(*listOf("films", "book", "stash:$H", "passport:$H", "shell:$H").mapNotNull { n -> s.read(n)?.let { n to com.phenomemental.pocketbeach.bsp.parseJson(it) } }.toTypedArray())
        val blocks = p.shore().mine.map { it.name }
        val loaded = Cook.load(H, s, PROCEDURES)
        val w = Cook.Walk(loaded.parsed, BenchWorld(p, s, now, true, c.before), c.said).start(c.start)
        var i = 0
        while (true) {
            val wait = w.waiting
            if (wait is Cook.Wait.Her) { w.her(c.her[i++]); continue }
            if (wait is Cook.Wait.Him && c.yes && (wait.card !is Cook.Card.Offer)) { w.yes(); continue }
            break
        }
        arr.items += com.phenomemental.pocketbeach.bsp.JsObj.of(
            "name" to com.phenomemental.pocketbeach.bsp.str(c.name),
            "event" to com.phenomemental.pocketbeach.bsp.str(if (c.start == "1") "his message (kernel 1: run cook 1)" else "a tap on “Sort as my day”: run cook 6"),
            "message" to com.phenomemental.pocketbeach.bsp.str(c.said),
            "before" to com.phenomemental.pocketbeach.bsp.JsArr(c.before.map<String, com.phenomemental.pocketbeach.bsp.Js?> { com.phenomemental.pocketbeach.bsp.str(it) }.toMutableList()),
            "today" to com.phenomemental.pocketbeach.bsp.str("Sunday 1 November 2026, 17:30, Europe/London"),
            "model" to com.phenomemental.pocketbeach.bsp.str("E4B: a ring goes to her"),
            "her choices" to com.phenomemental.pocketbeach.bsp.str(c.her.joinToString("; ") { r -> r.joinToString(", ") { it?.toString() ?: "none" } }.ifEmpty { "none asked" }),
            "his answer" to com.phenomemental.pocketbeach.bsp.str(if (c.yes) "yes, or tap, to whatever is set out (not to “Sort as my day”)" else "none given"),
            "his blocks, in his manifest's order" to com.phenomemental.pocketbeach.bsp.JsArr(blocks.map<String, com.phenomemental.pocketbeach.bsp.Js?> { com.phenomemental.pocketbeach.bsp.str(it) }.toMutableList()),
            "pocket" to pocket,
            "walker" to com.phenomemental.pocketbeach.bsp.JsArr(w.trace.map<Cook.Trace, com.phenomemental.pocketbeach.bsp.Js?> { t ->
                com.phenomemental.pocketbeach.bsp.JsObj.of("at" to com.phenomemental.pocketbeach.bsp.str(t.at), "reader" to com.phenomemental.pocketbeach.bsp.str(t.reader.name.lowercase()), "did" to com.phenomemental.pocketbeach.bsp.str(t.did),
                    "carried" to com.phenomemental.pocketbeach.bsp.JsObj.of(*t.carried.map { (k, v) -> k to com.phenomemental.pocketbeach.bsp.str(v) }.toTypedArray()))
            }.toMutableList()),
            "handed her" to com.phenomemental.pocketbeach.bsp.JsArr(w.handed.map<Cook.Handed, com.phenomemental.pocketbeach.bsp.Js?> { com.phenomemental.pocketbeach.bsp.str(it.line) }.toMutableList()),
            "set out" to com.phenomemental.pocketbeach.bsp.str(walkNote(w).lines().last()),
        )
    }
    out.writeText(com.phenomemental.pocketbeach.bsp.stringifyPretty(arr, " ") + "\n")
}
