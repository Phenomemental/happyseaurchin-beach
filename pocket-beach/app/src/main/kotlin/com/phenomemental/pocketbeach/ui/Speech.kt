package com.phenomemental.pocketbeach.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.phenomemental.pocketbeach.PocketApp
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What a recogniser tells the one listening: ready, words so far, its words at the end, or its error. */
interface Heard {
    fun ready()
    fun partial(words: String)
    fun done(words: String?)
    fun failed(error: Int)
}

/** A recogniser, as Speak uses one: listen once, then be let go. The phone's own is [OnDeviceRecogniser]; the device tests give their own. */
interface Recogniser {
    fun listen(heard: Heard)
    fun destroy()
}

/** The phone's own on-device recogniser, which never sends the voice to a server. */
class OnDeviceRecogniser(context: Context) : Recogniser {
    private val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

    override fun listen(heard: Heard) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = heard.ready()
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) =
                heard.partial(partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty())
            override fun onResults(results: Bundle?) =
                heard.done(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
            override fun onError(error: Int) = heard.failed(error)
        })
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // 0.6.1: his own sentences, punctuated, where the phone can (Android 13 on), so the pocket cuts his speech where he paused.
                .apply { if (android.os.Build.VERSION.SDK_INT >= 33) putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY) },
        )
    }

    override fun destroy() = recognizer.destroy()
}

/**
 * The one recogniser the app holds, at most, at any time. It is made on a
 * tap and let go (destroyed) when listening ends, when Matthew stops it, on
 * any error, when its screen is left and when the app goes to the
 * background, so the phone's speech service is free for others, Gboard's
 * voice typing among them. Nothing is ever tried again by itself: when the
 * phone says its speech is busy, nothing is started for a few seconds, and
 * he is told so.
 */
class Speech {
    companion object {
        /** How long the phone's speech is left alone after it says it is busy. */
        const val BUSY_MS = 5_000L

        /**
         * 0.7: how long after the keyboard has gone listening waits. With the
         * keyboard up, Gboard's voice typing holds the phone's on-device speech
         * (his Pixel said "busy, error 10" on 0.3.5, 0.4 and 0.6.4, when 0.4
         * started 400 ms after hiding it): the keyboard gone from the screen is
         * not Gboard letting its speech go.
         */
        const val KEYBOARD_MS = 1_000L

        /** The longest listening waits for the keyboard to go. */
        const val KEYBOARD_WAIT_MS = 3_000L

        /** 0.7: busy once, the one try more he sees comes this long after. */
        const val RETRY_MS = 1_000L
    }

    /** Makes the recogniser on a tap: the phone's own, on the device. The device tests put their own here. */
    var make: (Context) -> Recogniser = { OnDeviceRecogniser(it) }

    /** Whether the phone can recognise speech on the device at all. */
    var available: (Context) -> Boolean = ::onDeviceSpeech

    private var held: Recogniser? = null
    private var owner: Any? = null
    private var busyUntil = 0L

    /** The phone's error when it last said its speech was busy (10 or 8), for the message. */
    var busyError = 0
        private set

    /** Recognisers made and let go so far, for the device tests. */
    var made = 0
        private set
    var destroyed = 0
        private set

    val holding: Boolean get() = held != null

    val busy: Boolean get() = SystemClock.elapsedRealtime() < busyUntil

    /** Listen for [owner], letting go any other recogniser first. False, with nothing made, while the phone's speech is busy, unless this is the [retry] he sees. */
    fun start(context: Context, owner: Any, heard: Heard, retry: Boolean = false): Boolean {
        if (busy && !retry) return false
        letGo()
        val r = make(context)
        held = r
        this.owner = owner
        made++
        // Whatever ends the listening lets the recogniser go first; anything from one already let go is ignored.
        r.listen(object : Heard {
            override fun ready() { if (held === r) heard.ready() }
            override fun partial(words: String) { if (held === r) heard.partial(words) }
            override fun done(words: String?) {
                if (held !== r) return
                letGo()
                heard.done(words)
            }
            override fun failed(error: Int) {
                if (held !== r) return
                letGo()
                if (error == SpeechRecognizer.ERROR_TOO_MANY_REQUESTS || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    busyUntil = SystemClock.elapsedRealtime() + BUSY_MS
                    busyError = error
                }
                heard.failed(error)
            }
        })
        return true
    }

    /** Let the recogniser go: [owner]'s only, or whichever is held. */
    fun letGo(owner: Any? = null) {
        if (owner != null && owner !== this.owner) return
        val r = held ?: return
        held = null
        this.owner = null
        runCatching { r.destroy() }
        destroyed++
    }
}

/**
 * Speaking a line instead of typing it (decision 16). Only the phone's own
 * on-device recognition is used, and a phone that can't recognise speech on
 * the device is told so plainly. There is no fallback to the cloud. The
 * recogniser is the app's one ([Speech]).
 */
/**
 * The voice behind a Speak button: whether it is listening, what it has
 * heard so far, and a note when it can't. 0.6.3: shared by the Speak link
 * and Ask's compact button beside the box.
 */
class Voice internal constructor() {
    var listening by mutableStateOf(false)
        internal set
    var heard by mutableStateOf("")
        internal set
    var note by mutableStateOf<String?>(null)
        internal set
    internal var tap: () -> Unit = {}
    /** Start listening, or stop and keep what was heard. */
    fun tap() = tap.invoke()
}

