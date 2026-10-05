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

package com.sandboxr.launcher.anim

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import androidx.annotation.RequiresApi
import com.android.launcher3.Utilities

/**
 * WindowInsetsAnimation callback that drives smooth translation of views above the IME keyboard.
 */
@RequiresApi(Build.VERSION_CODES.R)
open class KeyboardInsetAnimationCallback(
    private val view: View
) : WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {

    private var initialTranslation: Float = 0f
    private var terminalTranslation: Float = 0f
    var keyboardTranslationState: KeyboardTranslationState = KeyboardTranslationState.SYSTEM
        protected set

    enum class KeyboardTranslationState {
        SYSTEM,
        MANUAL_PREPARED,
        MANUAL_ONGOING
    }

    override fun onPrepare(animation: WindowInsetsAnimation) {
        keyboardTranslationState = KeyboardTranslationState.MANUAL_PREPARED
        initialTranslation = view.translationY
    }

    override fun onStart(
        animation: WindowInsetsAnimation,
        bounds: WindowInsetsAnimation.Bounds
    ): WindowInsetsAnimation.Bounds {
        terminalTranslation = view.translationY
        view.translationY = initialTranslation
        keyboardTranslationState = KeyboardTranslationState.MANUAL_ONGOING
        val listener = view as? KeyboardInsetListener
        listener?.onTranslationStart()
        return super.onStart(animation, bounds)
    }

    override fun onProgress(
        insets: WindowInsets,
        runningAnimations: List<WindowInsetsAnimation>
    ): WindowInsets {
        if (runningAnimations.isEmpty()) {
            view.translationY = initialTranslation
            return insets
        }
        val animation = runningAnimations[0]
        if (animation.durationMillis > -1) {
            val progress = animation.interpolatedFraction
            view.translationY = Utilities.mapRange(progress, initialTranslation, terminalTranslation)
        } else {
            val imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom
            var translationY = -imeBottom.toFloat()
            val parent = view.parent
            if (translationY < 0 && parent is View) {
                translationY -= parent.translationY
            }
            view.translationY = translationY
        }

        val listener = view as? KeyboardInsetListener
        listener?.onKeyboardAlphaChanged(animation.alpha)

        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimation) {
        val listener = view as? KeyboardInsetListener
        listener?.onTranslationEnd()
        keyboardTranslationState = KeyboardTranslationState.SYSTEM
    }

    interface KeyboardInsetListener {
        fun onTranslationStart()
        fun onKeyboardAlphaChanged(alpha: Float) {}
        fun onTranslationEnd()
    }
}
