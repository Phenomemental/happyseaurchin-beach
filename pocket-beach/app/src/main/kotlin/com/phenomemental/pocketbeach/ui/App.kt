package com.phenomemental.pocketbeach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.phenomemental.pocketbeach.PocketApp
import com.phenomemental.pocketbeach.bsp.Kind
import com.phenomemental.pocketbeach.bsp.Region
import kotlinx.coroutines.delay

/** The app's pages. */
sealed interface Screen {
    data object Shore : Screen
    /** A block, standing at [address] in it ("" its opening). */
    data class Block(val region: Region, val name: String, val address: String) : Screen
    data class Write(val kind: Kind, val block: String, val address: String) : Screen
    data object About : Screen
    data object Calls : Screen
    data object Ask : Screen
    data object Received : Screen
    data object Bench : Screen
    data object Settings : Screen
    /** Sundial 0.10: Your days (the strandline, newest first; held, where they have come to rest). */
    data object Days : Screen
    /** Sundial 0.10: Your room, a box and Post. */
    data object Room : Screen
    /** Sundial 0.10: Your hand, from his ahead. */
    data object Hand : Screen
    /** Sundial 0.10: Your recipes, his cook and kernel. */
    data object Recipes : Screen
    /** Sundial 0.10.1: a kept day as its page (its title, lines, Tomorrow and its places line): from its shell, and from Your days. */
    data class Day(val address: String) : Screen
    /** Sundial 0.12: his older conversations, listed. */
    data object Talks : Screen
    /** Sundial 0.12: one older conversation, begun at [startedMs]. */
    data class Talk(val startedMs: Long) : Screen
    /** Sundial 0.12: one of his blocks in pscale structure, to edit. */
    data class Editor(val name: String) : Screen
    /** Sundial 0.12: a text file in his Sundial folder, read as plain text. */
    data class TextFile(val name: String) : Screen
    /** Sundial 0.12: the walker, a block walked as the beach's bsp() walks it, his own or (with Reach the beach on) the beach's. */
    data class Walker(val beach: Boolean, val block: String) : Screen
}

/** The way back: a stack of pages, kept across a turn of the phone. */
class Nav(initial: List<Screen>) {
    val stack = mutableStateListOf<Screen>().also { it.addAll(initial) }
    val top: Screen get() = stack.last()

    fun go(screen: Screen) { stack.add(screen) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replace(screen: Screen) { stack[stack.lastIndex] = screen }

    companion object {
        private const val SEP = "\u0001"

        private fun encode(s: Screen): String = when (s) {
            Screen.Shore -> "shore"
            is Screen.Block -> listOf("block", s.region.name, s.name, s.address).joinToString(SEP)
            is Screen.Write -> listOf("write", s.kind.name, s.block, s.address).joinToString(SEP)
            Screen.About -> "about"
            Screen.Calls -> "calls"
            Screen.Ask -> "ask"
            Screen.Received -> "received"
            Screen.Bench -> "bench"
            Screen.Settings -> "settings"
            Screen.Days -> "days"
            Screen.Room -> "room"
            Screen.Hand -> "hand"
            Screen.Recipes -> "recipes"
            is Screen.Day -> listOf("day", s.address).joinToString(SEP)
            Screen.Talks -> "talks"
            is Screen.Talk -> listOf("talk", s.startedMs.toString()).joinToString(SEP)
            is Screen.Editor -> listOf("editor", s.name).joinToString(SEP)
            is Screen.TextFile -> listOf("textfile", s.name).joinToString(SEP)
            is Screen.Walker -> listOf("walker", if (s.beach) "beach" else "pocket", s.block).joinToString(SEP)
        }

        private fun decode(text: String): Screen? {
            val p = text.split(SEP)
            return when (p[0]) {
                "shore" -> Screen.Shore
                "block" -> Screen.Block(Region.valueOf(p[1]), p[2], p[3])
                "write" -> Screen.Write(Kind.valueOf(p[1]), p[2], p[3])
                "about" -> Screen.About
                "calls" -> Screen.Calls
                "ask" -> Screen.Ask
                "received" -> Screen.Received
                "bench" -> Screen.Bench
                "settings" -> Screen.Settings
                "days" -> Screen.Days
                "room" -> Screen.Room
                "hand" -> Screen.Hand
                "recipes" -> Screen.Recipes
                "day" -> Screen.Day(p[1])
                "talks" -> Screen.Talks
                "talk" -> p.getOrNull(1)?.toLongOrNull()?.let { Screen.Talk(it) }
                "editor" -> p.getOrNull(1)?.let { Screen.Editor(it) }
                "textfile" -> p.getOrNull(1)?.let { Screen.TextFile(it) }
                "walker" -> Screen.Walker(p.getOrNull(1) == "beach", p.getOrNull(2).orEmpty())
                else -> null
            }
        }

        val saver = Saver<Nav, ArrayList<String>>(
            save = { nav -> ArrayList(nav.stack.map(::encode)) },
            restore = { saved -> Nav(saved.mapNotNull(::decode).ifEmpty { listOf(Screen.Shore) }) },
        )
    }
}

/** What the pages share: the pocket, a passing message, the line just written, and a count that moves whenever a block does. */
class Model(val app: PocketApp) {
    val pages get() = app.pages
    var version by mutableIntStateOf(0)
    /** A passing message, and the screen it belongs to: it shows there only. */
    var slip by mutableStateOf<Pair<Screen, String>?>(null)
    var landed by mutableStateOf<Pair<String, String>?>(null)
    var lastSavedMs by mutableStateOf(app.lastSavedMs)
    /** Sundial 0.10: Your places asked for the conversation saved as a file; Today hears it and asks where. */
    var saveConversation by mutableIntStateOf(0)

