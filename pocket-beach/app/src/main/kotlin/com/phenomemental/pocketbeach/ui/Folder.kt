package com.phenomemental.pocketbeach.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.phenomemental.pocketbeach.FOLDER
import com.phenomemental.pocketbeach.bsp.Move
import com.phenomemental.pocketbeach.bsp.SetAside
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Sundial 0.11: his blocks in a folder he can see (spine:pocket-beach
 * 1.111178719942 to 1.111178719943): the permission checked at every start
 * and return, one plain page without it; Settings naming where his blocks
 * are kept and offering the move; the files Sundial can't read, listed and
 * left as they are; a held write shown with its two answers; and the folder
 * watched for outside changes.
 */

/** Where the app stands in its life: how many times it has come to the front, and whether it is there now. */
class Life(val resumes: Int, val resumed: Boolean)

@Composable
fun rememberLife(): Life {
    val owner = LocalLifecycleOwner.current
    var resumes by remember { mutableIntStateOf(if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) 1 else 0) }
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val watch = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { resumes++; resumed = true }
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        owner.lifecycle.addObserver(watch)
        onDispose { owner.lifecycle.removeObserver(watch) }
    }
    return Life(resumes, resumed)
}

/** Android's page where Sundial's all-files access is turned on or off. */
fun openAllFilesAccess(context: Context) {
    val own = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
    try {
        context.startActivity(own)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
    }
}

/** The permission's plain line (spine:pocket-beach 1.111178719943): said truly, what it reaches. */
const val ACCESS_LINE = "Android asks for all-files access for this. It reaches all of this phone's shared storage, beyond Documents/$FOLDER, " +
    "though Sundial touches only its own folder. Turn it on for Sundial, then come back here."

/**
 * The one plain page when his blocks are in his folder and Sundial can't
 * reach it: nothing is shown and nothing is written, never an empty pocket.
 */
@Composable
fun BarredPage() {
    val ink = LocalInk.current
    val context = LocalContext.current
    Page(onBack = null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp).testTag("barred")) {
            Text("Sundial can't reach your blocks", style = Type.title, color = ink.ink)
            Stroke()
        }
        Text(
            "Your blocks are kept in Documents/$FOLDER on this phone, and Android has taken back Sundial's all-files access, " +
                "which it needs to reach them. They are still there, as you left them.",
            style = Type.body, color = ink.ink,
        )
        Text("Nothing is shown and nothing is written until the access is given back.", style = Type.small, color = ink.soft)
        Action("Open Android's settings", { openAllFilesAccess(context) }, Modifier.fillMaxWidth(), strong = true, tag = "barred-open-settings")
    }
}

/**
 * While his blocks are in his folder and Sundial is in front: on every
 * return, and every few seconds after, the folder is looked at, and when
 * anything in it moved (a sync from the laptop, an edit elsewhere), every
 * page reads again.
 */
@Composable
fun FolderWatch(model: Model, life: Life) {
    LaunchedEffect(life.resumes, life.resumed) {
        if (!life.resumed) return@LaunchedEffect
        while (true) {
            val print = withContext(Dispatchers.IO) { model.app.folder?.fingerprint() }
            if (print != null && print != model.folderPrint) {
                val first = model.folderPrint == null
                model.folderPrint = print
                if (!first) model.changed()
            }
            delay(WATCH_MS)
        }
    }
}

/** How often the folder is looked at while Sundial is in front. */
const val WATCH_MS = 3_000L

