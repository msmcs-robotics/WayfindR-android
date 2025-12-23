# WayfindR Improvement TODO List

## Priority Legend
- **P0** - Critical / Blocking
- **P1** - High priority, needed for MVP
- **P2** - Medium priority, nice to have
- **P3** - Low priority, future enhancement

---

## Completed Features
- [x] Basic chat interface with LLM backend
- [x] Speech-to-text input
- [x] Text-to-speech output
- [x] Kiosk mode with password protection
- [x] Kiosk mode visual states (4 colors)
- [x] Speech confirmation dialog with countdown
- [x] Camera streaming (single camera with switch capability)
- [x] Delete chat history
- [x] Export chat as Markdown
- [x] Configurable LLM URL
- [x] Configurable kiosk password
- [x] Device Owner setup dialog
- [x] Immersive mode with auto-hide
- [x] Continuous conversation mode (send context to LLM)
- [x] Error handling with retry logic and exponential backoff
- [x] Fixed CameraStreamManager for CameraX single-camera limitation

---

## Recently Completed (Session 2024-12)

### Camera Binding Issue - FIXED
Changed from dual-camera to single-camera model with switch capability.
CameraX only supports one camera at a time per lifecycle owner.

**Changes made:**
- `CameraStreamManager.kt`: New single-camera binding with `bindCameraPreview()` and `switchCamera()`
- `CameraPreviewComposable.kt`: New `KioskCameraPreview` with camera switch button
- `KioskMode.kt`: Updated to use new single-camera API

### Continuous Conversation Mode - COMPLETED
**Implementation:**
- Toggle in navigation drawer for "Continuous Conversation"
- Sends last 10 messages as context to LLM
- Context stored in ViewModel
- Settings persisted in DataStore

**Files modified:** `ChatViewModel.kt`, `LlmService.kt`, `MainActivity.kt`, `SettingsDataStore.kt`

### Error Handling - COMPLETED
**Implementation:**
- Added retry logic with exponential backoff (3 retries, 1-10 second delays)
- Detects retryable errors (timeouts, connection issues, 5xx errors)
- Logs retry attempts for debugging

**Files modified:** `LlmService.kt`

---

## P1 - High Priority (Remaining MVP Items)

### 1. Persist Chat History
**Problem:** Chat history is lost when app restarts.

**Needed:**
- [ ] Save messages to local database (Room) or DataStore
- [ ] Load history on app start
- [ ] Option to clear history

**Suggested approach:** Use Room database with a simple `Message` entity.

**Files:** New `ChatRepository.kt`, `ChatDatabase.kt`, `MessageDao.kt`

### 2. Better Speech Recognition Handling
**Problem:** Speech recognition sometimes fails silently or gives poor results.

**Needed:**
- [ ] Add noise level indicator
- [ ] Show confidence score
- [ ] Allow user to retry if recognition seems wrong
- [ ] Handle "no speech detected" gracefully

**Files:** `SpeechManager.kt`, `KioskMode.kt`

---

## P2 - Medium Priority (Polish)

### 3. Configurable Camera Stream Settings
**Needed:**
- [ ] Adjustable capture interval (1-10 seconds) via settings
- [ ] Image quality selection (low/medium/high)
- [ ] Option to disable camera streaming entirely
- [ ] Show upload progress/status indicator

**Note:** CameraStreamManager already has `setStreamInterval()`, `setImageQuality()`, and `setStreamingEnabled()` - just need UI in settings.

**Files:** `MainActivity.kt` (add settings UI), `SettingsDataStore.kt`

### 4. Network Status Indicator
**Problem:** User doesn't know if they're connected to the LLM server.

**Needed:**
- [ ] Add connection status in app bar (already partially implemented with error state)
- [ ] Ping server periodically to verify connectivity
- [ ] Show latency information
- [ ] Auto-reconnect on network change

**Files:** `MainActivity.kt`, new `NetworkMonitor.kt`

### 5. Improve UI/UX
**Suggestions:**
- [ ] Add haptic feedback on button presses
- [ ] Add sound effects for state changes in kiosk mode
- [ ] Improve message bubble animations
- [ ] Add typing indicator when waiting for LLM (partially done in kiosk mode)
- [ ] Dark mode support (currently uses system default)

### 6. Galaxy Tab A9+ Emulator Skin
**From notes.md:** Need proper emulator skin for testing.

**Action:**
- [ ] Download skin from Samsung developer site
- [ ] Configure AVD with correct resolution (1340x800)
- [ ] Test UI on correct aspect ratio

**Links:** https://developer.samsung.com/galaxy-emulator-skin/galaxy-tab.html

### 7. Backup/Restore Chat History
**From notes.md:** Create proper backup rules.

**Needed:**
- [ ] Implement Android Auto Backup
- [ ] Create proper `backup_rules.xml`
- [ ] Allow user to export/import chat history
- [ ] Google Drive integration for backup

**Files:** `backup_rules.xml`, `data_extraction_rules.xml`

---

## P3 - Low Priority (Future Enhancements)

### 8. Multiple LLM Profiles
**Idea:** Allow saving multiple LLM server configurations.

