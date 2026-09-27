package com.ujwal.sandboxr.data.db

import android.content.Context
import androidx.room.Room
import com.ujwal.sandboxr.data.repository.EnvironmentRepository
import com.sandboxr.virtual.model.SpoofProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnvironmentDatabaseTest {

    private lateinit var db: SandboxrDatabase
    private lateinit var dao: EnvironmentDao

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, SandboxrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.environmentDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveEnvironment() = runTest {
        val profile = SpoofProfile.generate()
        val env = EnvironmentEntity(
            id = UUID.randomUUID().toString(),
            displayName = "Personal Privacy Vault",
            colorTag = 0xFF6200EEL,
            iconEmoji = "LOCK",
            sortOrder = 1,
            networkConfig = NetworkConfig.WIREGUARD,
            proxyHost = null,
            proxyPort = null,
            wgConfig = "[Interface]\nPrivateKey = xxx\nAddress = 10.0.0.2/32",
            dnsUpstream = "1.1.1.1",
            adBlockEnabled = true,
            clipboardMode = ClipboardMode.ISOLATED,
            gmsEnabled = false,
            hardwareIds = profile,
            freezeWhenInactive = true,
            notifIsolation = true,
            isSystem = false
        )

        dao.insert(env)

        val retrieved = dao.getEnvironmentById(env.id)
        assertNotNull(retrieved)
        assertEquals(env.id, retrieved?.id)
        assertEquals("Personal Privacy Vault", retrieved?.displayName)
        assertEquals(0xFF6200EEL, retrieved?.colorTag)
        assertEquals("LOCK", retrieved?.iconEmoji)
        assertEquals(1, retrieved?.sortOrder)
        assertEquals(NetworkConfig.WIREGUARD, retrieved?.networkConfig)
        assertEquals("[Interface]\nPrivateKey = xxx\nAddress = 10.0.0.2/32", retrieved?.wgConfig)
        assertEquals("1.1.1.1", retrieved?.dnsUpstream)
        assertTrue(retrieved?.adBlockEnabled == true)
        assertEquals(ClipboardMode.ISOLATED, retrieved?.clipboardMode)
        assertFalse(retrieved?.gmsEnabled == true)
        assertTrue(retrieved?.freezeWhenInactive == true)
        assertTrue(retrieved?.notifIsolation == true)
        assertFalse(retrieved?.isSystem == true)

        // Verify Hardware Identity spoofing table embedded correctly
        assertEquals(profile.imei, retrieved?.hardwareIds?.imei)
        assertEquals(profile.androidId, retrieved?.hardwareIds?.androidId)
        assertEquals(profile.macAddress, retrieved?.hardwareIds?.macAddress)
        assertEquals(profile.serial, retrieved?.hardwareIds?.serial)
        assertEquals(profile.advertisingId, retrieved?.hardwareIds?.advertisingId)
        assertEquals(profile.wifiBssid, retrieved?.hardwareIds?.wifiBssid)
        assertTrue(SpoofProfile.isValidImei(retrieved!!.hardwareIds.imei))
    }

    @Test
    fun testUpdateEnvironmentFields() = runTest {
        val env = EnvironmentEntity(
            displayName = "Original Vault",
            colorTag = 0xFF00FF00L,
            sortOrder = 2
        )
        dao.insert(env)

        dao.updateDisplayName(env.id, "Renamed Vault")
        dao.updateNetworkConfig(env.id, NetworkConfig.SOCKS5)
        dao.updateGmsEnabled(env.id, true)
        dao.updateSortOrder(env.id, 5)

        val updated = dao.getEnvironmentById(env.id)
        assertNotNull(updated)
        assertEquals("Renamed Vault", updated?.displayName)
        assertEquals(NetworkConfig.SOCKS5, updated?.networkConfig)
        assertTrue(updated?.gmsEnabled == true)
        assertEquals(5, updated?.sortOrder)
    }

    @Test
    fun testDeleteEnvironment() = runTest {
        val env = EnvironmentEntity(
            displayName = "Ephemeral Env",
            colorTag = 0xFFFF0000L
        )
        dao.insert(env)
        assertEquals(1, dao.count())

        val deletedRows = dao.deleteById(env.id)
        assertEquals(1, deletedRows)
        assertNull(dao.getEnvironmentById(env.id))
        assertEquals(0, dao.count())
    }

    @Test
    fun testEnvironmentOrderingSystemPinnedFirst() = runTest {
        val systemEnv = EnvironmentEntity.createSystemEnvironment()
        val envA = EnvironmentEntity(displayName = "Work", colorTag = 0xFF111111L, sortOrder = 3)
        val envB = EnvironmentEntity(displayName = "Finance", colorTag = 0xFF222222L, sortOrder = 1)
        val envC = EnvironmentEntity(displayName = "Gaming", colorTag = 0xFF333333L, sortOrder = 2)

        dao.insert(envA)
        dao.insert(envB)
        dao.insert(envC)
        dao.insert(systemEnv)

        val list = dao.getAllEnvironments()
        assertEquals(4, list.size)
        // System must always be first regardless of creation time or sortOrder
        assertTrue(list[0].isSystem)
        assertEquals(EnvironmentEntity.SYSTEM_ENV_ID, list[0].id)
        // Followed by sortOrder 1 (Finance), 2 (Gaming), 3 (Work)
        assertEquals("Finance", list[1].displayName)
        assertEquals("Gaming", list[2].displayName)
        assertEquals("Work", list[3].displayName)
    }

    @Test
    fun testStateFlowEmissionsOnRepositoryMutations() = runTest {
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)
        val repository = EnvironmentRepository(
            environmentDao = dao,
            scope = backgroundScope,
            ioDispatcher = testDispatcher
        )

        // Initial setup: ensure System environment exists
        repository.ensureSystemEnvironmentExists()

        // Wait until StateFlow emits the system environment
        val initialList = repository.environmentsState.first { it.isNotEmpty() }
        assertEquals(1, initialList.size)
        assertTrue(initialList[0].isSystem)

        // 1. Insert new environment and verify StateFlow receives emission
        val testEnv = EnvironmentEntity(
            displayName = "Social Media",
            colorTag = 0xFF00AAFFL,
            sortOrder = 1
        )
        repository.insertEnvironment(testEnv)

        // Wait until StateFlow emits 2 environments
        val updatedList = repository.environmentsState.first { it.size == 2 }
        assertEquals("Social Media", updatedList[1].displayName)

        // 2. Set active environment
        repository.setActiveEnvironment(testEnv.id)
        assertEquals(testEnv.id, repository.activeEnvironment.value?.id)

        // 3. Update environment name and verify activeEnvironment & list StateFlow update
        repository.updateDisplayName(testEnv.id, "Social Media Isolated")
        val renamedList = repository.environmentsState.first { it.any { env -> env.displayName == "Social Media Isolated" } }
        assertEquals("Social Media Isolated", renamedList[1].displayName)
        assertEquals("Social Media Isolated", repository.activeEnvironment.value?.displayName)

        // 4. Update network config
        repository.updateNetworkConfig(testEnv.id, NetworkConfig.BLOCKED)
        assertEquals(NetworkConfig.BLOCKED, repository.activeEnvironment.value?.networkConfig)

        // 5. Delete environment and verify StateFlow updates and reverts active env to System
        repository.deleteEnvironment(testEnv.id)
        val afterDeleteList = repository.environmentsState.first { it.size == 1 }
        assertEquals(1, afterDeleteList.size)
        assertEquals(EnvironmentEntity.SYSTEM_ENV_ID, repository.activeEnvironment.value?.id)
    }
}
