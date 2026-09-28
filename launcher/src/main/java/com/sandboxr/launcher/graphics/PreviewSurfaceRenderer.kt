/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.graphics

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import java.util.concurrent.CompletableFuture

/**
 * Surface / view renderer responsible for generating live or cached previews
 * of the launcher workspace, wallpaper, and app icon grid.
 */
open class PreviewSurfaceRenderer(
    val context: Context,
    val bundle: Bundle = Bundle(),
) {
    private val rootView = FrameLayout(context)
    private var isDestroyed: Boolean = false
    private var isBottomRowHidden: Boolean = false

    open fun loadAsync(): CompletableFuture<View> {
        val future = CompletableFuture<View>()
        if (isDestroyed) {
            future.complete(null)
            return future
        }
        // Build preview layout asynchronously
        rootView.post {
            future.complete(rootView)
        }
        return future
    }

    open fun hideBottomRow(hide: Boolean) {
        this.isBottomRowHidden = hide
    }

    open fun previewColor(bundle: Bundle) {
        // Handle preview color overrides
    }

    open fun destroy() {
        isDestroyed = true
        rootView.removeAllViews()
    }

    companion object {
        const val KEY_HOST_TOKEN = "host_token"
        const val KEY_VIEW_WIDTH = "width"
        const val KEY_VIEW_HEIGHT = "height"
        const val KEY_DISPLAY_ID = "display_id"
        const val KEY_COLORS = "wallpaper_colors"
        const val KEY_DARK_MODE = "use_dark_mode"
        const val KEY_LAYOUT_XML = "layout_xml"
        const val KEY_WORKSPACE_PAGE_ID = "workspace_page_id"
    }
}
