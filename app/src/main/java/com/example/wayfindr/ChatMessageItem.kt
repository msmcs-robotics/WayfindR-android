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
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = if (isUser) 48.dp else 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
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
                        text = if (isUser) "You" else "Assistant",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (!isUser) {
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
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = timeFormatter.format(message.timestamp),
                    fontSize = 10.sp,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}