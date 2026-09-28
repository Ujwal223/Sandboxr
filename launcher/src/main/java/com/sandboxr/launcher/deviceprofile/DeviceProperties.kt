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

package com.sandboxr.launcher.deviceprofile

import android.graphics.Rect
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.display.LauncherDisplayInfo
import com.sandboxr.launcher.util.WindowBounds
import kotlin.math.max
import kotlin.math.min

/**
 * Immutable configuration flags that describe the device's current windowing context.
 *
 * @property isExternalDisplay         True when the window is on an external (non-default) display.
 * @property transposeLayoutWithOrientation True for phone-like devices where rows/columns swap
 *                                     when rotating.
 * @property isMultiDisplay            True when the device is a foldable or dual-display device.
 * @property isGestureMode             True when gesture navigation is active.
 * @property isWorkspaceItemsLabelHidden True when workspace icon labels are hidden by user pref.
 */
data class DeviceConfiguration(
    val isExternalDisplay: Boolean,
    val transposeLayoutWithOrientation: Boolean,
    val isMultiDisplay: Boolean,
    val isGestureMode: Boolean,
    val isWorkspaceItemsLabelHidden: Boolean,
)

/**
 * Configuration flags related to the taskbar presence.
 *
 * @property isTaskbarPresent True when a taskbar is drawn in-process (large screen or gesture mode).
 */
data class TaskbarConfiguration(val isTaskbarPresent: Boolean)

/**
 * Full set of resolved device/window properties used by [DeviceProfile] to compute all layout
 * metrics in a single pass.
 *
 * Create instances via [DeviceProperties.createDeviceProperties] rather than the primary
 * constructor, which is reserved for testing.
 */
data class DeviceProperties(
    val windowX: Int,
    val windowY: Int,
    val rotationHint: Int,
    val widthPx: Int,
    val heightPx: Int,
    val availableWidthPx: Int,
    val availableHeightPx: Int,
    val aspectRatio: Float,
    val isLargeScreen: Boolean,
    val isPhone: Boolean,
    val isTwoPanels: Boolean,
    val isLandscape: Boolean,
    val insets: Rect,
    val deviceConfiguration: DeviceConfiguration,
    val taskbarConfiguration: TaskbarConfiguration,
) {

    /** Creates a [WindowBounds] from the encoded window dimensions. */
    fun createWindowBounds(): WindowBounds =
        WindowBounds(widthPx, heightPx, availableWidthPx, availableHeightPx, rotationHint)

    companion object {
        /**
         * Constructs a [DeviceProperties] from a [LauncherDisplayInfo] and the active [WindowBounds].
         *
         * @param info                 Live display info snapshot.
         * @param windowBounds         Current window bounds (may differ from display bounds in
         *                             multi-window mode).
         * @param deviceConfiguration  Device-level context flags.
         * @param isTaskbarDrawnInProcess Whether the taskbar is rendered in-process.
         */
        fun createDeviceProperties(
            info: LauncherDisplayInfo,
            windowBounds: WindowBounds,
            deviceConfiguration: DeviceConfiguration,
            isTaskbarDrawnInProcess: Boolean,
        ): DeviceProperties {
            val isLargeScreen = info.isLargeScreen(windowBounds)
            val widthPx = windowBounds.bounds.width()
            val heightPx = windowBounds.bounds.height()
            val availableWidthPx = windowBounds.availableSize.x
            val availableHeightPx = windowBounds.availableSize.y

            return DeviceProperties(
                windowX = windowBounds.bounds.left,
                windowY = windowBounds.bounds.top,
                rotationHint = windowBounds.rotationHint,
                widthPx = widthPx,
                heightPx = heightPx,
                availableWidthPx = availableWidthPx,
                availableHeightPx = availableHeightPx,
                aspectRatio = max(widthPx, heightPx).toFloat() / min(widthPx, heightPx).toFloat(),
                isLargeScreen = isLargeScreen,
                isPhone = !isLargeScreen,
                isTwoPanels = isLargeScreen && deviceConfiguration.isMultiDisplay,
                isLandscape = windowBounds.isLandscape,
                insets = Rect(windowBounds.insets),
                deviceConfiguration = deviceConfiguration,
                taskbarConfiguration = TaskbarConfiguration(
                    isTaskbarPresent = isLargeScreen && isTaskbarDrawnInProcess,
                ),
            )
        }
    }
}
