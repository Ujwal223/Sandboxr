package com.sandboxr.virtual.client.hook

import android.content.Context
import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Installs proxy hooks directly into ServiceManager cache to intercept system services.
 */
object ServiceManagerHook {

    private const val TAG = "ServiceManagerHook"
    private val installedHooks = mutableMapOf<String, BinderHook>()

    fun installAll(context: Context) {
        try {
            val isAndroid = System.getProperty("java.vm.name")?.contains("Dalvik", ignoreCase = true) == true
            if (isAndroid) {
                // Unreflect hidden APIs safely using HiddenApiBypass
                HiddenApiBypass.addHiddenApiExemptions("")
            }

            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val sCacheField = serviceManagerClass.getDeclaredField("sCache")
            sCacheField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val sCache = sCacheField.get(null) as MutableMap<String, IBinder>

            // 1. Hook Package Manager
            installServiceHook(
                serviceName = "package",
                interfaceClassName = "android.content.pm.IPackageManager",
                hook = PackageManagerHook(context),
                getServiceMethod = getServiceMethod,
                sCache = sCache
            )

            // 2. Hook Activity Manager
            installServiceHook(
                serviceName = "activity",
                interfaceClassName = "android.app.IActivityManager",
                hook = ActivityManagerHook(context),
                getServiceMethod = getServiceMethod,
                sCache = sCache
            )

            // 3. Hook Window Manager
            installServiceHook(
                serviceName = "window",
                interfaceClassName = "android.view.IWindowManager",
                hook = WindowManagerHook(context),
                getServiceMethod = getServiceMethod,
                sCache = sCache
            )

            // Also update ActivityThread.sPackageManager cache if available
            try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager")
                sPackageManagerField.isAccessible = true
                val pmHook = installedHooks["package"]
                val proxiedPM = pmHook?.proxiedInterface
                if (proxiedPM != null) {
                    sPackageManagerField.set(null, proxiedPM)
                    Log.d(TAG, "Hooked ActivityThread.sPackageManager")
                }
            } catch (e: Throwable) {
                // Non-fatal if not yet initialized
            }

            Log.i(TAG, "All core system service hooks successfully injected.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject ServiceManager hooks", e)
        }
    }

    private fun installServiceHook(
        serviceName: String,
        interfaceClassName: String,
        hook: BinderHook,
        getServiceMethod: java.lang.reflect.Method,
        sCache: MutableMap<String, IBinder>
    ) {
        try {
            val baseBinder = getServiceMethod.invoke(null, serviceName) as? IBinder ?: return
            val interfaceClass = Class.forName(interfaceClassName)
            val hookedBinder = hook.install(baseBinder, interfaceClass)

            sCache[serviceName] = hookedBinder
            installedHooks[serviceName] = hook
            Log.d(TAG, "Installed hook for $serviceName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to hook service $serviceName", e)
        }
    }
}
