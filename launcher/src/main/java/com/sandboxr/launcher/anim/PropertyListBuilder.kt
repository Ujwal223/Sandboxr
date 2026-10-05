/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.anim

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import java.util.ArrayList

/**
 * Helper class to build a list of [PropertyValuesHolder] for view properties.
 */
open class PropertyListBuilder {

    private val properties = ArrayList<PropertyValuesHolder>()

    fun translationX(value: Float): PropertyListBuilder {
        properties.add(PropertyValuesHolder.ofFloat(View.TRANSLATION_X, value))
        return this
    }

    fun translationY(value: Float): PropertyListBuilder {
        properties.add(PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, value))
        return this
    }

    fun translationZ(value: Float): PropertyListBuilder {
        properties.add(PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, value))
        return this
    }

    fun scaleX(value: Float): PropertyListBuilder {
        properties.add(PropertyValuesHolder.ofFloat(View.SCALE_X, value))
        return this
    }

    fun scaleY(value: Float): PropertyListBuilder {
        properties.add(PropertyValuesHolder.ofFloat(View.SCALE_Y, value))
        return this
    }

    fun scale(value: Float): PropertyListBuilder {
        return scaleX(value).scaleY(value)
    }

    fun alpha(value: Float): PropertyListBuilder {
        properties.add(PropertyValuesHolder.ofFloat(View.ALPHA, value))
        return this
    }

    fun build(view: View): ObjectAnimator {
        return ObjectAnimator.ofPropertyValuesHolder(view, *properties.toTypedArray())
    }
}
