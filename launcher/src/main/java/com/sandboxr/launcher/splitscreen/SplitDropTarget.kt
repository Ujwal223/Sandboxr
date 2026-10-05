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

package com.sandboxr.launcher.splitscreen

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.sandboxr.launcher.R
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT
import com.sandboxr.launcher.splitscreen.SplitConfigurationOptions.StagePosition

/**
 * Drop target view positioned on display edges to initiate split-screen by dragging tasks.
 */
class SplitDropTarget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface SplitDropListener {
        fun onDropTask(taskId: Int, @StagePosition stagePosition: Int)
        fun onDropIntent(intent: Intent, @StagePosition stagePosition: Int)
    }

    @StagePosition
    var stagePosition: Int = STAGE_POSITION_TOP_OR_LEFT
        set(value) {
            field = value
            updateLabel()
        }

    var dropListener: SplitDropListener? = null

    private val labelView: TextView

    init {
        setBackgroundResource(R.drawable.bg_split_drop_target)
        labelView = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.materialColorOnPrimaryContainer))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER)
        addView(labelView, lp)
        updateLabel()

        setOnDragListener { _, event ->
            handleDragEvent(event)
        }
    }

    private fun updateLabel() {
        val textRes = when (stagePosition) {
            STAGE_POSITION_TOP_OR_LEFT -> R.string.split_screen_position_top
            STAGE_POSITION_BOTTOM_OR_RIGHT -> R.string.split_screen_position_bottom
            else -> R.string.recent_task_option_split_screen
        }
        labelView.setText(textRes)
    }

    fun setHighlighted(highlighted: Boolean) {
        alpha = if (highlighted) 1.0f else 0.7f
        scaleX = if (highlighted) 1.05f else 1.0f
        scaleY = if (highlighted) 1.05f else 1.0f
    }

    private fun handleDragEvent(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                visibility = View.VISIBLE
                setHighlighted(false)
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                setHighlighted(true)
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                setHighlighted(false)
                return true
            }
            DragEvent.ACTION_DROP -> {
                setHighlighted(false)
                val clipData = event.clipData
                if (clipData != null && clipData.itemCount > 0) {
                    val item = clipData.getItemAt(0)
                    val intent = item.intent
                    val text = item.text?.toString()
                    val taskId = text?.toIntOrNull()
                    if (taskId != null) {
                        dropListener?.onDropTask(taskId, stagePosition)
                    } else if (intent != null) {
                        dropListener?.onDropIntent(intent, stagePosition)
                    }
                }
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                visibility = View.GONE
                setHighlighted(false)
                return true
            }
        }
        return false
    }

    fun isPointInside(x: Float, y: Float): Boolean {
        val rect = Rect()
        getGlobalVisibleRect(rect)
        return rect.contains(x.toInt(), y.toInt())
    }
}
