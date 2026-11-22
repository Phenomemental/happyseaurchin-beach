package com.phenomemental.pocketbeach.model

/**
 * Whether a model can be asked yet, and if not why: on the phone, the
 * model's file as ModelStore finds it; on a laptop, whatever the front end
 * says. Sundial 0.12: the core's, with the rest of the mind's contract.
 */
sealed interface ModelState {
    class TooLittleMemory(val has: Long) : ModelState
    class TooLittleRoom(val free: Long, val needs: Long) : ModelState
    data object Absent : ModelState
    /** Fetching now: [done] of its size, at [bytesPerSecond]; [note] says what it is doing when it isn't plain fetching. */
    class Fetching(val done: Long, val bytesPerSecond: Long, val note: String?) : ModelState
    /** Part of it is on the phone, and nothing is fetching: it carries on from [done]. */
    class Paused(val done: Long) : ModelState
    data object Checking : ModelState
    /** It stopped for [why], with [done] bytes kept to carry on from. */
    class Failed(val why: String, val done: Long) : ModelState
    data object Ready : ModelState
}
