package com.phenomemental.pocketbeach

import com.phenomemental.pocketbeach.model.EngineKey
import com.phenomemental.pocketbeach.model.OneEngine
import com.phenomemental.pocketbeach.model.Pace
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sundial 0.10.5: her one engine, on a pretend phone whose GPU takes one
 * engine at a time, as his Pixel's does. On his Pixel on 15 November a second
 * engine was made on the CPU while the first still held the GPU, and she
 * wrote at 4.3 tokens a second for the rest of the app's run. The CODE part
 * of the line in the sand's test 9 for her engine; LineInTheSandDeviceTest
 * runs it through the app, a new conversation with + among it.
 */
class OneEngineTest {
    /** The phone's GPU, pretended: one engine on it at a time, and it refuses the next [refuse] asked of it. */
    class PretendGpu {
        var onGpu = 0
        var mostOnGpu = 0
        var refuse = 0
        val made = mutableListOf<String>()
        val shut = mutableListOf<String>()

        fun make(key: EngineKey, gpu: Boolean, @Suppress("UNUSED_PARAMETER") again: Boolean): String {
            if (gpu) {
                if (refuse > 0) {
                    refuse--
                    error("Failed to create the GPU delegate.")
                }
                check(onGpu == 0) { "another engine holds the GPU" }
                onGpu++
                mostOnGpu = maxOf(mostOnGpu, onGpu)
            }
            return "${key.model} at ${key.context} on the ${if (gpu) "GPU" else "CPU"} (${made.size + 1})".also { made += it }
        }

        fun shut(e: String) {
            if (e.contains("on the GPU")) onGpu--
            shut += e
        }
    }

    private val e4b = EngineKey("Gemma 4 E4B", "GPU", 8192)
    private val said = mutableListOf<String>()
    private fun one(gpu: PretendGpu) = OneEngine(gpu::make, gpu::shut, { said += it }, pause = {})

    /** A conversation kept open on the engine, counting how often it is let go. */
    private class Kept : OneEngine.Kept {
        var letGo = 0
        override fun letGo() { letGo++ }
    }

    @Test
    fun everyConversationOpensOnTheOneEngine() = runBlocking {
        val gpu = PretendGpu()
        val one = one(gpu)
        val (first, ms) = one.engine(e4b)
        assertEquals("GPU", first.processor)
        assertNotNull("made, and timed", ms)
        // Her talk kept open; then a new conversation, the greeting and keeping, each asking for her engine.
        val talk = Kept()
        one.opened(talk)
        repeat(3) {
            val (again, made) = one.engine(e4b)
            assertSame(first, again)
            assertNull("never made again", made)
        }
        one.closed(talk)
        assertSame(first, one.engine(e4b).first)
        assertEquals(1, one.made)
        assertEquals(0, talk.letGo)
        assertTrue(gpu.shut.isEmpty())
    }

    @Test
    fun madeAgainTheOldOneIsLetGoFirstAndItsConversationsWithItSoTheGpuIsFree() = runBlocking {
        // As on his Pixel on 15 November: her conversation still open on the engine, and her engine asked for under other settings.
        val gpu = PretendGpu()
        val one = one(gpu)
        one.engine(e4b)
        val talk = Kept()
        one.opened(talk)
        val (l, _) = one.engine(e4b.copy(context = 16384))
        assertEquals("on the GPU, never a second engine on the CPU", "GPU", l.processor)
        assertNull(l.whyNotGpu)
        assertEquals("her conversation let go with the engine", 1, talk.letGo)
        assertEquals(listOf("Gemma 4 E4B at 8192 on the GPU (1)"), gpu.shut)
        assertEquals("never two on the GPU", 1, gpu.mostOnGpu)
        assertEquals(2, one.made)
        assertTrue(said.toString(), said.any { it.contains("let go first, so the GPU is free") })
        // Still open: it opens again on the new engine as it is next asked.
        assertEquals(1, one.open)
    }

