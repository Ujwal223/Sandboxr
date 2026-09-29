/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.sandboxr.launcher.appprediction

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.R
import com.sandboxr.launcher.allapps.FloatingHeaderRow
import com.sandboxr.launcher.allapps.FloatingHeaderView
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.views.ActivityContext
import java.util.ArrayList

/**
 * A horizontal row displayed inside [FloatingHeaderView] at the top of All Apps drawer
 * showing AI-predicted and contextually relevant application recommendations.
 */
class PredictionRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), FloatingHeaderRow {

    private val activityContext: ActivityContext = ActivityContext.lookupContext(context)
    private var parentHeader: FloatingHeaderView? = null
    private val predictedApps = ArrayList<WorkspaceItemInfo>()
    private var numPredictedAppsPerRow: Int = 5
    private var predictionsEnabled = false

    init {
        orientation = HORIZONTAL
        val dp = activityContext.getDeviceProfile()
        numPredictedAppsPerRow = if (dp.numShownAllAppsColumns > 0) dp.numShownAllAppsColumns else 5
        val density = resources.displayMetrics.density
        val padV = (4 * density).toInt()
        val padH = (16 * density).toInt()
        setPadding(padH, padV, padH, padV)
        visibility = GONE
    }

    override fun setup(parent: FloatingHeaderView, rows: Array<FloatingHeaderRow>, tabsHidden: Boolean) {
        this.parentHeader = parent
    }

    override fun asView(): View = this

    override fun isVisible(): Boolean = visibility == VISIBLE

    override fun getExpectedHeight(): Int {
        if (!predictionsEnabled || visibility == GONE) return 0
        val dp = activityContext.getDeviceProfile()
        val cellHeight = dp.allAppsCellHeightPx
        return if (cellHeight > 0) cellHeight + paddingTop + paddingBottom else (88 * resources.displayMetrics.density).toInt()
    }

    override fun onScrollChanged(scrollY: Int, isHeaderVisible: Boolean) {
        alpha = if (isHeaderVisible) 1.0f else 0.0f
        translationY = scrollY.toFloat()
    }

    fun getPredictedApps(): List<WorkspaceItemInfo> = ArrayList(predictedApps)

    fun setPredictedApps(items: List<ItemInfo>) {
        predictedApps.clear()
        for (item in items) {
            when (item) {
                is WorkspaceItemInfo -> predictedApps.add(item)
                is AppInfo -> predictedApps.add(item.makeWorkspaceItem(context))
            }
        }
        applyPredictionApps()
    }

    private fun applyPredictionApps() {
        val dp = activityContext.getDeviceProfile()
        numPredictedAppsPerRow = if (dp.numShownAllAppsColumns > 0) dp.numShownAllAppsColumns else 5

        // Adjust child count to match columns
        while (childCount > numPredictedAppsPerRow) {
            removeViewAt(childCount - 1)
        }

        val inflater = LayoutInflater.from(context)
        while (childCount < numPredictedAppsPerRow) {
            val iconView = try {
                inflater.inflate(R.layout.all_apps_prediction_row_icon, this, false) as BubbleTextView
            } catch (_: Exception) {
                BubbleTextView(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
                }
            }

            iconView.display = BubbleTextView.DISPLAY_PREDICTION_ROW
            iconView.setOnClickListener(activityContext.getItemOnClickListener())
            iconView.setOnLongClickListener(activityContext.getAllAppsItemLongClickListener())
            val lp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f)
            iconView.layoutParams = lp
            addView(iconView)
        }

        val count = predictedApps.size
        for (i in 0 until childCount) {
            val child = getChildAt(i) as? BubbleTextView ?: continue
            child.reset()
            if (i < count) {
                val item = predictedApps[i]
                item.rank = i
                item.cellX = i
                item.cellY = 0
                child.visibility = VISIBLE
                child.applyFromWorkspaceItem(item)
            } else {
                child.visibility = if (count == 0) GONE else INVISIBLE
            }
        }

        val shouldEnable = count > 0
        if (shouldEnable != predictionsEnabled) {
            predictionsEnabled = shouldEnable
            visibility = if (shouldEnable) VISIBLE else GONE
        }

        parentHeader?.onHeightUpdated()
        requestLayout()
    }

    companion object {
        private const val TAG = "PredictionRowView"
    }
}
