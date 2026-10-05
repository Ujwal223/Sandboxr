/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.sandboxr.launcher.settings

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroup.PreferencePositionCallback
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.BuildConfig
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.LauncherFiles
import com.sandboxr.launcher.R
import com.sandboxr.launcher.display.DisplayController
import com.sandboxr.launcher.util.Executors.MAIN_EXECUTOR
import com.sandboxr.launcher.util.SafeCloseable
import com.sandboxr.launcher.util.SettingsCache

/**
 * Settings activity for Sandboxr Launcher.
 *
 * Manages a [PreferenceFragmentCompat] with launcher configuration options including
 * notification dots toggling, add-to-home-screen defaults, and developer options.
 * Supports deep-link highlighting of a specific preference row via intent extras.
 */
class SettingsActivity : FragmentActivity(),
    OnPreferenceStartFragmentCallback,
    OnPreferenceStartScreenCallback {

    companion object {
        @VisibleForTesting
        const val DEVELOPER_OPTIONS_KEY = "pref_developer_options"

        /** Preference key for fixed landscape orientation mode. */
        const val FIXED_LANDSCAPE_MODE = "pref_fixed_landscape_mode"

        private const val NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging"

        /** Intent extra: bundle of arguments passed to the fragment. */
        const val EXTRA_FRAGMENT_ARGS = ":settings:fragment_args"

        /**
         * Intent extra: the preference key to highlight when opening the settings activity.
         * Causes the list to scroll to and animate that row.
         */
        const val EXTRA_FRAGMENT_HIGHLIGHT_KEY = ":settings:fragment_args_key"

        /**
         * Intent extra: the preference key of the root [PreferenceScreen] to display.
         * Used for sub-screen navigation within settings.
         */
        const val EXTRA_FRAGMENT_ROOT_KEY: String = PreferenceFragmentCompat.ARG_PREFERENCE_ROOT

        private const val DELAY_HIGHLIGHT_DURATION_MILLIS = 600L

        const val SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        setActionBar(findViewById(R.id.action_bar))
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val intent = intent
        if (intent.hasExtra(EXTRA_FRAGMENT_ROOT_KEY)
            || intent.hasExtra(EXTRA_FRAGMENT_ARGS)
            || intent.hasExtra(EXTRA_FRAGMENT_HIGHLIGHT_KEY)
        ) {
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState == null) {
            val args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS) ?: Bundle()

            val highlight = intent.getStringExtra(EXTRA_FRAGMENT_HIGHLIGHT_KEY)
            if (!TextUtils.isEmpty(highlight)) {
                args.putString(EXTRA_FRAGMENT_HIGHLIGHT_KEY, highlight)
            }
            val root = intent.getStringExtra(EXTRA_FRAGMENT_ROOT_KEY)
            if (!TextUtils.isEmpty(root)) {
                args.putString(EXTRA_FRAGMENT_ROOT_KEY, root)
            }

            val fm: FragmentManager = supportFragmentManager
            val fragmentName = getString(R.string.settings_fragment_name)
            val f: Fragment = fm.fragmentFactory.instantiate(classLoader, fragmentName)
            f.arguments = args
            fm.beginTransaction().replace(R.id.content_frame, f).commit()
        }
    }

    private fun startPreference(fragment: String, args: Bundle, key: String): Boolean {
        if (supportFragmentManager.isStateSaved) {
            // Clicks can arrive after onPause due to handler posting — skip safely.
            return false
        }
        val fm: FragmentManager = supportFragmentManager
        val f: Fragment = fm.fragmentFactory.instantiate(classLoader, fragment)
        if (f is DialogFragment) {
            f.arguments = args
            f.show(fm, key)
        } else {
            startActivity(
                Intent(this, SettingsActivity::class.java)
                    .putExtra(EXTRA_FRAGMENT_ARGS, args)
            )
        }
        return true
    }

    override fun onPreferenceStartFragment(
        preferenceFragment: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = pref.fragment ?: return false
        return startPreference(fragment, pref.extras, pref.key)
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen
    ): Boolean {
        val args = Bundle().apply {
            putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        }
        return startPreference(getString(R.string.settings_fragment_name), args, pref.key)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            @Suppress("DEPRECATION")
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LauncherSettingsFragment
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fragment that hosts the launcher preference screen.
     *
     * Reads preferences from [R.xml.launcher_preferences], filters entries based on
     * device type and build flags, applies inset padding for edge-to-edge rendering,
     * and supports animated highlight of a deep-linked preference row.
     */
    class LauncherSettingsFragment : PreferenceFragmentCompat() {

        private var mSettingCacheSafeCloseable: SafeCloseable? = null
        protected var mDeveloperOptionsEnabled = false
        private var mRestartOnResume = false
        private var mHighLightKey: String? = null
        private var mPreferenceHighlighted = false

        override fun onCreate(savedInstanceState: Bundle?) {
            if (BuildConfig.IS_DEBUG_DEVICE) {
                // Listen for DEVELOPMENT_SETTINGS_ENABLED changes and recreate when toggled.
                val devUri: Uri = Settings.Global.getUriFor(
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
                )
                val settingsCache = SettingsCache.getInstance(requireContext())
                mDeveloperOptionsEnabled = settingsCache.getValue(devUri)
                mSettingCacheSafeCloseable = settingsCache.getListenableRef(devUri).forEach(
                    MAIN_EXECUTOR
                ) { v ->
                    if (v != mDeveloperOptionsEnabled) {
                        tryRecreateActivity()
                    }
                }
            }
            super.onCreate(savedInstanceState)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            mHighLightKey = arguments?.getString(EXTRA_FRAGMENT_HIGHLIGHT_KEY)

            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY)
            }

            preferenceManager.sharedPreferencesName = LauncherFiles.SHARED_PREFERENCES_KEY
            setPreferencesFromResource(R.xml.launcher_preferences, rootKey)

            val screen = preferenceScreen
            for (i in screen.preferenceCount - 1 downTo 0) {
                val preference = screen.getPreference(i)
                if (!initPreference(preference)) {
                    screen.removePreference(preference)
                }
            }

            // If the highlight target is not on this screen, navigate to its parent screen.
            val highlightKey = mHighLightKey
            if (highlightKey != null && !isKeyInPreferenceGroup(highlightKey, screen)) {
                val parentScreen = findParentPreference(screen, highlightKey)
                if (parentScreen != null && activity != null) {
                    if (!TextUtils.isEmpty(parentScreen.title)) {
                        activity?.title = parentScreen.title
                    }
                    preferenceScreen = parentScreen
                    return
                }
            }

            val screenTitle = preferenceScreen.title
            if (activity != null && !TextUtils.isEmpty(screenTitle)) {
                activity?.title = screenTitle
            }
        }

        private fun isKeyInPreferenceGroup(targetKey: String, parent: PreferenceGroup): Boolean {
            for (i in 0 until parent.preferenceCount) {
                val pref = parent.getPreference(i)
                if (pref.key == targetKey) return true
            }
            return false
        }

        /**
         * Recursively finds the [PreferenceScreen] that directly contains [targetKey].
         *
         * @param parent The screen to search within.
         * @param targetKey The preference key to locate.
         * @return The parent [PreferenceScreen], or `null` if not found.
         */
        private fun findParentPreference(
            parent: PreferenceScreen,
            targetKey: String
        ): PreferenceScreen? {
            for (i in 0 until parent.preferenceCount) {
                val pref = parent.getPreference(i)
                if (pref is PreferenceScreen) {
                    val found = findParentPreference(pref, targetKey)
                    if (found != null) return found
                } else if (pref.key == targetKey) {
                    return parent
                }
            }
            return null
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val listView = listView
            val bottomPadding = listView.paddingBottom
            listView.setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(
                    v.paddingLeft,
                    v.paddingTop,
                    v.paddingRight,
                    bottomPadding + insets.systemWindowInsetBottom
                )
                insets.consumeSystemWindowInsets()
            }
            // Honour locale text direction for RTL language support.
            view.textDirection = View.TEXT_DIRECTION_LOCALE
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted)
        }

        /**
         * Initialises a preference entry.
         *
         * Returning `false` removes the preference from the displayed list. Called for every
         * preference loaded from [R.xml.launcher_preferences].
         */
        protected open fun initPreference(preference: Preference): Boolean {
            val ctx = requireContext()
            return when (preference.key) {
                NOTIFICATION_DOTS_PREFERENCE_KEY -> {
                    BuildConfig.NOTIFICATION_DOTS_ENABLED
                }

                DEVELOPER_OPTIONS_KEY -> {
                    if (BuildConfig.IS_STUDIO_BUILD) {
                        preference.order = 0
                    }
                    mDeveloperOptionsEnabled
                }

                FIXED_LANDSCAPE_MODE -> {
                    val deviceType = InvariantDeviceProfile.get(ctx).deviceType
                    if (deviceType == InvariantDeviceProfile.TYPE_MULTI_DISPLAY
                        || deviceType == InvariantDeviceProfile.TYPE_TABLET
                    ) {
                        return false
                    }
                    // Rotate screen immediately when the setting toggles to preview the result.
                    preference.setOnPreferenceChangeListener { _, newValue ->
                        activity?.requestedOrientation = if (newValue as Boolean) {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_USER
                        }
                        true
                    }
                    val info = DisplayController.getInstance(ctx).info
                    !info.isLargeScreen(info.realBounds)
                }

                else -> true
            }
        }

        override fun onResume() {
            super.onResume()

            if (isAdded && !mPreferenceHighlighted) {
                val highlighter = createHighlighter()
                if (highlighter != null) {
                    view?.postDelayed(highlighter, DELAY_HIGHLIGHT_DURATION_MILLIS)
                    mPreferenceHighlighted = true
                }
            }

            if (mRestartOnResume) {
                recreateActivityNow()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            mSettingCacheSafeCloseable?.close()
            mSettingCacheSafeCloseable = null
        }

        /** Schedules an activity recreate, or defers it until [onResume] if not yet resumed. */
        protected fun tryRecreateActivity() {
            if (isResumed) recreateActivityNow() else mRestartOnResume = true
        }

        private fun recreateActivityNow() {
            activity?.recreate()
        }

        private fun createHighlighter(): PreferenceHighlighter? {
            val highlightKey = mHighLightKey?.takeIf { it.isNotEmpty() } ?: return null
            val screen = preferenceScreen ?: return null
            val list: RecyclerView = listView
            val callback = list.adapter as? PreferencePositionCallback ?: return null
            val position = callback.getPreferenceAdapterPosition(highlightKey)
            return if (position >= 0) {
                PreferenceHighlighter(list, position, screen.findPreference(highlightKey)!!)
            } else {
                null
            }
        }
    }
}
