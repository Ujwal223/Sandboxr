package com.sandboxr.launcher.loader

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LawnchairAppLoaderTest {

    private lateinit var context: Context
    private lateinit var loader: LawnchairAppLoader

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        loader = LawnchairAppLoader(context)
    }

    @Test
    fun testLoadAllSystemAppsWithPackageManagerFallback() = runBlocking {
        val pm = context.packageManager
        val shadowPm = shadowOf(pm)

        // Install test mock applications into the Robolectric environment
        val app1 = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfo1 = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.chrome"
                name = "com.android.chrome.Main"
                applicationInfo = ApplicationInfo().apply {
                    packageName = "com.android.chrome"
                    name = "Chrome"
                }
            }
        }
        val resolveInfo2 = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "org.mozilla.firefox"
                name = "org.mozilla.firefox.App"
                applicationInfo = ApplicationInfo().apply {
                    packageName = "org.mozilla.firefox"
                    name = "Firefox"
                }
            }
        }
        val selfResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = context.packageName
                name = "${context.packageName}.MainActivity"
                applicationInfo = ApplicationInfo().apply {
                    packageName = context.packageName
                    name = "Sandboxr"
                }
            }
        }

        shadowPm.addResolveInfoForIntent(app1, resolveInfo1)
        shadowPm.addResolveInfoForIntent(app1, resolveInfo2)
        shadowPm.addResolveInfoForIntent(app1, selfResolveInfo)

        val apps = loader.loadAllSystemApps()
        assertNotNull(apps)
        // Verify self package is filtered out
        assertFalse(apps.any { it.packageName == context.packageName })
        // Verify installed apps are returned
        assertTrue(apps.any { it.packageName == "com.android.chrome" })
        assertTrue(apps.any { it.packageName == "org.mozilla.firefox" })
    }

    @Test
    fun testDefaultLauncherIntentCreation() {
        val intent = loader.createDefaultLauncherIntent()
        assertNotNull(intent)
    }

    @Test
    fun testPackageListenerRegistrationAndUnregistration() {
        var callbackFired = false
        loader.registerPackageListener {
            callbackFired = true
        }
        loader.unregisterPackageListener()
        assertFalse(callbackFired)
    }
}
