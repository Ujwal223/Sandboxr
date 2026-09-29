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

package com.sandboxr.launcher.qsb

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.sandboxr.launcher.Insettable
import com.sandboxr.launcher.R
import com.sandboxr.launcher.Reorderable
import com.sandboxr.launcher.util.HorizontalInsettableView
import com.sandboxr.launcher.util.MultiTranslateDelegate

/**
 * A themed, insettable Quick Search Bar capsule for the home screen and hotseat.
 *
 * Features:
 * - Dynamic theme adaptation (Material 3 pill design with dark mode & light mode support).
 * - Smooth [HorizontalInsettableView] support for foldables and bubble bar adjustments.
 * - [Reorderable] integration via [MultiTranslateDelegate] for spring and displacement animations.
 * - Pinned search action dispatching (global search, voice search, lens).
 */
open class QsbLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), HorizontalInsettableView, Insettable, Reorderable {

    private var mHorizontalInsets: Float = 0f
    private val mInsets = Rect()
    private val mTranslateDelegate = MultiTranslateDelegate(this)
    private var mReorderBounceScale: Float = 1f

    private val mPillLayout: LinearLayout
    private val mSearchIcon: ImageView
    private val mSearchTextView: TextView
    private val mMicButton: ImageView
    private val mLensButton: ImageView
    private val mSetupButton: ImageView

    private var mCornerRadius: Float = 0f
    private var mIsDarkTheme: Boolean = false

    init {
        val density = context.resources.displayMetrics.density
        val heightPx = (48 * density).toInt()

        // Check dark mode
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        mIsDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        mPillLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, heightPx).apply {
                gravity = Gravity.CENTER
            }
        }

        // Search Icon (Magnifying glass / G)
        mSearchIcon = ImageView(context).apply {
            id = R.id.btn_qsb_search
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt()).apply {
                marginStart = (12 * density).toInt()
                marginEnd = (8 * density).toInt()
            }
            contentDescription = context.getString(R.string.abandoned_search)
            setColorFilter(if (mIsDarkTheme) 0xFFE0E0E0.toInt() else 0xFF5F6368.toInt())
            setOnClickListener { startSearch("") }
        }

        // Search Hint Text
        mSearchTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            text = context.getString(R.string.search_hint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(if (mIsDarkTheme) 0x99FFFFFF.toInt() else 0x99000000.toInt())
            isSingleLine = true
            isClickable = true
            isFocusable = false
            setOnClickListener { startSearch("") }
        }

        // Voice Search (Mic)
        mMicButton = ImageView(context).apply {
            id = R.id.btn_qsb_mic
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt()).apply {
                marginEnd = (4 * density).toInt()
            }
            contentDescription = context.getString(R.string.qsb_voice_search)
            setColorFilter(if (mIsDarkTheme) 0xFF8AB4F8.toInt() else 0xFF1A73E8.toInt())
            setOnClickListener { startVoiceSearch() }
        }

        // Lens Button
        mLensButton = ImageView(context).apply {
            id = R.id.btn_qsb_lens
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt()).apply {
                marginEnd = (8 * density).toInt()
            }
            contentDescription = context.getString(R.string.qsb_lens_search)
            setColorFilter(if (mIsDarkTheme) 0xFFF28B82.toInt() else 0xFFEA4335.toInt())
            setOnClickListener { startLensSearch() }
        }

        // Setup Button (for widget configuration if needed)
        mSetupButton = ImageView(context).apply {
            id = R.id.btn_qsb_setup
            layoutParams = LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt()).apply {
                marginEnd = (8 * density).toInt()
            }
            visibility = View.GONE
            contentDescription = context.getString(R.string.gadget_setup_text)
            setColorFilter(if (mIsDarkTheme) 0xFFBDC1C6.toInt() else 0xFF5F6368.toInt())
        }

        mPillLayout.addView(mSearchIcon)
        mPillLayout.addView(mSearchTextView)
        mPillLayout.addView(mMicButton)
        mPillLayout.addView(mLensButton)
        mPillLayout.addView(mSetupButton)

        addView(mPillLayout)

        updateBackgroundTheming()
        isClickable = true
        setOnClickListener { startSearch("") }
    }

    /**
     * Applies dynamic theme coloring (pill capsule with subtle glassmorphic alpha).
     */
    fun updateBackgroundTheming() {
        val density = context.resources.displayMetrics.density
        mCornerRadius = 24f * density

        val bgColor = if (mIsDarkTheme) {
            Color.argb(220, 36, 38, 42) // Material 3 dark surface container
        } else {
            Color.argb(235, 248, 249, 250) // Material 3 light surface container
        }

        val strokeColor = if (mIsDarkTheme) {
            Color.argb(40, 255, 255, 255)
        } else {
            Color.argb(30, 0, 0, 0)
        }

        val shapeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = mCornerRadius
            setColor(bgColor)
            setStroke((1 * density).toInt(), strokeColor)
        }

        mPillLayout.background = shapeDrawable
        elevation = 2f * density
    }

    /**
     * Sets custom search hint text.
     */
    fun setSearchHint(hint: CharSequence) {
        mSearchTextView.text = hint
    }

    /**
     * Shows or hides the setup button.
     */
    fun setSetupButtonVisible(visible: Boolean, clickListener: OnClickListener? = null) {
        mSetupButton.visibility = if (visible) View.VISIBLE else View.GONE
        mSetupButton.setOnClickListener(clickListener)
    }

    // HorizontalInsettableView implementation
    override fun setHorizontalInsets(insetPercentage: Float) {
        if (mHorizontalInsets != insetPercentage) {
            mHorizontalInsets = insetPercentage.coerceIn(0f, 1f)
            val parentWidth = measuredWidth
            val insetPx = (parentWidth * mHorizontalInsets).toInt()
            val lp = mPillLayout.layoutParams as? MarginLayoutParams
            if (lp != null) {
                lp.leftMargin = insetPx
                lp.rightMargin = insetPx
                mPillLayout.layoutParams = lp
            }
            invalidate()
        }
    }

    override fun getHorizontalInsets(): Float = mHorizontalInsets

    // Insettable implementation
    override fun setInsets(insets: Rect) {
        mInsets.set(insets)
    }

    // Reorderable implementation
    override fun getTranslateDelegate(): MultiTranslateDelegate = mTranslateDelegate

    override fun setReorderBounceScale(scale: Float) {
        mReorderBounceScale = scale
        scaleX = scale
        scaleY = scale
    }

    override fun getReorderBounceScale(): Float = mReorderBounceScale

    /**
     * Launches the search intent or delegates to the active launcher session.
     */
    open fun startSearch(query: String = "") {
        try {
            val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
            val globalSearchActivity = searchManager?.globalSearchActivity
            if (globalSearchActivity != null) {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    setPackage(globalSearchActivity.packageName)
                    putExtra(SearchManager.QUERY, query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
            // Fall through to generic web search
        }

        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Silently swallow in environments without web browser
        }
    }

    /**
     * Launches the voice search action.
     */
    open fun startVoiceSearch() {
        try {
            val intent = Intent("android.speech.action.WEB_SEARCH").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            startSearch("")
        }
    }

    /**
     * Launches visual search / lens action.
     */
    open fun startLensSearch() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.google.ar.lens")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            startSearch("")
        }
    }
}
