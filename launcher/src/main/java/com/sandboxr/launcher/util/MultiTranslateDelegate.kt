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

package com.sandboxr.launcher.util

import android.util.FloatProperty
import android.view.View

/**
 * A utility class to split translation components for various workspace items.
 */
open class MultiTranslateDelegate @JvmOverloads constructor(
    target: View,
    countX: Int = COUNT,
    countY: Int = COUNT
) {
    private val mTranslationX: MultiPropertyFactory<View> =
        MultiPropertyFactory(target, VIEW_TRANSLATE_X, countX, { a, b -> a + b })
    private val mTranslationY: MultiPropertyFactory<View> =
        MultiPropertyFactory(target, VIEW_TRANSLATE_Y, countY, { a, b -> a + b })

    fun setTranslation(index: Int, x: Float, y: Float) {
        getTranslationX(index).setValue(x)
        getTranslationY(index).setValue(y)
    }

    fun getTranslationX(index: Int): MultiPropertyFactory<View>.MultiProperty {
        return mTranslationX[index]
    }

    fun getTranslationY(index: Int): MultiPropertyFactory<View>.MultiProperty {
        return mTranslationY[index]
    }

    companion object {
        const val INDEX_REORDER_BOUNCE_OFFSET = 0
        const val INDEX_REORDER_PREVIEW_OFFSET = 1
        const val INDEX_MOVE_FROM_CENTER_ANIM = 2
        const val INDEX_TASKBAR_ALIGNMENT_ANIM = 3
        const val INDEX_TASKBAR_REVEAL_ANIM = 4
        const val INDEX_TASKBAR_PINNING_ANIM = 5
        const val INDEX_NAV_BAR_ANIM = 6
        const val INDEX_BUBBLE_BAR_ANIM = 7
        const val INDEX_TASKBAR_APP_RUNNING_STATE_ANIM = 8
        const val INDEX_CELLAYOUT_MULTIPAGE_SPACING = 3
        const val INDEX_WIDGET_CENTERING = 4
        const val INDEX_BUBBLE_ADJUSTMENT_ANIM = 3
        const val COUNT = 9

        @JvmField
        val VIEW_TRANSLATE_X: FloatProperty<View> = object : FloatProperty<View>("translationX") {
            override fun setValue(target: View, value: Float) {
                target.translationX = value
            }
            override fun get(target: View): Float = target.translationX
        }

        @JvmField
        val VIEW_TRANSLATE_Y: FloatProperty<View> = object : FloatProperty<View>("translationY") {
            override fun setValue(target: View, value: Float) {
                target.translationY = value
            }
            override fun get(target: View): Float = target.translationY
        }
    }
}
