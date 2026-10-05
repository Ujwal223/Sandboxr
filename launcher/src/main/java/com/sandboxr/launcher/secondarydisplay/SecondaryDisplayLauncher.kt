/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.sandboxr.launcher.secondarydisplay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import com.android.launcher3.util.PackageUserKey
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.BaseActivity
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DragSource
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.LauncherModel
import com.sandboxr.launcher.R
import com.sandboxr.launcher.allapps.ActivityAllAppsContainerView
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.graphics.DragPreviewProvider
import com.sandboxr.launcher.icons.FastBitmapDrawable
import com.sandboxr.launcher.model.BgDataModel
import com.sandboxr.launcher.model.StringCache
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.WorkspaceData
import com.sandboxr.launcher.util.Themes
import com.sandboxr.launcher.views.BaseDragLayer

/**
 * Launcher activity for secondary displays.
 */
class SecondaryDisplayLauncher : BaseActivity(), BgDataModel.Callbacks {

    private var mModel: LauncherModel? = null
    private var mDragLayer: SecondaryDragLayer? = null
    private var mDragController: SecondaryDragController? = null
    private var mAppsView: ActivityAllAppsContainerView<SecondaryDisplayLauncher>? = null
    private var mAppsButton: View? = null

    private var mAppDrawerShown = false
    private var mStringCache: StringCache? = null
    private var mSecondaryDisplayDelegate: SecondaryDisplayDelegate = SecondaryDisplayDelegate()

    private var mWallpaperManager: WallpaperManager? = null
    private val mWallpaperColorsListener = WallpaperManager.OnColorsChangedListener { colors, _ ->
        updateStatusBarIconColors(colors)
    }

    private val mTempXY = IntArray(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mModel = try {
            com.sandboxr.launcher.dagger.LauncherComponentProvider.get(this).let {
                null
            }
        } catch (e: Exception) {
            null
        }
        mDragController = SecondaryDragController(this)
        mSecondaryDisplayDelegate = SecondaryDisplayDelegate()

        mDeviceProfile = InvariantDeviceProfile.INSTANCE(this)
            .createDeviceProfileForSecondaryDisplay(this)

        setContentView(R.layout.secondary_launcher)
        mDragLayer = findViewById(R.id.drag_layer)
        mAppsView = findViewById(R.id.apps_view)
        mAppsButton = findViewById(R.id.all_apps_button)

        if (mSecondaryDisplayDelegate.enableTaskbarConnectedDisplays()) {
            mAppsButton?.visibility = View.INVISIBLE
        }

        mModel?.addCallbacksAndLoad(this)

        mWallpaperManager = getSystemService(WallpaperManager::class.java)
        try {
            mWallpaperManager?.addOnColorsChangedListener(
                mWallpaperColorsListener,
                android.os.Handler(android.os.Looper.getMainLooper())
            )
            updateStatusBarIconColors(
                mWallpaperManager?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            )
        } catch (ignored: Exception) {}

        mSecondaryDisplayDelegate.onCreate()
    }

    private fun updateStatusBarIconColors(wallpaperColors: WallpaperColors?) {
        if (wallpaperColors != null) {
            val colorHints = wallpaperColors.colorHints
            val window = window ?: return
            val setLightBars = (colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = setLightBars
        }
    }

    override fun onPause() {
        super.onPause()
        mDragController?.cancelDrag()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent?.action) {
            val v = window?.peekDecorView()
            if (v != null && v.windowToken != null) {
                getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(
                    v.windowToken, 0
                )
            }
        }
        showAppDrawer(false)
    }

    override fun getDragController(): DragController? = mDragController

    val secondaryDisplayDelegate: SecondaryDisplayDelegate
        get() = mSecondaryDisplayDelegate

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (finishAutoCancelActionMode()) return

        if (mDragController?.isDragging == true) {
            mDragController?.cancelDrag()
            return
        }

