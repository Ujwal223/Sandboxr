package com.sandboxr.virtual.server.am

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sandboxr.virtual.client.stub.StubActivity
import com.sandboxr.virtual.server.pm.VPackageManagerService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Metadata record for a running virtual application process.
 */
data class VirtualProcessRecord(
    val packageName: String,
    val envId: String,
    val pid: Int = -1,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Listener for process lifecycle and environment freezing events.
 */
interface ProcessLifecycleListener {
    fun onProcessStarted(packageName: String, envId: String) {}
    fun onProcessKilled(packageName: String, envId: String) {}
    fun onEnvironmentFrozen(envId: String, killedPackages: Set<String>) {}
}

/**
 * Virtual Activity Manager Service managing guest app lifecycle, intent rewrites,
 * and process state.
 *
 * Implements the Inactive Environment App Freezing Engine:
 * When switching away from an environment that has freezeWhenInactive enabled,
 * all background guest processes for that environment are immediately terminated
 * to reclaim RAM and prevent background telemetry or battery drain.
 */
class VActivityManagerService private constructor(private val hostContext: Context) {

    companion object {
        private const val TAG = "VActivityManagerService"

        const val EXTRA_TARGET_INTENT = "com.sandboxr.virtual.EXTRA_TARGET_INTENT"
        const val EXTRA_ENV_ID = "com.sandboxr.virtual.EXTRA_ENV_ID"
        const val EXTRA_TARGET_PKG = "com.sandboxr.virtual.EXTRA_TARGET_PKG"
        const val EXTRA_TARGET_ACTIVITY = "com.sandboxr.virtual.EXTRA_TARGET_ACTIVITY"

        // Android 17 (API 37) non-restarting configuration masks
        // CONFIG_KEYBOARD (0x0010) | CONFIG_KEYBOARD_HIDDEN (0x0020) | CONFIG_NAVIGATION (0x0040) | CONFIG_TOUCHSCREEN (0x0008) | CONFIG_COLOR_MODE (0x4000)
        const val API37_NON_RESTARTING_CONFIG_MASK = 0x0010 or 0x0020 or 0x0040 or 0x0008 or 0x4000

        @Volatile
        private var instance: VActivityManagerService? = null

        fun get(context: Context): VActivityManagerService {
            return instance ?: synchronized(this) {
                instance ?: VActivityManagerService(context.applicationContext ?: context).also { instance = it }
            }
        }

        fun createForTesting(context: Context): VActivityManagerService {
            return VActivityManagerService(context)
        }
    }

    /**
     * Evaluates if configuration change requires activity restart under Android 17 (API 37+).
     * On API 37+, changes to keyboard, keyboardHidden, navigation, touchscreen, and colorMode
     * are forwarded via onConfigurationChanged() rather than restarting the Activity.
     */
    fun shouldRecreateOnConfigChange(diff: Int, targetSdkVersion: Int = 37): Boolean {
        if (targetSdkVersion >= 37 || android.os.Build.VERSION.SDK_INT >= 37) {
            val remainingDiff = diff and API37_NON_RESTARTING_CONFIG_MASK.inv()
            return remainingDiff != 0
        }
        return diff != 0
    }

    /**
     * Handles system configuration changes and dispatches them in-place for API 37 non-restarting changes.
     */
    fun onConfigurationChanged(newConfig: android.content.res.Configuration, diff: Int): Boolean {
        val requiresRecreate = shouldRecreateOnConfigChange(diff)
        if (!requiresRecreate) {
            Log.i(TAG, "Config change (diff=0x${Integer.toHexString(diff)}) forwarded via onConfigurationChanged() without recreation.")
            return false // not recreated
        }
        Log.i(TAG, "Config change (diff=0x${Integer.toHexString(diff)}) requires activity restart.")
        return true // recreated
    }

    private val vpm by lazy { VPackageManagerService.get(hostContext) }

    // Key: envId -> Map<packageName, VirtualProcessRecord>
    private val activeProcesses = ConcurrentHashMap<String, ConcurrentHashMap<String, VirtualProcessRecord>>()

    // Key: envId -> Map<packageName, CopyOnWriteArrayList<WeakReference<Activity>>>
    private val activeActivityInstances = ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<java.lang.ref.WeakReference<android.app.Activity>>>>()

    @Volatile
    private var guestExecutingCount = 0

    fun onGuestActivityResumed() {
        guestExecutingCount++
    }

    fun onGuestActivityPaused() {
        guestExecutingCount = maxOf(0, guestExecutingCount - 1)
    }

    fun isGuestExecuting(): Boolean = guestExecutingCount > 0

    // Key: envId -> freezeWhenInactive boolean (default true)
    private val freezePolicies = ConcurrentHashMap<String, Boolean>()

    private val lifecycleListeners = CopyOnWriteArrayList<ProcessLifecycleListener>()

    fun registerActiveActivity(envId: String, packageName: String, activity: android.app.Activity) {
        val envMap = activeActivityInstances.computeIfAbsent(envId) { ConcurrentHashMap() }
        val list = envMap.computeIfAbsent(packageName) { CopyOnWriteArrayList() }
        list.add(java.lang.ref.WeakReference(activity))
    }

    fun unregisterActiveActivity(envId: String, packageName: String, activity: android.app.Activity) {
        val list = activeActivityInstances[envId]?.get(packageName) ?: return
        list.removeAll { it.get() == null || it.get() === activity }
    }

    /**
     * Configures the freeze-when-inactive policy for an environment.
     */
    fun setFreezeWhenInactive(envId: String, freeze: Boolean) {
        freezePolicies[envId] = freeze
    }

    /**
     * Checks if freeze-when-inactive is enabled for an environment.
     * Default per PRD Section 7.2 is true.
     */
    fun isFreezeWhenInactive(envId: String): Boolean {
        return freezePolicies[envId] ?: true
    }

    fun addLifecycleListener(listener: ProcessLifecycleListener) {
        lifecycleListeners.add(listener)
    }

    fun removeLifecycleListener(listener: ProcessLifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    /**
     * Rewrites an intent targeting a virtualized guest component into a StubActivity container intent.
     */
    fun createStubIntent(originalIntent: Intent, envId: String): Intent? {
        val activityInfo = vpm.resolveActivity(originalIntent, envId) ?: return null

        val stubIntent = Intent(hostContext, StubActivity::class.java).apply {
            putExtra(EXTRA_TARGET_INTENT, originalIntent)
            putExtra(EXTRA_ENV_ID, envId)
            putExtra(EXTRA_TARGET_PKG, activityInfo.packageName)
            putExtra(EXTRA_TARGET_ACTIVITY, activityInfo.name)
            flags = originalIntent.flags or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
        com.sandboxr.virtual.compat.SamsungCompat.applyMultiWindowIntentFlags(stubIntent)
        return stubIntent
    }

    /**
     * Launches a virtual activity by redirecting through StubActivity.
     */
    fun startActivity(intent: Intent, envId: String): Boolean {
        val stubIntent = createStubIntent(intent, envId) ?: return false
        hostContext.startActivity(stubIntent)
        val targetPkg = intent.component?.packageName ?: intent.`package` ?: "unknown"
        recordProcessStart(targetPkg, envId)
        return true
    }

    fun recordProcessStart(packageName: String, envId: String, pid: Int = -1) {
        val envProcesses = activeProcesses.computeIfAbsent(envId) { ConcurrentHashMap() }
        val record = VirtualProcessRecord(
            packageName = packageName,
            envId = envId,
            pid = pid,
            startTime = System.currentTimeMillis()
        )
        envProcesses[packageName] = record
        Log.i(TAG, "Recorded process start: $packageName in environment: $envId (PID=$pid)")
        lifecycleListeners.forEach { it.onProcessStarted(packageName, envId) }
    }

    fun recordProcessStop(packageName: String, envId: String) {
        val envProcesses = activeProcesses[envId] ?: return
        val removed = envProcesses.remove(packageName)
        if (removed != null) {
            Log.i(TAG, "Recorded process stop: $packageName in environment: $envId")
            lifecycleListeners.forEach { it.onProcessKilled(packageName, envId) }
        }
    }

    fun isProcessRunning(packageName: String, envId: String): Boolean {
        return activeProcesses[envId]?.containsKey(packageName) == true
    }

    fun getRunningPackages(envId: String): Set<String> {
        return activeProcesses[envId]?.keys?.toSet() ?: emptySet()
    }

    fun getRunningProcesses(envId: String): List<VirtualProcessRecord> {
        return activeProcesses[envId]?.values?.toList() ?: emptyList()
    }

    /**
     * Kills a specific guest process in an environment.
     */
    fun killProcess(packageName: String, envId: String): Boolean {
        val envProcesses = activeProcesses[envId] ?: return false
        val record = envProcesses.remove(packageName) ?: return false

        // 1. Finish all active Activity containers for this package
        val activities = activeActivityInstances[envId]?.remove(packageName)
        activities?.forEach { ref ->
            try {
                ref.get()?.finish()
            } catch (_: Throwable) {}
        }

        // 2. If a real isolated process PID was assigned, signal it
        if (record.pid > 0 && record.pid != android.os.Process.myPid()) {
            try {
                android.os.Process.killProcess(record.pid)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to kill PID ${record.pid}: ${e.message}")
            }
        }

        Log.i(TAG, "Terminated virtual process: $packageName in environment: $envId")
        lifecycleListeners.forEach { it.onProcessKilled(packageName, envId) }
        return true
    }

    /**
     * Terminates all running processes belonging to a specific environment.
     * Returns the set of package names that were killed.
     */
    fun killAllProcessesForEnvironment(envId: String): Set<String> {
        val envProcesses = activeProcesses[envId] ?: return emptySet()
        val packagesToKill = envProcesses.keys.toSet()

        packagesToKill.forEach { pkg ->
            killProcess(pkg, envId)
        }
        activeProcesses.remove(envId)

        // Clean up any remaining activity references for this environment
        val envActivities = activeActivityInstances.remove(envId)
        envActivities?.values?.forEach { list ->
            list.forEach { ref ->
                try {
                    ref.get()?.finish()
                } catch (_: Throwable) {}
            }
        }

        Log.i(TAG, "Froze/terminated all processes for environment '$envId' (${packagesToKill.size} killed)")
        lifecycleListeners.forEach { it.onEnvironmentFrozen(envId, packagesToKill) }
        return packagesToKill
    }

    /**
     * Lifecycle callback invoked when the user or launcher switches active environments.
     * Automatically enforces freezeWhenInactive policy on the previous environment.
     */
    fun onEnvironmentSwitched(previousEnvId: String?, newEnvId: String) {
        if (previousEnvId == null || previousEnvId == newEnvId) {
            return
        }

        val shouldFreeze = isFreezeWhenInactive(previousEnvId)
        if (shouldFreeze) {
            Log.i(TAG, "Environment switched from '$previousEnvId' to '$newEnvId'. Freezing inactive environment '$previousEnvId'...")
            killAllProcessesForEnvironment(previousEnvId)
        } else {
            Log.i(TAG, "Environment switched from '$previousEnvId' to '$newEnvId'. Freeze disabled for '$previousEnvId'; keeping processes alive.")
        }
    }

    /**
     * Clears all tracking state (used in tests).
     */
    fun clear() {
        activeProcesses.clear()
        activeActivityInstances.clear()
        freezePolicies.clear()
        lifecycleListeners.clear()
    }
}
