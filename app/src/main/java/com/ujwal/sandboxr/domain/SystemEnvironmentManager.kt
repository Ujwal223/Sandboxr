package com.ujwal.sandboxr.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.compat.SamsungCompat

/**
 * Execution mode for application launches.
 */
enum class LaunchMode {
    /**
     * Standard Android host OS intent execution.
     * Zero virtualization, zero hooks, zero overhead.
     */
    SYSTEM_PASSTHROUGH,

    /**
     * In-process userspace virtual container execution.
     * VirtualCore stub activity and dynamic proxy redirection.
     */
    VIRTUAL_CONTAINER
}

/**
 * Result of an application launch dispatch.
 */
sealed class LaunchResult {
    data class Success(
        val packageName: String,
        val environmentId: String,
        val mode: LaunchMode,
        val intent: Intent? = null
    ) : LaunchResult()

    data class PackageNotFound(
        val packageName: String,
        val environmentId: String,
        val mode: LaunchMode
    ) : LaunchResult()

    data class Error(
        val packageName: String,
        val environmentId: String,
        val message: String,
        val cause: Throwable? = null
    ) : LaunchResult()
}

/**
 * Basic application metadata for UI rendering.
 */
data class LauncherAppInfo(
    val packageName: String,
    val label: String,
    val environmentId: String,
    val isSystemApp: Boolean
)

/**
 * Manages environment dispatch routing, enforcing strict architectural separation between
 * the unhooked, zero-latency System Environment and isolated Virtual Environments.
 */
class SystemEnvironmentManager(
    private val virtualCoreProvider: () -> VirtualCore? = {
        try { VirtualCore.get() } catch (e: Exception) { null }
    },
    private val virtualLauncher: ((packageName: String, envId: String) -> Boolean)? = null
) {

    /**
     * Checks if a given environment ID represents the unvirtualized System environment.
     */
    fun isSystemEnvironment(environmentId: String): Boolean {
        return environmentId == EnvironmentEntity.SYSTEM_ENV_ID
    }

    /**
     * Checks if an EnvironmentEntity represents the unvirtualized System environment.
     */
    fun isSystemEnvironment(environment: EnvironmentEntity): Boolean {
        return environment.isSystem || environment.id == EnvironmentEntity.SYSTEM_ENV_ID
    }

    /**
     * Launches an application with strict routing:
     * - System Environment: Directs to host Android OS via standard Intent without VClient proxying.
     * - Virtual Environment: Dispatches to VirtualCore in-process container.
     */
    fun launchApp(
        context: Context,
        packageName: String,
        environment: EnvironmentEntity
    ): LaunchResult {
        return if (isSystemEnvironment(environment)) {
            launchHostApp(context, packageName, environment.id)
        } else {
            launchVirtualApp(context, packageName, environment.id)
        }
    }

    private fun launchHostApp(
        context: Context,
        packageName: String,
        environmentId: String
    ): LaunchResult {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
                ?: return LaunchResult.PackageNotFound(
                    packageName = packageName,
                    environmentId = environmentId,
                    mode = LaunchMode.SYSTEM_PASSTHROUGH
                )

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Apply Samsung-specific intent flags to prevent crashes and Multi-Window issues
            SamsungCompat.applyMultiWindowIntentFlags(launchIntent)
            
            context.startActivity(launchIntent)

            LaunchResult.Success(
                packageName = packageName,
                environmentId = environmentId,
                mode = LaunchMode.SYSTEM_PASSTHROUGH,
                intent = launchIntent
            )
        } catch (e: Exception) {
            LaunchResult.Error(
                packageName = packageName,
                environmentId = environmentId,
                message = "Failed to launch host application: ${e.message}",
                cause = e
            )
        }
    }

    private fun launchVirtualApp(
        context: Context,
        packageName: String,
        environmentId: String
    ): LaunchResult {
        return try {
            if (virtualLauncher != null) {
                val launched = virtualLauncher.invoke(packageName, environmentId)
                return if (launched) {
                    LaunchResult.Success(
                        packageName = packageName,
                        environmentId = environmentId,
                        mode = LaunchMode.VIRTUAL_CONTAINER
                    )
                } else {
                    LaunchResult.Error(
                        packageName = packageName,
                        environmentId = environmentId,
                        message = "Virtual launcher rejected package $packageName"
                    )
                }
            }

            val vc = virtualCoreProvider()
                ?: return LaunchResult.Error(
                    packageName = packageName,
                    environmentId = environmentId,
                    message = "VirtualCore runtime not initialized"
                )

            if (!vc.isAppInstalled(packageName, environmentId)) {
                return LaunchResult.PackageNotFound(
                    packageName = packageName,
                    environmentId = environmentId,
                    mode = LaunchMode.VIRTUAL_CONTAINER
                )
            }

            val launched = vc.launchApp(packageName, environmentId)
            if (launched) {
                LaunchResult.Success(
                    packageName = packageName,
                    environmentId = environmentId,
                    mode = LaunchMode.VIRTUAL_CONTAINER
                )
            } else {
                LaunchResult.Error(
                    packageName = packageName,
                    environmentId = environmentId,
                    message = "VirtualCore failed to dispatch stub activity for $packageName"
                )
            }
        } catch (e: Exception) {
            LaunchResult.Error(
                packageName = packageName,
                environmentId = environmentId,
                message = "Virtual launch exception: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Queries launchable applications for the target environment.
     */
    fun getInstalledApps(context: Context, environment: EnvironmentEntity): List<LauncherAppInfo> {
        return if (isSystemEnvironment(environment)) {
            getHostLauncherApps(context)
        } else {
            getVirtualLauncherApps(environment.id)
        }
    }

    private fun getHostLauncherApps(context: Context): List<LauncherAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            val label = ri.loadLabel(pm).toString()
            LauncherAppInfo(
                packageName = pkg,
                label = label,
                environmentId = EnvironmentEntity.SYSTEM_ENV_ID,
                isSystemApp = true
            )
        }
    }

    private fun getVirtualLauncherApps(envId: String): List<LauncherAppInfo> {
        val vc = virtualCoreProvider() ?: return emptyList()
        val installed = vc.packageManagerService.getInstalledPackages(0, envId)
        return installed.map { pkg ->
            LauncherAppInfo(
                packageName = pkg.packageName,
                label = pkg.packageName.substringAfterLast('.'),
                environmentId = envId,
                isSystemApp = false
            )
        }
    }
}
