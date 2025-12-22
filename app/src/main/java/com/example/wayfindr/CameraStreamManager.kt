package com.example.wayfindr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
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

data class CameraInfo(
    val cameraId: String,
    val isFrontFacing: Boolean,
    val isAvailable: Boolean
)

data class CameraState(
    val hasFrontCamera: Boolean = false,
    val hasRearCamera: Boolean = false,
    val isFrontCameraActive: Boolean = false,
    val isRearCameraActive: Boolean = false,
    val isStreaming: Boolean = false,
    val lastFrontImage: Bitmap? = null,
    val lastRearImage: Bitmap? = null,
    val streamIntervalMs: Long = 3000L
)

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

    private var frontCameraProvider: ProcessCameraProvider? = null
    private var rearCameraProvider: ProcessCameraProvider? = null

    private var frontImageCapture: ImageCapture? = null
    private var rearImageCapture: ImageCapture? = null

    private var frontPreview: Preview? = null
    private var rearPreview: Preview? = null

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
        }
    }

    fun bindCameraPreviews(
        frontPreviewView: PreviewView?,
        rearPreviewView: PreviewView?
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            try {
                // Unbind all existing use cases
                cameraProvider.unbindAll()

                val state = _cameraState.value

                // Bind front camera if available and preview view provided
                if (state.hasFrontCamera && frontPreviewView != null) {
                    bindFrontCamera(cameraProvider, frontPreviewView)
                }

                // Bind rear camera if available and preview view provided
                if (state.hasRearCamera && rearPreviewView != null) {
                    bindRearCamera(cameraProvider, rearPreviewView)
                }

            } catch (e: Exception) {
                Log.e(tag, "Error binding cameras: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindFrontCamera(cameraProvider: ProcessCameraProvider, previewView: PreviewView) {
        try {
            frontPreview = Preview.Builder()
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            frontImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                frontPreview,
                frontImageCapture
            )

            _cameraState.value = _cameraState.value.copy(isFrontCameraActive = true)
            Log.d(tag, "Front camera bound successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error binding front camera: ${e.message}")
            _cameraState.value = _cameraState.value.copy(isFrontCameraActive = false)
        }
    }

    private fun bindRearCamera(cameraProvider: ProcessCameraProvider, previewView: PreviewView) {
        try {
            rearPreview = Preview.Builder()
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            rearImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display?.rotation ?: 0)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                rearPreview,
                rearImageCapture
            )

            _cameraState.value = _cameraState.value.copy(isRearCameraActive = true)
            Log.d(tag, "Rear camera bound successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error binding rear camera: ${e.message}")
            _cameraState.value = _cameraState.value.copy(isRearCameraActive = false)
        }
    }

    fun startStreaming(intervalMs: Long = 3000L) {
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
                captureAndStreamImages()
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
            // Restart streaming with new interval
            stopStreaming()
            startStreaming(intervalMs)
        }
    }

    private suspend fun captureAndStreamImages() {
        val state = _cameraState.value

        // Capture from front camera if active
        if (state.isFrontCameraActive && frontImageCapture != null) {
            captureImage(frontImageCapture!!, "front")
        }

        // Capture from rear camera if active
        if (state.isRearCameraActive && rearImageCapture != null) {
            captureImage(rearImageCapture!!, "rear")
        }
    }

    private suspend fun captureImage(imageCapture: ImageCapture, cameraType: String) {
        withContext(Dispatchers.Main) {
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        streamingScope.launch {
                            try {
                                val bitmap = imageProxyToBitmap(image, cameraType == "front")
                                image.close()

                                // Update state with latest image
                                if (cameraType == "front") {
                                    _cameraState.value = _cameraState.value.copy(lastFrontImage = bitmap)
                                } else {
                                    _cameraState.value = _cameraState.value.copy(lastRearImage = bitmap)
                                }

                                // Stream the image to the server
                                streamImageToServer(bitmap, cameraType)
                            } catch (e: Exception) {
                                Log.e(tag, "Error processing captured image: ${e.message}")
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(tag, "Image capture failed for $cameraType: ${exception.message}")
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

        // Apply rotation based on image rotation
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
                // Convert bitmap to base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // Create JSON payload
                val payload = JSONObject().apply {
                    put("camera", cameraType)
                    put("timestamp", System.currentTimeMillis())
                    put("image", base64Image)
                    put("format", "jpeg")
                    put("width", bitmap.width)
                    put("height", bitmap.height)
                }

                // Send to server
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
                    Log.d(tag, "Image from $cameraType camera streamed successfully")
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
        frontCameraProvider?.unbindAll()
        rearCameraProvider?.unbindAll()
    }
}
