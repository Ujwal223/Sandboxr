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

package com.sandboxr.launcher.touch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.InputDevice
import android.view.MotionEvent
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherAnimUtils
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.MotionEventsUtils.isTrackpadMultiFingerSwipe
import com.sandboxr.launcher.MotionEventsUtils.isTrackpadScroll
import com.sandboxr.launcher.Utilities
import com.sandboxr.launcher.anim.AnimatorPlaybackController
import com.sandboxr.launcher.anim.Interpolators
import com.sandboxr.launcher.states.StateAnimationConfig
import com.sandboxr.launcher.util.FlingBlockCheck
import com.sandboxr.launcher.util.TouchController
import com.sandboxr.launcher.util.window.RefreshRateTracker

/**
 * TouchController for handling swipe-driven state changes across Launcher states (e.g. NORMAL <-> ALL_APPS).
 */
abstract class AbstractStateChangeTouchController(
    @JvmField protected val mLauncher: Launcher,
    dir: SingleAxisSwipeDetector.Direction
) : TouchController, SingleAxisSwipeDetector.Listener {

    @JvmField
    protected val mDetector: SingleAxisSwipeDetector = SingleAxisSwipeDetector(mLauncher, this, dir)
    @JvmField
    protected val mSwipeDirection: SingleAxisSwipeDetector.Direction = dir

    protected val mClearStateOnCancelListener = object : AnimatorListenerAdapter() {
        override fun onAnimationCancel(animation: Animator) {
            clearState()
        }
    }

    private val mFlingBlockCheck = FlingBlockCheck()

    @JvmField protected var mStartState: LauncherState? = null
    @JvmField protected var mFromState: LauncherState? = null
    @JvmField protected var mToState: LauncherState? = null
    @JvmField protected var mCurrentAnimation: AnimatorPlaybackController? = null
    @JvmField protected var mGoingBetweenStates = true
    @JvmField protected var mProgressMultiplier = 0f
    @JvmField protected var mIsTrackpadReverseScroll = false

    private var mNoIntercept = false
    private var mStartProgress = 0f
    private var mDisplacementShift = 0f
    private var mCanBlockFling = false

    protected abstract fun canInterceptTouch(ev: MotionEvent): Boolean

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            mNoIntercept = !canInterceptTouch(ev)
            if (mNoIntercept) {
                return false
            }

            mIsTrackpadReverseScroll = isTrackpadScroll(ev)

            val directionsToDetectScroll: Int
            val ignoreSlopWhenSettling: Boolean

            if (mCurrentAnimation != null) {
                directionsToDetectScroll = SingleAxisSwipeDetector.DIRECTION_BOTH
                ignoreSlopWhenSettling = true
            } else {
                directionsToDetectScroll = getSwipeDirection()
                val ignoreMouseScroll = ev.isFromSource(InputDevice.SOURCE_MOUSE) &&
                        !isTrackpadScroll(ev) &&
                        !isTrackpadMultiFingerSwipe(ev)

                if (directionsToDetectScroll == 0 || ignoreMouseScroll) {
                    mNoIntercept = true
                    return false
                }
                ignoreSlopWhenSettling = false
            }
            mDetector.setDetectableScrollConditions(directionsToDetectScroll, ignoreSlopWhenSettling)
        }

        if (mNoIntercept) {
            return false
        }

        onControllerTouchEvent(ev)
        return mDetector.isDraggingOrSettling()
    }

    private fun getSwipeDirection(): Int {
        val fromState = mLauncher.getStateManager().state
        var swipeDirection = 0
        if (getTargetState(fromState, true /* isDragTowardPositive */) != fromState) {
            swipeDirection = swipeDirection or SingleAxisSwipeDetector.DIRECTION_POSITIVE
        }
        if (getTargetState(fromState, false /* isDragTowardPositive */) != fromState) {
            swipeDirection = swipeDirection or SingleAxisSwipeDetector.DIRECTION_NEGATIVE
        }
        return swipeDirection
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        return mDetector.onTouchEvent(ev)
    }

    protected open fun getShiftRange(): Float {
        return (mLauncher.getAllAppsController()?.shiftRange ?: 0f).coerceAtLeast(
            mLauncher.getDeviceProfile().heightPx.toFloat()
        )
    }

    protected abstract fun getTargetState(
        fromState: LauncherState,
        isDragTowardPositive: Boolean
    ): LauncherState

    protected abstract fun initCurrentAnimation(): Float

    protected open fun reinitCurrentAnimation(reachedToState: Boolean, isDragTowardPositive: Boolean): Boolean {
        val newFromState = if (mFromState == null) {
            mLauncher.getStateManager().state
        } else if (reachedToState) {
            mToState ?: mLauncher.getStateManager().state
        } else {
            mFromState ?: mLauncher.getStateManager().state
        }
        val newToState = getTargetState(newFromState, isDragTowardPositive)

        if ((newFromState == mFromState && newToState == mToState) || (newFromState == newToState)) {
            return false
        }

        mFromState = newFromState
        mToState = newToState

        mStartProgress = 0f
        mCurrentAnimation?.target?.removeListener(mClearStateOnCancelListener)
        mProgressMultiplier = initCurrentAnimation()
        mCurrentAnimation?.dispatchOnStart()
        return true
    }

    override fun onDragStart(start: Boolean, startDisplacement: Float) {
        mStartState = mLauncher.getStateManager().state

        if (mCurrentAnimation == null) {
            mFromState = mStartState
            mToState = null
            cancelAnimationControllers()
            reinitCurrentAnimation(false, mDetector.wasInitialTouchPositive())
            mDisplacementShift = 0f
        } else {
            mCurrentAnimation?.pause()
            mStartProgress = mCurrentAnimation?.progressFraction ?: 0f
        }
        mCanBlockFling = mFromState == LauncherState.NORMAL
        mFlingBlockCheck.unblockFling()
    }

    override fun onDrag(displacement: Float): Boolean {
        val deltaProgress = mProgressMultiplier * (displacement - mDisplacementShift)
        val progress = deltaProgress + mStartProgress
        updateProgress(progress)
        val isDragTowardPositive = mSwipeDirection.isPositive(displacement - mDisplacementShift)

        if (progress <= 0f) {
            if (reinitCurrentAnimation(false, isDragTowardPositive)) {
                mDisplacementShift = displacement
                if (mCanBlockFling) {
                    mFlingBlockCheck.blockFling()
                }
            }
        } else if (progress >= 1f) {
            if (reinitCurrentAnimation(true, isDragTowardPositive)) {
                mDisplacementShift = displacement
                if (mCanBlockFling) {
                    mFlingBlockCheck.blockFling()
                }
            }
        } else {
            mFlingBlockCheck.onEvent()
        }

        return true
    }

    override fun onDrag(displacement: Float, event: MotionEvent): Boolean {
        var disp = displacement
        if (mIsTrackpadReverseScroll && mStartState == LauncherState.NORMAL) {
            disp = -disp
        }
        return onDrag(disp)
    }

    protected open fun updateProgress(fraction: Float) {
        mCurrentAnimation?.setPlayFraction(fraction)
    }

    protected open fun getConfigForStates(
        fromState: LauncherState,
        toState: LauncherState
    ): StateAnimationConfig {
        return StateAnimationConfig()
    }

    override fun onDragEnd(velocity: Float) {
        val currentAnim = mCurrentAnimation ?: return

        var vel = velocity
        if (mIsTrackpadReverseScroll && mStartState == LauncherState.NORMAL) {
            vel = -vel
        }
        var fling = mDetector.isFling(vel)
        val blockedFling = fling && mFlingBlockCheck.isBlocked()
        if (blockedFling) {
            fling = false
        }

        val progress = currentAnim.progressFraction
        val progressVelocity = vel * mProgressMultiplier
        val interpolatedProgress = currentAnim.getInterpolatedProgress()

        val targetState: LauncherState = if (fling) {
            if (Math.signum(vel) == Math.signum(mProgressMultiplier)) {
                mToState ?: mFromState ?: LauncherState.NORMAL
            } else {
                mFromState ?: LauncherState.NORMAL
            }
        } else {
            val successThreshold = if (mLauncher.getDeviceProfile().isTablet &&
                (mToState == LauncherState.ALL_APPS || mFromState == LauncherState.ALL_APPS)
            ) {
                LauncherAnimUtils.TABLET_BOTTOM_SHEET_SUCCESS_TRANSITION_PROGRESS
            } else {
                LauncherAnimUtils.SUCCESS_TRANSITION_PROGRESS
            }
            if (interpolatedProgress > successThreshold) {
                mToState ?: mFromState ?: LauncherState.NORMAL
            } else {
                mFromState ?: LauncherState.NORMAL
            }
        }

        val endProgress: Float
        val startProgress: Float
        val duration: Long
        val durationMultiplier = if (blockedFling && targetState == mFromState) {
            LauncherAnimUtils.blockedFlingDurationFactor(vel)
        } else {
            1
        }

        val singleFrameMs = RefreshRateTracker.getSingleFrameMs(mLauncher).toFloat()

        if (targetState == mToState) {
            endProgress = 1f
            if (progress >= 1f) {
                duration = 0
                startProgress = 1f
            } else {
                startProgress = Utilities.boundToRange(progress + progressVelocity * singleFrameMs, 0f, 1f)
                duration = BaseSwipeDetector.calculateDuration(vel, endProgress - Math.max(progress, 0f)) * durationMultiplier
            }
        } else {
            endProgress = 0f
            if (progress <= 0f) {
                duration = 0
                startProgress = 0f
            } else {
                startProgress = Utilities.boundToRange(progress + progressVelocity * singleFrameMs, 0f, 1f)
                duration = BaseSwipeDetector.calculateDuration(vel, Math.min(progress, 1f) - endProgress) * durationMultiplier
            }
            currentAnim.target.removeListener(mClearStateOnCancelListener)
            currentAnim.dispatchOnCancel()
        }

        currentAnim.setEndAction { onSwipeInteractionCompleted(targetState) }
        val anim = currentAnim.animationPlayer
        anim.setFloatValues(startProgress, endProgress)
        updateSwipeCompleteAnimation(anim, duration, targetState, vel, fling)
        currentAnim.dispatchOnStart()
        anim.start()
    }

    protected open fun updateSwipeCompleteAnimation(
        animator: ValueAnimator,
        expectedDuration: Long,
        targetState: LauncherState,
        velocity: Float,
        isFling: Boolean
    ) {
        animator.duration = expectedDuration
        animator.interpolator = Interpolators.scrollInterpolatorForVelocity(velocity)
    }

    protected open fun onSwipeInteractionCompleted(targetState: LauncherState) {
        clearState()
        val shouldGoToTargetState = mGoingBetweenStates || (mToState != targetState)
        if (shouldGoToTargetState) {
            goToTargetState(targetState)
        }
    }

    protected open fun goToTargetState(targetState: LauncherState) {
        if (!mLauncher.isInState(targetState)) {
            mLauncher.getStateManager().goToState(targetState, false /* animated */)
        }
    }

    protected open fun clearState() {
        cancelAnimationControllers()
        mGoingBetweenStates = true
        mDetector.finishedScrolling()
        mDetector.setDetectableScrollConditions(0, false)
        mIsTrackpadReverseScroll = false
    }

    private fun cancelAnimationControllers() {
        mCurrentAnimation = null
    }

    protected fun shouldOpenAllApps(isDragTowardPositive: Boolean): Boolean {
        return (isDragTowardPositive && !mIsTrackpadReverseScroll) ||
                (!isDragTowardPositive && mIsTrackpadReverseScroll)
    }
}
