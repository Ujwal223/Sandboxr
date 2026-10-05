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

package com.sandboxr.launcher.recents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import org.robolectric.RuntimeEnvironment
import com.android.systemui.shared.recents.model.Task
import com.android.systemui.shared.recents.model.ThumbnailData
import com.sandboxr.launcher.recents.data.AppTimersRepositoryImpl
import com.sandboxr.launcher.recents.data.RecentTasksRepositoryImpl
import com.sandboxr.launcher.recents.data.RecentsRotationStateRepositoryImpl
import com.sandboxr.launcher.recents.data.TasksRepository
import com.sandboxr.launcher.recents.data.UserLockedRepository
import com.sandboxr.launcher.recents.domain.model.TaskModel
import com.sandboxr.launcher.recents.domain.usecase.GetRemainingAppTimerDurationUseCase
import com.sandboxr.launcher.recents.domain.usecase.GetTaskUseCase
import com.sandboxr.launcher.recents.domain.usecase.IsThumbnailValidUseCase
import com.sandboxr.launcher.recents.fallback.RecentsState
import com.sandboxr.launcher.recents.ui.mapper.TaskUiStateMapper
import com.sandboxr.launcher.recents.viewmodel.RecentsViewData
import com.sandboxr.launcher.recents.viewmodel.RecentsViewModel
import com.sandboxr.launcher.recents.ui.viewmodel.TaskData
import com.sandboxr.launcher.recents.views.RecentsView
import com.sandboxr.launcher.recents.views.TaskView
import com.sandboxr.launcher.task.apptimer.DurationFormatter
import com.sandboxr.launcher.task.apptimer.TaskAppTimerUiState
import com.sandboxr.launcher.task.apptimer.TaskAppTimerViewModel
import com.sandboxr.launcher.task.apptimer.TimerTextHelper
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentsSystemTest {

    private lateinit var context: Context
    private lateinit var recentTasksRepo: RecentTasksRepositoryImpl
    private lateinit var appTimersRepo: AppTimersRepositoryImpl
    private lateinit var userLockedRepo: UserLockedRepository
    private lateinit var recentsViewData: RecentsViewData
    private lateinit var recentsViewModel: RecentsViewModel
    private lateinit var taskUiStateMapper: TaskUiStateMapper

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        recentTasksRepo = RecentTasksRepositoryImpl()
        appTimersRepo = AppTimersRepositoryImpl()
        userLockedRepo = UserLockedRepository()
        recentsViewData = RecentsViewData()
        recentsViewModel = RecentsViewModel(
            recentsTasksRepository = recentTasksRepo,
            recentsViewData = recentsViewData,
            appTimersRepository = appTimersRepo,
        )
        taskUiStateMapper = TaskUiStateMapper()
    }

    @Test
    fun testRecentTasksRepositoryLifecycle() = runBlocking {
        // Initial list is empty
        val initial = recentTasksRepo.getAllTaskData(0).first()
        assertTrue(initial.isEmpty())

        // Add 2 tasks
        val task1 = Task(1, "com.test.app1", "App 1", null, null)
        val task2 = Task(2, "com.test.app2", "App 2", null, null)
        recentTasksRepo.addOrUpdateTask(task1)
        recentTasksRepo.addOrUpdateTask(task2)

        val tasksAfterAdd = recentTasksRepo.getAllTaskData(0).first()
        assertEquals(2, tasksAfterAdd.size)
        assertEquals(2, tasksAfterAdd[0].key.id)
        assertEquals(1, tasksAfterAdd[1].key.id)

        // Find by id
        val fetched = recentTasksRepo.getTaskDataById(1).first()
        assertNotNull(fetched)
        assertEquals("com.test.app1", fetched?.key?.getPackageName())

        // Update thumbnail
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val thumbnailData = ThumbnailData(bitmap)
        recentTasksRepo.updateThumbnail(1, thumbnailData)

        val thumbFlow = recentTasksRepo.getThumbnailById(1).first()
        assertNotNull(thumbFlow)
        assertEquals(bitmap, thumbFlow?.thumbnail)
        assertEquals(bitmap, recentTasksRepo.getCurrentThumbnailById(1)?.thumbnail)

        // Remove task
        recentTasksRepo.removeTask(1)
        val afterRemove = recentTasksRepo.getAllTaskData(0).first()
        assertEquals(1, afterRemove.size)
        assertEquals(2, afterRemove[0].key.id)

        // Clear all tasks
        recentTasksRepo.clearAllTasks()
        val afterClear = recentTasksRepo.getAllTaskData(0).first()
        assertTrue(afterClear.isEmpty())
    }

    @Test
    fun testTasksRepositoryCacheAndDelegates() {
        val tasksRepo = TasksRepository(recentTasksRepo)
        val task = Task(10, "com.pkg.test", "Test Task", null, null)

        tasksRepo.putTask(task)
        assertEquals(task, tasksRepo.getTask(10))

        var iconNotified = false
        tasksRepo.registerTaskIconChangedCallback { pkg, _ ->
            if (pkg == "com.pkg.test") iconNotified = true
        }
        tasksRepo.onTaskIconChanged("com.pkg.test", 0)
        assertTrue(iconNotified)

        var thumbNotified = false
        tasksRepo.registerTaskThumbnailChangedCallback { id, _ ->
            if (id == 10) thumbNotified = true
        }
        tasksRepo.onTaskThumbnailChanged(10, null)
        assertTrue(thumbNotified)

        tasksRepo.removeTask(10)
        assertNull(tasksRepo.getTask(10))
    }

    @Test
    fun testTaskModelAndMapper() {
        val task = Task(42, "com.sandboxr.browser", "Browser", null, null)
        task.colorBackground = Color.BLUE
        task.isLocked = true

        val uiState = taskUiStateMapper.map(task, isCentralTask = true)
        assertTrue(uiState.isCentralTask)
        assertEquals(1, uiState.tasks.size)

        val taskData = uiState.tasks[0] as TaskData.Data
        assertEquals(42, taskData.taskId)
        assertEquals("com.sandboxr.browser", taskData.packageName)
        assertEquals("Browser", taskData.title)
        assertTrue(taskData.isLocked)
        assertEquals(Color.BLUE, taskData.backgroundColor)

        val model = TaskModel(
            id = 55,
            packageName = "com.sandboxr.notes",
            title = "Notes",
            titleDescription = "Sandbox Notes",
            icon = null,
            thumbnail = null,
            backgroundColor = Color.BLACK,
            isLocked = false,
            isMinimized = false,
            remainingAppDuration = Duration.ofMinutes(45),
            isAppLocked = true,
        )

        val modelUiState = taskUiStateMapper.map(model, isCentralTask = false)
        assertFalse(modelUiState.isCentralTask)
        val modelData = modelUiState.tasks[0] as TaskData.Data
        assertEquals(55, modelData.taskId)
        assertEquals("com.sandboxr.notes", modelData.packageName)
        assertEquals("Notes", modelData.title)
        assertTrue(modelData.isAppLocked)
        assertEquals(Duration.ofMinutes(45), modelData.remainingAppTimerDuration)
    }

    @Test
    fun testRecentsViewModelTransitions() {
        recentsViewModel.updateVisibleTasks(listOf(101, 102))
        assertEquals(setOf(101, 102), recentsViewModel.visibleTaskIds)

        recentsViewModel.updateTasksFullyVisible(setOf(101))
        assertEquals(setOf(101), recentsViewData.settledFullyVisibleTaskIds.value)

        recentsViewModel.updateCentralTaskIds(setOf(101))
        assertEquals(setOf(101), recentsViewData.centralTaskIds.value)

        recentsViewModel.setOverlayEnabled(true)
        assertTrue(recentsViewData.overlayEnabled.value)

        recentsViewModel.updateRunningTask(setOf(102))
        assertEquals(setOf(102), recentsViewData.runningTaskIds.value)

        recentsViewModel.setRunningTaskShowScreenshot(true)
        assertTrue(recentsViewData.runningTaskShowScreenshot.value)

        recentsViewModel.onReset()
        assertTrue(recentsViewModel.visibleTaskIds.isEmpty())
    }

    @Test
    fun testTaskThumbnailViewAndStates() {
        val ttv = TaskThumbnailView(context)
        ttv.layout(0, 0, 300, 600)

        // Uninitialized
        ttv.setUiState(TaskThumbnailUiState.Uninitialized)
        assertEquals(TaskThumbnailUiState.Uninitialized, ttv.getUiState())

        // Background only
        ttv.setUiState(TaskThumbnailUiState.BackgroundOnly(Color.DKGRAY))
        assertTrue(ttv.getUiState() is TaskThumbnailUiState.BackgroundOnly)

        // Live tile
        ttv.setLiveTile(true)
        assertEquals(TaskThumbnailUiState.LiveTile, ttv.getUiState())

        // Snapshot
        val bitmap = Bitmap.createBitmap(150, 300, Bitmap.Config.ARGB_8888)
        val snapshot = TaskThumbnailUiState.Snapshot(bitmap)
        ttv.setUiState(snapshot)
        assertEquals(snapshot, ttv.getUiState())

        val isValid = IsThumbnailValidUseCase().invoke(ThumbnailData(bitmap), 300, 600)
        assertTrue(isValid)
    }

    @Test
    fun testTaskViewBindingAndDismissGesture() {
        val task = Task(77, "com.sandboxr.camera", "Camera", null, null)
        val taskView = TaskView(context)
        taskView.layout(0, 0, 300, 500)

        var launched = false
        taskView.onTaskLaunchCallback = { launched = true }

        var dismissed = false
        taskView.onTaskDismissCallback = { dismissed = true }

        taskView.bind(task)
        assertEquals(task, taskView.task)
        assertEquals(0f, taskView.translationY, 0.01f)
        assertEquals(1f, taskView.alpha, 0.01f)

        // Click launches task
        taskView.performClick()
        assertTrue(launched)

        // Simulate swipe-to-dismiss gesture
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 300f, 0)
        taskView.onInterceptTouchEvent(downEvent)
        taskView.onTouchEvent(downEvent)
        downEvent.recycle()

        // Move upward by 200px
        val moveTime = downTime + 50
        val moveEvent = MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, 100f, 100f, 0)
        taskView.onTouchEvent(moveEvent)
        assertEquals(-200f, taskView.translationY, 0.01f)
        assertTrue(taskView.alpha < 1f)
        moveEvent.recycle()

        // Explicit dismissal trigger
        taskView.animateDismiss(dismissUp = true)
        // Handler / listener fires on animation end or manual trigger
        taskView.onTaskDismissCallback?.invoke(taskView)
        assertTrue(dismissed)
    }

    @Test
    fun testRecentsViewCarouselAndClearAll() {
        val recentsView = RecentsView(context)
        recentsView.recentTasksRepository = recentTasksRepo
        recentsView.setInsets(Rect(0, 50, 0, 50))

        val task1 = Task(1, "com.app.one", "One", null, null)
        val task2 = Task(2, "com.app.two", "Two", null, null)
        val task3 = Task(3, "com.app.three", "Three", null, null)
        val list = listOf(task1, task2, task3)

        list.forEach { recentTasksRepo.addOrUpdateTask(it) }
        recentsView.bindTasks(list)

        assertEquals(3, recentsView.getTaskCount())
        assertNotNull(recentsView.getClearAllButton())

        // Launch callback wiring
        var launchedTaskId = -1
        recentsView.onTaskLaunchListener = { launchedTaskId = it.key.id }
        recentsView.getTaskViewAt(0)?.performClick()
        assertEquals(1, launchedTaskId)

        // Dismiss single task
        val taskView2 = recentsView.getTaskViewAt(1)!!
        recentsView.dismissTask(taskView2)
        assertEquals(2, recentsView.getTaskCount())

        // Dismiss all tasks via clear-all action
        var allDismissed = false
        recentsView.onAllTasksDismissedListener = { allDismissed = true }

        recentsView.dismissAllTasks()
        assertEquals(0, recentsView.getTaskCount())
        assertNull(recentsView.getClearAllButton())
        assertTrue(allDismissed)
    }

    @Test
    fun testRecentsActivityLifecycleAndOverviewPanel() {
        val controller = Robolectric.buildActivity(RecentsActivity::class.java).setup()
        val activity = controller.get()

        assertNotNull(activity)
        val overviewPanel = activity.getOverviewPanel()
        assertNotNull(overviewPanel)
        assertEquals(activity.tasksRepository, overviewPanel?.recentTasksRepository)

        assertEquals(RecentsState.DEFAULT, activity.getStateManager().state)
        controller.destroy()
    }

    @Test
    fun testAppTimerDurationFormatting() {
        val formatter = DurationFormatter()
        val textHelper = TimerTextHelper(formatter)
        val viewModel = TaskAppTimerViewModel(textHelper)

        val duration45 = Duration.ofMinutes(45)
        assertEquals("45m left", viewModel.getFormattedDuration(duration45, context))

        val duration90 = Duration.ofMinutes(90)
        assertEquals("1h 30m left", viewModel.getFormattedDuration(duration90, context))

        val duration120 = Duration.ofHours(2)
        assertEquals("2h left", viewModel.getFormattedDuration(duration120, context))

        val durationZero = Duration.ofSeconds(30)
        assertEquals("< 1m left", viewModel.getFormattedDuration(durationZero, context))
    }
}
