package com.ujwal.sandboxr.ui

import com.ujwal.sandboxr.data.db.ClipboardMode
import com.ujwal.sandboxr.data.db.EnvironmentDao
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.ujwal.sandboxr.data.repository.EnvironmentRepository
import com.ujwal.sandboxr.domain.SystemEnvironmentManager
import com.ujwal.sandboxr.installer.InstallerEngine
import com.sandboxr.launcher.ui.AppItem
import com.sandboxr.launcher.ui.EnvironmentIconType
import com.sandboxr.launcher.ui.NavTab
import com.ujwal.sandboxr.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeEnvironmentDao : EnvironmentDao {
        val list = mutableListOf<EnvironmentEntity>()
        val flow = MutableStateFlow<List<EnvironmentEntity>>(emptyList())

        override suspend fun insert(environment: EnvironmentEntity): Long {
            list.removeAll { it.id == environment.id }
            list.add(environment)
            flow.value = list.toList()
            return 1L
        }

        override suspend fun insertAll(environments: List<EnvironmentEntity>) {
            list.addAll(environments)
            flow.value = list.toList()
        }

        override suspend fun upsert(environment: EnvironmentEntity) {
            insert(environment)
        }

        override suspend fun update(environment: EnvironmentEntity) {
            insert(environment)
        }

        override suspend fun delete(environment: EnvironmentEntity) {
            list.removeAll { it.id == environment.id }
            flow.value = list.toList()
        }

        override suspend fun deleteById(id: String): Int {
            val removed = list.removeAll { it.id == id }
            flow.value = list.toList()
            return if (removed) 1 else 0
        }

        override suspend fun deleteAllNonSystem(): Int {
            val count = list.count { !it.isSystem }
            list.removeAll { !it.isSystem }
            flow.value = list.toList()
            return count
        }

        override suspend fun getEnvironmentById(id: String): EnvironmentEntity? =
            list.find { it.id == id }

        override fun getEnvironmentByIdFlow(id: String): Flow<EnvironmentEntity?> =
            MutableStateFlow(list.find { it.id == id })

        override fun getAllEnvironmentsFlow(): Flow<List<EnvironmentEntity>> = flow

        override suspend fun getAllEnvironments(): List<EnvironmentEntity> = list.toList()

        override suspend fun count(): Int = list.size

        override suspend fun updateDisplayName(id: String, displayName: String): Int = 1
        override suspend fun updateSortOrder(id: String, sortOrder: Int): Int = 1
        override suspend fun updateNetworkConfig(id: String, networkConfig: NetworkConfig): Int = 1
        override suspend fun updateGmsEnabled(id: String, gmsEnabled: Boolean): Int = 1
        override suspend fun updateFreezeWhenInactive(id: String, freeze: Boolean): Int = 1
        override suspend fun updateNotifIsolation(id: String, notifIsolation: Boolean): Int = 1
        override suspend fun updateAdBlockEnabled(id: String, enabled: Boolean): Int = 1
        override suspend fun updateClipboardMode(id: String, mode: ClipboardMode): Int = 1
    }

    private lateinit var fakeDao: FakeEnvironmentDao
    private lateinit var repository: EnvironmentRepository
    private lateinit var viewModel: LauncherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeEnvironmentDao()
        val sysEnv = EnvironmentEntity.createSystemEnvironment()
        fakeDao.list.add(sysEnv)
        fakeDao.flow.value = listOf(sysEnv)

        repository = EnvironmentRepository(fakeDao, scope = kotlinx.coroutines.CoroutineScope(testDispatcher), ioDispatcher = testDispatcher)
        viewModel = LauncherViewModel(
            environmentRepository = repository,
            systemEnvManager = SystemEnvironmentManager(),
            installerEngine = InstallerEngine()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateHasSystemEnvironmentActive() = runTest {
        advanceUntilIdle()
        assertEquals(EnvironmentEntity.SYSTEM_ENV_ID, viewModel.activeEnvironmentId.value)
        val active = viewModel.activeEnvironment.value
        assertNotNull(active)
        assertEquals("Personal", active?.displayName)
        assertTrue(active?.isSystem == true)
    }

    @Test
    fun testNavigationTabSwitching() = runTest {
        assertEquals(NavTab.ENVIRONMENTS, viewModel.currentTab.value)
        viewModel.setTab(NavTab.FIREWALL)
        assertEquals(NavTab.FIREWALL, viewModel.currentTab.value)
        viewModel.setTab(NavTab.VAULT)
        assertEquals(NavTab.VAULT, viewModel.currentTab.value)
        viewModel.setTab(NavTab.SETTINGS)
        assertEquals(NavTab.SETTINGS, viewModel.currentTab.value)
    }

    @Test
    fun testSearchQueryFiltering() = runTest {
        viewModel.setSearchQuery("calc")
        assertEquals("calc", viewModel.searchQuery.value)
        viewModel.setSearchQuery("")
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun testCreateAndSwitchVirtualEnvironment() = runTest {
        advanceUntilIdle()

        viewModel.openCreateDialog()
        assertTrue(viewModel.showCreateDialog.value)

        // Mock context not needed for repository insertion
        val newEnv = EnvironmentEntity(
            id = "test_env_1",
            displayName = "Personal Sandbox",
            colorTag = 0xFF4A3A6BL,
            iconEmoji = EnvironmentIconType.TERMINAL.name,
            networkConfig = NetworkConfig.WIREGUARD
        )
        fakeDao.insert(newEnv)
        advanceUntilIdle()

        assertEquals(2, viewModel.environments.value.size)
    }

    @Test
    fun testWorkProfileAutoRegistrationAndCleanup() = runTest {
        advanceUntilIdle()
        assertEquals(1, viewModel.environments.value.size)

        // Simulate Work Profile detection
        repository.syncWorkProfile(hasWorkProfile = true)
        advanceUntilIdle()

        val envsWithWork = viewModel.environments.value
        assertEquals(2, envsWithWork.size)
        assertTrue(envsWithWork.any { it.id == EnvironmentEntity.WORK_PROFILE_ENV_ID })

        val workEnv = envsWithWork.find { it.id == EnvironmentEntity.WORK_PROFILE_ENV_ID }
        assertNotNull(workEnv)
        assertEquals("Work Profile", workEnv?.displayName)
        assertTrue(workEnv?.isSystem == true)

        // Simulate Work Profile removal
        repository.syncWorkProfile(hasWorkProfile = false)
        advanceUntilIdle()

        val envsWithoutWork = viewModel.environments.value
        assertEquals(1, envsWithoutWork.size)
        assertTrue(envsWithoutWork.none { it.id == EnvironmentEntity.WORK_PROFILE_ENV_ID })
    }

    @Test
    fun testAddAndRemoveFromHomeAndDock() = runTest {
        advanceUntilIdle()

        val testApp = AppItem(
            packageName = "org.torproject.torbrowser",
            label = "Tor Browser",
            envId = EnvironmentEntity.SYSTEM_ENV_ID,
            isSystemApp = true
        )

        // Add to home
        viewModel.addToHome(testApp)
        assertTrue(viewModel.homeApps.value.any { it.packageName == testApp.packageName })

        // Remove from home
        viewModel.removeFromHome(testApp)
        assertTrue(viewModel.homeApps.value.none { it.packageName == testApp.packageName })

        // Add to dock
        viewModel.addToDock(testApp)
        assertTrue(viewModel.dockApps.value.any { it.packageName == testApp.packageName })

        // Remove from dock
        viewModel.removeFromDock(testApp)
        assertTrue(viewModel.dockApps.value.none { it.packageName == testApp.packageName })
    }
}
