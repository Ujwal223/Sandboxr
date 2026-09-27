package com.sandboxr.launcher.loader

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.sandboxr.launcher.theme.SandboxrColors
import com.sandboxr.launcher.ui.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance App Loader & Package Lifecycle Monitor for the SANDBOXR Launcher.
 * Built on Lawnchair / AOSP Launcher3 architecture using [LauncherApps].
 *
 * Capabilities:
 * - Queries all launcher activities across all user profiles (personal, work, multi-user).
 * - Caches high-resolution application icons with [LruCache].
 * - Dynamically updates via [LauncherApps.Callback] when apps are installed, removed, or changed.
 * - Starts apps with canonical launcher intent flags and bounds.
 * - Detects and facilitates setting SANDBOXR as the default system home app (OneUI Home replacement).
 */
class LawnchairAppLoader(private val context: Context) {

    companion object {
        private const val TAG = "LawnchairAppLoader"
        private const val ICON_CACHE_MAX_ENTRIES = 250
    }

    private val launcherApps: LauncherApps? by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    }

    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(ICON_CACHE_MAX_ENTRIES)
    private var packageCallback: LauncherApps.Callback? = null
    private var onAppsChangedListener: (() -> Unit)? = null

    /**
     * Registers a callback listener that is triggered whenever packages are added,
     * removed, or modified on the host device.
     */
    fun registerPackageListener(onChanged: () -> Unit) {
        this.onAppsChangedListener = onChanged
        val apps = launcherApps ?: return

        if (packageCallback == null) {
            val callback = object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) {
                    Log.d(TAG, "Package added: $packageName on user $user")
                    iconCache.remove(packageName)
                    onAppsChangedListener?.invoke()
                }

                override fun onPackageRemoved(packageName: String, user: UserHandle) {
                    Log.d(TAG, "Package removed: $packageName on user $user")
                    iconCache.remove(packageName)
                    onAppsChangedListener?.invoke()
                }

                override fun onPackageChanged(packageName: String, user: UserHandle) {
                    Log.d(TAG, "Package changed: $packageName on user $user")
                    iconCache.remove(packageName)
                    onAppsChangedListener?.invoke()
                }

                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                    Log.d(TAG, "Packages available: ${packageNames.joinToString()} replacing=$replacing")
                    packageNames.forEach { iconCache.remove(it) }
                    onAppsChangedListener?.invoke()
                }

                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                    Log.d(TAG, "Packages unavailable: ${packageNames.joinToString()}")
                    packageNames.forEach { iconCache.remove(it) }
                    onAppsChangedListener?.invoke()
                }
            }
            try {
                apps.registerCallback(callback, Handler(Looper.getMainLooper()))
                packageCallback = callback
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to register LauncherApps callback: ${e.message}")
            }
        }
    }

    /**
     * Unregisters package change callbacks.
     */
    fun unregisterPackageListener() {
        packageCallback?.let { callback ->
            launcherApps?.unregisterCallback(callback)
            packageCallback = null
        }
        onAppsChangedListener = null
    }

    /**
     * Checks if the given [UserHandle] is an Android Work / Managed Profile.
     */
    fun isWorkProfile(user: UserHandle): Boolean {
        if (user == Process.myUserHandle()) return false
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        try {
            val method = UserManager::class.java.getMethod("isManagedProfile", Int::class.javaPrimitiveType)
            val idMethod = UserHandle::class.java.getMethod("getIdentifier")
            val userId = idMethod.invoke(user) as Int
            return method.invoke(userManager, userId) as Boolean
        } catch (_: Throwable) {}
        try {
            val testDrawable = ColorDrawable(0)
            val badged = context.packageManager.getUserBadgedIcon(testDrawable, user)
            if (badged !== testDrawable) return true
        } catch (_: Throwable) {}
        return true
    }

    /**
     * Returns true if device has an Android Work Profile setup.
     */
    fun hasWorkProfile(): Boolean {
        val apps = launcherApps ?: return false
        val myUser = Process.myUserHandle()
        return try {
            apps.profiles.any { isWorkProfile(it) }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Returns the Work Profile UserHandle, if one exists and is accessible.
     */
    fun getWorkProfileUser(): UserHandle? {
        val apps = launcherApps ?: return null
        val myUser = Process.myUserHandle()
        return try {
            apps.profiles.firstOrNull { isWorkProfile(it) }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Loads system applications installed on the device.
     * When [targetUser] is provided, queries exclusively for that user profile.
     * When [targetUser] is null and work profile exists:
     * - "work_profile_environment" queries exclusively the Work Profile.
     * - "system_default_environment" queries exclusively Personal.
     */
    suspend fun loadAllSystemApps(
        envId: String = "system",
        envColor: Color = SandboxrColors.PrimaryAccent,
        targetUser: UserHandle? = null
    ): List<AppItem> = withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppItem>()
        val selfPackage = context.packageName
        val apps = launcherApps

        if (apps != null) {
            val myUser = Process.myUserHandle()
            val profilesToQuery = when {
                targetUser != null -> listOf(targetUser)
                envId == "work_profile_environment" -> listOfNotNull(getWorkProfileUser())
                hasWorkProfile() && (envId == "system" || envId == "system_default_environment") -> listOf(myUser)
                else -> {
                    try {
                        apps.profiles
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to get profiles (likely Samsung work profile restriction): ${t.message}")
                        listOf(myUser)
                    }
                }
            }

            // Filter profiles to only include accessible ones to avoid Samsung security exceptions
            val accessibleProfiles = profilesToQuery.filter { profile ->
                try {
                    apps.getActivityList(null, profile)
                    true
                } catch (e: Throwable) {
                    Log.w(TAG, "Profile $profile is not accessible, skipping: ${e.message}")
                    false
                }
            }

            val seenPackages = HashSet<String>()

            for (profile in accessibleProfiles) {
                try {
                    val activities = apps.getActivityList(null, profile)
                    for (info in activities) {
                        val pkgName = info.applicationInfo.packageName
                        if (pkgName == selfPackage) continue // Don't show our own launcher in the grid
                        if (!seenPackages.add(pkgName)) continue // Deduplicate across launcher activities

                        val label = try {
                            info.label.toString()
                        } catch (_: Exception) {
                            pkgName
                        }

                        val icon = getOrLoadIcon(pkgName, info)

                        appList.add(
                            AppItem(
                                packageName = pkgName,
                                label = label,
                                iconBitmap = icon,
                                envId = envId,
                                envColor = envColor,
                                isSystemApp = true,
                                userHandle = info.user
                            )
                        )
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Cannot load apps for profile $profile: ${e.message}")
                }
            }
        }

        // Fallback or empty check: use PackageManager.queryIntentActivities
        if (appList.isEmpty() && envId != "work_profile_environment") {
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                val fallbackSeen = HashSet<String>()
                for (ri in resolveInfos) {
                    val pkgName = ri.activityInfo?.packageName ?: continue
                    if (pkgName == selfPackage) continue
                    if (!fallbackSeen.add(pkgName)) continue

                    val label = try {
                        ri.loadLabel(packageManager).toString()
                    } catch (_: Exception) {
                        pkgName
                    }

                    val icon = try {
                        getOrLoadIconFromDrawable(pkgName) { ri.loadIcon(packageManager) }
                    } catch (_: Exception) {
                        null
                    }

                    appList.add(
                        AppItem(
                            packageName = pkgName,
                            label = label,
                            iconBitmap = icon,
                            envId = envId,
                            envColor = envColor,
                            isSystemApp = true,
                            userHandle = Process.myUserHandle()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query apps via PackageManager fallback", e)
            }
        }

        // Distinct by packageName and sort alphabetically case-insensitive
        appList.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    /**
     * Launches a host application using [LauncherApps.startMainActivity] or standard [Intent].
     * Scoped to [targetUser] to ensure Work Profile vs Personal apps launch in the proper profile.
     */
    fun launchSystemApp(
        packageName: String,
        sourceBounds: Rect? = null,
        targetUser: UserHandle? = null
    ): Boolean {
        val apps = launcherApps
        val user = targetUser ?: Process.myUserHandle()
        if (apps != null) {
            try {
                val activities = apps.getActivityList(packageName, user)
                if (activities.isNotEmpty()) {
                    val activity = activities.first()
                    apps.startMainActivity(
                        activity.componentName,
                        activity.user,
                        sourceBounds,
                        null
                    )
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "LauncherApps launch failed for $packageName on user $user, trying Intent fallback", e)
            }
        }

        // Intent fallback
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            sourceBounds?.let { launchIntent.sourceBounds = it }
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app $packageName", e)
            false
        }
    }

    /**
     * Checks if SANDBOXR is currently the user's default Home app.
     */
    fun isDefaultLauncher(): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                }
            }

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val defaultResolve = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            return defaultResolve?.activityInfo?.packageName == context.packageName
        } catch (_: Throwable) {
            return false
        }
    }

    /**
     * Returns an Intent to request setting SANDBOXR as the default launcher.
     */
    fun createDefaultLauncherIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    private fun getOrLoadIcon(packageName: String, info: LauncherActivityInfo): ImageBitmap? {
        val cacheKey = "${info.user.hashCode()}_$packageName"
        iconCache.get(cacheKey)?.let { return it }
        val density = context.resources.displayMetrics.densityDpi
        val drawable = try {
            info.getBadgedIcon(density)
        } catch (_: Exception) {
            try {
                info.getIcon(density)
            } catch (_: Exception) {
                null
            }
        } ?: return null

        val bitmap = drawableToImageBitmap(drawable)
        if (bitmap != null) {
            iconCache.put(cacheKey, bitmap)
        }
        return bitmap
    }

    private fun getOrLoadIconFromDrawable(packageName: String, loader: () -> Drawable): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        val drawable = loader()
        val bitmap = drawableToImageBitmap(drawable)
        if (bitmap != null) {
            iconCache.put(packageName, bitmap)
        }
        return bitmap
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                return drawable.bitmap.asImageBitmap()
            }
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 144
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 144
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Throwable) {
            Log.e(TAG, "Error rasterizing drawable icon to ImageBitmap: ${e.message}")
            null
        }
    }
}
