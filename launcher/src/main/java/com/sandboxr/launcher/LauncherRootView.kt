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

package com.sandboxr.launcher

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import com.sandboxr.launcher.statemanager.StatefulContainer
import com.sandboxr.launcher.views.ActivityContext
import java.util.Collections

open class LauncherRootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : InsettableFrameLayout(context, attrs) {

    private val tempRect = Rect()
    private val systemGestureExclusionRect: List<Rect> = Collections.singletonList(Rect())

    private var windowStateListener: WindowStateListener? = null
    private var disallowBackGesture: Boolean = false
    private var forceHideBackArrow: Boolean = false

    private val container: StatefulContainer<*>? by lazy {
        try {
            ActivityContext.lookupContext(context)
        } catch (e: Exception) {
            null
        }
    }

    private fun handleSystemWindowInsets(insets: Rect) {
        container?.getDeviceProfile()?.updateInsets(insets)
        val resetState = insets != mInsets
        setInsets(insets)

        if (resetState) {
            container?.getStateManager()?.reapplyState(true)
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        container?.let {
            it.handleConfigurationChanged(it.asContext().resources.configuration)
        }
        return updateInsets(insets)
    }

    private fun updateInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val systemBars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                tempRect.set(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            } catch (e: Exception) {
                tempRect.set(0, 0, 0, 0)
            }
        } else {
            @Suppress("DEPRECATION")
            tempRect.set(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )
        }
        handleSystemWindowInsets(tempRect)
        return insets
    }

    fun dispatchInsets() {
        if (isAttachedToWindow) {
            rootWindowInsets?.let { updateInsets(it) }
        } else {
            container?.getDeviceProfile()?.updateInsets(mInsets)
        }
        super.setInsets(mInsets)
    }

    fun setWindowStateListener(listener: WindowStateListener?) {
        windowStateListener = listener
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        windowStateListener?.onWindowFocusChanged(hasWindowFocus)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        windowStateListener?.onWindowVisibilityChanged(visibility)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        systemGestureExclusionRect[0].set(l, t, r, b)
        setDisallowBackGesture(disallowBackGesture)
    }

    fun setForceHideBackArrow(forceHide: Boolean) {
        this.forceHideBackArrow = forceHide
        setDisallowBackGesture(disallowBackGesture)
    }

    fun setDisallowBackGesture(disallow: Boolean) {
        disallowBackGesture = disallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = if (forceHideBackArrow || disallowBackGesture) {
                systemGestureExclusionRect
            } else {
                emptyList()
            }
        }
    }

    interface WindowStateListener {
        fun onWindowFocusChanged(hasFocus: Boolean)
        fun onWindowVisibilityChanged(visibility: Int)
    }
}
