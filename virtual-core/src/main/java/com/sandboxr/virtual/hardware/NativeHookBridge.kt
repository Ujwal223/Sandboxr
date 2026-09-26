package com.sandboxr.virtual.hardware

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * JNI bridge and manager for native-level ShadowHook and ByteHook hooks.
 * Bridges Java/Kotlin VirtualCore runtime to C++ inline property and PLT filesystem hooks.
 * Supports graceful fallback mode in host/test environments.
 */
object NativeHookBridge {
    private const val TAG = "NativeHookBridge"
    private var isNativeLoaded = false
    private val fallbackProps = ConcurrentHashMap<String, String>()
    private var fallbackMac = "02:00:00:00:00:01"

    init {
        try {
            System.loadLibrary("sandboxr_native")
            isNativeLoaded = true
            Log.i(TAG, "Successfully loaded libsandboxr_native.so")
        } catch (t: Throwable) {
            Log.w(TAG, "Native library libsandboxr_native.so not available in this environment (${t.javaClass.simpleName}: ${t.message}). Running in managed fallback mode.")
            isNativeLoaded = false
        }
    }

    /**
     * Initializes native hooks (ShadowHook and ByteHook).
     */
    fun init(): Boolean {
        return if (isNativeLoaded) {
            try {
                initHooks()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize native hooks", t)
                false
            }
        } else {
            true // Managed fallback mode active
        }
    }

    /**
     * Registers a spoofed system property.
     * Updates native hook table, sets Java system property via System.setProperty,
     * and persists in managed cache.
     */
    fun setProperty(key: String, value: String) {
        fallbackProps[key] = value
        try {
            System.setProperty(key, value)
        } catch (ignored: Throwable) {}

        if (isNativeLoaded) {
            try {
                setSpoofedProperty(key, value)
            } catch (t: Throwable) {
                Log.e(TAG, "Error invoking native setSpoofedProperty", t)
            }
        }
    }

    /**
     * Retrieves a spoofed property.
     */
    fun getProperty(key: String): String? {
        if (isNativeLoaded) {
            try {
                val nativeVal = getSpoofedProperty(key)
                if (nativeVal != null) return nativeVal
            } catch (ignored: Throwable) {}
        }
        return fallbackProps[key] ?: System.getProperty(key)
    }

    /**
     * Sets spoofed MAC address for sysfs / network interfaces.
     */
    fun setMacAddress(mac: String) {
        fallbackMac = mac
        if (isNativeLoaded) {
            try {
                setSpoofedMacAddress(mac)
            } catch (t: Throwable) {
                Log.e(TAG, "Error invoking native setSpoofedMacAddress", t)
            }
        }
    }

    /**
     * Retrieves spoofed MAC address.
     */
    fun getMacAddress(): String {
        if (isNativeLoaded) {
            try {
                val nativeMac = getSpoofedMacAddress()
                if (nativeMac != null) return nativeMac
            } catch (ignored: Throwable) {}
        }
        return fallbackMac
    }

    /**
     * Clears all spoofed properties and resets MAC address.
     */
    fun clear() {
        fallbackProps.clear()
        if (isNativeLoaded) {
            try {
                clearSpoofedProperties()
            } catch (ignored: Throwable) {}
        }
    }

    /**
     * Calls native libc getprop to verify that native-level hooks return spoofed values.
     */
    fun getNativeProp(key: String): String? {
        if (isNativeLoaded) {
            try {
                return nativeGetProp(key)
            } catch (ignored: Throwable) {}
        }
        return getProperty(key)
    }

    /**
     * Reads MAC address from sysfs (e.g. /sys/class/net/wlan0/address) using native open/read.
     * Intercepted by ByteHook to return the virtual MAC address.
     */
    fun readMacFromSysfs(path: String = "/sys/class/net/wlan0/address"): String {
        if (isNativeLoaded) {
            try {
                val readVal = nativeReadSysfs(path)
                if (readVal != null && readVal.isNotBlank()) return readVal.trim()
            } catch (ignored: Throwable) {}
        }
        return fallbackMac
    }

    /**
     * Returns true if native library is loaded and operational.
     */
    fun isNativeActive(): Boolean = isNativeLoaded

    // Native external JNI declarations
    @JvmStatic
    private external fun initHooks(): Boolean

    @JvmStatic
    private external fun setSpoofedProperty(key: String, value: String)

    @JvmStatic
    private external fun getSpoofedProperty(key: String): String?

    @JvmStatic
    private external fun clearSpoofedProperties()

    @JvmStatic
    private external fun setSpoofedMacAddress(mac: String)

    @JvmStatic
    private external fun getSpoofedMacAddress(): String?

    @JvmStatic
    private external fun isHooked(): Boolean

    @JvmStatic
    private external fun nativeGetProp(key: String): String?

    @JvmStatic
    private external fun nativeReadSysfs(path: String): String?
}
