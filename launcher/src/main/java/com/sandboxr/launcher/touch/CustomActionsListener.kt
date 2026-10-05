/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.touch

import android.view.View
import com.sandboxr.launcher.AbstractFloatingViewHelper
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.DropTarget.DragObject
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.views.AbstractFloatingView
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.views.BubbleTextHolder
import com.sandboxr.launcher.widget.LauncherAppWidgetHostView

/**
 * Interface for listening to custom actions performed on a view (tap, long press, right-click, drag).
 */
interface CustomActionsListener {

    fun performActions(view: View, actionMask: Int)

    companion object {
        const val ACTION_POPUP_MENU: Int = 1 shl 0
        const val ACTION_START_DRAG: Int = 1 shl 1
        const val ACTION_LAUNCH: Int = 1 shl 2

        @JvmStatic
        fun hasFlags(mask: Int, flags: Int): Boolean = (mask and flags) == flags
    }
}

private val View.logicalTarget: View
    get() = (parent as? BubbleTextHolder)?.let { it as? View } ?: this

private val View.asBubbleTextView: BubbleTextView?
    get() = this as? BubbleTextView ?: (this as? BubbleTextHolder)?.bubbleText

/** Base class for listeners that act on specific items (Icons, Folders, App Pairs). */
abstract class BaseItemCustomActionsListener : CustomActionsListener {

    final override fun performActions(view: View, actionMask: Int) {
        val target = view.logicalTarget
        val btv = target.asBubbleTextView

        when {
            CustomActionsListener.hasFlags(
                actionMask,
                CustomActionsListener.ACTION_POPUP_MENU or CustomActionsListener.ACTION_START_DRAG
            ) -> target.performLongClick()

            CustomActionsListener.hasFlags(actionMask, CustomActionsListener.ACTION_LAUNCH) ->
                target.performClick()

            CustomActionsListener.hasFlags(actionMask, CustomActionsListener.ACTION_POPUP_MENU) ->
                onOpenPopupMenu(target, btv)

            CustomActionsListener.hasFlags(actionMask, CustomActionsListener.ACTION_START_DRAG) ->
                onStartDrag(target, btv)
        }
    }

    abstract fun onOpenPopupMenu(target: View, btv: BubbleTextView?)
    abstract fun onStartDrag(target: View, btv: BubbleTextView?)
}

/** Implementation of [CustomActionsListener] for widgets. */
object WorkspaceWidgetCustomActionsListener : CustomActionsListener {
    override fun performActions(view: View, actionMask: Int) {
        val widgetHostView = view as? LauncherAppWidgetHostView ?: return
        when {
            CustomActionsListener.hasFlags(
                actionMask,
                CustomActionsListener.ACTION_POPUP_MENU or CustomActionsListener.ACTION_START_DRAG
            ) -> widgetHostView.performLongClick()

            CustomActionsListener.hasFlags(actionMask, CustomActionsListener.ACTION_POPUP_MENU) -> {
                val launcher = Launcher.getLauncher(view.context)
                AbstractFloatingViewHelper.closeOpenViews(
                    launcher, true, AbstractFloatingView.TYPE_ALL
                )
            }

            CustomActionsListener.hasFlags(actionMask, CustomActionsListener.ACTION_START_DRAG) -> {
                val launcher = Launcher.getLauncher(view.context)
                if (ItemLongClickListener.canStartDrag(launcher)) {
                    val options = DragOptions().apply { isMouseDrag = true }
                    val tag = view.tag as? ItemInfo ?: return
                    launcher.getWorkspace()?.beginDragShared(view, null, tag)
                }
            }
        }
    }
}

/** Implementation of [BaseItemCustomActionsListener] for workspace items. */
object WorkspaceItemCustomActionsListener : BaseItemCustomActionsListener() {
    override fun onOpenPopupMenu(target: View, btv: BubbleTextView?) {
        val viewForPopup = btv ?: target
        viewForPopup.performLongClick()
    }

    override fun onStartDrag(target: View, btv: BubbleTextView?) {
        val viewForDrag = btv ?: target
        val tag = viewForDrag.tag as? ItemInfo ?: return
        val launcher = Launcher.getLauncher(viewForDrag.context)
        if (ItemLongClickListener.canStartDrag(launcher)) {
            launcher.getWorkspace()?.beginDragShared(viewForDrag, null, tag)
        }
    }
}

/** Implementation of [BaseItemCustomActionsListener] for AllApps items. */
object AllAppsItemCustomActionsListener : BaseItemCustomActionsListener() {
    override fun onOpenPopupMenu(target: View, btv: BubbleTextView?) {
        if (btv == null) return
        btv.performLongClick()
    }

    override fun onStartDrag(target: View, btv: BubbleTextView?) {
        if (btv == null) return
        val launcher = Launcher.getLauncher(btv.context)
        if (!ItemLongClickListener.canStartDrag(launcher)) return

        val dragOptions = DragOptions().apply { isMouseDrag = true }
        val info = btv.tag as? ItemInfo
        if (info != null) {
            launcher.getWorkspace()?.beginDragShared(
                btv,
                btv as? DraggableView,
                launcher.getAppsView() as? DragSource,
                info,
                null,
                dragOptions
            )
        } else {
            launcher.getWorkspace()?.beginDragShared(btv, launcher.getAppsView(), dragOptions)
        }
    }
}
