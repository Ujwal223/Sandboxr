/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.sandboxr.launcher.allapps

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.allapps.search.SearchAdapterProvider
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.views.ActivityContext
import java.util.Objects

/**
 * Base adapter managing views in the All Apps drawer grid.
 */
abstract class BaseAllAppsAdapter(
    protected val activityContext: ActivityContext,
    protected val layoutInflater: LayoutInflater,
    protected val apps: AlphabeticalAppsList,
    val adapterProvider: SearchAdapterProvider<*>?
) : RecyclerView.Adapter<BaseAllAppsAdapter.ViewHolder>() {

    protected var mAppsPerRow: Int = 4
    val appsPerRow: Int get() = mAppsPerRow

    protected var mIconFocusListener: View.OnFocusChangeListener? = null
    val iconFocusListener: View.OnFocusChangeListener? get() = mIconFocusListener

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

    open fun setAppsPerRow(appsPerRow: Int) {
        mAppsPerRow = appsPerRow
    }

    open fun setIconFocusListener(focusListener: View.OnFocusChangeListener?) {
        mIconFocusListener = focusListener
    }

    abstract fun getLayoutManager(): RecyclerView.LayoutManager

    override fun getItemCount(): Int = apps.getAdapterItems().size

    override fun getItemViewType(position: Int): Int = apps.getAdapterItems()[position].viewType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val searchHolder = adapterProvider?.onCreateViewHolder(layoutInflater, parent, viewType)
        if (searchHolder != null) return searchHolder as ViewHolder

        return when (viewType) {
            VIEW_TYPE_ICON, VIEW_TYPE_PREDICTION_ROW -> {
                val icon = createBubbleTextView(parent, viewType)
                ViewHolder(icon)
            }
            VIEW_TYPE_SECTION_HEADER -> {
                val header = TextView(parent.context).apply {
                    textSize = 14f
                    setTextColor(Color.parseColor("#99FFFFFF"))
                    setPadding(32, 24, 32, 8)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                ViewHolder(header)
            }
            VIEW_TYPE_ALL_APPS_DIVIDER -> {
                val divider = View(parent.context).apply {
                    setBackgroundColor(Color.parseColor("#1FFFFFFF"))
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        2
                    )
                }
                ViewHolder(divider)
            }
            VIEW_TYPE_EMPTY_SEARCH -> {
                val empty = TextView(parent.context).apply {
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#77FFFFFF"))
                    setPadding(32, 64, 32, 64)
                    text = "No apps found"
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                ViewHolder(empty)
            }
            VIEW_TYPE_BOTTOM_VIEW_TO_SCROLL_TO -> {
                val bottomSpacer = View(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        96
                    )
                }
                ViewHolder(bottomSpacer)
            }
            else -> {
                val fallback = View(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(0, 0)
                }
                ViewHolder(fallback)
            }
        }
    }

    protected open fun createBubbleTextView(parent: ViewGroup, viewType: Int): BubbleTextView {
        val icon = BubbleTextView(parent.context).apply {
            display = if (viewType == VIEW_TYPE_PREDICTION_ROW) {
                BubbleTextView.DISPLAY_PREDICTION_ROW
            } else {
                BubbleTextView.DISPLAY_ALL_APPS
            }
            onFocusChangeListener = mIconFocusListener
            setOnClickListener(activityContext.getItemOnClickListener())
            setOnLongClickListener(activityContext.getAllAppsItemLongClickListener())
        }
        val cellHeight = activityContext.getDeviceProfile().allAppsCellHeightPx
        val lp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (cellHeight > 0) cellHeight else ViewGroup.LayoutParams.WRAP_CONTENT
        )
        icon.layoutParams = lp
        return icon
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val items = apps.getAdapterItems()
        if (position >= items.size) return
        val adapterItem = items[position]

        when (holder.itemViewType) {
            VIEW_TYPE_ICON, VIEW_TYPE_PREDICTION_ROW -> {
                val icon = holder.itemView as? BubbleTextView ?: return
                adapterItem.itemInfo?.let { appInfo ->
                    icon.applyFromApplicationInfo(appInfo)
                    adapterItem.highlightedTitle?.let { highlighted ->
                        icon.text = highlighted
                    }
                }
            }
            VIEW_TYPE_SECTION_HEADER -> {
                val header = holder.itemView as? TextView ?: return
                header.text = adapterItem.sectionName ?: ""
            }
            else -> {
                // Other standard views
            }
        }

        adapterProvider?.let { provider ->
            if (position == 0 || provider.isViewSupported(holder.itemViewType)) {
                provider.onBindView(holder, position)
            }
        }
    }

    open class AdapterItem(val viewType: Int) {
        var rowIndex: Int = 0
        var rowAppIndex: Int = 0
        var itemInfo: AppInfo? = null
        var highlightedTitle: CharSequence? = null
        var sectionName: String? = null
        var decorationInfo: Any? = null

        open fun isCountedForAccessibility(): Boolean {
            return viewType == VIEW_TYPE_ICON || viewType == VIEW_TYPE_PREDICTION_ROW
        }

        open fun isSameAs(other: AdapterItem?): Boolean {
            if (other == null || other.viewType != viewType) return false
            if (viewType == VIEW_TYPE_ICON || viewType == VIEW_TYPE_PREDICTION_ROW) {
                if (itemInfo == null || other.itemInfo == null) {
                    return itemInfo === other.itemInfo
                }
                return Objects.equals(itemInfo?.user, other.itemInfo?.user) &&
                        Objects.equals(itemInfo?.targetComponent, other.itemInfo?.targetComponent)
            }
            if (viewType == VIEW_TYPE_SECTION_HEADER) {
                return Objects.equals(sectionName, other.sectionName)
            }
            return true
        }

        open fun isContentSame(other: AdapterItem?): Boolean {
            if (other == null) return false
            return isSameAs(other) && itemInfo?.title == other.itemInfo?.title
        }

        companion object {
            @JvmStatic
            @JvmOverloads
            fun asApp(appInfo: AppInfo, highlightedTitle: CharSequence? = null): AdapterItem {
                return AdapterItem(VIEW_TYPE_ICON).apply {
                    itemInfo = appInfo
                    this.highlightedTitle = highlightedTitle
                }
            }

            @JvmStatic
            fun asPrediction(appInfo: AppInfo): AdapterItem {
                return AdapterItem(VIEW_TYPE_PREDICTION_ROW).apply {
                    itemInfo = appInfo
                }
            }

            @JvmStatic
            fun asSectionHeader(sectionName: String): AdapterItem {
                return AdapterItem(VIEW_TYPE_SECTION_HEADER).apply {
                    this.sectionName = sectionName
                }
            }

            @JvmStatic
            fun asDivider(): AdapterItem {
                return AdapterItem(VIEW_TYPE_ALL_APPS_DIVIDER)
            }

            @JvmStatic
            fun asEmptySearch(): AdapterItem {
                return AdapterItem(VIEW_TYPE_EMPTY_SEARCH)
            }

            @JvmStatic
            fun asBottomSpacer(): AdapterItem {
                return AdapterItem(VIEW_TYPE_BOTTOM_VIEW_TO_SCROLL_TO)
            }

            @JvmStatic
            fun asPrivateSpaceHeader(): AdapterItem {
                return AdapterItem(VIEW_TYPE_PRIVATE_SPACE_HEADER)
            }
        }
    }

    companion object {
        const val VIEW_TYPE_ICON = 1 shl 1
        const val VIEW_TYPE_EMPTY_SEARCH = 1 shl 2
        const val VIEW_TYPE_ALL_APPS_DIVIDER = 1 shl 3
        const val VIEW_TYPE_WORK_EDU_CARD = 1 shl 4
        const val VIEW_TYPE_WORK_DISABLED_CARD = 1 shl 5
        const val VIEW_TYPE_PREDICTION_ROW = 1 shl 6
        const val VIEW_TYPE_SECTION_HEADER = 1 shl 7
        const val VIEW_TYPE_BOTTOM_VIEW_TO_SCROLL_TO = 1 shl 8
        /** Header view shown above the private space apps section in the Personal tab. */
        const val VIEW_TYPE_PRIVATE_SPACE_HEADER = 1 shl 9

        const val VIEW_TYPE_MASK_ICON = VIEW_TYPE_ICON or VIEW_TYPE_PREDICTION_ROW
        const val VIEW_TYPE_MASK_DIVIDER = VIEW_TYPE_ALL_APPS_DIVIDER

        @JvmStatic
        fun isIconViewType(viewType: Int): Boolean = (viewType and VIEW_TYPE_MASK_ICON) != 0

        @JvmStatic
        fun isDividerViewType(viewType: Int): Boolean = (viewType and VIEW_TYPE_MASK_DIVIDER) != 0
    }
}
