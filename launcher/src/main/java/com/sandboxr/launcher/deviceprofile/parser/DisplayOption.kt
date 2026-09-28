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
import android.graphics.PointF
import android.util.Xml
import com.sandboxr.launcher.R
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.INDEX_DEFAULT
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.parsePointMap
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.parsePointMapFromDefaults
import com.sandboxr.launcher.deviceprofile.parser.DeviceTypedMap.parseTypedMap
import com.sandboxr.launcher.display.LauncherDisplayInfo
import org.xmlpull.v1.XmlPullParser

/**
 * Parsed sizing parameters from a `<display-option>` XML element.
 *
 * A [DisplayOption] pairs with a [GridOption] to provide the full set of per-device-type
 * display sizing metrics (icon sizes, border spaces, hotseat spacing, etc.). Multiple
 * [DisplayOption]s may match a given display; [InvariantDeviceProfile] interpolates between
 * the nearest neighbours to compute final values.
 */
class DisplayOption private constructor(
    @JvmField val grid: GridOption,
    context: Context,
    ta: TypedArray,
) {
    @JvmField val minWidthDps: Float =
        ta.getFloat(R.styleable.ProfileDisplayOption_minWidthDps, 0f)
    @JvmField val minHeightDps: Float =
        ta.getFloat(R.styleable.ProfileDisplayOption_minHeightDps, 0f)
    @JvmField val canBeDefault: Boolean =
        ta.getBoolean(R.styleable.ProfileDisplayOption_canBeDefault, false)

    @JvmField
    val minCellSize: Array<PointF> = ta.parsePointMap(
        R.styleable.ProfileDisplayOption_minCellWidth,
        R.styleable.ProfileDisplayOption_minCellHeight,
        R.styleable.ProfileDisplayOption_minCellWidthLandscape,
        R.styleable.ProfileDisplayOption_minCellHeightLandscape,
        R.styleable.ProfileDisplayOption_minCellWidthTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_minCellHeightTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_minCellWidthTwoPanelLandscape,
        R.styleable.ProfileDisplayOption_minCellHeightTwoPanelLandscape,
    )

    private val defaultBorderSpace: Array<Float> =
        ta.parseTypedMap(
            0f,
            R.styleable.ProfileDisplayOption_borderSpace,
            R.styleable.ProfileDisplayOption_borderSpaceLandscape,
            R.styleable.ProfileDisplayOption_borderSpaceTwoPanelPortrait,
            R.styleable.ProfileDisplayOption_borderSpaceTwoPanelLandscape,
        ) { index, v -> getFloat(index, v) }

    @JvmField
    val borderSpaces: Array<PointF> = ta.parsePointMapFromDefaults(
        defaultBorderSpace,
        R.styleable.ProfileDisplayOption_borderSpaceHorizontal,
        R.styleable.ProfileDisplayOption_borderSpaceVertical,
        R.styleable.ProfileDisplayOption_borderSpaceLandscapeHorizontal,
        R.styleable.ProfileDisplayOption_borderSpaceLandscapeVertical,
        R.styleable.ProfileDisplayOption_borderSpaceTwoPanelPortraitHorizontal,
        R.styleable.ProfileDisplayOption_borderSpaceTwoPanelPortraitVertical,
        R.styleable.ProfileDisplayOption_borderSpaceTwoPanelLandscapeHorizontal,
        R.styleable.ProfileDisplayOption_borderSpaceTwoPanelLandscapeVertical,
    )

    @JvmField
    val horizontalMargin: FloatArray = ta.parseTypedMap(
        0f,
        R.styleable.ProfileDisplayOption_horizontalMargin,
        R.styleable.ProfileDisplayOption_horizontalMarginLandscape,
        R.styleable.ProfileDisplayOption_horizontalMarginTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_horizontalMarginTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val hotseatBarBottomSpace: FloatArray = ta.parseTypedMap(
        0f,
        R.styleable.ProfileDisplayOption_hotseatBarBottomSpace,
        R.styleable.ProfileDisplayOption_hotseatBarBottomSpaceLandscape,
        R.styleable.ProfileDisplayOption_hotseatBarBottomSpaceTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_hotseatBarBottomSpaceTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val hotseatQsbSpace: FloatArray = ta.parseTypedMap(
        0f,
        R.styleable.ProfileDisplayOption_hotseatQsbSpace,
        R.styleable.ProfileDisplayOption_hotseatQsbSpaceLandscape,
        R.styleable.ProfileDisplayOption_hotseatQsbSpaceTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_hotseatQsbSpaceTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val iconSizes: FloatArray = ta.parseTypedMap(
        48f, // default icon size in dp
        R.styleable.ProfileDisplayOption_iconImageSize,
        R.styleable.ProfileDisplayOption_iconSizeLandscape,
        R.styleable.ProfileDisplayOption_iconSizeTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_iconSizeTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val textSizes: FloatArray = ta.parseTypedMap(
        13f, // default text size in sp
        R.styleable.ProfileDisplayOption_iconTextSize,
        R.styleable.ProfileDisplayOption_iconTextSizeLandscape,
        R.styleable.ProfileDisplayOption_iconTextSizeTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_iconTextSizeTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val allAppsCellSize: Array<PointF> = ta.parsePointMap(
        R.styleable.ProfileDisplayOption_allAppsCellWidth,
        R.styleable.ProfileDisplayOption_allAppsCellHeight,
        R.styleable.ProfileDisplayOption_allAppsCellWidthLandscape,
        R.styleable.ProfileDisplayOption_allAppsCellHeightLandscape,
        R.styleable.ProfileDisplayOption_allAppsCellWidthTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_allAppsCellHeightTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_allAppsCellWidthTwoPanelLandscape,
        R.styleable.ProfileDisplayOption_allAppsCellHeightTwoPanelLandscape,
    )

    @JvmField
    val allAppsIconSizes: FloatArray = ta.parseTypedMap(
        iconSizes[INDEX_DEFAULT],
        R.styleable.ProfileDisplayOption_allAppsIconSize,
        R.styleable.ProfileDisplayOption_allAppsIconSizeLandscape,
        R.styleable.ProfileDisplayOption_allAppsIconSizeTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_allAppsIconSizeTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val allAppsIconTextSizes: FloatArray = ta.parseTypedMap(
        textSizes[INDEX_DEFAULT],
        R.styleable.ProfileDisplayOption_allAppsIconTextSize,
        R.styleable.ProfileDisplayOption_allAppsIconTextSize,
        R.styleable.ProfileDisplayOption_allAppsIconTextSizeTwoPanelPortrait,
        R.styleable.ProfileDisplayOption_allAppsIconTextSizeTwoPanelLandscape,
    ) { i, v -> getFloat(i, v) }.toFloatArray()

    @JvmField
    val allAppsBorderSpaces: Array<PointF> = ta.parsePointMapFromDefaults(
        ta.parseTypedMap(
            defaultBorderSpace[INDEX_DEFAULT],
            R.styleable.ProfileDisplayOption_allAppsBorderSpace,
            R.styleable.ProfileDisplayOption_allAppsBorderSpaceLandscape,
            R.styleable.ProfileDisplayOption_allAppsBorderSpaceTwoPanelPortrait,
            R.styleable.ProfileDisplayOption_allAppsBorderSpaceTwoPanelLandscape,
        ) { index, v -> getFloat(index, v) },
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceHorizontal,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceVertical,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceLandscapeHorizontal,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceLandscapeVertical,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceTwoPanelPortraitHorizontal,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceTwoPanelPortraitVertical,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceTwoPanelLandscapeHorizontal,
        R.styleable.ProfileDisplayOption_allAppsBorderSpaceTwoPanelLandscapeVertical,
    )

    companion object {

        /**
         * Parses all `<display-option>` children of the given `<grid-option>` element from
         * `res/xml/device_profiles.xml`.
         */
        fun parseAll(
            context: Context,
            displayInfo: LauncherDisplayInfo,
            gridOption: GridOption,
        ): List<DisplayOption> {
            val parser = context.resources.getXml(R.xml.device_profiles)
            val results = mutableListOf<DisplayOption>()
            var insideGrid = false
            try {
                var event = parser.next()
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        if (parser.name == GridOption.TAG_NAME) {
                            val ta = context.resources.obtainAttributes(
                                Xml.asAttributeSet(parser),
                                R.styleable.GridDisplayOption,
                            )
                            val name = ta.getString(R.styleable.GridDisplayOption_name)
                            ta.recycle()
                            insideGrid = (name == gridOption.name)
                        } else if (insideGrid && parser.name == "display-option") {
                            val ta = context.resources.obtainAttributes(
                                Xml.asAttributeSet(parser),
                                R.styleable.ProfileDisplayOption,
                            )
                            results.add(DisplayOption(gridOption, context, ta))
                            ta.recycle()
                        }
                    } else if (event == XmlPullParser.END_TAG &&
                        parser.name == GridOption.TAG_NAME
                    ) {
                        if (insideGrid) break
                    }
                    event = parser.next()
                }
            } finally {
                parser.close()
            }
            return results
        }
    }
}
