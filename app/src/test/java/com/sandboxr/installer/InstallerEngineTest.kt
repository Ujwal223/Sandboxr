package com.sandboxr.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageInfo
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.model.InstalledPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.content.pm.ProviderInfo
import android.os.Bundle
import androidx.core.content.FileProvider
import com.sandboxr.app.R
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], packageName = "com.sandboxr.app")
class InstallerEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val providerInfo = ProviderInfo().apply {
            authority = "com.sandboxr.app.fileprovider"
            grantUriPermissions = true
            metaData = Bundle().apply {
                putInt("android.support.FILE_PROVIDER_PATHS", R.xml.file_paths)
            }
        }
        Robolectric.buildContentProvider(FileProvider::class.java).create(providerInfo)

        val stagingDir = ApkStagingManager.getStagingDirectory(context)
        stagingDir.listFiles()?.forEach { it.delete() }
    }

    private fun createStagedApk(name: String = "sample.apk"): StagedApkInfo {
        val stagingDir = ApkStagingManager.getStagingDirectory(context)
        val file = File(stagingDir, name).apply {
            writeText("DUMMY_APK_CONTENT")
        }
        return StagedApkInfo(
            stagingId = file.nameWithoutExtension,
            stagedFile = file,
            packageName = "com.example.testapp",
            label = "Test App",
            versionName = "1.0.0",
            versionCode = 1L,
            fileSizeBytes = file.length(),
            targetSdk = 35,
            minSdk = 29,
            sourceUri = "content://dummy/test.apk"
        )
    }

    @Test
    fun testSystemInstallHandoffDispatchesIntent() {
        val engine = InstallerEngine(
            fileUriProvider = { _, auth, file ->
                Uri.parse("content://$auth/staging/${file.name}")
            }
        )
        val stagedApk = createStagedApk("system_target.apk")

        val result = engine.handoffToSystemInstaller(context, stagedApk)
        assertTrue("Expected Success but got: $result", result is InstallResult.Success)
        val startedIntent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals("content", startedIntent.data?.scheme)
        assertEquals("application/vnd.android.package-archive", startedIntent.type)
        assertTrue(startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun testSystemInstallHandoffFailsGracefullyWhenUriGenerationFails() {
        val engine = InstallerEngine(
            fileUriProvider = { _, _, _ ->
                throw IllegalArgumentException("Simulated provider failure")
            }
        )
        val stagedApk = createStagedApk("failing_target.apk")

        val result = engine.handoffToSystemInstaller(context, stagedApk)
        assertTrue(result is InstallResult.Error)
        assertEquals("Failed to generate secure FileProvider URI: Simulated provider failure", (result as InstallResult.Error).message)
        val startedIntent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        org.junit.Assert.assertNull(startedIntent)
    }

    @Test
    fun testVirtualInstallDeletesStagingFileOnCompletion() {
        val stagedApk = createStagedApk("virtual_target.apk")
        assertTrue(stagedApk.stagedFile.exists())

        // Engine with uninitialized VirtualCore still enforces staging cleanup in finally
        val engine = InstallerEngine(virtualCoreProvider = { null })
        val destination = InstallDestination.Virtual("env-vault-1", "Private Vault")

        val result = engine.installToVirtualEnvironment(context, stagedApk, destination)

        // Result is Error due to missing VirtualCore, but staging file MUST be purged
        assertTrue(result is InstallResult.Error)
        assertFalse("Staging file must be deleted upon installation completion", stagedApk.stagedFile.exists())
    }

    private fun engineCleanupVerification(file: File): Boolean {
        return ApkStagingManager.deleteStagedFile(file)
    }

    @Test
    fun testOrphanedStagingCleanupOnAppLaunch() {
        val stagingDir = ApkStagingManager.getStagingDirectory(context)

        // 1. Fresh file
        val fresh = File(stagingDir, "fresh.apk").apply {
            writeText("FRESH")
            setLastModified(System.currentTimeMillis())
        }

        // 2. Orphaned file older than 2 hours
        val orphan = File(stagingDir, "stale_orphan.apk").apply {
            writeText("ORPHAN")
            setLastModified(System.currentTimeMillis() - (3 * 60 * 60 * 1000L))
        }

        assertEquals(2, stagingDir.listFiles()?.size)

        val purged = InstallerEngine.cleanupOrphanedStagingFiles(context, maxAgeMillis = 60 * 60 * 1000L)
        assertEquals(1, purged)
        assertTrue(fresh.exists())
        assertFalse(orphan.exists())
    }
}
