package com.example.wayfindr

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Kiosk State represents the 4 distinct visual states
 */
enum class KioskState {
    LISTENING,        // Waiting for user to speak (blue pulsing)
    USER_SPEAKING,    // User is actively speaking (green animated)
    WAITING_LLM,      // Waiting for LLM response (orange loading)
    LLM_RESPONDING    // LLM response being spoken (purple waves)
}

@Composable
fun KioskMode(
    isActive: Boolean,
    uiState: UiState,
    passwordHash: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onExitKiosk: () -> Unit,
    onSpeakResponse: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onSendMessage: (String) -> Unit,
    showConfirmation: Boolean = false,
    pendingConfirmationMessage: String = "",
    onConfirmSend: () -> Unit = {},
    onCancelSend: () -> Unit = {},
    isSpeaking: Boolean = false,
    cameraState: CameraState? = null,
    onPreviewCreated: ((androidx.camera.view.PreviewView) -> Unit)? = null,
    onSwitchCamera: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isListeningContinuously by remember { mutableStateOf(false) }
    var lastResponse by remember { mutableStateOf("") }
    var hasSpokenCurrentResponse by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Determine current kiosk state
    val currentKioskState = when {
        isSpeaking -> KioskState.LLM_RESPONDING
        uiState.isLoading -> KioskState.WAITING_LLM
        uiState.isUserSpeaking -> KioskState.USER_SPEAKING
        else -> KioskState.LISTENING
    }

    // Track the last message to show the latest assistant response
    LaunchedEffect(uiState.messages.size) {
        val lastMessage = uiState.messages.lastOrNull()
        if (lastMessage?.isUser == false && lastMessage.content != lastResponse) {
            lastResponse = lastMessage.content
            hasSpokenCurrentResponse = false
            // Stop listening while we speak the response
            if (uiState.isListening) {
                onStopListening()
            }
        }
        // Auto-scroll to bottom when new messages arrive
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Handle text-to-speech for new responses
    LaunchedEffect(lastResponse, hasSpokenCurrentResponse) {
        if (isActive && lastResponse.isNotEmpty() && !hasSpokenCurrentResponse && !isSpeaking) {
            delay(500)
            onSpeakResponse(lastResponse)
            hasSpokenCurrentResponse = true
        }
    }

    // Start continuous listening when kiosk mode becomes active
    LaunchedEffect(isActive) {
        if (isActive && !isListeningContinuously) {
            isListeningContinuously = true
            delay(500)
            if (!isSpeaking) {
                onStartListening()
            }
        } else if (!isActive) {
            isListeningContinuously = false
            onStopListening()
            onStopSpeaking()
        }
    }

    // Restart listening when speech finishes and we're not loading
    LaunchedEffect(isSpeaking, uiState.isLoading, uiState.error, isListeningContinuously, showConfirmation) {
        if (isActive && isListeningContinuously && !isSpeaking && !uiState.isLoading &&
            uiState.error == null && !uiState.isListening && !showConfirmation) {
            delay(1000)
            onStartListening()
        }
    }

    // Stop listening when we start speaking
    LaunchedEffect(isSpeaking) {
        if (isSpeaking && uiState.isListening) {
            onStopListening()
        }
    }

    if (isActive) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showPasswordDialog = true }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top section: State animation indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    KioskStateAnimation(
                        state = currentKioskState,
                        partialSpeechText = uiState.partialSpeechText,
                        hasError = uiState.error != null
                    )
                }

                // Chat messages section (like normal chat mode)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            KioskChatMessageItem(message = message)
                        }

                        // Show what user is currently saying (partial speech)
                        if (uiState.partialSpeechText.isNotEmpty()) {
                            item {
                                KioskPartialSpeechItem(text = uiState.partialSpeechText)
                            }
                        }

                        // Loading indicator
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
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Thinking...",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom hint
                Text(
                    text = "Tap anywhere to exit kiosk mode",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            // Camera preview overlay (single camera with switch capability)
            if (cameraState != null && (cameraState.hasFrontCamera || cameraState.hasRearCamera)) {
                KioskCameraPreview(
                    cameraState = cameraState,
                    onPreviewCreated = { previewView ->
                        onPreviewCreated?.invoke(previewView)
                    },
                    onSwitchCamera = onSwitchCamera,
                    previewSize = 100.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Confirmation dialog with countdown
        if (showConfirmation && pendingConfirmationMessage.isNotEmpty()) {
            SpeechConfirmationDialog(
                recognizedText = pendingConfirmationMessage,
                onConfirm = onConfirmSend,
                onCancel = onCancelSend,
                onTimeout = onConfirmSend
            )
        }

        if (showPasswordDialog) {
            PasswordDialog(
                passwordHash = passwordHash,
                onCorrectPassword = {
                    showPasswordDialog = false
                    onExitKiosk()
                },
                onDismiss = { showPasswordDialog = false }
            )
        }
    }
}

/**
 * Callback interface for handling speech confirmation in kiosk mode
 */
@Composable
fun rememberKioskSpeechHandler(
    onShowConfirmation: (String) -> Unit
): (String) -> Unit {
    return remember { { text -> onShowConfirmation(text) } }
}

@Composable
fun KioskStateAnimation(
    state: KioskState,
    partialSpeechText: String,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kiosk_state_animation")

    // Base pulse animation
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Fast pulse for user speaking
    val fastPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fast_pulse"
    )

    // Wave animation for LLM responding
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Determine color based on state
    val stateColor = when {
        hasError -> Color(0xFFE53935) // Red for error
        else -> when (state) {
            KioskState.LISTENING -> Color(0xFF2196F3)      // Blue
            KioskState.USER_SPEAKING -> Color(0xFF4CAF50)  // Green
            KioskState.WAITING_LLM -> Color(0xFFFF9800)    // Orange
            KioskState.LLM_RESPONDING -> Color(0xFF9C27B0) // Purple
        }
    }

    val stateText = when {
        hasError -> "Connection Error"
        else -> when (state) {
            KioskState.LISTENING -> "Listening..."
            KioskState.USER_SPEAKING -> "You're speaking..."
            KioskState.WAITING_LLM -> "Processing..."
            KioskState.LLM_RESPONDING -> "Speaking..."
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Animated circles based on state
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .scale(
                        when (state) {
                            KioskState.USER_SPEAKING -> fastPulse
                            KioskState.LLM_RESPONDING -> fastPulse
                            else -> pulse
                        }
                    )
            ) {
                val radius = size.minDimension / 2 * 0.8f
                val strokeWidth = 4.dp.toPx()

                // Outer ring
                drawCircle(
                    color = stateColor.copy(alpha = 0.3f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )

                // Middle ring
                drawCircle(
                    color = stateColor.copy(alpha = 0.5f),
                    radius = radius * 0.7f,
                    style = Stroke(width = strokeWidth * 0.75f)
                )

                // Inner solid circle
                drawCircle(
                    color = stateColor.copy(alpha = 0.8f),
                    radius = radius * 0.4f
                )
            }

            // Loading spinner for WAITING_LLM state
            if (state == KioskState.WAITING_LLM) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }

            // Sound wave indicator for LLM_RESPONDING
            if (state == KioskState.LLM_RESPONDING) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    val centerY = size.height / 2
                    val barWidth = 6.dp.toPx()
                    val spacing = 10.dp.toPx()
                    val maxHeight = size.height * 0.6f

                    for (i in 0..2) {
                        val phase = (wave + i * 120) % 360
                        val heightFactor = kotlin.math.sin(Math.toRadians(phase.toDouble())).toFloat()
                        val barHeight = maxHeight * (0.3f + 0.7f * kotlin.math.abs(heightFactor))

                        drawRoundRect(
                            color = Color.White,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                x = (size.width / 2) - (spacing + barWidth / 2) + (i - 1) * (barWidth + spacing),
                                y = centerY - barHeight / 2
                            ),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stateText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        // Show partial speech text if user is speaking
        if (state == KioskState.USER_SPEAKING && partialSpeechText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$partialSpeechText\"",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun KioskChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "You" else "Assistant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun KioskPartialSpeechItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "You",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                    // Typing indicator dots
                    TypingIndicator()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ), label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ), label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ), label = "dot3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha1),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha2),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha3),
                    CircleShape
                )
        )
    }
}

