package com.example.wayfindr

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.security.MessageDigest
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var isListeningContinuously by remember { mutableStateOf(false) }
    var lastResponse by remember { mutableStateOf("") }
    var hasSpokenCurrentResponse by remember { mutableStateOf(false) }
    
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
    }

    // Handle text-to-speech for new responses
    LaunchedEffect(lastResponse, hasSpokenCurrentResponse) {
        if (isActive && lastResponse.isNotEmpty() && !hasSpokenCurrentResponse && !isSpeaking) {
            // Small delay to ensure UI updates
            delay(500)
            onSpeakResponse(lastResponse)
            hasSpokenCurrentResponse = true
        }
    }

    // Start continuous listening when kiosk mode becomes active
    LaunchedEffect(isActive) {
        if (isActive && !isListeningContinuously) {
            isListeningContinuously = true
            // Wait a moment for UI to settle
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
    LaunchedEffect(isSpeaking, uiState.isLoading, uiState.error, isListeningContinuously) {
        if (isActive && isListeningContinuously && !isSpeaking && !uiState.isLoading && uiState.error == null && !uiState.isListening) {
            // Restart listening after speech finishes
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
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showPasswordDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Circle animation takes up the top portion
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    KioskAnimation(
                        hasError = uiState.error != null,
                        isListening = uiState.isListening,
                        isLoading = uiState.isLoading,
                        isSpeaking = isSpeaking
                    )
                }
                
                // Response text area takes up the bottom portion
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (lastResponse.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = lastResponse,
                                color = Color.White,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState()),
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    } else {
                        // Show welcome message when no response yet
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "WayfindR Kiosk Mode",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Speak your question and I'll help you find your way",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
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

@Composable
fun KioskAnimation(
    hasError: Boolean,
    isListening: Boolean,
    isLoading: Boolean,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Animation for the pulsing circle
    val infiniteTransition = rememberInfiniteTransition(label = "kiosk_animation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    // Faster pulse when speaking
    val speakingScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_scale_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(240.dp)
                    .scale(if (isSpeaking) speakingScale else scale)
            ) {
                val radius = size.minDimension / 2 * 0.8f
                val strokeWidth = 6.dp.toPx()
                
                val circleColor = when {
                    hasError -> Color.Red
                    else -> Color(0xFF2196F3) // Blue color matching UI state indicator
                }
                
                // Outer pulsing circle
                drawCircle(
                    color = circleColor.copy(alpha = 0.4f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                
                // Middle circle
                drawCircle(
                    color = circleColor.copy(alpha = 0.6f),
                    radius = radius * 0.75f,
                    style = Stroke(width = strokeWidth * 0.75f)
                )
                
                // Inner solid circle
                drawCircle(
                    color = circleColor.copy(alpha = 0.8f),
                    radius = radius * 0.5f
                )
            }
            
            // Inner animated circle for listening state
            if (isListening && !isSpeaking) {
                Canvas(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(1.1f)
                ) {
                    val radius = size.minDimension / 2 * 0.7f
                    val strokeWidth = 3.dp.toPx()
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
            
            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = Color.White,
                    strokeWidth = 4.dp
                )
            }
            
            // Speaking indicator
            if (isSpeaking) {
                Canvas(
                    modifier = Modifier
                        .size(100.dp)
                ) {
                    val radius = size.minDimension / 2 * 0.6f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = radius
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status text
        Text(
            text = when {
                hasError -> "Connection Error"
                isSpeaking -> "Speaking..."
                isLoading -> "Processing..."
                isListening -> "Listening..."
                else -> "Ready"
            },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Tap anywhere to exit kiosk mode",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
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

// Password change dialog - Fixed version
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
                
                // Fixed: Use proper layout for three buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save button
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
                    
                    // Reset to default button
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
                    
                    // Cancel button
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

// Utility function to hash passwords
fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}