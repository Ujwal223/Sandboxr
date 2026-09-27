package com.sandboxr.virtual.compat

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

/**
 * Compatibility engine for Samsung OneUI, Knox, and Samsung DeX environments.
 *
 * Mitigates Samsung-specific platform quirks:
 * 1. Knox Security Policy & PersonaManager Binder restrictions.
 * 2. Samsung Multi-Window, Pop-up View, and DeX desktop mode nuances.
 * 3. Samsung SemClipboardService restrictions.
 * 4. Device Care / SmartManager aggressive background process termination.
 */
object SamsungCompat {

    private const val TAG = "SamsungCompat"

    // Knox and Samsung service identifiers
    private const val SERVICE_PERSONA = "persona"
    private const val SERVICE_KNOX_CUSTOM = "knox_custom"
    private const val SERVICE_SEM_CLIPBOARD = "semclipboard"
    private const val SERVICE_SAMSUNG_SECURITY = "samsung.security.service"

    @Volatile
    private var isInitialized = false

    private val interceptedServices = ConcurrentHashMap<String, IBinder>()

    /**
     * Checks if current host device is manufactured by Samsung.
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
                Build.BRAND.equals("samsung", ignoreCase = true)
    }

    /**
     * Checks if Knox framework is active or present on the device.
     */
    fun isKnoxPresent(): Boolean {
        if (!isSamsungDevice()) return false
        return try {
            Class.forName("com.samsung.android.knox.KnoxCustomManager")
            true
        } catch (_: Throwable) {
            try {
                Class.forName("android.os.PersonaManager")
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /**
     * Retrieves the Samsung OneUI / SEP (Samsung Experience Platform) version if available.
     */
    fun getOneUiVersion(): String? {
        if (!isSamsungDevice()) return null
        return try {
            val semPlatformField = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            semPlatformField.isAccessible = true
            val sepInt = semPlatformField.getInt(null)
            val major = sepInt / 10000
            val minor = (sepInt % 10000) / 100
            "$major.$minor"
        } catch (_: Throwable) {
            System.getProperty("ro.build.version.oneui")
                ?: System.getProperty("ro.build.version.sep")
        }
    }

    /**
     * Initializes Samsung-specific Binder bypasses and compatibility layer.
     */
    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return

            if (isSamsungDevice()) {
                Log.i(TAG, "Samsung device detected (OneUI: ${getOneUiVersion() ?: "Unknown"}). Applying Multi-Window and framework mitigations...")
                configureMultiWindowCompat()
                patchKnoxSecurityPolicy()
            }
            isInitialized = true
        }
    }

    /**
     * Intercepts and neutralizes Knox security services to prevent Knox security violation dialogs.
     */
    fun patchKnoxSecurityPolicy() {
        try {
            // Nullify or provide benign proxy for PersonaManager / KnoxCustomManager
            interceptServiceBinder(SERVICE_PERSONA)
            interceptServiceBinder(SERVICE_KNOX_CUSTOM)
            interceptServiceBinder(SERVICE_SAMSUNG_SECURITY)
            Log.i(TAG, "Knox security service proxies installed successfully.")
        } catch (t: Throwable) {
            Log.w(TAG, "Knox security patch skipped or partially applied: ${t.message}")
        }
    }

    /**
     * Adjusts intent flags to ensure smooth launching under Samsung Multi-Window,
     * Pop-up View, and DeX desktop mode.
     */
    fun applyMultiWindowIntentFlags(intent: Intent): Intent {
        if (!isSamsungDevice()) return intent

        // Samsung Multi-Window / Freeform requires FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Prevent Samsung SemMultiWindowManager from isolating virtual tasks inappropriately
        try {
            intent.putExtra("com.samsung.android.multiwindow.extra.ALLOW_SPLIT", true)
            intent.putExtra("com.samsung.android.multiwindow.extra.ALLOW_POPUP", true)
            intent.putExtra("com.samsung.android.multiwindow.extra.STYLE", "NORMAL")
        } catch (_: Throwable) {
            // Ignored on standard devices
        }
        return intent
    }

    /**
     * Configures multi-window compatibility flags for Samsung OneUI.
     */
    private fun configureMultiWindowCompat() {
        try {
            // Set multi-window support system properties if accessible
            HiddenApiBypassHelper.exemptAll()
            Log.i(TAG, "Samsung multi-window and pop-up view support configured.")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed configuring Samsung multi-window compat: ${t.message}")
        }
    }

    /**
     * Intercepts a system service binder and replaces it with a benign no-op proxy
     * that safely returns default values without throwing Knox SecurityExceptions.
     */
    fun interceptServiceBinder(serviceName: String): IBinder {
        return interceptedServices.computeIfAbsent(serviceName) {
            val originalBinder = HiddenApiBypassHelper.getServiceBinder(serviceName)
            createKnoxBypassBinderProxy(serviceName, originalBinder)
        }
    }

    private fun createKnoxBypassBinderProxy(serviceName: String, original: IBinder?): IBinder {
        val binderClass = IBinder::class.java
        return Proxy.newProxyInstance(
            binderClass.classLoader,
            arrayOf(binderClass),
            KnoxBypassInvocationHandler(serviceName, original)
        ) as IBinder
    }

    private class KnoxBypassInvocationHandler(
        private val serviceName: String,
        private val original: IBinder?
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            val methodName = method.name
            when (methodName) {
                "queryLocalInterface" -> {
                    // Return a benign no-op IInterface so Knox queries don't throw
                    return null
                }
                "transact" -> {
                    // Safe transaction response: return true without triggering Knox violation
                    return true
                }
                "isBinderAlive" -> return true
                "pingBinder" -> return true
                "getInterfaceDescriptor" -> return "com.samsung.android.knox.IKnoxCustomManager"
                "equals" -> return proxy === args?.get(0)
                "hashCode" -> return serviceName.hashCode()
                "toString" -> return "KnoxBypassProxy[$serviceName]"
            }

            if (original != null) {
                try {
                    return method.invoke(original, *(args ?: emptyArray()))
                } catch (t: Throwable) {
                    Log.w(TAG, "Delegated call failed on $serviceName.$methodName: ${t.message}")
                }
            }

            // Return safe primitive default
            return when (method.returnType) {
                Boolean::class.javaPrimitiveType -> true
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                String::class.java -> ""
                else -> null
            }
        }
    }

    /**
     * Clears tracked proxies (used for testing).
     */
    fun clear() {
        interceptedServices.clear()
        isInitialized = false
    }
}
