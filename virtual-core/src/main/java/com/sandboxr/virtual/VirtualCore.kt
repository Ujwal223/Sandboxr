package com.sandboxr.virtual

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sandboxr.virtual.client.hook.ServiceManagerHook
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
        Log.i(TAG, "Initializing VirtualCore engine...")
        // 1. Bypass hidden API restrictions on Android 12-15+ (API 31-36+)
        com.sandboxr.virtual.compat.HiddenApiBypassHelper.exemptAll()

        // 2. Initialize native ShadowHook inline properties and ByteHook PLT hooks
        com.sandboxr.virtual.hardware.NativeHookBridge.init()

        // 3. Initialize OEM hardware compatibility (Samsung Knox / Multi-Window)
        com.sandboxr.virtual.compat.SamsungCompat.init(hostContext)

        // 4. Inject ServiceManager Binder hooks for ActivityManager, PackageManager, WindowManager
        ServiceManagerHook.installAll(hostContext)
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
        return activityManagerService.startActivity(intent, envId)
    }
}
