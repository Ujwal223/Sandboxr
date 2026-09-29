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

package com.sandboxr.launcher

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.DisplayMetrics
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.deviceprofile.DeviceConfiguration
import com.sandboxr.launcher.deviceprofile.DeviceProperties
import com.sandboxr.launcher.deviceprofile.TaskbarConfiguration
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.COUNT_SIZES
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.INDEX_DEFAULT
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.INDEX_LANDSCAPE
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.INDEX_TWO_PANEL_LANDSCAPE
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.INDEX_TWO_PANEL_PORTRAIT
import com.sandboxr.launcher.deviceprofile.parser.DisplayOption
import com.sandboxr.launcher.display.LauncherDisplayInfo
import com.sandboxr.launcher.responsive.CalculatedCellSpec
import com.sandboxr.launcher.responsive.CalculatedHotseatSpec
import com.sandboxr.launcher.responsive.CalculatedResponsiveSpec
import com.sandboxr.launcher.responsive.HotseatSpecsProvider
import com.sandboxr.launcher.responsive.ResponsiveCellSpecsProvider
import com.sandboxr.launcher.responsive.ResponsiveSpec.Companion.ResponsiveSpecType
import com.sandboxr.launcher.responsive.ResponsiveSpec.DimensionType
import com.sandboxr.launcher.responsive.ResponsiveSpecsProvider
import com.sandboxr.launcher.util.ResourceHelper
import com.sandboxr.launcher.util.WindowBounds
import java.io.PrintWriter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Encapsulates all resolved display, grid, and layout metrics for the current device/window state.
 *
 * Instances are immutable once constructed. A new [DeviceProfile] is created whenever the display
 * configuration changes (rotation, density, window size, navigation mode).
 *
 * Key responsibilities:
 * - Translates [InvariantDeviceProfile] grid settings + [DisplayOption] sizing parameters into
 *   concrete pixel dimensions for every UI region (workspace, hotseat, folders, All Apps).
 * - Adapts to phone, large-screen (tablet), and two-panel (foldable) device types.
 * - Exposes a [OnDeviceProfileChangeListener] interface for components that must respond to
 *   layout changes.
 *
 * ## Creating instances
 * Use [InvariantDeviceProfile.getDeviceProfile] to obtain an appropriately configured instance.
 * The no-arg constructor is reserved for testing/default scenarios.
 */
open class DeviceProfile @VisibleForTesting constructor() {

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Device identity
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Width of the window in pixels. */
    @JvmField var widthPx: Int = 0

    /** Height of the window in pixels. */
    @JvmField var heightPx: Int = 0

    /** Width available to the launcher after deducting system insets. */
    @JvmField var availableWidthPx: Int = 0

    /** Height available to the launcher after deducting system insets. */
    @JvmField var availableHeightPx: Int = 0

    /** True if the device has a large screen (tablet / foldable open). */
    @JvmField var isTablet: Boolean = false

    /** True if the device is a two-panel (dual-screen / folded-open) device. */
    @JvmField var isTwoPanels: Boolean = false

    /** True if the window is in landscape orientation. */
    @JvmField var isLandscape: Boolean = false

    /** True if the launcher is in multi-window (split-screen) mode. */
    @JvmField var isMultiWindowMode: Boolean = false

    /** Active display rotation hint. */
    @JvmField var rotationHint: Int = 0

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Grid metrics
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Number of workspace icon rows. */
    @JvmField var numRows: Int = 5

    /** Number of workspace icon columns. */
    @JvmField var numColumns: Int = 5

    /** Number of hotseat icons visible (not the extended database count). */
    @JvmField var numShownHotseatIcons: Int = 5

    /** Number of folder rows in portrait. */
    @JvmField var numFolderRows: Int = 3

    /** Number of folder columns in portrait. */
    @JvmField var numFolderColumns: Int = 3

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Icon sizing
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Icon drawable size in pixels. */
    @JvmField var iconSizePx: Int = 0

    /** Icon label text size in pixels. */
    @JvmField var iconTextSizePx: Float = 0f

