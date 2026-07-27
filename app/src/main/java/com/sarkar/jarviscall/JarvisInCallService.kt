package com.sarkar.jarviscall

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.sarkar.jarviscall.data.AppDatabase
import com.sarkar.jarviscall.data.CallActionType
import com.sarkar.jarviscall.data.CallLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveCallState(
    val isCallActive: Boolean = false,
    val callerNumber: String = "",
    val callStateString: String = "IDLE",
    val currentTranscript: List<Pair<String, String>> = emptyList(), // Sender ("Caller" / "Sarkar AI") to Message
    val isAutoScreening: Boolean = true,
    val lastAction: String = "Idle"
)

class JarvisInCallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentCall: Call? = null
    private var speechHandler: SpeechHandler? = null
    private val responder = RuleBasedResponder()
    private val transcriptList = mutableListOf<Pair<String, String>>()
    private var dialogueTurnCount = 0

    companion object {
        private val _callStateFlow = MutableStateFlow(ActiveCallState())
        val callStateFlow: StateFlow<ActiveCallState> = _callStateFlow.asStateFlow()

        var isServiceEnabled = true
        var isAutoAnswerEnabled = true
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            val number = call.details?.handle?.schemeSpecificPart ?: "Unknown Caller"
            val stateName = when (state) {
                Call.STATE_RINGING -> "RINGING"
                Call.STATE_ACTIVE -> "ACTIVE"
                Call.STATE_DISCONNECTING -> "DISCONNECTING"
                Call.STATE_DISCONNECTED -> "DISCONNECTED"
                Call.STATE_HOLDING -> "HOLDING"
                else -> "UNKNOWN"
            }

            Log.d("JarvisInCallService", "Call state changed: $stateName for $number")

            _callStateFlow.value = _callStateFlow.value.copy(
                isCallActive = state == Call.STATE_ACTIVE || state == Call.STATE_RINGING,
                callerNumber = number,
                callStateString = stateName
            )

            if (state == Call.STATE_RINGING && isAutoAnswerEnabled && isServiceEnabled) {
                Log.d("JarvisInCallService", "Auto-answering incoming call from $number")
                call.answer(0)
            } else if (state == Call.STATE_ACTIVE) {
                startAiReceptionistScreening(call, number)
            } else if (state == Call.STATE_DISCONNECTED) {
                finishCallSession(number)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callCallback)

        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown Caller"
        val stateName = when (call.state) {
            Call.STATE_RINGING -> "RINGING"
            Call.STATE_ACTIVE -> "ACTIVE"
            else -> "INCOMING"
        }

        _callStateFlow.value = ActiveCallState(
            isCallActive = true,
            callerNumber = number,
            callStateString = stateName,
            currentTranscript = emptyList(),
            lastAction = "Call Intercepted"
        )

        if (call.state == Call.STATE_RINGING && isAutoAnswerEnabled && isServiceEnabled) {
            call.answer(0)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        currentCall = null
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown Caller"
        finishCallSession(number)
    }

    private fun startAiReceptionistScreening(call: Call, callerNumber: String) {
        transcriptList.clear()
        dialogueTurnCount = 0

        val persona = RuleBasedResponder.DEFAULT_PERSONAS.first()
        val initialGreeting = persona.greeting

        transcriptList.add("Sarkar AI" to initialGreeting)
        _callStateFlow.value = _callStateFlow.value.copy(
            currentTranscript = ArrayList(transcriptList),
            lastAction = "AI Speaking Greeting"
        )

        speechHandler = SpeechHandler(
            context = applicationContext,
            onSpeechRecognized = { spokenText ->
                handleSpokenCallerText(spokenText)
            },
            onTtsFinished = {
                // Resume listening for caller's answer
                speechHandler?.startListening()
            }
        )

        speechHandler?.setVoiceParams(persona.voicePitch, persona.voiceSpeed)
        speechHandler?.speak(initialGreeting)
    }

    private fun handleSpokenCallerText(spokenText: String) {
        dialogueTurnCount++
        transcriptList.add("Caller" to spokenText)
        _callStateFlow.value = _callStateFlow.value.copy(
            currentTranscript = ArrayList(transcriptList),
            lastAction = "Caller Spoke: $spokenText"
        )

        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val customRules = db.ruleDao().getActiveRulesSync()
            val persona = RuleBasedResponder.DEFAULT_PERSONAS.first()

            val eval = responder.evaluateCallInput(
                callerInput = spokenText,
                customRules = customRules,
                persona = persona,
                turnCount = dialogueTurnCount
            )

            transcriptList.add("Sarkar AI" to eval.replyText)
            _callStateFlow.value = _callStateFlow.value.copy(
                currentTranscript = ArrayList(transcriptList),
                lastAction = "AI Response: ${eval.actionType}"
            )

            speechHandler?.speak(eval.replyText)

            if (eval.shouldEndCall) {
                if (eval.actionType == CallActionType.DECLINE_AND_BLOCK) {
                    currentCall?.reject(false, null)
                } else {
                    currentCall?.disconnect()
                }
            }
        }
    }

    private fun finishCallSession(number: String) {
        speechHandler?.shutdown()
        speechHandler = null

        val fullTranscript = transcriptList.joinToString("\n") { "${it.first}: ${it.second}" }
        val jsonTranscript = buildSimpleJsonTranscript(transcriptList)

        serviceScope.launch {
            if (transcriptList.isNotEmpty()) {
                val db = AppDatabase.getDatabase(applicationContext)
                val isSpam = fullTranscript.contains("telemarketing", ignoreCase = true) || fullTranscript.contains("Spam", ignoreCase = true)
                val log = CallLogEntity(
                    phoneNumber = number,
                    callerName = "Caller",
                    durationSeconds = dialogueTurnCount * 10,
                    actionTaken = if (isSpam) "Auto-Declined Spam" else "Screened & Message Logged",
                    transcriptJson = jsonTranscript,
                    categoryTag = if (isSpam) "Spam" else "Screened Call",
                    isSpam = isSpam,
                    summaryNote = "Completed automated screening with ${transcriptList.size} dialogue exchanges."
                )
                db.callLogDao().insertCallLog(log)
            }
        }

        _callStateFlow.value = ActiveCallState(
            isCallActive = false,
            callerNumber = "",
            callStateString = "DISCONNECTED",
            currentTranscript = emptyList(),
            lastAction = "Call Session Saved"
        )
    }

    private fun buildSimpleJsonTranscript(list: List<Pair<String, String>>): String {
        val items = list.map { (sender, text) ->
            """{"sender":"${sender.replace("\"", "\\\"")}","text":"${text.replace("\"", "\\\"")}"}"""
        }
        return "[${items.joinToString(",")}]"
    }
}
