/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.sandboxr.launcher.R

/**
 * AppWidget host view with QSB specific orientation and error fallback logic.
 */
open class QsbWidgetHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mPreviousOrientation: Int = context.resources.configuration.orientation

    init {
        isFocusable = true
    }

    open fun updateAppWidget(remoteViews: RemoteViews?) {
        mPreviousOrientation = resources.configuration.orientation
    }

    open fun isReinflateRequired(orientation: Int): Boolean {
        return mPreviousOrientation != orientation
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    open fun shouldAllowDirectClick(): Boolean = true

    companion object {
        @JvmStatic
        fun getDefaultView(parent: ViewGroup): View {
            return try {
                LayoutInflater.from(parent.context).inflate(R.layout.qsb_default_view, parent, false)
            } catch (_: Exception) {
                QsbLayout(parent.context)
            }
        }
    }
}
