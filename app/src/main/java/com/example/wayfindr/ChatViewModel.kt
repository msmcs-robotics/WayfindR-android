package com.example.wayfindr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

import com.example.wayfindr.UiState

class ChatViewModel(
    private val llmService: LlmService
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Continuous conversation settings
    var continuousConversationEnabled: Boolean = true
    var contextMessageCount: Int = 10

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val trimmedMessage = message.trim()

        // Add user message
        val userMessage = ChatMessage(
            content = trimmedMessage,
            isUser = true
        )

        val currentMessages = _uiState.value.messages

        _uiState.value = _uiState.value.copy(
            messages = currentMessages + userMessage,
            currentInput = "",
            isLoading = true,
            error = null
        )

        // Get context for continuous conversation
        val context = if (continuousConversationEnabled && currentMessages.isNotEmpty()) {
            // Take the last N messages as context (before adding current message)
            currentMessages.takeLast(contextMessageCount)
        } else {
            emptyList()
        }

        // Send to LLM with context
        viewModelScope.launch {
            try {
                val response = llmService.sendMessageWithContext(trimmedMessage, context)

                val assistantMessage = ChatMessage(
                    content = response,
                    isUser = false
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + assistantMessage,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "Sorry, I encountered an error: ${e.message ?: "Unknown error"}",
                    isUser = false
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + errorMessage,
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setListening(isListening: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isListening = isListening,
                // Clear partial speech when we stop listening
                partialSpeechText = if (!isListening) "" else currentState.partialSpeechText,
                isUserSpeaking = if (!isListening) false else currentState.isUserSpeaking
            )
        }
    }

    fun setUserSpeaking(isSpeaking: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(isUserSpeaking = isSpeaking)
        }
    }

    fun setPartialSpeechText(text: String) {
        _uiState.update { currentState ->
            currentState.copy(partialSpeechText = text)
        }
    }

    fun setError(errorMessage: String?) {
        _uiState.update { currentState ->
            currentState.copy(error = errorMessage)
        }
    }

    fun updateInput(input: String) {
        _uiState.update { currentState ->
            currentState.copy(currentInput = input)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearChatHistory() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    fun exportChatHistoryAsMarkdown(): String {
        val builder = StringBuilder()
        builder.append("# Chat History\n\n")
        for (msg in _uiState.value.messages) {
            val sender = if (msg.isUser) "You" else "Assistant"
            builder.append("**$sender**: ${msg.content.replace("\n", "  \n")}\n\n")
        }
        return builder.toString()
    }
}