    /** Bitmap size used for caching icon bitmaps. */
    @JvmField var iconBitmapSizePx: Int = 0

    /** Icon drawable size for All Apps drawer items in pixels. */
    @JvmField var allAppsIconSizePx: Int = 0

    /** Label text size for All Apps items in pixels. */
    @JvmField var allAppsIconTextSizePx: Float = 0f

    /** Number of columns shown in All Apps drawer. */
    val numShownAllAppsColumns: Int
        get() = numColumns

    /** Cell height in pixels for All Apps drawer items. */
    val allAppsCellHeightPx: Int
        get() = if (minCellHeightPx > 0) minCellHeightPx else max(80, (allAppsIconSizePx * 1.5f).toInt())

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Cell / workspace sizing
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Minimum workspace cell width in pixels (scalable grids). */
    @JvmField var minCellWidthPx: Int = 0

    /** Minimum workspace cell height in pixels (scalable grids). */
    @JvmField var minCellHeightPx: Int = 0

    /** Horizontal padding on the left/right of the workspace content area. */
    @JvmField var workspacePaddingLeftPx: Int = 0

    /** Right padding of the workspace content area. */
    @JvmField var workspacePaddingRightPx: Int = 0

    /** Top padding of the workspace content area. */
    @JvmField var workspacePaddingTopPx: Int = 0

    /** Bottom padding of the workspace content area. */
    @JvmField var workspacePaddingBottomPx: Int = 0

    /** Horizontal spacing between icons in the workspace. */
    @JvmField var cellLayoutBorderSpacePx: Point = Point(0, 0)

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Hotseat
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Total hotseat bar height in pixels. */
    @JvmField var hotseatBarSizePx: Int = 0

    /** Bottom space below the hotseat in pixels. */
    @JvmField var hotseatBarBottomSpacePx: Int = 0

    /** Space between the QSB and the hotseat icons. */
    @JvmField var hotseatQsbSpacePx: Int = 0

    /** Hotseat cell width in pixels. */
    @JvmField var hotseatCellWidthPx: Int = 0

    /** Space between hotseat icons in pixels. */
    @JvmField var hotseatBorderSpace: Int = 0

    /** Width of QSB search bar in pixels. */
    @JvmField var hotseatQsbWidth: Int = 0

    /** Height of QSB search bar in pixels. */
    @JvmField var hotseatQsbHeight: Int = 0

    /** Whether the QSB is displayed inline alongside hotseat icons (tablets / foldables). */
    @JvmField var isQsbInline: Boolean = false

    /** Whether the current landscape orientation is seascape (rotated 180). */
    @JvmField var isSeascape: Boolean = false

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Taskbar
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Whether the taskbar is drawn in-process for this profile. */
    @JvmField var isTaskbarPresent: Boolean = false

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // System insets
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Current window system insets (status bar, nav bar, cutouts). */
    @JvmField var insets: Rect = Rect()

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Responsive Specs & Device Paddings
    // ─────────────────────────────────────────────────────────────────────────────────────────

    @JvmField var devicePaddings: DevicePaddings? = null

    @JvmField var responsiveWorkspaceWidthSpec: CalculatedResponsiveSpec? = null
    @JvmField var responsiveWorkspaceHeightSpec: CalculatedResponsiveSpec? = null
    @JvmField var responsiveAllAppsWidthSpec: CalculatedResponsiveSpec? = null
    @JvmField var responsiveAllAppsHeightSpec: CalculatedResponsiveSpec? = null
    @JvmField var responsiveFolderWidthSpec: CalculatedResponsiveSpec? = null
    @JvmField var responsiveFolderHeightSpec: CalculatedResponsiveSpec? = null
    @JvmField var responsiveWorkspaceCellSpec: CalculatedCellSpec? = null
    @JvmField var responsiveAllAppsCellSpec: CalculatedCellSpec? = null
    @JvmField var responsiveFolderCellSpec: CalculatedCellSpec? = null
    @JvmField var responsiveHotseatSpec: CalculatedHotseatSpec? = null

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Invariant reference
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * The [InvariantDeviceProfile] that produced this instance.
     * May be null for the default no-arg test instance.
     */
    @JvmField var inv: InvariantDeviceProfile? = null

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Layout helpers
    // ─────────────────────────────────────────────────────────────────────────────────────────

