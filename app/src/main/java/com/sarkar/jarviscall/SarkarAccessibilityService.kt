package com.sarkar.jarviscall

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SarkarAccessibilityService : AccessibilityService() {

    companion object {
        var isAccessibilityAutoAnswerEnabled = true
        private val ANSWER_BUTTON_KEYWORDS = listOf(
            "answer", "accept", "swipe to answer", "drag to answer",
            "incoming call", "pickup", "receive call", "respond"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAccessibilityAutoAnswerEnabled || event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val rootNode = rootInActiveWindow ?: return
                scanAndAutoAnswerNode(rootNode)
            }
        }
    }

    private fun scanAndAutoAnswerNode(node: AccessibilityNodeInfo) {
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        for (keyword in ANSWER_BUTTON_KEYWORDS) {
            if (text.contains(keyword) || contentDesc.contains(keyword)) {
                if (node.isClickable) {
                    Log.d("SarkarAccessibility", "Auto-clicking answer button with text/desc: $text / $contentDesc")
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                } else {
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            Log.d("SarkarAccessibility", "Auto-clicking parent node of answer button")
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return
                        }
                        parent = parent.parent
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            scanAndAutoAnswerNode(child)
        }
    }

    override fun onInterrupt() {
        Log.d("SarkarAccessibility", "Accessibility service interrupted")
    }
}
