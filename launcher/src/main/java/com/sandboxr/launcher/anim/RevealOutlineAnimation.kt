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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider

/**
 * A [ViewOutlineProvider] that provides helper functions to create reveal animations.
 */
abstract class RevealOutlineAnimation : ViewOutlineProvider() {

    @JvmField
    protected var mOutline: Rect = Rect()

    @JvmField
    protected var mOutlineRadius: Float = 0f

    abstract fun shouldRemoveElevationDuringAnimation(): Boolean

    abstract fun setProgress(progress: Float)

    @JvmOverloads
    fun createRevealAnimator(revealView: View, isReversed: Boolean, startProgress: Float = 0f): ValueAnimator {
        val va = if (isReversed) {
            ValueAnimator.ofFloat(1f - startProgress, 0f)
        } else {
            ValueAnimator.ofFloat(startProgress, 1f)
        }
        val elevation = revealView.elevation

        va.addListener(object : AnimatorListenerAdapter() {
            private var isClippedToOutline: Boolean = false
            private var oldOutlineProvider: ViewOutlineProvider? = null

            override fun onAnimationStart(animation: Animator) {
                isClippedToOutline = revealView.clipToOutline
                oldOutlineProvider = revealView.outlineProvider

                revealView.outlineProvider = this@RevealOutlineAnimation
                revealView.clipToOutline = true
                if (shouldRemoveElevationDuringAnimation()) {
                    revealView.translationZ = -elevation
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                revealView.outlineProvider = oldOutlineProvider
                revealView.clipToOutline = isClippedToOutline
                if (shouldRemoveElevationDuringAnimation()) {
                    revealView.translationZ = 0f
                }
            }
        })

        va.addUpdateListener { v ->
            val progress = (v.animatedValue as Number).toFloat()
            setProgress(progress)
            revealView.invalidateOutline()
        }
        return va
    }

    override fun getOutline(v: View, outline: Outline) {
        outline.setRoundRect(mOutline, mOutlineRadius)
    }

    val radius: Float
        get() = mOutlineRadius

    fun getOutline(out: Rect) {
        out.set(mOutline)
    }
}
