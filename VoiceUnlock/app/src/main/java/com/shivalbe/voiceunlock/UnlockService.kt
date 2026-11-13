package com.shivalbe.voiceunlock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class UnlockService : AccessibilityService() {

    companion object {
        var instance: UnlockService? = null
        const val TAG = "VoiceUnlock"
    }

    private val h = Handler(Looper.getMainLooper())

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }

    fun unlock(pin: String) {
        swipeUp()
        h.postDelayed({ typePin(pin, 0, 0) }, 800)
    }

    private fun swipeUp() {
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val p = Path().apply {
            moveTo(x, dm.heightPixels * 0.85f)
            lineTo(x, dm.heightPixels * 0.25f)
        }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 250))
            .build()
        dispatchGesture(g, null, null)
    }

    private fun typePin(pin: String, index: Int, attempts: Int) {
        if (index >= pin.length) {
            h.postDelayed({ pressEnter() }, 200)
            return
        }
        val digit = pin[index].toString()
        val node = findKey(digit)
        if (node == null) {
            if (attempts < 5) {
                h.postDelayed({ typePin(pin, index, attempts + 1) }, 400)
            } else {
                Log.w(TAG, "PIN key $digit not found")
            }
            return
        }
        clickNode(node)
        h.postDelayed({ typePin(pin, index + 1, 0) }, 180)
    }

    private fun pressEnter() {
        val node = findById("key_enter") ?: return
        clickNode(node)
    }

    private fun roots(): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        windows.forEach { w -> w.root?.let { list.add(it) } }
        rootInActiveWindow?.let { list.add(it) }
        return list
    }

    private fun findById(name: String): AccessibilityNodeInfo? {
        for (r in roots()) {
            val found = r.findAccessibilityNodeInfosByViewId("com.android.systemui:id/$name")
            if (!found.isNullOrEmpty()) return found[0]
        }
        return null
    }

    private fun findKey(digit: String): AccessibilityNodeInfo? {
        findById("key$digit")?.let { return it }
        for (r in roots()) {
            val found = r.findAccessibilityNodeInfosByText(digit)
            val exact = found?.firstOrNull {
                it.text?.toString() == digit || it.contentDescription?.toString() == digit
            }
            if (exact != null) return exact
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        var n: AccessibilityNodeInfo? = node
        while (n != null) {
            if (n.isClickable) { n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return }
            n = n.parent
        }
    }
}
