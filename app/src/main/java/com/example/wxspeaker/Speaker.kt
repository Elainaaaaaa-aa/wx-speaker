package com.example.wxspeaker

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 系统语音合成（TTS）的封装 */
object Speaker {

    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
                ready = true
            }
        }
    }

    fun speak(text: String?) {
        if (text.isNullOrEmpty() || !ready) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "wx")
        } catch (e: Exception) {
            // 忽略
        }
    }

    fun stop() {
        try { tts?.stop() } catch (e: Exception) {}
    }
}
