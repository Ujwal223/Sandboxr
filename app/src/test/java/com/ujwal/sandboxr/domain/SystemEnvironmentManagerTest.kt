package com.ujwal.sandboxr.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.model.InstalledPackage
import com.sandboxr.virtual.server.pm.VPackageManagerService
import org.junit.Assert.assertEquals
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SystemEnvironmentManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SystemEnvironmentManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        manager = SystemEnvironmentManager(virtualCoreProvider = { null })
    }

    @Test
    fun testSystemEnvironmentIdentification() {
        val systemEnv = EnvironmentEntity.createSystemEnvironment()
        val customSystemEnv = EnvironmentEntity(
            displayName = "Host System",
            colorTag = 0xFF000000L,
            isSystem = true
        )
        val virtualEnv = EnvironmentEntity(
            displayName = "Work Vault",
            colorTag = 0xFF4A90E2L,
            isSystem = false
        )

        assertTrue(manager.isSystemEnvironment(systemEnv))
        assertTrue(manager.isSystemEnvironment(customSystemEnv))
        assertTrue(manager.isSystemEnvironment(EnvironmentEntity.SYSTEM_ENV_ID))
        assertFalse(manager.isSystemEnvironment(virtualEnv))
        assertFalse(manager.isSystemEnvironment("some-random-uuid"))
    }

    @Test
    fun testLaunchAppFromSystemEnvironmentExecutesStandardIntentWithoutVClient() {
        val systemEnv = EnvironmentEntity.createSystemEnvironment()
        val targetPkg = "com.android.calculator2"

        // Register package in Robolectric's host PackageManager
        val pm = context.packageManager
        val shadowPm = shadowOf(pm)

        val queryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(targetPkg)
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = targetPkg
                name = "$targetPkg.CalculatorActivity"
                applicationInfo = ApplicationInfo().apply {
                    packageName = targetPkg
                    flags = ApplicationInfo.FLAG_INSTALLED
                }
            }
        }
        shadowPm.addResolveInfoForIntent(queryIntent, resolveInfo)

        // Launch app
        val result = manager.launchApp(context, targetPkg, systemEnv)

        // Verify standard Android Intent was executed with SYSTEM_PASSTHROUGH
        assertTrue("Result must be Success", result is LaunchResult.Success)
        val success = result as LaunchResult.Success
        assertEquals(targetPkg, success.packageName)
        assertEquals(EnvironmentEntity.SYSTEM_ENV_ID, success.environmentId)
        assertEquals(LaunchMode.SYSTEM_PASSTHROUGH, success.mode)

        // Verify started intent in shadow Application
        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStarted = shadowApp.nextStartedActivity
        assertNotNull("Host OS Intent must be dispatched to startActivity()", nextStarted)
        assertEquals(targetPkg, nextStarted?.`package` ?: nextStarted?.component?.packageName)
        assertTrue(nextStarted!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun testLaunchAppFromVirtualEnvironmentUsesVirtualContainer() {
        val virtualEnv = EnvironmentEntity(
            id = "env-virtual-work",
            displayName = "Work Space",
            colorTag = 0xFF4A90E2L,
            isSystem = false
        )
        val guestPkg = "com.guest.messenger"
        val launchedApps = mutableListOf<String>()

        val managerWithVc = SystemEnvironmentManager(
            virtualLauncher = { pkg, env ->
                launchedApps.add(pkg)
                pkg == guestPkg && env == virtualEnv.id
            }
        )

        val result = managerWithVc.launchApp(context, guestPkg, virtualEnv)
        assertTrue("Virtual launch must succeed via virtual container", result is LaunchResult.Success)
        val success = result as LaunchResult.Success
        assertEquals(LaunchMode.VIRTUAL_CONTAINER, success.mode)
        assertEquals(virtualEnv.id, success.environmentId)
        assertTrue(launchedApps.contains(guestPkg))
    }

    @Test
    fun testPackageNotFoundReturnsCleanFailure() {
        val systemEnv = EnvironmentEntity.createSystemEnvironment()
        val result = manager.launchApp(context, "com.nonexistent.app", systemEnv)

        assertTrue(result is LaunchResult.PackageNotFound)
        val notFound = result as LaunchResult.PackageNotFound
        assertEquals(LaunchMode.SYSTEM_PASSTHROUGH, notFound.mode)
        assertEquals("com.nonexistent.app", notFound.packageName)
    }
}