        val topView = AbstractFloatingView.getTopOpenView(this)
        if (topView != null && topView.canHandleBack()) {
            topView.onBackInvoked()
        } else {
            showAppDrawer(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSecondaryDisplayDelegate.onDestroy()
        mModel?.removeCallbacks(this)
        try {
            mWallpaperManager?.removeOnColorsChangedListener(mWallpaperColorsListener)
        } catch (ignored: Exception) {}
    }

    fun isAppDrawerShown(): Boolean = mAppDrawerShown

    override fun getAppsView(): ActivityAllAppsContainerView<SecondaryDisplayLauncher>? = mAppsView

    override fun getDragLayer(): BaseDragLayer<*>? = mDragLayer

    override fun getRootView(): View? = mDragLayer

    override fun bindIncrementalDownloadProgressUpdated(app: AppInfo) {
        // Incremental downloads not supported on secondary display
    }

    fun onAppsButtonClicked(v: View) {
        showAppDrawer(true)
    }

    fun showAppDrawer(show: Boolean) {
        if (show == mAppDrawerShown) return

        val appsView = mAppsView
        val appsButton = mAppsButton
        if (appsView == null || appsButton == null) {
            mAppDrawerShown = show
            return
        }

        if (appsView.width <= 0 || appsView.height <= 0 || !appsView.isAttachedToWindow) {
            mAppDrawerShown = show
            appsView.visibility = if (show) View.VISIBLE else View.INVISIBLE
            appsButton.visibility = if (show || mSecondaryDisplayDelegate.enableTaskbarConnectedDisplays()) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
            return
        }

        val openR = Math.hypot(appsView.width.toDouble(), appsView.height.toDouble()).toFloat()
        val closeR = Themes.getDialogCornerRadius(this)
        val startR = appsButton.width / 2f

        val buttonPos = floatArrayOf(startR, startR)
        mDragLayer?.getDescendantCoordRelativeToSelf(appsButton, buttonPos)
        mDragLayer?.mapCoordInSelfToDescendant(appsView, buttonPos)

        val animator = ViewAnimationUtils.createCircularReveal(
            appsView,
            buttonPos[0].toInt(),
            buttonPos[1].toInt(),
            if (show) closeR else openR,
            if (show) openR else closeR
        )

        if (show) {
            mAppDrawerShown = true
            appsView.visibility = View.VISIBLE
            appsButton.visibility = View.INVISIBLE
            mSecondaryDisplayDelegate.updateAppDivider()
        } else {
            mAppDrawerShown = false
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    appsView.visibility = View.INVISIBLE
                    appsButton.visibility = if (mSecondaryDisplayDelegate.enableTaskbarConnectedDisplays()) {
                        View.INVISIBLE
                    } else {
                        View.VISIBLE
                    }
                    appsView.resetSearch()
                }
            })
        }
        animator.start()
    }

    override fun bindAllApplications(
        apps: Array<AppInfo>,
        flags: Int,
        packageUserKeytoUidMap: Map<PackageUserKey, Int>
    ) {
        val appsStore = mAppsView?.getAppsStore() ?: return
        @Suppress("UNCHECKED_CAST")
        appsStore.setApps(
            apps,
            flags,
            packageUserKeytoUidMap as Map<com.sandboxr.launcher.util.PackageUserKey, Int>
        )
    }

    override fun bindCompleteModel(itemIdMap: WorkspaceData, isBindingSync: Boolean) {
        val pci = itemIdMap.get(com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION)
        if (pci is com.sandboxr.launcher.model.data.PredictedContainerInfo) {
            mSecondaryDisplayDelegate.setPredictedApps(pci)
        }
    }

    override fun bindItemsUpdated(updates: Set<ItemInfo>) {
        for (updatedItem in updates) {
            if (updatedItem.container == com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION &&
                updatedItem is com.sandboxr.launcher.model.data.PredictedContainerInfo
            ) {
                mSecondaryDisplayDelegate.setPredictedApps(updatedItem)
            }
        }
    }

    fun getStringCache(): StringCache? = mStringCache

    override fun bindStringCache(cache: StringCache) {
        mStringCache = cache
    }

    override fun getItemOnClickListener(): View.OnClickListener {
        return View.OnClickListener { onIconClicked(it) }
    }

    override fun getAllAppsItemLongClickListener(): View.OnLongClickListener {
        return View.OnLongClickListener { mDragLayer?.onIconLongClicked(it) ?: false }
    }

    private fun onIconClicked(v: View) {
        if (v.windowToken == null) return
        val tag = v.tag
        if (tag is ItemInfo) {
            val intent = if (tag is ItemInfoWithIcon &&
                (tag.runtimeStatusFlags and ItemInfoWithIcon.FLAG_INSTALL_SESSION_ACTIVE) != 0
            ) {
                tag.getMarketIntent(this)
            } else {
                tag.intent
            }
            if (intent != null) {
                startActivitySafely(v, intent, tag)
            }
        }
    }

    override fun startActivitySafely(v: View?, intent: Intent?, item: ItemInfo?): Boolean {
        if (intent == null) return false
        return try {
            startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun beginDragShared(child: View, source: DragSource, options: DragOptions) {
        val dragObject = child.tag
        if (dragObject !is ItemInfo) {
            throw IllegalStateException("Drag started with a view that has no tag set. View: $child tag: ${child.tag}")
        }
        beginDragShared(child, source, dragObject, DragPreviewProvider(child), options)
    }

    private fun beginDragShared(
        child: View,
        source: DragSource,
        dragObject: ItemInfo,
        previewProvider: DragPreviewProvider,
        options: DragOptions
    ) {
        val iconScale = 1f

        child.clearFocus()
        child.isPressed = false

        val draggableView = child as? DraggableView
        val contentView = previewProvider.getContentView()
        val scale: Float
        val drawable: Drawable?
        if (contentView == null) {
            drawable = previewProvider.createDrawable()
            scale = previewProvider.getScaleAndPosition(drawable, mTempXY)
        } else {
            drawable = null
            scale = previewProvider.getScaleAndPosition(contentView, mTempXY)
        }

        var dragLayerX = mTempXY[0]
        var dragLayerY = mTempXY[1]

        val dragRect = Rect()
        if (draggableView != null) {
            draggableView.getSourceBounds(dragRect)
            dragLayerY += dragRect.top
        }

        val preDragCondition = options.preDragCondition
        if (preDragCondition != null) {
            val xOffSet = preDragCondition.dragOffset.x
            val yOffSet = preDragCondition.dragOffset.y
            if (xOffSet != 0 && yOffSet != 0) {
                dragLayerX += xOffSet
                dragLayerY += yOffSet
            }
        }

        val dragController = mDragController ?: return
        if (contentView != null) {
            dragController.startDrag(
                null,
                contentView,
                draggableView,
                dragLayerX,
                dragLayerY,
                source,
                dragObject,
                dragRect,
                scale * iconScale,
                scale,
                options
            )
        } else {
            dragController.startDrag(
                drawable,
                null,
                draggableView,
                dragLayerX,
                dragLayerY,
                source,
                dragObject,
                dragRect,
                scale * iconScale,
                scale,
                options
            )
        }
    }

    override fun onActivityFlagsChanged(changeBits: Int) {
        super.onActivityFlagsChanged(changeBits)
        if (mDisplayId != Display.DEFAULT_DISPLAY && (changeBits and ACTIVITY_STATE_RESUMED) != 0) {
            mSecondaryDisplayDelegate.updateStashControllerStateFlags(mDisplayId, hasBeenResumed())
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return mSecondaryDisplayDelegate.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }
}
