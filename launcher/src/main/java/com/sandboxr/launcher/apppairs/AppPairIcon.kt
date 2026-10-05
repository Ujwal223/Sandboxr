/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.sandboxr.launcher.apppairs

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.FloatProperty
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.launcher3.LauncherAppState
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.R
import com.sandboxr.launcher.Reorderable
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.model.data.AppPairInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.util.MultiTranslateDelegate
import com.sandboxr.launcher.views.ActivityContext
import java.util.function.Predicate

/**
 * Visual workspace view representing an App Pair (split-screen combination of two apps).
 * Shows the dual-app preview graphic on top, title label below, and opens both apps
 * in split-screen on tap.
 */
open class AppPairIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), DraggableView, Reorderable {

    lateinit var iconDrawableArea: AppPairIconGraphic
        private set

    lateinit var titleTextView: BubbleTextView
        private set

    lateinit var info: AppPairInfo
        private set

    var container: Int = 0
        private set

    private val mTranslateDelegate = MultiTranslateDelegate(this)
    private var mScaleForReorderBounce = 1f

    override fun onFinishInflate() {
        super.onFinishInflate()
        iconDrawableArea = findViewById(R.id.app_pair_icon_graphic)
        titleTextView = findViewById(R.id.app_pair_icon_name)

        setOnClickListener {
            if (::info.isInitialized) {
                AppPairsController(context).launchAppPair(info)
            }
        }
    }

    /**
     * Binds and updates visual elements based on the given [AppPairInfo].
     */
    fun updateInfo(info: AppPairInfo) {
        this.info = info
        tag = info
        if (::iconDrawableArea.isInitialized) {
            iconDrawableArea.init(this, container)
        }
        updateTitleAndA11yTitle()
    }

    /**
     * Updates label and accessibility description.
     */
    fun updateTitleAndA11yTitle() {
        updateTitleAndTextView()
        updateAccessibilityTitle()
    }

    /**
     * Generates and applies the formatted title text.
     */
    fun updateTitleAndTextView() {
        if (!::info.isInitialized || !::titleTextView.isInitialized) return
        val newTitle = info.title ?: runCatching {
            val app1 = info.getFirstApp().title
            val app2 = info.getSecondApp().title
            "$app1 & $app2"
        }.getOrDefault("App Pair")
        titleTextView.text = newTitle
    }

    /**
     * Updates accessibility content description.
     */
    fun updateAccessibilityTitle() {
        if (!::info.isInitialized) return
        val app1 = runCatching { info.getFirstApp().title }.getOrDefault("")
        val app2 = runCatching { info.getSecondApp().title }.getOrDefault("")
        contentDescription = context.getString(R.string.app_pair_name_format, app1, app2)
    }

    // --- DraggableView Implementation ---

    override fun getViewType(): Int = DraggableView.DRAGGABLE_ICON

    override fun getSourceBounds(outBounds: Rect) {
        if (::iconDrawableArea.isInitialized) {
            iconDrawableArea.getIconBounds(outBounds)
        } else {
            outBounds.set(0, 0, width, height)
        }
    }

    fun setIconVisible(visible: Boolean) {
        if (::iconDrawableArea.isInitialized) {
            iconDrawableArea.visibility = if (visible) View.VISIBLE else View.INVISIBLE
        }
    }

    // --- Reorderable Implementation ---

    override fun getTranslateDelegate(): MultiTranslateDelegate = mTranslateDelegate

    override fun setReorderBounceScale(scale: Float) {
        mScaleForReorderBounce = scale
        scaleX = scale
        scaleY = scale
    }

    override fun getReorderBounceScale(): Float = mScaleForReorderBounce

    fun verifyHighRes() {
        if (::info.isInitialized) {
            val iconCache = LauncherAppState.getInstance(context).getIconCache()
            info.fetchHiResIconsIfNeeded(iconCache)
        }
    }

    fun maybeRedrawForWorkspaceUpdate(itemCheck: Predicate<ItemInfo>) {
        if (::info.isInitialized && info.anyMatch(itemCheck)) {
            updateTitleAndA11yTitle()
            if (::iconDrawableArea.isInitialized) {
                iconDrawableArea.redraw()
            }
        }
    }

    override fun onHoverChanged(hovered: Boolean) {
        super.onHoverChanged(hovered)
        if (::iconDrawableArea.isInitialized) {
            ObjectAnimator.ofFloat(
                this,
                HOVER_SCALE_PROPERTY,
                if (hovered) HOVER_SCALE_MAX else HOVER_SCALE_DEFAULT
            ).setDuration(HOVER_SCALE_DURATION).start()
        }
    }

    companion object {
        private const val HOVER_SCALE_DURATION = 150L
        private const val HOVER_SCALE_DEFAULT = 1f
        private const val HOVER_SCALE_MAX = 1.08f

        private val HOVER_SCALE_PROPERTY = object : FloatProperty<AppPairIcon>("hoverScale") {
            override fun setValue(view: AppPairIcon, scale: Float) {
                if (view::iconDrawableArea.isInitialized) {
                    view.iconDrawableArea.setHoverScale(scale)
                }
            }

            override fun get(view: AppPairIcon): Float {
                return if (view::iconDrawableArea.isInitialized) view.iconDrawableArea.getHoverScale() else 1f
            }
        }

        /**
         * Inflates and configures an [AppPairIcon] view.
         */
        @JvmStatic
        fun inflateIcon(
            resId: Int,
            activity: ActivityContext,
            group: ViewGroup?,
            appPairInfo: AppPairInfo,
            container: Int
        ): AppPairIcon {
            val inflater = LayoutInflater.from(activity.asContext())
            val icon = inflater.inflate(resId, group, false) as AppPairIcon
            icon.container = container
            icon.updateInfo(appPairInfo)
            return icon
        }
    }
}
