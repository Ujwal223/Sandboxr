package com.sandboxr.virtual

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sandboxr.virtual.compat.SamsungCompat
import com.sandboxr.virtual.core.VEnvironment
import com.sandboxr.virtual.model.InstalledPackage
import com.sandboxr.virtual.server.am.VActivityManagerService
import com.sandboxr.virtual.server.pm.VPackageManagerService
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Main singleton entry point and lifecycle controller for the SANDBOXR Virtual Core engine.
 */
class VirtualCore private constructor(val hostContext: Context) {

    companion object {
        private const val TAG = "VirtualCore"

        @Volatile
        private var instance: VirtualCore? = null

        /**
         * Initializes the VirtualCore runtime. Must be called in Application.onCreate.
         */
        fun init(context: Context): VirtualCore {
            return instance ?: synchronized(this) {
                instance ?: VirtualCore(context.applicationContext ?: context).also {
                    instance = it
                    it.initializeEngine()
                }
            }
        }

        fun get(): VirtualCore {
            return checkNotNull(instance) { "VirtualCore.init(context) must be called prior to get()" }
        }

        fun resetForTesting() {
            instance = null
        }
    }

    private val environments = ConcurrentHashMap<String, VEnvironment>()
    var currentEnvironmentId: String? = null
        private set

    val packageManagerService: VPackageManagerService by lazy {
        VPackageManagerService.get(hostContext)
    }

    val activityManagerService: VActivityManagerService by lazy {
        VActivityManagerService.get(hostContext)
    }

    private fun initializeEngine() {
        Log.i(TAG, "Initializing VirtualCore engine (host process services)...")

        // 1. Bypass hidden API restrictions on Android 12–17 (API 31–37+)
        // Note: HiddenApiBypass is also called in SandboxrApplication for both
        // host and guest processes; calling it here ensures VirtualCore services
        // that are accessed before Application.onCreate completes are also covered.
        try {
            com.sandboxr.virtual.compat.HiddenApiBypassHelper.exemptAll()
        } catch (t: Throwable) {
            Log.w(TAG, "HiddenApiBypass exemption notice: ${t.message}")
        }

        // 2. Initialize native ShadowHook inline properties and ByteHook PLT hooks
        try {
            com.sandboxr.virtual.hardware.NativeHookBridge.init()
        } catch (t: Throwable) {
            Log.w(TAG, "NativeHookBridge init skipped or fallback active: ${t.message}")
        }

        // 3. Initialize OEM hardware compatibility (Samsung Knox / Multi-Window)
        try {
            com.sandboxr.virtual.compat.SamsungCompat.init(hostContext)
        } catch (t: Throwable) {
            Log.w(TAG, "SamsungCompat init skipped: ${t.message}")
        }

        // NOTE: ServiceManagerHook is intentionally NOT installed here.
        // It is installed from SandboxrApplication.onCreate() with the correct
        // isGuestProcess flag:
        //  - Host main process  → isGuestProcess=false  → hooks skipped (preserves OEM binders)
        //  - Guest subprocess   → isGuestProcess=true   → hooks injected into guest process only
        Log.i(TAG, "VirtualCore engine initialized successfully.")
    }

    /**
     * Creates and registers a new isolated user environment.
     */
    fun createEnvironment(id: String, name: String, color: Long): VEnvironment {
        val env = VEnvironment.create(hostContext, id, name, color)
        environments[id] = env
        if (currentEnvironmentId == null) {
            currentEnvironmentId = id
        }
        return env
    }

    /**
     * Sets the active environment context.
     */
    fun switchEnvironment(id: String) {
        require(environments.containsKey(id)) { "Environment with id $id does not exist." }
        val previousId = currentEnvironmentId
        currentEnvironmentId = id
        activityManagerService.onEnvironmentSwitched(previousId, id)
    }

    fun getEnvironment(id: String): VEnvironment? = environments[id]

    fun getAllEnvironments(): List<VEnvironment> = environments.values.toList()

    /**
     * Installs an APK into the designated environment.
     */
    fun installPackage(apkFile: File, envId: String): InstalledPackage {
        if (!environments.containsKey(envId)) {
            createEnvironment(envId, "Default", 0L)
        }
        return packageManagerService.installPackage(apkFile, envId)
    }

    /**
     * Clones an already installed host system application into the designated environment.
     */
    fun cloneSystemPackage(packageName: String, envId: String): InstalledPackage {
        if (!environments.containsKey(envId)) {
            createEnvironment(envId, "Default", 0L)
        }
        return packageManagerService.cloneSystemPackage(packageName, envId)
    }

    /**
     * Checks if a guest application is installed in the target environment.
     */
    fun isAppInstalled(packageName: String, envId: String): Boolean {
        return packageManagerService.isPackageInstalled(packageName, envId)
    }

    /**
     * Launches a guest application by redirecting through the StubActivity container.
     */
    fun launchApp(packageName: String, envId: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            `package` = packageName
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        // Apply Samsung-specific intent flags to prevent crashes
        SamsungCompat.applyMultiWindowIntentFlags(intent)
        return activityManagerService.startActivity(intent, envId)
    }
}