/**
 * Speaking a line instead of typing it (decision 16). Only the phone's own
 * on-device recognition is used, and a phone that can't recognise speech on
 * the device is told so plainly. There is no fallback to the cloud. The
 * recogniser is the app's one ([Speech]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberVoice(onWords: (String) -> Unit): Voice {
    val context = LocalContext.current
    val speech = (context.applicationContext as PocketApp).speech
    val me = remember { Any() }
    val voice = remember { Voice() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val keyboardUp = WindowInsets.isImeVisible
    val imeUp by rememberUpdatedState(keyboardUp)
    val scope = rememberCoroutineScope()
    var waiting by remember { mutableStateOf<Job?>(null) }
    val deliver by rememberUpdatedState(onWords)

    fun ended() {
        waiting?.cancel()
        waiting = null
        voice.listening = false
        voice.heard = ""
    }

    fun start(retry: Boolean = false) {
        if (!retry) voice.note = null
        val started = speech.start(context, me, object : Heard {
            override fun ready() { voice.listening = true; voice.note = null }
            override fun partial(words: String) { voice.heard = words }
            override fun done(words: String?) {
                words?.takeIf { it.isNotBlank() }?.let { deliver(it) }
                ended()
            }
            override fun failed(error: Int) {
                // 0.7: busy once, one try more, where he can see it; busy again, he is told.
                if (!retry && (error == SpeechRecognizer.ERROR_TOO_MANY_REQUESTS || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
                    voice.note = WAITING
                    voice.listening = true
                    waiting = scope.launch {
                        delay(Speech.RETRY_MS)
                        waiting = null
                        start(retry = true)
                    }
                    return
                }
                voice.note = speechTrouble(error)
                ended()
            }
        }, retry)
        if (started) voice.listening = true else { voice.note = busy(speech.busyError); voice.listening = false }
    }

    // With the keyboard up the phone's recogniser may be the keyboard's (Gboard's) to hold, so the keyboard goes first:
    // 0.7, listening starts only once it has gone, and a second more.
    fun begin() {
        if (!keyboardUp) return start()
        keyboard?.hide()
        focus.clearFocus()
        voice.listening = true
        waiting = scope.launch {
            val since = android.os.SystemClock.elapsedRealtime()
            while (imeUp && android.os.SystemClock.elapsedRealtime() - since < Speech.KEYBOARD_WAIT_MS) delay(50)
            delay(Speech.KEYBOARD_MS)
            waiting = null
            start()
        }
    }

    // Stopped by a tap: the recogniser is let go at once, and what it had heard so far is the line.
    fun stop() {
        val heard = voice.heard
        speech.letGo(me)
        ended()
        if (heard.isNotBlank()) deliver(heard)
    }

    val askMicrophone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else voice.note = "Speaking needs the microphone. Allow it for Rockpool in the phone's settings, then try again."
    }

    // Leaving the screen, or the app going to the background, lets the recogniser go.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val watch = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                speech.letGo(me)
                ended()
            }
        }
        lifecycle.addObserver(watch)
        onDispose {
            lifecycle.removeObserver(watch)
            speech.letGo(me)
        }
    }

    voice.tap = {
        when {
            voice.listening -> stop()
            speech.busy -> voice.note = busy(speech.busyError)
            !speech.available(context) -> voice.note = NO_SPEECH
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ->
                askMicrophone.launch(Manifest.permission.RECORD_AUDIO)
            else -> begin()
        }
    }
    return voice
}

/** Speak the line: a link, and beneath it what has been heard so far, or why it can't listen. */
@Composable
fun Speak(onWords: (String) -> Unit) {
    val ink = LocalInk.current
    val voice = rememberVoice(onWords)
    Column {
        TextLink(if (voice.listening) "Listening… tap to stop" else "Speak the line", { voice.tap() }, tag = "speak")
        if (voice.heard.isNotEmpty()) Text(voice.heard, style = Type.aside, color = ink.soft, modifier = Modifier.testTag("speech-heard"))
        voice.note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("speech-note")) }
    }
}

const val NO_SPEECH = "This phone can't recognise speech on the device, so speaking is off. Rockpool never sends your voice to a server."

/** 0.7: shown while the one try more waits, after the phone first says its speech is busy. */
const val WAITING = "Waiting for the keyboard's voice typing to let go\u2026"

/** The phone said its speech is busy; its error's number shows which way (10: too many requests; 8: its recogniser busy). */
fun busy(error: Int): String = "Speech is busy" + (if (error != 0) " (the phone's error $error)" else "") + ". Try again in a moment."

private fun onDeviceSpeech(context: Context): Boolean = try {
    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
} catch (_: Exception) {
    false
}

private fun speechTrouble(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS, SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> busy(error)
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        "The phone has no on-device speech model for ${Locale.getDefault().displayLanguage} yet. " +
            "It can be added in the phone's settings, under offline speech recognition."
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nothing was heard. Tap to speak again."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Speaking needs the microphone. Allow it for Rockpool in the phone's settings."
    SpeechRecognizer.ERROR_AUDIO -> "The microphone couldn't be used just now."
    else -> "Speaking stopped (the recogniser's error $error)."
}