/** Settings: where his blocks are kept, and the move into his folder (spine:pocket-beach 1.111178719932 to 1.111178719933). */
@Composable
fun WhereKept(model: Model) {
    val ink = LocalInk.current
    val app = model.app
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val life = rememberLife()
    var asking by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf<Move.Check?>(null) }
    var said by remember { mutableStateOf<String?>(null) }
    var moving by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    var copyStands by remember { mutableStateOf(false) }
    val access = remember(life.resumes) { app.allFilesAccess() }
    val hatched = remember(model.version) { app.storage.isOpen }
    LaunchedEffect(life.resumes, model.version, asking, access) {
        copyStands = withContext(Dispatchers.IO) { app.privateCopyStands }
        check = if (asking && access && !app.inFolder) withContext(Dispatchers.IO) { app.moveCheck() } else null
    }
    Column(Modifier.fillMaxWidth().testTag("settings-where-kept"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Rule()
        Text("Where your blocks are kept", style = Type.heading, color = ink.ink)
        if (app.inFolder) {
            Text(
                "In Documents/$FOLDER on this phone, one file for each block, which you, your laptop and Claude can open. " +
                    "Your shell, your history and the app's own blocks stay inside the app.",
                style = Type.small, color = ink.soft, modifier = Modifier.testTag("kept-in-folder"),
            )
            if (copyStands) {
                Text("The copy kept inside the app from before the move is still there. Nothing reads it.", style = Type.small, color = ink.soft)
                TextLink("Remove the copy kept inside the app", { removing = true }, tag = "remove-private-copy")
            }
        } else {
            Text("Inside the app, on this phone only. Nothing else can see them.", style = Type.small, color = ink.soft, modifier = Modifier.testTag("kept-inside"))
            when {
                !hatched -> Text("Once you have a shell on this phone, they can be kept in Documents/$FOLDER instead.", style = Type.small, color = ink.soft)
                !asking -> {
                    Text(
                        "They can be kept in Documents/$FOLDER instead, where you, your laptop and Claude can open them. " +
                            "Your shell, your history and the app's own blocks stay inside the app.",
                        style = Type.small, color = ink.soft,
                    )
                    TextLink("Keep them in Documents/$FOLDER", { asking = true; said = null; app.folderOffered = true }, tag = "keep-in-folder")
                }
                !access -> {
                    Text(ACCESS_LINE, style = Type.small, color = ink.ink, modifier = Modifier.testTag("access-line"))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Action("Not now", { asking = false }, Modifier.weight(1f), tag = "access-not-now")
                        Action("Open Android's settings", { openAllFilesAccess(context) }, Modifier.weight(1f), strong = true, tag = "access-open-settings")
                    }
                }
                else -> {
                    val c = check
                    when {
                        c == null -> Text("Looking at Documents/$FOLDER…", style = Type.small, color = ink.soft)
                        c.refusal != null -> {
                            Text(c.refusal!!, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("move-refusal"))
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                TextLink("Look again", { scope.launch { check = withContext(Dispatchers.IO) { app.moveCheck() } } }, tag = "move-look-again")
                                TextLink("Not now", { asking = false }, tag = "move-not-now")
                            }
                        }
                        else -> {
                            Text(
                                "${Words.count(c.moving.size, "block")} will go into Documents/$FOLDER" +
                                    (if (c.keptPrivate.isNotEmpty()) "; ${c.keptPrivate.joinToString(", ")} stay inside the app" else "") + ". " +
                                    "A whole copy is saved first, then each block is copied and checked, and only then are they read from the folder. " +
                                    "The copy inside the app stays until you remove it.",
                                style = Type.small, color = ink.ink, modifier = Modifier.testTag("move-what"),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Action("Not now", { asking = false }, Modifier.weight(1f), tag = "move-cancel")
                                Action(if (moving) "Moving…" else "Move them", {
                                    moving = true
                                    scope.launch {
                                        val moved = withContext(Dispatchers.IO) { app.move() }
                                        moving = false
                                        asking = false
                                        said = if (moved.ok) "Moved ${Words.count(moved.moved.size, "block")} into Documents/$FOLDER. Each was read back and matched." else moved.problem
                                        model.changed()
                                    }
                                }, Modifier.weight(1f), enabled = !moving, strong = true, tag = "move-them")
                            }
                        }
                    }
                }
            }
        }
        said?.let { Text(it, style = Type.small, color = ink.ink, modifier = Modifier.testTag("move-said")) }
    }
    if (removing) PaperDialog(
        "Remove the copy kept inside the app?",
        onDismiss = { removing = false },
        actions = {
            Action("Keep it", { removing = false })
            Action("Remove it", {
                removing = false
                scope.launch {
                    withContext(Dispatchers.IO) { app.removePrivateCopy() }
                    said = "Removed the copy kept inside the app. Your blocks are in Documents/$FOLDER."
                    model.changed()
                }
            }, strong = true, tag = "remove-private-copy-yes")
        },
    ) {
        Text("It is the copy of your blocks from before they moved to Documents/$FOLDER. Nothing reads it now.", style = Type.body, color = ink.ink)
        Text("Your blocks in the folder, your shell and your history stay as they are.", style = Type.small, color = ink.soft)
    }
}

/** Your pocket, once: the folder offered, then never again unless he goes to Settings (spine:pocket-beach 1.111178719932). */
@Composable
fun FolderOffer(model: Model, nav: Nav) {
    val app = model.app
    var gone by remember { mutableStateOf(false) }
    if (gone || app.inFolder || app.folderOffered || !app.storage.isOpen) return
    Column {
        Notice(
            "Your blocks can be kept in a folder you can see, Documents/$FOLDER, so your laptop and Claude can open them too.",
            link = "See how, in Settings" to { app.folderOffered = true; nav.go(Screen.Settings) },
            tag = "folder-offer",
        )
        TextLink("Not now", { app.folderOffered = true; gone = true }, tag = "folder-offer-not-now")
    }
}

/** Your pocket: the files in his folder Sundial can't read as blocks, each with why, left as they are. */
@Composable
fun SetAsideFiles(files: List<SetAside>) {
    if (files.isEmpty()) return
    val ink = LocalInk.current
    Label("Files Sundial can't read", Modifier.padding(top = 6.dp))
    Text("They are in Documents/$FOLDER and are left as they are: Sundial never writes over them.", style = Type.small, color = ink.soft)
    Sheet(Modifier.testTag("set-aside")) {
        files.forEachIndexed { i, f ->
            if (i > 0) Rule()
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {}.testTag("set-aside:${f.file}")) {
                Text(f.file, style = Type.name, color = ink.ink)
                Text("It ${f.why}.", style = Type.aside, color = ink.soft)
            }
        }
    }
}

/**
 * A write held because the block changed since he saw it (spine:pocket-beach
 * 1.111178719943): what happened, and its two answers.
 */
@Composable
fun HeldNotice(why: String, again: () -> Unit, letGo: () -> Unit, tag: String = "held") {
    val ink = LocalInk.current
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Notice("$why It may have changed on another device or in another app.")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Action("Let it go", letGo, Modifier.weight(1f), tag = "$tag-let-go")
            Action("Plan it again on this version", again, Modifier.weight(1f), strong = true, tag = "$tag-again")
        }
        Text("Planning it again shows where it would land now; nothing is written until you say so.", style = Type.small, color = ink.soft)
    }
}
