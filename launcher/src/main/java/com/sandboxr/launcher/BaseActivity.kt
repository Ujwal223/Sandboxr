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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.window.OnBackInvokedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import com.sandboxr.launcher.DeviceProfile.OnDeviceProfileChangeListener
import com.sandboxr.launcher.dagger.ActivityContextComponent
import com.sandboxr.launcher.dagger.LauncherComponentProvider
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.SafeCloseable
import com.sandboxr.launcher.util.SystemUiController
import com.sandboxr.launcher.util.ViewCache
import com.sandboxr.launcher.util.WeakCleanupSet
import com.sandboxr.launcher.views.ActivityContext
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base activity implementing [ActivityContext], lifecycle management, and UI state tracking.
 */
abstract class BaseActivity : Activity(), ActivityContext {

    private val mDPChangeListeners = CopyOnWriteArrayList<OnDeviceProfileChangeListener>()

    private val mSavedStateRegistryController = SavedStateRegistryController.create(this)
    private val mLifecycleRegistry = LifecycleRegistry(this)
    private val mCleanupSet = WeakCleanupSet(this, Executors.MAIN_EXECUTOR)

    @Volatile
    @JvmField
    protected var mDeviceProfile: DeviceProfile = DeviceProfile.DEFAULT_DEVICE_PROFILE

    @JvmField
    protected var mSystemUiController: SystemUiController? = null

    @JvmField
    protected var mDisplayId: Int = Display.DEFAULT_DISPLAY

    private var mActivityFlags: Int = 0
    private var mForceInvisible: Int = 0

    private val mViewCache = ViewCache()

    private val mEventCallbacks = Array(4) { CopyOnWriteArrayList<Runnable>() }
    private val mTopResumedChangedCallbacks = CopyOnWriteArrayList<Runnable>()

    private var mCurrentActionMode: ActionMode? = null
    private var mActivityComponent: ActivityContextComponent? = null

    init {
        mSavedStateRegistryController.performAttach()
    }

    override fun getViewCache(): ViewCache = mViewCache

    override fun getLayoutInflater(): LayoutInflater = super.getLayoutInflater()

    override fun getDeviceProfile(): DeviceProfile = mDeviceProfile

    override fun getOnDeviceProfileChangeListeners(): List<OnDeviceProfileChangeListener> =
        mDPChangeListeners

    fun addOnDeviceProfileChangeListener(listener: OnDeviceProfileChangeListener) {
        mDPChangeListeners.add(listener)
    }

    fun removeOnDeviceProfileChangeListener(listener: OnDeviceProfileChangeListener) {
        mDPChangeListeners.remove(listener)
    }

    fun dispatchDeviceProfileChanged() {
        for (listener in mDPChangeListeners) {
            listener.onDeviceProfileChanged(mDeviceProfile)
        }
    }

    override fun getActivityComponent(): ActivityContextComponent? {
        if (mActivityComponent == null) {
            try {
                mActivityComponent = LauncherComponentProvider.get(this)
                    .activityContextComponentBuilder()
                    .bindContext(this)
                    .build()
            } catch (e: Exception) {
                Log.w(TAG, "ActivityContextComponent creation deferred or failed: ${e.message}")
            }
        }
        return mActivityComponent
    }

