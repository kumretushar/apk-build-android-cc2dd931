package com.tusharx.friday

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Wraps SpeechRecognizer (ears) and TextToSpeech (voice). Call everything from the main thread. */
class VoiceEngine(private val app: Context, private val prefs: Prefs) : TextToSpeech.OnInitListener {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(app, this)
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    var onPartial: (String) -> Unit = {}
    var onFinal: (String) -> Unit = {}
    var onListenError: (Int) -> Unit = {}
    var onSpeakDone: () -> Unit = {}

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        applyVoice()
    }

    private fun ttsLocale(): Locale = if (prefs.language == "Hindi") Locale("hi", "IN") else Locale("en", "IN")
    private fun sttLang(): String = if (prefs.language == "English") "en-IN" else "hi-IN"

    fun applyVoice() {
        val t = tts ?: return
        if (!ttsReady) return
        val r = t.setLanguage(ttsLocale())
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) t.setLanguage(Locale.ENGLISH)
        t.setSpeechRate(prefs.rate)
        t.setPitch(prefs.pitch)
    }

    fun speak(text: String) {
        val t = tts
        if (t == null || !ttsReady) { onSpeakDone(); return }
        applyVoice()
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { main.post { onSpeakDone() } }
            override fun onError(utteranceId: String?) { main.post { onSpeakDone() } }
        })
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u" + System.nanoTime())
    }

    fun stopSpeaking() { tts?.stop() }

    fun listen() {
        main.post {
            if (!SpeechRecognizer.isRecognitionAvailable(app)) { onListenError(-1); return@post }
            recognizer?.destroy()
            val r = SpeechRecognizer.createSpeechRecognizer(app)
            recognizer = r
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onError(error: Int) { onListenError(error) }
                override fun onResults(results: Bundle?) {
                    val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (t.isNullOrBlank()) onListenError(SpeechRecognizer.ERROR_NO_MATCH) else onFinal(t)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!t.isNullOrBlank()) onPartial(t)
                }
            })
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLang())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            r.startListening(i)
        }
    }

    fun stopListening() { main.post { recognizer?.cancel() } }

    fun shutdown() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
