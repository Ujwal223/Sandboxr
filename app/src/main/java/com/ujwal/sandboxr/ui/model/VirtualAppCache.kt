package com.ujwal.sandboxr.ui.model

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, memory-efficient in-RAM cache for Virtual Apps.
 * Designed specifically for low-end Android devices running 10+ virtual profiles/sandboxes.
 *
 * Prevents redundant APK zip parsing, disk I/O, and uncompressed bitmap thrashing.
 */
object VirtualAppCache {

    private const val TAG = "VirtualAppCache"

    data class CachedMetadata(
        val label: String,
        val packageName: String,
        val isSystemApp: Boolean,
        val lastModified: Long
    )

    // Calculate dynamic cache budget: max 1/16th of heap or 16MB, whichever is smaller.
    // Safe for 1GB/2GB low-end devices without causing OutOfMemoryError.
    private val maxMemoryBytes = Runtime.getRuntime().maxMemory()
    private val cacheSizeBytes = (maxMemoryBytes / 16).coerceIn(4 * 1024 * 1024, 16 * 1024 * 1024).toInt()

    private val iconCache = object : LruCache<String, ImageBitmap>(cacheSizeBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // ARGB_8888 uses 4 bytes per pixel
            return value.width * value.height * 4
        }
    }

    private val metadataCache = ConcurrentHashMap<String, CachedMetadata>()

    private fun makeKey(envId: String, packageName: String): String = "$envId:$packageName"

    fun getIcon(envId: String, packageName: String): ImageBitmap? {
        return iconCache.get(makeKey(envId, packageName))
    }

    fun putIcon(envId: String, packageName: String, icon: ImageBitmap) {
        iconCache.put(makeKey(envId, packageName), icon)
    }

    fun getMetadata(envId: String, packageName: String): CachedMetadata? {
        return metadataCache[makeKey(envId, packageName)]
    }

    fun putMetadata(envId: String, packageName: String, metadata: CachedMetadata) {
        metadataCache[makeKey(envId, packageName)] = metadata
    }

    /**
     * Resolves and caches an app icon and metadata in a single, zero-redundancy pass.
     */
    fun resolveOrLoad(
        context: Context,
        envId: String,
        packageName: String,
        apkSourceDir: String?
    ): Pair<String, ImageBitmap?> {
        val key = makeKey(envId, packageName)
        val apkFile = apkSourceDir?.let { File(it) }
        val lastModified = if (apkFile != null && apkFile.exists()) apkFile.lastModified() else 0L

        val cachedMeta = metadataCache[key]
        val cachedIcon = iconCache.get(key)

        if (cachedMeta != null && cachedIcon != null && (lastModified == 0L || cachedMeta.lastModified == lastModified)) {
            return Pair(cachedMeta.label, cachedIcon)
        }

        // Need to load from disk / PM
        val pm = context.packageManager
        var resolvedLabel: String = packageName.substringAfterLast('.')
        var resolvedIcon: ImageBitmap? = null

        try {
            if (apkFile != null && apkFile.exists()) {
                val archiveInfo = pm.getPackageArchiveInfo(apkSourceDir, 0)
                archiveInfo?.applicationInfo?.apply {
                    sourceDir = apkSourceDir
                    publicSourceDir = apkSourceDir
                }
                val label = archiveInfo?.applicationInfo?.loadLabel(pm)?.toString()
                if (!label.isNullOrBlank()) {
                    resolvedLabel = label
                }
                val drawable = archiveInfo?.applicationInfo?.loadIcon(pm)
                if (drawable != null) {
                    resolvedIcon = drawableToOptimizedImageBitmap(drawable)
                }
            } else {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    resolvedLabel = appInfo.loadLabel(pm).toString()
                    resolvedIcon = drawableToOptimizedImageBitmap(pm.getApplicationIcon(packageName))
                } catch (_: Exception) {
                    resolvedLabel = packageName.substringAfterLast('.')
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error resolving app info for $key: ${e.message}")
        }

        // Cache results
        metadataCache[key] = CachedMetadata(
            label = resolvedLabel,
            packageName = packageName,
            isSystemApp = false,
            lastModified = lastModified
        )
        if (resolvedIcon != null) {
            iconCache.put(key, resolvedIcon)
        }

        return Pair(resolvedLabel, resolvedIcon ?: cachedIcon)
    }

    /**
     * Converts a [Drawable] into a properly sized, memory-efficient [ImageBitmap].
     * Clamps dimensions to standard launcher icon size (e.g., 96x96 to 144x144 max)
     * so huge assets from third-party APKs don't exhaust low-end device RAM.
     */
    private fun drawableToOptimizedImageBitmap(drawable: Drawable): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val bmp = drawable.bitmap
            // If already modest size, reuse directly
            if (bmp.width <= 144 && bmp.height <= 144) {
                return bmp.asImageBitmap()
            }
        }

        val targetSize = 120 // Optimal resolution for launcher grid without memory bloating
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    /**
     * Invalidates cache when an app is updated or uninstalled.
     */
    fun invalidate(envId: String, packageName: String) {
        val key = makeKey(envId, packageName)
        iconCache.remove(key)
        metadataCache.remove(key)
    }

    /**
     * Invalidates all cached apps for an entire environment (e.g., when purged).
     */
    fun invalidateEnv(envId: String) {
        val prefix = "$envId:"
        val keysToRemove = metadataCache.keys.filter { it.startsWith(prefix) }
        keysToRemove.forEach { key ->
            metadataCache.remove(key)
            iconCache.remove(key)
        }
    }

    fun clear() {
        iconCache.evictAll()
        metadataCache.clear()
    }
}
