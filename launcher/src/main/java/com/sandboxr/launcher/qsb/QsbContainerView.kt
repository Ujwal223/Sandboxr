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

import android.app.SearchManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.WorkerThread
import com.sandboxr.launcher.Reorderable
import com.sandboxr.launcher.util.HorizontalInsettableView
import com.sandboxr.launcher.util.MultiTranslateDelegate

/**
 * A container FrameLayout that hosts the Quick Search Bar (QSB) or search widget.
 *
 * Implements [HorizontalInsettableView] and [Reorderable] for smooth animations and dock
 * alignments.
 */
open class QsbContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), HorizontalInsettableView, Reorderable {

    private val mTranslateDelegate = MultiTranslateDelegate(this)
    private var mReorderBounceScale: Float = 1f
    private var mHorizontalInsets: Float = 0f

    private var mQsbLayout: QsbLayout? = null

    init {
        // Automatically provide default QsbLayout if not populated by layout inflation
        post {
            if (childCount == 0) {
                val layout = QsbLayout(context)
                mQsbLayout = layout
                addView(layout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            } else {
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    if (child is QsbLayout) {
                        mQsbLayout = child
                        break
                    }
                }
            }
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    open fun setPaddingUnchecked(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
    }

    // HorizontalInsettableView implementation
    override fun setHorizontalInsets(insetPercentage: Float) {
        mHorizontalInsets = insetPercentage
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is HorizontalInsettableView) {
                child.setHorizontalInsets(insetPercentage)
            }
        }
    }

    override fun getHorizontalInsets(): Float = mHorizontalInsets

    // Reorderable implementation
    override fun getTranslateDelegate(): MultiTranslateDelegate = mTranslateDelegate

    override fun setReorderBounceScale(scale: Float) {
        mReorderBounceScale = scale
        scaleX = scale
        scaleY = scale
    }

    override fun getReorderBounceScale(): Float = mReorderBounceScale

    open fun getQsbLayout(): QsbLayout? = mQsbLayout

    companion object {
        const val SEARCH_ENGINE_SETTINGS_KEY = "selected_search_engine"

        /**
         * Returns the package name for user configured search provider or from SearchManager.
         */
        @WorkerThread
        @JvmStatic
        fun getSearchWidgetPackageName(context: Context): String? {
            try {
                val providerPkg = Settings.Secure.getString(
                    context.contentResolver,
                    SEARCH_ENGINE_SETTINGS_KEY
                )
                if (!providerPkg.isNullOrEmpty()) {
                    return providerPkg
                }
            } catch (_: Exception) {}

            return try {
                val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as? SearchManager
                searchManager?.globalSearchActivity?.packageName
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Returns AppWidgetProviderInfo using package name from [getSearchWidgetPackageName].
         */
        @WorkerThread
        @JvmStatic
        fun getSearchWidgetProviderInfo(context: Context): AppWidgetProviderInfo? {
            val providerPkg = getSearchWidgetPackageName(context) ?: return null
            return try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return null
                var defaultWidgetForSearchPackage: AppWidgetProviderInfo? = null
                for (info in appWidgetManager.getInstalledProvidersForPackage(providerPkg, null)) {
                    if (info.provider.packageName == providerPkg && info.configure == null) {
                        if ((info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX) != 0) {
                            return info
                        } else if (defaultWidgetForSearchPackage == null) {
                            defaultWidgetForSearchPackage = info
                        }
                    }
                }
                defaultWidgetForSearchPackage
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Returns ComponentName for search widget.
         */
        @WorkerThread
        @JvmStatic
        fun getSearchComponentName(context: Context): ComponentName? {
            val providerInfo = getSearchWidgetProviderInfo(context)
            if (providerInfo != null) {
                return providerInfo.provider
            }
            val pkgName = getSearchWidgetPackageName(context)
            return if (pkgName != null) ComponentName(pkgName, pkgName) else null
        }
    }
}
