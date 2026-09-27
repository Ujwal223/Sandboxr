package com.sandboxr.virtual.server.pm

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.util.Log
import com.sandboxr.virtual.core.VClassLoader
import com.sandboxr.virtual.model.InstalledPackage
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Virtual Package Manager Service managing installed guest applications and component resolution.
 */
class VPackageManagerService private constructor(private val hostContext: Context) {

    companion object {
        private const val TAG = "VPackageManagerService"
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

    init {
        restoreInstalledPackages()
    }

    private fun restoreInstalledPackages() {
        val rootEnvs = File(hostContext.filesDir?.parentFile ?: hostContext.filesDir, "envs")
        if (!rootEnvs.exists() || !rootEnvs.isDirectory) return
        val envDirs = rootEnvs.listFiles() ?: return
        val pm = hostContext.packageManager
        val flags = PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PERMISSIONS

        for (envDir in envDirs) {
            if (!envDir.isDirectory) continue
            val envId = envDir.name
            val appsDir = File(envDir, "apps")
            if (!appsDir.exists() || !appsDir.isDirectory) continue

            val appPkgDirs = appsDir.listFiles() ?: continue
            for (pkgDir in appPkgDirs) {
                if (!pkgDir.isDirectory) continue
                val apkFile = File(pkgDir, "base.apk")
                if (!apkFile.exists()) continue
                // Enforce read-only permissions for Android 14+ DCL compliance on startup
                VClassLoader.ensureFileReadOnly(apkFile)

                // Scan for split APKs and auto-backfill if missing from a previously cloned host app
                val existingSplits = pkgDir.listFiles { file ->
                    file.isFile && file.name.startsWith("split_") && file.name.endsWith(".apk")
                }?.toList() ?: emptyList()
                existingSplits.forEach { VClassLoader.ensureFileReadOnly(it) }
                val splitPaths = existingSplits.map { it.absolutePath }.toMutableList()

                try {
                    val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                        ?: pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    val pkgName = archiveInfo?.packageName ?: pkgDir.name

                    // Auto-sync split APKs from host if this package has splits on host that are missing locally
                    if (splitPaths.isEmpty()) {
                        try {
                            val hostApp = pm.getApplicationInfo(pkgName, 0)
                            hostApp.splitSourceDirs?.forEach { hostSplit ->
                                val src = File(hostSplit)
                                if (src.exists()) {
                                    val dst = File(pkgDir, src.name)
                                    src.copyTo(dst, overwrite = true)
                                    VClassLoader.ensureFileReadOnly(dst)
                                    splitPaths.add(dst.absolutePath)
                                }
                            }
                        } catch (_: Throwable) {}
                    }

                    val packageInfo = try {
                        pm.getPackageInfo(pkgName, flags)
                    } catch (_: Exception) {
                        try {
                            pm.getPackageInfo(pkgName, 0)
                        } catch (_: Exception) {
                            archiveInfo ?: PackageInfo().apply { packageName = pkgName }
                        }
                    }

                    val appInfo = packageInfo.applicationInfo ?: ApplicationInfo()
                    appInfo.sourceDir = apkFile.absolutePath
                    appInfo.publicSourceDir = apkFile.absolutePath
                    if (splitPaths.isNotEmpty()) {
                        appInfo.splitSourceDirs = splitPaths.toTypedArray()
                        appInfo.splitPublicSourceDirs = appInfo.splitSourceDirs
                    }

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
                        installTime = apkFile.lastModified(),
                        applicationInfo = appInfo,
                        packageInfo = packageInfo
                    )

                    val key = makeKey(envId, pkgName)
                    parsedPackages[key] = vPkg
                    installedPackages[key] = installedPkg
                    envPackages.computeIfAbsent(envId) { ConcurrentHashMap() }[pkgName] = installedPkg
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to restore package in $envId from ${apkFile.absolutePath}", e)
                }
            }
        }
    }

