package com.example.wayfindr

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit,
    label: String = ""
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                onPreviewViewCreated(previewView)
            }
        )

        // Label overlay
        if (label.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Single camera preview for kiosk mode.
 * CameraX only supports one camera at a time per lifecycle owner.
 * This shows the currently active camera with an option to switch.
 */
@Composable
fun KioskCameraPreview(
    cameraState: CameraState,
    onPreviewCreated: (PreviewView) -> Unit,
    onSwitchCamera: (() -> Unit)? = null,
    previewSize: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    // Determine which camera is active and its label
    val cameraLabel = when {
        cameraState.isFrontCameraActive -> "Front"
        cameraState.isRearCameraActive -> "Rear"
        else -> "Camera"
    }

    // Show if we can switch cameras (both available)
    val canSwitch = cameraState.hasFrontCamera && cameraState.hasRearCamera

    Box(modifier = modifier.fillMaxSize()) {
        // Single camera preview - bottom left
        if (cameraState.isFrontCameraActive || cameraState.isRearCameraActive) {
            CameraPreview(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 60.dp)
                    .size(previewSize),
                onPreviewViewCreated = onPreviewCreated,
                label = cameraLabel
            )

            // Switch camera button (if both cameras available)
            if (canSwitch && onSwitchCamera != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp + previewSize - 28.dp, bottom = 60.dp)
                        .size(28.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSwitchCamera() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u21BB", // Unicode for rotate symbol
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Streaming indicator
        if (cameraState.isStreaming) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.Red.copy(alpha = 0.8f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "STREAMING",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Error indicator
        if (cameraState.lastError != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(
                        Color(0xFFFF5722).copy(alpha = 0.9f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Camera Error",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Keep the old function for backwards compatibility but mark as deprecated
@Deprecated("Use KioskCameraPreview instead - CameraX only supports one camera at a time")
@Composable
fun KioskCameraPreviews(
    cameraState: CameraState,
    onFrontPreviewCreated: (PreviewView) -> Unit,
    onRearPreviewCreated: (PreviewView) -> Unit,
    previewSize: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    // Redirect to new single camera preview
    KioskCameraPreview(
        cameraState = cameraState,
        onPreviewCreated = { previewView ->
            if (cameraState.isFrontCameraActive) {
                onFrontPreviewCreated(previewView)
            } else {
                onRearPreviewCreated(previewView)
            }
        },
        onSwitchCamera = null,
        previewSize = previewSize,
        modifier = modifier
    )
}
