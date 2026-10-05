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

package com.sandboxr.launcher

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.anim.PropertySetter
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.celllayout.CellPosMapper
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.dragndrop.DragView
import com.sandboxr.launcher.graphics.DragPreviewProvider
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.pageindicators.PageIndicator
import com.sandboxr.launcher.statemanager.StateManager
import com.sandboxr.launcher.states.StateAnimationConfig
import com.sandboxr.launcher.util.IntArray
import com.sandboxr.launcher.util.IntSet
import com.sandboxr.launcher.util.IntSparseArrayMap

/**
 * The workspace contains desktop screens of cells, shortcuts, folders, and app widgets.
 * Manages paginated CellLayout pages, drag and drop targets, spring-loaded scaling,
 * and launcher state machine transitions.
 */
open class Workspace<T : View> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PagedView<T>(context, attrs, defStyleAttr),
    WorkspaceLayoutManager,
    CellLayoutContainer,
    DropTarget,
    DragSource,
    Insettable,
    StateManager.StateHandler<LauncherState> {

    @VisibleForTesting
    val mWorkspaceScreens = IntSparseArrayMap<CellLayout>()

    @VisibleForTesting
    val mScreenOrder = IntArray()

    private val mInsets = Rect()
    private var mCellPosMapper: CellPosMapper = CellPosMapper.DEFAULT
    protected var mLauncher: Launcher? = null

    var workspaceScale: Float = 1f
        set(value) {
            field = value
            scaleX = value
            scaleY = value
        }

    private var mTransitionProgress: Float = 1f
    private var mIsSpringLoaded: Boolean = false

    init {
        mLauncher = runCatching { Launcher.getLauncher(context) }.getOrNull()
        // Bind the initial screen on initialization
        bindAndInitFirstWorkspaceScreen()
    }

    override fun setInsets(insets: Rect) {
        mInsets.set(insets)
        setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    override fun getCellPosMapper(): CellPosMapper = mCellPosMapper

    fun setCellPosMapper(mapper: CellPosMapper) {
        mCellPosMapper = mapper
    }

    override fun getHotseat(): Hotseat? = mLauncher?.getHotseat()

    override fun getScreenWithId(screenId: Int): CellLayout? = mWorkspaceScreens.get(screenId)

    fun getScreenOrder(): IntArray = mScreenOrder

    fun getScreenIdForPageIndex(index: Int): Int {
        return if (index in 0 until mScreenOrder.size()) mScreenOrder.get(index) else -1
    }

    fun getPageIndexForScreenId(screenId: Int): Int {
        return mScreenOrder.indexOf(screenId)
    }

    /**
     * Returns the CellLayout associated with the current page.
     */
    open fun getCurrentCellLayout(): CellLayout? {
        val current = getCurrentPage()
        return if (current in 0 until childCount) getChildAt(current) as? CellLayout else null
    }

    /**
     * Initializes and binds the first desktop screen.
     */
    fun bindAndInitFirstWorkspaceScreen() {
        if (!mWorkspaceScreens.containsKey(FIRST_SCREEN_ID)) {
            insertNewWorkspaceScreen(FIRST_SCREEN_ID, childCount)
        }
    }

    /**
     * Inserts a new workspace screen at the end of the pages.
     */
    open fun insertNewWorkspaceScreen(screenId: Int): CellLayout {
        return insertNewWorkspaceScreen(screenId, childCount)
    }

    /**
     * Inserts a new workspace screen at the specified index.
     */
    open fun insertNewWorkspaceScreen(screenId: Int, insertIndex: Int): CellLayout {
        if (mWorkspaceScreens.containsKey(screenId)) {
            return mWorkspaceScreens.get(screenId)!!
        }

        val newScreen = CellLayout(context).apply {
            setCellLayoutContainer(this@Workspace)
        }

        mWorkspaceScreens.put(screenId, newScreen)
        mScreenOrder.add(insertIndex.coerceIn(0, mScreenOrder.size()), screenId)
        addView(newScreen, insertIndex.coerceIn(0, childCount))

        updatePageIndicator()
        return newScreen
    }

    /**
     * Inserts a new screen before any trailing extra empty screens.
     */
    open fun insertNewWorkspaceScreenBeforeEmptyScreen(screenId: Int): CellLayout {
        var insertIndex = mScreenOrder.indexOf(EXTRA_EMPTY_SCREEN_ID)
        if (insertIndex < 0) {
            insertIndex = mScreenOrder.size()
        }
        return insertNewWorkspaceScreen(screenId, insertIndex)
    }

    /**
     * Removes the workspace screen with the given ID.
     */
    open fun removeWorkspaceScreen(screenId: Int) {
        val screen = mWorkspaceScreens.get(screenId) ?: return
        val index = mScreenOrder.indexOf(screenId)
        if (index >= 0) {
            mScreenOrder.removeValue(screenId)
        }
        mWorkspaceScreens.remove(screenId)
        removeView(screen)
        updatePageIndicator()
    }

    /**
     * Removes all workspace screens.
     */
    open fun removeAllWorkspaceScreens() {
        mWorkspaceScreens.clear()
        mScreenOrder.clear()
        removeAllViews()
        updatePageIndicator()
    }

    /**
     * Adds an extra empty screen at the end for drag target drops.
     */
    open fun addExtraEmptyScreens() {
        if (!mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            insertNewWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID, childCount)
        }
    }

    /**
     * Removes the extra empty screen.
     */
    open fun removeExtraEmptyScreens() {
        if (mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            removeWorkspaceScreen(EXTRA_EMPTY_SCREEN_ID)
        }
    }

    /**
     * Commits extra empty screens and returns their IDs.
     */
    open fun commitExtraEmptyScreens(): IntSet {
        val emptyPageIds = IntSet()
        if (mWorkspaceScreens.containsKey(EXTRA_EMPTY_SCREEN_ID)) {
            val cl = mWorkspaceScreens.get(EXTRA_EMPTY_SCREEN_ID)
            mWorkspaceScreens.remove(EXTRA_EMPTY_SCREEN_ID)
            mScreenOrder.removeValue(EXTRA_EMPTY_SCREEN_ID)
            var newScreenId = 0
            while (mWorkspaceScreens.containsKey(newScreenId)) {
                newScreenId++
            }
            if (cl != null) {
                mWorkspaceScreens.put(newScreenId, cl)
            }
            mScreenOrder.add(newScreenId)
            emptyPageIds.add(newScreenId)
        }
        return emptyPageIds
    }

    private var mOnPageTransitionEndCallback: Runnable? = null

    open fun setOnPageTransitionEndCallback(callback: Runnable?) {
        mOnPageTransitionEndCallback = callback
    }

    override fun onPageEndTransition() {
        super.onPageEndTransition()
        mOnPageTransitionEndCallback?.run()
        mOnPageTransitionEndCallback = null
    }

    fun isOverlayShown(): Boolean = false

    open fun removeWidget(appWidgetId: Int) {
        // Will be connected to LauncherAppWidgetHost in Phase 6
    }

    open fun removeWorkspaceItem(view: View?, item: com.sandboxr.launcher.model.data.ItemInfo?) {
        if (view != null) {
            val parent = view.parent
            if (parent is android.view.ViewGroup) {
                parent.removeView(view)
            }
        }
        if (item != null && view == null) {
            for (i in 0 until childCount) {
                val screen = getChildAt(i)
                if (screen is CellLayout) {
                    val container = screen.shortcutsAndWidgets
                    for (j in 0 until container.childCount) {
                        val child = container.getChildAt(j)
                        val tag = child.tag as? com.sandboxr.launcher.model.data.ItemInfo
                        if (tag == item || (item.id != com.sandboxr.launcher.model.data.ItemInfo.NO_ID && tag?.id == item.id)) {
                            container.removeView(child)
                            break
                        }
                    }
                }
            }
        }
    }

    open fun stripEmptyScreens() {
        removeExtraEmptyScreens()
    }

    // --- CellLayoutContainer Implementation ---

    override fun getCellLayoutId(cellLayout: CellLayout): Int {
        for (i in 0 until mScreenOrder.size()) {
            val id = mScreenOrder.get(i)
            if (mWorkspaceScreens.get(id) === cellLayout) {
                return id
            }
        }
        return -1
    }

    override fun getCellLayoutIndex(cellLayout: CellLayout): Int {
        return indexOfChild(cellLayout)
    }

    override fun getPanelCount(): Int = 1

    override fun getPageDescription(pageIndex: Int): String {
        return "Page ${pageIndex + 1} of $pageCount"
    }

    // --- Drag and Drop & Spring Loaded Mode ---

    open fun enterSpringLoadedMode() {
        if (!mIsSpringLoaded) {
            mIsSpringLoaded = true
            addExtraEmptyScreens()
            animate().scaleX(SPRING_LOADED_SCALE).scaleY(SPRING_LOADED_SCALE)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    open fun exitSpringLoadedMode() {
        if (mIsSpringLoaded) {
            mIsSpringLoaded = false
            removeExtraEmptyScreens()
            animate().scaleX(1.0f).scaleY(1.0f)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    /** Returns true if the workspace is currently handling a drag/scroll touch sequence. */
    open fun isHandlingTouch(): Boolean = !mScroller.isFinished

    /** Called when the user taps on the wallpaper area (no item under touch). */
    open fun onWallpaperTap(ev: MotionEvent) {
        // Default: no action. Override to show wallpaper picker or similar.
    }

    open fun startDrag(v: View, info: ItemInfo) {
        enterSpringLoadedMode()
    }

    open fun beginDragShared(child: View, source: View?, info: ItemInfo) {
        enterSpringLoadedMode()
    }

    open fun beginDragShared(child: View, source: View?, dragOptions: DragOptions) {
        enterSpringLoadedMode()
    }

    open fun beginDragShared(
        child: View,
        draggableView: DraggableView?,
        source: DragSource?,
        info: ItemInfo,
        dragPreviewProvider: DragPreviewProvider?,
        dragOptions: DragOptions?,
    ): DragView? {
        enterSpringLoadedMode()
        return null
    }

    override fun onDrop(dragObject: DropTarget.DragObject, options: Any?) {
        val info = dragObject.dragInfo ?: return
        val screenId = getCurrentPage()
        val cellLayout = getScreenWithId(screenId) ?: return

        val cell = kotlin.IntArray(2)
        if (dragObject.x >= 0 && dragObject.y >= 0) {
            cellLayout.pointToCellExact(dragObject.x, dragObject.y, cell)
        } else {
            cellLayout.findCellForSpan(cell, info.spanX, info.spanY)
        }

        info.cellX = cell[0].coerceAtLeast(0)
        info.cellY = cell[1].coerceAtLeast(0)
        info.screenId = screenId
        info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP

        val launcher = try { Launcher.getLauncher(context) } catch (e: Exception) { null }
        launcher?.getModelWriter()?.addItemToDatabase(
            info,
            info.container,
            info.screenId,
            info.cellX,
            info.cellY
        )

        if (info is com.sandboxr.launcher.model.data.ItemInfoWithIcon) {
            val childView = BubbleTextView(context).apply {
                applyFromItemInfoWithIcon(info)
            }
            val lp = CellLayoutLayoutParams(info.cellX, info.cellY, info.spanX, info.spanY)
            cellLayout.addViewToCellLayout(childView, -1, info.id.toInt(), lp, true)
        }

        dragObject.dragComplete = true
        exitSpringLoadedMode()
    }

    override fun onDropCompleted(target: View?, d: DropTarget.DragObject, success: Boolean) {
        exitSpringLoadedMode()
    }

    // --- StateHandler<LauncherState> Implementation ---

    override fun setState(state: LauncherState) {
        when (state) {
            LauncherState.NORMAL -> {
                visibility = View.VISIBLE
                alpha = 1f
                workspaceScale = 1f
                translationX = 0f
                translationY = 0f
                exitSpringLoadedMode()
            }
            LauncherState.SPRING_LOADED -> {
                visibility = View.VISIBLE
                enterSpringLoadedMode()
            }
            LauncherState.ALL_APPS -> {
                visibility = View.GONE
                alpha = 0f
            }
            LauncherState.OVERVIEW -> {
                visibility = View.VISIBLE
                alpha = 0.5f
                workspaceScale = 0.8f
            }
            LauncherState.EDIT_MODE -> {
                visibility = View.VISIBLE
                workspaceScale = 0.9f
            }
            else -> {
                visibility = View.VISIBLE
            }
        }
    }

    override fun setStateWithAnimation(
        toState: LauncherState,
        config: StateAnimationConfig,
        animation: com.sandboxr.launcher.anim.PendingAnimation
    ) {
        setStateWithAnimation(toState, config, animation as PropertySetter)
    }

    open fun setStateWithAnimation(
        toState: LauncherState,
        config: StateAnimationConfig,
        setter: PropertySetter
    ) {
        when (toState) {
            LauncherState.NORMAL -> {
                setter.setViewAlpha(this, 1f, config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_FADE, DecelerateInterpolator()))
                setter.setFloat(this, WORKSPACE_SCALE_PROPERTY, 1f, config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_SCALE, DecelerateInterpolator()))
            }
            LauncherState.SPRING_LOADED -> {
                setter.setFloat(this, WORKSPACE_SCALE_PROPERTY, SPRING_LOADED_SCALE, config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_SCALE, DecelerateInterpolator()))
            }
            LauncherState.ALL_APPS -> {
                setter.setViewAlpha(this, 0f, config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_FADE, DecelerateInterpolator()))
            }
            LauncherState.OVERVIEW -> {
                setter.setFloat(this, WORKSPACE_SCALE_PROPERTY, 0.8f, config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_SCALE, DecelerateInterpolator()))
                setter.setViewAlpha(this, 0.5f, config.getInterpolator(StateAnimationConfig.ANIM_WORKSPACE_FADE, DecelerateInterpolator()))
            }
            else -> {
                setter.setViewAlpha(this, 1f, DecelerateInterpolator())
            }
        }
    }

    companion object {
        const val FIRST_SCREEN_ID: Int = 0
        const val EXTRA_EMPTY_SCREEN_ID: Int = -201
        const val EXTRA_EMPTY_SCREEN_SECOND_ID: Int = -200
        @JvmField
        val EXTRA_EMPTY_SCREEN_IDS: IntSet = WorkspaceLayoutManager.EXTRA_EMPTY_SCREEN_IDS

        const val SPRING_LOADED_SCALE: Float = 0.88f
        const val REORDER_TIMEOUT: Int = 650

        @JvmField
        val WORKSPACE_SCALE_PROPERTY: FloatProperty<Workspace<*>> =
            object : FloatProperty<Workspace<*>>("workspaceScale") {
                override fun setValue(target: Workspace<*>, value: Float) {
                    target.workspaceScale = value
                }

                override fun get(target: Workspace<*>): Float {
                    return target.workspaceScale
                }
            }
    }
}
