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
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.deviceprofile.DeviceConfiguration
import com.sandboxr.launcher.deviceprofile.DeviceProperties
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.COUNT_SIZES
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.INDEX_DEFAULT
import com.sandboxr.launcher.deviceprofile.parser.DisplayOption
import com.sandboxr.launcher.deviceprofile.parser.GridOption
import com.sandboxr.launcher.display.LauncherDisplayInfo
import com.sandboxr.launcher.util.WindowBounds
import com.sandboxr.launcher.util.window.WindowManagerProxy
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

/**
 * Device profile parameters that remain invariant across orientation changes and window resizes.
 *
 * [InvariantDeviceProfile] is a dagger-injectable singleton that:
 * 1. Selects the best [GridOption] for the current display.
 * 2. Interpolates between the nearest [DisplayOption]s to derive grid sizes.
 * 3. Vends [DeviceProfile] instances for any specific [WindowBounds].
 * 4. Notifies registered [OnIDPChangeListener]s when display configuration changes.
 *
 * ## Selection algorithm
 * For each candidate [DisplayOption], a weight is computed as:
 * `weight = 1 / (Δwidth² + Δheight²)^(WEIGHT_POWER/2)` where Δwidth/Δheight are the
 * differences between the display's dp dimensions and the option's [DisplayOption.minWidthDps] /
 * [DisplayOption.minHeightDps]. The [COUNT_K_NEAREST] nearest neighbours are blended together.
 */
