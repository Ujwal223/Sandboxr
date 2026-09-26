package com.sandboxr.virtual.compat

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Compatibility engine for Xiaomi MIUI and HyperOS environments.
 *
 * Mitigates Xiaomi-specific background restrictions:
 * 1. MIUI / HyperOS Autostart permission requirements.
 * 2. Aggressive SecurityCenter / PowerKeeper background process termination.
 * 3. Custom permissions (Background Pop-up windows, Lock screen display).
 * 4. Deep links to Xiaomi settings activities for 1-tap user authorization.
 */
object XiaomiCompat {

    private const val TAG = "XiaomiCompat"

    // Xiaomi SecurityCenter Components
    private const val PACKAGE_SECURITY_CENTER = "com.miui.securitycenter"
    private const val PACKAGE_POWER_KEEPER = "com.miui.powerkeeper"

    private const val ACTIVITY_AUTOSTART = "com.miui.permcenter.autostart.AutoStartManagementActivity"
    private const val ACTIVITY_PERMISSIONS_EDITOR = "com.miui.permcenter.permissions.PermissionsEditorActivity"
    private const val ACTIVITY_POWER_CONFIG = "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"

    /**
     * Checks if the device is manufactured by Xiaomi (including Redmi and Poco).
     */
    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer == "xiaomi" || brand == "xiaomi" || brand == "redmi" || brand == "poco"
    }

    /**
     * Checks if the ROM is MIUI or HyperOS.
     */
    fun isMiuiOrHyperOs(): Boolean {
        return getRomVersion() != null || isXiaomiDevice()
    }

    /**
     * Inspects the Xiaomi OS version name (HyperOS or MIUI).
     */
    fun getRomVersion(): String? {
        val hyperOsProp = getSystemProperty("ro.mi.os.version.name")
        if (!hyperOsProp.isNullOrEmpty()) {
            return "HyperOS $hyperOsProp"
        }

        val miuiProp = getSystemProperty("ro.miui.ui.version.name")
        if (!miuiProp.isNullOrEmpty()) {
            return "MIUI $miuiProp"
        }

        return null
    }

    /**
     * Checks if the app has battery optimization exemptions on this device.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to query battery optimization status: ${t.message}")
            true
        }
    }

    /**
     * Generates a 1-tap deep link intent to open Xiaomi Autostart settings.
     */
    fun getAutostartSettingsIntent(context: Context): Intent {
        val intent = Intent()
        try {
            intent.component = ComponentName(PACKAGE_SECURITY_CENTER, ACTIVITY_AUTOSTART)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        } catch (_: Throwable) {}

        // Fallback 1: Generic MIUI autostart action
        val fallbackAction = Intent("miui.intent.action.OP_AUTO_START")
        fallbackAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.packageManager.resolveActivity(fallbackAction, 0) != null) {
            return fallbackAction
        }

        // Fallback 2: Application Details Settings
        return getAppDetailsSettingsIntent(context)
    }

    /**
     * Generates a 1-tap deep link intent to open Xiaomi Battery Saver / PowerKeeper settings.
     */
    fun getBatterySaverSettingsIntent(context: Context): Intent {
        val intent = Intent()
        try {
            intent.component = ComponentName(PACKAGE_POWER_KEEPER, ACTIVITY_POWER_CONFIG)
            intent.putExtra("package_name", context.packageName)
            intent.putExtra("package_label", "SANDBOXR")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        } catch (_: Throwable) {}

        // Fallback: System battery optimization settings
        val systemBattery = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        systemBattery.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return systemBattery
    }

    /**
     * Generates a 1-tap deep link intent to open Xiaomi Custom Permissions Editor
     * (Display pop-ups in background, show on lock screen, etc.).
     */
    fun getCustomPermissionsEditorIntent(context: Context): Intent {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        try {
            intent.setClassName(PACKAGE_SECURITY_CENTER, ACTIVITY_PERMISSIONS_EDITOR)
            intent.putExtra("extra_pkgname", context.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        } catch (_: Throwable) {}

        return getAppDetailsSettingsIntent(context)
    }

    /**
     * Fallback standard App Details Settings.
     */
    fun getAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Queries whether user should be prompted with the Xiaomi background optimization notice.
     * Returns true if running on Xiaomi/MIUI/HyperOS and battery optimization is not ignored.
     */
    fun shouldPromptBackgroundExemption(context: Context): Boolean {
        if (!isMiuiOrHyperOs()) return false
        return !isBatteryOptimizationIgnored(context)
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            val result = getMethod.invoke(null, key) as? String
            if (result.isNullOrEmpty()) null else result
        } catch (_: Throwable) {
            System.getProperty(key)
        }
    }
}
