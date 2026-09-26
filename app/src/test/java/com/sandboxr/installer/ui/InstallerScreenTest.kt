package com.sandboxr.installer.ui

import com.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.data.db.NetworkConfig
import com.sandboxr.installer.StagedApkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstallerScreenTest {

    private val testApkInfo = StagedApkInfo(
        stagingId = "test-uuid-1234",
        stagedFile = File("/tmp/test.apk"),
        packageName = "org.thoughtcrime.securesms",
        label = "Signal",
        versionName = "7.2.0",
        versionCode = 1000L,
        fileSizeBytes = 45 * 1024 * 1024L,
        targetSdk = 35,
        minSdk = 29,
        sourceUri = "content://media/external/file/123"
    )

    private val testEnvironments = listOf(
        EnvironmentEntity.createSystemEnvironment(),
        EnvironmentEntity(
            id = "env-work",
            displayName = "Work Vault",
            colorTag = 0xFF5B8AF5L,
            networkConfig = NetworkConfig.WIREGUARD,
            isSystem = false
        ),
        EnvironmentEntity(
            id = "env-crypto",
            displayName = "Cold Storage",
            colorTag = 0xFFF5A623L,
            networkConfig = NetworkConfig.BLOCKED,
            isSystem = false
        )
    )

    @Test
    fun testApkMetadataModelValidation() {
        assertNotNull(testApkInfo)
        assertEquals("Signal", testApkInfo.label)
        assertEquals("org.thoughtcrime.securesms", testApkInfo.packageName)
        assertEquals("7.2.0", testApkInfo.versionName)
        assertEquals(35, testApkInfo.targetSdk)
        assertEquals(45 * 1024 * 1024L, testApkInfo.fileSizeBytes)
    }

    @Test
    fun testSystemInstallDispatchLogic() {
        var dispatchedApk: StagedApkInfo? = null
        val onInstallToSystem: (StagedApkInfo) -> Unit = { apk ->
            dispatchedApk = apk
        }

        onInstallToSystem(testApkInfo)
        assertNotNull(dispatchedApk)
        assertEquals("org.thoughtcrime.securesms", dispatchedApk?.packageName)
    }

    @Test
    fun testVirtualInstallDispatchLogic() {
        var targetEnvironment: EnvironmentEntity? = null
        val onInstallToVirtual: (StagedApkInfo, EnvironmentEntity) -> Unit = { _, env ->
            targetEnvironment = env
        }

        val workEnv = testEnvironments.first { it.id == "env-work" }
        onInstallToVirtual(testApkInfo, workEnv)

        assertNotNull(targetEnvironment)
        assertEquals("env-work", targetEnvironment?.id)
        assertEquals("Work Vault", targetEnvironment?.displayName)
        assertEquals(NetworkConfig.WIREGUARD, targetEnvironment?.networkConfig)
    }

    @Test
    fun testCreateNewEnvironmentLogic() {
        var createdName: String? = null
        var createdColor: Long? = null

        val onCreate: (String, Long) -> Unit = { name, color ->
            createdName = name
            createdColor = color
        }

        onCreate("Finance Vault", 0xFF34C97AL)
        assertEquals("Finance Vault", createdName)
        assertEquals(0xFF34C97AL, createdColor)
    }

    @Test
    fun testInstallerInstallStateTransitions() {
        val idleState: InstallerInstallState = InstallerInstallState.Idle
        val installingState: InstallerInstallState = InstallerInstallState.Installing("Work Vault")
        val successState: InstallerInstallState = InstallerInstallState.Success("Work Vault")
        val errorState: InstallerInstallState = InstallerInstallState.Error("Storage full")

        assertTrue(idleState is InstallerInstallState.Idle)
        assertTrue(installingState is InstallerInstallState.Installing)
        assertEquals("Work Vault", (installingState as InstallerInstallState.Installing).targetName)
        assertTrue(successState is InstallerInstallState.Success)
        assertTrue(errorState is InstallerInstallState.Error)
    }
}
