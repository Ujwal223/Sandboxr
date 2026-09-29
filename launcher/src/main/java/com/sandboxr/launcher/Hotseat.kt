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

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sandboxr.launcher.LauncherAnimUtils.VIEW_TRANSLATE_X
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.qsb.QsbContainerView
import com.sandboxr.launcher.util.HorizontalInsettableView
import com.sandboxr.launcher.util.MultiPropertyFactory
import com.sandboxr.launcher.util.MultiTranslateDelegate
import com.sandboxr.launcher.util.MultiValueAlpha
import com.sandboxr.launcher.views.ActivityContext
import java.io.PrintWriter

/**
 * Persistent dock container at the bottom (or side in vertical landscape layout) of the home screen.
 *
 * Extends [CellLayout] with [CONTAINER_TYPE_HOTSEAT], managing pinned shortcut items,
 * the Quick Search Bar ([qsb]), multi-channel alpha blending, touch delegation to [Workspace],
 * and bubble bar / taskbar alignment transitions.
 */
open class Hotseat @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CellLayout(context, attrs, defStyleAttr, CONTAINER_TYPE_HOTSEAT), Insettable {

    var hasVerticalHotseat: Boolean = false
        private set

    var workspace: Workspace<*>? = null
        private set

    private var mSendTouchToWorkspace: Boolean = false
    private val mIconsAlphaChannels: MultiValueAlpha
    private var mQsbAlphaChannels: MultiValueAlpha? = null

    private var mQsbTranslationX: MultiPropertyFactory<View>.MultiProperty? = null
    private val mIconsTranslationXFactory: MultiPropertyFactory<View>

    private var mQsb: View? = null
    private val mInsets = Rect()

    private var mPredictionListener: HotseatPredictionListener? = null

    init {
        // Inflate or create QSB search view
        var qsbView: View? = null
        try {
            qsbView = LayoutInflater.from(context).inflate(R.layout.search_container_hotseat, this, false)
        } catch (_: Exception) {}

        if (qsbView == null) {
            qsbView = QsbContainerView(context).apply {
                id = R.id.search_container_hotseat
            }
        }
        mQsb = qsbView
        addView(qsbView)

        mIconsAlphaChannels = MultiValueAlpha(shortcutsAndWidgets, ALPHA_CHANNEL_CHANNELS_COUNT)
        mIconsTranslationXFactory = MultiPropertyFactory(
            shortcutsAndWidgets,
            VIEW_TRANSLATE_X,
            ICONS_TRANSLATION_X_CHANNELS_COUNT,
            { a, b -> a + b }
        )

        attachQsbProperties(qsbView)

        val dp = getDeviceProfile()
        resetCellSize(dp)
        setGridSize(dp.numShownHotseatIcons, 1)
    }

    private fun attachQsbProperties(qsbView: View?) {
        if (qsbView != null) {
            if (qsbView is Reorderable) {
                mQsbTranslationX = qsbView.getTranslateDelegate()
                    .getTranslationX(MultiTranslateDelegate.INDEX_NAV_BAR_ANIM)
            }
            mQsbAlphaChannels = MultiValueAlpha(qsbView, ALPHA_CHANNEL_CHANNELS_COUNT)
        }
    }

    /** Provides translation X for hotseat icons for the channel. */
    fun getIconsTranslationX(channelId: Int): MultiPropertyFactory<View>.MultiProperty {
        return mIconsTranslationXFactory[channelId]
    }

    /** Provides translation X for hotseat Qsb. */
    fun getQsbTranslationX(): MultiPropertyFactory<View>.MultiProperty? = mQsbTranslationX

    /** Returns orientation specific cell X given invariant order in the hotseat. */
    fun getCellXFromOrder(rank: Int): Int = if (hasVerticalHotseat) 0 else rank

    /** Returns orientation specific cell Y given invariant order in the hotseat. */
    fun getCellYFromOrder(rank: Int): Int = if (hasVerticalHotseat) (countY - (rank + 1)) else 0

    fun isHasVerticalHotseat(): Boolean = hasVerticalHotseat

    /**
     * Resets the hotseat grid and dimensions for the given layout orientation.
     */
    open fun resetLayout(hasVerticalHotseat: Boolean) {
        val dp = getDeviceProfile()
        this.hasVerticalHotseat = hasVerticalHotseat

        removeAllViewsInLayout()

        resetCellSize(dp)
        if (hasVerticalHotseat) {
            setGridSize(1, dp.numShownHotseatIcons)
        } else {
            setGridSize(dp.numShownHotseatIcons, 1)
        }
    }

