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

package com.sandboxr.launcher.anim

import androidx.dynamicanimation.animation.SpringForce

/**
 * Configuration holder for spring-based animation parameters.
 */
open class SpringProperty @JvmOverloads constructor(
    @JvmField val flags: Int = 0
) {

    @JvmField
    var dampingRatio: Float = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY

    @JvmField
    var stiffness: Float = SpringForce.STIFFNESS_MEDIUM

    fun setDampingRatio(dampingRatio: Float): SpringProperty {
        this.dampingRatio = dampingRatio
        return this
    }

    fun setStiffness(stiffness: Float): SpringProperty {
        this.stiffness = stiffness
        return this
    }

    companion object {
        const val FLAG_CAN_SPRING_ON_END = 1 shl 0
        const val FLAG_CAN_SPRING_ON_START = 1 shl 1

        @JvmField
        val DEFAULT = SpringProperty()
    }
}
