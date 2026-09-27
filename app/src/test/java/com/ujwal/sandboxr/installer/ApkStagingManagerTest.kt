package com.ujwal.sandboxr.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApkStagingManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        val stagingDir = ApkStagingManager.getStagingDirectory(context)
        stagingDir.listFiles()?.forEach { it.delete() }
    }

    private fun createDummyApkFile(name: String = "test.apk", content: String = "APK_DUMMY_BINARY_DATA"): File {
        val file = tempFolder.newFile(name)
        // Write as valid zip file since APKs are zip archives
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            val entry = ZipEntry("classes.dex")
            zos.putNextEntry(entry)
            zos.write(content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return file
    }

    @Test
    fun testStageApkImmediateByteCopy() {
        val dummyApk = createDummyApkFile("sample_app.apk", "TEST_RAW_BYTECODE_STREAM")
        val uri = Uri.fromFile(dummyApk)

        val result = ApkStagingManager.stageApkSync(context, uri)

        assertTrue("Expected StagedApkResult.Success but got $result", result is StagedApkResult.Success)
        val info = (result as StagedApkResult.Success).info

        assertNotNull(info.stagingId)
        assertTrue(info.stagedFile.exists())
        assertEquals(dummyApk.length(), info.fileSizeBytes)
        assertTrue(info.stagedFile.name.endsWith(".apk"))
        assertEquals(ApkStagingManager.getStagingDirectory(context).absolutePath, info.stagedFile.parent)
    }

    @Test
    fun testEmptyApkRejected() {
        val emptyFile = tempFolder.newFile("empty.apk")
        val uri = Uri.fromFile(emptyFile)

        val result = ApkStagingManager.stageApkSync(context, uri)

        assertTrue(result is StagedApkResult.Error)
        val stagingFiles = ApkStagingManager.getStagingDirectory(context).listFiles() ?: emptyArray()
        assertEquals(0, stagingFiles.size)
    }

    @Test
    fun testInvalidUriReturnsErrorGracefully() {
        val nonExistentUri = Uri.parse("file:///non/existent/path/app.apk")
        val result = ApkStagingManager.stageApkSync(context, nonExistentUri)

        assertTrue(result is StagedApkResult.Error)
    }

    @Test
    fun testPurgeOrphanedStagingFiles() {
        val stagingDir = ApkStagingManager.getStagingDirectory(context)

        // 1. Create a fresh file (modified now)
        val freshFile = File(stagingDir, "fresh.apk").apply {
            writeText("FRESH_CONTENT")
            setLastModified(System.currentTimeMillis())
        }

        // 2. Create an orphaned file (modified 48 hours ago)
        val oldFile = File(stagingDir, "orphaned.apk").apply {
            writeText("ORPHANED_CONTENT")
            setLastModified(System.currentTimeMillis() - (48 * 60 * 60 * 1000L))
        }

        assertEquals(2, stagingDir.listFiles()?.size)

        // Purge files older than 24 hours
        val purged = ApkStagingManager.purgeOrphanedStagingFiles(context, maxAgeMillis = 24 * 60 * 60 * 1000L)

        assertEquals(1, purged)
        assertTrue(freshFile.exists())
        assertFalse(oldFile.exists())
    }

    @Test
    fun testInstallerActivityReceivesIntentAndStages() {
        val dummyApk = createDummyApkFile("target_app.apk", "TEST_APK_BYTES")
        val uri = Uri.fromFile(dummyApk)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val controller = Robolectric.buildActivity(InstallerActivity::class.java, intent)
        controller.create()
        val activity = controller.get()

        assertNotNull(activity)
        // Activity state must not be in initial error
        assertFalse(activity.uiState is InstallerUiState.Error)
    }
}
