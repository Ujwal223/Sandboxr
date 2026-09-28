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
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.sandboxr.launcher.allapps.ActivityAllAppsContainerView
import com.sandboxr.launcher.dragndrop.DragLayer
import com.sandboxr.launcher.statemanager.StateManager
import com.sandboxr.launcher.statemanager.StatefulActivity
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.ScrimView

/**
 * Default Launcher Activity for SANDBOXR.
 * Implements complete activity lifecycle management, state machine transitions
 * (NORMAL, SPRING_LOADED, ALL_APPS, OVERVIEW, EDIT_MODE), edge-to-edge system bars,
 * and responsive device profile adaptations.
 */
open class Launcher : StatefulActivity<LauncherState>(),
    InvariantDeviceProfile.OnIDPChangeListener {

    protected val mLauncherUiState = LauncherUiState()
    private lateinit var mStateManager: StateManager<LauncherState, Launcher>

    protected var mDragLayer: DragLayer? = null
    protected var mWorkspace: Workspace<*>? = null
    protected var mHotseat: Hotseat? = null
    protected var mAppsView: ActivityAllAppsContainerView<Launcher>? = null
    protected var mScrimView: ScrimView? = null
    protected var mOverviewPanel: View? = null

    protected lateinit var mIdp: InvariantDeviceProfile

    protected var mRotationHelper: com.sandboxr.launcher.states.RotationHelper? = null
    protected var mDepthController: com.sandboxr.launcher.statehandlers.DepthController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show live system wallpaper behind window for home screen experience
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        super.onCreate(savedInstanceState)

        // Initialize DeviceProfile
        mIdp = InvariantDeviceProfile.INSTANCE(this)
        mDeviceProfile = mIdp.getDeviceProfile(this)
        mIdp.addOnChangeListener(this)

        // Initialize RotationHelper & DepthController
        mRotationHelper = com.sandboxr.launcher.states.RotationHelper(this)
        mDepthController = com.sandboxr.launcher.statehandlers.DepthController(this)

        // Initialize StateManager
        mStateManager = StateManager(this, LauncherState.NORMAL)
        mStateManager.launcherUiState = mLauncherUiState

        // Setup Views
        setupViews()

        // Handle Home intent & restore state
        restoreState(savedInstanceState)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (!onBackPressedLauncher()) {
            // Already at root home screen workspace: do not exit launcher
        }
    }

    protected open fun setupViews() {
        val root = LauncherRootView(this).apply {
            id = R.id.launcher
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        mRootView = root

        val dragLayer = DragLayer(this).apply {
            id = R.id.drag_layer
            layoutParams = InsettableFrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        mDragLayer = dragLayer

        val workspace = Workspace<com.sandboxr.launcher.pageindicators.PageIndicatorDots>(this).apply {
            id = R.id.workspace
            layoutParams = InsettableFrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        mWorkspace = workspace

        val hotseat = Hotseat(this).apply {
            id = R.id.hotseat
            layoutParams = InsettableFrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        mHotseat = hotseat

        val scrimView = ScrimView(this).apply {
            id = R.id.scrim_view
            layoutParams = InsettableFrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        mScrimView = scrimView

        val appsView = ActivityAllAppsContainerView<Launcher>(this).apply {
            id = R.id.apps_view
            layoutParams = InsettableFrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }
        mAppsView = appsView

        dragLayer.addView(workspace)
        dragLayer.addView(hotseat)
        dragLayer.addView(scrimView)
        dragLayer.addView(appsView)
        root.addView(dragLayer)

        setContentView(root)
    }

    override fun getStateManager(): StateManager<LauncherState, Launcher> = mStateManager

    override fun getDragLayer(): DragLayer? = mDragLayer

    fun getWorkspace(): Workspace<*>? = mWorkspace

    fun getHotseat(): Hotseat? = mHotseat

    fun getAppsView(): ActivityAllAppsContainerView<Launcher>? = mAppsView

    fun getScrimView(): ScrimView? = mScrimView

    override fun collectStateHandlers(out: MutableList<StateManager.StateHandler<LauncherState>>) {
        mDepthController?.let { out.add(it) }
        out.add(object : StateManager.StateHandler<LauncherState> {
            override fun setState(state: LauncherState) {
                when (state) {
                    LauncherState.NORMAL -> {
                        mWorkspace?.visibility = View.VISIBLE
                        mHotseat?.visibility = View.VISIBLE
                        mAppsView?.visibility = View.GONE
                        mScrimView?.visibility = View.GONE
                        mOverviewPanel?.visibility = View.GONE
                    }
                    LauncherState.ALL_APPS -> {
                        mWorkspace?.visibility = View.GONE
                        mHotseat?.visibility = View.GONE
                        mAppsView?.visibility = View.VISIBLE
                        mScrimView?.visibility = View.VISIBLE
                        mOverviewPanel?.visibility = View.GONE
                    }
                    LauncherState.OVERVIEW -> {
                        mWorkspace?.visibility = View.GONE
                        mHotseat?.visibility = View.GONE
                        mAppsView?.visibility = View.GONE
                        mScrimView?.visibility = View.VISIBLE
                        mOverviewPanel?.visibility = View.VISIBLE
                    }
                    LauncherState.SPRING_LOADED -> {
                        mWorkspace?.visibility = View.VISIBLE
                        mHotseat?.visibility = View.VISIBLE
                        mAppsView?.visibility = View.GONE
                        mScrimView?.visibility = View.GONE
                    }
                    LauncherState.EDIT_MODE -> {
                        mWorkspace?.visibility = View.VISIBLE
                        mHotseat?.visibility = View.VISIBLE
                    }
                    else -> {}
                }
            }

            override fun setStateWithAnimation(
                toState: LauncherState,
                config: com.sandboxr.launcher.states.StateAnimationConfig,
                animation: com.sandboxr.launcher.anim.PendingAnimation
            ) {
                setState(toState)
            }
        })
    }

    fun showAllApps(animated: Boolean = true) {
        mStateManager.goToState(LauncherState.ALL_APPS, animated)
    }

    fun showOverview(animated: Boolean = true) {
        mStateManager.goToState(LauncherState.OVERVIEW, animated)
    }

    fun showHomeScreen(animated: Boolean = true) {
        mStateManager.goToState(LauncherState.NORMAL, animated)
    }

    fun toggleAllApps(focusSearch: Boolean = false) {
        if (mStateManager.isInStableState(LauncherState.ALL_APPS)) {
            mStateManager.goToState(LauncherState.NORMAL)
        } else {
            mStateManager.goToState(LauncherState.ALL_APPS)
        }
    }

    override fun isInState(state: LauncherState): Boolean {
        return mStateManager.state == state
    }

    /**
     * Intercepts back press to navigate to NORMAL state from drawers/overview panels.
     * Returns true if back press was consumed internally, or false if already at home workspace.
     */
    open fun onBackPressedLauncher(): Boolean {
        if (!mStateManager.isInStableState(LauncherState.NORMAL)) {
            mStateManager.goToState(LauncherState.NORMAL, animated = false)
            return true
        }
        return false
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null && intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_HOME)
        ) {
            // User pressed Home button: return to home screen
            showHomeScreen(animated = false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(RUNTIME_STATE, mStateManager.state.ordinal)
    }

    protected open fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val stateOrdinal = savedInstanceState.getInt(RUNTIME_STATE, LauncherState.NORMAL_STATE_ORDINAL)
            for (state in LauncherState.values()) {
                if (state.ordinal == stateOrdinal) {
                    mStateManager.goToState(state, animated = false)
                    break
                }
            }
        }
    }

    override fun onHandleConfigurationChanged() {
        mDeviceProfile = mIdp.getDeviceProfile(this)
        mLauncherUiState.setDeviceProfile(mDeviceProfile)
        reapplyUi()
    }

    override fun onIdpChanged(modelPropertiesChanged: Boolean) {
        mDeviceProfile = mIdp.getDeviceProfile(this)
        mLauncherUiState.setDeviceProfile(mDeviceProfile)
        reapplyUi()
    }

    override fun onDestroy() {
        mRotationHelper?.destroy()
        mIdp.removeOnChangeListener(this)
        super.onDestroy()
    }

    companion object {
        const val TAG = "Launcher"
        const val RUNTIME_STATE = "launcher.state"

        @JvmStatic
        fun getLauncher(context: Context): Launcher {
            return fromContext(context)
        }
    }
}
