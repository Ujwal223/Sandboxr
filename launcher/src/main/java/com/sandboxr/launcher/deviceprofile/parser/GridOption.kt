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

package com.sandboxr.launcher.deviceprofile.parser

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Point
import android.util.Xml
import androidx.annotation.StyleRes
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.InvariantDeviceProfile.DeviceType
import com.sandboxr.launcher.R
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.parseTypedMap
import com.sandboxr.launcher.display.LauncherDisplayInfo
import org.xmlpull.v1.XmlPullParser
import kotlin.math.min

/**
 * Parsed representation of a single `<grid-option>` XML element from `res/xml/device_profiles.xml`.
 *
 * Each [GridOption] describes the row/column grid, hotseat size, folder configuration, and
 * responsive-spec resource references for one named grid preset (e.g. "normal", "practical",
 * "large"). [InvariantDeviceProfile] selects the best matching [GridOption] for the current
 * display and applies the corresponding [DisplayOption] sizing.
 */
class GridOption private constructor(ta: TypedArray, displayInfo: LauncherDisplayInfo) {

    @JvmField val name: String = requireNotNull(ta.getString(R.styleable.GridDisplayOption_name)) {
        "GridOption: 'name' attribute is required"
    }
    @JvmField val gridTitle: String? = ta.getString(R.styleable.GridDisplayOption_gridTitle)
    @JvmField val gridIconId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_gridIconId, 0)

    // Grid dimensions
    @JvmField val numRows: Int = ta.getInt(R.styleable.GridDisplayOption_numRows, 5)
    @JvmField val numColumns: Int = ta.getInt(R.styleable.GridDisplayOption_numColumns, 5)
    @JvmField val numSearchContainerColumns: Int =
        ta.getInt(R.styleable.GridDisplayOption_numSearchContainerColumns, numColumns)
    @JvmField val dbFile: String? = ta.getString(R.styleable.GridDisplayOption_dbFile)
    @JvmField val defaultLayoutId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_defaultLayoutId, 0)

    @JvmField
    val numFolderRows: IntArray =
        ta.parseTypedMap(
            numRows,
            R.styleable.GridDisplayOption_numFolderRows,
            R.styleable.GridDisplayOption_numFolderRowsLandscape,
            R.styleable.GridDisplayOption_numFolderRowsTwoPanelPortrait,
            R.styleable.GridDisplayOption_numFolderRowsTwoPanelLandscape,
        ) { index, v -> getInt(index, v) }.toIntArray()

    @JvmField
    val numFolderColumns: IntArray =
        ta.parseTypedMap(
            numColumns,
            R.styleable.GridDisplayOption_numFolderColumns,
            R.styleable.GridDisplayOption_numFolderColumnsLandscape,
            R.styleable.GridDisplayOption_numFolderColumnsTwoPanelPortrait,
            R.styleable.GridDisplayOption_numFolderColumnsTwoPanelLandscape,
        ) { index, v -> getInt(index, v) }.toIntArray()

    @StyleRes @JvmField
    val folderStyle: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_folderStyle, 0)

    @StyleRes @JvmField
    val cellStyle: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_cellStyle, 0)

    @StyleRes @JvmField
    val allAppsStyle: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_allAppsStyle, 0)

    @JvmField val numAllAppsColumns: Int =
        ta.getInt(R.styleable.GridDisplayOption_numAllAppsColumns, numColumns)
    @JvmField val numAllAppsRowsForCellHeightCalculation: Int =
        ta.getInt(R.styleable.GridDisplayOption_numAllAppsRowsForCellHeightCalculation, numRows)
    @JvmField val numDatabaseAllAppsColumns: Int =
        ta.getInt(R.styleable.GridDisplayOption_numExtendedAllAppsColumns, 2 * numAllAppsColumns)

    @JvmField val numHotseatIcons: Int =
        ta.getInt(R.styleable.GridDisplayOption_numHotseatIcons, numColumns)
    @JvmField val numDatabaseHotseatIcons: Int =
        ta.getInt(R.styleable.GridDisplayOption_numExtendedHotseatIcons, 2 * numHotseatIcons)

    @JvmField val inlineQsb: BooleanArray =
        ta.getInt(R.styleable.GridDisplayOption_inlineQsb, DONT_INLINE_QSB)
            .mapToFlagArray(
                INLINE_QSB_FOR_PORTRAIT,
                INLINE_QSB_FOR_LANDSCAPE,
                INLINE_QSB_FOR_TWO_PANEL_PORTRAIT,
                INLINE_QSB_FOR_TWO_PANEL_LANDSCAPE,
            )

    @JvmField val inlineNavButtonsEndSpacing: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_inlineNavButtonsEndSpacing, 0)

    @JvmField val isScalable: Boolean =
        ta.getBoolean(R.styleable.GridDisplayOption_isScalable, false)
    @JvmField val mIsDualGrid: Boolean =
        ta.getBoolean(R.styleable.GridDisplayOption_isDualGrid, false)
    @JvmField val devicePaddingId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_devicePaddingId, 0)

    @JvmField val workspaceSpecsId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_workspaceSpecsId, INVALID_RESOURCE_HANDLE)
    @JvmField val workspaceSpecsTwoPanelId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_workspaceSpecsTwoPanelId, workspaceSpecsId)
    @JvmField val allAppsSpecsId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_allAppsSpecsId, INVALID_RESOURCE_HANDLE)
    @JvmField val allAppsSpecsTwoPanelId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_allAppsSpecsTwoPanelId, allAppsSpecsId)
    @JvmField val folderSpecsId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_folderSpecsId, INVALID_RESOURCE_HANDLE)
    @JvmField val folderSpecsTwoPanelId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_folderSpecsTwoPanelId, folderSpecsId)
    @JvmField val hotseatSpecsId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_hotseatSpecsId, INVALID_RESOURCE_HANDLE)
    @JvmField val hotseatSpecsTwoPanelId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_hotseatSpecsTwoPanelId, hotseatSpecsId)
    @JvmField val workspaceCellSpecsId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_workspaceCellSpecsId, INVALID_RESOURCE_HANDLE)
    @JvmField val workspaceCellSpecsTwoPanelId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_workspaceCellSpecsTwoPanelId, workspaceCellSpecsId)
    @JvmField val allAppsCellSpecsId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_allAppsCellSpecsId, INVALID_RESOURCE_HANDLE)
    @JvmField val allAppsCellSpecsTwoPanelId: Int =
        ta.getResourceId(R.styleable.GridDisplayOption_allAppsCellSpecsTwoPanelId, allAppsCellSpecsId)

    @JvmField val isFixedLandscape: Boolean =
        ta.getBoolean(R.styleable.GridDisplayOption_isFixedLandscape, false)

    @JvmField val deviceCategory: Int =
        ta.getInt(R.styleable.GridDisplayOption_deviceCategory, DEVICE_CATEGORY_ANY)

    @JvmField val gridType: Int =
        ta.getInt(R.styleable.GridDisplayOption_gridType, GRID_TYPE_ANY)

    /** Returns true if this grid is applicable for the given [deviceType]. */
    fun isEnabled(@DeviceType deviceType: Int): Boolean = when (deviceType) {
        InvariantDeviceProfile.TYPE_PHONE ->
            (deviceCategory and DEVICE_CATEGORY_PHONE) == DEVICE_CATEGORY_PHONE
        InvariantDeviceProfile.TYPE_TABLET ->
            (deviceCategory and DEVICE_CATEGORY_TABLET) == DEVICE_CATEGORY_TABLET
        InvariantDeviceProfile.TYPE_MULTI_DISPLAY ->
            (deviceCategory and DEVICE_CATEGORY_MULTI_DISPLAY) == DEVICE_CATEGORY_MULTI_DISPLAY
        InvariantDeviceProfile.TYPE_DESKTOP ->
            (deviceCategory and DEVICE_CATEGORY_DESKTOP) == DEVICE_CATEGORY_DESKTOP
        else -> false
    }

    /** Returns true if the grid should be included given the current device/landscape state. */
    fun filterByFlag(deviceType: Int, isFixedLandscapeMode: Boolean): Boolean {
        if (this.isFixedLandscape || isFixedLandscapeMode) {
            return this.isFixedLandscape && isFixedLandscapeMode
        }
        return true
    }

    override fun toString(): String =
        "GridConfig{name='$name', gridTitle='$gridTitle', numRows=$numRows, numColumns=$numColumns}"

    companion object {
        const val TAG_NAME: String = "grid-option"

        const val INVALID_RESOURCE_HANDLE: Int = -1

        private const val DEVICE_CATEGORY_PHONE = 1 shl 0
        private const val DEVICE_CATEGORY_TABLET = 1 shl 1
        private const val DEVICE_CATEGORY_MULTI_DISPLAY = 1 shl 2
        private const val DEVICE_CATEGORY_DESKTOP = 1 shl 3
        private const val DEVICE_CATEGORY_ANY =
            DEVICE_CATEGORY_PHONE or DEVICE_CATEGORY_TABLET or
                DEVICE_CATEGORY_MULTI_DISPLAY or DEVICE_CATEGORY_DESKTOP

        private const val INLINE_QSB_FOR_PORTRAIT = 1 shl 0
        private const val INLINE_QSB_FOR_LANDSCAPE = 1 shl 1
        private const val INLINE_QSB_FOR_TWO_PANEL_PORTRAIT = 1 shl 2
        private const val INLINE_QSB_FOR_TWO_PANEL_LANDSCAPE = 1 shl 3
        private const val DONT_INLINE_QSB = 0

        private const val GRID_TYPE_ANY = 0xFFFF

        private fun Int.mapToFlagArray(vararg mask: Int): BooleanArray =
            BooleanArray(mask.size) { (this and mask[it]) == mask[it] }

        private fun findMinWidthAndHeightPxForDevice(displayInfo: LauncherDisplayInfo): Point {
            var minW = Int.MAX_VALUE
            var minH = Int.MAX_VALUE
            for (display in displayInfo.allDisplays) {
                minW = min(minW, display.size.x)
                minH = min(minH, display.size.y)
            }
            return Point(minW, minH)
        }

        /**
         * Parses all `<grid-option>` elements from `res/xml/device_profiles.xml`, applying
         * [filter] to skip irrelevant entries, and [mapper] to transform each match.
         */
        fun <T> parseAllDefined(
            context: Context,
            displayInfo: LauncherDisplayInfo,
            filter: (GridOption) -> Boolean = { true },
            mapper: (GridOption) -> T,
        ): List<T> {
            val parser = context.resources.getXml(R.xml.device_profiles)
            val results = mutableListOf<T>()
            try {
                var event = parser.next()
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG &&
                        parser.name == TAG_NAME
                    ) {
                        val ta = context.resources.obtainAttributes(
                            Xml.asAttributeSet(parser),
                            R.styleable.GridDisplayOption,
                        )
                        val option = GridOption(ta, displayInfo)
                        ta.recycle()
                        if (filter(option)) results.add(mapper(option))
                    }
                    event = parser.next()
                }
            } finally {
                parser.close()
            }
            return results
        }

        /** Returns all [GridOption]s valid for the given device type and landscape mode. */
        @JvmStatic
        fun parseAllValid(
            context: Context,
            displayInfo: LauncherDisplayInfo,
            deviceType: Int,
            isFixedLandscape: Boolean,
        ): List<GridOption> = parseAllDefined(
            context,
            displayInfo,
            filter = { it.isEnabled(deviceType) && it.filterByFlag(deviceType, isFixedLandscape) },
            mapper = { it },
        )
    }
}
