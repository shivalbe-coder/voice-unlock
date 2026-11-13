package com.shivalbe.voiceunlock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceService : Service() {

    private var rec: SpeechRecognizer? = null
    private val h = Handler(Looper.getMainLooper())
    private var running = false
    private var lastFire = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("vu", "Voice Unlock", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "vu")
            .setContentTitle("Voice Unlock is listening")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        running = true
        listen()
        return START_STICKY
    }

    private fun listen() {
        if (!running) return
        rec?.destroy()
        rec = SpeechRecognizer.createSpeechRecognizer(this)
        rec?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onError(error: Int) {
                val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500L else 400L
                h.postDelayed({ listen() }, delay)
            }
            override fun onPartialResults(partialResults: Bundle?) { check(partialResults) }
            override fun onResults(results: Bundle?) {
                check(results)
                h.postDelayed({ listen() }, 300)
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        rec?.startListening(i)
    }

    private fun check(b: Bundle?) {
        val prefs = getSharedPreferences("vu", MODE_PRIVATE)
        val phrase = prefs.getString("phrase", "")?.lowercase()?.trim().orEmpty()
        val pin = prefs.getString("pin", "").orEmpty()
        if (phrase.isEmpty() || pin.isEmpty()) return
        val heard = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        if (heard.none { it.lowercase().contains(phrase) }) return
        val now = System.currentTimeMillis()
        if (now - lastFire < 5000) return
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isInteractive) return
        lastFire = now
        UnlockService.instance?.unlock(pin)
    }

    override fun onDestroy() {
        running = false
        rec?.destroy()
        super.onDestroy()
    }
}
