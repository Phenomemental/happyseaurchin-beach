package com.phenomemental.pocketbeach.bsp

/**
 * The one way out (decision 18): append one message to a pool on the online
 * beach, only after Matthew taps to confirm. The beach's address is fixed
 * here, the request is an append and nothing else, and the core never reaches
 * the network itself: a front end hands it the [Wire] that carries the one
 * request, and that wire is the only network code the front end has.
 *
 * The request is the one bsp-mcp-server's wire sends for an append:
 * POST <beach>/.well-known/pscale-beach?block=pool:<name> with
 * {append: true, content: {_: message, 1: author}}; the beach allocates the
 * slot and adds the arrival stamp. Appending to Matthew's pool needs no key:
 * the beach asks for one only when a pool's root is locked, and his pool says
 * it is open. If it ever answers that a key is needed, nothing is retried.
 */
fun interface Wire {
    /**
     * POST [body] (JSON) to [url], returning the status and the text the beach
     * answered. Failing to connect throws; once the whole message has gone,
     * failing to hear back throws [NoAnswer], since it may have landed.
     */
    fun post(url: String, body: String): WireReply
}

/** What a wire throws when the message went whole but no answer came back: it may have landed. */
class NoAnswer(message: String, cause: Throwable? = null) : java.io.IOException(message, cause)

class WireReply(val status: Int, val text: String)

/**
 * A message ready to post, shown whole to Matthew before he confirms.
 * [scratch]: made in a scratch pocket (the bench's, a trial's, any pocket
 * held only in memory), so [PoolPost.send] never lets it leave the phone.
 */
class PoolMessage internal constructor(val pool: String, val author: String, val message: String, val url: String, val body: String, val scratch: Boolean = false)

sealed class Posted {
    /** The beach took it, at [slot] of the pool. */
    class Landed(val pool: String, val slot: String) : Posted()

    /** The beach answered, and nothing landed: why, in plain words. */
    class Refused(val why: String) : Posted()

    /**
     * The beach couldn't be reached, so nothing landed; or, [mayHaveLanded],
     * the message went but no answer came back. Either way the front end
     * keeps it, to be posted again only when Matthew taps for it.
     */
    class Unreached(val why: String, val mayHaveLanded: Boolean = false) : Posted()
}

/**
 * Sundial 0.10 (spine:pocket-beach 1.1111787171, 1.111178719918): who a post is
 * from, and the one room it may go to, read from the shell the pocket was
 * hatched as, never hard-coded and never from his words. [author] is the
 * hatched handle; [pool] is the first pool his passport's place 3 names
 * ("Meet me in the pool: … (pool:Phenomemental)"), else the first his
 * shell's manifest names, else pool:<handle>. [scratch]: the room of a
 * scratch pocket (one held only in memory: the bench's, a trial's, a test's);
 * a post made for it never leaves the phone.
 */
class Room(val author: String, val pool: String, val scratch: Boolean = false)

object PoolPost {
    const val BEACH = "https://beach.happyseaurchin.com"
    const val MAX_CHARS = 2000

    /** Why a scratch pocket's post is refused: it never leaves the phone. */
    const val SCRATCH = "This post was made in a scratch pocket (the bench's or a trial's), so it never leaves the phone."

    private val POOL = Regex("""\bpool:([A-Za-z0-9][A-Za-z0-9_-]*)""")

    /** The room of the shell this pocket was hatched as; null before a hatch. */
    fun room(storage: BlockStorage): Room? {
        // A pocket held only in memory is a scratch pocket, never the phone's own (a FileStorage): its room, hatched or stood in, is marked so.
        val scratch = storage is MemoryStorage
        val h = Hatch.hatched(storage) ?: return (storage as? MemoryStorage)?.standInRoom?.let { Room(it.author, it.pool, scratch = true) }
        fun named(block: String, at: String): String? {
            val b = storage.read(block)?.let { runCatching { parseJson(it) }.getOrNull() } ?: return null
            val node = jsGet(b, at) ?: return null
            val text = buildString {
                append((node as? JsStr)?.value ?: collectUnderscore(node).orEmpty())
                HerWindow.linesOf(node).forEach { append("\n").append(it.second) }
            }
            return POOL.find(text)?.groupValues?.get(1)
        }
        val pool = named("passport:$h", "3") ?: named(Hatch.shellOf(h), "3") ?: h
        return Room(h, pool, scratch)
    }

    /** The message as it will go to [room], the hatched shell's own, or a plain reason it can't. */
    fun prepare(pool: String, message: String, room: Room?): PoolMessage {
        require(room != null) { "Posting waits for your shell: the room a post goes to is the one your shell names." }
        val author = room.author
        val name = pool.trim().removePrefix("pool:")
        require(name == room.pool) { "The pocket posts only to your own room, pool:${room.pool}, so not to \"$pool\"." }
        val text = message.trim()
        require(text.isNotEmpty()) { "The message is empty." }
        require(text.length <= MAX_CHARS) { "The message is ${text.length} characters; the pocket posts at most $MAX_CHARS." }
        val body = stringify(JsObj.of("append" to JsBool(true), "content" to JsObj.of("_" to str(text), "1" to str(author))))!!
        return PoolMessage(name, author, text, "$BEACH/.well-known/pscale-beach?block=pool:$name", body, scratch = room.scratch)
    }

    /** Send it, once. A message made in a scratch pocket is refused here, before the wire is touched. */
    fun send(m: PoolMessage, wire: Wire): Posted {
        if (m.scratch) return Posted.Refused(SCRATCH)
        val reply = try {
            wire.post(m.url, m.body)
        } catch (e: NoAnswer) {
            return Posted.Unreached("The message went, but no answer came back from the beach (${e.message ?: e::class.simpleName}), so it may have landed.", mayHaveLanded = true)
        } catch (e: Exception) {
            return Posted.Unreached("The beach couldn't be reached, so nothing was posted (${e.message ?: e::class.simpleName}).")
        }
        val answer = try { parseJson(reply.text) as? JsObj } catch (_: JsonSyntaxError) { null }
        val error = (answer?.get("error") as? JsStr)?.value
        val code = (answer?.get("code") as? JsStr)?.value
        return when {
            reply.status in 200..299 -> {
                val slot = answer?.get("slot")?.let(::jsString) ?: answer?.get("address")?.let(::jsString)
                if (slot != null) Posted.Landed(m.pool, slot) else Posted.Refused("The beach answered without saying where the message landed: ${reply.text.take(200)}")
            }
            reply.status == 403 && code == "lock_required" ->
                Posted.Refused("pool:${m.pool} now needs a key, so nothing was posted. The pocket keeps no key for it yet.")
            reply.status == 429 -> Posted.Refused("The beach is taking no more for the moment: ${error ?: "try again shortly"}.")
            else -> Posted.Refused("The beach refused the message (${reply.status}): ${error ?: reply.text.take(200)}")
        }
    }
}
