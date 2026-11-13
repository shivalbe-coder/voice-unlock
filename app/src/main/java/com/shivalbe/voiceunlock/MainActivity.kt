package com.shivalbe.voiceunlock

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("vu", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        fun label(t: String) = TextView(this).apply { text = t; textSize = 16f; setPadding(0, pad / 2, 0, 0) }
        fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
            text = t; setOnClickListener { onClick() }
        }

        root.addView(TextView(this).apply { text = "Voice Unlock"; textSize = 26f; gravity = Gravity.START })

        root.addView(label("Your PIN"))
        val pin = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setText(prefs.getString("pin", ""))
        }
        root.addView(pin)

        root.addView(label("Your unlock phrase (pick something unusual)"))
        val phrase = EditText(this).apply { setText(prefs.getString("phrase", "open sesame")) }
        root.addView(phrase)

        fun save() {
            prefs.edit().putString("pin", pin.text.toString().trim())
                .putString("phrase", phrase.text.toString().trim()).apply()
        }

        root.addView(btn("1. Save PIN and phrase") { save(); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() })
        root.addView(btn("2. Allow microphone and notifications") {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        root.addView(btn("3. Turn on Accessibility (find Voice Unlock)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("Test unlock in 5 seconds (lock your phone now)") {
            save()
            Handler(Looper.getMainLooper()).postDelayed({
                val s = UnlockService.instance
                if (s == null) Toast.makeText(this, "Accessibility is not on", Toast.LENGTH_LONG).show()
                else s.unlock(prefs.getString("pin", "").orEmpty())
            }, 5000)
        })
        root.addView(btn("Start listening") {
            save()
            startForegroundService(Intent(this, VoiceService::class.java))
        })
        root.addView(btn("Stop listening") { stopService(Intent(this, VoiceService::class.java)) })

        setContentView(root)
    }
}
