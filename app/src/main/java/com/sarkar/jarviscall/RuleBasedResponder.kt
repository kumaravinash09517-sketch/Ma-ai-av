package com.sarkar.jarviscall

import com.sarkar.jarviscall.data.CallActionType
import com.sarkar.jarviscall.data.RuleEntity

data class ResponseResult(
    val replyText: String,
    val actionType: CallActionType,
    val matchedKeyword: String? = null,
    val categoryTag: String = "Inquiry",
    val isSpam: Boolean = false,
    val shouldEndCall: Boolean = false
)

data class ReceptionistPersona(
    val id: String,
    val name: String,
    val description: String,
    val greeting: String,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f
)

class RuleBasedResponder {

    companion object {
        val DEFAULT_PERSONAS = listOf(
            ReceptionistPersona(
                id = "professional",
                name = "Sarkar Professional",
                description = "Polite and formal assistant. Screens calls efficiently.",
                greeting = "Hello! You have reached Sarkar's AI Assistant. How may I direct your call?",
                voicePitch = 1.0f,
                voiceSpeed = 1.0f
            ),
            ReceptionistPersona(
                id = "executive",
                name = "Executive Shield",
                description = "Strict filter against spam, telemarketers, and unverified calls.",
                greeting = "Hello. Sarkar's Executive Call Screening is active. State your name and exact reason for calling.",
                voicePitch = 0.9f,
                voiceSpeed = 1.05f
            ),
            ReceptionistPersona(
                id = "friendly",
                name = "Friendly Screener",
                description = "Warm and welcoming tone for personal and business inquiries.",
                greeting = "Hi there! I'm Sarkar's virtual receptionist. Sarkar is unavailable right now, but I can take a quick message for you!",
                voicePitch = 1.1f,
                voiceSpeed = 0.95f
            )
        )

        val DEFAULT_SPAM_KEYWORDS = listOf(
            "loan", "credit card", "insurance", "investment", "crypto",
            "trading", "lottery", "cashback", "free trial", "telemarketer",
            "survey", "bank offer", "unclaimed money", "policy renew"
        )

        val DEFAULT_DELIVERY_KEYWORDS = listOf(
            "delivery", "package", "parcel", "courier", "amazon",
            "swiggy", "zomato", "fedex", "dhl", "otp", "doorstep"
        )

        val DEFAULT_URGENT_KEYWORDS = listOf(
            "urgent", "emergency", "doctor", "hospital", "family",
            "boss", "office", "interview", "critical", "police"
        )
    }

    /**
     * Evaluates caller's spoken input against rules, active persona, and default keyword engines.
     */
    fun evaluateCallInput(
        callerInput: String,
        customRules: List<RuleEntity> = emptyList(),
        persona: ReceptionistPersona = DEFAULT_PERSONAS.first(),
        turnCount: Int = 1
    ): ResponseResult {
        val cleanedInput = callerInput.lowercase().trim()

        if (cleanedInput.isEmpty()) {
            return ResponseResult(
                replyText = "I couldn't hear you clearly. Could you please repeat your name and purpose?",
                actionType = CallActionType.ASK_CALLER_NAME,
                categoryTag = "Inquiry"
            )
        }

        // 1. Check custom user rules first (Highest priority)
        for (rule in customRules) {
            if (!rule.isEnabled) continue
            val keywords = rule.keywordsCsv.lowercase().split(",").map { it.trim() }
            for (kw in keywords) {
                if (kw.isNotEmpty() && cleanedInput.contains(kw)) {
                    val endCall = rule.actionType == CallActionType.DECLINE_AND_BLOCK
                    return ResponseResult(
                        replyText = rule.responseText,
                        actionType = rule.actionType,
                        matchedKeyword = kw,
                        categoryTag = if (endCall) "Spam" else "Custom Rule",
                        isSpam = endCall,
                        shouldEndCall = endCall
                    )
                }
            }
        }

        // 2. Check built-in Spam keywords
        for (spamKw in DEFAULT_SPAM_KEYWORDS) {
            if (cleanedInput.contains(spamKw)) {
                return ResponseResult(
                    replyText = "Thank you, but Sarkar does not accept telemarketing or sales calls. Ending call now.",
                    actionType = CallActionType.DECLINE_AND_BLOCK,
                    matchedKeyword = spamKw,
                    categoryTag = "Spam",
                    isSpam = true,
                    shouldEndCall = true
                )
            }
        }

        // 3. Check Delivery keywords
        for (deliveryKw in DEFAULT_DELIVERY_KEYWORDS) {
            if (cleanedInput.contains(deliveryKw)) {
                return ResponseResult(
                    replyText = "Got it! Please leave the package at the doorstep or security desk. I have logged your delivery note for Sarkar. Thank you!",
                    actionType = CallActionType.TAKE_MESSAGE,
                    matchedKeyword = deliveryKw,
                    categoryTag = "Delivery",
                    isSpam = false,
                    shouldEndCall = true
                )
            }
        }

        // 4. Check Urgent / Emergency keywords
        for (urgentKw in DEFAULT_URGENT_KEYWORDS) {
            if (cleanedInput.contains(urgentKw)) {
                return ResponseResult(
                    replyText = "Understood. Marking this as high priority and alerting Sarkar immediately. Please hold.",
                    actionType = CallActionType.ACCEPT_AND_NOTIFY,
                    matchedKeyword = urgentKw,
                    categoryTag = "Urgent",
                    isSpam = false,
                    shouldEndCall = false
                )
            }
        }

        // 5. Turn-based multi-turn dialog fallback
        return when (turnCount) {
            1 -> ResponseResult(
                replyText = "Thank you. Could you briefly state what this call is regarding so I can notify Sarkar?",
                actionType = CallActionType.ASK_REASON_FOR_CALLING,
                categoryTag = "Inquiry"
            )
            2 -> ResponseResult(
                replyText = "Thank you for the details. I have recorded your audio message and transcript for Sarkar. We will get back to you shortly. Goodbye!",
                actionType = CallActionType.TAKE_MESSAGE,
                categoryTag = "Inquiry",
                shouldEndCall = true
            )
            else -> ResponseResult(
                replyText = "Your message has been saved. Sarkar will review it soon. Thank you and have a great day!",
                actionType = CallActionType.TAKE_MESSAGE,
                categoryTag = "Inquiry",
                shouldEndCall = true
            )
        }
    }
}