    @JvmField var allAppsProfile: com.sandboxr.launcher.deviceprofile.AllAppsProfile? = null

    open fun getAllAppsProfile(): com.sandboxr.launcher.deviceprofile.AllAppsProfile =
        allAppsProfile ?: com.sandboxr.launcher.deviceprofile.AllAppsProfile(
            cellHeightPx = allAppsCellHeightPx,
            shiftRange = heightPx,
            numShownAllAppsColumns = numShownAllAppsColumns
        )

    /**
     * Returns `true` if the launcher should use a vertical bar layout (side hotseat) instead of
     * the bottom hotseat. This is the case on phones in landscape when not in tablet mode.
     */
    open fun isVerticalBarLayout(): Boolean = isLandscape && !isTablet

    /** Returns whether the current device is in seascape mode. */
    open fun isSeascape(): Boolean = isSeascape

    /** Returns vertical offset for positioning the QSB. */
    open fun getQsbOffsetY(): Int = hotseatBarBottomSpacePx + hotseatQsbSpacePx

    /** Returns padding for the hotseat view. */
    open fun getHotseatLayoutPadding(context: Context): Rect {
        val padding = Rect()
        if (isVerticalBarLayout()) {
            val paddingTop = max(insets.top, 0)
            val paddingBottom = max(insets.bottom, 0)
            if (isSeascape()) {
                padding.set(insets.left, paddingTop, 0, paddingBottom)
            } else {
                padding.set(0, paddingTop, insets.right, paddingBottom)
            }
        } else {
            val sidePadding = max(0, (widthPx - numShownHotseatIcons * hotseatCellWidthPx) / 2)
            padding.set(sidePadding, 0, sidePadding, hotseatBarBottomSpacePx)
        }
        return padding
    }

    // Bubble bar adjustment helpers
    open fun shouldAdjustHotseatForBubbleBar(context: Context, hasBubbles: Boolean): Boolean = false
    open fun shouldAdjustHotseatOrQsbForBubbleBar(context: Context): Boolean = false
    open fun shouldAlignBubbleBarWithHotseat(): Boolean = false
    open fun shouldAlignBubbleBarWithQSB(): Boolean = false
    open fun getHotseatAdjustedTranslation(context: Context, cellX: Int): Float = 0f

    /**
     * Returns the available bounds for an open folder, clipped to safe insets.
     */
    open fun getAbsoluteOpenFolderBounds(): Rect {
        val margin = (iconSizePx * 0.3f).roundToInt()
        return Rect(
            insets.left + margin,
            insets.top + margin,
            widthPx - insets.right - margin,
            heightPx - insets.bottom - hotseatBarSizePx - margin,
        )
    }

    /**
     * Returns the type index (DEFAULT, LANDSCAPE, TWO_PANEL_PORTRAIT, TWO_PANEL_LANDSCAPE)
     * corresponding to the current device state. Used to pick the correct [DisplayOption] slot.
     */
    fun getTypeIndex(): Int = when {
        isTwoPanels && isLandscape -> INDEX_TWO_PANEL_LANDSCAPE
        isTwoPanels -> INDEX_TWO_PANEL_PORTRAIT
        isLandscape -> INDEX_LANDSCAPE
        else -> INDEX_DEFAULT
    }

    /** Updates system insets and recomputes padding-dependent metrics. */
    fun updateInsets(newInsets: Rect) {
        insets.set(newInsets)
        recomputeAfterInsetsChange()
    }

