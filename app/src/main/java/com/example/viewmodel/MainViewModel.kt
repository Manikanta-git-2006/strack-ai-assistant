package com.example.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Content
import com.example.data.model.Part
import com.example.data.repository.AiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface UiLabTheme {
    data object CosmicMidnight : UiLabTheme
    data object EmeraldForest : UiLabTheme
    data object CyberpunkNeon : UiLabTheme
    data object SunsetGlow : UiLabTheme
}

class MainViewModel : ViewModel() {

    private val aiRepository = AiRepository()

    // --- Chat State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am Strack AI, your high-performance Kotlin and Jetpack Compose assistant. Let's build something exceptional! How can I assist you with your UI Lab or asynchronous API calls today?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    private val _inputPrompt = MutableStateFlow("")
    val inputPrompt: StateFlow<String> = _inputPrompt.asStateFlow()

    // --- UI Lab State ---
    private val _asyncTaskRunning = MutableStateFlow(false)
    val asyncTaskRunning: StateFlow<Boolean> = _asyncTaskRunning.asStateFlow()

    private val _asyncTaskProgress = MutableStateFlow(0f)
    val asyncTaskProgress: StateFlow<Float> = _asyncTaskProgress.asStateFlow()

    private val _asyncTaskResult = MutableStateFlow("Idle. Start a simulated lab task to view async states.")
    val asyncTaskResult: StateFlow<String> = _asyncTaskResult.asStateFlow()

    private val _activeLabTheme = MutableStateFlow<UiLabTheme>(UiLabTheme.CosmicMidnight)
    val activeLabTheme: StateFlow<UiLabTheme> = _activeLabTheme.asStateFlow()

    private val _cardCornerRounding = MutableStateFlow(16f) // DP
    val cardCornerRounding: StateFlow<Float> = _cardCornerRounding.asStateFlow()

    // --- API Telemetry State ---
    private val _totalQueries = MutableStateFlow(0)
    val totalQueries: StateFlow<Int> = _totalQueries.asStateFlow()

    private val _averageLatency = MutableStateFlow(0L)
    val averageLatency: StateFlow<Long> = _averageLatency.asStateFlow()

    private var simulatedJob: Job? = null
    private var chatJob: Job? = null

    // Update message input text
    fun updateInputPrompt(text: String) {
        _inputPrompt.value = text
    }

    // Send chat message to Strack AI
    fun sendMessage() {
        val prompt = _inputPrompt.value.trim()
        if (prompt.isEmpty() || _chatLoading.value) return

        // Clear input text
        _inputPrompt.value = ""

        // Add user message to list
        val userMessage = ChatMessage(text = prompt, isUser = true)
        _chatMessages.update { it + userMessage }

        _chatLoading.value = true

        chatJob = viewModelScope.launch {
            val startTime = SystemClock.elapsedRealtime()
            val historyContents = _chatMessages.value.takeLast(6).map { msg ->
                Content(parts = listOf(Part(text = msg.text)))
            }

            // Call API
            val response = aiRepository.getAiResponse(prompt, historyContents)
            val endTime = SystemClock.elapsedRealtime()
            val latency = endTime - startTime

            // Add response message
            val aiMessage = ChatMessage(text = response, isUser = false)
            _chatMessages.update { it + aiMessage }

            // Update telemetry metrics
            _totalQueries.update { it + 1 }
            _averageLatency.update { currentAvg ->
                if (currentAvg == 0L) latency else (currentAvg + latency) / 2
            }

            _chatLoading.value = false
        }
    }

    // Clear chat history
    fun clearChat() {
        chatJob?.cancel()
        _chatLoading.value = false
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Chat history cleared. I'm ready for new inquiries! How can I help you compile your thoughts?",
                isUser = false
            )
        )
    }

    // --- UI Labs Simulation Methods ---

    // Run a high-performance simulated network/data calculation coroutine
    fun startSimulatedAsyncTask(latencyMs: Long, isFailing: Boolean = false) {
        // Cancel prior running simulations safely
        simulatedJob?.cancel()
        _asyncTaskRunning.value = true
        _asyncTaskProgress.value = 0f
        _asyncTaskResult.value = "Connecting to mocked repository... (Kotlin Coroutines at work)"

        simulatedJob = viewModelScope.launch {
            try {
                val steps = 10
                val delayPerStep = latencyMs / steps

                for (i in 1..steps) {
                    delay(delayPerStep)
                    _asyncTaskProgress.value = i / steps.toFloat()
                    _asyncTaskResult.value = "Processing data chunk $i of $steps (${(i / steps.toFloat() * 100).toInt()}%)"
                }

                if (isFailing) {
                    throw RuntimeException("Simulation Error: Simulated remote server responded with HTTP 503 Service Unavailable.")
                }

                _asyncTaskResult.value = "Success! Loaded 25 UI widget schemas flawlessly in ${latencyMs}ms. [Clean Lifecycle Sync]"
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _asyncTaskResult.value = "Task Aborted! Coroutine was safely cancelled on user thread command."
                } else {
                    _asyncTaskResult.value = "Failure: ${e.message}"
                }
            } finally {
                _asyncTaskRunning.value = false
            }
        }
    }

    // Cancel current asynchronous running simulation
    fun cancelSimulatedAsyncTask() {
        simulatedJob?.cancel()
    }

    // Set interactive lab theme
    fun selectLabTheme(theme: UiLabTheme) {
        _activeLabTheme.value = theme
    }

    // Set interactive card rounding
    fun updateCardCornerRounding(value: Float) {
        _cardCornerRounding.value = value
    }

    override fun onCleared() {
        super.onCleared()
        simulatedJob?.cancel()
        chatJob?.cancel()
    }
}
