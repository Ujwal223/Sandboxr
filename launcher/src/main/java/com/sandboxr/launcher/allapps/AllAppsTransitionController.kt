/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.util.FloatProperty
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.Interpolator
import androidx.annotation.FloatRange
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.DeviceProfile.OnDeviceProfileChangeListener
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherAnimUtils
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.anim.AnimatedFloat
import com.sandboxr.launcher.anim.Interpolators
import com.sandboxr.launcher.anim.PendingAnimation
import com.sandboxr.launcher.anim.PropertySetter
import com.sandboxr.launcher.statemanager.StateManager
import com.sandboxr.launcher.states.StateAnimationConfig
import com.sandboxr.launcher.util.MultiPropertyFactory
import com.sandboxr.launcher.util.MultiValueAlpha
import com.sandboxr.launcher.views.ScrimView
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Handles All Apps drawer opening, closing, spring-loaded drag translation,
 * and state transitions between Workspace and All Apps.
 *
 * Progress semantics:
 * - 0f: fully open in All Apps
 * - 1f: fully closed at Workspace / Home
 */
class AllAppsTransitionController(
    private val launcher: Launcher
) : StateManager.StateHandler<LauncherState>, OnDeviceProfileChangeListener {

    private val listeners = CopyOnWriteArrayList<AllAppsTransitionListener>()

    private var appsView: ActivityAllAppsContainerView<Launcher>? = null
    private var scrimView: ScrimView? = null

    private var appsViewAlpha: MultiValueAlpha? = null
    private var appsViewTranslationY: MultiPropertyFactory<View>? = null

    val allAppScale = AnimatedFloat({ onScaleProgressChanged() }, 1f)
    private var shouldScaleHeader: Boolean = false
    private var searchBackAnimationListener: Animator.AnimatorListener? = null

    var isVerticalLayout: Boolean = false
        private set

    /**
     * Vertical translation distance in pixels.
     */
    var shiftRange: Float = 0f
        private set

    /**
     * Current vertical progress: 0f = open (All Apps), 1f = closed (Workspace).
     */
    var progress: Float = 1f
        private set

    init {
        val dp = launcher.getDeviceProfile()
        isVerticalLayout = dp.isVerticalBarLayout()
        shiftRange = dp.getAllAppsProfile().shiftRange.toFloat().coerceAtLeast(dp.heightPx.toFloat())
        allAppScale.value = 1f
        launcher.addOnDeviceProfileChangeListener(this)
    }

    fun addListener(listener: AllAppsTransitionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: AllAppsTransitionListener) {
        listeners.remove(listener)
    }

    fun setupViews(scrim: ScrimView?, apps: ActivityAllAppsContainerView<Launcher>?) {
        scrimView = scrim
        appsView = apps

        apps?.let { view ->
            view.scrimView = scrim
            appsViewAlpha = MultiValueAlpha(view, APPS_VIEW_INDEX_COUNT, View.GONE).apply {
                setUpdateVisibility(true)
            }
            appsViewTranslationY = MultiPropertyFactory(
                view,
                LauncherAnimUtils.VIEW_TRANSLATE_Y,
                APPS_VIEW_INDEX_COUNT,
                { a, b -> a + b }
            )
        }
    }

    /**
     * Sets vertical transition progress in [0f, 1f].
     */
    fun setProgress(value: Float) {
        val targetProgress = value.coerceIn(0f, 1f)
        if (targetProgress == progress) return
        progress = targetProgress

        val dp = launcher.getDeviceProfile()
        val effectiveShift = if (shiftRange > 0f) shiftRange else dp.heightPx.toFloat()
        val translationY = progress * effectiveShift

        appsViewTranslationY?.get(INDEX_APPS_VIEW_PROGRESS)?.setValue(translationY)
        if (appsViewTranslationY == null) {
            appsView?.translationY = translationY
        }

        // Modulate scrim darkness/progress (0 when progress=1, 1 when progress=0)
        val allAppsAlpha = (1f - progress).coerceIn(0f, 1f)
        scrimView?.let { scrim ->
            scrim.progress = allAppsAlpha
            if (allAppsAlpha > 0f && scrim.visibility != View.VISIBLE) {
                scrim.visibility = View.VISIBLE
            } else if (allAppsAlpha == 0f && scrim.visibility != View.GONE) {
                scrim.visibility = View.GONE
            }
        }

        // Modulate workspace scale and hotseat alpha
        launcher.getWorkspace()?.let { ws ->
            val scale = 0.95f + (0.05f * progress)
            ws.scaleX = scale
            ws.scaleY = scale
            ws.alpha = progress
        }
        launcher.getHotseat()?.let { hs ->
            hs.alpha = progress
            if (!isVerticalLayout) {
                hs.translationY = (1f - progress) * (effectiveShift * 0.25f)
            }
        }

        appsView?.let { view ->
            if (progress < 1f && view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
            } else if (progress >= 1f && view.visibility != View.GONE) {
                view.visibility = View.GONE
            }
        }

        launcher.onAllAppsTransition(1f - progress)
    }

    override fun setState(state: LauncherState) {
        setProgress(state.getVerticalProgress(launcher))
        setAlphas(state, StateAnimationConfig(), PropertySetter.NO_ANIM_PROPERTY_SETTER)
    }

    override fun setStateWithAnimation(
        toState: LauncherState,
        config: StateAnimationConfig,
        builder: PendingAnimation
    ) {
        val targetProgress = toState.getVerticalProgress(launcher)
        if (progress == targetProgress) {
            setAlphas(toState, config, builder)
            return
        }

        val toAllApps = (toState == LauncherState.ALL_APPS)
        for (listener in listeners) {
            listener.onAllAppsTransitionStart(toAllApps)
        }

        val interpolator: Interpolator = config.getInterpolator(
            StateAnimationConfig.ANIM_VERTICAL_PROGRESS,
            if (config.isUserControlled()) Interpolators.LINEAR else Interpolators.DECELERATE_1_7
        )

        val anim = createSpringAnimation(progress, targetProgress).apply {
            this.interpolator = interpolator
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    setProgress(targetProgress)
                    for (listener in this@AllAppsTransitionController.listeners) {
                        listener.onAllAppsTransitionEnd(toAllApps)
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    for (listener in this@AllAppsTransitionController.listeners) {
                        listener.onAllAppsTransitionEnd(toAllApps)
                    }
                }
            })
        }

        builder.add(anim)
        setAlphas(toState, config, builder)

        if (toAllApps && launcher.isInState(LauncherState.NORMAL)) {
            appsView?.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
    }

    fun createSpringAnimation(vararg progressValues: Float): Animator {
        return ObjectAnimator.ofFloat(this, ALL_APPS_PROGRESS, *progressValues)
    }

    fun setAlphas(state: LauncherState, config: StateAnimationConfig, setter: PropertySetter) {
        val elements = state.getVisibleElements(launcher)
        val hasAllAppsContent = (elements and LauncherState.ALL_APPS_CONTENT) != 0
        val allAppsFade = config.getInterpolator(StateAnimationConfig.ANIM_ALL_APPS_FADE, Interpolators.LINEAR)

        val targetAlpha = if (hasAllAppsContent) 1f else 0f
        appsViewAlpha?.let {
            setter.setFloat(
                it.get(INDEX_APPS_VIEW_PROGRESS),
                MultiPropertyFactory.MULTI_PROPERTY_VALUE,
                targetAlpha,
                allAppsFade
            )
        } ?: run {
            appsView?.let { view ->
                setter.setViewAlpha(view, targetAlpha, allAppsFade)
            }
        }

        val shouldProtectHeader = !config.hasAnimationFlag(StateAnimationConfig.SKIP_SCRIM)
            && (LauncherState.ALL_APPS == state || launcher.getStateManager().state == LauncherState.ALL_APPS)
        scrimView?.drawingController = if (shouldProtectHeader) appsView else null
    }

    override fun onDeviceProfileChanged(dp: DeviceProfile) {
        isVerticalLayout = dp.isVerticalBarLayout()
        shiftRange = dp.getAllAppsProfile().shiftRange.toFloat().coerceAtLeast(dp.heightPx.toFloat())

        if (isVerticalLayout) {
            launcher.getHotseat()?.translationY = 0f
            launcher.getWorkspace()?.getPageIndicator()?.translationY = 0f
        }
    }

    override fun onBackStarted(toState: LauncherState) {
        setShouldScaleHeader(appsView?.shouldBackExitSearch() != true)
    }

    override fun onBackProgressed(
        toState: LauncherState,
        @FloatRange(from = 0.0, to = 1.0) backProgress: Float
    ) {
        if (!launcher.isInState(LauncherState.ALL_APPS) || toState != LauncherState.NORMAL) {
            return
        }
        val minScale = 0.9f
        val scaleProgress = minScale + (1f - minScale) * (1f - backProgress)
        allAppScale.updateValue(scaleProgress)
    }

    private fun onScaleProgressChanged() {
        val scale = allAppScale.value
        appsView?.let { view ->
            view.scaleX = scale
            view.scaleY = scale
        }
        if (shouldScaleHeader) {
            scrimView?.scrimHeaderScale = scale
        }
    }

    fun setShouldScaleHeader(shouldScale: Boolean) {
        shouldScaleHeader = shouldScale
    }

    fun setAllAppsSearchBackAnimationListener(listener: Animator.AnimatorListener?) {
        searchBackAnimationListener = listener
    }

    fun animateAllAppsToNoScale() {
        if (allAppScale.isAnimating()) {
            allAppScale.cancelAnimation()
        }
        val anim = allAppScale.animateToValue(1f).apply {
            duration = REVERT_SWIPE_ALL_APPS_TO_HOME_ANIMATION_DURATION_MS
            searchBackAnimationListener?.let { addListener(it) }
        }
        anim.start()
    }

    companion object {
        const val INTERP_COEFF = 1.7f
        const val REVERT_SWIPE_ALL_APPS_TO_HOME_ANIMATION_DURATION_MS = 200L
        private const val INDEX_APPS_VIEW_PROGRESS = 0
        private const val APPS_VIEW_INDEX_COUNT = 1

        @JvmField
        val ALL_APPS_PROGRESS: FloatProperty<AllAppsTransitionController> =
            object : FloatProperty<AllAppsTransitionController>("allAppsProgress") {
                override fun get(controller: AllAppsTransitionController): Float {
                    return controller.progress
                }

                override fun setValue(controller: AllAppsTransitionController, value: Float) {
                    controller.setProgress(value)
                }
            }
    }
}
