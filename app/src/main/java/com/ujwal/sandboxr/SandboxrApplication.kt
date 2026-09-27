package com.ujwal.sandboxr

import android.app.Activity
import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.os.Bundle
import android.os.Process
import android.util.Log
import com.ujwal.sandboxr.data.db.SandboxrDatabase
import com.ujwal.sandboxr.data.repository.EnvironmentRepository
import com.ujwal.sandboxr.installer.InstallerEngine
import com.sandboxr.launcher.LauncherApplication
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.client.hook.ServiceManagerHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Application subclass for SANDBOXR.
 * Extends [LauncherApplication] to integrate GrapheneOS Launcher3 lifecycle,
 * Dagger dependency injection bootstrap, and MainProcessInitializer, while handling
 * crash recovery sentinels, SQLite database integrity repair, and staging directory maintenance.
 */
class SandboxrApplication : LauncherApplication() {

    companion object {
        private const val TAG = "SandboxrApp"
        const val SENTINEL_DIR = "sentinels"
        const val SESSION_SENTINEL_FILE = "active_session.sentinel"
        const val LOGS_DIR = "logs"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeActivityCount = 0

    override fun onCreate() {
        // Detect if this is a guest virtual process (name contains ':') or the host main process
        val processName = getCurrentProcessName()
        val isGuestProcess = processName != null && processName.contains(':')

        if (isGuestProcess) {
            // --- GUEST SUBPROCESS PATH ---
            // Only install the binder hooks needed for the virtual container.
            // Do NOT run crash recovery, DB access, or host-side VirtualCore initialization.
            Log.i(TAG, "SANDBOXR guest process '$processName' starting — installing virtual hooks only.")
            try {
                com.sandboxr.virtual.compat.HiddenApiBypassHelper.exemptAll()
            } catch (t: Throwable) {
                Log.w(TAG, "HiddenApiBypass in guest process: ${t.message}")
            }
            try {
                ServiceManagerHook.installAll(this, isGuestProcess = true)
            } catch (t: Throwable) {
                Log.w(TAG, "Guest ServiceManagerHook install notice: ${t.message}")
            }
            return
        }

        // --- HOST MAIN PROCESS PATH ---
        Log.i(TAG, "SANDBOXR Privacy Platform initializing (host process: $processName)...")

        // 1. Initialize Launcher base lifecycle & Dagger component & MainProcessInitializer
        super.onCreate()

        // 2. Initialize Samsung compatibility layer early to prevent profile crashes
        try {
            com.sandboxr.virtual.compat.SamsungCompat.init(this)
        } catch (e: Exception) {
            Log.w(TAG, "SamsungCompat initialization failed: ${e.message}")
        }

        // 2. Run Crash Recovery Sentinel Check & SQLite Repair synchronously before DB access
        performCrashRecoveryPipeline()

        // 3. Install UncaughtExceptionHandler to capture crashes and preserve SQLite WAL integrity
        installCrashHandler()

        // 4. Register Activity lifecycle tracking to manage clean session exit
        registerSessionLifecycleTracker()

        // 5. Initialize VirtualCore runtime (host process — hooks are NOT installed here, only services)
        try {
            VirtualCore.init(this)
        } catch (e: Exception) {
            Log.e(TAG, "VirtualCore runtime initialization failed: ${e.message}", e)
        }

        // 5. Ensure Default System Environment exists in Room DB and sync Work Profile
        applicationScope.launch {
            try {
                val db = SandboxrDatabase.getInstance(this@SandboxrApplication)
                val repository = EnvironmentRepository(db.environmentDao())
                repository.ensureSystemEnvironmentExists()
                val loader = com.sandboxr.launcher.loader.LawnchairAppLoader(this@SandboxrApplication)
                repository.syncWorkProfile(loader.hasWorkProfile())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize default system environment: ${e.message}", e)
            }
        }
    }

    /**
     * Returns the current process name. Uses reflection to access the hidden
     * android.app.ActivityThread.currentProcessName() API on Android 9+ (API 28+),
     * with a reliable /proc/self/cmdline fallback for all API levels.
     */
    override fun getCurrentProcessName(): String? {
        // Fast path: reflection on ActivityThread (API 28+, hidden but widely available)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val method = activityThreadClass.getDeclaredMethod("currentProcessName")
                method.isAccessible = true
                val name = method.invoke(null) as? String
                if (!name.isNullOrBlank()) return name
            } catch (_: Throwable) {}
        }
        // Reliable fallback: /proc/self/cmdline (works on all API levels, all OEMs)
        return getProcessNameFromProc()
    }

    private fun getProcessNameFromProc(): String? {
        return try {
            val cmdlineFile = File("/proc/self/cmdline")
            if (cmdlineFile.exists()) {
                val cmdline = cmdlineFile.readText().trim().trimEnd('\u0000')
                if (cmdline.isNotEmpty()) cmdline else null
            } else null
        } catch (_: Exception) { null }
    }

    /**
     * Executes the crash recovery pipeline:
     * - Checks for prior unclean shutdown or crash sentinel.
     * - Purges all orphaned and partial staging APKs.
     * - Runs SQLite PRAGMA integrity check and WAL checkpoint/repair.
     * - Generates a fresh session sentinel.
     */
    fun performCrashRecoveryPipeline(): Boolean {
        var crashDetected = false
        val sentinelFile = getSessionSentinelFile()

        if (sentinelFile.exists()) {
            val previousSessionInfo = try { sentinelFile.readText() } catch (_: Exception) { "Unknown" }
            Log.w(TAG, "Abnormal termination detected on previous session ($previousSessionInfo). Starting crash recovery...")
            crashDetected = true

            // 1. Delete all partial and orphaned staging files immediately
            val purged = try {
                InstallerEngine.cleanupOrphanedStagingFiles(this, maxAgeMillis = 0L)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to purge staging files during recovery: ${e.message}")
                0
            }
            Log.i(TAG, "Crash recovery purged $purged orphaned staging files.")

            // 2. Perform SQLite database integrity check and repair
            repairSqliteDatabaseIfCorrupted()

            // 3. Clean up sentinel
            try {
                sentinelFile.delete()
            } catch (_: Exception) {}
        } else {
            // Normal startup: clean stale staging files older than 1 hour
            try {
                InstallerEngine.cleanupOrphanedStagingFiles(this, maxAgeMillis = 60 * 60 * 1000L)
            } catch (_: Exception) {}
        }

        // Write fresh active session sentinel
        writeSessionSentinel()
        return crashDetected
    }

    /**
     * Inspects SQLite database file integrity using PRAGMA quick_check.
     * Recovers from WAL corruption by executing a truncate checkpoint or safe backup.
     */
    fun repairSqliteDatabaseIfCorrupted(): Boolean {
        val dbFile = getDatabasePath(SandboxrDatabase.DATABASE_NAME)
        if (!dbFile.exists()) {
            return true
        }

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            // 1. Run quick integrity check
            val cursor = db.rawQuery("PRAGMA quick_check;", null)
            var isOk = false
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                isOk = result.equals("ok", ignoreCase = true)
            }
            cursor.close()

            if (!isOk) {
                Log.e(TAG, "SQLite corruption detected! Executing WAL checkpoint truncate recovery...")
                db.execSQL("PRAGMA wal_checkpoint(TRUNCATE);")
            } else {
                // Flush WAL to maintain file consistency
                db.execSQL("PRAGMA wal_checkpoint(PASSIVE);")
            }
            return true

        } catch (corruptEx: SQLiteDatabaseCorruptException) {
            Log.e(TAG, "SQLite database corrupted! Backing up and rebuilding: ${corruptEx.message}", corruptEx)
            val backupFile = File(dbFile.parentFile, "${dbFile.name}.corrupt.${System.currentTimeMillis()}")
            dbFile.renameTo(backupFile)
            return false

        } catch (e: Exception) {
            Log.w(TAG, "SQLite check completed with notice: ${e.message}")
            return true

        } finally {
            try {
                db?.close()
            } catch (_: Exception) {}
        }
    }

    fun getSessionSentinelFile(): File {
        val sentinelDir = File(filesDir, SENTINEL_DIR)
        if (!sentinelDir.exists()) {
            sentinelDir.mkdirs()
        }
        return File(sentinelDir, SESSION_SENTINEL_FILE)
    }

    private fun writeSessionSentinel() {
        try {
            val file = getSessionSentinelFile()
            val sessionInfo = "PID=${Process.myPid()}|STARTED=${System.currentTimeMillis()}|TIME=${Date()}"
            file.writeText(sessionInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Could not write session sentinel: ${e.message}")
        }
    }

    fun clearSessionSentinel() {
        try {
            val file = getSessionSentinelFile()
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Uncaught fatal crash in thread ${thread.name}: ${throwable.message}", throwable)
                val logsDir = File(filesDir, LOGS_DIR)
                if (!logsDir.exists()) logsDir.mkdirs()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val crashFile = File(logsDir, "crash_$timestamp.log")
                FileWriter(crashFile).use { writer ->
                    writer.write("FATAL EXCEPTION: ${thread.name}\n")
                    writer.write("PID: ${Process.myPid()}\n")
                    writer.write("TIME: ${Date()}\n")
                    writer.write("MESSAGE: ${throwable.message}\n\n")
                    writer.write(Log.getStackTraceString(throwable))
                }
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun registerSessionLifecycleTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activeActivityCount++
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activeActivityCount = maxOf(0, activeActivityCount - 1)
                if (activeActivityCount == 0) {
                    try {
                        val dbFile = getDatabasePath(SandboxrDatabase.DATABASE_NAME)
                        if (dbFile.exists()) {
                            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                            db.execSQL("PRAGMA wal_checkpoint(PASSIVE);")
                            db.close()
                        }
                    } catch (_: Exception) {}
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
