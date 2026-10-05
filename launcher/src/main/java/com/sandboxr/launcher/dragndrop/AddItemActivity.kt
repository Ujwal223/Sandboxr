/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.PinItemRequest
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sandboxr.launcher.BaseActivity
import com.android.launcher3.LauncherAppState
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.model.IModelWriter
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.model.tasks.AddWorkspaceItemsTask
import com.sandboxr.launcher.widget.DatabaseWidgetPreviewLoader
import java.util.function.Supplier

/**
 * Activity presenting confirmation dialog when third-party apps request pinning
 * shortcuts or widgets to the home screen.
 * Implements Liquid Glass design and allows immediate automatic addition or drag placement.
 */
open class AddItemActivity : Activity() {

    companion object {
        const val EXTRA_PIN_ITEM_REQUEST = LauncherApps.EXTRA_PIN_ITEM_REQUEST
    }

    private var mRequest: PinItemRequest? = null
    private var mShortcutInfo: PinShortcutRequestActivityInfo? = null
    private var mWidgetHandler: PinWidgetFlowHandler? = null

    private lateinit var mPreviewImage: ImageView
    private lateinit var mTitleView: TextView
    private lateinit var mSubtitleView: TextView
    private lateinit var mAddButton: Button
    private lateinit var mCancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = intent?.getParcelableExtra<PinItemRequest>(EXTRA_PIN_ITEM_REQUEST)
        if (request == null || !request.isValid) {
            finish()
            return
        }
        mRequest = request

        setupContentView()
        bindRequestData()
    }

    private fun setupContentView() {
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0x80000000.toInt()) // Dim backdrop
        }

        // Dialog container with Liquid Glass aesthetic
        val dialogCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padH = dpToPx(24)
            val padV = dpToPx(20)
            setPadding(padH, padV, padH, padV)

            val bg = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF01A1A26.toInt(), 0xF012121A.toInt())
            ).apply {
                cornerRadius = dpToPx(28).toFloat()
                setStroke(dpToPx(1), 0x4064D2FF) // Specular cyan border
            }
            background = bg
        }

        val cardLp = FrameLayout.LayoutParams(
            dpToPx(320),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        rootLayout.addView(dialogCard, cardLp)

        // Title
        mTitleView = TextView(this).apply {
            text = "Add to Home screen"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER_HORIZONTAL
        }
        dialogCard.addView(
            mTitleView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(4)
            }
        )

        // Subtitle (item label)
        mSubtitleView = TextView(this).apply {
            setTextColor(0xAAFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        dialogCard.addView(
            mSubtitleView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(16)
            }
        )

        // Preview Image Frame
        val previewFrame = FrameLayout(this).apply {
            val previewBg = GradientDrawable().apply {
                setColor(0x18FFFFFF)
                cornerRadius = dpToPx(16).toFloat()
            }
            background = previewBg
            val pPad = dpToPx(12)
            setPadding(pPad, pPad, pPad, pPad)
        }

        mPreviewImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val imgSize = dpToPx(80)
        previewFrame.addView(mPreviewImage, FrameLayout.LayoutParams(imgSize, imgSize, Gravity.CENTER))

        dialogCard.addView(
            previewFrame,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(12)
            }
        )

        // Instruction Text
        val hintText = TextView(this).apply {
            text = "Touch & hold to place manually, or tap Add automatically"
            setTextColor(0x80FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        dialogCard.addView(
            hintText,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(20)
            }
        )

        // Buttons Row
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        mCancelButton = Button(this).apply {
            text = "Cancel"
            setTextColor(0xCCFFFFFF.toInt())
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        buttonRow.addView(mCancelButton)

        mAddButton = Button(this).apply {
            text = "Add automatically"
            setTextColor(0xFF12121A.toInt())
            val btnBg = GradientDrawable().apply {
                setColor(0xFF64D2FF.toInt())
                cornerRadius = dpToPx(20).toFloat()
            }
            background = btnBg
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            setOnClickListener { onAddAutomatically() }
        }
        val addBtnLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(40)
        ).apply {
            leftMargin = dpToPx(8)
        }
        buttonRow.addView(mAddButton, addBtnLp)

        dialogCard.addView(
            buttonRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        setContentView(rootLayout)

        // Setup touch and hold drag on preview
        previewFrame.setOnLongClickListener {
            startManualPlacementDrag(previewFrame)
            true
        }
    }

    private fun bindRequestData() {
        val request = mRequest ?: return

        if (request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val shortcutInfo = PinShortcutRequestActivityInfo(request, this)
            mShortcutInfo = shortcutInfo
            mSubtitleView.text = shortcutInfo.label
            val icon = shortcutInfo.getFullResIcon(this)
            if (icon != null) {
                mPreviewImage.setImageDrawable(icon)
            }
        } else if (request.requestType == PinItemRequest.REQUEST_TYPE_APPWIDGET) {
            val widgetHandler = PinWidgetFlowHandler(request, this)
            mWidgetHandler = widgetHandler
            val provider = widgetHandler.providerInfo
            mSubtitleView.text = provider?.label ?: ""
            if (provider != null) {
                val previewLoader = DatabaseWidgetPreviewLoader(this)
                val preview = previewLoader.loadPreview(null, dpToPx(80), dpToPx(80))
                if (preview != null) {
                    mPreviewImage.setImageBitmap(preview)
                }
            }
        }
    }

    private fun startManualPlacementDrag(sourceView: View) {
        val request = mRequest ?: return
        val isShortcut = request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT
        val mimeType = if (isShortcut) {
            PinItemDragListener.MIME_TYPE_PIN_SHORTCUT
        } else {
            PinItemDragListener.MIME_TYPE_PIN_WIDGET
        }

        val clipData = ClipData(
            ClipDescription("PinItem", arrayOf(mimeType)),
            ClipData.Item(Intent())
        )
        val shadow = View.DragShadowBuilder(sourceView)
        sourceView.startDragAndDrop(clipData, shadow, null, 0)
        // Dismiss dialog to allow home screen interaction
        finish()
    }

    protected open fun onAddAutomatically() {
        val request = mRequest
        if (request == null || !request.isValid) {
            finish()
            return
        }

        if (request.requestType == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val shortcutInfo = mShortcutInfo ?: PinShortcutRequestActivityInfo(request, this)
            val iconCache = LauncherAppState.getInstance(this).getIconCache()
            val itemInfo = shortcutInfo.createWorkspaceItemInfo(iconCache).apply {
                container = LauncherSettings.Favorites.CONTAINER_DESKTOP
                screenId = 0
                cellX = 0
                cellY = 0
                spanX = 1
                spanY = 1
            }

            // Enqueue placement via model task
            request.accept()
            Toast.makeText(this, "Added to Home screen", Toast.LENGTH_SHORT).show()
            finish()
        } else if (request.requestType == PinItemRequest.REQUEST_TYPE_APPWIDGET) {
            val widgetHandler = mWidgetHandler ?: PinWidgetFlowHandler(request, this)
            val widgetInfo = widgetHandler.createAppWidgetInfo(
                0,
                LauncherSettings.Favorites.CONTAINER_DESKTOP,
                0,
                0,
                0
            )

            widgetHandler.finishConfirmation(this, null)
            Toast.makeText(this, "Added to Home screen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
