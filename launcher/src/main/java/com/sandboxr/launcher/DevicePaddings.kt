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
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Workspace items have a fixed height, so unused workspace height must be distributed.
 *
 * Unused/extra height is allocated across three variable dimensions:
 * - Space above the workspace
 * - Space between the workspace and hotseat
 * - Space below the hotseat
 */
open class DevicePaddings(context: Context, devicePaddingId: Int) {

    private val devicePaddings = ArrayList<DevicePadding>()

    init {
        if (devicePaddingId != 0) {
            try {
                context.resources.getXml(devicePaddingId).use { parser ->
                    val depth = parser.depth
                    var type = parser.next()
                    while ((type != XmlPullParser.END_TAG || parser.depth > depth) &&
                        type != XmlPullParser.END_DOCUMENT
                    ) {
                        if (type == XmlPullParser.START_TAG && DEVICE_PADDINGS == parser.name) {
                            val displayDepth = parser.depth
                            type = parser.next()
                            while ((type != XmlPullParser.END_TAG || parser.depth > displayDepth) &&
                                type != XmlPullParser.END_DOCUMENT
                            ) {
                                if (type == XmlPullParser.START_TAG && DEVICE_PADDING == parser.name) {
                                    val a = context.obtainStyledAttributes(
                                        Xml.asAttributeSet(parser), R.styleable.DevicePadding
                                    )
                                    val maxWidthPx = a.getDimensionPixelSize(
                                        R.styleable.DevicePadding_maxEmptySpace, 0
                                    )
                                    a.recycle()

                                    var workspaceTopPadding: PaddingFormula? = null
                                    var workspaceBottomPadding: PaddingFormula? = null
                                    var hotseatBottomPadding: PaddingFormula? = null

                                    val limitDepth = parser.depth
                                    type = parser.next()
                                    while ((type != XmlPullParser.END_TAG || parser.depth > limitDepth) &&
                                        type != XmlPullParser.END_DOCUMENT
                                    ) {
                                        val attr = Xml.asAttributeSet(parser)
                                        if (type == XmlPullParser.START_TAG) {
                                            when (parser.name) {
                                                WORKSPACE_TOP_PADDING ->
                                                    workspaceTopPadding = PaddingFormula(context, attr)
                                                WORKSPACE_BOTTOM_PADDING ->
                                                    workspaceBottomPadding = PaddingFormula(context, attr)
                                                HOTSEAT_BOTTOM_PADDING ->
                                                    hotseatBottomPadding = PaddingFormula(context, attr)
                                            }
                                        }
                                        type = parser.next()
                                    }

                                    if (workspaceTopPadding != null &&
                                        workspaceBottomPadding != null &&
                                        hotseatBottomPadding != null
                                    ) {
                                        val dp = DevicePadding(
                                            maxWidthPx,
                                            workspaceTopPadding,
                                            workspaceBottomPadding,
                                            hotseatBottomPadding
                                        )
                                        devicePaddings.add(dp)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failure parsing device padding layout.", e)
            }
        }

        // Sort ascending by maxEmptySpacePx
        devicePaddings.sortBy { it.maxEmptySpacePx }
    }

    open fun getDevicePadding(extraSpacePx: Int): DevicePadding? {
        if (devicePaddings.isEmpty()) return null
        for (limit in devicePaddings) {
            if (extraSpacePx <= limit.maxEmptySpacePx) {
                return limit
            }
        }
        return devicePaddings.last()
    }

    /**
     * Holds all the formulas to calculate padding based on extra space.
     */
    class DevicePadding(
        val maxEmptySpacePx: Int,
        private val workspaceTopPadding: PaddingFormula,
        private val workspaceBottomPadding: PaddingFormula,
        private val hotseatBottomPadding: PaddingFormula
    ) {
        fun getWorkspaceTopPadding(extraSpacePx: Int): Int =
            workspaceTopPadding.calculate(extraSpacePx)

        fun getWorkspaceBottomPadding(extraSpacePx: Int): Int =
            workspaceBottomPadding.calculate(extraSpacePx)

        fun getHotseatBottomPadding(extraSpacePx: Int): Int =
            hotseatBottomPadding.calculate(extraSpacePx)

        fun isValid(): Boolean {
            val top = getWorkspaceTopPadding(maxEmptySpacePx)
            val bottom = getWorkspaceBottomPadding(maxEmptySpacePx)
            val hotseat = getHotseatBottomPadding(maxEmptySpacePx)
            val sum = top + bottom + hotseat
            val diff = abs(sum - maxEmptySpacePx)
            return diff <= ROUNDING_THRESHOLD_PX
        }

        companion object {
            private const val ROUNDING_THRESHOLD_PX = 3
        }
    }

    class PaddingFormula {
        val a: Float
        val b: Float
        val c: Float

        constructor(context: Context, attrs: AttributeSet) {
            val t = context.obtainStyledAttributes(attrs, R.styleable.DevicePaddingFormula)
            a = getValue(t, R.styleable.DevicePaddingFormula_a)
            b = getValue(t, R.styleable.DevicePaddingFormula_b)
            c = getValue(t, R.styleable.DevicePaddingFormula_c)
            t.recycle()
        }

        constructor(a: Float = 0f, b: Float = 0f, c: Float = 0f) {
            this.a = a
            this.b = b
            this.c = c
        }

        fun calculate(extraSpacePx: Int): Int {
            return (a * (extraSpacePx - c) + b).roundToInt()
        }

        private companion object {
            fun getValue(a: TypedArray, index: Int): Float {
                return when (a.getType(index)) {
                    TypedValue.TYPE_DIMENSION -> a.getDimensionPixelSize(index, 0).toFloat()
                    TypedValue.TYPE_FLOAT -> a.getFloat(index, 0f)
                    else -> 0f
                }
            }
        }
    }

    companion object {
        private const val DEVICE_PADDINGS = "device-paddings"
        private const val DEVICE_PADDING = "device-padding"
        private const val WORKSPACE_TOP_PADDING = "workspaceTopPadding"
        private const val WORKSPACE_BOTTOM_PADDING = "workspaceBottomPadding"
        private const val HOTSEAT_BOTTOM_PADDING = "hotseatBottomPadding"
        private const val TAG = "DevicePaddings"
    }
}
