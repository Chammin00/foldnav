package com.foldnav.app.voice

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Hands-free TextToSpeech co-pilot announcing theme shifts, routes, and speed alerts.
 */
class FoldVoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var lastSpokenMs = 0L

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(1.04f)
            ready = true
        }
    }

    fun speak(message: String, enabled: Boolean, force: Boolean = false) {
        if (!enabled || !ready || message.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastSpokenMs < COOLDOWN_MS) return
        lastSpokenMs = now
        try {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "foldnav_${now}")
        } catch (_: Throwable) {
        }
    }

    fun shutdown() {
        ready = false
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
    }

    companion object {
        private const val COOLDOWN_MS = 2500L
    }
}
