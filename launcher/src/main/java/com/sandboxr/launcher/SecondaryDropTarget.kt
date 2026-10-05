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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import com.sandboxr.launcher.DropTarget.DragObject
import com.sandboxr.launcher.dragndrop.DragOptions
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.model.data.ItemInfoWithIcon
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo

/**
 * Secondary drop target that dynamically adapts between Uninstall, App Info, or Widget Reconfigure
 * based on the dragged item.
 */
open class SecondaryDropTarget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ButtonDropTarget(context, attrs, defStyle) {

    enum class TargetMode {
        UNINSTALL,
        APP_INFO,
        RECONFIGURE
    }

    var currentMode: TargetMode = TargetMode.UNINSTALL
        private set

    private val uninstallGlowBackground = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 32f
        setColor(0x33FF9F0A.toInt()) // Liquid Glass amber glow
        setStroke(2, 0x80FF9F0A.toInt())
    }

    private val infoGlowBackground = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 32f
        setColor(0x330A84FF.toInt()) // Liquid Glass azure glow
        setStroke(2, 0x800A84FF.toInt())
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        setMode(TargetMode.UNINSTALL)
    }

    fun setMode(mode: TargetMode) {
        currentMode = mode
        when (mode) {
            TargetMode.UNINSTALL -> {
                setDrawable(R.drawable.ic_uninstall_no_shadow)
                setText(R.string.uninstall_drop_target_label)
            }
            TargetMode.APP_INFO -> {
                setDrawable(R.drawable.ic_info_no_shadow)
                setText(R.string.app_info_drop_target_label)
            }
            TargetMode.RECONFIGURE -> {
                setDrawable(R.drawable.ic_info_no_shadow)
                setText(R.string.reconfigure_drop_target_label)
            }
        }
        mText = text
    }

    override fun supportsDrop(info: ItemInfo?): Boolean {
        if (info == null) return false
        return getTargetPackage(info) != null || info is LauncherAppWidgetInfo
    }

    private fun getTargetPackage(info: ItemInfo): String? {
        return (info as? AppInfo)?.componentName?.packageName
            ?: (info as? WorkspaceItemInfo)?.targetPackage
            ?: (info as? WorkspaceItemInfo)?.intent?.`package`
    }

    private fun isSystemApp(info: ItemInfo): Boolean {
        if (info is ItemInfoWithIcon) {
            return (info.runtimeStatusFlags and ItemInfoWithIcon.FLAG_SYSTEM_MASK) != ItemInfoWithIcon.FLAG_SYSTEM_NO
        }
        return false
    }

    override fun onDragStart(dragObject: DragObject, options: DragOptions) {
        super.onDragStart(dragObject, options)
        val item = dragObject.dragInfo
        if (item == null || !supportsDrop(item)) {
            mActive = false
            visibility = GONE
            return
        }

        mActive = true
        visibility = VISIBLE

        if (item is LauncherAppWidgetInfo) {
            setMode(TargetMode.RECONFIGURE)
        } else if (isSystemApp(item)) {
            setMode(TargetMode.APP_INFO)
        } else {
            setMode(TargetMode.UNINSTALL)
        }
    }

    override fun onDragEnter(dragObject: DragObject) {
        super.onDragEnter(dragObject)
        when (currentMode) {
            TargetMode.UNINSTALL -> {
                background = uninstallGlowBackground
                setTextColor(Color.parseColor("#FFFFB340"))
            }
            TargetMode.APP_INFO, TargetMode.RECONFIGURE -> {
                background = infoGlowBackground
                setTextColor(Color.parseColor("#FF64D2FF"))
            }
        }
    }

    override fun onDragExit(dragObject: DragObject) {
        super.onDragExit(dragObject)
        background = null
        setTextColor(Color.WHITE)
    }

    override fun onDrop(dragObject: DragObject, options: Any?) {
        background = null
        setTextColor(Color.WHITE)
        mDropTargetBar?.deferOnDragEnd()
        super.onDrop(dragObject, options)
    }

    override fun completeDrop(dragObject: DragObject) {
        val item = dragObject.dragInfo ?: return
        val targetPkg = getTargetPackage(item)
        val componentName = if (targetPkg != null) ComponentName(targetPkg, "") else null

        when (currentMode) {
            TargetMode.UNINSTALL -> {
                mDropTargetHandler?.onSecondaryTargetCompleteDrop(componentName, dragObject)
            }
            TargetMode.APP_INFO -> {
                mDropTargetHandler?.onAppInfoCompleteDrop(componentName, dragObject)
            }
            TargetMode.RECONFIGURE -> {
                if (item is LauncherAppWidgetInfo) {
                    // Reconfigure widget
                }
            }
        }
    }

    override fun getSupportedAccessibilityAction(info: ItemInfo?, view: View?): Int {
        if (info == null) return -1
        return if (getUninstallTarget(context, info) != null) {
            R.id.action_uninstall
        } else {
            -1
        }
    }

    override fun onAccessibilityDrop(view: View?, info: ItemInfo?, action: Int) {
        if (action == R.id.action_uninstall && info != null) {
            performUninstall(context, getUninstallTarget(context, info), info)
        }
    }

    companion object {
        @JvmStatic
        fun getUninstallTarget(context: Context, item: ItemInfo?): ComponentName? {
            if (item == null) return null
            val targetPkg = item.targetComponent?.packageName ?: return null
            return ComponentName(targetPkg, item.targetComponent?.className ?: "")
        }

        @JvmStatic
        fun performUninstall(context: Context, cn: ComponentName?, info: ItemInfo?): ComponentName? {
            if (cn == null) return null
            try {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = android.net.Uri.fromParts("package", cn.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return cn
            } catch (e: Exception) {
                return null
            }
        }
    }
}
