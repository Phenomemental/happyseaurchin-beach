package com.phenomemental.pocketbeach.bsp

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Today's finish line (decision 17): a test across every node.
 *
 * 1. In the pocket, a block `hello-world` holding, line by line, the shell's
 *    opening line, Matthew's secret, a seal the app makes at random (neither
 *    Matthew nor the model chooses it), and a fingerprint of the block as it
 *    stood before that last line, computed by the app.
 * 2. With Matthew's confirmation, one message to `pool:Phenomemental` on the
 *    online beach, carrying the secret, the seal, the fingerprint and the
 *    block's pocket address, under the name Phenomemental.
 *
 * The companion (Gemma wearing the shell) proposes both steps; this object is
 * also the by-hand path the test screen takes, so the chain can be shown
 * without the model.
 */
object HelloWorld {
    const val BLOCK = "hello-world"
    const val POOL = "Phenomemental"
    const val SHELL = "pocket-companion"
    const val SHELL_OPENING = "Pocket companion. I live on this phone, read freely, and write only when Matthew says yes."

    /** Where the shell's hello world has the companion write Matthew's secret; an answer that leaves it is sent back. */
    const val SECRET_SLOT = "HIS SECRET"

    /** The four lines of a hello world, as the shell gives them: the app fills the gaps, the companion writes in the secret. */
    val SHELL_LINES = listOf("{{shell}}", "Secret: $SECRET_SLOT", "Seal: {{seal}}", "Fingerprint: {{fingerprint}}")

    /** The post a hello world offers, as the shell gives it: [message]'s form, with the app's gaps. */
    const val SHELL_POST = "Hello world from the pocket. Shell: “{{shell}}” Secret: $SECRET_SLOT. Seal: {{seal}}. Fingerprint: {{fingerprint}}. Block: pocket/hello-world."

    /**
     * How the companion does the hello world, as one line of its shell: the
     * answer itself, to be copied with the secret written in. (A small model
     * copies a whole answer more surely than it follows a description of one.)
     */
    val SHELL_RECIPE: String = "Hello world: when Matthew asks for the hello world, answer with exactly these steps and this post, " +
        "writing the secret he gives where it says HIS SECRET and changing nothing else: " +
        "{\"steps\": [{\"do\": \"make\", \"block\": \"$BLOCK\", \"at\": \"\", \"text\": \"\"}, " +
        SHELL_LINES.joinToString(", ") { "{\"do\": \"add\", \"block\": \"$BLOCK\", \"at\": \"\", \"text\": \"$it\"}" } + "], " +
        "\"post\": {\"pool\": \"$POOL\", \"message\": \"$SHELL_POST\"}}"

    /** The companion's shell as it is first made: its opening line, and how to do the hello world. */
    fun shellBlock(): JsObj = JsObj.of("_" to str(SHELL_OPENING), "1" to str(SHELL_RECIPE))

    /** Make the companion's shell, by Matthew's tap: [shellBlock], as one guarded write. */
    fun makeShell(pages: Pages) {
        require(!pages.exists(SHELL)) { "There's already a block called $SHELL." }
        val plan = pages.planKeep(SHELL, shellBlock())
        plan.refusal?.let { throw IllegalStateException(it) }
        val done = pages.carryOut(plan)
        check(done.ok) { done.problem ?: "The shell was not made." }
    }

