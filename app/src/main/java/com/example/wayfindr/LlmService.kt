package com.example.wayfindr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class LlmService {
    private val baseUrl = "http://192.168.0.100:5000"
    private val chatEndpoint = "$baseUrl/chat"

    suspend fun sendMessage(message: String): String = withContext(Dispatchers.IO) {
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

            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("message", message)
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
                return@withContext jsonResponse.optString("response", "No response received")

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