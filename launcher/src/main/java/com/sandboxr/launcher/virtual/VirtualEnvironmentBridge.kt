/*
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.virtual

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.client.stub.StubActivity
import com.sandboxr.virtual.core.VEnvironment
import com.sandboxr.virtual.model.InstalledPackage
import com.sandboxr.virtual.server.am.VActivityManagerService
import com.sandboxr.virtual.server.pm.VPackageManagerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Summary record representing an environment for UI display and filtering.
 */
data class VirtualEnvironmentSummary(
    val id: String,
    val name: String,
    val color: Long,
    val isSystem: Boolean = false,
    val appCount: Int = 0
)

/**
 * Bridge singleton connecting the ported GrapheneOS Launcher3 and Liquid Glass UI
 * to the underlying [VirtualCore] runtime and [VPackageManagerService].
 *
 * Provides thread-safe, coroutine-powered querying of virtual environments,
 * installed guest applications, and launcher intent dispatching.
 */
class VirtualEnvironmentBridge private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "VirtualEnvBridge"

        @Volatile
        private var instance: VirtualEnvironmentBridge? = null

        fun get(context: Context): VirtualEnvironmentBridge {
            return instance ?: synchronized(this) {
                instance ?: VirtualEnvironmentBridge(context.applicationContext ?: context).also {
                    instance = it
                }
            }
        }
    }

    private val vCore: VirtualCore by lazy { VirtualCore.get() }
    private val vpm: VPackageManagerService by lazy { vCore.packageManagerService }
    private val vam: VActivityManagerService by lazy { vCore.activityManagerService }

    private val _environmentsFlow = MutableStateFlow<List<VirtualEnvironmentSummary>>(emptyList())
    val environmentsFlow: StateFlow<List<VirtualEnvironmentSummary>> = _environmentsFlow.asStateFlow()

    private val _virtualAppsFlow = MutableStateFlow<List<VirtualAppInfo>>(emptyList())
    val virtualAppsFlow: StateFlow<List<VirtualAppInfo>> = _virtualAppsFlow.asStateFlow()

    private val changeListeners = CopyOnWriteArrayList<() -> Unit>()

    init {
        refreshEnvironments()
    }

    fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }

    fun notifyChanged() {
        refreshEnvironments()
        changeListeners.forEach { it.invoke() }
    }

    /**
     * Refreshes the cached environment summaries from VirtualCore.
     */
    fun refreshEnvironments(): List<VirtualEnvironmentSummary> {
        val envs = vCore.getAllEnvironments()
        val summaries = mutableListOf<VirtualEnvironmentSummary>()

        // System environment placeholder (Pinned first per PRD)
        summaries.add(
            VirtualEnvironmentSummary(
                id = "system",
                name = "System",
                color = 0xFF388E3C, // Standard system green
                isSystem = true,
                appCount = 0
            )
        )

        for (env in envs) {
            val count = vpm.getInstalledPackages(0, env.id).size
            summaries.add(
                VirtualEnvironmentSummary(
                    id = env.id,
                    name = env.name,
                    color = env.color,
                    isSystem = false,
                    appCount = count
                )
            )
        }

        _environmentsFlow.value = summaries
        return summaries
    }

    /**
     * Loads all virtual applications installed across all environments on an IO dispatcher.
     */
    suspend fun loadAllVirtualApps(): List<VirtualAppInfo> = withContext(Dispatchers.IO) {
        val allApps = mutableListOf<VirtualAppInfo>()
        val envs = vCore.getAllEnvironments()

        for (env in envs) {
            val envApps = loadAppsForEnvironment(env.id)
            allApps.addAll(envApps)
        }

        _virtualAppsFlow.value = allApps
        allApps
    }

    /**
     * Loads virtual applications installed in a specific environment.
     */
    suspend fun loadAppsForEnvironment(envId: String): List<VirtualAppInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<VirtualAppInfo>()
        val env = vCore.getEnvironment(envId) ?: return@withContext emptyList()
        val packages = vpm.getInstalledPackages(0, envId)

        for (pkg in packages) {
            val packageName = pkg.packageName
            val appInfo = vpm.getApplicationInfo(packageName, 0, envId) ?: continue
            val label = appInfo.loadLabel(appContext.packageManager).toString()

            // Resolve launcher activity
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = packageName
            }
            val activityInfo = vpm.resolveActivity(launchIntent, envId)
            val mainActivity = activityInfo?.name ?: continue

            // Extract base icon and apply environment badge
            val baseIcon = try {
                val drawable = appInfo.loadIcon(appContext.packageManager)
                drawableToBitmap(drawable)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed loading icon for $packageName: ${t.message}")
                null
            }

            val badgedIcon = if (baseIcon != null) {
                VirtualIconBadgeRenderer.createBadgedBitmap(baseIcon, env.color.toInt())
            } else null

            result.add(
                VirtualAppInfo(
                    packageName = packageName,
                    mainActivity = mainActivity,
                    label = label,
                    envId = envId,
                    envName = env.name,
                    envColor = env.color,
                    iconBitmap = badgedIcon,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = pkg.versionCode.toLong()
                )
            )
        }

        result
    }

    /**
     * Launches a virtual application by routing its intent through the StubActivity container.
     */
    fun launchVirtualApp(packageName: String, envId: String): Boolean {
        val success = vCore.launchApp(packageName, envId)
        if (success) {
            Log.i(TAG, "Launched virtual app $packageName in environment $envId")
        } else {
            Log.e(TAG, "Failed to launch virtual app $packageName in environment $envId")
        }
        return success
    }

    /**
     * Checks if a package is installed inside a virtual environment.
     */
    fun isVirtualApp(packageName: String, envId: String): Boolean {
        return vCore.isAppInstalled(packageName, envId)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