    /**
     * Resets cell dimensions according to [DeviceProfile].
     */
    open fun resetCellSize(dp: DeviceProfile) {
        val cellW = if (dp.hotseatCellWidthPx > 0) dp.hotseatCellWidthPx else dp.iconSizePx
        val cellH = if (dp.hotseatBarSizePx > 0) dp.hotseatBarSizePx else dp.iconSizePx
        setCellDimensions(cellW, cellH)
    }

    /**
     * Adjusts hotseat icons and QSB for the bubble bar visibility.
     */
    open fun adjustForBubbleBar(isBubbleBarVisible: Boolean) {
        val dp = getDeviceProfile()
        val shouldAdjust = isBubbleBarVisible && dp.shouldAdjustHotseatOrQsbForBubbleBar(context)
        val shouldAdjustHotseat = shouldAdjust && dp.shouldAlignBubbleBarWithHotseat()
        val icons = shortcutsAndWidgets

        val animatorSet = AnimatorSet()
        for (i in 0 until icons.childCount) {
            val child = icons.getChildAt(i)
            val lp = child.layoutParams as? CellLayoutLayoutParams ?: continue
            val tx = if (shouldAdjustHotseat) dp.getHotseatAdjustedTranslation(context, lp.cellX) else 0f
            if (child is Reorderable) {
                val mtd = child.getTranslateDelegate()
                animatorSet.play(
                    mtd.getTranslationX(MultiTranslateDelegate.INDEX_BUBBLE_ADJUSTMENT_ANIM).animateToValue(tx)
                )
            } else {
                animatorSet.play(ObjectAnimator.ofFloat(child, VIEW_TRANSLATE_X, tx))
            }
        }

        val qsb = mQsb
        val shouldAdjustQsb = shouldAdjustHotseat || (shouldAdjust && dp.shouldAlignBubbleBarWithQSB())
        if (qsb is HorizontalInsettableView) {
            val currentInsets = qsb.getHorizontalInsets()
            val targetInsets = if (shouldAdjustQsb && dp.hotseatQsbWidth > 0) {
                dp.iconSizePx.toFloat() / dp.hotseatQsbWidth
            } else {
                0f
            }
            val qsbAnimator = ValueAnimator.ofFloat(currentInsets, targetInsets).apply {
                addUpdateListener { anim ->
                    qsb.setHorizontalInsets(anim.animatedValue as Float)
                }
            }
            animatorSet.play(qsbAnimator)
        }

        animatorSet.duration = BUBBLE_BAR_ADJUSTMENT_ANIMATION_DURATION_MS
        animatorSet.start()
    }

    override fun setInsets(insets: Rect) {
        mInsets.set(insets)
        val lp = layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val dp = getDeviceProfile()

        if (dp.isVerticalBarLayout()) {
            mQsb?.visibility = View.GONE
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            if (dp.isSeascape()) {
                lp.gravity = Gravity.LEFT
                lp.width = dp.hotseatBarSizePx + insets.left
            } else {
                lp.gravity = Gravity.RIGHT
                lp.width = dp.hotseatBarSizePx + insets.right
            }
        } else {
            mQsb?.visibility = View.VISIBLE
            lp.gravity = Gravity.BOTTOM
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = dp.hotseatBarSizePx
        }

        val padding = dp.getHotseatLayoutPadding(context)
        setPadding(padding.left, padding.top, padding.right, padding.bottom)
        layoutParams = lp
        InsettableFrameLayout.dispatchInsets(this, insets)
    }

