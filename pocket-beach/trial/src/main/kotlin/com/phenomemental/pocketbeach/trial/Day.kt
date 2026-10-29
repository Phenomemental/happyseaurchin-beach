package com.phenomemental.pocketbeach.trial

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.phenomemental.pocketbeach.bsp.Asks
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.Telling
import java.io.File

/**
 * 0.5's item 7 on the laptop: one telling, three landings. Each telling is put
 * to the pocket (the block, today's date and what he said; the pocket's check;
 * the hand's cards landed for the second telling) and, beside it, to the same
 * Gemma with no pocket (his names as plain text, then the paragraph; the
 * second telling in the same conversation). Once as the phone asks (the
 * likeliest word each time), then five times with Gemma's own sampling, each
 * with its seed. Her answers are kept in [out].
 */
@OptIn(ExperimentalApi::class)
internal fun tellings(engine: Engine, tellings: List<Pair<String, String>>, out: File, runs: Int = 5) {
    out.mkdirs()
    val ways = listOf("as the phone asks" to null) + (1..runs).map { "sampled, seed $it" to it }
    for ((name, said) in tellings) {
        for (pocket in listOf(true, false)) {
            val who = if (pocket) "pocket" else "plain"
            println("\n== $name, $who ==")
            for ((way, seed) in ways) {
                val sampler = if (seed == null) SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0)
                else SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)
                val config = ConversationConfig(
                    systemInstruction = if (pocket) Contents.of(Telling.INSTRUCTION) else null,
                    samplerConfig = sampler,
                    maxOutputToken = Asks.Tuning.MAX_OUTPUT,
                    thinkingConfig = ThinkingConfig(enableThinking = false),
                )
                val tag = "${name.replace(' ', '-')}-$who-${seed ?: 0}"
                engine.createConversation(config).use { c ->
                    val first = send(c, if (pocket) Telling.ask(Telling.shape(), Telling.BENCH_DATE, said) else Telling.plainAsk(said))
                    File(out, "$tag-1.txt").writeText(first.text)
                    val placed = if (pocket) Telling.placed(first.text) else Telling.fromPlain(first.text)
                    val marks = Telling.marks(said, placed)
                    // The second telling: the pocket's to the block with the hand landed; plain Gemma's in the same conversation.
                    val after: JsObj
                    val second: Reply
                    if (pocket) {
                        val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
                        second = engine.createConversation(config).use { c2 -> send(c2, Telling.ask(state, Telling.BENCH_DATE, Telling.SECOND)) }
                        after = Telling.land(state, Telling.check(Telling.SECOND, Telling.placed(second.text)).kept)
                    } else {
                        second = send(c, Telling.SECOND)
                        val hand = Telling.fromPlain(second.text).filter { it.place in Telling.HAND }.ifEmpty { placed.filter { it.place in Telling.HAND } }
                        after = Telling.land(Telling.shape(), hand)
                    }
                    File(out, "$tag-2.txt").writeText(second.text)
                    val all = marks + Telling.secondMarks(after)
                    println("$way: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines; ${first.timing}; then ${second.timing}")
                    for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(400)}")
                    if (pocket) println("  rungs: " + placed.filter { it.place in Telling.RUNGS }.joinToString(" | ") { "${it.place} ${it.words}" })
                }
            }
        }
    }
}

/**
 * The shape for when the one-pass fill drops pieces or misses the doubles:
 * the phone cuts the telling into pieces, copying and never changing a word,
 * and she labels each piece with its places, one short choice at a time, in
 * one conversation; then the rungs, once. The second telling likewise, to the
 * block with the hand landed.
 */
