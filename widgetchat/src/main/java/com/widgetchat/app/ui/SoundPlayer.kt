package com.widgetchat.app.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.widgetchat.app.data.SoundConfiguration

/**
 * Lightweight sound feedback (parity with iOS). Uses the system ToneGenerator so it needs no
 * bundled assets or Context. Honors the per-event flags in SoundConfiguration.
 */
object SoundPlayer {
    fun play(kind: String, cfg: SoundConfiguration?) {
        val c = cfg ?: SoundConfiguration()
        if (!c.enabled) return
        val enabled = when (kind) {
            "sent" -> c.messageSent
            "received" -> c.messageReceived
            "error" -> c.error
            else -> false
        }
        if (!enabled) return
        try {
            val volume = (c.volume * 100).toInt().coerceIn(1, 100)
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
            val tone = when (kind) {
                "received" -> ToneGenerator.TONE_PROP_ACK
                "error" -> ToneGenerator.TONE_PROP_NACK
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            tg.startTone(tone, 150)
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { tg.release() } }, 300)
        } catch (_: Exception) {
            // ToneGenerator can throw on some devices/emulators — never break the chat over a sound.
        }
    }
}
