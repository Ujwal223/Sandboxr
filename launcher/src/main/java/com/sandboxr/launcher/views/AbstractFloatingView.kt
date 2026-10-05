/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.sandboxr.launcher.touch.TouchController

/**
 * Base class for views that float on top of the launcher UI (folders, context menus,
 * bottom sheets, option popups, etc.).
 */
abstract class AbstractFloatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TouchController {

    companion object {
        const val TYPE_FOLDER = 1 shl 0
        const val TYPE_ACTION_POPUP = 1 shl 1
        const val TYPE_WIDGETS_BOTTOM_SHEET = 1 shl 2
        const val TYPE_WIDGET_RESIZE_FRAME = 1 shl 3
        const val TYPE_ON_BOARD_POPUP = 1 shl 5
        const val TYPE_DISCOVERY_BOUNCE = 1 shl 6
        const val TYPE_SNACKBAR = 1 shl 7
        const val TYPE_LISTENER = 1 shl 8
        const val TYPE_ALL_APPS_EDU = 1 shl 9
        const val TYPE_DRAG_DROP_POPUP = 1 shl 10
        const val TYPE_TASK_MENU = 1 shl 11
        const val TYPE_OPTIONS_POPUP = 1 shl 12
        const val TYPE_ICON_SURFACE = 1 shl 13
        const val TYPE_OPTIONS_POPUP_DIALOG = 1 shl 14
        const val TYPE_PIN_WIDGET_FROM_EXTERNAL_POPUP = 1 shl 15
        const val TYPE_TASKBAR_EDUCATION_DIALOG = 1 shl 16
        const val TYPE_TASKBAR_ALL_APPS = 1 shl 17
        const val TYPE_ADD_TO_HOME_CONFIRMATION = 1 shl 18
        const val TYPE_TASKBAR_OVERLAY_PROXY = 1 shl 19
        const val TYPE_TASKBAR_PINNING_POPUP = 1 shl 20
        const val TYPE_PIN_IME_POPUP = 1 shl 21
        const val TYPE_ONE_GRID_MIGRATION_EDU = 1 shl 22
        const val TYPE_NUDGE = 1 shl 23
        const val TYPE_TASKBAR_OVERFLOW = 1 shl 24
        const val TYPE_DIALOG_LISTENER = 1 shl 25

        const val TYPE_ALL = 0xFFFFFFF
        const val TYPE_HIDE_BACK_BUTTON = TYPE_ON_BOARD_POPUP or TYPE_DISCOVERY_BOUNCE

        @JvmStatic
        fun <T : Context> getTopOpenView(activityContext: T): AbstractFloatingView? {
            return getTopOpenViewWithType(activityContext, TYPE_ALL)
        }

        @JvmStatic
        fun <T : Context> getTopOpenViewWithType(activityContext: T, type: Int): AbstractFloatingView? {
            return getOpenView(activityContext, type)
        }

        @JvmStatic
        fun <T : Context> getOpenView(activityContext: T, type: Int): AbstractFloatingView? {
            val dragLayer = (activityContext as? ActivityContext)?.dragLayer ?: return null
            for (i in dragLayer.childCount - 1 downTo 0) {
                val child = dragLayer.getChildAt(i)
                if (child is AbstractFloatingView && child.isOpen && (child.isOfType(type))) {
                    return child
                }
            }
            return null
        }

        @JvmStatic
        fun <T : Context> closeAllOpenViews(activityContext: T, animate: Boolean) {
            closeOpenViews(activityContext, animate, TYPE_ALL)
        }

        @JvmStatic
        fun <T : Context> closeOpenViews(activityContext: T, animate: Boolean, type: Int) {
            val dragLayer = (activityContext as? ActivityContext)?.dragLayer ?: return
            for (i in dragLayer.childCount - 1 downTo 0) {
                val child = dragLayer.getChildAt(i)
                if (child is AbstractFloatingView && child.isOpen && child.isOfType(type)) {
                    child.close(animate)
                }
            }
        }

        @JvmStatic
        fun <T : Context> hasOpenView(activityContext: T, type: Int): Boolean {
            return getOpenView(activityContext, type) != null
        }
    }

    protected var mIsOpen: Boolean = false
    open val isOpen: Boolean get() = mIsOpen

    abstract fun handleClose(animate: Boolean)

    open fun close(animate: Boolean) {
        handleClose(animate)
        mIsOpen = false
    }

    abstract fun isOfType(type: Int): Boolean

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean = false

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean = false

    open fun canInterceptEventsInDescendingOrder(): Boolean = false
}