    open fun setWorkspace(w: Workspace<*>) {
        workspace = w
        setCellLayoutContainer(w)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val yThreshold = measuredHeight - paddingBottom
        val ws = workspace
        if (ws != null && ev.y <= yThreshold) {
            mSendTouchToWorkspace = ws.onInterceptTouchEvent(ev)
            return mSendTouchToWorkspace
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mSendTouchToWorkspace) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mSendTouchToWorkspace = false
                }
            }
            return workspace?.onTouchEvent(event) ?: false
        }
        return false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val dp = getDeviceProfile()
        val qsbWidth = if (dp.hotseatQsbWidth > 0) dp.hotseatQsbWidth else (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0)
        val qsbHeight = if (dp.hotseatQsbHeight > 0) dp.hotseatQsbHeight else dp.iconSizePx

        mQsb?.measure(
            MeasureSpec.makeMeasureSpec(qsbWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(qsbHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        val qsb = mQsb ?: return
        if (qsb.visibility == View.GONE) return

        val dp = getDeviceProfile()
        val qsbMeasuredWidth = qsb.measuredWidth
        val qsbMeasuredHeight = qsb.measuredHeight

        val left: Int
        if (dp.isQsbInline) {
            val qsbSpace = dp.hotseatBorderSpace
            val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            left = if (isRtl) {
                r - paddingRight + qsbSpace
            } else {
                l + paddingLeft - qsbMeasuredWidth - qsbSpace
            }
        } else {
            left = (r - l - qsbMeasuredWidth) / 2
        }
        val right = left + qsbMeasuredWidth

        val bottom = (b - t) - dp.getQsbOffsetY()
        val top = bottom - qsbMeasuredHeight
        qsb.layout(left, top, right, bottom)
    }

    /** Sets the alpha value of the specified alpha channel of just our ShortcutAndWidgetContainer. */
    fun setIconsAlpha(alpha: Float, channelId: Int) {
        getIconsAlpha(channelId).setValue(alpha)
    }

    /** Sets the alpha value of just our QSB. */
    fun setQsbAlpha(alpha: Float, channelId: Int) {
        getQsbAlpha(channelId).setValue(alpha)
    }

    /** Returns the alpha channel for ShortcutAndWidgetContainer. */
    fun getIconsAlpha(channelId: Int): MultiPropertyFactory<View>.MultiProperty {
        return mIconsAlphaChannels[channelId]
    }

    /** Returns the alpha channel for Qsb. */
    fun getQsbAlpha(channelId: Int): MultiPropertyFactory<View>.MultiProperty {
        val channels = mQsbAlphaChannels ?: MultiValueAlpha(mQsb ?: this, ALPHA_CHANNEL_CHANNELS_COUNT).also {
            mQsbAlphaChannels = it
        }
        return channels[channelId]
    }

    /** Returns the QSB inside hotseat. */
    fun getQsb(): View? = mQsb

    /** Sets a custom QSB inside hotseat. */
    fun setQsb(qsb: View?) {
        mQsb?.let { removeView(it) }
        mQsb = qsb
        if (qsb != null) {
            addView(qsb)
            attachQsbProperties(qsb)
        }
        requestLayout()
    }

    /**
     * Prediction listener integration point for hybrid hotseat.
     */
    fun interface HotseatPredictionListener {
        fun onPredictionsUpdated(predictedItems: List<ItemInfo>)
    }

    fun setPredictionListener(listener: HotseatPredictionListener?) {
        mPredictionListener = listener
    }

    fun onPredictionsUpdated(predictedItems: List<ItemInfo>) {
        mPredictionListener?.onPredictionsUpdated(predictedItems)
    }

    open fun dump(prefix: String, writer: PrintWriter) {
        writer.println("${prefix}Hotseat:")
        writer.println("${prefix}  hasVerticalHotseat=$hasVerticalHotseat")
        writer.println("${prefix}  qsbVisible=${mQsb?.visibility != View.GONE}")
        mIconsAlphaChannels.dump(
            "$prefix\t",
            writer,
            "mIconsAlphaChannels",
            "ALPHA_CHANNEL_TASKBAR_ALIGNMENT",
            "ALPHA_CHANNEL_PREVIEW_RENDERER",
            "ALPHA_CHANNEL_TASKBAR_STASH"
        )
        mQsbAlphaChannels?.dump(
            "$prefix\t",
            writer,
            "mQsbAlphaChannels",
            "ALPHA_CHANNEL_TASKBAR_ALIGNMENT",
            "ALPHA_CHANNEL_PREVIEW_RENDERER",
            "ALPHA_CHANNEL_TASKBAR_STASH"
        )
    }

    private fun getDeviceProfile(): DeviceProfile {
        return try {
            ActivityContext.lookupContext<ActivityContext>(context).getDeviceProfile()
        } catch (_: Exception) {
            DeviceProfile.fromContext(context)
        }
    }

    companion object {
        const val ALPHA_CHANNEL_TASKBAR_ALIGNMENT = 0
        const val ALPHA_CHANNEL_PREVIEW_RENDERER = 1
        const val ALPHA_CHANNEL_TASKBAR_STASH = 2
        const val ALPHA_CHANNEL_CHANNELS_COUNT = 3

        const val ICONS_TRANSLATION_X_NAV_BAR_ALIGNMENT = 0
        const val ICONS_TRANSLATION_X_CHANNELS_COUNT = 1

        const val QSB_CENTER_FACTOR = 0.325f
        private const val BUBBLE_BAR_ADJUSTMENT_ANIMATION_DURATION_MS = 250L
    }
}
