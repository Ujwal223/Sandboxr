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

package com.sandboxr.launcher.taskbar

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.R
import com.sandboxr.launcher.dagger.ActivityContextComponent
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.taskbar.bubbles.BubbleBarController
import com.sandboxr.launcher.taskbar.bubbles.BubbleBarView
import com.sandboxr.launcher.taskbar.bubbles.BubbleControllers
import com.sandboxr.launcher.taskbar.edu.TaskbarEduTooltipController
import com.sandboxr.launcher.taskbar.growth.NudgeController
import com.sandboxr.launcher.taskbar.growth.NudgeViewController
import com.sandboxr.launcher.taskbar.handoff.TaskbarHandoffController
import com.sandboxr.launcher.taskbar.navbutton.NearestTouchFrame
import com.sandboxr.launcher.taskbar.overlay.TaskbarOverlayController
import com.sandboxr.launcher.taskbar.unfold.TaskbarUnfoldAnimationController
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * Host Context managing the Taskbar window, DragLayer, and all Taskbar controller subsystems.
 */
class TaskbarActivityContext(
    base: Context,
    private val deviceProfile: DeviceProfile,
    val sharedState: TaskbarSharedState = TaskbarSharedState(),
) : ContextWrapper(base), ActivityContext, LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val layoutInflater: LayoutInflater = LayoutInflater.from(base).cloneInContext(this)

    lateinit var dragLayer: TaskbarDragLayer
        private set
    lateinit var controllers: TaskbarControllers
        private set

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun getActivityComponent(): ActivityContextComponent? = null
    override fun getDeviceProfile(): DeviceProfile = deviceProfile
    override fun getRootView(): View? = if (::dragLayer.isInitialized) dragLayer else null
    override fun getDragLayer(): BaseDragLayer<*>? = if (::dragLayer.isInitialized) dragLayer else null
    override fun getDragController(): DragController? = if (::controllers.isInitialized) controllers.taskbarDragController else null
    override fun getLayoutInflater(): LayoutInflater = layoutInflater

    fun init() {
        val root = layoutInflater.inflate(R.layout.taskbar, null, false) as TaskbarDragLayer
        dragLayer = root

        val taskbarView = root.findViewById<TaskbarView>(R.id.taskbar_view)
        val scrimView = root.findViewById<TaskbarScrimView>(R.id.taskbar_scrim)
        val stashedHandleView = root.findViewById<StashedHandleView>(R.id.stashed_handle)
        val navButtonsView = root.findViewById<NearestTouchFrame>(R.id.navbuttons_view)
        val bubbleBarView = root.findViewById<BubbleBarView>(R.id.taskbar_bubbles)

        val taskbarDragController = TaskbarDragController(this)
        val navButtonController = TaskbarNavButtonController(this)
        val navbarButtonsViewController = NavbarButtonsViewController(this, navButtonsView)
        val taskbarDragLayerController = TaskbarDragLayerController(this, root)
        val taskbarViewController = TaskbarViewController(this, taskbarView)
        val taskbarScrimViewController = TaskbarScrimViewController(this, scrimView)
        val stashedHandleViewController = StashedHandleViewController(this, stashedHandleView)
        val taskbarStashController = TaskbarStashController(this)
        val taskbarInsetsController = TaskbarInsetsController(this)
        val taskbarPinningController = TaskbarPinningController(this)
        val taskbarViewDragDropController = TaskbarViewDragDropController(this)
        val taskbarEduTooltipController = TaskbarEduTooltipController(this)
        val nudgeController = NudgeController(this)
        val nudgeViewController = NudgeViewController(null)
        val taskbarHandoffController = TaskbarHandoffController(this)
        val taskbarOverlayController = TaskbarOverlayController(this)
        val taskbarUnfoldAnimationController = TaskbarUnfoldAnimationController()
        val bubbleControllers = BubbleControllers(BubbleBarController(bubbleBarView))

        controllers = TaskbarControllers(
            taskbarActivityContext = this,
            taskbarDragController = taskbarDragController,
            navButtonController = navButtonController,
            navbarButtonsViewController = navbarButtonsViewController,
            taskbarDragLayerController = taskbarDragLayerController,
            taskbarViewController = taskbarViewController,
            taskbarScrimViewController = taskbarScrimViewController,
            stashedHandleViewController = stashedHandleViewController,
            taskbarStashController = taskbarStashController,
            taskbarInsetsController = taskbarInsetsController,
            taskbarPinningController = taskbarPinningController,
            taskbarViewDragDropController = taskbarViewDragDropController,
            taskbarEduTooltipController = taskbarEduTooltipController,
            nudgeController = nudgeController,
            nudgeViewController = nudgeViewController,
            taskbarHandoffController = taskbarHandoffController,
            taskbarOverlayController = taskbarOverlayController,
            taskbarUnfoldAnimationController = taskbarUnfoldAnimationController,
            bubbleControllers = bubbleControllers,
        )

        controllers.init(sharedState)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun startActivitySafely(v: View?, intent: Intent?, item: ItemInfo?): Boolean {
        if (intent == null) return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        if (::controllers.isInitialized) {
            controllers.onDestroy()
        }
    }
}
