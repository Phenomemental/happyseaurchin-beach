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
    /** POST [body] (JSON) to [url], returning the status and the text the beach answered. Failing to connect throws. */
    fun post(url: String, body: String): WireReply
}

class WireReply(val status: Int, val text: String)

/** A message ready to post, shown whole to Matthew before he confirms. */
class PoolMessage internal constructor(val pool: String, val author: String, val message: String, val url: String, val body: String)

sealed class Posted {
    /** The beach took it, at [slot] of the pool. */
    class Landed(val pool: String, val slot: String) : Posted()

    /** Nothing landed, and why, in plain words. */
    class Refused(val why: String) : Posted()
}

object PoolPost {
    const val BEACH = "https://beach.happyseaurchin.com"
    const val AUTHOR = "Phenomemental"
    const val MAX_CHARS = 2000

    /** The pools the pocket may post to: Matthew's own, and no other, until he says otherwise. */
    val POOLS = setOf("Phenomemental")

    /** The message as it will go, or a plain reason it can't. */
    fun prepare(pool: String, message: String, author: String = AUTHOR): PoolMessage {
        val name = pool.trim().removePrefix("pool:")
        require(name in POOLS) { "The pocket posts only to ${POOLS.joinToString { "pool:$it" }}, so not to \"$pool\"." }
        val text = message.trim()
        require(text.isNotEmpty()) { "The message is empty." }
        require(text.length <= MAX_CHARS) { "The message is ${text.length} characters; the pocket posts at most $MAX_CHARS." }
        val body = stringify(JsObj.of("append" to JsBool(true), "content" to JsObj.of("_" to str(text), "1" to str(author))))!!
        return PoolMessage(name, author, text, "$BEACH/.well-known/pscale-beach?block=pool:$name", body)
    }

    /** Send it, once. */
    fun send(m: PoolMessage, wire: Wire): Posted {
        val reply = try {
            wire.post(m.url, m.body)
        } catch (e: Exception) {
            return Posted.Refused("The beach couldn't be reached, so nothing was posted (${e.message ?: e::class.simpleName}).")
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