    /** 128 random bits from the phone's secure random source, as 32 hex digits. */
    fun seal(random: SecureRandom = SecureRandom()): String = ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    /** The first 16 hex digits of the SHA-256 of a block's stored text. */
    fun fingerprint(stored: String): String =
        MessageDigest.getInstance("SHA-256").digest(stored.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(16)

    /** A block's pocket address, as bsp() names it in its replies. */
    fun address(block: String) = "pocket/$block"

    /** The post: the shell's opening line, so it can be read on the beach too, then the secret, the seal, the fingerprint and the block's address. */
    fun message(shellOpening: String, secret: String, seal: String, fingerprint: String, block: String = BLOCK): String =
        "Hello world from the pocket. Shell: “${shellOpening.trim()}” Secret: $secret. Seal: $seal. Fingerprint: $fingerprint. Block: ${address(block)}."

    /** Whether a shell's hello world line is one the app wrote (this one or an earlier form), and not the present [SHELL_RECIPE]. */
    fun recipeIsOlder(line: String?): Boolean = line != null && line.startsWith("Hello world: when Matthew asks for the hello world") && line != SHELL_RECIPE

    /**
     * Whether a block's fingerprint line matches it: the last line's
     * fingerprint, set against the block as it stood before that line.
     */
    fun verify(stored: String): Boolean {
        val block = parseJson(stored) as? JsObj ?: return false
        val digits = digitKeys(block)
        val last = digits.lastOrNull() ?: return false
        val line = collectUnderscore(block[last]) ?: (block[last] as? JsStr)?.value ?: return false
        val claimed = line.removePrefix("Fingerprint: ").trim()
        val before = deepCopy(block) as JsObj
        before.remove(last)
        return claimed == fingerprint(stringify(before)!!)
    }

    /**
     * What a made hello-world lacks, in plain words; empty when it holds the
     * shell's opening line, a secret, the seal the app made for it, and as its
     * last line a fingerprint that matches the block before that line.
     */
    fun check(stored: String?, shellOpening: String, seal: String): List<String> {
        val block = stored?.let { runCatching { parseJson(it) }.getOrNull() } as? JsObj ?: return listOf("There's no block $BLOCK.")
        val lines = bspRead(block, null, 0).discEntries.orEmpty().mapNotNull { it.content?.trim() } + listOfNotNull(collectUnderscore(block))
        val problems = mutableListOf<String>()
        if (lines.none { it == shellOpening.trim() }) problems += "No line holds the shell's opening line."
        if (lines.none { it.startsWith("Secret: ") && it.length > "Secret: ".length }) problems += "No line holds a secret."
        if (lines.none { it == "Seal: $seal" }) problems += "No line holds the seal the app made."
        if (!verify(stored)) problems += "The last line isn't a fingerprint that matches the block."
        return problems
    }

    /** What a hello-world message lacks: the shell's opening line, the secret, the seal, the fingerprint and the block's pocket address. */
    fun checkMessage(message: String, shellOpening: String, seal: String, fingerprint: String, block: String = BLOCK): List<String> = buildList {
        if (!message.contains(shellOpening.trim())) add("The message doesn't carry the shell's opening line.")
        if (!Regex("Secret: \\S").containsMatchIn(message)) add("The message carries no secret.")
        if (!message.contains(seal)) add("The message doesn't carry the seal.")
        if (!message.contains(fingerprint)) add("The message doesn't carry the fingerprint.")
        if (!message.contains(address(block))) add("The message doesn't carry the block's address, ${address(block)}.")
    }

    /**
     * The message a made hello-world carries, from its own lines: the shell's
     * opening line (its first line), its secret, seal and fingerprint. Null
     * unless the fingerprint matches the block.
     */
    fun messageFor(stored: String?): String? {
        if (stored == null || !verify(stored)) return null
        val block = parseJson(stored) as JsObj
        // The block's own lines, its opening line (on the root's underscore chain) left out.
        val lines = bspRead(block, null, 0).discEntries.orEmpty()
            .filter { e -> e.address.isNotEmpty() && !e.address.all { it == '0' || it == '.' } }
            .mapNotNull { it.content?.trim() }
        fun after(prefix: String) = lines.lastOrNull { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()?.takeIf { it.isNotEmpty() }
        val shell = lines.firstOrNull { line -> listOf("Secret: ", "Seal: ", "Fingerprint: ").none { line.startsWith(it) } } ?: return null
        val secret = after("Secret: ") ?: return null
        val seal = after("Seal: ") ?: return null
        val print = after("Fingerprint: ") ?: return null
        return message(shell, secret.removeSuffix("."), seal, print)
    }

    class Made(val stored: String, val seal: String, val fingerprint: String)

    /**
     * Step 1 by hand: make `hello-world` and its four lines, each a guarded
     * write, the fingerprint taken from the block as it stands before its own
     * line. Matthew's one tap on the test screen is the confirmation.
     */
    fun makeByHand(pages: Pages, storage: BlockStorage, shellOpening: String, secret: String, seal: String = seal()): Made {
        require(secret.isNotBlank()) { "Give the secret first." }
        require(!pages.exists(BLOCK)) { "There's already a block called $BLOCK. Rename or remove it, then try again." }
        fun go(plan: Plan) {
            plan.refusal?.let { throw IllegalStateException(it) }
            val done = pages.carryOut(plan)
            check(done.ok) { done.problem ?: "A write did not land." }
        }
        go(pages.planNewBlock(BLOCK))
        go(pages.planAdd(BLOCK, "", shellOpening))
        go(pages.planAdd(BLOCK, "", "Secret: ${secret.trim()}"))
        go(pages.planAdd(BLOCK, "", "Seal: $seal"))
        val print = fingerprint(storage.read(BLOCK)!!)
        go(pages.planAdd(BLOCK, "", "Fingerprint: $print"))
        return Made(storage.read(BLOCK)!!, seal, print)
    }
}
