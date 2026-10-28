package com.phenomemental.pocketbeach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.phenomemental.pocketbeach.bsp.JsArr
import com.phenomemental.pocketbeach.bsp.JsBool
import com.phenomemental.pocketbeach.bsp.JsNum
import com.phenomemental.pocketbeach.bsp.JsObj
import com.phenomemental.pocketbeach.bsp.HelloWorld
import com.phenomemental.pocketbeach.bsp.PoolMessage
import com.phenomemental.pocketbeach.bsp.PoolPost
import com.phenomemental.pocketbeach.bsp.Posted
import com.phenomemental.pocketbeach.bsp.Region
import com.phenomemental.pocketbeach.bsp.str
import com.phenomemental.pocketbeach.bsp.stringify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The fields of one bsp() call, as typed. An empty field is left out of the call. */
private class Fields {
    var agentId by mutableStateOf("pocket")
    var block by mutableStateOf("")
    var spindle by mutableStateOf("")
    var pscale by mutableStateOf("")
    var content by mutableStateOf("")
    var append by mutableStateOf(false)
    var secret by mutableStateOf("")
    var newLock by mutableStateOf("")
    var gray by mutableStateOf(false)
    var encSecret by mutableStateOf("")
    var members by mutableStateOf("")
    var face by mutableStateOf("")
    var tier by mutableStateOf("")

    fun toCall(): JsObj {
        val p = JsObj()
        fun text(key: String, v: String) { if (v.isNotEmpty()) p[key] = str(v) }
        text("agent_id", agentId.trim())
        text("block", block)
        text("spindle", spindle.trim())
        pscale.trim().takeIf { it.isNotEmpty() }?.let { p["pscale_attention"] = it.toIntOrNull()?.let { n -> JsNum(n.toDouble()) } ?: str(it) }
        text("content", content)
        if (append) p["append"] = JsBool(true)
        text("secret", secret)
        text("new_lock", newLock)
        if (gray) p["gray"] = JsBool(true)
        text("enc_secret", encSecret)
        members.split(',').map { it.trim() }.filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }
            ?.let { list -> p["members"] = JsArr(list.map { str(it) }.toMutableList()) }
        text("face", face.trim())
        text("tier", tier.trim())
        return p
    }
}

/** "Try a call by hand": the hello world by hand, then every bsp() parameter as a field, Run, and the raw reply. */
@Composable
fun CallsScreen(model: Model, nav: Nav) {
    val pocket = model.app.pocket
    val f = rememberSaveable(saver = androidx.compose.runtime.saveable.Saver<Fields, List<Any>>(
        save = { listOf(it.agentId, it.block, it.spindle, it.pscale, it.content, it.append, it.secret, it.newLock, it.gray, it.encSecret, it.members, it.face, it.tier) },
        restore = { v ->
            Fields().apply {
                agentId = v[0] as String; block = v[1] as String; spindle = v[2] as String; pscale = v[3] as String
                content = v[4] as String; append = v[5] as Boolean; secret = v[6] as String; newLock = v[7] as String
                gray = v[8] as Boolean; encSecret = v[9] as String; members = v[10] as String; face = v[11] as String; tier = v[12] as String
            }
        },
    )) { Fields() }
    var sent by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    var took by rememberSaveable { mutableStateOf("") }
    var running by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Page(onBack = { nav.back() }) {
        HelloByHand(model)
        HorizontalDivider()
        Text("A call by hand", style = Type.heading, color = LocalInk.current.ink)
        Text(
            "Each field is one bsp() parameter; an empty field is left out. agent_id \"pocket\" is your blocks on this phone, \"pscale\" the teaching blocks.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { f.agentId = "pscale"; f.block = "whetstone"; f.spindle = ""; f.pscale = ""; f.content = ""; f.append = false }) {
                Text("The whetstone")
            }
            OutlinedButton(onClick = { f.agentId = "pocket"; f.block = ""; f.spindle = ""; f.pscale = ""; f.content = ""; f.append = false }) {
                Text("My blocks")
            }
        }

        Field("agent_id", f.agentId) { f.agentId = it }
        Field("block", f.block) { f.block = it }
        Field("spindle", f.spindle) { f.spindle = it }
        Field("pscale_attention", f.pscale, numeric = true) { f.pscale = it }
        Field("content", f.content, lines = 4) { f.content = it }
        Toggle("append", f.append) { f.append = it }

        Spacer(Modifier.padding(2.dp))
        Text("Refused in this version — the phone is the latch", style = MaterialTheme.typography.labelLarge)
        Field("secret", f.secret) { f.secret = it }
        Field("new_lock", f.newLock) { f.newLock = it }
        Toggle("gray", f.gray) { f.gray = it }
        Field("enc_secret", f.encSecret) { f.encSecret = it }
        Field("members (comma-separated)", f.members, tag = "members") { f.members = it }
        Text("Accepted and ignored, as bsp-mcp-server ignores them", style = MaterialTheme.typography.labelLarge)
        Field("face", f.face) { f.face = it }
        Field("tier", f.tier) { f.tier = it }

        Button(
            onClick = {
                val call = f.toCall()
                sent = stringify(call) ?: "{}"
                running = true
                scope.launch {
                    val started = System.nanoTime()
                    val reply = withContext(Dispatchers.IO) { pocket.call(call) }
                    took = "took ${(System.nanoTime() - started) / 1_000_000} ms"
                    result = reply
                    running = false
                }
            },
            enabled = !running,
            modifier = Modifier.fillMaxWidth().testTag("run"),
        ) { Text(if (running) "Running…" else "Run") }

        if (sent.isNotEmpty()) {
            HorizontalDivider()
            Text("Sent", style = MaterialTheme.typography.labelLarge)
            SelectionContainer { Text(sent, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall) }
        }
        if (result.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Result", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Text(took, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("took"))
            }
            SelectionContainer {
                Text(result, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("result"))
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, lines: Int = 1, numeric: Boolean = false, tag: String = label, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lines == 1,
        minLines = lines,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange, modifier = Modifier.testTag(label))
    }
}

