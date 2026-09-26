package com.sandboxr.virtual

import com.sandboxr.virtual.server.fs.StorageRedirector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageRedirectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var internalRoot: File
    private lateinit var externalRoot: File
    private lateinit var redirector: StorageRedirector

    @Before
    fun setUp() {
        internalRoot = tempFolder.newFolder("internal_storage")
        externalRoot = tempFolder.newFolder("external_storage")
        redirector = StorageRedirector.createForTesting(internalRoot, externalRoot)
    }

    @Test
    fun testTwoEnvironmentsWritingSameRelativePathHaveZeroCollision() {
        val envAlpha = "env-uuid-alpha"
        val envBeta = "env-uuid-beta"
        val packageName = "com.privacy.messenger"
        val relativePath = "databases/messages.db"

        // Env Alpha resolves and writes data
        val alphaRedirectedPath = redirector.redirectPath("/data/data/$packageName/$relativePath", envAlpha, packageName)
        val alphaFile = File(alphaRedirectedPath).apply {
            parentFile?.mkdirs()
            writeText("ENCRYPTED_MESSAGES_ALPHA_12345")
        }

        // Env Beta resolves and writes different data to the exact same guest path
        val betaRedirectedPath = redirector.redirectPath("/data/data/$packageName/$relativePath", envBeta, packageName)
        val betaFile = File(betaRedirectedPath).apply {
            parentFile?.mkdirs()
            writeText("ENCRYPTED_MESSAGES_BETA_67890")
        }

        // 1. Host filepaths must be distinct
        assertNotEquals(alphaFile.absolutePath, betaFile.absolutePath)
        assertTrue(alphaFile.absolutePath.contains("/envs/$envAlpha/data/$packageName/databases/messages.db"))
        assertTrue(betaFile.absolutePath.contains("/envs/$envBeta/data/$packageName/databases/messages.db"))

        // 2. Data contents must remain isolated with zero collision
        assertEquals("ENCRYPTED_MESSAGES_ALPHA_12345", alphaFile.readText())
        assertEquals("ENCRYPTED_MESSAGES_BETA_67890", betaFile.readText())

        // 3. Modifying Alpha does not affect Beta
        alphaFile.writeText("ALPHA_MODIFIED")
        assertEquals("ALPHA_MODIFIED", alphaFile.readText())
        assertEquals("ENCRYPTED_MESSAGES_BETA_67890", betaFile.readText())
    }

    @Test
    fun testExternalStorageRedirection() {
        val envId = "env-uuid-external"
        val pkg = "com.camera.app"
        val originalGuestPath = "/sdcard/Android/data/$pkg/files/dcim.jpg"

        val redirected = redirector.redirectPath(originalGuestPath, envId, pkg)
        assertTrue("Redirected path must point to external env root", redirected.contains("/envs/$envId/$pkg/files/dcim.jpg"))

        val file = File(redirected).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        }
        assertTrue(file.exists())
        assertEquals(4, file.length())

        // Reverse mapping check
        val guestPerspective = redirector.toGuestPath(redirected, envId, pkg)
        assertEquals("/sdcard/Android/data/$pkg/files/dcim.jpg", guestPerspective)
    }

    @Test
    fun testReversePathTranslationPreventsContainerLeak() {
        val envId = "env-uuid-stealth"
        val pkg = "com.target.guest"
        val internalFile = redirector.redirectPath("/data/data/$pkg/shared_prefs/prefs.xml", envId, pkg)

        // Guest app inquiring path must see /data/data/..., never com.sandboxr/envs/...
        val guestPath = redirector.toGuestPath(internalFile, envId, pkg)
        assertEquals("/data/data/$pkg/shared_prefs/prefs.xml", guestPath)
        assertFalse(guestPath.contains("com.sandboxr"))
        assertFalse(guestPath.contains("envs"))
    }

    @Test
    fun testEnvironmentStorageCalculationAndWipe() {
        val envId = "env-uuid-wipe-test"
        val pkg1 = "com.app.one"
        val pkg2 = "com.app.two"

        val f1 = File(redirector.redirectPath("/data/data/$pkg1/files/data1.bin", envId, pkg1)).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(1024)) // 1 KB
        }
        val f2 = File(redirector.redirectPath("/data/data/$pkg2/files/data2.bin", envId, pkg2)).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(2048)) // 2 KB
        }

        val totalSize = redirector.calculateEnvironmentStorageSize(envId)
        assertEquals(3072L, totalSize)

        val pkg1Size = redirector.calculatePackageStorageSize(envId, pkg1)
        assertEquals(1024L, pkg1Size)

        // Wipe environment
        val wiped = redirector.wipeEnvironmentStorage(envId)
        assertTrue(wiped)
        assertFalse(f1.exists())
        assertFalse(f2.exists())
        assertEquals(0L, redirector.calculateEnvironmentStorageSize(envId))
    }

    @Test
    fun testSymlinkEscapeAttemptIsBlocked() {
        val envId = "env-symlink-test"
        val pkg = "com.attacker.app"

        // Create a fake target host file (e.g. host databases)
        val hostDb = File(internalRoot, "databases/sandboxr.db").apply {
            parentFile?.mkdirs()
            writeText("HOST_CRITICAL_DATABASE_SECRET")
        }

        // Create a symlink inside the environment pointing out to host databases
        val pkgFilesDir = File(internalRoot, "envs/$envId/data/$pkg/files").apply { mkdirs() }
        val symlinkFile = File(pkgFilesDir, "escape_link")

        try {
            java.nio.file.Files.createSymbolicLink(symlinkFile.toPath(), hostDb.toPath())
        } catch (_: Exception) {
            // If OS/filesystem doesn't support symlinks in test, return gracefully
            return
        }

        // Accessing through the symlink must be blocked by validateSandboxContainment
        try {
            redirector.redirectPath("/data/data/$pkg/files/escape_link", envId, pkg)
            org.junit.Assert.fail("Expected SecurityException for symlink escape")
        } catch (e: SecurityException) {
            assertTrue("SecurityException must explain path traversal / escape: ${e.message}",
                e.message?.contains("Path traversal blocked") == true || e.message?.contains("escapes sandbox") == true)
        }
    }

    @Test
    fun testDirectHostEscapeAttemptIsBlocked() {
        val envId = "env-direct-escape"
        val pkg = "com.attacker.app"

        // Accessing host database path directly
        val hostDbPath = File(internalRoot, "databases/sandboxr.db").absolutePath
        try {
            redirector.redirectPath(hostDbPath, envId, pkg)
            org.junit.Assert.fail("Expected SecurityException when accessing host path outside environment")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Path traversal blocked") == true)
        }
    }
}

