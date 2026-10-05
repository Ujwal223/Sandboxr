/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop

import android.graphics.drawable.Drawable
import android.view.View
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.LauncherState
import com.sandboxr.launcher.statemanager.StateManager

/**
 * A [DragView] drawn and managed within the [Launcher] activity lifecycle.
 */
open class LauncherDragView : DragView, StateManager.StateListener<LauncherState> {

    private val mLauncher: Launcher

    constructor(
        launcher: Launcher,
        drawable: Drawable,
        registrationX: Int,
        registrationY: Int,
        initialScale: Float,
        scaleOnDrop: Float,
        finalScaleDps: Float,
        allowSpringDrawable: Boolean
    ) : super(launcher, drawable, registrationX, registrationY, initialScale, scaleOnDrop, finalScaleDps, allowSpringDrawable) {
        mLauncher = launcher
    }

    constructor(
        launcher: Launcher,
        content: View,
        width: Int,
        height: Int,
        registrationX: Int,
        registrationY: Int,
        initialScale: Float,
        scaleOnDrop: Float,
        finalScaleDps: Float,
        allowSpringDrawable: Boolean
    ) : super(launcher, content, width, height, registrationX, registrationY, initialScale, scaleOnDrop, finalScaleDps, allowSpringDrawable) {
        mLauncher = launcher
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mLauncher.getStateManager().addStateListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mLauncher.getStateManager().removeStateListener(this)
    }

    override fun onStateTransitionComplete(finalState: LauncherState) {
        visibility = if (finalState == LauncherState.NORMAL
            || finalState == LauncherState.SPRING_LOADED
            || finalState == LauncherState.EDIT_MODE
        ) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun animateTo(toTouchX: Int, toTouchY: Int, onCompleteRunnable: Runnable?, duration: Int) {
        mTempLoc[0] = toTouchX - mRegistrationX
        mTempLoc[1] = toTouchY - mRegistrationY
        val dragLayer = mLauncher.getDragLayer()
        if (dragLayer != null) {
            dragLayer.animateViewIntoPosition(
                this,
                mTempLoc,
                1f,
                mScaleOnDrop,
                mScaleOnDrop,
                DragLayer.ANIMATION_END_DISAPPEAR,
                onCompleteRunnable,
                duration
            )
        } else {
            super.animateTo(toTouchX, toTouchY, onCompleteRunnable, duration)
        }
    }
}