    private fun recomputeAfterInsetsChange() {
        availableWidthPx = widthPx - insets.left - insets.right
        availableHeightPx = heightPx - insets.top - insets.bottom
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Debug
    // ─────────────────────────────────────────────────────────────────────────────────────────

    fun dump(prefix: String, pw: PrintWriter) {
        pw.println("${prefix}DeviceProfile:")
        pw.println("${prefix}  widthPx=$widthPx, heightPx=$heightPx")
        pw.println("${prefix}  isTablet=$isTablet, isTwoPanels=$isTwoPanels, isLandscape=$isLandscape")
        pw.println("${prefix}  numRows=$numRows, numColumns=$numColumns")
        pw.println("${prefix}  iconSizePx=$iconSizePx, iconTextSizePx=$iconTextSizePx")
        pw.println("${prefix}  hotseatBarSizePx=$hotseatBarSizePx, numShownHotseatIcons=$numShownHotseatIcons")
        pw.println("${prefix}  isTaskbarPresent=$isTaskbarPresent")
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Listener
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Callback interface for components that must respond to device-profile changes. */
    fun interface OnDeviceProfileChangeListener {
        /**
         * Invoked whenever the active [DeviceProfile] has changed (orientation, multi-window,
         * density, etc.).
         *
         * @param dp The new [DeviceProfile] instance.
         */
        fun onDeviceProfileChanged(dp: DeviceProfile)
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Companion / factory
    // ─────────────────────────────────────────────────────────────────────────────────────────

    companion object {

        /** A default/empty profile used as a sentinel before real display info is available. */
        @JvmField
        val DEFAULT_DEVICE_PROFILE: DeviceProfile = DeviceProfile()

        /**
         * Scales a dp value to pixels using the given [densityDpi].
         */
        @JvmStatic
        fun pxFromDp(dp: Float, densityDpi: Int): Int =
            (dp * densityDpi / DisplayMetrics.DENSITY_DEFAULT).roundToInt()

        /**
         * Converts pixels to dp using the given [densityDpi].
         */
        @JvmStatic
        fun dpiFromPx(px: Float, densityDpi: Int): Float =
            px / (densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)

        /**
         * Constructs a [DeviceProfile] from context display metrics alone.
         * This is a lightweight factory used when full [InvariantDeviceProfile] is not yet
         * available (e.g. early initialisation, tests).
         */
        @JvmStatic
        fun fromContext(context: Context): DeviceProfile {
            val dm = context.resources.displayMetrics
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val isLandscape = dm.widthPixels > dm.heightPixels
            return DeviceProfile().apply {
                widthPx = dm.widthPixels
                heightPx = dm.heightPixels
                availableWidthPx = dm.widthPixels
                availableHeightPx = dm.heightPixels
                this.isLandscape = isLandscape
                isTablet = dm.widthPixels.coerceAtMost(dm.heightPixels) >=
                    pxFromDp(600f, dm.densityDpi)
                iconSizePx = pxFromDp(54f, dm.densityDpi)
                iconTextSizePx = pxFromDp(13f, dm.densityDpi).toFloat()
                iconBitmapSizePx = iconSizePx
                allAppsIconSizePx = iconSizePx
                allAppsIconTextSizePx = iconTextSizePx
                hotseatBarSizePx = iconSizePx + pxFromDp(48f, dm.densityDpi)
                hotseatBarBottomSpacePx = pxFromDp(48f, dm.densityDpi)
                hotseatQsbSpacePx = pxFromDp(36f, dm.densityDpi)
                hotseatQsbHeight = pxFromDp(48f, dm.densityDpi)
                hotseatBorderSpace = pxFromDp(16f, dm.densityDpi)
                isQsbInline = isTablet
                hotseatQsbWidth = if (isQsbInline) pxFromDp(260f, dm.densityDpi) else (widthPx - pxFromDp(32f, dm.densityDpi))
                numShownHotseatIcons = 5
                hotseatCellWidthPx = widthPx / numShownHotseatIcons
                cellLayoutBorderSpacePx = Point(
                    pxFromDp(16f, dm.densityDpi),
                    pxFromDp(16f, dm.densityDpi),
                )
                workspacePaddingLeftPx = pxFromDp(22f, dm.densityDpi)
                workspacePaddingRightPx = pxFromDp(22f, dm.densityDpi)
                workspacePaddingTopPx = pxFromDp(8f, dm.densityDpi)
                workspacePaddingBottomPx = hotseatBarSizePx + hotseatBarBottomSpacePx
            }
        }

        /**
         * Constructs a [DeviceProfile] from [InvariantDeviceProfile] + [DisplayOption] + [DeviceProperties].
         *
         * This is the primary production factory. The caller is responsible for selecting the
         * best-matching [DisplayOption] for the device and interpolating if necessary.
         */
        @JvmStatic
        @JvmOverloads
        fun build(
            inv: InvariantDeviceProfile,
            displayOption: DisplayOption,
            deviceProperties: DeviceProperties,
            displayInfo: LauncherDisplayInfo,
            context: Context? = null,
        ): DeviceProfile {
            val densityDpi = displayInfo.densityDpi
            val typeIndex = when {
                deviceProperties.isTwoPanels && deviceProperties.isLandscape -> INDEX_TWO_PANEL_LANDSCAPE
                deviceProperties.isTwoPanels -> INDEX_TWO_PANEL_PORTRAIT
                deviceProperties.isLandscape -> INDEX_LANDSCAPE
                else -> INDEX_DEFAULT
            }

            return DeviceProfile().apply {
                this.inv = inv
                widthPx = deviceProperties.widthPx
                heightPx = deviceProperties.heightPx
                availableWidthPx = deviceProperties.availableWidthPx
                availableHeightPx = deviceProperties.availableHeightPx
                isTablet = deviceProperties.isLargeScreen
                isTwoPanels = deviceProperties.isTwoPanels
                isLandscape = deviceProperties.isLandscape
                rotationHint = deviceProperties.rotationHint
                isTaskbarPresent = deviceProperties.taskbarConfiguration.isTaskbarPresent
                insets = Rect(deviceProperties.insets)

                // Grid
                numRows = inv.numRows
                numColumns = inv.numColumns
                numShownHotseatIcons = inv.numDatabaseHotseatIcons

                val folderIdx = typeIndex.coerceAtMost(inv.numFolderRows.size - 1)
                numFolderRows = inv.numFolderRows.getOrElse(folderIdx) { 3 }
                numFolderColumns = inv.numFolderColumns.getOrElse(folderIdx) { 3 }

                // Icon sizing (dp → px)
                val iconDp = displayOption.iconSizes.getOrElse(typeIndex) { 54f }
                iconSizePx = pxFromDp(iconDp, densityDpi)
                iconBitmapSizePx = iconSizePx
                iconTextSizePx = pxFromDp(
                    displayOption.textSizes.getOrElse(typeIndex) { 13f }, densityDpi
                ).toFloat()

                // All-Apps icon sizing
                allAppsIconSizePx = pxFromDp(
                    displayOption.allAppsIconSizes.getOrElse(typeIndex) { iconDp }, densityDpi
                )
                allAppsIconTextSizePx = pxFromDp(
                    displayOption.allAppsIconTextSizes.getOrElse(typeIndex) { 13f }, densityDpi
                ).toFloat()

                // Border spaces
                val borderDp = displayOption.borderSpaces.getOrElse(typeIndex) {
                    PointF(16f, 16f)
                }
                cellLayoutBorderSpacePx = Point(
                    pxFromDp(borderDp.x, densityDpi),
                    pxFromDp(borderDp.y, densityDpi),
                )

                // Horizontal margin
                val marginDp = displayOption.horizontalMargin.getOrElse(typeIndex) { 22f }
                workspacePaddingLeftPx = pxFromDp(marginDp, densityDpi)
                workspacePaddingRightPx = pxFromDp(marginDp, densityDpi)
                workspacePaddingTopPx = pxFromDp(8f, densityDpi)

                // Hotseat
                val hotseatBottomDp = displayOption.hotseatBarBottomSpace.getOrElse(typeIndex) { 48f }
                val hotseatQsbDp = displayOption.hotseatQsbSpace.getOrElse(typeIndex) { 36f }
                hotseatBarBottomSpacePx = pxFromDp(hotseatBottomDp, densityDpi)
                hotseatQsbSpacePx = pxFromDp(hotseatQsbDp, densityDpi)
                hotseatBarSizePx = iconSizePx + hotseatBarBottomSpacePx
                hotseatQsbHeight = pxFromDp(48f, densityDpi)
                hotseatBorderSpace = pxFromDp(16f, densityDpi)
                isQsbInline = isTablet || isTwoPanels
                hotseatQsbWidth = if (isQsbInline) pxFromDp(260f, densityDpi) else (widthPx - pxFromDp(32f, densityDpi))
                workspacePaddingBottomPx = hotseatBarSizePx + hotseatBarBottomSpacePx

                // Cell metrics
                val effectiveCols = if (isLandscape && !isTablet) numRows else numColumns
                hotseatCellWidthPx = max(1, availableWidthPx / max(1, numShownHotseatIcons))

                // Min cell size
                val minCellDp = displayOption.minCellSize.getOrElse(typeIndex) { PointF(0f, 0f) }
                minCellWidthPx = if (minCellDp.x > 0) pxFromDp(minCellDp.x, densityDpi) else 0
                minCellHeightPx = if (minCellDp.y > 0) pxFromDp(minCellDp.y, densityDpi) else 0

                // Responsive Specs and DevicePaddings
                if (context != null) {
                    if (inv.devicePaddingId > 0) {
                        try {
                            devicePaddings = DevicePaddings(context, inv.devicePaddingId)
                        } catch (e: Exception) {
                            // Ignored if resource not found or invalid
                        }
                    }

                    val aspectRatio = availableWidthPx.toFloat() / max(1, availableHeightPx).toFloat()

                    // Workspace specs
                    val wsSpecsId = if (isTwoPanels && inv.workspaceSpecsTwoPanelId > 0) {
                        inv.workspaceSpecsTwoPanelId
                    } else {
                        inv.workspaceSpecsId
                    }
                    if (wsSpecsId > 0) {
                        try {
                            val wsProvider = ResponsiveSpecsProvider.create(
                                ResourceHelper(context, wsSpecsId),
                                ResponsiveSpecType.Workspace
                            )
                            responsiveWorkspaceWidthSpec = wsProvider.getCalculatedSpec(
                                aspectRatio, DimensionType.WIDTH, numColumns, availableWidthPx
                            )
                            responsiveWorkspaceHeightSpec = wsProvider.getCalculatedSpec(
                                aspectRatio, DimensionType.HEIGHT, numRows, availableHeightPx
                            )
                            responsiveWorkspaceWidthSpec?.let { spec ->
                                workspacePaddingLeftPx = spec.startPaddingPx
                                workspacePaddingRightPx = spec.endPaddingPx
                                cellLayoutBorderSpacePx = Point(spec.gutterPx, cellLayoutBorderSpacePx.y)
                            }
                            responsiveWorkspaceHeightSpec?.let { spec ->
                                workspacePaddingTopPx = spec.startPaddingPx
                                workspacePaddingBottomPx = spec.endPaddingPx
                                cellLayoutBorderSpacePx = Point(cellLayoutBorderSpacePx.x, spec.gutterPx)
                            }
                        } catch (e: Exception) {
                            // Graceful fallback
                        }
                    }

                    // All Apps specs
                    val aaSpecsId = if (isTwoPanels && inv.allAppsSpecsTwoPanelId > 0) {
                        inv.allAppsSpecsTwoPanelId
                    } else {
                        inv.allAppsSpecsId
                    }
                    if (aaSpecsId > 0) {
                        try {
                            val aaProvider = ResponsiveSpecsProvider.create(
                                ResourceHelper(context, aaSpecsId),
                                ResponsiveSpecType.AllApps
                            )
                            responsiveAllAppsWidthSpec = responsiveWorkspaceWidthSpec?.let {
                                aaProvider.getCalculatedSpec(
                                    aspectRatio, DimensionType.WIDTH, numColumns, availableWidthPx, it
                                )
                            } ?: aaProvider.getCalculatedSpec(
                                aspectRatio, DimensionType.WIDTH, numColumns, availableWidthPx
                            )
                            responsiveAllAppsHeightSpec = responsiveWorkspaceHeightSpec?.let {
                                aaProvider.getCalculatedSpec(
                                    aspectRatio, DimensionType.HEIGHT, numRows, availableHeightPx, it
                                )
                            } ?: aaProvider.getCalculatedSpec(
                                aspectRatio, DimensionType.HEIGHT, numRows, availableHeightPx
                            )
                        } catch (e: Exception) {
                        }
                    }

                    // Folder specs
                    val fSpecsId = if (isTwoPanels && inv.folderSpecsTwoPanelId > 0) {
                        inv.folderSpecsTwoPanelId
                    } else {
                        inv.folderSpecsId
                    }
                    if (fSpecsId > 0) {
                        try {
                            val fProvider = ResponsiveSpecsProvider.create(
                                ResourceHelper(context, fSpecsId),
                                ResponsiveSpecType.Folder
                            )
                            responsiveFolderWidthSpec = fProvider.getCalculatedSpec(
                                aspectRatio, DimensionType.WIDTH, numFolderColumns, availableWidthPx
                            )
                            responsiveFolderHeightSpec = fProvider.getCalculatedSpec(
                                aspectRatio, DimensionType.HEIGHT, numFolderRows, availableHeightPx
                            )
                        } catch (e: Exception) {
                        }
                    }

                    // Hotseat specs
                    val hSpecsId = if (isTwoPanels && inv.hotseatSpecsTwoPanelId > 0) {
                        inv.hotseatSpecsTwoPanelId
                    } else {
                        inv.hotseatSpecsId
                    }
                    if (hSpecsId > 0) {
                        try {
                            val hProvider = HotseatSpecsProvider.create(ResourceHelper(context, hSpecsId))
                            val dim = if (isVerticalBarLayout()) DimensionType.WIDTH else DimensionType.HEIGHT
                            val space = if (isVerticalBarLayout()) availableWidthPx else availableHeightPx
                            responsiveHotseatSpec = hProvider.getCalculatedSpec(aspectRatio, dim, space)
                            responsiveHotseatSpec?.let { spec ->
                                hotseatQsbSpacePx = spec.hotseatQsbSpace
                                hotseatBarBottomSpacePx = spec.edgePadding
                                hotseatBarSizePx = iconSizePx + hotseatBarBottomSpacePx
                            }
                        } catch (e: Exception) {
                        }
                    }

                    // Workspace Cell specs
                    val wcSpecsId = if (isTwoPanels && inv.workspaceCellSpecsTwoPanelId > 0) {
                        inv.workspaceCellSpecsTwoPanelId
                    } else {
                        inv.workspaceCellSpecsId
                    }
                    if (wcSpecsId > 0) {
                        try {
                            val wcProvider = ResponsiveCellSpecsProvider.create(ResourceHelper(context, wcSpecsId))
                            responsiveWorkspaceCellSpec = wcProvider.getCalculatedSpec(aspectRatio, availableHeightPx)
                            responsiveWorkspaceCellSpec?.let { spec ->
                                iconSizePx = spec.iconSize
                                iconBitmapSizePx = iconSizePx
                                iconTextSizePx = spec.iconTextSize.toFloat()
                            }
                        } catch (e: Exception) {
                        }
                    }

                    // All Apps Cell specs
                    val acSpecsId = if (isTwoPanels && inv.allAppsCellSpecsTwoPanelId > 0) {
                        inv.allAppsCellSpecsTwoPanelId
                    } else {
                        inv.allAppsCellSpecsId
                    }
                    if (acSpecsId > 0) {
                        try {
                            val acProvider = ResponsiveCellSpecsProvider.create(ResourceHelper(context, acSpecsId))
                            responsiveAllAppsCellSpec = responsiveWorkspaceCellSpec?.let {
                                acProvider.getCalculatedSpec(aspectRatio, availableHeightPx, it)
                            } ?: acProvider.getCalculatedSpec(aspectRatio, availableHeightPx)
                            responsiveAllAppsCellSpec?.let { spec ->
                                allAppsIconSizePx = spec.iconSize
                                allAppsIconTextSizePx = spec.iconTextSize.toFloat()
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }
}