/**
 * The hello world by hand (decision 17), so the chain can be shown without
 * the companion: 1, make hello-world, the shell's opening line, the secret,
 * a seal the app makes and the fingerprint, each a guarded write; 2, after
 * its own confirmation, post the one message to pool:Phenomemental.
 */
@Composable
private fun HelloByHand(model: Model) {
    val ink = LocalInk.current
    val scope = rememberCoroutineScope()
    var secret by rememberSaveable { mutableStateOf("") }
    var made by rememberSaveable { mutableStateOf("") }
    var said by rememberSaveable { mutableStateOf("") }
    var confirming by androidx.compose.runtime.remember { mutableStateOf<PoolMessage?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val stored by androidx.compose.runtime.produceState<String?>(null, model.version) {
        value = withContext(Dispatchers.IO) { model.app.storage.read(HelloWorld.BLOCK) }
    }
    val message = stored?.let(HelloWorld::messageFor)

    Text("The hello world by hand", style = Type.heading, color = ink.ink)
    Text(
        "The same two steps the companion offers, without it. Step 1 makes the block ${HelloWorld.BLOCK}; step 2 posts one message to " +
            "pool:${HelloWorld.POOL} on the beach, after you confirm it.",
        style = MaterialTheme.typography.bodySmall,
    )
    Field("Your secret", secret, tag = "hello-secret") { secret = it }
    Button(
        onClick = {
            busy = true
            scope.launch {
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        val opening = model.pages.position(Region.POCKET, HelloWorld.SHELL)?.text ?: HelloWorld.SHELL_OPENING
                        HelloWorld.makeByHand(model.pages, model.app.storage, opening, secret)
                    }
                }
                busy = false
                model.changed()
                outcome.onSuccess { m -> made = "Made ${HelloWorld.BLOCK}. Seal ${m.seal}, fingerprint ${m.fingerprint}."; said = "" }
                    .onFailure { made = it.message ?: "It wasn't made." }
            }
        },
        enabled = !busy && secret.isNotBlank(),
        modifier = Modifier.fillMaxWidth().testTag("hello-make"),
    ) { Text("1. Make ${HelloWorld.BLOCK}") }
    if (made.isNotEmpty()) Text(made, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("hello-made"))
    stored?.let { SelectionContainer { Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("hello-block")) } }
    if (stored != null && message == null) {
        Text("${HelloWorld.BLOCK} has no fingerprint that matches it, so there's nothing to post from it.", style = MaterialTheme.typography.bodySmall)
    }
    Button(
        onClick = { confirming = runCatching { PoolPost.prepare(HelloWorld.POOL, message!!) }.getOrNull() },
        enabled = !busy && message != null,
        modifier = Modifier.fillMaxWidth().testTag("hello-post"),
    ) { Text("2. Post it to pool:${HelloWorld.POOL}…") }
    if (said.isNotEmpty()) Text(said, style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("hello-said"))

    confirming?.let { m ->
        PaperDialog(
            title = "Post this to the beach?",
            onDismiss = { confirming = null },
            actions = {
                Action("Not now", { confirming = null }, Modifier.weight(1f), tag = "hello-not-now")
                Action("Post it", {
                    confirming = null
                    busy = true
                    scope.launch {
                        val posted = withContext(Dispatchers.IO) { PoolPost.send(m, model.app.wire) }
                        busy = false
                        said = when (posted) {
                            is Posted.Landed -> "Posted. It landed at slot ${posted.slot} of pool:${posted.pool}."
                            is Posted.Refused -> posted.why
                        }
                    }
                }, Modifier.weight(1f), strong = true, tag = "hello-confirm")
            },
        ) {
            Text(m.message, style = Type.body, color = ink.ink, modifier = Modifier.testTag("hello-message"))
            Text("To pool:${m.pool} at beach.happyseaurchin.com, under the name ${m.author}. It can't be taken back.", style = Type.small, color = ink.soft)
        }
    }
}
