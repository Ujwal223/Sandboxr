package com.sandboxr.virtual.server.pm

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import com.sandboxr.virtual.model.InstalledPackage
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Virtual Package Manager Service managing installed guest applications and component resolution.
 */
class VPackageManagerService private constructor(private val hostContext: Context) {

    companion object {
        @Volatile
        private var instance: VPackageManagerService? = null

        fun get(context: Context): VPackageManagerService {
            return instance ?: synchronized(this) {
                instance ?: VPackageManagerService(context.applicationContext ?: context).also { instance = it }
            }
        }
    }

    // Key: "$envId:$packageName" -> InstalledPackage
    private val installedPackages = ConcurrentHashMap<String, InstalledPackage>()
    // Secondary environment index: envId -> (packageName -> InstalledPackage)
    private val envPackages = ConcurrentHashMap<String, ConcurrentHashMap<String, InstalledPackage>>()
    // Key: "$envId:$packageName" -> VPackage
    private val parsedPackages = ConcurrentHashMap<String, VPackage>()

    private fun makeKey(envId: String, packageName: String) = "$envId:$packageName"

    /**
     * Installs an APK into the designated environment.
     */
    fun installPackage(apkFile: File, envId: String): InstalledPackage {
        require(apkFile.exists()) { "APK file does not exist: ${apkFile.absolutePath}" }

        val pm = hostContext.packageManager
        val flags = PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PERMISSIONS

        val packageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: error("Failed to parse APK archive: ${apkFile.absolutePath}")

        val appInfo = packageInfo.applicationInfo ?: ApplicationInfo()
        appInfo.sourceDir = apkFile.absolutePath
        appInfo.publicSourceDir = apkFile.absolutePath

        val pkgName = packageInfo.packageName
        val vPkg = VPackage(
            packageName = pkgName,
            versionName = packageInfo.versionName ?: "1.0.0",
            versionCode = packageInfo.longVersionCode,
            apkFile = apkFile,
            packageInfo = packageInfo,
            applicationInfo = appInfo,
            activities = packageInfo.activities?.toList() ?: emptyList(),
            services = packageInfo.services?.toList() ?: emptyList(),
            providers = packageInfo.providers?.toList() ?: emptyList(),
            requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        )

        val installedPkg = InstalledPackage(
            packageName = pkgName,
            versionName = vPkg.versionName,
            versionCode = vPkg.versionCode,
            apkFile = apkFile,
            envId = envId,
            installTime = System.currentTimeMillis(),
            applicationInfo = appInfo,
            packageInfo = packageInfo
        )

        val key = makeKey(envId, pkgName)
        parsedPackages[key] = vPkg
        installedPackages[key] = installedPkg
        envPackages.computeIfAbsent(envId) { ConcurrentHashMap() }[pkgName] = installedPkg

        return installedPkg
    }

    /**
     * Registers a pre-parsed PackageInfo directly into the virtual package manager.
     */
    fun registerPackage(packageInfo: PackageInfo, envId: String, apkFile: File = File(packageInfo.applicationInfo?.sourceDir ?: "")): InstalledPackage {
        val appInfo = packageInfo.applicationInfo ?: ApplicationInfo()
        val pkgName = packageInfo.packageName
        val vPkg = VPackage(
            packageName = pkgName,
            versionName = packageInfo.versionName ?: "1.0.0",
            versionCode = packageInfo.longVersionCode,
            apkFile = apkFile,
            packageInfo = packageInfo,
            applicationInfo = appInfo,
            activities = packageInfo.activities?.toList() ?: emptyList(),
            services = packageInfo.services?.toList() ?: emptyList(),
            providers = packageInfo.providers?.toList() ?: emptyList(),
            requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        )

        val installedPkg = InstalledPackage(
            packageName = pkgName,
            versionName = vPkg.versionName,
            versionCode = vPkg.versionCode,
            apkFile = apkFile,
            envId = envId,
            installTime = System.currentTimeMillis(),
            applicationInfo = appInfo,
            packageInfo = packageInfo
        )

        val key = makeKey(envId, pkgName)
        parsedPackages[key] = vPkg
        installedPackages[key] = installedPkg
        envPackages.computeIfAbsent(envId) { ConcurrentHashMap() }[pkgName] = installedPkg

        return installedPkg
    }