    /**
     * Clones an already installed host system application into the designated environment.
     * High-reliability path that does not depend on archive file permission parsing.
     */
    fun cloneSystemPackage(packageName: String, envId: String): InstalledPackage {
        val pm = hostContext.packageManager
        val flags = PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_RECEIVERS

        val packageInfo = try {
            pm.getPackageInfo(packageName, flags)
        } catch (_: Exception) {
            pm.getPackageInfo(packageName, 0)
        }

        val appInfo = packageInfo.applicationInfo ?: hostContext.packageManager.getApplicationInfo(packageName, 0)
        val sourceApk = File(appInfo.sourceDir)
        require(sourceApk.exists()) { "Source APK not found for package $packageName at ${sourceApk.absolutePath}" }

        val envDir = File(hostContext.filesDir?.parentFile ?: hostContext.filesDir, "envs/$envId")
        val appDir = File(envDir, "apps/$packageName").apply { mkdirs() }
        val permanentApk = File(appDir, "base.apk")

        if (sourceApk.canonicalPath != permanentApk.canonicalPath) {
            try {
                if (permanentApk.exists()) {
                    permanentApk.setWritable(true)
                }
                sourceApk.copyTo(permanentApk, overwrite = true)
            } catch (_: Exception) {
                if (permanentApk.exists()) {
                    permanentApk.setWritable(true)
                }
                sourceApk.inputStream().use { input ->
                    permanentApk.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            VClassLoader.ensureFileReadOnly(permanentApk)
        } else {
            VClassLoader.ensureFileReadOnly(permanentApk)
        }

        // Copy all split APKs if present (crucial for modern AAB split bundles like Chrome/Brave)
        val copiedSplitPaths = mutableListOf<String>()
        appInfo.splitSourceDirs?.forEach { splitPath ->
            try {
                val sourceSplit = File(splitPath)
                if (sourceSplit.exists()) {
                    val destSplit = File(appDir, sourceSplit.name)
                    if (sourceSplit.canonicalPath != destSplit.canonicalPath) {
                        if (destSplit.exists()) {
                            destSplit.setWritable(true)
                        }
                        sourceSplit.copyTo(destSplit, overwrite = true)
                    }
                    VClassLoader.ensureFileReadOnly(destSplit)
                    copiedSplitPaths.add(destSplit.absolutePath)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy split APK $splitPath", e)
            }
        }

        val targetAppInfo = ApplicationInfo(appInfo).apply {
            sourceDir = permanentApk.absolutePath
            publicSourceDir = permanentApk.absolutePath
            if (copiedSplitPaths.isNotEmpty()) {
                splitSourceDirs = copiedSplitPaths.toTypedArray()
                splitPublicSourceDirs = splitSourceDirs
            }
        }

        val vPkg = VPackage(
            packageName = packageName,
            versionName = packageInfo.versionName ?: "1.0.0",
            versionCode = packageInfo.longVersionCode,
            apkFile = permanentApk,
            packageInfo = packageInfo,
            applicationInfo = targetAppInfo,
            activities = packageInfo.activities?.toList() ?: emptyList(),
            services = packageInfo.services?.toList() ?: emptyList(),
            providers = packageInfo.providers?.toList() ?: emptyList(),
            requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        )

        val installedPkg = InstalledPackage(
            packageName = packageName,
            versionName = vPkg.versionName,
            versionCode = vPkg.versionCode,
            apkFile = permanentApk,
            envId = envId,
            installTime = System.currentTimeMillis(),
            applicationInfo = targetAppInfo,
            packageInfo = packageInfo
        )

        val key = makeKey(envId, packageName)
        parsedPackages[key] = vPkg
        installedPackages[key] = installedPkg
        envPackages.computeIfAbsent(envId) { ConcurrentHashMap() }[packageName] = installedPkg

        // Ensure package directories exist
        File(envDir, "data/$packageName").mkdirs()

        Log.i(TAG, "Successfully cloned system package $packageName into environment $envId")
        return installedPkg
    }

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

        val rawArchiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: error("Failed to parse APK archive: ${apkFile.absolutePath}")

        val pkgName = rawArchiveInfo.packageName

        // If package is installed on host system (e.g. cloned app), query full verified PackageInfo
        val packageInfo = try {
            pm.getPackageInfo(pkgName, flags)
        } catch (_: Exception) {
            rawArchiveInfo
        }

        // Copy APK to permanent environment storage so it survives staging file cleanup
        val envDir = File(hostContext.filesDir?.parentFile ?: hostContext.filesDir, "envs/$envId")
        val appDir = File(envDir, "apps/$pkgName").apply { mkdirs() }
        val permanentApk = File(appDir, "base.apk")
        if (apkFile.canonicalPath != permanentApk.canonicalPath) {
            if (permanentApk.exists()) {
                permanentApk.setWritable(true)
            }
            apkFile.copyTo(permanentApk, overwrite = true)
        }
        VClassLoader.ensureFileReadOnly(permanentApk)

        val appInfo = packageInfo.applicationInfo ?: ApplicationInfo()
        appInfo.sourceDir = permanentApk.absolutePath
        appInfo.publicSourceDir = permanentApk.absolutePath

        val vPkg = VPackage(
            packageName = pkgName,
            versionName = packageInfo.versionName ?: "1.0.0",
            versionCode = packageInfo.longVersionCode,
            apkFile = permanentApk,
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
            apkFile = permanentApk,
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
        val removed = installedPackages.remove(key) != null
        val envDir = File(hostContext.filesDir?.parentFile ?: hostContext.filesDir, "envs/$envId")
        val appDir = File(envDir, "apps/$packageName")
        if (appDir.exists()) {
            appDir.deleteRecursively()
        }
        val dataDir = File(envDir, "data/$packageName")
        if (dataDir.exists()) {
            dataDir.deleteRecursively()
        }
        return removed
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
     * Falls back gracefully when exported flag isn't populated by archive parsing.
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
                // Prefer explicitly exported launcher activity, but fall back to any
                // activity (including ones parsed without exported=true from APK archive)
                return vPkg.activities.firstOrNull { it.exported }
                    ?: vPkg.activities.firstOrNull()
            }
        }

        // Broad scan across environment packages for matching action
        val prefix = "$envId:"
        for ((key, vPkg) in parsedPackages) {
            if (key.startsWith(prefix)) {
                for (act in vPkg.activities) {
                    if (intent.action == Intent.ACTION_MAIN) {
                        // Accept exported or first available (archive parsing may not set exported)
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
