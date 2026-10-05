/*
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

package com.sandboxr.launcher.splitscreen

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator

/**
 * Organizes timing information for split screen animations.
 */
interface SplitAnimationTimings {

    val duration: Int
    val placeholderFadeInStart: Int
    val placeholderFadeInEnd: Int
    val placeholderIconFadeInStart: Int
    val placeholderIconFadeInEnd: Int
    val stagedRectSlideStart: Int
    val stagedRectSlideEnd: Int

    val stagedRectXInterpolator: Interpolator
        get() = STANDARD_INTERPOLATOR

    val stagedRectYInterpolator: Interpolator
        get() = STANDARD_INTERPOLATOR

    val stagedRectScaleXInterpolator: Interpolator
        get() = STANDARD_INTERPOLATOR

    val stagedRectScaleYInterpolator: Interpolator
        get() = STANDARD_INTERPOLATOR

    val placeholderFadeInStartOffset: Float
        get() = if (duration > 0) placeholderFadeInStart.toFloat() / duration else 0f

    val placeholderFadeInEndOffset: Float
        get() = if (duration > 0) placeholderFadeInEnd.toFloat() / duration else 0f

    val placeholderIconFadeInStartOffset: Float
        get() = if (duration > 0) placeholderIconFadeInStart.toFloat() / duration else 0f

    val placeholderIconFadeInEndOffset: Float
        get() = if (duration > 0) placeholderIconFadeInEnd.toFloat() / duration else 0f

    val stagedRectSlideStartOffset: Float
        get() = if (duration > 0) stagedRectSlideStart.toFloat() / duration else 0f

    val stagedRectSlideEndOffset: Float
        get() = if (duration > 0) stagedRectSlideEnd.toFloat() / duration else 0f

    val desktopFadeSplitAnimationEndOffset: Float
        get() = if (duration > 0) DESKTOP_FADE_OUT_DURATION.toFloat() / duration else 0f

    val gridSlideStartOffset: Float get() = 0f
    val gridSlideStaggerOffset: Float get() = 0f
    val gridSlideDurationOffset: Float get() = 0f
    val actionsFadeStartOffset: Float get() = 0f
    val actionsFadeEndOffset: Float get() = 0f
    val iconFadeStartOffset: Float get() = 0f
    val iconFadeEndOffset: Float get() = 0f
    val instructionsContainerFadeInStartOffset: Float get() = 0f
    val instructionsContainerFadeInEndOffset: Float get() = 0f
    val instructionsTextFadeInStartOffset: Float get() = 0f
    val instructionsTextFadeInEndOffset: Float get() = 0f
    val instructionsUnfoldStartOffset: Float get() = 0f
    val instructionsUnfoldEndOffset: Float get() = 0f
    val gridSlidePrimaryInterpolator: Interpolator get() = LINEAR_INTERPOLATOR
    val gridSlideSecondaryInterpolator: Interpolator get() = LINEAR_INTERPOLATOR
    val desktopTaskFadeInterpolator: Interpolator get() = LINEAR_INTERPOLATOR
    val scrimFadeInStartOffset: Float get() = 0f
    val scrimFadeInEndOffset: Float get() = 0f
    val instructionsFadeStartOffset: Float get() = 0f
    val instructionsFadeEndOffset: Float get() = 0f
    val cellSplitStartOffset: Float get() = 0f
    val cellSplitEndOffset: Float get() = 0f
    val appRevealStartOffset: Float get() = 0f
    val appRevealEndOffset: Float get() = 0f
    val cellSplitInterpolator: Interpolator get() = LINEAR_INTERPOLATOR
    val iconFadeInterpolator: Interpolator get() = LINEAR_INTERPOLATOR
    val desktopTaskScaleInterpolator: Interpolator get() = STANDARD_INTERPOLATOR

    companion object {
        const val TABLET_ENTER_DURATION = 866
        const val TABLET_CONFIRM_DURATION = 500
        const val PHONE_ENTER_DURATION = 517
        const val PHONE_CONFIRM_DURATION = 333
        const val ABORT_DURATION = 500
        const val TABLET_APP_PAIR_LAUNCH_DURATION = 998
        const val PHONE_APP_PAIR_LAUNCH_DURATION = 915
        const val DESKTOP_FADE_OUT_DURATION = 200

        val LINEAR_INTERPOLATOR: Interpolator = LinearInterpolator()
        val STANDARD_INTERPOLATOR: Interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)

        val PHONE_OVERVIEW_TO_SPLIT: SplitAnimationTimings = PhoneOverviewToSplitTimings()
        val PHONE_SPLIT_TO_CONFIRM: SplitAnimationTimings = PhoneSplitToConfirmTimings()
        val TABLET_OVERVIEW_TO_SPLIT: SplitAnimationTimings = TabletOverviewToSplitTimings()
        val TABLET_SPLIT_TO_CONFIRM: SplitAnimationTimings = TabletSplitToConfirmTimings()
    }
}

open class PhoneOverviewToSplitTimings : SplitAnimationTimings {
    override val duration: Int = SplitAnimationTimings.PHONE_ENTER_DURATION
    override val placeholderFadeInStart: Int = 0
    override val placeholderFadeInEnd: Int = 133
    override val placeholderIconFadeInStart: Int = 50
    override val placeholderIconFadeInEnd: Int = 133
    override val stagedRectSlideStart: Int = 0
    override val stagedRectSlideEnd: Int = 383
}

open class PhoneSplitToConfirmTimings : SplitAnimationTimings {
    override val duration: Int = SplitAnimationTimings.PHONE_CONFIRM_DURATION
    override val placeholderFadeInStart: Int = 0
    override val placeholderFadeInEnd: Int = 67
    override val placeholderIconFadeInStart: Int = 0
    override val placeholderIconFadeInEnd: Int = 67
    override val stagedRectSlideStart: Int = 0
    override val stagedRectSlideEnd: Int = 333
}

open class TabletOverviewToSplitTimings : SplitAnimationTimings {
    override val duration: Int = SplitAnimationTimings.TABLET_ENTER_DURATION
    override val placeholderFadeInStart: Int = 0
    override val placeholderFadeInEnd: Int = 150
    override val placeholderIconFadeInStart: Int = 67
    override val placeholderIconFadeInEnd: Int = 150
    override val stagedRectSlideStart: Int = 0
    override val stagedRectSlideEnd: Int = 417
}

open class TabletSplitToConfirmTimings : SplitAnimationTimings {
    override val duration: Int = SplitAnimationTimings.TABLET_CONFIRM_DURATION
    override val placeholderFadeInStart: Int = 0
    override val placeholderFadeInEnd: Int = 100
    override val placeholderIconFadeInStart: Int = 0
    override val placeholderIconFadeInEnd: Int = 100
    override val stagedRectSlideStart: Int = 0
    override val stagedRectSlideEnd: Int = 500
}
