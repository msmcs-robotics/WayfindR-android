# Kiosk Mode Implementation Guide

## Overview

This document covers the various methods to implement a true kiosk mode on Android tablets, specifically targeting the Samsung Galaxy Tab A7.

## Current Implementation

WayfindR uses **Lock Task Mode** with Device Owner privileges, which is the recommended approach for true kiosk functionality.

### How It Works

1. App is set as Device Owner via ADB
2. When kiosk mode is activated, the app calls `startLockTask()`
3. Home, Back, and Recent Apps buttons are disabled
4. Status bar and navigation bar are hidden
5. Only the password dialog can exit kiosk mode

### Setup Command

```bash
adb shell dpm set-device-owner com.example.wayfindr/.KioskDeviceAdminReceiver
```

**Requirements:**
- Device must have no accounts configured (factory fresh or reset)
- USB debugging enabled
- ADB installed on computer

---

## All Kiosk Mode Options for Android

### 1. Lock Task Mode (Device Owner) - **RECOMMENDED**

**What it does:** Pins the app and disables all system navigation.

**Pros:**
- Complete lockdown - no gestures or buttons work
- Official Android API (stable, well-supported)
- No root required
- Works on all Android 5.0+ devices

**Cons:**
- Requires ADB setup (one-time)
- Device must be fresh (no accounts) or factory reset
- Cannot be removed without ADB or factory reset

**Implementation:** Already implemented in WayfindR via `KioskDeviceAdminReceiver` and `enableKioskLockTask()`.

---

### 2. Samsung Knox (Enterprise Only)

**What it does:** Samsung's enterprise MDM solution with kiosk capabilities.

**Pros:**
- Most powerful kiosk solution for Samsung devices
- Can lock hardware buttons, disable USB, control everything
- Remote management capabilities
- No ADB required for setup

**Cons:**
- Requires Knox license ($$$)
- Samsung devices only
- Complex setup and management
- Enterprise-focused, overkill for simple deployments

**How to use:**
1. Get Knox license from Samsung
2. Enroll device in Knox
3. Use Knox Configure or Knox Manage to set kiosk policies

---

### 3. Android Enterprise (Fully Managed Device)

**What it does:** Google's enterprise device management framework.

**Pros:**
- Works with EMM/MDM solutions
- Can set kiosk policies remotely
- Supports multiple device manufacturers

**Cons:**
- Requires MDM solution (many are paid)
- Complex to set up for single devices
- Overkill for small deployments

---

### 4. Screen Pinning (Built-in Android)

**What it does:** Pins a single app to the screen.

**Pros:**
- No setup required
- Built into Android
- No special permissions needed

**Cons:**
- **Not secure** - users can exit by holding Back + Recent Apps
- Requires manual activation each time
- Not suitable for true kiosk use

**How to enable:**
Settings > Security > Screen pinning

---

### 5. Custom ROM / Root Solutions

**What it does:** Modify Android at system level.

**Pros:**
- Complete control over everything
- Can disable any system component

**Cons:**
- Voids warranty
- Security risks
- Complex to implement and maintain
- May break with updates
- Not recommended for production

---

### 6. Launcher Replacement

**What it does:** Replace the home screen with your app.

**Pros:**
- No special setup required
- Works on any device

**Cons:**
- Users can still access settings via notifications
- Back button can exit the app
- Not a true lockdown

**Implementation:** Already partially implemented - manifest declares HOME intent category.

---

### 7. Immersive Mode Only (Current Fallback)

**What it does:** Hides system bars, auto-hides when they appear.

**Pros:**
- Works without any setup
- No permissions required

**Cons:**
- Users can swipe to reveal navigation
- After a few taps, users can exit
- Only suitable for presentations, not true kiosk

**Implementation:** Currently used when app is NOT Device Owner.

---

## Comparison Table

| Method | Security Level | Setup Difficulty | Cost | Best For |
|--------|---------------|------------------|------|----------|
| Lock Task Mode | High | Medium (ADB) | Free | **Our Use Case** |
| Samsung Knox | Highest | Low (GUI) | $$$ | Enterprise Samsung |
| Android Enterprise | High | High | $-$$$ | Enterprise Fleet |
| Screen Pinning | Low | Easy | Free | Quick demos |
| Custom ROM | Highest | Very High | Free | Dev/Research |
| Launcher Replace | Low | Easy | Free | Home use |
| Immersive Mode | Very Low | None | Free | Presentations |

---

## Recommended Approach for WayfindR

### For Development/Testing:
1. Use **Lock Task Mode** (already implemented)
2. Run ADB command once per tablet
3. Test with `isDeviceOwner()` check

### For Production Deployment:
1. Factory reset tablet
2. Install WayfindR APK via ADB
3. Run Device Owner command
4. Configure kiosk password
5. Cover physical buttons with enclosure/case

### Physical Security Recommendations:
- Use a tablet enclosure/kiosk stand
- Cover power button (or disable via Knox if available)
- Cover volume buttons
- Use USB-C port cover or lock
- Consider anti-theft mounting

---

## Troubleshooting

### "Device already has owner" Error
The device has a Google account or other admin. Solutions:
1. Factory reset the device, OR
2. Remove all accounts in Settings > Accounts, OR
3. Run: `adb shell pm clear com.google.android.gms`

### Cannot Exit Kiosk Mode
If you forget the password:
1. Connect via ADB
2. Run: `adb shell dpm remove-active-admin com.example.wayfindr/.KioskDeviceAdminReceiver`
3. Force stop the app

### System UI Keeps Appearing
This happens when NOT in Device Owner mode. The app auto-hides after 3 seconds, but for true lockdown, you need Device Owner setup.

---

## Files Involved

- `KioskDeviceAdminReceiver.kt` - Device admin receiver
- `MainActivity.kt` - Lock task mode control
- `KioskMode.kt` - Kiosk UI and state management
- `res/xml/device_admin.xml` - Admin policies
- `AndroidManifest.xml` - Activity launch mode and receiver registration

---

## Future Improvements

1. **Samsung Knox integration** - For Samsung-specific deployments
2. **Remote kiosk exit** - Via API call from admin panel
3. **Scheduled kiosk mode** - Auto-enable during business hours
4. **Multiple kiosk profiles** - Different lockdown levels
5. **Admin PIN recovery** - Backup unlock mechanism
