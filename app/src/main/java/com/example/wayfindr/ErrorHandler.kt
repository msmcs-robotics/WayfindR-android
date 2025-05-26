package com.example.wayfindr

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import android.speech.SpeechRecognizer

object ErrorHandler {
    fun handleNetworkError(exception: Exception): String {
        return when (exception) {
            is ConnectException -> "Cannot connect to server. Please check if the server is running and the network connection is available."
            is SocketTimeoutException -> "Request timed out. The server may be busy or unreachable."
            is UnknownHostException -> "Cannot resolve server address. Please check your network connection."
            else -> "Network error: ${exception.message ?: "Unknown error occurred"}"
        }
    }

    fun handleSpeechError(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Client side error occurred."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please wait."
            SpeechRecognizer.ERROR_SERVER -> "Server error during speech recognition."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
            else -> "Unknown speech recognition error"
        }
    }
}