package com.javonpeart.ideacompanion.conversation

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechOutput(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    var available = false
        private set
    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val voice = tts.voices?.firstOrNull { !it.isNetworkConnectionRequired && it.locale.language == Locale.ENGLISH.language }
        if (voice != null) { tts.voice = voice; available = true }
    }
    fun speak(value: String) { if (available) tts.speak(value, TextToSpeech.QUEUE_FLUSH, null, "idea-answer") }
    fun close() { tts.stop(); tts.shutdown() }
}
