package com.example.wayfindr

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

class LlmService(private val baseUrl: String) {
    private val tag = "LlmService"
    private val chatEndpoint get() = "$baseUrl/chat"

    companion object {
        const val MAX_RETRIES = 3
        const val INITIAL_DELAY_MS = 1000L
        const val MAX_DELAY_MS = 10000L
    }

    /**
     * Send a single message without context (backward compatible)
     */
    suspend fun sendMessage(message: String): String =
        sendMessageWithContext(message, emptyList())

    /**
     * Send a message with conversation context for continuous conversation mode.
     * The context is sent as an array of previous messages.
     * Includes retry logic with exponential backoff for network errors.
     */
    suspend fun sendMessageWithContext(
        message: String,
        context: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        var currentDelay = INITIAL_DELAY_MS

        for (attempt in 1..MAX_RETRIES) {
            try {
                return@withContext sendMessageInternal(message, context)
            } catch (e: Exception) {
                lastException = e
                val isRetryable = isRetryableError(e)

                Log.w(tag, "Attempt $attempt failed: ${e.message}, retryable: $isRetryable")

                if (!isRetryable || attempt == MAX_RETRIES) {
                    break
                }

                // Exponential backoff
                Log.d(tag, "Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(MAX_DELAY_MS)
            }
        }

        // All retries failed
        throw lastException ?: Exception("Unknown error occurred")
    }

    /**
     * Check if an error is retryable (network issues, timeouts)
     */
    private fun isRetryableError(e: Exception): Boolean {
        return when {
            e is SocketTimeoutException -> true
            e is UnknownHostException -> true
            e.message?.contains("timeout", ignoreCase = true) == true -> true
            e.message?.contains("connection", ignoreCase = true) == true -> true
            e.message?.contains("network", ignoreCase = true) == true -> true
            e.message?.contains("Server error (5", ignoreCase = true) == true -> true // 5xx errors
            else -> false
        }
    }

    /**
     * Internal message sending without retry logic
     */
    private suspend fun sendMessageInternal(
        message: String,
        context: List<ChatMessage>
    ): String {
        try {
            val url = URL(chatEndpoint)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                doInput = true
                connectTimeout = 30000 // 30 seconds
                readTimeout = 60000 // 60 seconds
            }

            // Create JSON payload with optional context
            val jsonPayload = JSONObject().apply {
                put("message", message)

                // Add conversation context if available
                if (context.isNotEmpty()) {
                    val contextArray = JSONArray()
                    for (msg in context) {
                        val msgObj = JSONObject().apply {
                            put("role", if (msg.isUser) "user" else "assistant")
                            put("content", msg.content)
                        }
                        contextArray.put(msgObj)
                    }
                    put("context", contextArray)
                }
            }

            // Send request
            val outputStream = connection.outputStream
            val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            writer.write(jsonPayload.toString())
            writer.flush()
            writer.close()
            outputStream.close()

            // Read response
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                inputStream.close()

                // Parse JSON response
                val jsonResponse = JSONObject(response.toString())
                return jsonResponse.optString("response", "No response received")

            } else {
                // Read error stream
                val errorStream = connection.errorStream
                val errorMessage = if (errorStream != null) {
                    val errorReader = BufferedReader(InputStreamReader(errorStream, StandardCharsets.UTF_8))
                    val errorResponse = StringBuilder()
                    var line: String?

                    while (errorReader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }

                    errorReader.close()
                    errorStream.close()
                    errorResponse.toString()
                } else {
                    "HTTP Error $responseCode"
                }

                throw Exception("Server error ($responseCode): $errorMessage")
            }

        } catch (e: Exception) {
            throw Exception("Failed to send message: ${e.message}")
        }
    }
}