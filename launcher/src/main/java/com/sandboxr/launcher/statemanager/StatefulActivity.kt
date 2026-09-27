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

import android.content.pm.ActivityInfo.CONFIG_ORIENTATION
import android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CallSuper
import com.sandboxr.launcher.BaseActivity
import com.sandboxr.launcher.LauncherRootView

/**
 * Abstract activity with state management capabilities.
 */
abstract class StatefulActivity<STATE_TYPE : BaseState<STATE_TYPE>> :
    BaseActivity(), StatefulContainer<STATE_TYPE> {

    val mHandler = Handler(Looper.getMainLooper())
    private val mHandleDeferredResume = Runnable { handleDeferredResume() }
    private var mDeferredResumePending: Boolean = false

    protected var mRootView: LauncherRootView? = null
    protected var mOldConfig: Configuration? = null
    private var mOldRotation: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mOldConfig = Configuration(resources.configuration)
        mOldRotation = try {
            display?.rotation ?: 0
        } catch (e: Exception) {
            0
        }
    }

    protected open fun inflateRootView(layoutId: Int) {
        val root = LayoutInflater.from(this).inflate(layoutId, null) as LauncherRootView
        @Suppress("DEPRECATION")
        root.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        mRootView = root
        setContentView(root)
    }

    override fun getRootView(): LauncherRootView? = mRootView

    override fun <T : View> findViewById(id: Int): T? {
        val root = mRootView
        return if (root != null) root.findViewById(id) else super.findViewById(id)
    }

    @CallSuper
    override fun onStateSetStart(state: STATE_TYPE) {
        if (mDeferredResumePending) {
            handleDeferredResume()
        }
        super.onStateSetStart(state)
    }

    override fun shouldAnimateStateChange(): Boolean {
        return !isForceInvisible() && isStarted()
    }

    override fun reapplyUi() {
        mRootView?.dispatchInsets()
        getStateManager().reapplyState(true)
    }

    override fun onStop() {
        val origState = getStateManager().state
        super.onStop()

        if (!isChangingConfigurations) {
            getStateManager().moveToRestState()
        }
    }

    private fun handleDeferredResume() {
        if (hasBeenResumed() && !getStateManager().state.hasFlag(BaseState.FLAG_NON_INTERACTIVE)) {
            addActivityFlags(ACTIVITY_STATE_DEFERRED_RESUMED)
            onDeferredResumed()
            mDeferredResumePending = false
        } else {
            mDeferredResumePending = true
        }
    }

    protected open fun onDeferredResumed() {}

    override fun onResume() {
        super.onResume()
        mHandler.removeCallbacks(mHandleDeferredResume)
        mHandler.post(mHandleDeferredResume)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        handleConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun handleConfigurationChanged(newConfig: Configuration) {
        val old = mOldConfig
        val rotation = try {
            display?.rotation ?: 0
        } catch (e: Exception) {
            0
        }
        if (old != null) {
            val diff = newConfig.diff(old)
            if ((diff and (CONFIG_ORIENTATION or CONFIG_SCREEN_SIZE)) != 0 || rotation != mOldRotation) {
                onHandleConfigurationChanged()
            }
        }
        mOldConfig = Configuration(newConfig)
        mOldRotation = rotation
    }

    protected abstract fun onHandleConfigurationChanged()
}
