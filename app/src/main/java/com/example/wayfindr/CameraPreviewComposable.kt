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
    // Track if we've already called the callback to avoid re-binding on every recomposition
    var hasCalledCallback by remember { mutableStateOf(false) }

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
                // Only call the callback once when the view is first created/attached
                if (!hasCalledCallback) {
                    hasCalledCallback = true
                    onPreviewViewCreated(previewView)
                }
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
 * Dual camera preview layout for kiosk mode.
 * Shows front camera (top-left) and rear camera (top-right) above the chat area.
 *
 * Note: CameraX only supports one LIVE camera at a time per lifecycle owner.
 * The active camera shows live preview, the other shows a placeholder with switch button.
 */
@Composable
fun KioskCameraPreview(
    cameraState: CameraState,
    onPreviewCreated: (PreviewView) -> Unit,
    onSwitchCamera: (() -> Unit)? = null,
    previewSize: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    // Show if we can switch cameras (both available)
    val canSwitch = cameraState.hasFrontCamera && cameraState.hasRearCamera

    // Show preview if ANY camera is available
    val showPreview = cameraState.hasFrontCamera || cameraState.hasRearCamera

    Box(modifier = modifier.fillMaxSize()) {
        // Front camera preview - TOP LEFT
        if (cameraState.hasFrontCamera) {
            if (cameraState.isFrontCameraActive) {
                // Live preview for front camera
                CameraPreview(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                        .size(previewSize),
                    onPreviewViewCreated = onPreviewCreated,
                    label = "Front"
                )
            } else {
                // Placeholder for front camera (tap to switch)
                CameraPlaceholder(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                        .size(previewSize),
                    label = "Front",
                    isClickable = canSwitch && onSwitchCamera != null,
                    onClick = { onSwitchCamera?.invoke() }
                )
            }
        }

        // Rear camera preview - TOP RIGHT
        if (cameraState.hasRearCamera) {
            if (cameraState.isRearCameraActive) {
                // Live preview for rear camera
                CameraPreview(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 12.dp)
                        .size(previewSize),
                    onPreviewViewCreated = onPreviewCreated,
                    label = "Rear"
                )
            } else {
                // Placeholder for rear camera (tap to switch)
                CameraPlaceholder(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 12.dp)
                        .size(previewSize),
                    label = "Rear",
                    isClickable = canSwitch && onSwitchCamera != null,
                    onClick = { onSwitchCamera?.invoke() }
                )
            }
        }

        // Streaming indicator - below the camera previews
        if (cameraState.isStreaming) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = previewSize + 16.dp)
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
                    .align(Alignment.TopCenter)
                    .padding(top = if (cameraState.isStreaming) previewSize + 40.dp else previewSize + 16.dp)
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

/**
 * Placeholder for inactive camera - shows label and tap-to-switch hint
 */
@Composable
fun CameraPlaceholder(
    modifier: Modifier = Modifier,
    label: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (isClickable) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to switch",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp
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
