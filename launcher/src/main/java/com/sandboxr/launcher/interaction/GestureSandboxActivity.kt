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

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.sandboxr.launcher.R

/**
 * Interactive tutorial and sandbox activity where users can practice gesture navigation.
 */
class GestureSandboxActivity : Activity() {

    var currentTutorialStep: SwipeUpGestureTutorialController.TutorialType =
        SwipeUpGestureTutorialController.TutorialType.HOME_NAVIGATION
        private set

    lateinit var tutorialController: SwipeUpGestureTutorialController
    lateinit var edgeBackHandler: EdgeBackGestureHandler
    lateinit var navBarHandler: NavBarGestureHandler

    private lateinit var feedbackTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            setBackgroundColor(resources.getColor(R.color.materialColorSurface, theme))
        }

        feedbackTextView = TextView(this).apply {
            textSize = 20f
            setTextColor(resources.getColor(R.color.materialColorOnSurface, theme))
            gravity = android.view.Gravity.CENTER
            text = getString(R.string.gesture_tutorial_title)
        }

        root.addView(feedbackTextView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        ))
        setContentView(root)

        setupTutorial(currentTutorialStep)
    }

    fun setupTutorial(type: SwipeUpGestureTutorialController.TutorialType) {
        currentTutorialStep = type
        tutorialController = SwipeUpGestureTutorialController(this, type).apply {
            onStateChanged = { state ->
                feedbackTextView.text = state.name
                if (state == SwipeUpGestureTutorialController.FeedbackState.SUCCESS) {
                    advanceToNextStep()
                }
            }
        }
        edgeBackHandler = EdgeBackGestureHandler(this).apply {
            onBackInvoked = {
                if (currentTutorialStep == SwipeUpGestureTutorialController.TutorialType.BACK_NAVIGATION) {
                    tutorialController.onUserGestureCompleted(isHold = false, distanceUpPx = 100f, velocityY = 0f)
                }
            }
        }
        navBarHandler = NavBarGestureHandler(this).apply {
            onSwipeUp = {
                tutorialController.onUserGestureCompleted(isHold = false, distanceUpPx = 200f, velocityY = -600f)
            }
            onLongPress = {
                tutorialController.onUserGestureCompleted(isHold = true, distanceUpPx = 200f, velocityY = 0f)
            }
        }
    }

    private fun advanceToNextStep() {
        when (currentTutorialStep) {
            SwipeUpGestureTutorialController.TutorialType.HOME_NAVIGATION -> {
                setupTutorial(SwipeUpGestureTutorialController.TutorialType.OVERVIEW_NAVIGATION)
            }
            SwipeUpGestureTutorialController.TutorialType.OVERVIEW_NAVIGATION -> {
                setupTutorial(SwipeUpGestureTutorialController.TutorialType.BACK_NAVIGATION)
            }
            SwipeUpGestureTutorialController.TutorialType.BACK_NAVIGATION -> {
                feedbackTextView.text = getString(R.string.allset_title)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (edgeBackHandler.onTouchEvent(ev)) {
            return true
        }
        if (navBarHandler.onTouchEvent(ev)) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
