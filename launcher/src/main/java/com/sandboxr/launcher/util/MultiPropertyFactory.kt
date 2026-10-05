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

import android.animation.Animator
import android.animation.ObjectAnimator
import android.util.FloatProperty
import java.io.PrintWriter
import java.util.Arrays

/**
 * Allows combining multiple values set by several sources on a single target property.
 */
open class MultiPropertyFactory<T> @JvmOverloads constructor(
    protected val target: T,
    private val property: FloatProperty<T>,
    size: Int,
    private val aggregator: FloatBiFunction,
    defaultPropertyValue: Float = 0f
) {

    fun interface FloatBiFunction {
        fun apply(a: Float, b: Float): Float
    }

    private val properties: Array<MultiProperty> = Array(size) { i ->
        MultiProperty(i, defaultPropertyValue)
    }

    private var aggregationOfOthers: Float = 0f
    private var lastIndexSet: Int = -1

    operator fun get(index: Int): MultiProperty {
        return properties[index]
    }

    override fun toString(): String {
        return Arrays.deepToString(properties)
    }

    fun dump(prefix: String, pw: PrintWriter, label: String, vararg alphaIndexLabels: String) {
        pw.println(prefix + label)
        val innerPrefix = "$prefix\t"
        for (i in alphaIndexLabels.indices) {
            if (i >= properties.size) {
                pw.println("$innerPrefix${alphaIndexLabels[i]} given for index $i but only ${properties.size} exist.")
                continue
            }
            pw.println("$innerPrefix${alphaIndexLabels[i]}=${get(i).value}")
        }
    }

    protected open fun apply(value: Float) {
        property.set(target, value)
    }

    inner class MultiProperty(
        val index: Int,
        private val defaultValue: Float
    ) {
        private var mValue: Float = defaultValue

        val value: Float
            get() = mValue

        fun setValue(newValue: Float) {
            if (lastIndexSet != index) {
                aggregationOfOthers = defaultValue
                for (other in properties) {
                    if (other.index != index) {
                        aggregationOfOthers = aggregator.apply(aggregationOfOthers, other.value)
                    }
                }
                lastIndexSet = index
            }
            val lastAggregatedValue = aggregator.apply(aggregationOfOthers, newValue)
            mValue = newValue
            apply(lastAggregatedValue)
        }

        fun animateToValue(targetValue: Float): Animator {
            val animator = ObjectAnimator.ofFloat(this, MULTI_PROPERTY_VALUE, targetValue)
            animator.setAutoCancel(true)
            return animator
        }

        override fun toString(): String = mValue.toString()
    }

    companion object {
        @JvmField
        val MULTI_PROPERTY_VALUE: FloatProperty<MultiPropertyFactory<*>.MultiProperty> =
            object : FloatProperty<MultiPropertyFactory<*>.MultiProperty>("value") {
                override fun get(obj: MultiPropertyFactory<*>.MultiProperty): Float = obj.value
                override fun setValue(obj: MultiPropertyFactory<*>.MultiProperty, value: Float) {
                    obj.setValue(value)
                }
            }
    }
}