@OptIn(ExperimentalApi::class)
internal fun pieces(engine: Engine, tellings: List<Pair<String, String>>, out: File, runs: Int = 5, mode: String = "alone") {
    out.mkdirs()
    val off = ThinkingConfig(enableThinking = false)
    val ways = listOf("as the phone asks" to null) + (1..runs).map { "sampled, seed $it" to it }
    for ((name, said) in tellings) {
        println("\n== $name, a piece at a time ($mode) ==")
        for ((way, seed) in ways) {
            val sampler = if (seed == null) SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0)
            else SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)
            fun label(state: JsObj, telling: String, log: StringBuilder): Pair<List<Telling.Placed>, String> {
                val pieces = Telling.cut(telling)
                val start = System.nanoTime()
                val firsts = mutableListOf<String>()
                fun note(piece: String, r: Reply) {
                    firsts += r.timing.substringBefore(",").removePrefix("first token ")
                    log.append("\u201c$piece\u201d -> ${r.text.trim().lineSequence().first()}\n")
                }
                val labelled = when (mode) {
                    "alone" -> pieces.map { piece ->
                        val config = ConversationConfig(systemInstruction = Contents.of(Telling.aloneInstruction(Telling.BENCH_DAY, Telling.cards(state))), samplerConfig = sampler, maxOutputToken = Telling.PIECE_TOKENS, thinkingConfig = off)
                        engine.createConversation(config).use { c -> send(c, Telling.aloneAsk(piece, telling)).also { note(piece, it) } }.let { piece to Telling.places(it.text) }
                    }
                    "words" -> pieces.map { piece ->
                        val cards = Telling.cards(state)
                        val config = ConversationConfig(systemInstruction = Contents.of(Telling.wordsInstruction(Telling.BENCH_DAY, cards)), samplerConfig = sampler, maxOutputToken = Telling.PIECE_TOKENS, thinkingConfig = off)
                        engine.createConversation(config).use { c -> send(c, Telling.aloneAsk(piece, telling)).also { note(piece, it) } }.let { piece to Telling.fromWords(it.text, cards) }
                    }
                    "all" -> engine.createConversation(ConversationConfig(samplerConfig = sampler, maxOutputToken = 512, thinkingConfig = off)).use { c ->
                        val r = send(c, Telling.allAsk(state, Telling.BENCH_DAY, telling, pieces))
                        firsts += r.timing.substringBefore(",").removePrefix("first token ")
                        log.append(r.text.trim() + "\n")
                        Telling.fromAll(r.text, pieces)
                    }
                    else -> {
                        val config = ConversationConfig(systemInstruction = Contents.of(Telling.piecesInstruction(state, Telling.BENCH_DAY, telling)), samplerConfig = sampler, maxOutputToken = Telling.PIECE_TOKENS, enableResponseFormat = true, thinkingConfig = off)
                        engine.createConversation(config).use { c -> pieces.map { piece -> send(c, Telling.pieceAsk(piece), Telling.PIECE_FORM).also { note(piece, it) }.let { piece to Telling.places(it.text) } } }
                    }
                }
                val rungs = engine.createConversation(ConversationConfig(samplerConfig = sampler, maxOutputToken = 256, thinkingConfig = off)).use { r ->
                    send(r, Telling.rungsAsk(state, Telling.BENCH_DAY, telling))
                }
                log.append("rungs -> ${rungs.text.trim()}\n")
                return Telling.fromPieces(telling, labelled, rungs.text) to "${pieces.size} pieces, ${"%.1f".format((System.nanoTime() - start) / 1e9)} s in all with the rungs, first tokens ${firsts.take(3).joinToString(", ")}, …"
            }
            val log = StringBuilder()
            val (placed, timing) = label(Telling.shape(), said, log)
            val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
            log.append("-- second --\n")
            val (placed2, timing2) = label(state, Telling.SECOND, log)
            val after = Telling.land(state, Telling.check(Telling.SECOND, placed2).kept)
            File(out, "${name.replace(' ', '-')}-pieces-$mode-${seed ?: 0}.txt").writeText(log.toString())
            val all = Telling.marks(said, placed) + Telling.secondMarks(after)
            println("$way: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines; $timing; then $timing2")
            for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(400)}")
            println("  rungs: " + placed.filter { it.place in Telling.RUNGS }.joinToString(" | ") { "${it.place} ${it.words}" })
            println("  hand after both: " + Telling.cards(after).entries.joinToString(" | ") { "${it.key} ${it.value}" })
        }
    }
}

/**
 * 0.5's Stage 4 on the laptop: code cuts the telling and reckons when each
 * piece falls from the time words; she gives each piece one word for its
 * place in the day, a conversation of its own each; a piece sharing words
 * with a card on the hand is put to her, yes or no. Then the rungs, once.
 */
