package com.example.wayfindr

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wayfindr.ui.theme.WayfindRTheme
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.NetworkInterface
import java.net.Inet4Address
import androidx.compose.foundation.border
import android.speech.tts.TextToSpeech
import java.util.*

import com.example.wayfindr.dataStore

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var speechManager: SpeechManager
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var settingsDataStore: SettingsDataStore
    private var llmService: LlmService? = null
    private var textToSpeech: TextToSpeech? = null
    private var isSpeaking by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            chatViewModel.setError("Microphone permission is required")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsDataStore = SettingsDataStore(applicationContext)

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }
                    
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }
                })
            }
        }

        setContent {
            WayfindRTheme {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var showUrlDialog by remember { mutableStateOf(false) }
                var showWifiDialog by remember { mutableStateOf(false) }
                var showPasswordDialog by remember { mutableStateOf(false) }
                var isKioskMode by remember { mutableStateOf(false) }
                var llmUrl by remember { mutableStateOf("") }
                var viewModelReady by remember { mutableStateOf(false) }
                var kioskPasswordHash by remember { mutableStateOf("") }

                // Load persisted settings and initialize services on launch
                LaunchedEffect(Unit) {
                    llmUrl = settingsDataStore.getLlmUrl()
                    kioskPasswordHash = settingsDataStore.getKioskPasswordHash()
                    llmService = LlmService(llmUrl)
                    chatViewModel = ChatViewModel(llmService!!)

                    // Initialize SpeechManager after chatViewModel is ready
                    speechManager = SpeechManager(
                        context = this@MainActivity,
                        onSpeechResult = { recognizedText ->
                            // Append recognized text to current input
                            val currentInput = chatViewModel.uiState.value.currentInput
                            val newInput = if (currentInput.isBlank()) {
                                recognizedText
                            } else {
                                "$currentInput $recognizedText"
                            }
                            chatViewModel.updateInput(newInput)
                            
                            // In kiosk mode, automatically send the message
                            if (isKioskMode && recognizedText.isNotBlank()) {
                                chatViewModel.sendMessage(recognizedText)
                            }
                        },
                        onListeningStateChanged = { isListening ->
                            chatViewModel.setListening(isListening)
                        },
                        onError = { errorMessage ->
                            chatViewModel.setError(errorMessage)
                        }
                    )

                    viewModelReady = true
                }

                if (isKioskMode && viewModelReady) {
                    // Kiosk Mode Overlay
                    KioskMode(
                        isActive = isKioskMode,
                        uiState = chatViewModel.uiState.collectAsState().value,
                        passwordHash = kioskPasswordHash,
                        onStartListening = {
                            if (hasMicrophonePermission()) {
                                speechManager.startListening()
                            } else {
                                requestMicrophonePermission()
                            }
                        },
                        onStopListening = {
                            speechManager.stopListening()
                        },
                        onExitKiosk = {
                            isKioskMode = false
                            speechManager.stopListening()
                            speechManager.stopSpeaking()
                            stopTextToSpeech()
                        },
                        onSpeakResponse = { text ->
                            speakText(text)
                        },
                        onStopSpeaking = {
                            stopTextToSpeech()
                        },
                        isSpeaking = isSpeaking
                    )
                } else {
                    // Normal App UI
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Menu", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(16.dp))
                                NavigationDrawerItem(
                                    label = { Text("Delete all chat history") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (viewModelReady) chatViewModel.clearChatHistory()
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Export chat as Markdown") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (viewModelReady) {
                                            val markdown = chatViewModel.exportChatHistoryAsMarkdown()
                                            shareMarkdown(this@MainActivity, markdown)
                                        }
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Set LLM Server URL") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        showUrlDialog = true
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Change Kiosk Password") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        showPasswordDialog = true
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Start Kiosk Mode") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (viewModelReady) {
                                            isKioskMode = true
                                        }
                                    }
                                )
                                NavigationDrawerItem(
                                    label = { Text("Show WiFi Info") },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        showWifiDialog = true
                                    }
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                if (viewModelReady) {
                                    val uiState by chatViewModel.uiState.collectAsState()
                                    TopAppBar(
                                        title = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(stringResource(R.string.chat_title))
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
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                                            }
                                        }
                                    )
                                } else {
                                    TopAppBar(
                                        title = { Text(stringResource(R.string.chat_title)) },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                                            }
                                        }
                                    )
                                }
                            }
                        ) { paddingValues ->
                            if (viewModelReady) {
                                ChatScreen(
                                    viewModel = chatViewModel,
                                    onSpeechToText = {
                                        if (hasMicrophonePermission()) {
                                            if (::speechManager.isInitialized) {
                                                val uiState = chatViewModel.uiState.value
                                                if (uiState.isListening) {
                                                    speechManager.stopListening()
                                                } else {
                                                    speechManager.startListening()
                                                }
                                            }
                                        } else {
                                            requestMicrophonePermission()
                                        }
                                    },
                                    onTextToSpeech = { text ->
                                        speakText(text)
                                    },
                                    onStopSpeaking = {
                                        stopTextToSpeech()
                                    },
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }
                            
                            // URL dialog
                            if (showUrlDialog) {
                                LlmUrlDialog(
                                    currentUrl = llmUrl,
                                    onSave = { url ->
                                        llmUrl = url
                                        scope.launch {
                                            settingsDataStore.setLlmUrl(url)
                                            llmService = LlmService(url)
                                            chatViewModel = ChatViewModel(llmService!!)
                                        }
                                        showUrlDialog = false
                                    },
                                    onReset = { defaultUrl ->
                                        scope.launch {
                                            settingsDataStore.clearLlmUrl()
                                            llmUrl = defaultUrl // update state
                                            llmService = LlmService(defaultUrl)
                                            chatViewModel = ChatViewModel(llmService!!)
                                        }
                                        showUrlDialog = false
                                    },
                                    onDismiss = { showUrlDialog = false }
                                )
                            }
                            
                            // Password change dialog
                            if (showPasswordDialog) {
                                PasswordChangeDialog(
                                    onSave = { newPassword ->
                                        scope.launch {
                                            val hashedPassword = SettingsDataStore.hashPassword(newPassword)
                                            settingsDataStore.setKioskPasswordHash(hashedPassword)
                                            kioskPasswordHash = hashedPassword
                                        }
                                        showPasswordDialog = false
                                    },
                                    onReset = {
                                        scope.launch {
                                            settingsDataStore.clearKioskPassword()
                                            val defaultPassword = settingsDataStore.getKioskPasswordHash()
                                            kioskPasswordHash = defaultPassword
                                        }
                                        showPasswordDialog = false
                                    },
                                    onDismiss = { showPasswordDialog = false }
                                )
                            }
                            
                            // WiFi dialog
                            if (showWifiDialog) {
                                WifiInfoDialog(
                                    context = this@MainActivity,
                                    onDismiss = { showWifiDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
        checkMicrophonePermission()
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    private fun stopTextToSpeech() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        isSpeaking = false
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
        textToSpeech?.shutdown()
    }

    override fun onPause() {
        super.onPause()
        if (::speechManager.isInitialized) {
            speechManager.stopSpeaking()
            speechManager.stopListening()
        }
        stopTextToSpeech()
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
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Chat messages with border
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp), // Padding inside the bordered area
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onSpeakMessage = onTextToSpeech
                    )
                }

                // Add loading indicator as an item in the LazyColumn
                if (uiState.isLoading) {
                    item {
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
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Speech input button
            IconButton(
                onClick = onSpeechToText,
                enabled = !isLoading,
                modifier = modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Speech to text",
                    tint = when {
                        isListening -> MaterialTheme.colorScheme.error
                        !isLoading -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = modifier.size(24.dp)
                )
            }

            // Text input field
            OutlinedTextField(
                value = currentInput,
                onValueChange = onInputChange,
                modifier = modifier.weight(1f),
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
                modifier = modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (currentInput.isNotBlank() && !isLoading && !isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    modifier = modifier.size(24.dp)
                )
            }
        }
    }
}

// Dialog for editing LLM URL
@Composable
fun LlmUrlDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onReset: suspend (String) -> Unit, // Pass the new url to parent
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var url by remember { mutableStateOf(currentUrl) }
    val defaultUrl = stringResource(R.string.default_llm_url)
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set LLM Server URL") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("LLM Server URL") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Example: $defaultUrl")
            }
        },
        confirmButton = {
            Button(onClick = { onSave(url) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        scope.launch {
                            url = defaultUrl // Update the dialog's text field immediately
                            onReset(defaultUrl) // Clear storage and update app state
                        }
                    }
                ) { Text("Reset to Default") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

// Dialog for WiFi info
@Composable
fun WifiInfoDialog(context: Context, onDismiss: () -> Unit) {
    // Get IP address using ConnectivityManager and NetworkCapabilities
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
    var ipAddress = "Unavailable"
    if (networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addrs = intf.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    ipAddress = addr.hostAddress ?: "Unavailable"
                }
            }
        }
    }

    // Try to get MAC address (may be unavailable on Android 10+)
    var macAddress = "Unavailable"
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            if (intf.name.equals("wlan0", ignoreCase = true)) {
                val mac = intf.hardwareAddress
                if (mac != null) {
                    macAddress = mac.joinToString(":") { "%02X".format(it) }
                }
                break
            }
        }
    } catch (_: Exception) {}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WiFi Info") },
        text = {
            Column {
                Text("IP Address: $ipAddress")
                Text("MAC Address: $macAddress")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}