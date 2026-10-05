/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewTreeObserver
import com.sandboxr.launcher.util.window.RefreshRateTracker

/**
 * Adjusts the current play time of an animation during the first two frames to prevent jank.
 */
open class FirstFrameAnimatorHelper(target: View) :
    ViewTreeObserver.OnDrawListener,
    View.OnAttachStateChangeListener {

    private var rootView: View? = null
    private var globalFrameCount: Long = 0L

    init {
        target.addOnAttachStateChangeListener(this)
        if (target.isAttachedToWindow) {
            onViewAttachedToWindow(target)
        }
    }

    fun <T : ValueAnimator> addTo(anim: T): T {
        anim.addUpdateListener(MyListener())
        return anim
    }

    override fun onDraw() {
        globalFrameCount++
    }

    override fun onViewAttachedToWindow(view: View) {
        rootView = view.rootView
        rootView?.viewTreeObserver?.addOnDrawListener(this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        rootView?.let {
            it.viewTreeObserver.removeOnDrawListener(this)
            rootView = null
        }
    }

    private inner class MyListener : ValueAnimator.AnimatorUpdateListener {
        private var startFrame: Long = 0L
        private var startTime: Long = -1L
        private var handlingOnAnimationUpdate: Boolean = false
        private var adjustedSecondFrameTime: Boolean = false

        override fun onAnimationUpdate(animation: ValueAnimator) {
            val currentTime = System.currentTimeMillis()
            if (startTime == -1L) {
                startFrame = globalFrameCount
                startTime = currentTime
            }

            val currentPlayTime = animation.currentPlayTime
            val isFinalFrame = animation.animatedFraction.compareTo(1f) == 0

            val currentRoot = rootView
            if (!handlingOnAnimationUpdate &&
                currentRoot != null &&
                currentRoot.windowVisibility == View.VISIBLE &&
                currentPlayTime < animation.duration &&
                !isFinalFrame
            ) {
                handlingOnAnimationUpdate = true
                val frameNum = globalFrameCount - startFrame

                if (frameNum == 0L && currentTime < startTime + MAX_DELAY && currentPlayTime > 0) {
                    currentRoot.invalidate()
                    animation.currentPlayTime = 0
                } else {
                    val singleFrameMs = RefreshRateTracker.getSingleFrameMs(currentRoot.context).toLong()
                    if (frameNum == 1L &&
                        currentTime < startTime + MAX_DELAY &&
                        !adjustedSecondFrameTime &&
                        currentTime > startTime + singleFrameMs &&
                        currentPlayTime > singleFrameMs
                    ) {
                        animation.currentPlayTime = singleFrameMs
                        adjustedSecondFrameTime = true
                    } else {
                        if (frameNum > 1L) {
                            currentRoot.post { animation.removeUpdateListener(this) }
                        }
                    }
                }
                handlingOnAnimationUpdate = false
            }
        }
    }

    companion object {
        private const val MAX_DELAY = 1000
    }
}
