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

package com.sandboxr.launcher.splitscreen

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.IntDef
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Constants and data models defining Split Screen positions and parameters.
 */
object SplitConfigurationOptions {
    const val STAGE_POSITION_UNDEFINED = -1
    const val STAGE_POSITION_TOP_OR_LEFT = 0
    const val STAGE_POSITION_BOTTOM_OR_RIGHT = 1

    const val STAGE_TYPE_MAIN = 0
    const val STAGE_TYPE_SIDE = 1

    const val DEFAULT_SPLIT_RATIO = 0.5f

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(STAGE_POSITION_UNDEFINED, STAGE_POSITION_TOP_OR_LEFT, STAGE_POSITION_BOTTOM_OR_RIGHT)
    annotation class StagePosition

    data class SplitPositionOption(
        @StagePosition val stagePosition: Int,
        val iconResId: Int = 0,
        val textResId: Int = 0,
    )

    data class SplitSelectSource(
        val view: View? = null,
        val drawable: Drawable? = null,
        val intent: Intent? = null,
        val position: SplitPositionOption? = null,
        val itemInfo: ItemInfo? = null,
    )
}
