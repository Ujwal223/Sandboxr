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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.FloatProperty
import android.util.IntProperty
import android.view.View

/**
 * Common property animators and utilities for Launcher animations.
 */
object LauncherAnimUtils {

    @JvmField
    val VIEW_ALPHA: FloatProperty<View> = object : FloatProperty<View>("alpha") {
        override fun setValue(target: View, value: Float) {
            target.alpha = value
        }

        override fun get(target: View): Float = target.alpha
    }

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

    @JvmField
    val VIEW_BACKGROUND_COLOR: IntProperty<View> = object : IntProperty<View>("backgroundColor") {
        override fun setValue(target: View, value: Int) {
            target.setBackgroundColor(value)
        }

        override fun get(target: View): Int {
            val bg = target.background
            return if (bg is ColorDrawable) bg.color else Color.TRANSPARENT
        }
    }
}
