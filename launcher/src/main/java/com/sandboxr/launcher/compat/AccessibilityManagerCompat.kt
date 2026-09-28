/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.compat

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * Compatibility wrapper for AccessibilityManager events and TalkBack integration.
 */
object AccessibilityManagerCompat {

    @JvmStatic
    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am?.isEnabled ?: false
    }

    @JvmStatic
    fun sendCustomAccessibilityEvent(target: View?, type: Int, text: String?) {
        if (target == null) return
        val am = target.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return
        if (!am.isEnabled) return

        val event = AccessibilityEvent.obtain(type)
        target.onInitializeAccessibilityEvent(event)
        if (!TextUtils.isEmpty(text)) {
            event.text.add(text)
        }
        am.sendAccessibilityEvent(event)
    }

    @JvmStatic
    fun getRecommendedTimeoutMillis(context: Context, originalTimeout: Int, flags: Int): Int {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return originalTimeout
        return am.getRecommendedTimeoutMillis(originalTimeout, flags)
    }
}
