/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.sandboxr.launcher.workprofile

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * An overlay view shown in the Work tab when the managed work profile is paused (quiet mode).
 *
 * Presents:
 *  1. A briefcase / lock icon indicating the paused state.
 *  2. A title "Work apps are paused".
 *  3. A subtitle explaining how to resume.
 *  4. A "Turn on work" primary action button.
 *
 * The design uses the Liquid Glass card aesthetic with a frosted-glass background.
 */
class WorkProfilePausedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val density = context.resources.displayMetrics.density

    private val iconView: ImageView
    private val titleView: TextView
    private val subtitleView: TextView
    private val enableButton: Button

    private var onEnableClicked: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false

        val horizPad = (32 * density).toInt()
        val vertPad = (48 * density).toInt()
        setPadding(horizPad, vertPad, horizPad, vertPad)

        // Icon
        iconView = ImageView(context).apply {
            id = View.generateViewId()
            // Use the system work badge icon; fallback to a vector drawable placeholder
            try {
                setImageDrawable(context.getDrawable(android.R.drawable.ic_menu_manage))
                setColorFilter(Color.parseColor("#99AACCFF"))
            } catch (e: Exception) {
                // Ignored: icon will be blank if resource unavailable
            }
            layoutParams = LayoutParams((48 * density).toInt(), (48 * density).toInt()).apply {
                bottomMargin = (20 * density).toInt()
            }
        }

        // Title
        titleView = TextView(context).apply {
            id = View.generateViewId()
            text = "Work apps are paused"
            textSize = 20f
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        // Subtitle
        subtitleView = TextView(context).apply {
            id = View.generateViewId()
            text = "Turn on work to see your work apps and receive notifications"
            textSize = 14f
            setTextColor(Color.parseColor("#99FFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (32 * density).toInt()
            }
        }

        // Enable button
        enableButton = Button(context).apply {
            id = View.generateViewId()
            text = "Turn on work"
            textSize = 14f
            setTextColor(Color.parseColor("#FFFFFF"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#4D6B8FBF")) // Frosted glass button
                setStroke((1.5f * density).toInt(), Color.parseColor("#66AACCFF"))
            }
            val buttonPadH = (24 * density).toInt()
            val buttonPadV = (12 * density).toInt()
            setPadding(buttonPadH, buttonPadV, buttonPadH, buttonPadV)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            setOnClickListener { onEnableClicked?.invoke() }
        }

        // Glass card background
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20 * density
            setColor(Color.parseColor("#1A0D0D14"))
            setStroke((1f * density).toInt(), Color.parseColor("#1AFFFFFF"))
        }
        elevation = 4 * density

        addView(iconView)
        addView(titleView)
        addView(subtitleView)
        addView(enableButton)
    }

    fun setOnEnableClickedListener(listener: () -> Unit) {
        onEnableClicked = listener
    }

    /**
     * Updates the view for the given [state].
     * If [WorkProfileManager.WorkProfileState.DISABLED], shows the paused state.
     * Otherwise, hides this view.
     */
    fun applyState(state: WorkProfileManager.WorkProfileState) {
        visibility = when (state) {
            WorkProfileManager.WorkProfileState.DISABLED -> View.VISIBLE
            else -> View.GONE
        }
    }
}
