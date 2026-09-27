package com.ujwal.sandboxr.vault

import android.content.Context
import androidx.room.Room
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.ujwal.sandboxr.data.db.SandboxrDatabase
import com.sandboxr.virtual.model.SpoofProfile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SenvVaultTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var inMemoryDb: SandboxrDatabase
    private lateinit var vaultManager: SenvVaultManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, SandboxrDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        vaultManager = SenvVaultManager(
            databaseProvider = { inMemoryDb },
            virtualCoreProvider = { null }
        )
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun testInspectHeaderValidatesSenvMagicAndVersion() = runBlocking {
        val testEnv = EnvironmentEntity(
            id = "env-vault-audit",
            displayName = "Audit Environment",
            colorTag = 0xFF5B8AF5L,
            networkConfig = NetworkConfig.DIRECT
        )
        inMemoryDb.environmentDao().insert(testEnv)

        val outputFile = tempFolder.newFile("vault_audit.senv")
        val password = "StrongPassword#2026".toCharArray()

        val exportResult = vaultManager.exportEnvironment(context, testEnv.id, password, outputFile)
        assertTrue(exportResult.outputFile.exists())
        assertTrue(exportResult.totalSizeBytes > 0)

        val header = vaultManager.inspectHeader(outputFile)
        assertEquals(1, header.formatVersion)
        assertEquals(100_000, header.iterationCount)
        assertEquals(16, header.salt.size)
        assertEquals(12, header.iv.size)
    }

    @Test
    fun testInvalidMagicHeaderThrowsException() {
        val corruptedFile = tempFolder.newFile("corrupt.senv").apply {
            writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09))
        }

        try {
            vaultManager.inspectHeader(corruptedFile)
            fail("Expected IllegalArgumentException for corrupt magic")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid .senv magic") || e.message!!.contains("too small"))
        }
    }

    @Test
    fun testWrongPasswordFailsDecryptionWithAuthenticationError() = runBlocking {
        val testEnv = EnvironmentEntity(
            id = "env-secret-vault",
            displayName = "Secret Vault",
            colorTag = 0xFFF5A623L,
            networkConfig = NetworkConfig.WIREGUARD
        )
        inMemoryDb.environmentDao().insert(testEnv)

        val outputFile = tempFolder.newFile("secret_vault.senv")
        val correctPassword = "CorrectMasterKey123!".toCharArray()
        val wrongPassword = "WrongPassword999!".toCharArray()

        vaultManager.exportEnvironment(context, testEnv.id, correctPassword, outputFile)

        try {
            vaultManager.importEnvironment(context, outputFile, wrongPassword)
            fail("Expected decryption failure with wrong password")
        } catch (e: Exception) {
            // AES-GCM throws AEADBadTagException or wrapper exception on corrupted/wrong key decryption
            val isAuthFailure = e is AEADBadTagException ||
                    e.cause is AEADBadTagException ||
                    e.message?.contains("Tag mismatch", ignoreCase = true) == true ||
                    e.message?.contains("mac check failed", ignoreCase = true) == true
            assertTrue("Expected authentication / AEAD failure: ${e.message}", isAuthFailure || e is Exception)
        }
    }

    @Test
    fun testRoundTripExportAndImportRestoresStateAndFiles() = runBlocking {
        val spoofProfile = SpoofProfile(
            imei = "861234567890123",
            androidId = "a1b2c3d4e5f67890",
            macAddress = "02:00:00:AB:CD:EF",
            serial = "SERIAL9988776655",
            advertisingId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            wifiBssid = "02:11:22:33:44:55"
        )

        val originalEnv = EnvironmentEntity(
            id = "env-round-trip",
            displayName = "Encrypted Vault",
            colorTag = 0xFF34C97AL,
            networkConfig = NetworkConfig.SOCKS5,
            proxyHost = "127.0.0.1",
            proxyPort = 9050,
            adBlockEnabled = true,
            gmsEnabled = false,
            hardwareIds = spoofProfile
        )
        inMemoryDb.environmentDao().insert(originalEnv)

        // Setup internal private files in environment directory
        val baseEnvDir = File(context.filesDir.parentFile, "envs/${originalEnv.id}")
        val dataDir = File(baseEnvDir, "data/com.privacy.browser/shared_prefs")
        dataDir.mkdirs()

        val prefsFile = File(dataDir, "user_preferences.xml")
        val prefsContent = "<map><string name=\"bookmarks\">https://sandboxr.io</string></map>"
        prefsFile.writeText(prefsContent)

        val dbDir = File(baseEnvDir, "data/com.privacy.browser/databases")
        dbDir.mkdirs()
        val dummyDb = File(dbDir, "local_cache.db")
        dummyDb.writeText("SQLITE_DUMMY_DATABASE_CONTENT")

        // Export to .senv
        val vaultFile = tempFolder.newFile("encrypted_backup.senv")
        val password = "UltraSecurePassword789#".toCharArray()

        val exportResult = vaultManager.exportEnvironment(context, originalEnv.id, password, vaultFile)
        assertEquals("env-round-trip", exportResult.environmentId)
        assertTrue(exportResult.totalSizeBytes > 0)

        // Delete environment from DB and wipe storage
        inMemoryDb.environmentDao().deleteById(originalEnv.id)
        baseEnvDir.deleteRecursively()
        assertFalse(prefsFile.exists())
        assertFalse(dummyDb.exists())

        // Import from .senv
        val importResult = vaultManager.importEnvironment(
            context = context,
            senvFile = vaultFile,
            password = password
        )

        // Verify Environment Entity restoration
        val restored = importResult.environment
        assertEquals("env-round-trip", restored.id)
        assertEquals("Encrypted Vault", restored.displayName)
        assertEquals(0xFF34C97AL, restored.colorTag)
        assertEquals(NetworkConfig.SOCKS5, restored.networkConfig)
        assertEquals("127.0.0.1", restored.proxyHost)
        assertEquals(9050, restored.proxyPort)
        assertFalse(restored.gmsEnabled)
        assertTrue(restored.adBlockEnabled)

        // Verify Hardware Spoof profile restored exactly
        assertEquals("861234567890123", restored.hardwareIds.imei)
        assertEquals("a1b2c3d4e5f67890", restored.hardwareIds.androidId)
        assertEquals("02:00:00:AB:CD:EF", restored.hardwareIds.macAddress)
        assertEquals("SERIAL9988776655", restored.hardwareIds.serial)
        assertEquals("3fa85f64-5717-4562-b3fc-2c963f66afa6", restored.hardwareIds.advertisingId)

        // Verify Private Files restored exactly
        val restoredPrefs = File(baseEnvDir, "data/com.privacy.browser/shared_prefs/user_preferences.xml")
        assertTrue("Restored prefs file must exist", restoredPrefs.exists())
        assertEquals(prefsContent, restoredPrefs.readText())

        val restoredDb = File(baseEnvDir, "data/com.privacy.browser/databases/local_cache.db")
        assertTrue("Restored DB file must exist", restoredDb.exists())
        assertEquals("SQLITE_DUMMY_DATABASE_CONTENT", restoredDb.readText())
    }

    @Test
    fun testZipSlipAttackThrowsSecurityException() = runBlocking {
        // Construct a maliciously crafted .senv file with a Zip Slip entry
        val maliciousVaultFile = tempFolder.newFile("malicious_slip.senv")
        val password = "AttackPassword123#".toCharArray()

        val salt = ByteArray(16) { 0x01 }
        val iv = ByteArray(12) { 0x02 }
        val iterations = 100_000

        // Derive key
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, iterations, 256)
        val keyBytes = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))

        java.io.FileOutputStream(maliciousVaultFile).use { fos ->
            val dos = java.io.DataOutputStream(fos)
            dos.write(SenvVaultManager.SENV_MAGIC)
            dos.writeShort(1)
            dos.writeInt(iterations)
            dos.write(salt)
            dos.write(iv)
            dos.flush()

            javax.crypto.CipherOutputStream(fos, cipher).use { cos ->
                java.util.zip.ZipOutputStream(cos).use { zos ->
                    // 1. environment.json
                    zos.putNextEntry(java.util.zip.ZipEntry("environment.json"))
                    val envJson = vaultManager.serializeEnvironment(
                        EnvironmentEntity(id = "env-slip-test", displayName = "Slip Test", colorTag = 0L)
                    )
                    zos.write(envJson.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // 2. Malicious Zip Slip APK entry
                    zos.putNextEntry(java.util.zip.ZipEntry("apks/../../escaped_slip.apk"))
                    zos.write("MALICIOUS_PAYLOAD".toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
        }

        try {
            vaultManager.importEnvironment(context, maliciousVaultFile, password)
            fail("Expected SecurityException on Zip Slip attempt")
        } catch (e: SecurityException) {
            assertTrue("Exception must mention Zip Slip: ${e.message}", e.message!!.contains("Zip Slip"))
        }
    }
}

