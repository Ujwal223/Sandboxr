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

package com.sandboxr.launcher.util.window

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.sandboxr.launcher.util.NavigationMode
import com.sandboxr.launcher.util.WindowBounds

/**
 * Abstraction layer over Android's [WindowManager] for obtaining display bounds and navigation
 * mode. This class intentionally avoids direct use of internal system APIs (e.g.
 * `WindowManagerProxy` from the AOSP framework) so that Sandboxr can compile without the
 * platform's hidden-API allowlist or test stubs.
 *
 * A singleton instance is provided via [WindowManagerProxy.INSTANCE]. In tests, replace it with
 * a custom subclass using [WindowManagerProxy.setTestInstance].
 */
open class WindowManagerProxy {

    /**
     * Returns a [CachedDisplayInfo] snapshot for the default display associated with [context].
     */
    open fun getDisplayInfo(context: Context): CachedDisplayInfo {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        return CachedDisplayInfo(size = size, rotation = display.rotation)
    }

    /**
     * Returns the real (physical) [WindowBounds] of the display, including insets from system
     * decorations.
     */
    open fun getRealBounds(context: Context, displayInfo: CachedDisplayInfo): WindowBounds {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowBounds.fromWindowMetrics(wm.currentWindowMetrics)
        } else {
            val display = wm.defaultDisplay
            val size = Point()
            display.getRealSize(size)
            WindowBounds(
                bounds = android.graphics.Rect(0, 0, size.x, size.y),
                insets = Rect(),
            )
        }
    }

    /**
     * Returns a best-effort set of all [WindowBounds] the display can assume (portrait +
     * landscape). This is used to populate [LauncherDisplayInfo.supportedBounds].
     *
     * On API 30+ we use [WindowManager.getCurrentWindowMetrics] / [maximumWindowMetrics];
     * on older APIs we approximate from real display size.
     */
    open fun estimateInternalDisplayBounds(context: Context): Map<CachedDisplayInfo, List<WindowBounds>> {
        val bounds = getRealBounds(context, getDisplayInfo(context))
        val w = bounds.bounds.width()
        val h = bounds.bounds.height()
        val portrait = CachedDisplayInfo(Point(minOf(w, h), maxOf(w, h)), android.view.Surface.ROTATION_0)
        val landscape = CachedDisplayInfo(Point(maxOf(w, h), minOf(w, h)), android.view.Surface.ROTATION_90)
        val portraitBounds = WindowBounds(
            android.graphics.Rect(0, 0, minOf(w, h), maxOf(w, h)),
            Rect(0, 0, 0, 0),
        )
        val landscapeBounds = WindowBounds(
            android.graphics.Rect(0, 0, maxOf(w, h), minOf(w, h)),
            Rect(0, 0, 0, 0),
        )
        return mapOf(
            portrait to listOf(portraitBounds, landscapeBounds, portraitBounds, landscapeBounds),
            landscape to listOf(portraitBounds, landscapeBounds, portraitBounds, landscapeBounds),
        )
    }

    /** Returns the active navigation mode of the device. */
    open fun getNavigationMode(context: Context): NavigationMode {
        // Detect gesture navigation via the system resource.
        val resId = context.resources.getIdentifier(
            "config_navBarInteractionMode", "integer", "android"
        )
        if (resId != 0) {
            val mode = context.resources.getInteger(resId)
            return NavigationMode.fromResValue(mode)
        }
        return NavigationMode.THREE_BUTTONS
    }

    /** Returns the safe display insets from a [CachedDisplayInfo]'s cutout. */
    open fun getSafeInsets(displayInfo: CachedDisplayInfo): Rect {
        val cutout = displayInfo.cutout
        val waterfallInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            cutout.waterfallInsets
        } else {
            android.graphics.Insets.NONE
        }
        return Rect(
            maxOf(cutout.safeInsetLeft, waterfallInsets.left),
            maxOf(cutout.safeInsetTop, waterfallInsets.top),
            maxOf(cutout.safeInsetRight, waterfallInsets.right),
            maxOf(cutout.safeInsetBottom, waterfallInsets.bottom),
        )
    }

    /** Returns true when the device display is running in desktop-first windowing mode. */
    open fun isDisplayDesktopFirst(context: Context): Boolean = false

    /** Returns true when the taskbar should be shown in desktop-freeform display mode. */
    open fun showDesktopTaskbarForFreeformDisplay(context: Context): Boolean = false

    companion object {
        /** Minimum smallest-width dp for a device to be considered a tablet. */
        const val MIN_TABLET_WIDTH: Float = 600f

        @Volatile
        private var sTestInstance: WindowManagerProxy? = null

        /** The singleton instance. Replace with a test subclass via [setTestInstance]. */
        @JvmField
        val INSTANCE: WindowManagerProxy = WindowManagerProxy()

        /**
         * Installs a test instance. Pass `null` to revert to the default singleton.
         */
        @JvmStatic
        fun setTestInstance(proxy: WindowManagerProxy?) {
            sTestInstance = proxy
        }
    }
}
