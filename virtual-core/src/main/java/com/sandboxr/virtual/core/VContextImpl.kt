package com.sandboxr.virtual.core

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import com.sandboxr.virtual.server.fs.StorageRedirector
import com.sandboxr.virtual.server.fs.VFileSystem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * ContextWrapper subclass that sandboxes and isolates filesystem storage, package identity,
 * databases, and SharedPreferences for a running virtual application.
 *
 * Guarantees zero leakage into the host Sandboxr application storage.
 */
class VContextImpl(
    base: Context,
    val environment: VEnvironment,
    private val guestPackageName: String,
    private val guestClassLoader: ClassLoader,
    private val guestAppInfo: ApplicationInfo
) : ContextWrapper(base) {

    private val redirector = StorageRedirector.get(base)
    private val fileSystem = VFileSystem(environment, redirector)
    private val packageDataDir = environment.getPackageDataDir(guestPackageName)
    private val packageExternalDir = environment.getPackageExternalDir(guestPackageName)
    private val sharedPrefsCache = ConcurrentHashMap<String, SharedPreferences>()

    private val guestAssetManager: android.content.res.AssetManager by lazy {
        try {
            val am = android.content.res.AssetManager::class.java.getDeclaredConstructor().newInstance()
            val addAssetPath = android.content.res.AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
            addAssetPath.isAccessible = true
            addAssetPath.invoke(am, guestAppInfo.sourceDir)
            guestAppInfo.splitSourceDirs?.forEach { splitPath ->
                if (File(splitPath).exists()) {
                    try {
                        addAssetPath.invoke(am, splitPath)
                    } catch (_: Throwable) {}
                }
            }
            am
        } catch (_: Throwable) {
            baseContext.assets
        }
    }

    private val guestResources: android.content.res.Resources by lazy {
        try {
            android.content.res.Resources(
                guestAssetManager,
                baseContext.resources.displayMetrics,
                baseContext.resources.configuration
            )
        } catch (_: Throwable) {
            baseContext.resources
        }
    }

    override fun getPackageName(): String = guestPackageName

    override fun getClassLoader(): ClassLoader = guestClassLoader

    override fun getApplicationInfo(): ApplicationInfo = guestAppInfo

    override fun getAssets(): android.content.res.AssetManager = guestAssetManager

    override fun getResources(): android.content.res.Resources = guestResources

    override fun getDataDir(): File = packageDataDir

    override fun getFilesDir(): File {
        return File(packageDataDir, "files").apply { mkdirs() }
    }

    override fun getCacheDir(): File {
        return File(packageDataDir, "cache").apply { mkdirs() }
    }

    override fun getCodeCacheDir(): File {
        return File(packageDataDir, "code_cache").apply { mkdirs() }
    }

    override fun getNoBackupFilesDir(): File {
        return File(packageDataDir, "no_backup").apply { mkdirs() }
    }

    override fun getDatabasePath(name: String): File {
        val dbDir = File(packageDataDir, "databases").apply { mkdirs() }
        val target = File(dbDir, name)
        val canonicalDbDir = dbDir.canonicalPath
        val canonicalTarget = target.canonicalPath
        if (!canonicalTarget.startsWith(canonicalDbDir + File.separator) && canonicalTarget != canonicalDbDir) {
            throw SecurityException("Database path traversal blocked: $name escapes $canonicalDbDir")
        }
        return target
    }

    override fun getDir(name: String, mode: Int): File {
        val dir = File(packageDataDir, "app_$name").apply { mkdirs() }
        val canonicalDataDir = packageDataDir.canonicalPath
        val canonicalDir = dir.canonicalPath
        if (!canonicalDir.startsWith(canonicalDataDir + File.separator) && canonicalDir != canonicalDataDir) {
            throw SecurityException("Path traversal in getDir blocked: $name escapes $canonicalDataDir")
        }
        return dir
    }

    override fun getExternalFilesDir(type: String?): File? {
        val sub = if (type == null) "files" else "files/$type"
        return File(packageExternalDir, sub).apply { mkdirs() }
    }

    override fun getExternalCacheDir(): File? {
        return File(packageExternalDir, "cache").apply { mkdirs() }
    }

    override fun getExternalMediaDirs(): Array<File> {
        val mediaDir = File(packageExternalDir, "media").apply { mkdirs() }
        return arrayOf(mediaDir)
    }

    override fun getObbDir(): File {
        val obbDir = File(packageExternalDir.parentFile, "obb/$guestPackageName").apply { mkdirs() }
        return obbDir
    }

    override fun getObbDirs(): Array<File> {
        return arrayOf(obbDir)
    }

    override fun openFileInput(name: String): FileInputStream {
        val file = File(filesDir, name)
        val canonicalFilesDir = filesDir.canonicalPath
        val canonicalFile = file.canonicalPath
        if (!canonicalFile.startsWith(canonicalFilesDir + File.separator)) {
            throw SecurityException("Path traversal attempt in openFileInput: $name")
        }
        return FileInputStream(file)
    }

    override fun openFileOutput(name: String, mode: Int): FileOutputStream {
        val file = File(filesDir, name)
        val canonicalFilesDir = filesDir.canonicalPath
        val canonicalFile = file.canonicalPath
        if (!canonicalFile.startsWith(canonicalFilesDir + File.separator)) {
            throw SecurityException("Path traversal attempt in openFileOutput: $name")
        }
        val append = (mode and Context.MODE_APPEND) != 0
        return FileOutputStream(file, append)
    }

    override fun deleteFile(name: String): Boolean {
        val file = File(filesDir, name)
        val canonicalFilesDir = filesDir.canonicalPath
        val canonicalFile = file.canonicalPath
        if (!canonicalFile.startsWith(canonicalFilesDir + File.separator)) {
            throw SecurityException("Path traversal attempt in deleteFile: $name")
        }
        return file.delete()
    }

    override fun fileList(): Array<String> {
        return filesDir.list() ?: emptyArray()
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?
    ): SQLiteDatabase {
        val dbFile = getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        return SQLiteDatabase.openOrCreateDatabase(dbFile, factory)
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: SQLiteDatabase.CursorFactory?,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase {
        val dbFile = getDatabasePath(name)
        dbFile.parentFile?.mkdirs()
        return SQLiteDatabase.openOrCreateDatabase(dbFile.path, factory, errorHandler)
    }

    override fun moveDatabaseFrom(sourceContext: Context, name: String): Boolean {
        val source = sourceContext.getDatabasePath(name)
        val dest = getDatabasePath(name)
        return source.renameTo(dest)
    }

    override fun deleteDatabase(name: String): Boolean {
        return SQLiteDatabase.deleteDatabase(getDatabasePath(name))
    }

    override fun databaseList(): Array<String> {
        val dbDir = File(packageDataDir, "databases")
        return dbDir.list() ?: emptyArray()
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
        return sharedPrefsCache.computeIfAbsent(name) { spName ->
            val prefsDir = File(packageDataDir, "shared_prefs").apply { mkdirs() }
            val prefsFile = File(prefsDir, "$spName.xml")

            // 1. Attempt reflection on ContextImpl.getSharedPreferences(File, int)
            try {
                val getSpMethod = baseContext.javaClass.getMethod(
                    "getSharedPreferences",
                    File::class.java,
                    Int::class.javaPrimitiveType
                )
                getSpMethod.isAccessible = true
                getSpMethod.invoke(baseContext, prefsFile, mode) as SharedPreferences
            } catch (_: Throwable) {
                // 2. Fallback to isolated, file-backed VSharedPreferences
                VSharedPreferences(prefsFile)
            }
        }
    }

    override fun deleteSharedPreferences(name: String): Boolean {
        sharedPrefsCache.remove(name)
        val prefsDir = File(packageDataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "$name.xml")
        return if (prefsFile.exists()) prefsFile.delete() else true
    }
}