@OptIn(ExperimentalApi::class)
internal fun stage4(engine: Engine, label: String, tellings: List<Pair<String, String>>, out: File, runs: Int = 5) {
    out.mkdirs()
    val off = ThinkingConfig(enableThinking = false)
    val ways = listOf("as the phone asks" to null) + (1..runs).map { "sampled, seed $it" to it }
    for ((name, said) in tellings) {
        println("\n== $name, Stage 4, $label ==")
        for ((way, seed) in ways) {
            val sampler = if (seed == null) SamplerConfig(topK = Asks.Tuning.TOP_K, topP = Asks.Tuning.TOP_P, temperature = Asks.Tuning.TEMPERATURE, seed = 0)
            else SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0, seed = seed)
            val log = StringBuilder()
            val firsts = mutableListOf<Double>()
            fun ask(system: String?, message: String): String =
                engine.createConversation(ConversationConfig(systemInstruction = system?.let { Contents.of(it) }, samplerConfig = sampler, maxOutputToken = Telling.PIECE_TOKENS, thinkingConfig = off)).use { c ->
                    send(c, message).also { r -> r.timing.substringAfter("first token ").substringBefore(" s").toDoubleOrNull()?.let(firsts::add) }.text
                }
            fun sort(state: JsObj, telling: String): Pair<List<Telling.Placed>, String> {
                val start = System.nanoTime()
                val (placed, steps) = Telling.sorted(
                    telling, Telling.BENCH_DAY, com.phenomemental.pocketbeach.bsp.Whens.first(), state,
                    judge = { piece -> ask(Telling.sortInstruction(Telling.BENCH_DAY), Telling.aloneAsk(piece, telling)).also { log.append("\u201c$piece\u201d -> ${it.trim().lineSequence().first()}\n") } },
                    about = { piece, card -> Telling.yes(ask(null, Telling.cardAsk(piece, telling, card)).also { log.append("  about \u201c$card\u201d? ${it.trim().lineSequence().first()}\n") }) },
                )
                for (st in steps) if (st.whenBy != null) log.append("  when: \u201c${st.piece}\u201d by \u201c${st.whenBy}\u201d -> ${st.places.last()}\n")
                val sorting = (System.nanoTime() - start) / 1e9
                val rungs = engine.createConversation(ConversationConfig(samplerConfig = sampler, maxOutputToken = 256, thinkingConfig = off)).use { r -> send(r, Telling.rungsAsk(state, Telling.BENCH_DAY, telling)) }
                log.append("rungs -> ${rungs.text.trim()}\n")
                val rungLines = Telling.placed(rungs.text).filter { it.place in Telling.RUNGS }
                return (placed + rungLines) to "${steps.size} pieces sorted in ${"%.1f".format(sorting)} s (first tokens median ${"%.1f".format(firsts.sorted().getOrElse(firsts.size / 2) { 0.0 })} s), rungs in ${rungs.timing.substringAfter(", ").substringBefore(" in all")}"
            }
            val (placed, timing) = sort(Telling.shape(), said)
            val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
            log.append("-- second --\n")
            val (placed2, timing2) = sort(state, Telling.SECOND)
            val after = Telling.land(state, Telling.check(Telling.SECOND, placed2).kept)
            File(out, "${name.replace(' ', '-')}-stage4-$label-${seed ?: 0}.txt").writeText(log.toString())
            val all = Telling.marks(said, placed) + Telling.secondMarks(after)
            println("$way: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines; $timing; then $timing2")
            for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(400)}")
            println("  rungs: " + placed.filter { it.place in Telling.RUNGS }.joinToString(" | ") { "${it.place} ${it.words}" })
            println("  hand after both: " + Telling.cards(after).entries.joinToString(" | ") { "${it.key} ${it.value}" })
        }
    }
}

/** The one-pass marks again, from her answers kept in [dir] by [tellings]: the same reading, without the model. */
internal fun remark(tellings: List<Pair<String, String>>, dir: File) {
    for ((name, said) in tellings) for (pocket in listOf(true, false)) {
        val who = if (pocket) "pocket" else "plain"
        println("\n== $name, $who, marked again ==")
        for (seed in 0..9) {
            val tag = "${name.replace(' ', '-')}-$who-$seed"
            val first = File(dir, "$tag-1.txt").takeIf { it.exists() }?.readText() ?: continue
            val second = File(dir, "$tag-2.txt").readText()
            val placed = if (pocket) Telling.placed(first) else Telling.fromPlain(first)
            val after = if (pocket) {
                val state = Telling.land(Telling.shape(), Telling.check(said, placed).kept)
                Telling.land(state, Telling.check(Telling.SECOND, Telling.placed(second)).kept)
            } else {
                Telling.land(Telling.shape(), Telling.fromPlain(second).filter { it.place in Telling.HAND }.ifEmpty { placed.filter { it.place in Telling.HAND } })
            }
            val all = Telling.marks(said, placed) + Telling.secondMarks(after)
            println("${if (seed == 0) "as the phone asks" else "sampled, seed $seed"}: ${all.count { it.met }} of ${all.size} marks; ${placed.size} lines")
            for (m in all) println("  ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(300)}")
            if (!pocket) {
                // Her plain answer anchored to his words: she sorts, the pocket keeps only his.
                val anchored = Telling.anchored(placed, said)
                val state = Telling.land(Telling.shape(), Telling.check(said, anchored).kept)
                val second2 = Telling.anchored(Telling.fromPlain(second).filter { it.place in Telling.HAND }, Telling.SECOND)
                val after2 = Telling.land(state, Telling.check(Telling.SECOND, second2).kept)
                val marks2 = Telling.marks(said, anchored) + Telling.secondMarks(after2)
                println("  anchored to his words: ${marks2.count { it.met }} of ${marks2.size} marks; ${anchored.size} lines")
                for (m in marks2) println("    ${if (m.met) "✓" else "✗"} ${m.name}: ${m.note.take(300)}")
            }
        }
    }
}

internal class Reply(val text: String, val timing: String)

@OptIn(ExperimentalApi::class)
private fun send(c: Conversation, message: String, form: String? = null): Reply {
    val start = System.nanoTime()
    var first: Long? = null
    val whole = StringBuilder()
    kotlinx.coroutines.runBlocking {
        (if (form != null) c.sendMessageAsync(message, responseFormat = com.google.ai.edge.litertlm.ResponseFormat.regex(form)) else c.sendMessageAsync(message)).collect { m ->
            if (first == null) first = (System.nanoTime() - start) / 1_000_000
            whole.append(m.toString())
        }
    }
    val b = c.getBenchmarkInfo()
    return Reply(whole.toString(), "first token ${first?.let { "%.1f".format(it / 1000.0) }} s, ${"%.1f".format((System.nanoTime() - start) / 1e9)} s in all, read ${b.lastPrefillTokenCount}, wrote ${b.lastDecodeTokenCount}")
}
