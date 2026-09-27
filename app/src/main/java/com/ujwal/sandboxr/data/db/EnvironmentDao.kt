package com.ujwal.sandboxr.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Environment entities.
 * Supports reactive Flow streams and transactional CRUD operations.
 */
@Dao
interface EnvironmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(environment: EnvironmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(environments: List<EnvironmentEntity>)

    @Upsert
    suspend fun upsert(environment: EnvironmentEntity)

    @Update
    suspend fun update(environment: EnvironmentEntity)

    @Delete
    suspend fun delete(environment: EnvironmentEntity)

    @Query("DELETE FROM environments WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM environments WHERE is_system = 0")
    suspend fun deleteAllNonSystem(): Int

    @Query("SELECT * FROM environments WHERE id = :id LIMIT 1")
    suspend fun getEnvironmentById(id: String): EnvironmentEntity?

    @Query("SELECT * FROM environments WHERE id = :id LIMIT 1")
    fun getEnvironmentByIdFlow(id: String): Flow<EnvironmentEntity?>

    @Query("SELECT * FROM environments ORDER BY is_system DESC, sort_order ASC, created_at ASC")
    fun getAllEnvironmentsFlow(): Flow<List<EnvironmentEntity>>

    @Query("SELECT * FROM environments ORDER BY is_system DESC, sort_order ASC, created_at ASC")
    suspend fun getAllEnvironments(): List<EnvironmentEntity>

    @Query("SELECT COUNT(*) FROM environments")
    suspend fun count(): Int

    @Query("UPDATE environments SET display_name = :displayName WHERE id = :id")
    suspend fun updateDisplayName(id: String, displayName: String): Int

    @Query("UPDATE environments SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int): Int

    @Query("UPDATE environments SET network_config = :networkConfig WHERE id = :id")
    suspend fun updateNetworkConfig(id: String, networkConfig: NetworkConfig): Int

    @Query("UPDATE environments SET gms_enabled = :gmsEnabled WHERE id = :id")
    suspend fun updateGmsEnabled(id: String, gmsEnabled: Boolean): Int

    @Query("UPDATE environments SET freeze_when_inactive = :freeze WHERE id = :id")
    suspend fun updateFreezeWhenInactive(id: String, freeze: Boolean): Int

    @Query("UPDATE environments SET notif_isolation = :notifIsolation WHERE id = :id")
    suspend fun updateNotifIsolation(id: String, notifIsolation: Boolean): Int

    @Query("UPDATE environments SET ad_block_enabled = :enabled WHERE id = :id")
    suspend fun updateAdBlockEnabled(id: String, enabled: Boolean): Int

    @Query("UPDATE environments SET clipboard_mode = :mode WHERE id = :id")
    suspend fun updateClipboardMode(id: String, mode: ClipboardMode): Int
}
