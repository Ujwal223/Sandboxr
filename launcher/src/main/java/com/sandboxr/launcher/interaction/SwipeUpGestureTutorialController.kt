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

package com.sandboxr.launcher.interaction

import android.content.Context

/**
 * Controller managing the swipe-up gesture tutorial (Home and Overview gestures).
 */
class SwipeUpGestureTutorialController(
    val context: Context,
    val tutorialType: TutorialType = TutorialType.HOME_NAVIGATION
) {

    enum class TutorialType {
        HOME_NAVIGATION,
        OVERVIEW_NAVIGATION,
        BACK_NAVIGATION
    }

    enum class FeedbackState {
        WAITING_FOR_INPUT,
        SUCCESS,
        TOO_FAST,
        HOLD_LONGER,
        SWIPE_HIGHER,
        CANCELLED
    }

    var currentState: FeedbackState = FeedbackState.WAITING_FOR_INPUT
        private set

    var onStateChanged: ((FeedbackState) -> Unit)? = null

    fun onUserGestureCompleted(isHold: Boolean, distanceUpPx: Float, velocityY: Float) {
        when (tutorialType) {
            TutorialType.HOME_NAVIGATION -> {
                if (distanceUpPx >= 150f && !isHold) {
                    setState(FeedbackState.SUCCESS)
                } else if (isHold) {
                    setState(FeedbackState.TOO_FAST) // For Home, don't hold!
                } else {
                    setState(FeedbackState.SWIPE_HIGHER)
                }
            }
            TutorialType.OVERVIEW_NAVIGATION -> {
                if (distanceUpPx >= 150f && isHold) {
                    setState(FeedbackState.SUCCESS)
                } else if (!isHold) {
                    setState(FeedbackState.HOLD_LONGER) // For Recents, hold!
                } else {
                    setState(FeedbackState.SWIPE_HIGHER)
                }
            }
            TutorialType.BACK_NAVIGATION -> {
                setState(FeedbackState.SUCCESS)
            }
        }
    }

    fun reset() {
        setState(FeedbackState.WAITING_FOR_INPUT)
    }

    private fun setState(state: FeedbackState) {
        currentState = state
        onStateChanged?.invoke(state)
    }
}
