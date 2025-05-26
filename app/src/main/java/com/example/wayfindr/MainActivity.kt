package com.example.wayfindr

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wayfindr.ui.theme.WayfindRTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Send
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.Context
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private lateinit var speechManager: SpeechManager
    private lateinit var chatViewModel: ChatViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            chatViewModel.setError("Microphone permission is required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WayfindRTheme {
                chatViewModel = viewModel()
                if (!::speechManager.isInitialized) {
                    speechManager = SpeechManager(
                        context = this@MainActivity,
                        onSpeechResult = { chatViewModel.sendMessage(it) },
                        onListeningStateChanged = { chatViewModel.setListening(it) },
                        onError = { chatViewModel.setError(it) }
                    )
                }
                ChatScreen(
                    viewModel = chatViewModel,
                    onSpeechToText = {
                        if (hasMicrophonePermission()) {
                            speechManager.startListening()
                        } else {
                            requestMicrophonePermission()
                        }
                    },
                    onTextToSpeech = { speechManager.speakText(it) },
                    onStopSpeaking = { speechManager.stopSpeaking() }
                )
            }
        }
        checkMicrophonePermission()
    }

    private fun checkMicrophonePermission() {
        if (!hasMicrophonePermission()) requestMicrophonePermission()
    }

    private fun hasMicrophonePermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestMicrophonePermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechManager.isInitialized) speechManager.cleanup()
    }

    override fun onPause() {
        super.onPause()
        if (::speechManager.isInitialized) {
            speechManager.stopSpeaking()
            speechManager.stopListening()
        }
    }
}

fun shareMarkdown(context: Context, markdown: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_SUBJECT, "WayfindR Chat History")
        putExtra(Intent.EXTRA_TEXT, markdown)
    }
    context.startActivity(Intent.createChooser(intent, "Share chat as markdown"))
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onSpeechToText: () -> Unit,
    onTextToSpeech: (String) -> Unit,
    onStopSpeaking: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { viewModel.clearChatHistory() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Delete all chat history")
            }
            Button(
                onClick = {
                    val markdown = viewModel.exportChatHistoryAsMarkdown()
                    shareMarkdown(context, markdown)
                }
            ) {
                Text("Export chat as Markdown")
            }
        }

        // Chat header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chat_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (uiState.error == null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (uiState.error == null) "Connected" else "Error",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onSpeakMessage = onTextToSpeech
                )
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.thinking))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input section
        ChatInputSection(
            currentInput = uiState.currentInput,
            onInputChange = viewModel::updateInput,
            onSendMessage = {
                viewModel.sendMessage(uiState.currentInput)
                keyboardController?.hide()
            },
            onSpeechToText = onSpeechToText,
            isListening = uiState.isListening,
            isLoading = uiState.isLoading
        )
    }
}

@Composable
fun ChatInputSection(
    currentInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onSpeechToText: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Speech input button
            IconButton(
                onClick = onSpeechToText,
                enabled = !isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Speech to text",
                    tint = when {
                        isListening -> MaterialTheme.colorScheme.error
                        !isLoading -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text input field
            OutlinedTextField(
                value = currentInput,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { 
                    Text(
                        if (isListening) "Listening..." else "Type your message..."
                    ) 
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { 
                        if (currentInput.isNotBlank() && !isLoading) {
                            onSendMessage()
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = !isListening && !isLoading,
                maxLines = 3
            )

            // Send button
            IconButton(
                onClick = onSendMessage,
                enabled = currentInput.isNotBlank() && !isLoading && !isListening,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (currentInput.isNotBlank() && !isLoading && !isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}