    /** Sundial 0.11: how his folder last looked, so a change made outside the app is noticed (FolderWatch). */
    var folderPrint: String? = null

    fun changed() { version++ }

    init { app.asking.changed = { changed() } }

    fun say(on: Screen, text: String) { slip = on to text }

    fun saved(ms: Long) {
        app.lastSavedMs = ms
        lastSavedMs = ms
    }
}

@Composable
fun PocketScreens(app: PocketApp) {
    // 0.8.1: once he has hatched, the app opens on the chat; the Shore stays beneath it, a tap away in the menu.
    val nav = rememberSaveable(saver = Nav.saver) {
        Nav(if (runCatching { com.phenomemental.pocketbeach.bsp.Hatch.hatched(app.storage) }.getOrNull() != null) listOf(Screen.Shore, Screen.Ask) else listOf(Screen.Shore))
    }
    val model = remember { Model(app) }
    // Sundial 0.11 (spine:pocket-beach 1.111178719943): the permission checked at every start and return. Without it,
    // while his blocks are in his folder, one plain page and no writes, never an empty pocket.
    val life = rememberLife()
    val barred = remember(life.resumes) { app.barred() != null }
    if (barred) {
        BarredPage()
        return
    }
    FolderWatch(model, life)
    // Sundial 0.10: the three lines on every screen open Your places, Sundial's own menu.
    var places by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = places) { places = false }
    BackHandler(enabled = !places && nav.stack.size > 1) { nav.back() }
    val slip = model.slip?.takeIf { it.first == nav.top }?.let { SlipNote(it.second) { model.slip = null } }
    CompositionLocalProvider(LocalSlip provides slip, LocalPlaces provides { places = true }) {
        when (val s = nav.top) {
            Screen.Shore -> ShoreScreen(model, nav)
            is Screen.Block -> BlockScreen(model, nav, s)
            is Screen.Write -> WriteScreen(model, nav, s)
            Screen.About -> AboutScreen(model, nav)
            Screen.Calls -> CallsScreen(model, nav)
            Screen.Ask -> AskScreen(model, nav)
            Screen.Received -> ReceivedScreen(model, nav)
            Screen.Bench -> BenchScreen(model, nav)
            Screen.Settings -> SettingsScreen(model, nav)
            Screen.Days -> DaysScreen(model, nav)
            is Screen.Day -> DayScreen(model, nav, s.address)
            Screen.Talks -> TalksScreen(model, nav)
            is Screen.Talk -> TalkScreen(model, nav, s.startedMs)
            is Screen.Editor -> EditorScreen(model, nav, s.name)
            is Screen.TextFile -> TextFileScreen(model, nav, s.name)
            is Screen.Walker -> WalkerScreen(model, nav, s)
            Screen.Room -> RoomScreen(model, nav)
            Screen.Hand -> HandScreen(model, nav)
            Screen.Recipes -> RecipesScreen(model, nav)
        }
        if (places) PlacesSheet(model, nav) { places = false }
    }
    // A block shared in from another app is shown as soon as it arrives.
    LaunchedEffect(app.inbox) {
        // Sundial 0.12: text shared in lands in the chat as a file added to his next message; a block still opens to be kept.
        val received = app.inbox
        if (received != null && received.block.isFailure) {
            app.asking.attach(com.phenomemental.pocketbeach.bsp.Chats.Attached(received.subject?.trim()?.takeIf { it.isNotEmpty() } ?: "shared text", received.text, block = false))
            app.inbox = null
            if (nav.top != Screen.Ask) nav.go(Screen.Ask)
        } else if (received != null && nav.top != Screen.Received) nav.go(Screen.Received)
    }
    model.slip?.let { said ->
        LaunchedEffect(said) {
            delay(6000)
            if (model.slip == said) model.slip = null
        }
    }
}
