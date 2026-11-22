package com.phenomemental.pocketbeach.model

import com.phenomemental.pocketbeach.bsp.Talk
import com.phenomemental.pocketbeach.bsp.Talks
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sundial 0.12 (spine:pocket-beach 1.111178719996): the mind's contract is
 * the core's, plain Kotlin on the JVM. A mind written here, with no Android,
 * answers through it as Gemma does on the phone: the shape a laptop front end
 * running the same core would give a larger model.
 */
class MindContractTest {
    /** A mind on the laptop: it answers by rule, as a larger model would answer by its own lights. */
    private class Laptop(private val answer: (String?, String) -> String) : Mind {
        val asked = mutableListOf<Pair<String?, String>>()
        override fun state(): ModelState = ModelState.Ready
        override suspend fun ask(instruction: String?, message: String, settings: JobSettings): Mind.Answer {
            asked += instruction to message
            return Mind.Answer(answer(instruction, message), 1, null, "CPU", null, null)
        }
        override val loaded = true
        override val processor = "CPU"
        override suspend fun chat(instruction: String): Mind.Chat = error("not asked here")
        override suspend fun talker(instruction: String, history: List<Pair<String, String>>, held: Boolean, settings: JobSettings): Mind.Talker {
            val said = history.toMutableList()
            return object : Mind.Talker {
                override val stream = Talks.Stream { message, piece ->
                    val reply = answer(instruction, message).also { said += message to it }
                    reply.split(" ").forEach { piece("$it ") }
                    reply
                }
                override val pace = Pace("CPU", null, null)
                override fun stop() {}
                override fun close() {}
            }
        }
    }

    private fun <T> now(block: suspend () -> T): T {
        var out: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { out = it })
        return out!!.getOrThrow()
    }

    @Test
    fun aMindWrittenInTheCoreAnswersThroughTheContract() {
        val mind = Laptop { _, message -> if (message.contains("ring")) "3, the third place" else "Walk somewhere new.\nTomorrow: the river" }
        val settings = JobSettings.of(Job.TALK)
        assertEquals(ModelState.Ready, mind.state())
        assertEquals(3, now { mind.choose("Which place?", "a ring of four", 4, settings) })
        assertNull(now { mind.choose("Which place?", "a ring of two", 2, settings) }, "a digit beyond the ring is no choice")
        val sheet = now { mind.sheet("Draw his day.", "He walked.", settings, tool = false) }!!
        assertEquals("Walk somewhere new.", sheet.title)
        assertEquals("the river", sheet.tomorrow)
        assertNull(now { mind.chooseMany("Which places?", "words", 6, settings) }, "the free mind names none")
        // Her conversation, streamed piece by piece through the core's own Stream.
        val talker = now { mind.talker("It is Sunday 22 November 2026, 11:30 as this conversation opens.", emptyList(), settings = settings) }
        val pieces = mutableListOf<String>()
        val reply = talker.stream.say("(It is Sunday 22 November 2026, 11:30, midday.)\nHello") { pieces += it }
        assertEquals("Walk somewhere new.\nTomorrow: the river", reply)
        assertTrue(pieces.size > 1)
        assertEquals("free", talker.route)
    }

    @Test
    fun eachJobsMindIsItsSettingsAndTheGpuGoesToOneEngine() {
        val asked = Job.values().associateWith { JobSettings.of(it) }
        assertEquals(1, asked.values.map { it.engine }.toSet().size, "the three share one engine by default")
        val apart = asked + (Job.WRITE to JobSettings.of(Job.WRITE).copy(model = JobSettings.GEMMA_E4B))
        assertEquals("CPU", Gpu.running(apart, Job.WRITE).processor)
        assertEquals("Talk's Gemma 4 E2B holds the GPU, and the phone runs one engine on it at a time", Gpu.why(apart, Job.WRITE))
        assertEquals("GPU", Gpu.running(apart, Job.TALK).processor)
        assertTrue(JobSettings.of(Job.WRITE).greedy)
    }

    @Test
    fun aTalkFromTheCoreIsWhatTheChatSpeaksThrough() {
        val talk = Talk { message -> "heard: $message" }
        assertEquals("heard: hello", talk.send("hello"))
        val steps = Steps()
        steps.add("a conversation opened", 1200)
        steps.note("her first words")
        assertEquals(listOf("a conversation opened: 1.2 s", "her first words"), steps.take())
        assertEquals(emptyList(), steps.take())
    }
}
