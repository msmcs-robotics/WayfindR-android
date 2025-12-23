package com.example.wayfindr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class CameraState(
    val hasFrontCamera: Boolean = false,
    val hasRearCamera: Boolean = false,
    val isFrontCameraActive: Boolean = false,
    val isRearCameraActive: Boolean = false,
    val isStreaming: Boolean = false,
    val streamIntervalMs: Long = 3000L,
    val imageQuality: Int = 80,
    val streamingEnabled: Boolean = true,
    val lastError: String? = null
)

/**
 * Manages camera preview and image streaming.
 *
 * Note: CameraX can only bind ONE camera at a time per lifecycle owner.
 * This implementation alternates between front and rear cameras for capture,
 * but only shows one preview at a time.
 */
class CameraStreamManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val baseUrl: String
) {
    private val tag = "CameraStreamManager"

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var streamingJob: Job? = null
    private val streamingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var cameraProvider: ProcessCameraProvider? = null
    private var currentImageCapture: ImageCapture? = null
    private var currentPreview: Preview? = null
    private var currentCameraSelector: CameraSelector? = null

    // Track which camera is currently bound
    private var currentlyBoundCamera: String? = null // "front" or "rear"

    init {
        detectAvailableCameras()
    }

    private fun detectAvailableCameras() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager
            val cameraIds = cameraManager.cameraIdList

            var hasFront = false
            var hasRear = false

            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                when (lensFacing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        hasFront = true
                        Log.d(tag, "Front camera detected: $cameraId")
                    }
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        hasRear = true
                        Log.d(tag, "Rear camera detected: $cameraId")
                    }
                }
            }

            _cameraState.value = _cameraState.value.copy(
                hasFrontCamera = hasFront,
                hasRearCamera = hasRear
            )

            Log.d(tag, "Camera detection complete - Front: $hasFront, Rear: $hasRear")
        } catch (e: Exception) {
            Log.e(tag, "Error detecting cameras: ${e.message}")
            _cameraState.value = _cameraState.value.copy(lastError = "Camera detection failed")
        }
    }

    /**
     * Bind a single camera preview. CameraX only supports one camera at a time.
     * For dual preview, you would need to use Camera2 API directly.
     */
    fun bindCameraPreview(
        previewView: PreviewView?,
        preferFront: Boolean = true
    ) {
        if (previewView == null) return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll()

                val state = _cameraState.value

                // Determine which camera to bind
                val useFront = preferFront && state.hasFrontCamera
                val useRear = !preferFront && state.hasRearCamera

                val cameraToUse = when {
                    useFront -> "front"
                    useRear -> "rear"
                    state.hasFrontCamera -> "front"
                    state.hasRearCamera -> "rear"
                    else -> null
                }

                if (cameraToUse != null) {
                    bindCamera(previewView, cameraToUse)
                } else {
                    Log.w(tag, "No cameras available to bind")
                    _cameraState.value = _cameraState.value.copy(lastError = "No cameras available")
                }

            } catch (e: Exception) {
                Log.e(tag, "Error binding camera: ${e.message}")
                _cameraState.value = _cameraState.value.copy(lastError = e.message)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(previewView: PreviewView, cameraType: String) {
        try {
            val lensFacing = if (cameraType == "front") {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

            currentCameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            currentPreview = Preview.Builder()
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            currentImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()

            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                currentCameraSelector!!,
                currentPreview,
                currentImageCapture
            )

            currentlyBoundCamera = cameraType

            _cameraState.value = _cameraState.value.copy(
                isFrontCameraActive = cameraType == "front",
                isRearCameraActive = cameraType == "rear",
                lastError = null
            )

            Log.d(tag, "$cameraType camera bound successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error binding $cameraType camera: ${e.message}")
            _cameraState.value = _cameraState.value.copy(
                isFrontCameraActive = false,
                isRearCameraActive = false,
                lastError = e.message
            )
        }
    }

    /**
     * Switch to the other camera (front <-> rear)
     */
    fun switchCamera(previewView: PreviewView?) {
        if (previewView == null) return

        val newCamera = if (currentlyBoundCamera == "front") "rear" else "front"
        val state = _cameraState.value

        // Check if the other camera is available
        val canSwitch = when (newCamera) {
            "front" -> state.hasFrontCamera
            "rear" -> state.hasRearCamera
            else -> false
        }

        if (canSwitch) {
            cameraProvider?.unbindAll()
            bindCamera(previewView, newCamera)
        }
    }

    fun startStreaming(intervalMs: Long = 3000L) {
        if (!_cameraState.value.streamingEnabled) {
            Log.d(tag, "Streaming is disabled")
            return
        }

        if (streamingJob?.isActive == true) {
            Log.d(tag, "Streaming already active")
            return
        }

        _cameraState.value = _cameraState.value.copy(
            isStreaming = true,
            streamIntervalMs = intervalMs
        )

        streamingJob = streamingScope.launch {
            Log.d(tag, "Starting image streaming with interval: ${intervalMs}ms")
            while (isActive) {
                captureAndStreamImage()
                delay(intervalMs)
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _cameraState.value = _cameraState.value.copy(isStreaming = false)
        Log.d(tag, "Streaming stopped")
    }

    fun setStreamInterval(intervalMs: Long) {
        _cameraState.value = _cameraState.value.copy(streamIntervalMs = intervalMs)
        if (_cameraState.value.isStreaming) {
            stopStreaming()
            startStreaming(intervalMs)
        }
    }

    fun setImageQuality(quality: Int) {
        _cameraState.value = _cameraState.value.copy(imageQuality = quality.coerceIn(10, 100))
    }

    fun setStreamingEnabled(enabled: Boolean) {
        _cameraState.value = _cameraState.value.copy(streamingEnabled = enabled)
        if (!enabled) {
            stopStreaming()
        }
    }

    private suspend fun captureAndStreamImage() {
        val imageCapture = currentImageCapture ?: return
        val cameraType = currentlyBoundCamera ?: return

        withContext(Dispatchers.Main) {
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        streamingScope.launch {
                            try {
                                val bitmap = imageProxyToBitmap(image, cameraType == "front")
                                image.close()
                                streamImageToServer(bitmap, cameraType)
                            } catch (e: Exception) {
                                Log.e(tag, "Error processing captured image: ${e.message}")
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(tag, "Image capture failed: ${exception.message}")
                    }
                }
            )
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy, mirrorHorizontally: Boolean): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        if (bitmap == null) {
            // Fallback: create a small placeholder bitmap
            bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        }

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0 || mirrorHorizontally) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            if (mirrorHorizontally) {
                matrix.postScale(-1f, 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        return bitmap
    }

    private suspend fun streamImageToServer(bitmap: Bitmap, cameraType: String) {
        withContext(Dispatchers.IO) {
            try {
                val quality = _cameraState.value.imageQuality

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val payload = JSONObject().apply {
                    put("camera", cameraType)
                    put("timestamp", System.currentTimeMillis())
                    put("image", base64Image)
                    put("format", "jpeg")
                    put("width", bitmap.width)
                    put("height", bitmap.height)
                }

                val url = URL("$baseUrl/images")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(tag, "Image streamed successfully")
                } else {
                    Log.w(tag, "Image stream returned code: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e(tag, "Error streaming image: ${e.message}")
            }
        }
    }

    fun cleanup() {
        stopStreaming()
        streamingScope.cancel()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
