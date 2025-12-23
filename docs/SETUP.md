# WayfindR Setup Guide

## Development Environment

### Requirements
- Android Studio (latest stable)
- JDK 17 or higher
- ADB (Android Debug Bridge)
- Target device: Samsung Galaxy Tab A7 (or emulator)

### Remote Development Setup

This project uses remote development - code is edited locally (WSL) and synced to an Ubuntu server with Android Studio.

**Connection details:** See `connections.md`

**Rsync command:**
```bash
rsync -avz --exclude '.gradle' --exclude 'build' --exclude '.idea' --exclude '*.iml' \
  ~/WayfindR-android/ devel@192.168.0.7:~/Desktop/WayfindR-android/
```

**Build on remote:**
```bash
ssh devel@192.168.0.7 "cd ~/Desktop/WayfindR-android && \
  JAVA_HOME=/snap/android-studio/209/jbr ./gradlew assembleDebug"
```

---

## Device Setup

### 1. Enable Developer Options
1. Go to Settings > About tablet
2. Tap "Build number" 7 times
3. Enter PIN if prompted
4. "Developer mode enabled" will appear

### 2. Enable USB Debugging
1. Go to Settings > Developer options
2. Enable "USB debugging"
3. Connect tablet via USB
4. Authorize the computer when prompted

### 3. Install the App
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Set Up Device Owner (for true kiosk mode)

**IMPORTANT:** Device must have NO accounts configured. If it does:
- Factory reset, OR
- Remove all accounts in Settings > Accounts

Then run:
```bash
adb shell dpm set-device-owner com.example.wayfindr/.KioskDeviceAdminReceiver
```

Verify:
```bash
adb shell dpm list-owners
# Should show: com.example.wayfindr/.KioskDeviceAdminReceiver
```

---

## Backend Setup

The app expects an LLM backend at the configured URL (default: `http://192.168.0.100:5000`).

### Required Endpoints

**Chat endpoint:**
```
POST /chat
Content-Type: application/json

Request:  { "message": "Hello" }
Response: { "response": "Hi there!" }
```

**Image endpoint (optional, for camera streaming):**
```
POST /images
Content-Type: application/json

Request: {
  "camera": "front",
  "timestamp": 1703256000000,
  "image": "<base64-jpeg>",
  "format": "jpeg",
  "width": 1920,
  "height": 1080
}
Response: { "status": "ok" }
```

### Sample Flask Backend

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/chat', methods=['POST'])
def chat():
    data = request.json
    message = data.get('message', '')
    # Call your LLM here
    response = f"You said: {message}"
    return jsonify({"response": response})

@app.route('/images', methods=['POST'])
def images():
    data = request.json
    camera = data.get('camera')
    # Process image here
    return jsonify({"status": "ok"})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

---

## App Configuration

### Change LLM Server URL
1. Open app
2. Tap menu (hamburger icon)
3. Select "Set LLM Server URL"
4. Enter your backend URL

### Change Kiosk Password
1. Open app
2. Tap menu
3. Select "Change Kiosk Password"
4. Enter new password (min 4 characters)
5. Default password: `wayfindr`

---

## Testing Kiosk Mode

### With Device Owner (recommended)
1. Complete Device Owner setup above
2. Open app
3. Tap menu > "Start Kiosk Mode"
4. Verify: Home/Back/Recent buttons don't work
5. Tap screen, enter password to exit

### Without Device Owner (fallback)
1. Open app
2. Tap menu > "Start Kiosk Mode"
3. Note: User can swipe to see navigation buttons
4. System UI auto-hides after 3 seconds
5. Tap screen, enter password to exit

---

## Troubleshooting

### App crashes on launch
- Check logcat: `adb logcat -s wayfindr`
- Verify permissions granted

### Speech recognition not working
- Grant microphone permission
- Check internet connection (some devices need it)
- Try speaking closer to the mic

### Camera not showing in kiosk mode
- Grant camera permission
- Check if device has front/rear camera
- View logs for camera errors

### Cannot set Device Owner
- Error: "Device already has owner"
  - Factory reset or remove all accounts
- Error: "Not allowed to..."
  - Device is already managed by MDM

### Remove Device Owner
```bash
adb shell dpm remove-active-admin com.example.wayfindr/.KioskDeviceAdminReceiver
```

---

## Build Variants

| Variant | Use Case |
|---------|----------|
| debug | Development and testing |
| release | Production (not yet configured) |

### Build Debug APK
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK (future)
```bash
./gradlew assembleRelease
# Requires signing configuration
```
