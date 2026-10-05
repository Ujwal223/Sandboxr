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

package com.sandboxr.launcher.settings

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import com.sandboxr.launcher.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsActivityTest {

    @Test
    fun testSettingsActivityLaunchAndFragmentCreation() {
        val context: Context = RuntimeEnvironment.getApplication()
        val intent = Intent(context, SettingsActivity::class.java)
        val controller = Robolectric.buildActivity(SettingsActivity::class.java, intent)
        controller.create().start().resume()
        val activity = controller.get()

        assertNotNull(activity)
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)
        assertNotNull(fragment)
        assertTrue(fragment is SettingsActivity.LauncherSettingsFragment)

        val settingsFragment = fragment as SettingsActivity.LauncherSettingsFragment
        val screen = settingsFragment.preferenceScreen
        assertNotNull(screen)

        // Verify preferences are loaded from XML
        val notifPref = screen.findPreference<Preference>("pref_icon_badging")
        assertNotNull(notifPref)
        assertTrue(notifPref is NotificationDotsPreference)

        val addIconPref = screen.findPreference<Preference>("pref_add_icon_to_home")
        assertNotNull(addIconPref)
        assertTrue(addIconPref is SwitchPreference)

        controller.pause().stop().destroy()
    }

    @Test
    fun testSettingsActivityWithHighlightKey() {
        val context: Context = RuntimeEnvironment.getApplication()
        val intent = Intent(context, SettingsActivity::class.java).apply {
            putExtra(SettingsActivity.EXTRA_FRAGMENT_HIGHLIGHT_KEY, "pref_icon_badging")
        }
        val controller = Robolectric.buildActivity(SettingsActivity::class.java, intent)
        controller.create().start().resume()
        val activity = controller.get()

        assertNotNull(activity)
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)
        assertNotNull(fragment)

        controller.pause().stop().destroy()
    }

    @Test
    fun testPreferenceHighlighterCreation() {
        val context: Context = RuntimeEnvironment.getApplication()
        val rv = RecyclerView(context)
        val pref = Preference(context)
        val highlighter = PreferenceHighlighter(rv, 0, pref)
        assertNotNull(highlighter)
    }
}
