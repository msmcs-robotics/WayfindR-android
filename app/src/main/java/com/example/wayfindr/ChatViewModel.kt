package com.example.wayfindr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val llmService: LlmService
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(input: String) {
        _uiState.value = _uiState.value.copy(currentInput = input)
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val trimmedMessage = message.trim()

        // Add user message
        val userMessage = ChatMessage(
            content = trimmedMessage,
            isUser = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            currentInput = "",
            isLoading = true,
            error = null
        )

        // Send to LLM
        viewModelScope.launch {
            try {
                val response = llmService.sendMessage(trimmedMessage)

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

    fun setListening(listening: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = listening)
    }

    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
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