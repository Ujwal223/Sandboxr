/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.sandboxr.launcher.accessibility

import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.sandboxr.launcher.views.ActivityContext

/**
 * Periodically sends accessibility events to announce ongoing state changed.
 */
class DragViewStateAnnouncer private constructor(
    private val mTargetView: View
) : Runnable {

    fun announce(msg: CharSequence) {
        mTargetView.contentDescription = msg
        mTargetView.removeCallbacks(this)
        mTargetView.postDelayed(this, TIMEOUT_SEND_ACCESSIBILITY_EVENT)
    }

    fun cancel() {
        mTargetView.removeCallbacks(this)
    }

    override fun run() {
        mTargetView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
    }

    fun completeAction(announceResId: Int) {
        cancel()
        val activityContext = ActivityContext.lookupContext<ActivityContext>(mTargetView.context)
        activityContext.getDragLayer()?.announceForAccessibility(
            mTargetView.context.getText(announceResId)
        )
    }

    companion object {
        private const val TIMEOUT_SEND_ACCESSIBILITY_EVENT = 200L

        @JvmStatic
        fun createFor(v: View): DragViewStateAnnouncer? {
            val am = v.context.getSystemService(AccessibilityManager::class.java)
            return if (am != null && am.isEnabled) DragViewStateAnnouncer(v) else null
        }
    }
}
