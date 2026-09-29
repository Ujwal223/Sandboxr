/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.PathInterpolator
import androidx.annotation.Keep
import com.android.launcher3.AbstractFloatingView
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.R
import com.sandboxr.launcher.statemanager.StateManager
import com.sandboxr.launcher.util.OnboardingPrefs

/**
 * Floating view responsible for showing the swipe-up discovery bounce animation
 * on the hotseat to guide users towards All Apps.
 */
class DiscoveryBounce(
    private val launcher: Launcher
) : AbstractFloatingView(launcher) {

    private val discoBounceAnimation: Animator
    private val stateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            handleClose(false)
        }

        override fun onStateTransitionComplete(finalState: LauncherState) {}
    }

    init {
        val dragLayerHeight = (launcher.getDragLayer()?.height ?: 0).toFloat().coerceAtLeast(100f)
        val targetWrapper = VerticalProgressWrapper(launcher.getHotseat(), dragLayerHeight)

        val loadedAnim: Animator? = try {
            AnimatorInflater.loadAnimator(launcher, R.animator.discovery_bounce).apply {
                setTarget(targetWrapper)
            }
        } catch (e: Exception) {
            null
        }

        discoBounceAnimation = loadedAnim ?: createFallbackBounceAnimator(targetWrapper)
        discoBounceAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                handleClose(false)
            }
        })
        launcher.getStateManager().addStateListener(stateListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        discoBounceAnimation.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (discoBounceAnimation.isRunning) {
            discoBounceAnimation.end()
        }
    }

    override fun canHandleBack(): Boolean {
        close(false)
        return false
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        handleClose(false)
        return false
    }

    override fun handleClose(animate: Boolean) {
        if (mIsOpen) {
            mIsOpen = false
            launcher.getDragLayer()?.removeView(this)
            launcher.getHotseat()?.translationY = 0f
            launcher.getStateManager().removeStateListener(stateListener)
        }
    }

    override fun isOfType(type: Int): Boolean {
        return (type and TYPE_DISCOVERY_BOUNCE) != 0
    }

    fun show() {
        mIsOpen = true
        launcher.getDragLayer()?.addView(this)
    }

    private fun createFallbackBounceAnimator(target: VerticalProgressWrapper): Animator {
        val bounceInterpolator = PathInterpolator(0.35f, 0f, 0.5f, 1f)
        val pvh = PropertyValuesHolder.ofFloat("progress", 1f, 0.9738f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(target, pvh).apply {
            duration = 1000
            repeatCount = 1
            interpolator = bounceInterpolator
        }
    }

    /**
     * A wrapper around view translation allowing progress-based manipulation.
     */
    class VerticalProgressWrapper(
        private val view: View?,
        private val limit: Float
    ) {
        @get:Keep
        @set:Keep
        var progress: Float
            get() = if (view != null && limit != 0f) 1f + (view.translationY / limit) else 1f
            set(value) {
                view?.translationY = limit * (value - 1f)
            }
    }

    companion object {
        private const val DELAY_MS = 450L

        @JvmStatic
        fun showForHomeIfNeeded(launcher: Launcher) {
            showForHomeIfNeeded(launcher, true)
        }

        @JvmStatic
        fun showForHomeIfNeeded(launcher: Launcher, withDelay: Boolean) {
            if (!launcher.isInState(LauncherState.NORMAL)
                || OnboardingPrefs.HOME_BOUNCE_SEEN.get(launcher)
                || AbstractFloatingView.getTopOpenView(launcher) != null
            ) {
                return
            }

            val userManager = launcher.getSystemService(UserManager::class.java)
            if (userManager?.isDemoUser == true) {
                return
            }

            if (withDelay) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showForHomeIfNeeded(launcher, false)
                }, DELAY_MS)
                return
            }

            OnboardingPrefs.HOME_BOUNCE_COUNT.increment(launcher)
            DiscoveryBounce(launcher).show()
        }
    }
}