    /**
     * Uninstalls a guest package from the designated environment.
     */
    fun uninstallPackage(packageName: String, envId: String): Boolean {
        val key = makeKey(envId, packageName)
        parsedPackages.remove(key)
        envPackages[envId]?.remove(packageName)
        return installedPackages.remove(key) != null
    }

    /**
     * Retrieves PackageInfo for a guest package in the designated environment.
     */
    fun getPackageInfo(packageName: String, flags: Int, envId: String): PackageInfo? {
        if (com.sandboxr.virtual.server.gms.GmsInterceptor.get().shouldBlockPackageQuery(packageName, envId)) {
            return null
        }
        val key = makeKey(envId, packageName)
        return installedPackages[key]?.packageInfo
    }

    /**
     * Retrieves ApplicationInfo for a guest package in the designated environment.
     */
    fun getApplicationInfo(packageName: String, flags: Int, envId: String): ApplicationInfo? {
        if (com.sandboxr.virtual.server.gms.GmsInterceptor.get().shouldBlockPackageQuery(packageName, envId)) {
            return null
        }
        val key = makeKey(envId, packageName)
        return installedPackages[key]?.applicationInfo
    }

    /**
     * Retrieves all installed packages in the designated environment.
     */
    fun getInstalledPackages(flags: Int, envId: String): List<PackageInfo> {
        val packages = envPackages[envId]?.values ?: return emptyList()
        return packages.map { it.packageInfo }
    }

    /**
     * Retrieves all installed applications in the designated environment.
     */
    fun getInstalledApplications(flags: Int, envId: String): List<ApplicationInfo> {
        val packages = envPackages[envId]?.values ?: return emptyList()
        return packages.mapNotNull { it.applicationInfo }
    }

    /**
     * Resolves the target Activity for a given package and class within the environment.
     */
    fun resolveActivity(packageName: String, className: String, envId: String): ActivityInfo? {
        val key = makeKey(envId, packageName)
        val vPkg = parsedPackages[key] ?: return null
        return vPkg.activities.firstOrNull { it.name == className }
    }

    /**
     * Resolves the target Activity for a given Intent within the environment.
     */
    fun resolveActivity(intent: Intent, envId: String): ActivityInfo? {
        val component = intent.component
        if (component != null) {
            return resolveActivity(component.packageName, component.className, envId)
        }

        val targetPkg = intent.`package`
        if (targetPkg != null) {
            val key = makeKey(envId, targetPkg)
            val vPkg = parsedPackages[key] ?: return null
            if (intent.action == Intent.ACTION_MAIN) {
                return vPkg.activities.firstOrNull()
            }
        }

        // Broad scan across environment packages for matching action
        val prefix = "$envId:"
        for ((key, vPkg) in parsedPackages) {
            if (key.startsWith(prefix)) {
                for (act in vPkg.activities) {
                    if (intent.action == Intent.ACTION_MAIN && act.exported) {
                        return act
                    }
                }
            }
        }
        return null
    }

    /**
     * Resolves the target Service for a given Intent within the environment.
     */
    fun resolveService(intent: Intent, envId: String): ServiceInfo? {
        val component = intent.component ?: return null
        val key = makeKey(envId, component.packageName)
        val vPkg = parsedPackages[key] ?: return null
        return vPkg.services.firstOrNull { it.name == component.className }
    }

    /**
     * Checks if a package is installed in the given environment.
     */
    fun isPackageInstalled(packageName: String, envId: String): Boolean {
        return installedPackages.containsKey(makeKey(envId, packageName))
    }
}
