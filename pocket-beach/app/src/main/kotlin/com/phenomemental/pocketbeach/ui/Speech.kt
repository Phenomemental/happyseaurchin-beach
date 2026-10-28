package com.phenomemental.pocketbeach.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import java.util.Locale

/**
 * Speaking a line instead of typing it (decision 16). Only the phone's own
 * on-device recognition is used: the recognizer is made with
 * [SpeechRecognizer.createOnDeviceSpeechRecognizer], which never sends the
 * voice to a server, and a phone that can't recognise speech on the device is
 * told so plainly. There is no fallback to the cloud.
 */
@Composable
fun Speak(onWords: (String) -> Unit) {
    val ink = LocalInk.current
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var heard by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    val recognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun stop() {
        recognizer.value?.destroy()
        recognizer.value = null
        listening = false
        heard = ""
    }

    fun start() {
        note = null
        val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer.value = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                heard = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let(onWords)
                stop()
            }
            override fun onError(error: Int) {
                note = speechTrouble(error)
                stop()
            }
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
    }

    val askMicrophone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else note = "Speaking needs the microphone. Allow it for Pocket beach in the phone's settings, then try again."
    }

    DisposableEffect(Unit) { onDispose { recognizer.value?.destroy() } }

    Column {
        TextLink(
            if (listening) "Listening… tap to stop" else "Speak the line",
            {
                when {
                    recognizer.value != null -> recognizer.value?.stopListening()
                    !onDeviceSpeech(context) -> note = NO_SPEECH
                    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ->
                        askMicrophone.launch(Manifest.permission.RECORD_AUDIO)
                    else -> start()
                }
            },
            tag = "speak",
        )
        if (heard.isNotEmpty()) Text(heard, style = Type.aside, color = ink.soft)
        note?.let { Text(it, style = Type.small, color = ink.crimson, modifier = Modifier.testTag("speech-note")) }
    }
}

const val NO_SPEECH = "This phone can't recognise speech on the device, so speaking is off. Pocket beach never sends your voice to a server."

private fun onDeviceSpeech(context: Context): Boolean = try {
    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
} catch (_: Exception) {
    false
}

private fun speechTrouble(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        "The phone has no on-device speech model for ${Locale.getDefault().displayLanguage} yet. " +
            "It can be added in the phone's settings, under offline speech recognition."
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nothing was heard. Tap to speak again."
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Speaking needs the microphone. Allow it for Pocket beach in the phone's settings."
    SpeechRecognizer.ERROR_AUDIO -> "The microphone couldn't be used just now."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The phone's recogniser is busy. Try again in a moment."
    else -> "Speaking stopped (the recogniser's error $error)."
}
