/*
 * Copyright (C) 2016 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.sandboxr.launcher.provider.RestoreDbTask
import java.io.File
import java.io.IOException

/**
 * BackupAgent responsible for managing cloud and cross-device backups of the Sandboxr Launcher database and preferences.
 */
open class LauncherBackupAgent : BackupAgent() {

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) {
        // Full backup mode is handled by the platform; key-value incremental restore is a no-op
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) {
        // Full backup mode is handled via backupscheme.xml
    }

    @Throws(IOException::class)
    override fun onRestoreFile(
        data: ParcelFileDescriptor?,
        size: Long,
        destination: File?,
        type: Int,
        mode: Long,
        mtime: Long,
    ) {
        if (destination != null && destination.exists()) {
            val deleted = destination.delete()
            if (deleted) {
                Log.d(TAG, "Removed obsolete destination file before restore: ${destination.path}")
            }
        }
        super.onRestoreFile(data, size, destination, type, mode, mtime)
    }

    override fun onRestoreFinished() {
        RestoreDbTask.setPending(this, true)
        Log.i(TAG, "onRestoreFinished: Set pending for RestoreDbTask")
        markIfFilesWereNotActuallyRestored()
    }

    /**
     * Checks if any database files were successfully restored into the app's databases directory.
     */
    fun markIfFilesWereNotActuallyRestored(): Boolean {
        val dbFile = getDatabasePath(LauncherFiles.LAUNCHER_DB)
        val directory = dbFile.parentFile
        if (directory == null || !directory.exists()) {
            Log.e(TAG, "Restore target database directory doesn't exist: $directory")
            markNoDbFilesRestored(this, true)
            return false
        }

        val restoredDbFiles = directory.listFiles { _, name ->
            name.startsWith(DB_FILE_PREFIX) && name.endsWith(DB_FILE_SUFFIX)
        }

        if (restoredDbFiles.isNullOrEmpty()) {
            Log.w(TAG, "No database files were successfully restored into $directory")
            markNoDbFilesRestored(this, true)
            return false
        } else {
            val names = restoredDbFiles.joinToString(", ") { it.name }
            Log.i(TAG, "Database files successfully restored: $names")
            markNoDbFilesRestored(this, false)
            return true
        }
    }

    companion object {
        private const val TAG = "LauncherBackupAgent"
        private const val DB_FILE_PREFIX = "launcher"
        private const val DB_FILE_SUFFIX = ".db"
        const val PREF_NO_DB_FILES_RESTORED = "no_db_files_restored"

        @JvmStatic
        fun markNoDbFilesRestored(context: Context, failed: Boolean) {
            val prefs = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_NO_DB_FILES_RESTORED, failed).apply()
        }

        @JvmStatic
        fun isNoDbFilesRestored(context: Context): Boolean {
            val prefs = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_NO_DB_FILES_RESTORED, false)
        }
    }
}
