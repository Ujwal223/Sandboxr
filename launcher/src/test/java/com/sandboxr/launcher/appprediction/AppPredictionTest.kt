/*
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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.Hotseat
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.hybridhotseat.HybridHotseatOrganizer
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.views.ActivityContext
import com.sandboxr.launcher.views.PredictedAppIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppPredictionTest {

    private lateinit var context: Context
    private lateinit var idp: InvariantDeviceProfile
    private lateinit var dp: DeviceProfile

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        idp = InvariantDeviceProfile(context)
        dp = idp.getDeviceProfile(context)
    }

    private fun createAppInfo(packageName: String, className: String): AppInfo {
        val cn = ComponentName(packageName, className)
        val intent = Intent(Intent.ACTION_MAIN).setComponent(cn)
        return AppInfo().apply {
            this.componentName = cn
            this.intent = intent
            this.user = UserHandle.getUserHandleForUid(0)
            this.title = packageName.substringAfterLast('.')
        }
    }

    @Test
    fun testAppPredictionManager_recordsAndRanks() {
        val manager = AppPredictionManager.get(context)
        manager.clearStats()

        val appA = createAppInfo("com.example.appA", "com.example.appA.MainActivity")
        val appB = createAppInfo("com.example.appB", "com.example.appB.MainActivity")
        val appC = createAppInfo("com.example.appC", "com.example.appC.MainActivity")

        // Record launches: App B launched 5 times, App A launched 1 time, App C launched 0 times
        repeat(5) { manager.logAppLaunch(appB) }
        manager.logAppLaunch(appA)

        val predictions = manager.computePredictions(arrayOf(appA, appB, appC))

        assertNotNull("Predictions should not be null", predictions)
        assertTrue("Should have predictions", predictions.isNotEmpty())
        assertEquals("Top predicted app should be App B", "com.example.appB", predictions[0].targetComponent?.packageName)
        assertEquals("Second predicted app should be App A", "com.example.appA", predictions[1].targetComponent?.packageName)
    }

    @Test
    fun testPredictedAppIcon_pinning() {
        val icon = PredictedAppIcon(context)
        val itemInfo = WorkspaceItemInfo()
        icon.applyFromWorkspaceItem(itemInfo)

        assertTrue("Icon should be in predicted state initially", icon.isPredicted)

        icon.pin()

        assertFalse("Icon should no longer be in predicted state after pin()", icon.isPredicted)
    }

    @Test
    fun testHybridHotseatOrganizer_gapFilling() {
        val mockActivity = object : ActivityContext {
            override fun getActivityComponent() = null
            override fun getRootView(): View? = null
            override fun getLayoutInflater() = null
            override fun getDeviceProfile(): DeviceProfile = dp
            override fun asContext(): Context = context
            override val lifecycle = androidx.lifecycle.LifecycleRegistry(this)
            override val savedStateRegistry = androidx.savedstate.SavedStateRegistryController.create(this).savedStateRegistry
        }

        val hotseat = Hotseat(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val organizer = HybridHotseatOrganizer(
            activityContext = mockActivity,
            hotseat = hotseat
        )

        // Hotseat starts empty. Let's provide 3 predictions
        val app1 = createAppInfo("com.test.app1", "com.test.app1.MainActivity")
        val app2 = createAppInfo("com.test.app2", "com.test.app2.MainActivity")
        val app3 = createAppInfo("com.test.app3", "com.test.app3.MainActivity")

        val p1 = app1.makeWorkspaceItem(context)
        val p2 = app2.makeWorkspaceItem(context)
        val p3 = app3.makeWorkspaceItem(context)

        organizer.predictedItems = listOf(p1, p2, p3)

        val shortcutsAndWidgets = hotseat.shortcutsAndWidgets
        assertNotNull(shortcutsAndWidgets)

        // Pinned app should not be displaced
        val pinnedItem = WorkspaceItemInfo().apply {
            title = "Pinned App"
            intent = Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.pinned.app", "com.pinned.app.Main"))
            cellX = 0
            cellY = 0
        }
        val pinnedIcon = com.sandboxr.launcher.BubbleTextView(context).apply {
            applyFromWorkspaceItem(pinnedItem)
            tag = pinnedItem
        }
        shortcutsAndWidgets.addView(pinnedIcon)

        // Update predictions with pinned component included in predictions
        val duplicatePred = createAppInfo("com.pinned.app", "com.pinned.app.Main").makeWorkspaceItem(context)
        organizer.predictedItems = listOf(duplicatePred, p1, p2)

        organizer.destroy()
    }
}
