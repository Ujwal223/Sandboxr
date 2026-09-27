package com.ujwal.sandboxr.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Main Room Database for SANDBOXR.
 * Persists environments and system configuration.
 */
@Database(
    entities = [EnvironmentEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SandboxrDatabase : RoomDatabase() {

    abstract fun environmentDao(): EnvironmentDao

    companion object {
        const val DATABASE_NAME = "sandboxr.db"

        @Volatile
        private var INSTANCE: SandboxrDatabase? = null

        fun getInstance(context: Context): SandboxrDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SandboxrDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build().also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): SandboxrDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SandboxrDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
    }
}
