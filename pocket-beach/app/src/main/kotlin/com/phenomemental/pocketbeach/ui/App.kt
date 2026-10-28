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
        }

        private fun decode(text: String): Screen? {
            val p = text.split(SEP)
            return when (p[0]) {
                "shore" -> Screen.Shore
                "block" -> Screen.Block(Region.valueOf(p[1]), p[2], p[3])
                "write" -> Screen.Write(Kind.valueOf(p[1]), p[2], p[3])
                "about" -> Screen.About
                "calls" -> Screen.Calls
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

    fun changed() { version++ }

    fun say(on: Screen, text: String) { slip = on to text }

    fun saved(ms: Long) {
        app.lastSavedMs = ms
        lastSavedMs = ms
    }
}

@Composable
fun PocketScreens(app: PocketApp) {
    val nav = rememberSaveable(saver = Nav.saver) { Nav(listOf(Screen.Shore)) }
    val model = remember { Model(app) }
    BackHandler(enabled = nav.stack.size > 1) { nav.back() }
    val slip = model.slip?.takeIf { it.first == nav.top }?.let { SlipNote(it.second) { model.slip = null } }
    CompositionLocalProvider(LocalSlip provides slip) {
        when (val s = nav.top) {
            Screen.Shore -> ShoreScreen(model, nav)
            is Screen.Block -> BlockScreen(model, nav, s)
            is Screen.Write -> WriteScreen(model, nav, s)
            Screen.About -> AboutScreen(model, nav)
            Screen.Calls -> CallsScreen(app.pocket, nav)
        }
    }
    model.slip?.let { said ->
        LaunchedEffect(said) {
            delay(6000)
            if (model.slip == said) model.slip = null
        }
    }
}