    @Test
    fun theGpuRefusingOnceIsTriedOnceMoreAndSheStaysOnTheGpu() = runBlocking {
        val gpu = PretendGpu().apply { refuse = 1 }
        val one = one(gpu)
        val (l, _) = one.engine(e4b)
        assertEquals("GPU", l.processor)
        assertNull(l.whyNotGpu)
        assertNull(Pace(l.processor, l.whyNotGpu, null).slower)
        assertTrue(said.toString(), said.any { it.contains("tried once more") })
    }

    @Test
    fun theGpuRefusingTwiceRunsHerOnTheCpuAndSaysSoPlainly() = runBlocking {
        val gpu = PretendGpu().apply { refuse = 2 }
        val one = one(gpu)
        val (l, ms) = one.engine(e4b)
        assertEquals("CPU", l.processor)
        assertEquals("Failed to create the GPU delegate.", l.whyNotGpu)
        val pace = Pace(l.processor, l.whyNotGpu, ms)
        assertEquals("She's on the slower processor; closing and reopening Sundial will fix it.", pace.slower)
        assertEquals("The GPU couldn't run it: Failed to create the GPU delegate.", pace.detail()!!.substringBefore(" Loading"))
        assertNotNull("until Sundial is opened again", one.noGpu(e4b))
    }

    @Test
    fun anAnswerFailingOnTheGpuMakesHerEngineAgainOnTheGpuOnceThenTheCpu() = runBlocking {
        val gpu = PretendGpu()
        val one = one(gpu)
        one.engine(e4b)
        val talk = Kept()
        one.opened(talk)
        one.gpuFailed(e4b, "Failed to invoke the compiled model.")
        assertNull("let go", one.held)
        assertEquals(1, talk.letGo)
        assertEquals("made again on the GPU", "GPU", one.engine(e4b).first.processor)
        assertNull(one.noGpu(e4b))
        one.gpuFailed(e4b, "Failed to invoke the compiled model.")
        val (l, _) = one.engine(e4b)
        assertEquals("CPU", l.processor)
        assertEquals("an answer failed on the GPU twice (Failed to invoke the compiled model.)", l.whyNotGpu)
        assertEquals(Pace.SLOWER, Pace(l.processor, l.whyNotGpu, null).slower)
        assertEquals(1, gpu.mostOnGpu)
    }

    @Test
    fun herEngineIsNeverLetGoUnderACallUnderWay() = runBlocking {
        val gpu = PretendGpu()
        val one = one(gpu)
        one.engine(e4b)
        val gate = CompletableDeferred<Unit>()
        val call = launch { one.busy { gate.await() } }
        yield()
        // Android short of memory while she writes: let go only when she has finished.
        one.release()
        assertNotNull(one.held)
        // Her engine asked for under other settings while she writes: made once she has finished, the old one let go first.
        val other = async { one.engine(e4b.copy(model = "Gemma 4 E2B")) }
        delay(100)
        assertFalse(other.isCompleted)
        assertEquals(1, gpu.onGpu)
        gate.complete(Unit)
        call.join()
        assertEquals("GPU", other.await().first.processor)
        assertEquals(1, gpu.mostOnGpu)
        assertEquals(2, one.made)
    }

    @Test
    fun aJobSetToTheCpuHasTheEngineMadeAgainThereWithNothingToSay() = runBlocking {
        val gpu = PretendGpu()
        val one = one(gpu)
        one.engine(e4b)
        val (l, _) = one.engine(EngineKey("Gemma 4 E2B", "CPU", 4096))
        assertEquals("CPU", l.processor)
        assertNull(l.whyNotGpu)
        assertNull("asked for, so nothing to say", Pace(l.processor, l.whyNotGpu, null).slower)
        assertEquals("hers let go first", 0, gpu.onGpu)
        assertEquals("GPU", one.engine(e4b).first.processor)
        assertEquals(1, gpu.mostOnGpu)
    }
}
