/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Property
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatTextView
import com.sandboxr.launcher.dot.DotInfo
import com.sandboxr.launcher.dot.DotRenderer
import com.sandboxr.launcher.dragndrop.DraggableView
import com.sandboxr.launcher.icons.FastBitmapDrawable
import com.sandboxr.launcher.icons.IconCache
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.util.MultiTranslateDelegate

import com.sandboxr.launcher.anim.AnimatedFloat
import com.sandboxr.launcher.popup.IconViewController
import com.sandboxr.launcher.popup.Poppable
import com.sandboxr.launcher.popup.PoppableType
import com.sandboxr.launcher.util.MultiPropertyFactory

/**
 * Compound drawable TextView displaying app icons with labels, notification dot badges,
 * press feedback bounce animations, and drag/reorder transformations.
 */
open class BubbleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr),
    IconCache.ItemInfoUpdateReceiver,
    DraggableView,
    Reorderable,
    IconViewController,
    Poppable {

    companion object {
        const val TAG = "BubbleTextView"

        const val DISPLAY_WORKSPACE = 0
        const val DISPLAY_ALL_APPS = 1
        const val DISPLAY_FOLDER = 2
        const val DISPLAY_TASKBAR = 5
        const val DISPLAY_SEARCH_RESULT = 6
        const val DISPLAY_PREDICTION_ROW = 8

        val DOT_SCALE_PROPERTY: Property<BubbleTextView, Float> =
            object : Property<BubbleTextView, Float>(Float::class.java, "dotScale") {
                override fun get(view: BubbleTextView): Float = view.mDotParams.scale
                override fun set(view: BubbleTextView, value: Float) {
                    view.mDotParams.scale = value
                    view.invalidate()
                }
            }

        val TEXT_ALPHA_PROPERTY: Property<BubbleTextView, Float> =
            object : Property<BubbleTextView, Float>(Float::class.java, "textAlpha") {
                override fun get(view: BubbleTextView): Float = view.mTextAlpha
                override fun set(view: BubbleTextView, alpha: Float) {
                    view.setTextAlpha(alpha)
                }
            }
    }

    private val mTranslateDelegate = MultiTranslateDelegate(this)

    var display: Int = DISPLAY_WORKSPACE
        set(value) {
            field = value
            refreshIconDisplay()
        }

    var iconSize: Int = 0
        set(value) {
            field = value
            applyCompoundDrawables(mIcon)
        }

    var iconScale: Float = 1f
        set(value) {
            field = value
            invalidate()
        }

    private var mIcon: Drawable? = null
    private var mScaleForReorderBounce: Float = 1f
    private var mCenterVertically: Boolean = false
    private var mLayoutHorizontal: Boolean = false
    private var mStayPressed: Boolean = false
    private var mIsIconVisible: Boolean = true
    private var mForceHideDot: Boolean = false
    private var mTextAlpha: Float = 1f

    private var mDotInfo: DotInfo? = null
    private var mDotRenderer: DotRenderer = DotRenderer(16)
    protected val mDotParams: DotRenderer.DrawParams = DotRenderer.DrawParams()
    private var mDotScaleAnim: Animator? = null

    private var mDisableRelayout: Boolean = false

    init {
        gravity = Gravity.CENTER_HORIZONTAL
        ellipsize = TextUtils.TruncateAt.END
        setSingleLine(true)

        // Resolve default icon size from DeviceProfile or fallback 48dp
        val density = context.resources.displayMetrics.density
        iconSize = (48 * density).toInt()

        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        mDotParams.scale = 0f
    }

    // --- Data Binding ---

    @UiThread
    open fun reset() {
        tag = null
        text = null
        setIcon(null)
    }

    @UiThread
    @JvmOverloads
    open fun applyFromWorkspaceItem(info: WorkspaceItemInfo, animate: Boolean = false) {
        applyIconAndLabel(info)
        tag = info
        applyDotState(info, animate)
    }

    @UiThread
    open fun applyFromApplicationInfo(info: AppInfo) {
        applyIconAndLabel(info)
        tag = info
        applyDotState(info, false)
    }

    @UiThread
    open fun applyFromItemInfoWithIcon(info: ItemInfoWithIcon) {
        applyIconAndLabel(info)
        tag = info
        applyDotState(info, false)
    }

    @UiThread
    open fun applyIconAndLabel(info: ItemInfoWithIcon) {
        val iconDrawable = info.newIcon(context)
        mDotParams.appColor = iconDrawable.iconColor
        mDotParams.dotColor = Color.TRANSPARENT
        setIcon(iconDrawable)
        applyLabel(info)
    }

    @UiThread
    open fun applyIconAndLabel(icon: Drawable?, label: CharSequence?) {
        applyCompoundDrawables(icon)
        text = label
        contentDescription = label
    }

    @UiThread
    open fun applyLabel(info: ItemInfo) {
        text = info.title
        contentDescription = info.contentDescription ?: info.title
    }

    override fun reapplyItemInfo(info: ItemInfoWithIcon?) {
        if (info != null && tag === info) {
            applyIconAndLabel(info)
        }
    }

    // --- Icon & Drawables ---

    open fun setIcon(icon: Drawable?) {
        mIcon = icon
        if (mIsIconVisible) {
            applyCompoundDrawables(icon)
        }
    }

    open fun getIcon(): Drawable? = mIcon

    override fun setIconVisible(visible: Boolean) {
        mIsIconVisible = visible
        val icon = if (visible) mIcon else ColorDrawable(Color.TRANSPARENT)
        applyCompoundDrawables(icon)
    }

    open fun isIconVisible(): Boolean = mIsIconVisible

    open fun setIconDisabled(disabled: Boolean) {
        (mIcon as? FastBitmapDrawable)?.setDisabled(disabled)
    }

    protected open fun applyCompoundDrawables(icon: Drawable?) {
        if (icon == null) return

        mDisableRelayout = mIcon != null
        icon.setBounds(0, 0, iconSize, iconSize)
        if (mLayoutHorizontal) {
            setCompoundDrawablesRelative(icon, null, null, null)
        } else {
            setCompoundDrawables(null, icon, null, null)
        }
        mDisableRelayout = false
    }

    override fun requestLayout() {
        if (!mDisableRelayout) {
            super.requestLayout()
        }
    }

    open fun getIconBounds(outBounds: Rect) {
        getIconBounds(iconSize, outBounds)
    }

    open fun getIconBounds(size: Int, outBounds: Rect) {
        outBounds.set(0, 0, size, size)
        if (mLayoutHorizontal) {
            val top = (height - size) / 2
            if (layoutDirection == LAYOUT_DIRECTION_RTL) {
                outBounds.offsetTo(width - size - paddingRight, top)
            } else {
                outBounds.offsetTo(paddingLeft, top)
            }
        } else {
            outBounds.offset((width - size) / 2, paddingTop)
        }
    }

    // --- Notification Dot Badging ---

    open fun setDotInfo(dotInfo: DotInfo?, animate: Boolean = false) {
        val wasHasDot = hasDot()
        mDotInfo = dotInfo
        val nowHasDot = hasDot()
        if (wasHasDot != nowHasDot) {
            animateDotScale(if (nowHasDot) 1f else 0f, animate)
        } else {
            invalidate()
        }
    }

    open fun getDotInfo(): DotInfo? = mDotInfo

    open fun hasDot(): Boolean = mDotInfo != null && mDotInfo!!.hasDot()

    override fun setForceHideDot(forceHideDot: Boolean) {
        if (mForceHideDot != forceHideDot) {
            mForceHideDot = forceHideDot
            invalidate()
        }
    }

    open fun applyDotState(info: ItemInfoWithIcon, animate: Boolean) {
        // Can be attached from notification manager / dot cache
        val hasDot = mDotInfo != null && mDotInfo!!.hasDot()
        mDotParams.scale = if (hasDot) 1f else 0f
        invalidate()
    }

    open fun animateDotScale(targetScale: Float, animate: Boolean = true) {
        mDotScaleAnim?.cancel()
        if (!animate) {
            mDotParams.scale = targetScale
            invalidate()
            return
        }

        mDotScaleAnim = ObjectAnimator.ofFloat(this, DOT_SCALE_PROPERTY, targetScale).apply {
            duration = 200L
            start()
        }
    }

    open fun setDotRenderer(dotRenderer: DotRenderer) {
        mDotRenderer = dotRenderer
        invalidate()
    }

    open fun getDotRenderer(): DotRenderer = mDotRenderer

    open fun getDotParams(): DotRenderer.DrawParams = mDotParams

    // --- Touch Feedback & Long Press ---

    open fun setStayPressed(stayPressed: Boolean) {
        mStayPressed = stayPressed
        refreshDrawableState()
    }

    open fun isStayPressed(): Boolean = mStayPressed

    open fun setTextAlpha(alpha: Float) {
        mTextAlpha = alpha
        val currentAlpha = (alpha * 255).toInt().coerceIn(0, 255)
        setTextColor(textColors.withAlpha(currentAlpha))
    }

    open fun getTextAlpha(): Float = mTextAlpha

    private val mTextAlphaMultiPropertyFactory = MultiPropertyFactory<AnimatedFloat>(
        AnimatedFloat(java.util.function.Consumer { value: Float -> setTextAlpha(value) }, 1f),
        AnimatedFloat.VALUE,
        2,
        { a, b -> a * b },
        1f
    )

    override fun getFloatingViewTextAlpha(): MultiPropertyFactory<AnimatedFloat>.MultiProperty? =
        mTextAlphaMultiPropertyFactory.get(0)

    override fun getPoppableType(): PoppableType = PoppableType.APP

    override fun getIconHeight(): Int = iconSize

    var showingMinimalPopup: Boolean = false
        private set

    fun configureMinimalPopup(shouldDisableAnimationAndShortcuts: Boolean) {
        showingMinimalPopup = shouldDisableAnimationAndShortcuts
    }

    fun setHideBadge(hide: Boolean) {
    }


    // --- Reorderable & DraggableView ---

    override fun setReorderBounceScale(scale: Float) {
        mScaleForReorderBounce = scale
        scaleX = scale
        scaleY = scale
    }

    override fun getReorderBounceScale(): Float = mScaleForReorderBounce

    override fun getTranslateDelegate(): MultiTranslateDelegate = mTranslateDelegate

    override fun getViewType(): Int = DraggableView.DRAGGABLE_ICON

    override fun getSourceBounds(outBounds: Rect) {
        getIconBounds(outBounds)
    }

    // --- Measurement & Layout ---

    open fun setLayoutHorizontal(layoutHorizontal: Boolean) {
        if (mLayoutHorizontal != layoutHorizontal) {
            mLayoutHorizontal = layoutHorizontal
            applyCompoundDrawables(if (mIsIconVisible) mIcon else ColorDrawable(Color.TRANSPARENT))
        }
    }

    open fun setCenterVertically(centerVertically: Boolean) {
        mCenterVertically = centerVertically
        requestLayout()
    }

    private fun refreshIconDisplay() {
        if (display == DISPLAY_WORKSPACE || display == DISPLAY_FOLDER) {
            setSingleLine(true)
        }
    }

    // --- Drawing ---

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDotIfNecessary(canvas)
    }

    protected open fun drawDotIfNecessary(canvas: Canvas) {
        if (!mForceHideDot && (hasDot() || mDotParams.scale > 0f)) {
            getIconBounds(mDotParams.iconBounds)
            val scrollX = scrollX
            val scrollY = scrollY
            canvas.translate(scrollX.toFloat(), scrollY.toFloat())
            mDotRenderer.draw(canvas, mDotParams, mDotInfo?.getNotificationCount() ?: 0)
            canvas.translate(-scrollX.toFloat(), -scrollY.toFloat())
        }
    }
}
