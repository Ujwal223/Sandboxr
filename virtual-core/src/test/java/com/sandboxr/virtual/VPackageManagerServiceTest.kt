package com.sandboxr.virtual

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.sandboxr.virtual.client.hook.BinderHook
import com.sandboxr.virtual.client.hook.PackageManagerHook
import com.sandboxr.virtual.server.pm.VPackageManagerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Method

class VPackageManagerServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var vpm: VPackageManagerService

    @Before
    fun setup() {
        val filesDir = tempFolder.newFolder("test_mock_files")
        val dummyContext = object : android.content.ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = filesDir
        }
        mockContext = dummyContext
        vpm = VPackageManagerService.get(dummyContext)
    }

    @Test
    fun testInstallAndQueryPackageMetadata() {
        val envId = "env-alpha-123"
        val guestPkg = "com.guest.testapp"

        val appInfo = ApplicationInfo().apply {
            packageName = guestPkg
            sourceDir = "/data/data/com.ujwal.sandboxr/envs/$envId/guest.apk"
            flags = ApplicationInfo.FLAG_INSTALLED
        }

        val activityInfo = ActivityInfo().apply {
            packageName = guestPkg
            name = "com.guest.testapp.MainActivity"
            exported = true
        }

        val packageInfo = PackageInfo().apply {
            packageName = guestPkg
            versionName = "2.5.0"
            applicationInfo = appInfo
            activities = arrayOf(activityInfo)
        }

        // Register package in VPackageManagerService
        val installedPkg = vpm.registerPackage(packageInfo, envId, File(appInfo.sourceDir))
        assertEquals(guestPkg, installedPkg.packageName)
        assertEquals("2.5.0", installedPkg.versionName)

        // Query getPackageInfo
        val queriedPkgInfo = vpm.getPackageInfo(guestPkg, 0, envId)
        assertNotNull("PackageInfo must be returned for installed guest package", queriedPkgInfo)
        assertEquals(guestPkg, queriedPkgInfo?.packageName)
        assertEquals("2.5.0", queriedPkgInfo?.versionName)

        // Query getApplicationInfo
        val queriedAppInfo = vpm.getApplicationInfo(guestPkg, 0, envId)
        assertNotNull("ApplicationInfo must be returned", queriedAppInfo)
        assertEquals(guestPkg, queriedAppInfo?.packageName)
        assertEquals("/data/data/com.ujwal.sandboxr/envs/$envId/guest.apk", queriedAppInfo?.sourceDir)

        // Query getInstalledPackages
        val allPkgs = vpm.getInstalledPackages(0, envId)
        assertTrue("Installed packages list must contain the guest package", allPkgs.any { it.packageName == guestPkg })

        // Check environment isolation: another env must NOT see this package
        val otherEnvPkgs = vpm.getInstalledPackages(0, "other-env-456")
        assertFalse("Other environment must not contain package from env-alpha-123", otherEnvPkgs.any { it.packageName == guestPkg })
        assertNull("Other env must return null for getPackageInfo", vpm.getPackageInfo(guestPkg, 0, "other-env-456"))

        // Resolve Activity
        val resolved = vpm.resolveActivity(guestPkg, "com.guest.testapp.MainActivity", envId)
        assertNotNull("Resolved activity must not be null", resolved)
        assertEquals("com.guest.testapp.MainActivity", resolved?.name)
    }

    @Test
    fun testPackageManagerHookInterception() {
        val envId = "env-hook-test"
        val guestPkg = "com.hooked.app"

        val appInfo = ApplicationInfo().apply {
            packageName = guestPkg
            sourceDir = "/fake/path/app.apk"
        }
        val packageInfo = PackageInfo().apply {
            packageName = guestPkg
            versionName = "3.0.1"
            applicationInfo = appInfo
        }

        vpm.registerPackage(packageInfo, envId)

        // Initialize VirtualCore with current environment
        VirtualCore.init(mockContext)
        val vCore = VirtualCore.get()
        vCore.createEnvironment(envId, "Hook Test", 0L)
        vCore.switchEnvironment(envId)

        val pmHook = PackageManagerHook(mockContext)

        // Simulate IPackageManager.getPackageInfo call
        val getPackageInfoMethod = Any::class.java.methods.first { it.name == "toString" } // dummy method signature
        val fakeMethod = object : Any() {
            fun getPackageInfo(packageName: String, flags: Long, userId: Int): PackageInfo? = null
        }.javaClass.methods.first { it.name == "getPackageInfo" }

        val result = pmHook.onIntercept(fakeMethod, arrayOf<Any>(guestPkg, 0L, 0))
        assertNotNull("PackageManagerHook must intercept getPackageInfo", result)
        val interceptedPkg = result?.value as? PackageInfo
        assertEquals(guestPkg, interceptedPkg?.packageName)
        assertEquals("3.0.1", interceptedPkg?.versionName)

        // Uninstall package
        val uninstalled = vpm.uninstallPackage(guestPkg, envId)
        assertTrue("Package uninstall must succeed", uninstalled)
        assertFalse(vpm.isPackageInstalled(guestPkg, envId))
    }
}
