/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.graphics.Point
import android.graphics.Rect

data class AllAppsProfile(
    val borderSpacePx: Point = Point(0, 0),
    val cellHeightPx: Int = 100,
    val iconSizePx: Int = 48,
    val iconTextSizePx: Float = 12f,
    val iconDrawablePaddingPx: Int = 8,
    val maxAllAppsTextLineCount: Int = 1,
    val cellWidthPx: Int = 80,
    val openDuration: Int = 300,
    val closeDuration: Int = 250,
    val leftRightMargin: Int = 0,
    val padding: Rect = Rect(),
    val shiftRange: Int = 0,
    val numShownAllAppsColumns: Int = 4,
)
