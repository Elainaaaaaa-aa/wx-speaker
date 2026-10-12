package com.example.wxspeaker

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 系统语音合成（TTS）的封装，带状态诊断 */
object Speaker {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile var ready = false
    @Volatile var initStatus = -999
    @Volatile var langResult = -999
    @Volatile var engineName = ""
    @Volatile var lastError = ""

    fun init(ctx: Context) {
        if (tts != null) return
        initStatus = -1
        try {
            tts = TextToSpeech(ctx.applicationContext) { status ->
                initStatus = status
                if (status == TextToSpeech.SUCCESS) {
                    val t = tts
                    if (t != null) {
                        langResult = t.setLanguage(Locale.CHINESE)
                        t.setSpeechRate(1.0f)
                        t.setPitch(1.0f)
                        engineName = t.defaultEngine ?: ""
                        ready = true
                    }
                }
            }
        } catch (e: Exception) {
            lastError = "init异常:$e"
        }
    }

    fun speak(text: String?) {
        if (text.isNullOrEmpty()) return
        if (!ready) {
            lastError = "TTS未就绪(initStatus=$initStatus)"
            return
        }
        try {
            val r = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "wx")
            if (r != TextToSpeech.SUCCESS) lastError = "speak返回$r"
            else lastError = "已调用speak"
        } catch (e: Exception) {
            lastError = "speak异常:$e"
        }
    }

    fun stop() {
        try { tts?.stop() } catch (e: Exception) {}
    }

    fun statusText(): String {
        return "TTS状态：init=$initStatus ready=$ready lang=$langResult engine=$engineName err=$lastError"
    }
}
