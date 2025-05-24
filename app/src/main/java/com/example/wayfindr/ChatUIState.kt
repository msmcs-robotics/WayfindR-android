package com.example.wayfindr

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val error: String? = null
)