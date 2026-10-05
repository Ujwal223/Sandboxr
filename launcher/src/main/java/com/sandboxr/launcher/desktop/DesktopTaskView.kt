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

package com.sandboxr.launcher.desktop

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.android.systemui.shared.recents.model.Task
import com.sandboxr.launcher.R
import com.sandboxr.launcher.recents.views.TaskView
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailView

/**
 * TaskView subclass or container that represents multiple windowed tasks
 * grouped on a desktop workspace in the recents overview.
 */
class DesktopTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TaskView(context, attrs, defStyleAttr) {

    var desktopTask: DesktopTask? = null
        private set

    val deskId: Int
        get() = desktopTask?.deskId ?: DesktopVisibilityController.INACTIVE_DESK_ID

    var contentView: DesktopTaskContentView? = null
        private set

    var backgroundView: View? = null
        private set

    val taskThumbnailViews = mutableListOf<TaskThumbnailView>()

    /**
     * Controls the layout spread of freeform windows in overview.
     */
    var explodeProgress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            positionTaskWindows()
        }

    var onWindowSelected: ((taskId: Int) -> Unit)? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        contentView = findViewById(R.id.desktop_content)
        backgroundView = findViewById(R.id.background)
        backgroundView?.setBackgroundColor(
            ContextCompat.getColor(context, R.color.materialColorSurfaceContainerHigh)
        )
    }

    /**
     * Binds a [DesktopTask] containing multiple windowed tasks to this view.
     */
    fun bind(desktopTask: DesktopTask) {
        this.desktopTask = desktopTask
        val tasks = desktopTask.tasks
        contentView?.removeAllViews()
        taskThumbnailViews.clear()

        // Bind first task to super if non-empty
        if (tasks.isNotEmpty()) {
            super.bind(tasks[0])
        }

        // Add thumbnails for each windowed task
        for (t in tasks) {
            val thumbnail = TaskThumbnailView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    onWindowSelected?.invoke(t.key.id)
                }
            }
            taskThumbnailViews.add(thumbnail)
            contentView?.addView(thumbnail)
        }

        positionTaskWindows()
    }

    /**
     * Positions window thumbnails according to explodeProgress and available layout bounds.
     */
    fun positionTaskWindows() {
        if (taskThumbnailViews.isEmpty()) return
        val count = taskThumbnailViews.size
        val w = measuredWidth.coerceAtLeast(1)
        val h = measuredHeight.coerceAtLeast(1)

        val cols = if (count <= 2) count else 2
        val rows = (count + cols - 1) / cols

        val itemWidth = (w / cols) * 0.85f
        val itemHeight = (h / rows) * 0.85f

        for (i in 0 until count) {
            val thumb = taskThumbnailViews[i]
            val r = i / cols
            val c = i % cols

            val targetLeft = c * (w / cols.toFloat()) + (w / cols.toFloat() - itemWidth) / 2f
            val targetTop = r * (h / rows.toFloat()) + (h / rows.toFloat() - itemHeight) / 2f

            thumb.layoutParams = (thumb.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(0, 0)).apply {
                width = itemWidth.toInt()
                height = itemHeight.toInt()
                leftMargin = (targetLeft * explodeProgress).toInt()
                topMargin = (targetTop * explodeProgress).toInt()
                gravity = Gravity.TOP or Gravity.START
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            positionTaskWindows()
        }
    }
}
