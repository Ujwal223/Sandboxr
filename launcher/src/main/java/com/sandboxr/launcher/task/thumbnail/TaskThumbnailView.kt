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

package com.sandboxr.launcher.task.thumbnail

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.android.systemui.shared.recents.model.ThumbnailData
import com.sandboxr.launcher.R
import com.sandboxr.launcher.recents.views.FixedSizeImageView
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState.AppLocked
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState.BackgroundOnly
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState.LiveTile
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState.Snapshot
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState.SnapshotSplash
import com.sandboxr.launcher.task.thumbnail.TaskThumbnailUiState.Uninitialized

class TaskThumbnailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val scrimView: View? get() = findViewById(R.id.task_thumbnail_scrim)
    private val liveTileView: LiveTileView? get() = findViewById(R.id.task_thumbnail_live_tile)
    private val thumbnailView: FixedSizeImageView? get() = findViewById(R.id.task_thumbnail)
    private val splashBackground: View? get() = findViewById(R.id.splash_background)
    private val splashIcon: FixedSizeImageView? get() = findViewById(R.id.splash_icon)
    private val appLockIcon: FixedSizeImageView? get() = findViewById(R.id.app_lock_icon)

    private var uiState: TaskThumbnailUiState = Uninitialized
    var outlineBounds: Rect? = null
        set(value) {
            field = value
            invalidateOutline()
        }

    var cornerRadius: Float = 0f
        set(value) {
            field = value
            invalidateOutline()
        }

    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val targetBounds = outlineBounds ?: Rect(0, 0, view.width, view.height)
                if (cornerRadius > 0f) {
                    outline.setRoundRect(targetBounds, cornerRadius)
                } else {
                    outline.setRect(targetBounds)
                }
            }
        }
    }

    fun setThumbnail(thumbnailData: ThumbnailData?) {
        val bitmap = thumbnailData?.thumbnail
        if (bitmap != null && !bitmap.isRecycled) {
            setUiState(Snapshot(bitmap = bitmap, thumbnailRotation = thumbnailData.rotation))
        } else {
            setUiState(BackgroundOnly(Color.TRANSPARENT))
        }
    }

    fun setLiveTile(isLive: Boolean) {
        if (isLive) {
            setUiState(LiveTile)
        } else if (uiState is LiveTile) {
            setUiState(Uninitialized)
        }
    }

    fun setUiState(newState: TaskThumbnailUiState) {
        uiState = newState
        when (newState) {
            is Uninitialized -> {
                thumbnailView?.isInvisible = true
                liveTileView?.isInvisible = true
                splashBackground?.isInvisible = true
                splashIcon?.isInvisible = true
                appLockIcon?.isVisible = false
            }
            is BackgroundOnly -> {
                thumbnailView?.isInvisible = true
                liveTileView?.isInvisible = true
                splashBackground?.isVisible = true
                splashBackground?.setBackgroundColor(newState.backgroundColor)
                splashIcon?.isInvisible = true
                appLockIcon?.isVisible = false
            }
            is AppLocked -> {
                thumbnailView?.isInvisible = true
                liveTileView?.isInvisible = true
                splashBackground?.isVisible = true
                splashBackground?.setBackgroundColor(newState.backgroundColor)
                splashIcon?.isInvisible = true
                appLockIcon?.isVisible = true
            }
            is LiveTile -> {
                thumbnailView?.isInvisible = true
                liveTileView?.isVisible = true
                splashBackground?.isInvisible = true
                splashIcon?.isInvisible = true
                appLockIcon?.isVisible = false
            }
            is Snapshot -> {
                liveTileView?.isInvisible = true
                splashBackground?.isInvisible = true
                splashIcon?.isInvisible = true
                appLockIcon?.isVisible = false
                thumbnailView?.isVisible = true
                thumbnailView?.setImageBitmap(newState.bitmap)
                updateThumbnailMatrix(newState.bitmap.width, newState.bitmap.height)
            }
            is SnapshotSplash -> {
                setUiState(newState.snapshot)
                if (newState.splash != null) {
                    splashIcon?.isVisible = true
                    splashIcon?.setImageDrawable(newState.splash)
                }
            }
        }
    }

    fun getUiState(): TaskThumbnailUiState = uiState

    private fun updateThumbnailMatrix(thumbW: Int, thumbH: Int) {
        val tv = thumbnailView ?: return
        if (width <= 0 || height <= 0 || thumbW <= 0 || thumbH <= 0) return
        val matrix = Matrix()
        val scaleX = width.toFloat() / thumbW.toFloat()
        val scaleY = height.toFloat() / thumbH.toFloat()
        val scale = maxOf(scaleX, scaleY)
        matrix.setScale(scale, scale)
        val dx = (width - thumbW * scale) * 0.5f
        val dy = (height - thumbH * scale) * 0.5f
        matrix.postTranslate(dx, dy)
        tv.imageMatrix = matrix
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val state = uiState
            if (state is Snapshot) {
                updateThumbnailMatrix(state.bitmap.width, state.bitmap.height)
            }
        }
    }
}
