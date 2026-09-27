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

package com.sandboxr.launcher.statemanager

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import androidx.annotation.FloatRange
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.LauncherUiState
import com.sandboxr.launcher.anim.AnimationSuccessListener
import com.sandboxr.launcher.anim.AnimatorPlaybackController
import com.sandboxr.launcher.anim.PendingAnimation
import com.sandboxr.launcher.states.StateAnimationConfig
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Class to manage transitions, animations, and predictive gestures between states for a [StatefulContainer].
 */
open class StateManager<S : BaseState<S>, T : StatefulContainer<S>>(
    val container: T,
    val baseState: S
) {

    private val uiHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<StateListener<S>>()
    private val atomicAnimationFactory: AtomicAnimationFactory<S> = container.createAtomicAnimationFactory()

    private var stateHandlersCache: Array<StateHandler<S>>? = null

    @Volatile
    var state: S = baseState
        protected set

    @Volatile
    var targetState: S = baseState
        protected set

    @Volatile
    var lastStableState: S = baseState
        protected set

    @Volatile
    var currentStableState: S = baseState
        protected set

    var restState: S = baseState

    var launcherUiState: LauncherUiState? = null
        set(value) {
            field = value
            if (state is LauncherState) {
                value?.launcherState = state as LauncherState
            }
        }

    private var currentAnimation: AnimatorSet? = null
    private var playbackController: AnimatorPlaybackController? = null
    private var changeId: Int = 0

    fun getStateHandlers(): Array<StateHandler<S>> {
        if (stateHandlersCache == null) {
            val list = mutableListOf<StateHandler<S>>()
            container.collectStateHandlers(list)
            @Suppress("UNCHECKED_CAST")
            stateHandlersCache = list.toTypedArray()
        }
        return stateHandlersCache!!
    }

    fun addStateListener(listener: StateListener<S>) {
        listeners.add(listener)
    }

    fun removeStateListener(listener: StateListener<S>) {
        listeners.remove(listener)
    }

    fun shouldAnimateStateChange(): Boolean = container.shouldAnimateStateChange()

    fun isInStableState(queryState: S): Boolean {
        return state == queryState && currentStableState == queryState && currentAnimation == null
    }

    fun isInTransition(): Boolean = currentAnimation != null || playbackController != null

    @JvmOverloads
    fun goToState(state: S, animated: Boolean = shouldAnimateStateChange(), listener: Animator.AnimatorListener? = null) {
        goToState(state, animated, 0L, listener)
    }

    fun goToState(state: S, listener: Animator.AnimatorListener?) {
        goToState(state, shouldAnimateStateChange(), 0L, listener)
    }

    fun goToState(state: S, delay: Long, listener: Animator.AnimatorListener? = null) {
        goToState(state, true, delay, listener)
    }

    fun goToState(state: S, animated: Boolean, delay: Long, listener: Animator.AnimatorListener?) {
        val animEnabled = animated && ValueAnimator.areAnimatorsEnabled()

        if (this.state == state) {
            if (currentAnimation == null && playbackController == null) {
                listener?.onAnimationEnd(AnimatorSet())
                onRepeatStateSetAborted(state)
                return
            } else if (targetState == state || state.shouldPreserveDataStateOnReapply()) {
                if (listener != null) {
                    currentAnimation?.addListener(listener)
                }
                onRepeatStateSetAborted(state)
                return
            }
        }

        val fromState = this.state
        cancelAnimation()
        changeId++

        if (!animEnabled) {
            atomicAnimationFactory.cancelAllStateElementAnimation()
            onStateTransitionStart(state)
            for (handler in getStateHandlers()) {
                handler.setState(state)
            }
            onStateTransitionEnd(state)
            listener?.onAnimationEnd(AnimatorSet())
            return
        }

        if (delay > 0) {
            val startChangeId = changeId
            uiHandler.postDelayed({
                if (changeId == startChangeId) {
                    goToStateAnimated(state, fromState, listener)
                }
            }, delay)
        } else {
            goToStateAnimated(state, fromState, listener)
        }
    }

    private fun goToStateAnimated(state: S, fromState: S, listener: Animator.AnimatorListener?) {
        val duration = if (state == baseState) {
            fromState.getTransitionDuration(container, false).toLong()
        } else {
            state.getTransitionDuration(container, true).toLong()
        }

        val config = StateAnimationConfig()
        config.duration = duration
        prepareForAtomicAnimation(fromState, state, config)

        val pendingAnimation = PendingAnimation(duration)
        if (!config.hasAnimationFlag(StateAnimationConfig.SKIP_ALL_ANIMATIONS)) {
            for (handler in getStateHandlers()) {
                handler.setStateWithAnimation(state, config, pendingAnimation)
            }
        }

        pendingAnimation.addListener(createStateAnimationListener(state))
        val animSet = pendingAnimation.buildAnim()
        if (listener != null) {
            animSet.addListener(listener)
        }

        currentAnimation = animSet
        targetState = state

        uiHandler.post {
            animSet.start()
        }
    }

    private fun createStateAnimationListener(toState: S): Animator.AnimatorListener {
        return object : AnimationSuccessListener() {
            override fun onAnimationStart(animation: Animator) {
                onStateTransitionStart(toState)
            }

            override fun onAnimationSuccess(animator: Animator) {
                onStateTransitionEnd(toState)
            }
        }
    }

    fun prepareForAtomicAnimation(fromState: S, toState: S, config: StateAnimationConfig) {
        atomicAnimationFactory.prepareForAtomicAnimation(fromState, toState, config)
    }

    fun createAtomicAnimation(fromState: S, toState: S, config: StateAnimationConfig): AnimatorSet {
        val builder = PendingAnimation(config.duration)
        prepareForAtomicAnimation(fromState, toState, config)
        for (handler in getStateHandlers()) {
            handler.setStateWithAnimation(toState, config, builder)
        }
        return builder.buildAnim()
    }

    @JvmOverloads
    fun createAnimationToNewWorkspace(state: S, duration: Long, animFlags: Int = 0): AnimatorPlaybackController {
        val config = StateAnimationConfig()
        config.duration = duration
        config.animFlags = animFlags
        return createAnimationToNewWorkspace(state, config)
    }

    fun createAnimationToNewWorkspace(state: S, config: StateAnimationConfig): AnimatorPlaybackController {
        config.animProps = config.animProps or StateAnimationConfig.USER_CONTROLLED
        cancelAnimation()

        val builder = PendingAnimation(config.duration)
        if (!config.hasAnimationFlag(StateAnimationConfig.SKIP_ALL_ANIMATIONS)) {
            for (handler in getStateHandlers()) {
                handler.setStateWithAnimation(state, config, builder)
            }
        }
        builder.addListener(createStateAnimationListener(state))
        val controller = builder.createPlaybackController()
        playbackController = controller
        targetState = state
        return controller
    }

    private fun onStateTransitionStart(toState: S) {
        state = toState
        launcherUiState?.let { uiState ->
            if (toState is LauncherState) {
                uiState.launcherState = toState
            }
        }
        container.onStateSetStart(toState)
        for (listener in listeners) {
            listener.onStateTransitionStart(toState)
        }
    }

    private fun onStateTransitionEnd(finalState: S) {
        if (finalState != currentStableState) {
            lastStableState = finalState.getHistoryForState(currentStableState)
            currentStableState = finalState
        }

        currentAnimation = null
        playbackController = null

        container.onStateSetEnd(finalState)
        if (finalState == baseState) {
            restState = baseState
        }

        announceStateForAccessibility(finalState)

        for (listener in listeners) {
            listener.onStateTransitionComplete(finalState)
        }
    }

    private fun announceStateForAccessibility(state: S) {
        if (state is LauncherState && !state.hasFlag(LauncherState.FLAG_SKIP_STATE_ANNOUNCEMENT)) {
            val rootView = (container as? StatefulActivity<*>)?.getRootView()
            rootView?.announceForAccessibility(state.toString())
        }
    }

    private fun onRepeatStateSetAborted(state: S) {
        container.onRepeatStateSetAborted(state)
    }

    fun onBackStarted(toState: S) {
        for (handler in getStateHandlers()) {
            handler.onBackStarted(toState)
        }
    }

    fun onBackProgressed(toState: S, @FloatRange(from = 0.0, to = 1.0) backProgress: Float) {
        for (handler in getStateHandlers()) {
            handler.onBackProgressed(toState, backProgress)
        }
    }

    fun onBackCancelled(toState: S) {
        for (handler in getStateHandlers()) {
            handler.onBackCancelled(toState)
        }
    }

    @JvmOverloads
    fun reapplyState(cancelCurrentAnimation: Boolean = false) {
        val wasInAnimation = isInTransition()
        if (cancelCurrentAnimation) {
            if (state.shouldPreserveDataStateOnReapply() && currentAnimation != null) {
                currentAnimation?.end()
            }
            atomicAnimationFactory.cancelAllStateElementAnimation()
            cancelAnimation()
        }
        if (currentAnimation == null) {
            for (handler in getStateHandlers()) {
                handler.setState(state)
            }
            if (wasInAnimation) {
                onStateTransitionEnd(state)
            }
        }
    }

    @JvmOverloads
    fun moveToRestState(isAnimated: Boolean = shouldAnimateStateChange()) {
        if (playbackController != null) return
        if (state.shouldDisableRestore()) {
            goToState(restState, isAnimated)
            lastStableState = baseState
        }
    }

    fun cancelAnimation() {
        currentAnimation?.cancel()
        currentAnimation = null
        playbackController?.pause()
        playbackController = null
    }

    fun dump(prefix: String, writer: PrintWriter) {
        writer.println("${prefix}StateManager:")
        writer.println("$prefix\tmLastStableState: $lastStableState")
        writer.println("$prefix\tmCurrentStableState: $currentStableState")
        writer.println("$prefix\tmState: $state")
        writer.println("$prefix\tmRestState: $restState")
        writer.println("$prefix\tisInTransition: ${isInTransition()}")
    }

    interface StateListener<S> {
        fun onStateTransitionStart(toState: S) {}
        fun onStateTransitionComplete(finalState: S) {}
    }

    interface StateHandler<STATE_TYPE> {
        fun setState(state: STATE_TYPE)
        fun setStateWithAnimation(toState: STATE_TYPE, config: StateAnimationConfig, animation: PendingAnimation) {}
        fun onBackStarted(toState: STATE_TYPE) {}
        fun onBackProgressed(toState: STATE_TYPE, @FloatRange(from = 0.0, to = 1.0) backProgress: Float) {}
        fun onBackCancelled(toState: STATE_TYPE) {}
    }

    open class AtomicAnimationFactory<STATE_TYPE>(sharedElementAnimCount: Int = 0) {
        private val stateElementAnimators: Array<Animator?> = arrayOfNulls(sharedElementAnimCount)

        open fun cancelAllStateElementAnimation() {
            for (animator in stateElementAnimators) {
                animator?.cancel()
            }
        }

        open fun prepareForAtomicAnimation(fromState: STATE_TYPE, toState: STATE_TYPE, config: StateAnimationConfig) {}
    }
}
