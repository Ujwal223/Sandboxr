/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.sandboxr.launcher.secondarydisplay

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import android.os.UserHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.sandboxr.launcher.AbstractFloatingView
import com.sandboxr.launcher.BubbleTextView
import com.sandboxr.launcher.R
import com.sandboxr.launcher.allapps.AllAppsStore
import com.sandboxr.launcher.allapps.AppInfoComparator
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.ItemInfo
import com.sandboxr.launcher.pm.UserCache
import com.sandboxr.launcher.popup.SystemShortcut
import com.sandboxr.launcher.util.ComponentKey
import com.sandboxr.launcher.util.Executors

/**
 * Adapter to manage pinned apps and show them in a grid on secondary display.
 */
class PinnedAppsAdapter(
    private val mLauncher: SecondaryDisplayLauncher,
    private val mAllAppsList: AllAppsStore,
    private val mOnLongClickListener: View.OnLongClickListener
) : BaseAdapter(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val PINNED_APPS_KEY = "pinned_apps"
    }

    private val mOnClickListener: View.OnClickListener = mLauncher.getItemOnClickListener()
    private val mPrefs: SharedPreferences = mLauncher.getSharedPreferences(PINNED_APPS_KEY, Context.MODE_PRIVATE)
    private val mAppNameComparator: AppInfoComparator = AppInfoComparator(mLauncher)

    private val mPinnedApps: MutableSet<ComponentKey> = HashSet()
    private val mItems: ArrayList<AppInfo> = ArrayList()

    init {
        mAllAppsList.addUpdateListener(::createFilteredAppsList)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (PINNED_APPS_KEY == key) {
            Executors.MODEL_EXECUTOR.submit {
                val rawSet = prefs.getStringSet(key, emptySet()) ?: emptySet()
                val apps = rawSet.mapNotNull { parseComponentKey(it) }.toSet()
                Executors.MAIN_EXECUTOR.submit {
                    mPinnedApps.clear()
                    mPinnedApps.addAll(apps)
                    createFilteredAppsList()
                }
            }
        }
    }

    override fun getCount(): Int = mItems.size

    override fun getItem(position: Int): AppInfo = mItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val icon = if (convertView is BubbleTextView) {
            convertView
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.app_icon, parent, false)
            val btv = view as BubbleTextView
            btv.setOnClickListener(mOnClickListener)
            btv.setOnLongClickListener(mOnLongClickListener)
            val padding = mLauncher.resources.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin)
            btv.setPadding(padding, padding, padding, padding)
            btv
        }

        icon.applyFromApplicationInfo(mItems[position])
        return icon
    }

    fun createFilteredAppsList() {
        mItems.clear()
        mPinnedApps.mapNotNull { mAllAppsList.getApp(it) }.forEach { mItems.add(it) }
        mItems.sortWith(mAppNameComparator)
        notifyDataSetChanged()
    }

    fun init() {
        mPrefs.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(mPrefs, PINNED_APPS_KEY)
    }

    fun destroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun update(info: ItemInfo, op: (ComponentKey) -> Boolean) {
        val component = info.targetComponent ?: return
        val user = info.user ?: Process.myUserHandle()
        val key = ComponentKey(component, user)
        if (op(key)) {
            createFilteredAppsList()
            val copy = HashSet(mPinnedApps)
            Executors.MODEL_EXECUTOR.submit {
                mPrefs.edit().putStringSet(
                    PINNED_APPS_KEY,
                    copy.map { encode(it) }.toSet()
                ).apply()
            }
        }
    }

    private fun parseComponentKey(string: String): ComponentKey? {
        return try {
            val parts = string.split("#")
            val user = if (parts.size >= 2) {
                val serial = parts.last().toLongOrNull()
                if (serial != null) {
                    UserCache.getInstance(mLauncher).getUserForSerialNumber(serial)
                        ?: Process.myUserHandle()
                } else {
                    Process.myUserHandle()
                }
            } else {
                Process.myUserHandle()
            }
            val cn = ComponentName.unflattenFromString(parts[0]) ?: return null
            ComponentKey(cn, user)
        } catch (e: Exception) {
            null
        }
    }

    private fun encode(key: ComponentKey): String {
        return key.componentName.flattenToShortString() + "#" +
                UserCache.getInstance(mLauncher).getSerialNumberForUser(key.user)
    }

    fun getSystemShortcut(info: ItemInfo, originalView: View): SystemShortcut<SecondaryDisplayLauncher> {
        val user = info.user ?: Process.myUserHandle()
        val key = info.targetComponent?.let { ComponentKey(it, user) }
        val isPinned = key != null && mPinnedApps.contains(key)
        return PinUnPinShortcut(mLauncher, info, originalView, isPinned)
    }

    fun addPinnedApp(info: ItemInfo) {
        update(info) { mPinnedApps.add(it) }
    }

    fun removePinnedApp(info: ItemInfo) {
        update(info) { mPinnedApps.remove(it) }
    }

    fun isPinned(info: ItemInfo): Boolean {
        val user = info.user ?: Process.myUserHandle()
        val key = info.targetComponent?.let { ComponentKey(it, user) } ?: return false
        return mPinnedApps.contains(key)
    }

    private inner class PinUnPinShortcut(
        target: SecondaryDisplayLauncher,
        info: ItemInfo,
        originalView: View,
        private val mIsPinned: Boolean
    ) : SystemShortcut<SecondaryDisplayLauncher>(
        if (mIsPinned) R.drawable.ic_remove_no_shadow else R.drawable.ic_pin,
        if (mIsPinned) R.string.remove_drop_target_label else R.string.action_add_to_workspace,
        target,
        info,
        originalView
    ) {
        override fun onClick(view: View?) {
            if (mIsPinned) {
                update(mItemInfo) { mPinnedApps.remove(it) }
            } else {
                update(mItemInfo) { mPinnedApps.add(it) }
            }
            AbstractFloatingView.closeAllOpenViews(mLauncher)
        }
    }
}
