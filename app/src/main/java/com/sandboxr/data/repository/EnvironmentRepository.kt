package com.sandboxr.data.repository

import com.sandboxr.data.db.EnvironmentDao
import com.sandboxr.data.db.EnvironmentEntity
import com.sandboxr.data.db.NetworkConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Repository mediating access to Environment data, maintaining reactive StateFlow state
 * for UI consumers (e.g. Launcher LazyRow, Settings, Quick Settings).
 */
class EnvironmentRepository(
    private val environmentDao: EnvironmentDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * Continuous StateFlow of all configured environments, ordered with System first.
     */
    val environmentsState: StateFlow<List<EnvironmentEntity>> = environmentDao.getAllEnvironmentsFlow()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _activeEnvironment = MutableStateFlow<EnvironmentEntity?>(null)
    val activeEnvironment: StateFlow<EnvironmentEntity?> = _activeEnvironment.asStateFlow()

    suspend fun getEnvironment(id: String): EnvironmentEntity? = withContext(ioDispatcher) {
        environmentDao.getEnvironmentById(id)
    }

    suspend fun insertEnvironment(environment: EnvironmentEntity): Long = withContext(ioDispatcher) {
        environmentDao.insert(environment)
    }

    suspend fun updateEnvironment(environment: EnvironmentEntity) = withContext(ioDispatcher) {
        environmentDao.update(environment)
    }

    suspend fun deleteEnvironment(id: String): Boolean = withContext(ioDispatcher) {
        // Prevent deletion of System environment
        if (id == EnvironmentEntity.SYSTEM_ENV_ID) {
            return@withContext false
        }
        val deleted = environmentDao.deleteById(id) > 0
        if (deleted && _activeEnvironment.value?.id == id) {
            // Revert active environment to system
            _activeEnvironment.value = environmentDao.getEnvironmentById(EnvironmentEntity.SYSTEM_ENV_ID)
        }
        deleted
    }

    suspend fun setActiveEnvironment(id: String): Boolean = withContext(ioDispatcher) {
        val env = environmentDao.getEnvironmentById(id)
        if (env != null) {
            _activeEnvironment.value = env
            true
        } else {
            false
        }
    }

    suspend fun updateDisplayName(id: String, displayName: String): Boolean = withContext(ioDispatcher) {
        val updated = environmentDao.updateDisplayName(id, displayName) > 0
        if (updated && _activeEnvironment.value?.id == id) {
            _activeEnvironment.value = _activeEnvironment.value?.copy(displayName = displayName)
        }
        updated
    }

    suspend fun updateNetworkConfig(id: String, networkConfig: NetworkConfig): Boolean = withContext(ioDispatcher) {
        val updated = environmentDao.updateNetworkConfig(id, networkConfig) > 0
        if (updated && _activeEnvironment.value?.id == id) {
            _activeEnvironment.value = _activeEnvironment.value?.copy(networkConfig = networkConfig)
        }
        updated
    }

    suspend fun updateGmsEnabled(id: String, enabled: Boolean): Boolean = withContext(ioDispatcher) {
        val updated = environmentDao.updateGmsEnabled(id, enabled) > 0
        if (updated && _activeEnvironment.value?.id == id) {
            _activeEnvironment.value = _activeEnvironment.value?.copy(gmsEnabled = enabled)
        }
        updated
    }

    suspend fun updateSortOrder(id: String, order: Int): Boolean = withContext(ioDispatcher) {
        environmentDao.updateSortOrder(id, order) > 0
    }

    suspend fun ensureSystemEnvironmentExists() = withContext(ioDispatcher) {
        val existing = environmentDao.getEnvironmentById(EnvironmentEntity.SYSTEM_ENV_ID)
        if (existing == null) {
            val systemEnv = EnvironmentEntity.createSystemEnvironment()
            environmentDao.insert(systemEnv)
            if (_activeEnvironment.value == null) {
                _activeEnvironment.value = systemEnv
            }
        } else if (_activeEnvironment.value == null) {
            _activeEnvironment.value = existing
        }
    }
}
