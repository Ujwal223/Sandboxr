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

package com.sandboxr.launcher.recents.views

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.systemui.shared.recents.model.Task
import com.sandboxr.launcher.Insettable
import com.sandboxr.launcher.PagedView
import com.sandboxr.launcher.R
import com.sandboxr.launcher.recents.data.RecentTasksRepository

/**
 * Paged carousel view holding task cards and clear-all action in recents overview.
 */
class RecentsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : PagedView<RecentsView>(context, attrs, defStyleAttr), Insettable {

    private val mInsets = Rect()
    private val mInflater = LayoutInflater.from(context)
    private var mClearAllButton: ClearAllButton? = null

    var recentTasksRepository: RecentTasksRepository? = null
    var onTaskLaunchListener: ((Task) -> Unit)? = null
    var onAllTasksDismissedListener: (() -> Unit)? = null

    init {
        mPageSpacing = resources.getDimensionPixelSize(R.dimen.recents_page_spacing)
        layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setDuration(200)
        }
    }

    override fun setInsets(insets: Rect) {
        mInsets.set(insets)
        setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    fun bindTasks(tasks: List<Task>) {
        removeAllViews()

        for (task in tasks) {
            val taskView = mInflater.inflate(R.layout.task, this, false) as TaskView
            taskView.bind(task)
            taskView.onTaskLaunchCallback = { clickedTask ->
                onTaskLaunchListener?.invoke(clickedTask)
            }
            taskView.onTaskDismissCallback = { dismissedView ->
                dismissTask(dismissedView)
            }
            addView(taskView)
        }

        if (tasks.isNotEmpty()) {
            val clearAll = ClearAllButton(context)
            clearAll.setOnClickListener {
                dismissAllTasks()
            }
            mClearAllButton = clearAll
            addView(clearAll)
        } else {
            mClearAllButton = null
        }

        setCurrentPage(0)
    }

    fun getTaskViews(): List<TaskView> {
        val list = mutableListOf<TaskView>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is TaskView) {
                list.add(child)
            }
        }
        return list
    }

    fun getTaskViewAt(index: Int): TaskView? {
        val tasks = getTaskViews()
        return if (index in tasks.indices) tasks[index] else null
    }

    fun getTaskCount(): Int = getTaskViews().size

    fun dismissTask(taskView: TaskView) {
        val task = taskView.task
        val taskId = task?.key?.id
        if (taskId != null) {
            recentTasksRepository?.removeTask(taskId)
        }

        removeView(taskView)

        if (getTaskCount() == 0) {
            mClearAllButton?.let { removeView(it) }
            mClearAllButton = null
            onAllTasksDismissedListener?.invoke()
        } else {
            val targetPage = mCurrentPage.coerceIn(0, (childCount - 1).coerceAtLeast(0))
            snapToPage(targetPage)
        }
    }

    fun dismissAllTasks() {
        val taskViews = getTaskViews()
        for (tv in taskViews) {
            val taskId = tv.task?.key?.id
            if (taskId != null) {
                recentTasksRepository?.removeTask(taskId)
            }
        }
        recentTasksRepository?.clearAllTasks()
        removeAllViews()
        mClearAllButton = null
        onAllTasksDismissedListener?.invoke()
    }

    fun getClearAllButton(): ClearAllButton? = mClearAllButton

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val count = childCount
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val childWidth = (width * 0.75f).toInt().coerceAtLeast(1)
        val childHeight = (height * 0.72f).toInt().coerceAtLeast(1)

        val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child is TaskView) {
                child.measure(childWidthSpec, childHeightSpec)
            } else if (child is ClearAllButton) {
                child.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST)
                )
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
            }
        }
    }
}