    fun getSystemUiController(): SystemUiController {
        var controller = mSystemUiController
        if (controller == null) {
            val decorView = window?.decorView ?: findViewById(android.R.id.content)
            controller = SystemUiController(decorView)
            mSystemUiController = controller
        }
        return controller
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSavedStateRegistryController.performRestore(savedInstanceState)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mDisplayId = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.displayId ?: Display.DEFAULT_DISPLAY
            } else {
                Display.DEFAULT_DISPLAY
            }
        } catch (e: Exception) {
            Display.DEFAULT_DISPLAY
        }

        registerBackDispatcher()
    }

    override fun onStart() {
        addActivityFlags(ACTIVITY_STATE_STARTED)
        super.onStart()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        executeAndClearCallbacks(EVENT_STARTED)
    }

    override fun onResume() {
        setResumed()
        super.onResume()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        executeAndClearCallbacks(EVENT_RESUMED)
    }

    override fun onPause() {
        setPaused()
        super.onPause()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        mSystemUiController?.updateUiState(SystemUiController.UI_STATE_FULLSCREEN_TASK, 0)
    }

    override fun onStop() {
        removeActivityFlags(ACTIVITY_STATE_STARTED or ACTIVITY_STATE_USER_ACTIVE)
        mForceInvisible = 0
        super.onStop()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        executeAndClearCallbacks(EVENT_STOPPED)
        mSystemUiController?.updateUiState(SystemUiController.UI_STATE_FULLSCREEN_TASK, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        executeAndClearCallbacks(EVENT_DESTROYED)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mSavedStateRegistryController.performSave(outState)
    }

    override fun onUserLeaveHint() {
        removeActivityFlags(ACTIVITY_STATE_USER_ACTIVE)
        super.onUserLeaveHint()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            addActivityFlags(ACTIVITY_STATE_WINDOW_FOCUSED)
        } else {
            removeActivityFlags(ACTIVITY_STATE_WINDOW_FOCUSED)
        }
    }

    override fun onTopResumedActivityChanged(isTopResumed: Boolean) {
        super.onTopResumedActivityChanged(isTopResumed)
        if (isTopResumed) {
            addActivityFlags(ACTIVITY_STATE_IS_TOP_RESUMED)
        } else {
            removeActivityFlags(ACTIVITY_STATE_IS_TOP_RESUMED)
        }
        for (callback in mTopResumedChangedCallbacks) {
            callback.run()
        }
    }

    protected open fun registerBackDispatcher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                onBackPressed()
            }
        }
    }

    fun isStarted(): Boolean = (mActivityFlags and ACTIVITY_STATE_STARTED) != 0

    fun hasBeenResumed(): Boolean = (mActivityFlags and ACTIVITY_STATE_RESUMED) != 0

    fun setResumed() {
        addActivityFlags(ACTIVITY_STATE_RESUMED or ACTIVITY_STATE_USER_ACTIVE)
    }

    fun setPaused() {
        removeActivityFlags(ACTIVITY_STATE_RESUMED or ACTIVITY_STATE_DEFERRED_RESUMED)
    }

    fun isUserActive(): Boolean = (mActivityFlags and ACTIVITY_STATE_USER_ACTIVE) != 0

    fun isTopResumedActivity(): Boolean = (mActivityFlags and ACTIVITY_STATE_IS_TOP_RESUMED) != 0

    fun getActivityFlags(): Int = mActivityFlags

    protected fun addActivityFlags(toAdd: Int) {
        val oldFlags = mActivityFlags
        mActivityFlags = mActivityFlags or toAdd
        onActivityFlagsChanged(toAdd)
    }

    protected fun removeActivityFlags(toRemove: Int) {
        val oldFlags = mActivityFlags
        mActivityFlags = mActivityFlags and toRemove.inv()
        onActivityFlagsChanged(toRemove)
    }

    protected open fun onActivityFlagsChanged(changeBits: Int) {}

    fun addForceInvisibleFlag(flag: Int) {
        mForceInvisible = mForceInvisible or flag
    }

    fun clearForceInvisibleFlag(flag: Int) {
        mForceInvisible = mForceInvisible and flag.inv()
    }

    fun isForceInvisible(): Boolean = (mForceInvisible and INVISIBLE_FLAGS) != 0

    fun hasSomeInvisibleFlag(mask: Int): Boolean = (mForceInvisible and mask) != 0

    fun addEventCallback(event: Int, callback: Runnable) {
        if (event in mEventCallbacks.indices) {
            mEventCallbacks[event].add(callback)
        }
    }

    fun removeEventCallback(event: Int, callback: Runnable) {
        if (event in mEventCallbacks.indices) {
            mEventCallbacks[event].remove(callback)
        }
    }

    fun addTopResumedChangedCallback(callback: Runnable) {
        mTopResumedChangedCallbacks.add(callback)
    }

    fun removeTopResumedChangedCallback(callback: Runnable) {
        mTopResumedChangedCallbacks.remove(callback)
    }

    private fun executeAndClearCallbacks(event: Int) {
        if (event in mEventCallbacks.indices) {
            val callbacks = ArrayList(mEventCallbacks[event])
            mEventCallbacks[event].clear()
            for (cb in callbacks) {
                cb.run()
            }
        }
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        mCurrentActionMode = mode
    }

    override fun onActionModeFinished(mode: ActionMode) {
        super.onActionModeFinished(mode)
        mCurrentActionMode = null
    }

    protected fun isInAutoCancelActionMode(): Boolean {
        return mCurrentActionMode != null && AUTO_CANCEL_ACTION_MODE == mCurrentActionMode?.tag
    }

    override fun finishAutoCancelActionMode(): Boolean {
        if (isInAutoCancelActionMode()) {
            mCurrentActionMode?.finish()
            return true
        }
        return false
    }

    override fun closeOnDestroy(closeable: SafeCloseable) {
        addEventCallback(EVENT_DESTROYED) { closeable.close() }
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = mSavedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = mLifecycleRegistry

    fun dumpMisc(prefix: String, writer: PrintWriter) {
        writer.println("${prefix}deviceProfile isTransposed=${getDeviceProfile().isVerticalBarLayout()}")
        writer.println("${prefix}orientation=${resources.configuration.orientation}")
        writer.println("${prefix}mSystemUiController: $mSystemUiController")
        writer.println("${prefix}mActivityFlags: $mActivityFlags")
        writer.println("${prefix}mForceInvisible: $mForceInvisible")
    }

    companion object {
        private const val TAG = "BaseActivity"

        const val INVISIBLE_BY_STATE_HANDLER = 1 shl 0
        const val INVISIBLE_BY_APP_TRANSITIONS = 1 shl 1
        const val INVISIBLE_BY_PENDING_FLAGS = 1 shl 2
        const val PENDING_INVISIBLE_BY_WALLPAPER_ANIMATION = 1 shl 3

        const val INVISIBLE_FLAGS =
            INVISIBLE_BY_STATE_HANDLER or INVISIBLE_BY_APP_TRANSITIONS or INVISIBLE_BY_PENDING_FLAGS

        const val ACTIVITY_STATE_STARTED = 1 shl 0
        const val ACTIVITY_STATE_RESUMED = 1 shl 1
        const val ACTIVITY_STATE_DEFERRED_RESUMED = 1 shl 2
        const val ACTIVITY_STATE_WINDOW_FOCUSED = 1 shl 3
        const val ACTIVITY_STATE_USER_ACTIVE = 1 shl 4
        const val ACTIVITY_STATE_TRANSITION_ACTIVE = 1 shl 6
        const val ACTIVITY_STATE_IS_TOP_RESUMED = 1 shl 7

        const val EVENT_STARTED = 0
        const val EVENT_RESUMED = 1
        const val EVENT_STOPPED = 2
        const val EVENT_DESTROYED = 3

        @JvmField
        val AUTO_CANCEL_ACTION_MODE = Any()

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : BaseActivity> fromContext(context: Context): T {
            return when (context) {
                is BaseActivity -> context as T
                is ContextWrapper -> fromContext(context.baseContext)
                else -> throw IllegalArgumentException("Cannot find BaseActivity in parent tree of $context")
            }
        }
    }
}