@Composable
fun SpeechConfirmationDialog(
    recognizedText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onTimeout: () -> Unit
) {
    var countdown by remember { mutableStateOf(5) }
    val scope = rememberCoroutineScope()

    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        onTimeout()
    }

    // Animated progress
    val progress = countdown / 5f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = LinearEasing),
        label = "countdown_progress"
    )

    Dialog(
        onDismissRequest = { /* Prevent dismiss by clicking outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Done Speaking?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show what was recognized
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "\"$recognizedText\"",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Countdown circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            countdown <= 2 -> MaterialTheme.colorScheme.error
                            countdown <= 3 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = countdown.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sending in $countdown seconds...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontSize = 16.sp)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Send Now", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordDialog(
    passwordHash: String,
    onCorrectPassword: () -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Exit Kiosk Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter password to exit kiosk mode",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Incorrect password. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val enteredHash = hashPassword(password)
                            if (enteredHash == passwordHash) {
                                onCorrectPassword()
                            } else {
                                isError = true
                                password = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = password.isNotBlank()
                    ) {
                        Text("Enter")
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordChangeDialog(
    onSave: (String) -> Unit,
    onReset: suspend () -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Change Kiosk Password",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter new password for kiosk mode",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        isError = false
                    },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        isError = false
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            when {
                                newPassword.isBlank() -> {
                                    isError = true
                                    errorMessage = "Password cannot be empty"
                                }
                                newPassword.length < 4 -> {
                                    isError = true
                                    errorMessage = "Password must be at least 4 characters"
                                }
                                newPassword != confirmPassword -> {
                                    isError = true
                                    errorMessage = "Passwords do not match"
                                }
                                else -> {
                                    onSave(newPassword)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank()
                    ) {
                        Text("Save New Password")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                onReset()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset to Default")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
