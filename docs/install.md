# WayfindR Installation Guide

This guide provides step-by-step instructions for installing the WayfindR app on your Android tablet.

## Prerequisites

- Android tablet running Android 11 (API 30) or higher
- USB cable to connect tablet to computer
- WayfindR APK file (see [build.md](build.md) for build instructions)

## Installation Methods

### Method 1: USB Transfer and Manual Installation (Recommended)

This is the easiest method for most users.

#### Step 1: Enable Developer Options on Your Tablet

1. Open **Settings** on your Android tablet
2. Scroll down and tap **About tablet** (or **About device**)
3. Find **Build number**
4. Tap **Build number** 7 times rapidly
5. You'll see a message: "You are now a developer!"

#### Step 2: Enable USB Debugging (Optional, for ADB method)

1. Go back to **Settings**
2. Tap **Developer options** (now visible in Settings menu)
3. Toggle **USB debugging** to ON
4. Confirm the warning dialog

#### Step 3: Connect Tablet to Computer

1. Connect your Android tablet to your Ubuntu computer using a USB cable
2. On the tablet, you'll see a notification about USB connection
3. Swipe down from the top to open the notification panel
4. Tap the **USB** notification
5. Select **File Transfer** or **MTP (Media Transfer Protocol)** mode
   - On some devices this may be called "Transfer files"
   - Do NOT select "Charging only"

#### Step 4: Transfer the APK File

1. On your Ubuntu computer, open the **Files** app (file manager)
2. You should see your tablet appear in the left sidebar under "Devices"
   - It may be named after your tablet model (e.g., "Samsung Galaxy Tab A9+")
3. Click on your tablet in the sidebar to access its storage
4. Navigate to the **Download** folder on your tablet
   - Path is usually: `Internal storage > Download`
5. Open a new file manager window or tab
6. Navigate to the WayfindR APK location on your computer:
   ```
   $HOME/Desktop/WayfindR-android/app/build/outputs/apk/debug/
   ```
7. **Drag and drop** the `WayfindR-debug-v1.0.apk` file into the Download folder on your tablet
8. Wait for the file transfer to complete (64MB file, should take 5-10 seconds)

#### Step 5: Enable Installation from Unknown Sources

Before you can install the APK, you need to allow installations from your file manager:

1. On your tablet, open **Settings**
2. Go to **Security** or **Apps & notifications**
3. Find and tap **Install unknown apps** or **Special app access**
4. Select your file manager app (usually **My Files**, **Files**, or **Downloads**)
5. Toggle **Allow from this source** to ON

**Note:** On some Android versions, this permission is requested during installation (Step 6).

#### Step 6: Install the APK

