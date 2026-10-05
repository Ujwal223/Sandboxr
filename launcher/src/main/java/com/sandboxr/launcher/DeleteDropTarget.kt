/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import com.sandboxr.launcher.DropTarget.DragObject
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Drop target for deleting/removing items from the workspace (or cancelling a drag).
 * Provides distinct Liquid Glass red warning feedback when hovered.
 */
open class DeleteDropTarget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ButtonDropTarget(context, attrs, defStyle) {

    private val activeGlowBackground = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 32f
        setColor(0x33FF453A.toInt()) // Liquid Glass crimson warning tint
        setStroke(2, 0x80FF453A.toInt())
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setDrawable(R.drawable.ic_remove_no_shadow)
        if (text.isNullOrEmpty()) {
            setText(R.string.remove_drop_target_label)
        }
    }

    override fun supportsDrop(info: ItemInfo?): Boolean {
        if (info == null) return false
        return true
    }

    fun canRemove(item: ItemInfo?): Boolean {
        if (item == null) return false
        return item.id != ItemInfo.NO_ID
    }

    override fun onDragStart(dragObject: DragObject, options: DragOptions) {
        super.onDragStart(dragObject, options)
        val item = dragObject.dragInfo
        if (canRemove(item)) {
            setText(R.string.remove_drop_target_label)
        } else {
            setText(R.string.cancel_drop_target_label)
        }
        mText = text
        mActive = true
        visibility = VISIBLE
    }

    override fun onDragEnter(dragObject: DragObject) {
        super.onDragEnter(dragObject)
        background = activeGlowBackground
        setTextColor(Color.parseColor("#FFFF6961"))
    }

    override fun onDragExit(dragObject: DragObject) {
        super.onDragExit(dragObject)
        background = null
        setTextColor(Color.WHITE)
    }

    override fun onDrop(dragObject: DragObject, options: Any?) {
        background = null
        setTextColor(Color.WHITE)
        mDropTargetBar?.deferOnDragEnd()
        super.onDrop(dragObject, options)
    }

    override fun completeDrop(dragObject: DragObject) {
        val item = dragObject.dragInfo ?: return
        if (canRemove(item)) {
            mDropTargetHandler?.onDeleteComplete(item, dragObject.dragView)
        }
    }

    override fun getSupportedAccessibilityAction(info: ItemInfo?, view: View?): Int {
        return if (canRemove(info)) R.id.action_remove else -1
    }

    override fun onAccessibilityDrop(view: View?, info: ItemInfo?, action: Int) {
        if (info != null && canRemove(info)) {
            mDropTargetHandler?.onDeleteComplete(info, view)
        }
    }
}
