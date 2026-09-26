package com.sandboxr

import com.sandboxr.data.db.SandboxrDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = SandboxrApplication::class)
class CrashRecoverySentinelTest {

    private lateinit var app: SandboxrApplication

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as SandboxrApplication
    }

    @Test
    fun testNormalStartupWithoutSentinel() {
        // Clear any existing sentinel
        app.clearSessionSentinel()
        val sentinelFile = app.getSessionSentinelFile()
        assertFalse("Sentinel should be cleared", sentinelFile.exists())

        // Normal pipeline should not report crash
        val crashDetected = app.performCrashRecoveryPipeline()
        assertFalse("Normal startup should not report crash", crashDetected)

        // Fresh sentinel should now be written
        assertTrue("Active session sentinel should be written after startup", sentinelFile.exists())
    }

    @Test
    fun testAbnormalShutdownCrashSentinelDetectionAndRecovery() {
        val sentinelFile = app.getSessionSentinelFile()

        // Simulate previous crash by writing dangling sentinel
        sentinelFile.writeText("PID=99999|STARTED=100000|TIME=SimulatedCrash")
        assertTrue("Simulated dangling sentinel must exist", sentinelFile.exists())

        // Run recovery pipeline
        val crashDetected = app.performCrashRecoveryPipeline()
        assertTrue("Recovery pipeline must detect abnormal shutdown", crashDetected)

        // Session sentinel must now contain current process info
        assertTrue("New active session sentinel must be written", sentinelFile.exists())
        val newContent = sentinelFile.readText()
        assertTrue("New sentinel must contain current process info", newContent.contains("PID="))
    }

    @Test
    fun testSqliteDatabaseIntegrityCheckAndRepair() {
        // Ensure database exists
        val db = SandboxrDatabase.getInstance(app)
        val envDao = db.environmentDao()
        assertNotNull(envDao)

        // Run repairSqliteDatabaseIfCorrupted
        val repairSuccess = app.repairSqliteDatabaseIfCorrupted()
        assertTrue("SQLite integrity check and repair should succeed on valid database", repairSuccess)
    }

    @Test
    fun testStagingFileCleanupOnCrashRecovery() {
        val stagingDir = com.sandboxr.installer.ApkStagingManager.getStagingDirectory(app)
        if (!stagingDir.exists()) stagingDir.mkdirs()

        // Create a simulated abandoned staging file with timestamp in the past
        val abandonedApk = File(stagingDir, "abandoned_crash_test.apk")
        abandonedApk.writeText("simulated apk content")
        abandonedApk.setLastModified(System.currentTimeMillis() - 5000L)
        assertTrue("Abandoned APK must exist before recovery", abandonedApk.exists())

        // Simulate crash sentinel
        val sentinelFile = app.getSessionSentinelFile()
        sentinelFile.writeText("PID=88888|CRASH")

        // Run recovery
        app.performCrashRecoveryPipeline()

        // Abandoned file should be purged
        assertFalse("Orphaned staging file must be purged during crash recovery", abandonedApk.exists())
    }
}