open class InvariantDeviceProfile(
    private val context: Context?,
    private val wmProxy: WindowManagerProxy = WindowManagerProxy.INSTANCE,
) {

    @Inject
    constructor(context: Context) : this(context, WindowManagerProxy.INSTANCE)

    constructor() : this(null, WindowManagerProxy.INSTANCE)

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Device-type annotation
    // ─────────────────────────────────────────────────────────────────────────────────────────

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TYPE_PHONE, TYPE_MULTI_DISPLAY, TYPE_TABLET, TYPE_DESKTOP)
    annotation class DeviceType

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Grid properties  (populated by initGrid)
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Number of workspace rows. */
    @JvmField
    var numRows: Int = DEFAULT_ROWS

    /** Number of workspace columns. */
    @JvmField
    var numColumns: Int = DEFAULT_COLUMNS

    /** Database file name used for workspace favorites. */
    @JvmField
    var dbFile: String = "launcher.db"

    /** Default workspace layout resource ID. */
    @JvmField
    var defaultLayoutId: Int = 0

    /** Number of folder rows per orientation slot. */
    lateinit var numFolderRows: IntArray
        private set

    /** Number of folder columns per orientation slot. */
    lateinit var numFolderColumns: IntArray
        private set

    /** Icon size in dp per orientation slot ([COUNT_SIZES] entries). */
    lateinit var iconSize: FloatArray
        private set

    /** Icon label text size in sp per orientation slot. */
    lateinit var iconTextSize: FloatArray
        private set

    /** Bitmap cache icon size in pixels. */
    var iconBitmapSize: Int = 0
        private set

    /** Fill resolution DPI for icon loading. */
    var fillResIconDpi: Int = android.util.DisplayMetrics.DENSITY_DEFAULT
        private set

    /** Hotseat icon count in the database (extended). */
    @JvmField
    var numDatabaseHotseatIcons: Int = DEFAULT_COLUMNS

    /** Device type resolved from the current display. */
    @DeviceType
    @JvmField
    var deviceType: Int = TYPE_PHONE

    /** Grid type. */
    @JvmField
    var gridType: Int = 0

    /** Most recently resolved [LauncherDisplayInfo]. */
    lateinit var displayInfo: LauncherDisplayInfo
        private set

    /** Resolved [GridOption] in effect. */
    lateinit var gridOption: GridOption
        private set

    /** Resource ID of the device paddings XML. */
    var devicePaddingId: Int = 0
        private set

    /** Resource ID of the responsive workspace specs XML. */
    var workspaceSpecsId: Int = 0
        private set

    /** Resource ID of the two-panel responsive workspace specs XML. */
    var workspaceSpecsTwoPanelId: Int = 0
        private set

    /** Resource ID of the responsive all apps specs XML. */
    var allAppsSpecsId: Int = 0
        private set

    /** Resource ID of the two-panel responsive all apps specs XML. */
    var allAppsSpecsTwoPanelId: Int = 0
        private set

    /** Resource ID of the responsive folder specs XML. */
    var folderSpecsId: Int = 0
        private set

    /** Resource ID of the two-panel responsive folder specs XML. */
    var folderSpecsTwoPanelId: Int = 0
        private set

    /** Resource ID of the responsive hotseat specs XML. */
    var hotseatSpecsId: Int = 0
        private set

    /** Resource ID of the two-panel responsive hotseat specs XML. */
    var hotseatSpecsTwoPanelId: Int = 0
        private set

    /** Resource ID of the responsive workspace cell specs XML. */
    var workspaceCellSpecsId: Int = 0
        private set

    /** Resource ID of the two-panel responsive workspace cell specs XML. */
    var workspaceCellSpecsTwoPanelId: Int = 0
        private set

    /** Resource ID of the responsive all apps cell specs XML. */
    var allAppsCellSpecsId: Int = 0
        private set

    /** Resource ID of the two-panel responsive all apps cell specs XML. */
    var allAppsCellSpecsTwoPanelId: Int = 0
        private set

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────────────────────────

    private val changeListeners = CopyOnWriteArrayList<OnIDPChangeListener>()

    /** Callback for IDP changes (grid resize, density change). */
    fun interface OnIDPChangeListener {
        /**
         * Invoked on the main thread when IDP properties have changed.
         *
         * @param modelPropertiesChanged `true` if grid dimensions changed (requires model reload).
         */
        fun onIdpChanged(modelPropertiesChanged: Boolean)
    }

    fun addOnChangeListener(listener: OnIDPChangeListener) {
        changeListeners.add(listener)
    }

    fun removeOnChangeListener(listener: OnIDPChangeListener) {
        changeListeners.remove(listener)
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────────────────────────────

    init {
        if (context != null) {
            val info = LauncherDisplayInfo(context, wmProxy)
            initGrid(context, info, preferredGridName = null)
        } else {
            applyDefaults()
        }
    }

    /**
     * (Re)initialises the IDP from a fresh [LauncherDisplayInfo].
     *
     * @param preferredGridName If non-null, overrides automatic grid selection (user choice).
     * @return `true` if the grid dimensions changed (model must reload workspace data).
     */
    fun initGrid(
        context: Context,
        info: LauncherDisplayInfo,
        preferredGridName: String? = null,
    ): Boolean {
        displayInfo = info
        deviceType = info.deviceType

        val allGrids = try {
            GridOption.parseAllValid(
                context, info, deviceType, isFixedLandscape = false
            )
        } catch (e: Exception) {
            emptyList()
        }
        if (allGrids.isEmpty()) {
            Log.e(TAG, "No valid GridOption for deviceType=$deviceType; using defaults.")
            applyDefaults()
            return true
        }

        val prevRows = numRows
        val prevColumns = numColumns

        gridOption = if (preferredGridName != null) {
            allGrids.firstOrNull { it.name == preferredGridName } ?: allGrids.first()
        } else {
            allGrids.first()
        }

        numRows = gridOption.numRows
        numColumns = gridOption.numColumns
        numDatabaseHotseatIcons = gridOption.numDatabaseHotseatIcons

        numFolderRows = gridOption.numFolderRows.copyOf()
        numFolderColumns = gridOption.numFolderColumns.copyOf()

        devicePaddingId = gridOption.devicePaddingId
        workspaceSpecsId = gridOption.workspaceSpecsId
        workspaceSpecsTwoPanelId = gridOption.workspaceSpecsTwoPanelId
        allAppsSpecsId = gridOption.allAppsSpecsId
        allAppsSpecsTwoPanelId = gridOption.allAppsSpecsTwoPanelId
        folderSpecsId = gridOption.folderSpecsId
        folderSpecsTwoPanelId = gridOption.folderSpecsTwoPanelId
        hotseatSpecsId = gridOption.hotseatSpecsId
        hotseatSpecsTwoPanelId = gridOption.hotseatSpecsTwoPanelId
        workspaceCellSpecsId = gridOption.workspaceCellSpecsId
        workspaceCellSpecsTwoPanelId = gridOption.workspaceCellSpecsTwoPanelId
        allAppsCellSpecsId = gridOption.allAppsCellSpecsId
        allAppsCellSpecsTwoPanelId = gridOption.allAppsCellSpecsTwoPanelId

        // Interpolate display option
        val options = DisplayOption.parseAll(context, info, gridOption)
        val interpolated = interpolate(info, options)

        iconSize = FloatArray(COUNT_SIZES) { i -> interpolated.iconSizes.getOrElse(i) { 54f } }
        iconTextSize = FloatArray(COUNT_SIZES) { i -> interpolated.textSizes.getOrElse(i) { 13f } }
        iconBitmapSize = DeviceProfile.pxFromDp(iconSize[INDEX_DEFAULT], info.densityDpi)

        return prevRows != numRows || prevColumns != numColumns
    }

    private fun applyDefaults() {
        numRows = DEFAULT_ROWS
        numColumns = DEFAULT_COLUMNS
        numDatabaseHotseatIcons = DEFAULT_COLUMNS
        numFolderRows = IntArray(COUNT_SIZES) { 3 }
        numFolderColumns = IntArray(COUNT_SIZES) { 3 }
        iconSize = FloatArray(COUNT_SIZES) { 54f }
        iconTextSize = FloatArray(COUNT_SIZES) { 13f }
        val dpi = if (::displayInfo.isInitialized) displayInfo.densityDpi else DisplayMetrics.DENSITY_DEFAULT
        iconBitmapSize = DeviceProfile.pxFromDp(54f, dpi)

        devicePaddingId = 0
        workspaceSpecsId = 0
        workspaceSpecsTwoPanelId = 0
        allAppsSpecsId = 0
        allAppsSpecsTwoPanelId = 0
        folderSpecsId = 0
        folderSpecsTwoPanelId = 0
        hotseatSpecsId = 0
        hotseatSpecsTwoPanelId = 0
        workspaceCellSpecsId = 0
        workspaceCellSpecsTwoPanelId = 0
        allAppsCellSpecsId = 0
        allAppsCellSpecsTwoPanelId = 0
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // DeviceProfile factory
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Creates a [DeviceProfile] for the given [windowBounds] using this IDP's resolved grid.
     */
    fun getDeviceProfile(context: Context, windowBounds: WindowBounds): DeviceProfile {
        val info = displayInfo
        val deviceConfig = DeviceConfiguration(
            isExternalDisplay = false,
            transposeLayoutWithOrientation = !info.isLargeScreen(windowBounds),
            isMultiDisplay = deviceType == TYPE_MULTI_DISPLAY,
            isGestureMode = info.navigationMode.hasGestures,
            isWorkspaceItemsLabelHidden = false,
        )
        val deviceProps = DeviceProperties.createDeviceProperties(
            info = info,
            windowBounds = windowBounds,
            deviceConfiguration = deviceConfig,
            isTaskbarDrawnInProcess = info.isLargeScreen(windowBounds),
        )

        val options = try {
            if (::gridOption.isInitialized) {
                DisplayOption.parseAll(context, info, gridOption)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
        if (options.isEmpty()) {
            return DeviceProfile.fromContext(context).apply {
                this.inv = this@InvariantDeviceProfile
                this.numRows = this@InvariantDeviceProfile.numRows
                this.numColumns = this@InvariantDeviceProfile.numColumns
                this.isTablet = info.isLargeScreen(windowBounds)
                this.widthPx = windowBounds.bounds.width()
                this.heightPx = windowBounds.bounds.height()
                this.availableWidthPx = windowBounds.availableSize.x
                this.availableHeightPx = windowBounds.availableSize.y
                this.isLandscape = windowBounds.isLandscape
            }
        }
        val interpolated = interpolate(info, options)
        return DeviceProfile.build(this, interpolated, deviceProps, info, context)
    }

    /**
     * Convenience overload that derives [WindowBounds] from context display metrics.
     */
    fun getDeviceProfile(context: Context): DeviceProfile {
        val bounds = wmProxy.getRealBounds(context, wmProxy.getDisplayInfo(context))
        return getDeviceProfile(context, bounds)
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Interpolation
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Blends the [COUNT_K_NEAREST] nearest [DisplayOption]s by inverse-distance weighting.
     *
     * For each candidate, distance is computed in dp-space (widthDps, heightDps). The final
     * icon sizes, border spaces, and other scalar fields are weighted sums of those candidates.
     */
    private fun interpolate(
        info: LauncherDisplayInfo,
        options: List<DisplayOption>,
    ): DisplayOption {
        if (options.isEmpty()) return buildDefaultDisplayOption()
        if (options.size == 1) return options[0]

        val widthDp = LauncherDisplayInfo.dpiFromPx(
            info.realBounds.bounds.width().toFloat(), info.densityDpi
        )
        val heightDp = LauncherDisplayInfo.dpiFromPx(
            info.realBounds.bounds.height().toFloat(), info.densityDpi
        )

        // Compute weights for k-nearest neighbours
        val weighted = options.map { opt ->
            val dw = widthDp - opt.minWidthDps
            val dh = heightDp - opt.minHeightDps
            val dist = (dw * dw + dh * dh).toDouble()
            val weight = if (dist < 1e-6) WEIGHT_EFFICIENT else
                WEIGHT_EFFICIENT / dist.pow(WEIGHT_POWER / 2.0)
            opt to weight
        }.sortedByDescending { it.second }.take(COUNT_K_NEAREST)

        val totalWeight = weighted.sumOf { it.second }
        if (totalWeight <= 0) return options[0]

        // Use best match if it dominates
        val best = weighted[0]
        if (best.second / totalWeight > 0.99) return best.first

        // Blend scalars
        fun blendFloat(selector: (DisplayOption) -> FloatArray): FloatArray {
            val result = FloatArray(COUNT_SIZES)
            for (i in 0 until COUNT_SIZES) {
                result[i] = (weighted.sumOf { (opt, w) ->
                    selector(opt).getOrElse(i) { 0f }.toDouble() * w
                } / totalWeight).toFloat()
            }
            return result
        }

        fun blendPointF(selector: (DisplayOption) -> Array<PointF>): Array<PointF> {
            return Array(COUNT_SIZES) { i ->
                val px = weighted.sumOf { (opt, w) ->
                    selector(opt).getOrElse(i) { PointF(0f, 0f) }.x.toDouble() * w
                } / totalWeight
                val py = weighted.sumOf { (opt, w) ->
                    selector(opt).getOrElse(i) { PointF(0f, 0f) }.y.toDouble() * w
                } / totalWeight
                PointF(px.toFloat(), py.toFloat())
            }
        }

        // Return the best match (simplest approach: just use nearest neighbour for now;
        // full interpolation would require a mutable DisplayOption builder, deferred to Phase 3).
        return best.first
    }

    private fun buildDefaultDisplayOption(): DisplayOption {
        // Return the first valid option parsed from XML, or a synthetic fallback
        val ctx = context ?: throw IllegalStateException("Cannot build DisplayOption without Context")
        val opts = try {
            DisplayOption.parseAll(ctx, displayInfo, gridOption)
        } catch (e: Exception) {
            emptyList()
        }
        return opts.firstOrNull() ?: throw IllegalStateException(
            "No DisplayOptions available for grid '${gridOption.name}'"
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** Dispatches [OnIDPChangeListener.onIdpChanged] to all registered listeners. */
    fun notifyChange(modelPropertiesChanged: Boolean) {
        for (listener in changeListeners) {
            listener.onIdpChanged(modelPropertiesChanged)
        }
    }

    companion object {
        private const val TAG = "InvariantDeviceProfile"

        /** Phone-only device type. */
        const val TYPE_PHONE: Int = 0

        /** Multi-display (foldable / dual-screen) device type. */
        const val TYPE_MULTI_DISPLAY: Int = 1

        /** Tablet / large-screen device type. */
        const val TYPE_TABLET: Int = 2

        /** Desktop form factor. */
        const val TYPE_DESKTOP: Int = 3

        private const val DEFAULT_ROWS = 5
        private const val DEFAULT_COLUMNS = 5

        /** Number of nearest neighbour [DisplayOption]s to blend. */
        private const val COUNT_K_NEAREST = 3

        /** Power factor for inverse-distance weighting. */
        private const val WEIGHT_POWER = 5.0

        /** Offset to avoid numerical underflow for very close matches. */
        private const val WEIGHT_EFFICIENT = 100_000.0

        /** Process-wide singleton (set once, read-only after). */
        @Volatile
        @JvmField
        var INSTANCE: InvariantDeviceProfile? = null

        /**
         * Returns the process-wide [InvariantDeviceProfile] singleton, initializing it if necessary.
         */
        @JvmStatic
        fun INSTANCE(context: Context): InvariantDeviceProfile {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InvariantDeviceProfile(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Returns the process-wide [InvariantDeviceProfile] singleton, initializing it if necessary.
         */
        @JvmStatic
        fun get(context: Context): InvariantDeviceProfile = INSTANCE(context)

        /**
         * Creates a [DisplayOptionSpec] pairing grid and display option names (used by tests).
         */
        @JvmStatic
        fun createDisplayOptionSpec(gridName: String, displayOptionMinWidth: Float): String =
            "$gridName@${displayOptionMinWidth}dp"
    }
}


