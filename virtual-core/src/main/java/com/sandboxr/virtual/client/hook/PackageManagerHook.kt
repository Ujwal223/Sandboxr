package com.sandboxr.virtual.client.hook

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.server.pm.VPackageManagerService
import java.lang.reflect.Method

/**
 * Intercepts calls to IPackageManager to return virtualized package details.
 */
class PackageManagerHook(private val hostContext: Context) : BinderHook("package") {

    override fun onIntercept(method: Method, args: Array<out Any>): HookResult? {
        val vCore = try { VirtualCore.get() } catch (_: Throwable) { return null }
        val currentEnvId = vCore.currentEnvironmentId ?: return null
        val vpm = VPackageManagerService.get(hostContext)
        val isGuest = try {
            com.sandboxr.virtual.server.am.VActivityManagerService.get(hostContext).isGuestExecuting()
        } catch (_: Throwable) { false }

        when (method.name) {
            "getPackageInfo" -> {
                val pkgName = args.getOrNull(0) as? String ?: return null
                val flags = (args.getOrNull(1) as? Number)?.toInt() ?: 0

                // The host launcher package and android framework must never be concealed
                if (pkgName == hostContext.packageName || pkgName == "android") {
                    return null
                }

                if (com.sandboxr.virtual.server.gms.GmsInterceptor.get().shouldBlockPackageQuery(pkgName, currentEnvId)) {
                    return HookResult.handled(null)
                }

                val virtualPkg = vpm.getPackageInfo(pkgName, flags, currentEnvId)
                if (virtualPkg != null) {
                    return HookResult.handled(virtualPkg)
                }

                // Conceal host applications ONLY when running inside a guest application
                if (isGuest) {
                    return HookResult.handled(null)
                }
                // When launcher or host system queries, pass through to system package manager
                return null
            }
            "getApplicationInfo" -> {
                val pkgName = args.getOrNull(0) as? String ?: return null
                val flags = (args.getOrNull(1) as? Number)?.toInt() ?: 0

                // The host launcher package and android framework must never be concealed
                if (pkgName == hostContext.packageName || pkgName == "android") {
                    return null
                }

                if (com.sandboxr.virtual.server.gms.GmsInterceptor.get().shouldBlockPackageQuery(pkgName, currentEnvId)) {
                    return HookResult.handled(null)
                }

                val virtualApp = vpm.getApplicationInfo(pkgName, flags, currentEnvId)
                if (virtualApp != null) {
                    return HookResult.handled(virtualApp)
                }

                // Conceal host applications ONLY when running inside a guest application
                if (isGuest) {
                    return HookResult.handled(null)
                }
                // When launcher or host system queries, pass through to system package manager
                return null
            }
            "getInstalledPackages" -> {
                if (isGuest) {
                    val flags = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                    val list = vpm.getInstalledPackages(flags, currentEnvId)
                    return HookResult.handled(list)
                }
                // Host launcher needs full system installed packages
                return null
            }
            "getInstalledApplications" -> {
                if (isGuest) {
                    val flags = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                    val list = vpm.getInstalledApplications(flags, currentEnvId)
                    return HookResult.handled(list)
                }
                // Host launcher needs full system installed applications
                return null
            }
            "resolveIntent" -> {
                val intent = args.getOrNull(0) as? Intent ?: return null
                val activityInfo = vpm.resolveActivity(intent, currentEnvId)
                if (activityInfo != null) {
                    val resolveInfo = android.content.pm.ResolveInfo().apply {
                        this.activityInfo = activityInfo
                    }
                    return HookResult.handled(resolveInfo)
                }
            }
        }
        return null
    }
}
