package com.example.wayfindr

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeakMessage: (String) -> Unit
) {
    val isUser = message.isUser
    val isError = message.content.startsWith("Sorry, I encountered an error") || message.content.contains("error", ignoreCase = true)
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = when {
            isUser -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp) // Reduced max width for better balance
                .padding(
                    start = if (isUser) 48.dp else 8.dp,
                    end = if (isUser) 8.dp else 48.dp,
                    top = 2.dp,
                    bottom = 2.dp
                ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isError -> MaterialTheme.colorScheme.errorContainer
                    isUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Message header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isUser -> "You"
                            isError -> "Error"
                            else -> "Assistant"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isError -> MaterialTheme.colorScheme.onErrorContainer
                            isUser -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (!isUser && !isError) {
                        IconButton(
                            onClick = { onSpeakMessage(message.content) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak message",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message content
                Text(
                    text = message.content,
                    fontSize = 16.sp,
                    color = when {
                        isError -> MaterialTheme.colorScheme.onErrorContainer
                        isUser -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = timeFormatter.format(message.timestamp),
                    fontSize = 10.sp,
                    color = when {
                        isError -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        isUser -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}