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

package com.sandboxr.launcher.recents.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.sandboxr.launcher.R

class IconAppChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var iconView: ImageView? = null
    private var iconTitle: TextView? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        iconView = findViewById(R.id.icon_view)
        iconTitle = findViewById(R.id.icon_title)
    }

    fun setIcon(drawable: Drawable?) {
        iconView?.setImageDrawable(drawable)
    }

    fun setTitle(title: CharSequence?) {
        iconTitle?.text = title
    }
}
