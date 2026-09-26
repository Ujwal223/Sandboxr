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
        val vCore = VirtualCore.get()
        val currentEnvId = vCore.currentEnvironmentId ?: return null
        val vpm = VPackageManagerService.get(hostContext)

        when (method.name) {
            "getPackageInfo" -> {
                val pkgName = args.getOrNull(0) as? String ?: return null
                val flags = (args.getOrNull(1) as? Number)?.toInt() ?: 0
                if (com.sandboxr.virtual.server.gms.GmsInterceptor.get().shouldBlockPackageQuery(pkgName, currentEnvId)) {
                    return HookResult.handled(null)
                }
                val virtualPkg = vpm.getPackageInfo(pkgName, flags, currentEnvId)
                if (virtualPkg != null) {
                    return HookResult.handled(virtualPkg)
                }
                // Conceal host applications from guest probing: only allow "android" framework
                if (pkgName != "android") {
                    return HookResult.handled(null)
                }
            }
            "getApplicationInfo" -> {
                val pkgName = args.getOrNull(0) as? String ?: return null
                val flags = (args.getOrNull(1) as? Number)?.toInt() ?: 0
                if (com.sandboxr.virtual.server.gms.GmsInterceptor.get().shouldBlockPackageQuery(pkgName, currentEnvId)) {
                    return HookResult.handled(null)
                }
                val virtualApp = vpm.getApplicationInfo(pkgName, flags, currentEnvId)
                if (virtualApp != null) {
                    return HookResult.handled(virtualApp)
                }
                // Conceal host applications from guest probing: only allow "android" framework
                if (pkgName != "android") {
                    return HookResult.handled(null)
                }
            }
            "getInstalledPackages" -> {
                val flags = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                val list = vpm.getInstalledPackages(flags, currentEnvId)
                // Always return virtual list (never leak host installed packages when empty)
                return HookResult.handled(list)
            }
            "getInstalledApplications" -> {
                val flags = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                val list = vpm.getInstalledApplications(flags, currentEnvId)
                // Always return virtual list (never leak host installed applications)
                return HookResult.handled(list)
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
