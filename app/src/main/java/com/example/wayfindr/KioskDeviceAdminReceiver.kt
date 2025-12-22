package com.example.wayfindr

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin Receiver for Kiosk Mode.
 *
 * This receiver is required for the app to be set as a Device Owner,
 * which enables lock task mode (true kiosk functionality).
 *
 * To set this app as device owner, run this ADB command ONCE on the tablet:
 * adb shell dpm set-device-owner com.example.wayfindr/.KioskDeviceAdminReceiver
 *
 * Note: The device must be in a fresh state (no accounts configured) for this to work.
 * You may need to factory reset the tablet first.
 */
class KioskDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }

    companion object {
        fun getComponentName(context: Context): android.content.ComponentName {
            return android.content.ComponentName(context, KioskDeviceAdminReceiver::class.java)
        }
    }
}
