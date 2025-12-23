# WayfindR Architecture Overview

## Project Structure

```
app/src/main/
├── java/com/example/wayfindr/
│   ├── MainActivity.kt              # Main entry point, navigation, dialogs
│   ├── ChatViewModel.kt             # Chat state management (MVVM)
│   ├── ChatMessage.kt               # Message data model
│   ├── ChatUIState.kt               # UI state data class
│   ├── ChatMessageItem.kt           # Individual message composable
│   ├── KioskMode.kt                 # Kiosk mode UI and logic
│   ├── KioskDeviceAdminReceiver.kt  # Device owner receiver
│   ├── SpeechManager.kt             # STT/TTS management
│   ├── CameraStreamManager.kt       # Camera capture and streaming
│   ├── CameraPreviewComposable.kt   # Camera preview UI
│   ├── LlmService.kt                # Backend API communication
│   ├── SettingsDataStore.kt         # Persistent settings
│   ├── ErrorHandler.kt              # Error message utilities
│   └── ui/theme/                    # Material 3 theming
├── res/
│   ├── values/strings.xml           # String resources
│   ├── xml/device_admin.xml         # Device admin policies
│   └── xml/network_security_config.xml
└── AndroidManifest.xml
```

## Technology Stack

- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel)
- **Min SDK:** Android 11 (API 30)
- **Target SDK:** Android 15 (API 35)

## Key Components

### 1. Chat System

```
User Input → ChatViewModel → LlmService → Backend API
                ↓
         UiState (messages, loading, errors)
                ↓
         ChatScreen / KioskMode (UI)
```

### 2. Speech System

```
Microphone → SpeechManager (STT) → ChatViewModel
                                          ↓
                              LlmService → Backend
                                          ↓
TextToSpeech ← SpeechManager (TTS) ← Response
```

### 3. Camera System (Kiosk Mode)

```
Front Camera ─┬─→ CameraStreamManager → HTTP POST /images
Rear Camera  ─┘           ↓
                    Preview Views (CameraX)
```

### 4. Kiosk Mode Flow

```
Enter Kiosk Mode
      ↓
Check Device Owner? ─── Yes ──→ startLockTask() [Full lock]
      │                              ↓
      No                      Hide System UI
      ↓                              ↓
Immersive Mode Only          Start Camera Streaming
[Can be bypassed]                   ↓
                             Start Speech Listening
                                    ↓
                             User speaks → Confirmation Dialog
                                    ↓
                             Send to LLM → TTS Response
                                    ↓
                             Loop listening
```

## Data Flow

### Settings Persistence

```kotlin
SettingsDataStore (DataStore Preferences)
├── LLM URL (configurable backend)
└── Kiosk Password Hash (SHA-256)
```

### API Communication

```
POST {baseUrl}/chat
Request:  { "message": "user input" }
Response: { "response": "LLM output" }

POST {baseUrl}/images
Request:  {
  "camera": "front|rear",
  "timestamp": 1234567890,
  "image": "<base64-jpeg>",
  "format": "jpeg",
  "width": 1920,
  "height": 1080
}
```

## State Management

### UiState (ChatUIState.kt)

```kotlin
data class UiState(
    val messages: List<ChatMessage>,
    val currentInput: String,
    val isLoading: Boolean,
    val isListening: Boolean,
    val error: String?,
    val partialSpeechText: String,
    val isUserSpeaking: Boolean
)
```

### CameraState (CameraStreamManager.kt)

```kotlin
data class CameraState(
    val hasFrontCamera: Boolean,
    val hasRearCamera: Boolean,
    val isFrontCameraActive: Boolean,
    val isRearCameraActive: Boolean,
    val isStreaming: Boolean,
    val streamIntervalMs: Long
)
```

### KioskState (KioskMode.kt)

```kotlin
enum class KioskState {
    LISTENING,       // Blue - waiting for user
    USER_SPEAKING,   // Green - user talking
    WAITING_LLM,     // Orange - processing
    LLM_RESPONDING   // Purple - TTS speaking
}
```

## Permissions Required

| Permission | Purpose |
|------------|---------|
| INTERNET | Backend API communication |
| RECORD_AUDIO | Speech-to-text |
| CAMERA | Image streaming |
| WAKE_LOCK | Keep screen on in kiosk |
| REORDER_TASKS | Lock task mode |

## Dependencies

- Jetpack Compose (BOM managed)
- Material 3
- CameraX (camera-core, camera2, lifecycle, view)
- DataStore Preferences
- Lifecycle/ViewModel
- Coroutines

## Build Configuration

- Compose compiler: 1.5.8
- 16KB page size support enabled
- Cleartext traffic allowed (local network)
