/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.GridType.Companion.GRID_TYPE_ANY
import com.sandboxr.launcher.LauncherFiles.DEVICE_PREFERENCES_KEY
import com.sandboxr.launcher.LauncherFiles.SHARED_PREFERENCES_KEY
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppComponent
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.model.DeviceGridState
import com.sandboxr.launcher.pm.InstallSessionHelper
import com.sandboxr.launcher.popup.ui.ExpandedSection
import com.sandboxr.launcher.provider.RestoreDbTask
import com.sandboxr.launcher.provider.RestoreDbTask.Companion.FIRST_LOAD_AFTER_RESTORE_KEY
import com.android.launcher3.settings.SettingsActivity
import com.sandboxr.launcher.util.DaggerSingletonObject
import com.sandboxr.launcher.util.DefaultsValueProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Manages Launcher [SharedPreferences] through [Item] instances.
 */
@LauncherAppSingleton
open class LauncherPrefs
@Inject
constructor(@ApplicationContext private val encryptedContext: Context) {

    private val deviceProtectedSharedPrefs: SharedPreferences by lazy {
        encryptedContext
            .createDeviceProtectedStorageContext()
            .getSharedPreferences(BOOT_AWARE_PREFS_KEY, MODE_PRIVATE)
    }

    protected open fun getSharedPrefs(item: Item): SharedPreferences =
        item.run {
            if (encryptionType == EncryptionType.DEVICE_PROTECTED) deviceProtectedSharedPrefs
            else encryptedContext.getSharedPreferences(sharedPrefFile, MODE_PRIVATE)
        }

    @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
    val backedUpPrefs: SharedPreferences
        get() = getSharedPrefs(GRID_NAME)

    @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
    val devicePrefs: SharedPreferences
        get() = getSharedPrefs(IS_FIRST_LOAD_AFTER_RESTORE)

    /** Returns the value with type [T] for [item]. */
    fun <T> get(item: ContextualItem<T>): T =
        getInner(item, item.defaultValueFromContext(encryptedContext))

    /** Returns the value with type [T] for [item]. */
    fun <T> get(item: ConstantItem<T>): T = getInner(item, item.defaultValue)

    /**
     * Retrieves the value for an [Item] from [SharedPreferences].
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    private fun <T> getInner(item: Item, default: T): T {
        val sp = getSharedPrefs(item)
        return when {
            item.type == String::class.java -> sp.getString(item.sharedPrefKey, default as? String)
            item.type == Boolean::class.java || item.type == java.lang.Boolean::class.java ->
                sp.getBoolean(item.sharedPrefKey, default as Boolean)
            item.type == Int::class.java || item.type == java.lang.Integer::class.java ->
                sp.getInt(item.sharedPrefKey, default as Int)
            item.type == Float::class.java || item.type == java.lang.Float::class.java ->
                sp.getFloat(item.sharedPrefKey, default as Float)
            item.type == Long::class.java || item.type == java.lang.Long::class.java ->
                sp.getLong(item.sharedPrefKey, default as Long)
            Set::class.java.isAssignableFrom(item.type) ->
                sp.getStringSet(item.sharedPrefKey, default as? Set<String>)
            item.type.isEnum -> {
                val name = sp.getString(item.sharedPrefKey, (default as Enum<*>).name)
                item.type.enumConstants?.find { (it as Enum<*>).name == name } ?: default
            }
            else ->
                throw IllegalArgumentException(
                    "item type: ${item.type} is not compatible with sharedPref methods"
                )
        }
            as T
    }

    /**
     * Stores each of the values provided in `SharedPreferences` according to the configuration
     * contained within the associated items provided.
     */
    fun put(vararg itemsToValues: Pair<Item, Any>): Unit =
        prepareToPutValues(itemsToValues).forEach { it.apply() }

    /** See referenced `put` method above. */
    fun <T : Any> put(item: Item, value: T): Unit = put(item.to(value))

    /**
     * Synchronously stores all the values provided according to their associated Item
     * configuration.
     */
    fun putSync(vararg itemsToValues: Pair<Item, Any>): Unit =
        prepareToPutValues(itemsToValues).forEach { it.commit() }

    private fun prepareToPutValues(
        updates: Array<out Pair<Item, Any>>
    ): List<SharedPreferences.Editor> {
        val updatesPerPrefFile = updates.groupBy { getSharedPrefs(it.first) }.toMap()

        return updatesPerPrefFile.map { (sharedPref, itemList) ->
            sharedPref.edit().apply { itemList.forEach { (item, value) -> putValue(item, value) } }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun SharedPreferences.Editor.putValue(
        item: Item,
        value: Any?,
    ): SharedPreferences.Editor =
        when {
            item.type == String::class.java -> putString(item.sharedPrefKey, value as? String)
            item.type == Boolean::class.java || item.type == java.lang.Boolean::class.java ->
                putBoolean(item.sharedPrefKey, value as Boolean)
            item.type == Int::class.java || item.type == java.lang.Integer::class.java ->
                putInt(item.sharedPrefKey, value as Int)
            item.type == Float::class.java || item.type == java.lang.Float::class.java ->
                putFloat(item.sharedPrefKey, value as Float)
            item.type == Long::class.java || item.type == java.lang.Long::class.java ->
                putLong(item.sharedPrefKey, value as Long)
            Set::class.java.isAssignableFrom(item.type) ->
                putStringSet(item.sharedPrefKey, value as? Set<String>)
            item.type.isEnum -> putString(item.sharedPrefKey, (value as Enum<*>).name)
            else ->
                throw IllegalArgumentException(
                    "item type: ${item.type} is not compatible with sharedPref methods"
                )
        }

    fun addListener(listener: LauncherPrefChangeListener, vararg items: Item) {
        items
            .map { getSharedPrefs(it) }
            .distinct()
            .forEach { it.registerOnSharedPreferenceChangeListener(listener) }
    }

    fun removeListener(listener: LauncherPrefChangeListener, vararg items: Item) {
        items
            .map { getSharedPrefs(it) }
            .distinct()
            .forEach { it.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun has(vararg items: Item): Boolean {
        items
            .groupBy { getSharedPrefs(it) }
            .forEach { (prefs, itemsSublist) ->
                if (!itemsSublist.none { !prefs.contains(it.sharedPrefKey) }) return false
            }
        return true
    }

    fun remove(vararg items: Item) = prepareToRemove(items).forEach { it.apply() }

    fun removeSync(vararg items: Item) = prepareToRemove(items).forEach { it.commit() }

    private fun prepareToRemove(items: Array<out Item>): List<SharedPreferences.Editor> {
        val itemsPerFile = items.groupBy { getSharedPrefs(it) }.toMap()

        return itemsPerFile.map { (prefs, items) ->
            prefs.edit().also { editor ->
                items.forEach { item -> editor.remove(item.sharedPrefKey) }
            }
        }
    }

    companion object {
        @VisibleForTesting const val BOOT_AWARE_PREFS_KEY = "boot_aware_prefs"

        const val COMPOSITION_TRACING_PREF_KEY = "pref_enableCompositionTracing"

        @JvmField val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getLauncherPrefs)

        @JvmStatic fun get(context: Context): LauncherPrefs = INSTANCE.get(context)

        const val TASKBAR_PINNING_KEY = "TASKBAR_PINNING_KEY"
        const val TASKBAR_PINNING_DESKTOP_MODE_KEY = "TASKBAR_PINNING_DESKTOP_MODE_KEY"

        const val ENABLE_TWO_LINE_TOGGLE_KEY: String = "pref_enable_two_line_toggle"

        @JvmField
        val ENABLE_TWOLINE_ALLAPPS_TOGGLE =
            backedUpItem(ENABLE_TWO_LINE_TOGGLE_KEY, Boolean::class.java) { context ->
                DefaultsValueProvider.get(context).enableTwoLineToggle
            }

        @JvmField
        val PROMISE_ICON_IDS = nonRestorableItem(InstallSessionHelper.PROMISE_ICON_IDS, "")
        @JvmField val WORK_EDU_STEP = backedUpItem("showed_work_profile_edu", 0)
        @JvmField
        val WORKSPACE_SIZE =
            backedUpItem(DeviceGridState.KEY_WORKSPACE_SIZE, "", EncryptionType.ENCRYPTED)
        @JvmField
        val HOTSEAT_COUNT =
            backedUpItem(DeviceGridState.KEY_HOTSEAT_COUNT, -1, EncryptionType.ENCRYPTED)
        @JvmField
        val TASKBAR_PINNING =
            backedUpItem(TASKBAR_PINNING_KEY, false, EncryptionType.DEVICE_PROTECTED)
        @JvmField
        val TASKBAR_PINNING_IN_DESKTOP_MODE =
            backedUpItem(TASKBAR_PINNING_DESKTOP_MODE_KEY, true, EncryptionType.DEVICE_PROTECTED)

        @JvmField
        val DEVICE_TYPE =
            backedUpItem(
                DeviceGridState.KEY_DEVICE_TYPE,
                0, // TYPE_PHONE
                EncryptionType.ENCRYPTED,
            )
        @JvmField
        val DB_FILE = backedUpItem(DeviceGridState.KEY_DB_FILE, "", EncryptionType.ENCRYPTED)
        @JvmField
        val GRID_TYPE =
            backedUpItem(DeviceGridState.KEY_GRID_TYPE, GRID_TYPE_ANY, EncryptionType.ENCRYPTED)
        @JvmField
        val RESTORE_DEVICE =
            backedUpItem(
                RestoreDbTask.RESTORED_DEVICE_TYPE,
                0, // TYPE_PHONE
                EncryptionType.ENCRYPTED,
            )
        @JvmField
        val NO_DB_FILES_RESTORED =
            nonRestorableItem("no_db_files_restored", false, EncryptionType.DEVICE_PROTECTED)
        @JvmField
        val IS_FIRST_LOAD_AFTER_RESTORE =
            nonRestorableItem(FIRST_LOAD_AFTER_RESTORE_KEY, false, EncryptionType.ENCRYPTED)
        @JvmField val APP_WIDGET_IDS = backedUpItem(RestoreDbTask.APPWIDGET_IDS, "")
        @JvmField val OLD_APP_WIDGET_IDS = backedUpItem(RestoreDbTask.APPWIDGET_OLD_IDS, "")

        @JvmField
        val GRID_NAME =
            ConstantItem(
                "grid_name",
                isBackedUp = true,
                defaultValue = null,
                encryptionType = EncryptionType.ENCRYPTED,
                type = String::class.java,
            )

        @JvmField val ENABLE_COMPOSITION_TRACING = backedUpItem(COMPOSITION_TRACING_PREF_KEY, false)

        @JvmField
        val FIXED_LANDSCAPE_MODE = backedUpItem(SettingsActivity.FIXED_LANDSCAPE_MODE, false)

        @JvmField
        val WORKSPACE_ITEMS_LABEL_HIDDEN = backedUpItem("pref_workspace_items_label_hidden", false)

        @JvmField
        val EXPANDED_POPUP_MENU_SECTION =
            backedUpItem("pref_expanded_popup_menu", ExpandedSection.SYSTEM)

        @JvmField
        val NON_FIXED_LANDSCAPE_GRID_NAME =
            ConstantItem(
                "non_fixed_landscape_grid_name",
                isBackedUp = true,
                defaultValue = null,
                encryptionType = EncryptionType.ENCRYPTED,
                type = String::class.java,
            )

        // Preferences for widget configurations
        @JvmField
        val RECONFIGURABLE_WIDGET_EDUCATION_TIP_SEEN =
            backedUpItem("launcher.reconfigurable_widget_education_tip_seen", false)

        // Preferences for Launcher statistics
        @JvmField
        val ALL_APPS_TIP_SHOWN_TIMESTAMPS =
            backedUpItem("launcher.all_apps_tip_shown_timestamps", "")

        @JvmField val ALL_APPS_OPEN_UP_COUNT = backedUpItem("launcher.all_apps_open_up_count", 0)

        @JvmField val INITIAL_TIMESTAMP = backedUpItem("launcher.initial_timestamp", 0L)

        @JvmField val SELECT_TIP_SEEN = backedUpItem("launcher.select_tip_seen", false)

        @JvmStatic
        fun <T> backedUpItem(
            sharedPrefKey: String,
            defaultValue: T,
            encryptionType: EncryptionType = EncryptionType.ENCRYPTED,
        ): ConstantItem<T> =
            ConstantItem(sharedPrefKey, isBackedUp = true, defaultValue, encryptionType)

        @JvmStatic
        fun <T> backedUpItem(
            sharedPrefKey: String,
            type: Class<out T>,
            encryptionType: EncryptionType = EncryptionType.ENCRYPTED,
            defaultValueFromContext: (c: Context) -> T,
        ): ContextualItem<T> =
            ContextualItem(
                sharedPrefKey,
                isBackedUp = true,
                defaultValueFromContext,
                encryptionType,
                type,
            )

        @JvmStatic
        fun <T> nonRestorableItem(
            sharedPrefKey: String,
            defaultValue: T,
            encryptionType: EncryptionType = EncryptionType.ENCRYPTED,
        ): ConstantItem<T> =
            ConstantItem(sharedPrefKey, isBackedUp = false, defaultValue, encryptionType)

        @Deprecated("Don't use shared preferences directly. Use other LauncherPref methods.")
        @JvmStatic
        fun getPrefs(context: Context) = INSTANCE.get(context).backedUpPrefs
    }
}

abstract class Item {
    abstract val sharedPrefKey: String
    abstract val isBackedUp: Boolean
    abstract val type: Class<*>
    abstract val encryptionType: EncryptionType
    val sharedPrefFile: String
        get() = if (isBackedUp) SHARED_PREFERENCES_KEY else DEVICE_PREFERENCES_KEY

    fun <T> to(value: T): Pair<Item, T> = Pair(this, value)
}

data class ConstantItem<T>(
    override val sharedPrefKey: String,
    override val isBackedUp: Boolean,
    val defaultValue: T,
    override val encryptionType: EncryptionType,
    override val type: Class<out T> = defaultValue!!::class.java,
) : Item() {
    fun get(c: Context): T = LauncherPrefs.get(c).get(this)
}

data class ContextualItem<T>(
    override val sharedPrefKey: String,
    override val isBackedUp: Boolean,
    private val defaultSupplier: (c: Context) -> T,
    override val encryptionType: EncryptionType,
    override val type: Class<out T>,
) : Item() {
    private var default: T? = null

    fun defaultValueFromContext(context: Context): T {
        if (default == null) {
            default = defaultSupplier(context)
        }
        return default!!
    }

    fun get(c: Context): T = LauncherPrefs.get(c).get(this)
}

enum class EncryptionType {
    ENCRYPTED,
    DEVICE_PROTECTED,
}

class ProxyPrefs(context: Context, private val prefs: SharedPreferences) : LauncherPrefs(context) {

    private val copiedPrefs = ConcurrentHashMap<SharedPreferences, Boolean>()

    override fun getSharedPrefs(item: Item): SharedPreferences {
        val originalPrefs = super.getSharedPrefs(item)
        copiedPrefs.computeIfAbsent(originalPrefs) { op ->
            val editor = prefs.edit()
            op.all.forEach { (key, value) ->
                if (value != null) {
                    editor.putValue(backedUpItem(key, value), value)
                }
            }
            editor.commit()
        }
        return prefs
    }
}