**Needed:**
- [ ] Profile list in settings
- [ ] Quick switch between profiles
- [ ] Name each profile
- [ ] Different endpoints for different use cases (e.g., general chat vs. specific domain)

### 9. Wake Word Detection
**Idea:** Always-on listening for a wake word like "Hey WayfindR"

**Needed:**
- [ ] Integrate with on-device wake word detection (Vosk, Picovoice, or similar)
- [ ] Battery optimization
- [ ] Privacy considerations
- [ ] Visual indicator when wake word detected

### 10. Scheduled Kiosk Mode
**Idea:** Auto-enable kiosk mode during business hours.

**Needed:**
- [ ] Time picker for start/end times
- [ ] Day of week selection
- [ ] Auto-lock/unlock with notification
- [ ] Override option for admins

### 11. Admin Dashboard / Remote Management
**Idea:** Web dashboard to manage deployed tablets.

**Needed:**
- [ ] Firebase/custom backend integration
- [ ] Remote configuration updates
- [ ] Usage analytics (queries per day, response times)
- [ ] Remote kiosk exit/restart
- [ ] Push notifications for errors

### 12. Accessibility Improvements
**Needed:**
- [ ] TalkBack support
- [ ] High contrast mode
- [ ] Adjustable text sizes
- [ ] Screen reader announcements for state changes

### 13. Localization
**Needed:**
- [ ] Extract all strings to resources (partially done)
- [ ] Support multiple languages
- [ ] RTL layout support
- [ ] Language selection in settings

### 14. Offline Mode
**Idea:** Basic functionality without network.

**Needed:**
- [ ] Cached responses for common queries
- [ ] Queue messages when offline
- [ ] Sync when network returns
- [ ] On-device LLM fallback (if feasible)

---

## Code Quality Improvements

### 15. Refactoring Suggestions
- [ ] Extract dialog composables to separate file (`Dialogs.kt`) - reduce MainActivity size
- [ ] Create `Constants.kt` for magic numbers (timeouts, intervals, etc.)
- [ ] Add KDoc comments to public functions
- [ ] Implement proper dependency injection (Hilt/Koin)
- [ ] Add unit tests for ViewModel
- [ ] Add UI tests for critical flows (kiosk mode entry/exit, chat send)

### 16. Performance Optimizations
- [ ] Lazy load camera when entering kiosk mode (currently initialized eagerly)
- [ ] Optimize image compression for streaming (consider WebP format)
- [ ] Use `remember` for expensive calculations in Compose
- [ ] Profile and fix any UI jank
- [ ] Consider image resize before base64 encoding

### 17. Security Improvements
- [ ] Use encrypted DataStore for password storage
- [ ] Add certificate pinning for API calls
- [ ] Implement proper ProGuard rules for release builds
- [ ] Audit for data leaks (logging sensitive info)
- [ ] Validate server responses

---

## Technical Debt

### 18. Known Issues
- [ ] `setDecorFitsSystemWindows` deprecation warning - migrate to WindowInsets API
- [ ] Camera library 16KB alignment warning (non-critical, already configured)
- [ ] Some duplicate imports in MainActivity - clean up
- [ ] MainActivity is getting large (~1000 lines) - consider splitting

### 19. Dependencies to Update
- [ ] CameraX version (currently 1.3.1) - check for 1.4.x
- [ ] Compose BOM (check for updates)
- [ ] Gradle version
- [ ] Kotlin version

---

## Notes from Development

### What Works Well
- Compose UI is responsive and clean
- Speech recognition is reliable on good hardware
- Kiosk mode animations are smooth
- Settings persistence via DataStore is robust
- Retry logic handles network issues gracefully
- Continuous conversation provides better context to LLM

### What Needs Work
- Chat history persistence (lost on app restart)
- Camera only supports one at a time (CameraX limitation)
- No offline capability
- No analytics/monitoring
- MainActivity is getting large

### Hardware Considerations
- Test on actual Samsung Galaxy Tab A7/A9+
- Physical kiosk enclosure needed for production
- Consider power management for always-on deployment
- External speaker may be needed for noisy environments
- USB-C power recommended for continuous operation

---

## Quick Wins (Easy to Implement)

These can be done in a single session:

1. **Camera settings UI** - Just add menu items to toggle existing functionality
2. **Haptic feedback** - Add `LocalHapticFeedback` to button clicks
3. **Clean up imports** - Android Studio can auto-organize
4. **Extract Dialogs.kt** - Copy-paste existing dialogs to new file
5. **Add more logging** - Help with debugging deployment issues

---

## Backend API Requirements

For the app to work properly, the backend needs these endpoints:

### POST /chat
```json
Request: {
  "message": "user input",
  "context": [  // Optional, for continuous conversation
    {"role": "user", "content": "previous message"},
    {"role": "assistant", "content": "previous response"}
  ]
}
Response: { "response": "LLM output" }
```

### POST /images (Optional, for camera streaming)
```json
Request: {
  "camera": "front|rear",
  "timestamp": 1234567890,
  "image": "<base64-jpeg>",
  "format": "jpeg",
  "width": 1920,
  "height": 1080
}
Response: { "status": "ok" }
```
