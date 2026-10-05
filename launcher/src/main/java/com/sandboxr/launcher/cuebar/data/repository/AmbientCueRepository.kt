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

package com.sandboxr.launcher.cuebar.data.repository

import android.content.Context
import android.graphics.Rect
import android.util.Log
import com.sandboxr.launcher.cuebar.data.ActionModel
import com.sandboxr.launcher.cuebar.data.InsightListener
import com.sandboxr.launcher.cuebar.logger.AmbientCueAceLogger
import com.sandboxr.launcher.cuebar.logger.AmbientCueLogger
import com.sandboxr.launcher.util.Executors
import com.sandboxr.launcher.util.ListenableRef
import com.sandboxr.launcher.util.MutableListenableRef
import java.io.PrintWriter
import java.util.concurrent.Executor

/**
 * Source of truth for ambient actions and visibility of the CueBar system space.
 */
interface AmbientCueRepository {
    /** Chips that should be visible on the UI. */
    val actions: ListenableRef<List<ActionModel>>

    /** If IME (keyboard) is visible or not. */
    val isImeVisible: MutableListenableRef<Boolean>

    /** If the UI is occluded by System UI. */
    val isOccludedBySystemUi: MutableListenableRef<Boolean>

    /** If the UI is deactivated (e.g. dismissed by user or timed out). */
    val isDeactivated: MutableListenableRef<Boolean>

    /** If the taskbar is fully visible and not stashed. */
    val isTaskBarVisible: MutableListenableRef<Boolean>

    /** True if in gesture nav mode, false when in 3-button navbar. */
    val isGestureNav: MutableListenableRef<Boolean>

    /** Position of the recents button in 3-button nav mode. */
    val recentsButtonPosition: MutableListenableRef<Rect?>

    /** If Ambient Cue feature is enabled. */
    val isAmbientCueEnabled: MutableListenableRef<Boolean>

    /** The timeout for Ambient Cue to disappear in milliseconds. */
    val ambientCueTimeoutMs: MutableListenableRef<Int>

    /** Task ID which is globally focused on display. */
    val globallyFocusedTaskId: MutableListenableRef<Int>

    /** The package name of the task in foreground. */
    val frontTaskPackageName: MutableListenableRef<String>

    /** Flag indicating test mode. */
    val isTestMode: MutableListenableRef<Boolean>

    fun updateActions(newActions: List<ActionModel>)

    fun connectToAce()

    fun disconnectFromAce()

    fun dump(pw: PrintWriter, prefix: String)

    fun reportCloseEvent()

    fun injectTestInsightForCueBar()
}

/**
 * Standard implementation of [AmbientCueRepository].
 */
class AmbientCueRepositoryImpl(
    private val context: Context,
    private val ambientCueLogger: AmbientCueLogger? = null,
    private val ambientCueAceLogger: AmbientCueAceLogger? = null,
    private val bgExecutor: Executor = Executors.ORDERED_BG_EXECUTOR,
    private val uiExecutor: Executor = Executors.MAIN_EXECUTOR,
) : AmbientCueRepository, InsightListener {

    private val _actions = MutableListenableRef<List<ActionModel>>(emptyList())
    override val actions: MutableListenableRef<List<ActionModel>> = _actions

    override val isDeactivated = MutableListenableRef(false)
    override val isTaskBarVisible = MutableListenableRef(true)
    override val isGestureNav = MutableListenableRef(true)
    override val recentsButtonPosition = MutableListenableRef<Rect?>(null)

    override val isTestMode = MutableListenableRef(false)
    override val isImeVisible = MutableListenableRef(false)
    override val isOccludedBySystemUi = MutableListenableRef(false)
    override val isAmbientCueEnabled = MutableListenableRef(true)
    override val ambientCueTimeoutMs = MutableListenableRef(DEFAULT_TIMEOUT_MS)
    override val globallyFocusedTaskId = MutableListenableRef(-1)
    override val frontTaskPackageName = MutableListenableRef("")

    override fun updateActions(newActions: List<ActionModel>) {
        Log.d(TAG, "updateActions: ${newActions.size} actions")
        _actions.dispatchValue(newActions)
    }

    override fun onActionsReceived(actions: List<ActionModel>) {
        uiExecutor.execute {
            if (actions.isNotEmpty()) {
                isDeactivated.dispatchValue(false)
            }
            updateActions(actions)
        }
    }

    override fun connectToAce() {
        if (!isAmbientCueEnabled.value) {
            Log.d(TAG, "Ace listener connect skipped: Ambient Cue is disabled.")
            return
        }
        Log.d(TAG, "connectToAce: connected")
    }

    override fun disconnectFromAce() {
        Log.d(TAG, "disconnectFromAce: disconnected")
    }

    override fun reportCloseEvent() {
        ambientCueAceLogger?.reportCloseEvent()
    }

    override fun injectTestInsightForCueBar() {
        isTestMode.dispatchValue(true)
        isDeactivated.dispatchValue(false)
    }

    override fun dump(pw: PrintWriter, prefix: String) {
        pw.println("$prefix AmbientCueRepositoryImpl:")
        pw.println("$prefix   isDeactivated: ${isDeactivated.value}")
        pw.println("$prefix   isImeVisible: ${isImeVisible.value}")
        pw.println("$prefix   isOccludedBySystemUi: ${isOccludedBySystemUi.value}")
        pw.println("$prefix   isTaskBarVisible: ${isTaskBarVisible.value}")
        pw.println("$prefix   isGestureNav: ${isGestureNav.value}")
        pw.println("$prefix   actions: ${actions.value.size} actions")
        pw.println("$prefix   isAmbientCueEnabled: ${isAmbientCueEnabled.value}")
        pw.println("$prefix   ambientCueTimeoutMs: ${ambientCueTimeoutMs.value}")
        pw.println("$prefix   globallyFocusedTaskId: ${globallyFocusedTaskId.value}")
        pw.println("$prefix   frontTaskPackageName: ${frontTaskPackageName.value}")
    }

    companion object {
        private const val TAG = "AmbientCueRepo"
        const val DEFAULT_TIMEOUT_MS = 30_000
    }
}
