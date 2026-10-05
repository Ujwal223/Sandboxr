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

import com.sandboxr.launcher.taskbar.bubbles.BubbleControllers
import com.sandboxr.launcher.taskbar.edu.TaskbarEduTooltipController
import com.sandboxr.launcher.taskbar.growth.NudgeController
import com.sandboxr.launcher.taskbar.growth.NudgeViewController
import com.sandboxr.launcher.taskbar.handoff.TaskbarHandoffController
import com.sandboxr.launcher.taskbar.overlay.TaskbarOverlayController
import com.sandboxr.launcher.taskbar.unfold.TaskbarUnfoldAnimationController

/**
 * Registry hosting and orchestrating all Taskbar controller subsystems.
 */
class TaskbarControllers(
    val taskbarActivityContext: TaskbarActivityContext,
    val taskbarDragController: TaskbarDragController,
    val navButtonController: TaskbarNavButtonController,
    val navbarButtonsViewController: NavbarButtonsViewController,
    val taskbarDragLayerController: TaskbarDragLayerController,
    val taskbarViewController: TaskbarViewController,
    val taskbarScrimViewController: TaskbarScrimViewController,
    val stashedHandleViewController: StashedHandleViewController,
    val taskbarStashController: TaskbarStashController,
    val taskbarInsetsController: TaskbarInsetsController,
    val taskbarPinningController: TaskbarPinningController,
    val taskbarViewDragDropController: TaskbarViewDragDropController,
    val taskbarEduTooltipController: TaskbarEduTooltipController,
    val nudgeController: NudgeController,
    val nudgeViewController: NudgeViewController,
    val taskbarHandoffController: TaskbarHandoffController,
    val taskbarOverlayController: TaskbarOverlayController,
    val taskbarUnfoldAnimationController: TaskbarUnfoldAnimationController,
    val bubbleControllers: BubbleControllers?,
) {
    var uiController: TaskbarUIController = TaskbarUIController.DEFAULT
    private var areAllControllersInitialized = false
    var sharedState: TaskbarSharedState? = null
        private set

    fun init(sharedState: TaskbarSharedState) {
        areAllControllersInitialized = false
        this.sharedState = sharedState

        taskbarDragController.init(this)
        navbarButtonsViewController.init(this)
        navButtonController.init(this)
        taskbarDragLayerController.init(this)
        taskbarViewController.init(this)
        taskbarScrimViewController.init(this)
        stashedHandleViewController.init(this)
        taskbarStashController.init(this)
        taskbarPinningController.init(this)
        taskbarViewDragDropController.init(this)
        uiController.init(this)

        areAllControllersInitialized = true
    }

    fun onDestroy() {
        uiController.onDestroy()
        areAllControllersInitialized = false
    }

    fun isInitialized(): Boolean = areAllControllersInitialized
}
