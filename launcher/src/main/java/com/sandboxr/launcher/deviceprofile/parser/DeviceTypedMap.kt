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

import android.content.res.TypedArray
import android.graphics.PointF

/**
 * Utilities for parsing XML attribute arrays that encode device-type–specific values.
 *
 * The launcher defines configuration for four display orientations / form factors:
 * - **Default** (portrait phone / default orientation).
 * - **Landscape** (phone landscape).
 * - **Two-panel portrait** (foldable open in portrait / split-screen portrait).
 * - **Two-panel landscape** (foldable open in landscape / split-screen landscape).
 *
 * XML attributes that vary per device type are packed into four successive indices inside a
 * [TypedArray]. These helpers parse those packed arrays into typed Kotlin arrays of size [COUNT_SIZES].
 */
object DeviceTypedMap {

    /** Number of device-type slots in every sized attribute map. */
    const val COUNT_SIZES: Int = 4

    /** Index for the default (portrait phone) value. */
    const val INDEX_DEFAULT: Int = 0

    /** Index for the landscape-phone value. */
    const val INDEX_LANDSCAPE: Int = 1

    /** Index for the two-panel portrait (e.g. open foldable) value. */
    const val INDEX_TWO_PANEL_PORTRAIT: Int = 2

    /** Index for the two-panel landscape value. */
    const val INDEX_TWO_PANEL_LANDSCAPE: Int = 3

    /**
     * Parses a [TypedArray] into an [Array] of size [COUNT_SIZES] by applying [mapper] once per
     * device type, falling back to the previous value when an attribute is not set.
     *
     * @param startValue      Seed value used as the default fallback for INDEX_DEFAULT.
     * @param indexDefault    Attribute index for the default entry.
     * @param indexLandscape  Attribute index for the landscape entry.
     * @param indexTwoPanelPortrait  Attribute index for the two-panel portrait entry.
     * @param indexTwoPanelLandscape Attribute index for the two-panel landscape entry.
     * @param mapper          Reads the attribute at [R] and returns [T], using [T] as the fallback.
     */
    inline fun <reified T : Any, reified R> TypedArray.parseTypedMap(
        startValue: T,
        indexDefault: R,
        indexLandscape: R,
        indexTwoPanelPortrait: R,
        indexTwoPanelLandscape: R,
        mapper: TypedArray.(R, T) -> T,
    ): Array<T> {
        val firstValue = mapper.invoke(this, indexDefault, startValue)
        return arrayOf(
            firstValue,
            mapper.invoke(this, indexLandscape, firstValue),
            mapper.invoke(this, indexTwoPanelPortrait, firstValue),
            mapper.invoke(this, indexTwoPanelLandscape, firstValue),
        )
    }

    /**
     * Convenience variant that parses a pair of float attributes into a [PointF] per device type.
     */
    fun TypedArray.parsePointMap(
        indexDefaultX: Int,
        indexDefaultY: Int,
        indexLandscapeX: Int,
        indexLandscapeY: Int,
        indexTwoPanelPortraitX: Int,
        indexTwoPanelPortraitY: Int,
        indexTwoPanelLandscapeX: Int,
        indexTwoPanelLandscapeY: Int,
    ): Array<PointF> =
        parseTypedMap(
            PointF(0f, 0f),
            indexDefaultX to indexDefaultY,
            indexLandscapeX to indexLandscapeY,
            indexTwoPanelPortraitX to indexTwoPanelPortraitY,
            indexTwoPanelLandscapeX to indexTwoPanelLandscapeY,
        ) { (xi, yi), v ->
            PointF(getFloat(xi, v.x), getFloat(yi, v.y))
        }

    /**
     * Variant of [parsePointMap] that uses separate per-orientation default scalars from [defaults].
     */
    fun TypedArray.parsePointMapFromDefaults(
        defaults: Array<Float>,
        indexDefaultX: Int,
        indexDefaultY: Int,
        indexLandscapeX: Int,
        indexLandscapeY: Int,
        indexTwoPanelPortraitX: Int,
        indexTwoPanelPortraitY: Int,
        indexTwoPanelLandscapeX: Int,
        indexTwoPanelLandscapeY: Int,
    ): Array<PointF> = arrayOf(
        PointF(
            getFloat(indexDefaultX, defaults[INDEX_DEFAULT]),
            getFloat(indexDefaultY, defaults[INDEX_DEFAULT]),
        ),
        PointF(
            getFloat(indexLandscapeX, defaults[INDEX_LANDSCAPE]),
            getFloat(indexLandscapeY, defaults[INDEX_LANDSCAPE]),
        ),
        PointF(
            getFloat(indexTwoPanelPortraitX, defaults[INDEX_TWO_PANEL_PORTRAIT]),
            getFloat(indexTwoPanelPortraitY, defaults[INDEX_TWO_PANEL_PORTRAIT]),
        ),
        PointF(
            getFloat(indexTwoPanelLandscapeX, defaults[INDEX_TWO_PANEL_LANDSCAPE]),
            getFloat(indexTwoPanelLandscapeY, defaults[INDEX_TWO_PANEL_LANDSCAPE]),
        ),
    )
}
