/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.display

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.ArrayMap
import android.util.ArraySet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.util.NavigationMode
import com.sandboxr.launcher.util.WindowBounds
import com.sandboxr.launcher.util.window.CachedDisplayInfo
import com.sandboxr.launcher.util.window.WindowManagerProxy
import java.io.PrintWriter
import java.util.Collections
import kotlin.math.min

/**
 * Cached snapshot of display properties used throughout the Sandboxr launcher.
 *
 * Encapsulates the physical display characteristics (rotation, size, cutout), active
 * configuration (density, font scale, navigation mode), and the set of all supported
 * [WindowBounds] that the display can assume across orientations.
 *
 * Instances are created once per display-configuration change and distributed via
 * [DisplayController].
 */
class LauncherDisplayInfo @JvmOverloads constructor(
    /** Application context associated with this display. */
    @JvmField val context: Context,
    wmProxy: WindowManagerProxy = WindowManagerProxy.INSTANCE,
    defaultDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE,
    perDisplayBoundsCache: Map<CachedDisplayInfo, List<WindowBounds>> = emptyMap(),
) {
    // -----------------------------------------------------------------------------------------
    // Display identity
    // -----------------------------------------------------------------------------------------

    private val rawDisplayInfo: CachedDisplayInfo = wmProxy.getDisplayInfo(context)

    /** Display info normalized to [android.view.Surface.ROTATION_0] for stable comparisons. */
    @JvmField
    val normalizedDisplayInfo: CachedDisplayInfo = rawDisplayInfo.normalize()

    /** Current physical rotation of the display. */
    @JvmField
    val rotation: Int = rawDisplayInfo.rotation

    /** Physical display size in the current rotation. */
    @JvmField
    val currentSize: Point = rawDisplayInfo.size

    /** Safe display-cutout insets for the current rotation. */
    @JvmField
    val cutout: Rect = wmProxy.getSafeInsets(rawDisplayInfo)

    // -----------------------------------------------------------------------------------------
    // Configuration properties
    // -----------------------------------------------------------------------------------------

    private val config = context.resources.configuration

    /** Font scale factor from the system configuration. */
    @JvmField
    val fontScale: Float = config.fontScale

    /** Density in dpi from the system configuration. */
    val densityDpi: Int = config.densityDpi

    /** Stable density scale factor used for dp ↔ px conversions that must survive density changes. */
    val stableDensityScaleFactor: Float =
        defaultDensityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT

    /** Portrait-normalized screen size in dp. */
    @JvmField
    val screenSizeDp: PortraitSize = PortraitSize.from(config.screenHeightDp, config.screenWidthDp)

    /** Active navigation mode (gesture, 2-button, or 3-button). */
    val navigationMode: NavigationMode = wmProxy.getNavigationMode(context)

    /** Whether night-mode (dark theme) is currently active. */
    @JvmField
    val isNightModeActive: Boolean = config.isNightModeActive

    // -----------------------------------------------------------------------------------------
    // Window bounds
    // -----------------------------------------------------------------------------------------

    /** Full real-display bounds including system insets, in the current rotation. */
    @JvmField
    val realBounds: WindowBounds = wmProxy.getRealBounds(context, rawDisplayInfo)

    /** All [WindowBounds] that this display can assume across all supported orientations. */
    @JvmField
    val supportedBounds: MutableSet<WindowBounds> = ArraySet()

    /** Mapping from normalized [CachedDisplayInfo] to the list of orientation bounds. */
    val perDisplayBounds = ArrayMap<CachedDisplayInfo, List<WindowBounds>>().apply {
        putAll(perDisplayBoundsCache)
    }

    // -----------------------------------------------------------------------------------------
    // Desktop windowing
    // -----------------------------------------------------------------------------------------

    /** Whether the device is in desktop-first windowing mode. */
    @JvmField
    val isInDesktopFirstMode: Boolean = wmProxy.isDisplayDesktopFirst(context)

    /** Whether the taskbar should display desktop tasks for a freeform display. */
    val showDesktopTaskbarForFreeformDisplay: Boolean =
        wmProxy.showDesktopTaskbarForFreeformDisplay(context)

    // -----------------------------------------------------------------------------------------
    // Initialisation — populate supportedBounds
    // -----------------------------------------------------------------------------------------

    init {
        var cachedBounds = perDisplayBounds[normalizedDisplayInfo]
        if (cachedBounds == null) {
            Log.w(TAG, "normalizedDisplayInfo not in cache; estimating bounds.")
            perDisplayBounds.putAll(wmProxy.estimateInternalDisplayBounds(context))
            cachedBounds = perDisplayBounds[normalizedDisplayInfo]
        }

        if (cachedBounds != null) {
            // Replace the real-rotation slot with our observed realBounds for accuracy.
            val expectedBounds = cachedBounds.getOrNull(rawDisplayInfo.rotation)
            if (realBounds != expectedBounds) {
                val clone = ArrayList(cachedBounds)
                if (rawDisplayInfo.rotation < clone.size) {
                    clone[rawDisplayInfo.rotation] = realBounds
                } else {
                    clone.add(realBounds)
                }
                perDisplayBounds[normalizedDisplayInfo] = clone
            }
        } else {
            supportedBounds.add(realBounds)
        }

        perDisplayBounds.values.forEach { supportedBounds.addAll(it) }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    /** Returns `true` if the given [bounds] represent a large-screen (tablet/foldable) layout. */
    fun isLargeScreen(bounds: WindowBounds): Boolean {
        if (smallestSizeDp(bounds) >= WindowManagerProxy.MIN_TABLET_WIDTH) {
            return true
        }
        val displayId = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.display?.displayId
                } catch (e: UnsupportedOperationException) {
                    (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)
                        ?.defaultDisplay?.displayId
                }
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager)
                    ?.defaultDisplay?.displayId
            }
        } catch (e: Exception) {
            Display.DEFAULT_DISPLAY
        }
        return displayId != null && displayId != Display.DEFAULT_DISPLAY
    }

    /** Returns the smallest dimension of [bounds] converted to dp. */
    fun smallestSizeDp(bounds: WindowBounds): Float =
        dpiFromPx(
            min(bounds.bounds.width(), bounds.bounds.height()).toFloat(),
            densityDpi,
        )

    /** All display infos represented in [perDisplayBounds]. */
    val allDisplays: Set<CachedDisplayInfo>
        get() = Collections.unmodifiableSet(perDisplayBounds.keys)

    /** The list of [WindowBounds] for the current (normalized) display, or `null` if unknown. */
    val currentBounds: List<WindowBounds>?
        get() = perDisplayBounds[normalizedDisplayInfo]

    /**
     * Derives the [InvariantDeviceProfile] device type from the supported bounds set.
     *
     * @return One of [InvariantDeviceProfile.TYPE_PHONE], [TYPE_TABLET], [TYPE_MULTI_DISPLAY], or
     *   [TYPE_DESKTOP].
     */
    @InvariantDeviceProfile.DeviceType
    val deviceType: Int
        get() {
            if (isInDesktopFirstMode) return InvariantDeviceProfile.TYPE_DESKTOP
            var hasPhone = false
            var hasTablet = false
            supportedBounds.forEach {
                if (isLargeScreen(it)) hasTablet = true else hasPhone = true
            }
            return when {
                hasPhone && hasTablet -> InvariantDeviceProfile.TYPE_MULTI_DISPLAY
                hasTablet -> InvariantDeviceProfile.TYPE_TABLET
                else -> InvariantDeviceProfile.TYPE_PHONE
            }
        }

    /** Bitmask of which properties differ between this and [other]. */
    fun diff(other: LauncherDisplayInfo): Int {
        var change = 0
        if (normalizedDisplayInfo != other.normalizedDisplayInfo) change = change or CHANGE_ACTIVE_SCREEN
        if (rotation != other.rotation) change = change or CHANGE_ROTATION
        if (densityDpi != other.densityDpi || fontScale != other.fontScale) change = change or CHANGE_DENSITY
        if (navigationMode != other.navigationMode) change = change or CHANGE_NAVIGATION_MODE
        if (perDisplayBounds != other.perDisplayBounds || supportedBounds != other.supportedBounds) {
            change = change or CHANGE_SUPPORTED_BOUNDS
        }
        if (showDesktopTaskbarForFreeformDisplay != other.showDesktopTaskbarForFreeformDisplay) {
            change = change or CHANGE_SHOW_DESKTOP_FIRST_TASKBAR
        }
        if (isNightModeActive != other.isNightModeActive) change = change or CHANGE_NIGHT_MODE
        return change
    }

    fun dump(pw: PrintWriter) {
        pw.println("  normalizedDisplayInfo=$normalizedDisplayInfo")
        pw.println("  rotation=$rotation")
        pw.println("  fontScale=$fontScale")
        pw.println("  densityDpi=$densityDpi")
        pw.println("  navigationMode=${navigationMode.name}")
        pw.println("  isInDesktopFirstMode=$isInDesktopFirstMode")
        pw.println("  showDesktopFirstTaskbar=$showDesktopTaskbarForFreeformDisplay")
        pw.println("  currentSize=$currentSize")
        perDisplayBounds.forEach { (key, value) -> pw.println("  perDisplayBounds - $key: $value") }
    }

    companion object {
        private const val TAG = "LauncherDisplayInfo"

        const val CHANGE_ACTIVE_SCREEN: Int = 1 shl 0
        const val CHANGE_ROTATION: Int = 1 shl 1
        const val CHANGE_DENSITY: Int = 1 shl 2
        const val CHANGE_SUPPORTED_BOUNDS: Int = 1 shl 3
        const val CHANGE_NAVIGATION_MODE: Int = 1 shl 4
        const val CHANGE_SHOW_DESKTOP_FIRST_TASKBAR: Int = 1 shl 5
        const val CHANGE_NIGHT_MODE: Int = 1 shl 6

        const val CHANGE_ALL: Int = CHANGE_ACTIVE_SCREEN or CHANGE_ROTATION or CHANGE_DENSITY or
            CHANGE_SUPPORTED_BOUNDS or CHANGE_NAVIGATION_MODE or
            CHANGE_SHOW_DESKTOP_FIRST_TASKBAR or CHANGE_NIGHT_MODE

        /**
         * Converts pixels to density-independent pixels using the given [densityDpi].
         */
        @JvmStatic
        fun dpiFromPx(px: Float, densityDpi: Int): Float =
            px / (densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}