1. On your tablet, open the **My Files** or **Files** app
2. Navigate to the **Downloads** folder
3. Find and tap on **WayfindR-debug-v1.0.apk**
4. If prompted, allow installation from this source (if you didn't do Step 5)
5. Tap **Install**
6. Wait for the installation to complete (5-10 seconds)
7. Tap **Open** to launch WayfindR, or **Done** to finish

#### Step 7: Launch the App

1. Find the WayfindR app icon in your app drawer
2. Tap to launch
3. Grant any permissions the app requests (camera, storage, etc.)

---

### Method 2: ADB Installation (Advanced)

This method uses the Android Debug Bridge (ADB) to install directly from your computer.

#### Step 1: Install ADB on Ubuntu

```bash
sudo apt update
sudo apt install -y adb
```

#### Step 2: Enable USB Debugging

Follow the instructions in Method 1 (Steps 1-2) to enable Developer Options and USB Debugging.

#### Step 3: Connect Tablet

1. Connect your tablet to your computer via USB
2. On the tablet, you'll see a dialog: **Allow USB debugging?**
3. Check **Always allow from this computer**
4. Tap **OK**

#### Step 4: Verify Connection

```bash
adb devices
```

You should see output like:
```
List of devices attached
ABC123456789    device
```

If you see "unauthorized", check your tablet for the USB debugging permission dialog.

#### Step 5: Install the APK

```bash
cd $HOME/Desktop/WayfindR-android
adb install app/build/outputs/apk/debug/WayfindR-debug-v1.0.apk
```

You'll see:
```
Performing Streamed Install
Success
```

#### Step 6: Launch the App

The app is now installed. Find it in your app drawer or launch it via ADB:

```bash
adb shell am start -n com.example.wayfindr/.MainActivity
```

---

### Method 3: Wireless Transfer

If you don't have a USB cable available:

#### Using Cloud Storage (Google Drive, Dropbox, etc.)

1. Upload `WayfindR-debug-v1.0.apk` to your cloud storage from your computer
2. Open the cloud storage app on your tablet
3. Download the APK file
4. Follow Steps 5-7 from Method 1

#### Using Email

1. Email the APK to yourself as an attachment
2. Open the email on your tablet
3. Download the attachment
4. Follow Steps 5-7 from Method 1

**Note:** Some email providers may block APK files for security reasons.

---

## Updating the App

To install a new version of WayfindR:

### Option 1: Install Over Existing App
1. Follow the same installation steps above
2. You'll see "Do you want to update this app?"
3. Tap **Update**
4. Your app data will be preserved

### Option 2: Clean Install
1. First, uninstall the existing app:
   - Long press the WayfindR icon
   - Tap **Uninstall** or drag to uninstall
   - Confirm
2. Follow the installation steps above

**Warning:** Clean install will delete all app data!

---

## Uninstalling the App

### Via Settings
1. Open **Settings**
2. Go to **Apps** or **Applications**
3. Find and tap **WayfindR**
4. Tap **Uninstall**
5. Confirm

### Via ADB
```bash
adb uninstall com.example.wayfindr
```

---

## Troubleshooting

### "App not installed" Error

**Cause:** Package conflict or corrupted APK

**Solutions:**
1. Uninstall any existing version of WayfindR
2. Re-download/re-transfer the APK file
3. Verify the APK file size is correct (~64MB)
4. Clear the Downloads app cache: Settings > Apps > Downloads > Clear cache

### "Parse error: There was a problem parsing the package"

**Cause:** APK file corrupted during transfer

**Solutions:**
1. Delete the APK from your tablet
2. Re-transfer the file from your computer
3. Verify file integrity on computer before transferring

### "Installation blocked" Message

**Cause:** Installation from unknown sources not enabled

**Solution:**
1. Go to Settings > Security > Install unknown apps
2. Find your file manager app
3. Enable "Allow from this source"

### ADB: "no devices/emulators found"

**Solutions:**
1. Ensure USB debugging is enabled on tablet
2. Accept the "Allow USB debugging" dialog on tablet
3. Try a different USB cable
4. Try a different USB port on your computer
5. Restart ADB server:
   ```bash
   adb kill-server
   adb start-server
   ```

### Tablet Not Appearing in Ubuntu File Manager

**Solutions:**
1. Unlock your tablet screen
2. Swipe down notification panel and change USB mode to "File Transfer"
3. Try disconnecting and reconnecting the USB cable
4. Install `mtpfs` if needed:
   ```bash
   sudo apt install mtpfs
   ```

### App Crashes on Launch

1. Check if your tablet meets minimum requirements (Android 11+)
2. Grant all requested permissions
3. Clear app data: Settings > Apps > WayfindR > Storage > Clear data
4. Reinstall the app

---

## Security Notes

- The debug APK is signed with a debug certificate and should only be used for testing
- For production use, build and install a release APK signed with your own release certificate
- Only install APKs from trusted sources
- Consider disabling "Install unknown apps" after installation for security

---

## Permissions Required

WayfindR requires the following permissions:

- **Camera** - For image capture and streaming features
- **Storage** - For saving images and app data
- **Internet** - For connecting to LLM services (if configured)

You'll be prompted to grant these permissions when you first launch the app or when accessing specific features.

---

## Additional Resources

- Build instructions: [build.md](build.md)
- Project setup: [SETUP.md](SETUP.md)
- For ADB documentation: [Android Debug Bridge Guide](https://developer.android.com/studio/command-line/adb)
