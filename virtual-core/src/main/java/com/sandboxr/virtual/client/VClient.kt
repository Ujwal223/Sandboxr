package com.sandboxr.virtual.client

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.core.VClassLoader
import com.sandboxr.virtual.core.VContextImpl
import com.sandboxr.virtual.core.VEnvironment
import com.sandboxr.virtual.server.pm.VPackageManagerService
import java.io.File
import java.lang.reflect.Method

/**
 * Guest process client orchestrator managing in-process application binding,
 * DexClassLoader runtime resolution, and userspace component execution.
 */
class VClient private constructor() {

    companion object {
        private const val TAG = "VClient"
        val instance = VClient()
    }

    private var currentGuestApplication: Application? = null

    /**
     * Creates an isolated VClassLoader instance for a guest APK within an environment.
     */
    fun loadGuestDex(
        apkFile: File,
        envDataDir: File,
        parentClassLoader: ClassLoader = VClient::class.java.classLoader!!
    ): VClassLoader {
        require(apkFile.exists()) { "Guest APK does not exist: ${apkFile.absolutePath}" }
        return VClassLoader.create(apkFile, envDataDir, parentClassLoader)
    }

    /**
     * Binds and initializes a guest application inside the current process without system installation.
     */
    fun bindApplication(hostContext: Context, packageName: String, envId: String): Application? {
        val vpm = VPackageManagerService.get(hostContext)
        val vCore = VirtualCore.get()

        val appInfo = vpm.getApplicationInfo(packageName, 0, envId) ?: run {
            Log.e(TAG, "ApplicationInfo not found for $packageName in environment $envId")
            return null
        }

        val apkFile = File(appInfo.sourceDir)
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file missing: ${apkFile.absolutePath}")
            return null
        }

        val env = vCore.getEnvironment(envId) ?: VEnvironment.create(hostContext, envId, "Default", 0L)
        val packageDataDir = env.getPackageDataDir(packageName)

        try {
            val classLoader = loadGuestDex(apkFile, packageDataDir, hostContext.classLoader)
            val vContext = VContextImpl(
                base = hostContext.applicationContext,
                environment = env,
                guestPackageName = packageName,
                guestClassLoader = classLoader,
                guestAppInfo = appInfo
            )

            // Resolve Application class name or fallback to standard android.app.Application
            val appClassName = appInfo.className ?: "android.app.Application"
            val appClass = classLoader.loadClass(appClassName)
            val guestApp = appClass.getDeclaredConstructor().newInstance() as Application
            currentGuestApplication = guestApp

            // Invoke attachBaseContext
            val attachBaseContextMethod: Method = Application::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
            attachBaseContextMethod.isAccessible = true
            attachBaseContextMethod.invoke(guestApp, vContext)

            // Invoke onCreate
            guestApp.onCreate()
            Log.i(TAG, "Successfully initialized guest application: $packageName ($appClassName)")
            return guestApp
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind guest application $packageName", e)
            return null
        }
    }

    /**
     * Instantiates and launches a guest Activity directly in userspace for testing or execution.
     */
    fun launchActivityInUserspace(
        hostContext: Context,
        apkFile: File,
        env: VEnvironment,
        packageName: String,
        activityClassName: String,
        appInfo: ApplicationInfo,
        intent: Intent = Intent(),
        savedInstanceState: Bundle? = null
    ): Activity {
        val packageDataDir = env.getPackageDataDir(packageName)
        val classLoader = loadGuestDex(apkFile, packageDataDir, hostContext.classLoader)
        val vContext = VContextImpl(
            base = hostContext,
            environment = env,
            guestPackageName = packageName,
            guestClassLoader = classLoader,
            guestAppInfo = appInfo
        )

        val activityClass = classLoader.loadClass(activityClassName)
        val activityInstance = activityClass.getDeclaredConstructor().newInstance() as Activity

        // Attach virtual context
        val attachBaseContextMethod = Activity::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
        attachBaseContextMethod.isAccessible = true
        attachBaseContextMethod.invoke(activityInstance, vContext)

        // Set intent
        val setIntentMethod = Activity::class.java.getDeclaredMethod("setIntent", Intent::class.java)
        setIntentMethod.isAccessible = true
        setIntentMethod.invoke(activityInstance, intent)

        // Invoke onCreate
        val onCreateMethod = Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreateMethod.isAccessible = true
        onCreateMethod.invoke(activityInstance, savedInstanceState)

        return activityInstance
    }

    fun getCurrentGuestApplication(): Application? = currentGuestApplication
}
