package com.phenomemental.pocketbeach.bsp

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The pocket's own rules, which have no counterpart upstream: what it refuses, and where it keeps blocks. */
class PocketTest {
    private val root: File = Files.createTempDirectory("pocket-test").toFile()
    private val pocket = PocketBsp.inFolders(root, File(System.getProperty("pocket.shelf") ?: "../shelf")) { 1_793_178_000_000 }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    private fun call(json: String) = pocket.call(json)

    @Test
    fun theBeachIsOutOfReach() {
        for (agent in listOf("https://beach.happyseaurchin.com", "weft", "sed:club", "grain:abc123", "")) {
            assertEquals(PocketBsp.BEACH_OUT_OF_REACH, call("""{"agent_id":"$agent","block":"x"}"""), agent)
        }
        for (block in listOf("sed:club", "grain:abc123")) {
            assertEquals(PocketBsp.BEACH_OUT_OF_REACH, call("""{"agent_id":"pocket","block":"$block","spindle":"1","content":"x"}"""), block)
        }
    }

    @Test
    fun locksAndEncryptionAreRefusedNotIgnored() {
        val cases = mapOf(
            """{"agent_id":"pocket","block":"x","secret":"s"}""" to "secret",
            """{"agent_id":"pocket","block":"x","spindle":"1","content":"y","new_lock":null}""" to "new_lock",
            """{"agent_id":"pocket","block":"x","gray":false}""" to "gray",
            """{"agent_id":"pscale","block":"whetstone","enc_secret":"k"}""" to "enc_secret",
            """{"agent_id":"pocket","block":"x","members":["matthew"]}""" to "members",
        )
        for ((json, name) in cases) {
            val reply = call(json)
            assertTrue(reply.startsWith("The pocket keeps no locks and no encryption"), reply)
            assertTrue(reply.contains("Leave out $name "), reply)
        }
        assertTrue(!File(root, "regions/pocket/x.json").exists(), "a refused call writes nothing")
    }

    @Test
    fun faceAndTierAreAcceptedAndIgnored() {
        assertEquals(call("""{"agent_id":"pscale","block":"whetstone","spindle":"2"}"""),
            call("""{"agent_id":"pscale","block":"whetstone","spindle":"2","face":"character","tier":"soft"}"""))
    }

    @Test
    fun anOmittedAgentIsThePocket() {
        call("""{"block":"garden","spindle":"1","content":"sow peas"}""")
        assertTrue(call("""{"agent_id":"pocket","block":"garden","spindle":"1"}""").contains("sow peas"))
    }

    @Test
    fun malformedParametersAreNamed() {
        assertTrue(call("""{"agent_id":"pocket","block":"x","pscale_attention":1.5}""").startsWith("pscale_attention must be a whole number"))
        assertTrue(call("""{"agent_id":"pocket","block":"x","spindle":12}""").startsWith("spindle must be text"))
        assertTrue(call("""{"agent_id":"pocket","block":7}""").startsWith("block must be text"))
        assertTrue(call("""{"agent_id":"pocket","block":"x","append":"yes"}""").startsWith("append must be true or false"))
        assertTrue(call("""{"agent_id":3}""").startsWith("agent_id must be text"))
        assertTrue(call("""{not json""").startsWith("The call is not readable JSON"))
        assertTrue(call("""[1]""").startsWith("The call must be a JSON object"))
    }

    @Test
    fun theWhetstoneIsOnTheShelf() {
        val reply = call("""{"agent_id":"pscale","block":"whetstone"}""")
        assertTrue(reply.startsWith("[disc @ pscale 0 (depth 1)]"), reply)
        assertTrue(reply.contains("[2]: Selection-shape derivation"), reply)
        assertTrue(reply.contains("the whole block is 40810 characters"), reply)
    }

    @Test
    fun blocksLiveInOneFileEachAndWritesLeaveNothingBehind() {
        call("""{"agent_id":"pocket","block":"shell:matthew","spindle":"1","content":"a colon in the name"}""")
        call("""{"agent_id":"pocket","block":"log","content":"first","append":true}""")
        val dir = File(root, "regions/pocket")
        assertEquals(listOf("log.json", "shell%3Amatthew.json"), dir.list()!!.sorted())
        assertTrue(File(root, "regions/pocket.meta.json").isFile)
        assertEquals(listOf("log", "shell:matthew"), FileStorage(root).names().sorted())
        val stored = parseJson(File(dir, "log.json").readText()) as JsObj
        assertEquals(str("log at pocket."), stored["_"])

        // Removing a block removes its one file, and nothing else.
        FileStorage(root).delete("shell:matthew")
        assertEquals(listOf("log.json"), dir.list()!!.sorted())
        FileStorage(root).delete("never-was")
        assertEquals(listOf("log"), FileStorage(root).names())
    }

    @Test
    fun theTenthLineWrapsTheRoot() {
        for (i in 1..9) call("""{"agent_id":"pocket","block":"log","content":"line $i","append":true}""")
        val reply = call("""{"agent_id":"pocket","block":"log","content":"line 10","append":true}""")
        assertTrue(reply.startsWith("[append @ \"pocket/log\" → slot 11  ⤴ supernested → floor 2"), reply)
        // The first nine now stand under the root's underscore: a node of nine, which takes no tenth.
        val full = call("""{"agent_id":"pocket","block":"log","spindle":"0","content":"x","append":true}""")
        assertTrue(full.startsWith("Append rejected: Beach append rejected: \"0\" of \"log\" is full: nine slots"), full)
    }

    @Test
    fun blockNamesBecomeFileNamesEverySystemAccepts() {
        val names = listOf("garden", "Garden", "GARDEN", "a b", "ü-name", "x/y", "x\\y", "100%", "*star*", "dots..", "日本", "q?", "<a|b>", "\"quoted\"",
            "con", "CON", "nul.txt", "com1", "lpt9", "console", "shell:matthew", "tide 🌊")
        val files = names.map(::fileNameOf)
        for ((name, file) in names.zip(files)) {
            assertEquals(name, blockNameOf(file), name)
            // Lower-case letters, digits, a few marks, and %XX escapes in upper-case hex.
            assertTrue(Regex("^([a-z0-9\\-_.!~'()]|%[0-9A-F]{2})+$").matches(file), "$name → $file")
            assertTrue(!Regex("^(con|prn|aux|nul|com[0-9]|lpt[0-9])$").matches(file.substringBefore('.').lowercase()), "$name → $file")
        }
        assertEquals(files.size, files.map { it.lowercase() }.toSet().size, "no two names share a file where case is ignored")
        assertEquals("garden.json", fileNameOf("garden"))
        assertEquals("shell%3Amatthew.json", fileNameOf("shell:matthew"))
        assertEquals("%47arden.json", fileNameOf("Garden"))
        assertEquals("%63on.json", fileNameOf("con"))
        assertEquals("console.json", fileNameOf("console"))
    }

    @Test
    fun gardenAndCapitalGardenStayTwoBlocks() {
        call("""{"agent_id":"pocket","block":"garden","spindle":"1","content":"lower"}""")
        call("""{"agent_id":"pocket","block":"Garden","spindle":"1","content":"upper"}""")
        assertTrue(call("""{"agent_id":"pocket","block":"garden","spindle":"1"}""").contains("lower"))
        assertTrue(call("""{"agent_id":"pocket","block":"Garden","spindle":"1"}""").contains("upper"))
    }
}
