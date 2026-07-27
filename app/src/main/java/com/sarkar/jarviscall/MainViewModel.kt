package com.sarkar.jarviscall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sarkar.jarviscall.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SimulationTurn(
    val sender: String, // "Caller" or "Sarkar AI"
    val text: String,
    val isSpam: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveCallUiState(
    val isSimulatingCall: Boolean = false,
    val callerNumber: String = "+1 (800) 555-0199",
    val callerName: String = "Unknown Caller",
    val callStateString: String = "IDLE", // IDLE, RINGING, ACTIVE, DISCONNECTED
    val transcriptTurns: List<SimulationTurn> = emptyList(),
    val currentAiAction: String = "Ready",
    val turnCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = CallRepository(db)
    private val responder = RuleBasedResponder()

    // Preferences & Settings State
    val isReceptionistActive = MutableStateFlow(true)
    val isAutoAnswerEnabled = MutableStateFlow(true)
    val selectedPersona = MutableStateFlow(RuleBasedResponder.DEFAULT_PERSONAS.first())
    val voicePitch = MutableStateFlow(1.0f)
    val voiceSpeed = MutableStateFlow(1.0f)

    // Interactive Call Simulator State
    private val _simState = MutableStateFlow(LiveCallUiState())
    val simState: StateFlow<LiveCallUiState> = _simState.asStateFlow()

    // Room DB State Flows
    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCallLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rules: StateFlow<List<RuleEntity>> = repository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val contactFilters: StateFlow<List<ContactFilterEntity>> = repository.allContactFilters.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.populateDefaultRulesIfNeeded()
        }
    }

    // --- RULE MANAGEMENT ---
    fun addRule(title: String, keywordsCsv: String, responseText: String, actionType: CallActionType) {
        viewModelScope.launch {
            repository.addRule(
                RuleEntity(
                    title = title,
                    keywordsCsv = keywordsCsv,
                    responseText = responseText,
                    actionType = actionType,
                    isEnabled = true,
                    priority = 5
                )
            )
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            repository.deleteRule(id)
        }
    }

    // --- CONTACT FILTER MANAGEMENT ---
    fun addContactFilter(number: String, name: String, type: ContactFilterType, note: String = "") {
        viewModelScope.launch {
            repository.addContactFilter(
                ContactFilterEntity(
                    phoneNumber = number,
                    contactName = name,
                    filterType = type,
                    note = note
                )
            )
        }
    }

    fun deleteContactFilter(id: Long) {
        viewModelScope.launch {
            repository.deleteContactFilter(id)
        }
    }

    fun clearAllCallHistory() {
        viewModelScope.launch {
            repository.clearAllCallLogs()
        }
    }

    fun deleteCallLog(id: Long) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
        }
    }

    // --- LIVE CALL SCREENING SIMULATION ENGINE ---
    fun startSimulatedCall(callerNumber: String = "+1 (800) 555-0199", callerName: String = "Unknown Caller") {
        val greeting = selectedPersona.value.greeting
        _simState.value = LiveCallUiState(
            isSimulatingCall = true,
            callerNumber = callerNumber,
            callerName = callerName,
            callStateString = "RINGING",
            transcriptTurns = listOf(SimulationTurn("Sarkar AI", greeting)),
            currentAiAction = "Screening Initiated - Speaking Greeting",
            turnCount = 0
        )

        viewModelScope.launch {
            delay(1500)
            _simState.value = _simState.value.copy(callStateString = "ACTIVE")
        }
    }

    fun submitSimulatedCallerSpeech(spokenText: String) {
        val current = _simState.value
        if (!current.isSimulatingCall) return

        val newTurnCount = current.turnCount + 1
        val updatedTurns = current.transcriptTurns + SimulationTurn("Caller", spokenText)

        _simState.value = current.copy(
            transcriptTurns = updatedTurns,
            currentAiAction = "Processing caller speech...",
            turnCount = newTurnCount
        )

        viewModelScope.launch {
            delay(800) // Simulate processing lag
            val activeRules = rules.value
            val result = responder.evaluateCallInput(
                callerInput = spokenText,
                customRules = activeRules,
                persona = selectedPersona.value,
                turnCount = newTurnCount
            )

            val turnResult = SimulationTurn("Sarkar AI", result.replyText, isSpam = result.isSpam)
            val finalTurns = updatedTurns + turnResult

            _simState.value = _simState.value.copy(
                transcriptTurns = finalTurns,
                currentAiAction = "Action: ${result.actionType.name}"
            )

            if (result.shouldEndCall) {
                delay(2000)
                endSimulatedCall(result.actionType.name, result.isSpam)
            }
        }
    }

    fun endSimulatedCall(actionSummary: String = "Call Ended by User", isSpam: Boolean = false) {
        val current = _simState.value
        if (!current.isSimulatingCall && current.transcriptTurns.isEmpty()) return

        val fullTranscript = current.transcriptTurns.joinToString("\n") { "${it.sender}: ${it.text}" }
        val jsonTranscript = buildSimpleJson(current.transcriptTurns)

        viewModelScope.launch {
            if (current.transcriptTurns.isNotEmpty()) {
                val log = CallLogEntity(
                    phoneNumber = current.callerNumber,
                    callerName = current.callerName,
                    durationSeconds = current.turnCount * 12,
                    actionTaken = actionSummary,
                    transcriptJson = jsonTranscript,
                    categoryTag = if (isSpam) "Spam" else "Simulated Screen",
                    isSpam = isSpam,
                    summaryNote = "Simulated screening completed with ${current.transcriptTurns.size} turns."
                )
                repository.addCallLog(log)
            }

            _simState.value = LiveCallUiState(
                isSimulatingCall = false,
                callStateString = "DISCONNECTED",
                currentAiAction = "Session Saved to Log"
            )
        }
    }

    private fun buildSimpleJson(turns: List<SimulationTurn>): String {
        val items = turns.map {
            """{"sender":"${it.sender}","text":"${it.text.replace("\"", "\\\"")}"}"""
        }
        return "[${items.joinToString(",")}]"
    }
}
