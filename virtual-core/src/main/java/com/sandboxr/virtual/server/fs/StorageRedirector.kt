package com.sandboxr.virtual.server.fs

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * High-performance, isolated storage namespacing engine for SANDBOXR.
 *
 * Redirects all guest application filesystem access to per-environment sandboxes:
 * - Internal: /data/data/com.sandboxr/envs/{envId}/data/{pkg}/
 * - External: /sdcard/Android/data/com.sandboxr/envs/{envId}/{pkg}/
 *
 * Enforces strict sandbox isolation and path security:
 * 1. Sandboxing: Two guest apps in different environments writing to the same relative
 *    or absolute guest path (/data/data/{pkg}/files/data.txt) write to completely isolated
 *    underlying storage directories with zero data collision.
 * 2. Reverse Resolution: Translates physical host paths back to guest-expected paths
 *    so guest applications cannot detect the host container wrapper.
 * 3. Path Traversal Defense: Blocks path traversal attempts ('..') attempting to escape
 *    the environment sandbox directory.
 */
class StorageRedirector(
    private val hostInternalBaseDir: File,
    private val hostExternalBaseDir: File? = null
) {

    // High-performance directory memoization caches to eliminate repeated mkdirs/exists syscalls
    private val internalDirCache = java.util.concurrent.ConcurrentHashMap<String, File>()
    private val externalDirCache = java.util.concurrent.ConcurrentHashMap<String, File>()

    private val internalBaseCanonical: String by lazy {
        try { hostInternalBaseDir.canonicalPath } catch (e: IOException) { hostInternalBaseDir.absolutePath }
    }
    private val externalBaseCanonical: String? by lazy {
        hostExternalBaseDir?.let {
            try { it.canonicalPath } catch (e: IOException) { it.absolutePath }
        }
    }

    companion object {
        private const val DEFAULT_HOST_PKG = "com.sandboxr"

        @Volatile
        private var INSTANCE: StorageRedirector? = null

        fun get(context: Context): StorageRedirector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val internalBase = context.filesDir.parentFile ?: File("/data/data/$DEFAULT_HOST_PKG")
                    val externalBase = context.getExternalFilesDir(null)?.parentFile?.parentFile
                    StorageRedirector(internalBase, externalBase).also { INSTANCE = it }
                }
            }
        }

        /**
         * Creates an isolated StorageRedirector instance for unit testing.
         */
        fun createForTesting(internalRoot: File, externalRoot: File? = null): StorageRedirector {
            return StorageRedirector(internalRoot, externalRoot)
        }
    }

    /**
     * Resolves the root sandbox directory for a specific environment.
     * /data/data/com.sandboxr/envs/{envId}/
     */
    fun getEnvironmentRootDir(envId: String): File {
        return File(hostInternalBaseDir, "envs/$envId").apply { mkdirs() }
    }

    /**
     * Resolves the isolated internal data directory for a guest package in an environment:
     * /data/data/com.sandboxr/envs/{envId}/data/{packageName}/
     */
    fun getInternalDataDir(envId: String, packageName: String): File {
        val cacheKey = "$envId:$packageName"
        return internalDirCache.computeIfAbsent(cacheKey) {
            val envDir = getEnvironmentRootDir(envId)
            val dataDir = File(envDir, "data/$packageName")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
                File(dataDir, "files").mkdirs()
                File(dataDir, "cache").mkdirs()
                File(dataDir, "code_cache").mkdirs()
                File(dataDir, "databases").mkdirs()
                File(dataDir, "shared_prefs").mkdirs()
                File(dataDir, "no_backup").mkdirs()
            }
            dataDir
        }
    }

    /**
     * Resolves the isolated external storage directory for a guest package in an environment:
     * /sdcard/Android/data/com.sandboxr/envs/{envId}/{packageName}/
     */
    fun getExternalDataDir(envId: String, packageName: String): File {
        val cacheKey = "$envId:$packageName"
        return externalDirCache.computeIfAbsent(cacheKey) {
            val baseExternal = hostExternalBaseDir ?: File(hostInternalBaseDir, "mock_sdcard")
            val envExternalDir = File(baseExternal, "envs/$envId/$packageName")
            if (!envExternalDir.exists()) {
                envExternalDir.mkdirs()
                File(envExternalDir, "files").mkdirs()
                File(envExternalDir, "cache").mkdirs()
            }
            envExternalDir
        }
    }

    /**
     * Resolves the guest database directory for a given package and environment.
     */
    fun getDatabaseDir(envId: String, packageName: String): File {
        return File(getInternalDataDir(envId, packageName), "databases").apply { mkdirs() }
    }

    /**
     * Resolves the guest SharedPreferences directory for a given package and environment.
     */
    fun getSharedPrefsDir(envId: String, packageName: String): File {
        return File(getInternalDataDir(envId, packageName), "shared_prefs").apply { mkdirs() }
    }

    /**
     * Redirects a guest path to its physical isolated host path.
     *
     * @param originalPath The path requested by the guest app (e.g. /data/data/com.example/files/db.sqlite).
     * @param envId Target environment UUID.
     * @param packageName Package name of the calling guest app.
     * @return Absolute path on host filesystem inside the isolated environment container.
     */
    fun redirectPath(originalPath: String, envId: String, packageName: String): String {
        val internalData = getInternalDataDir(envId, packageName).absolutePath
        val externalData = getExternalDataDir(envId, packageName).absolutePath

        val guestDataPrefix = "/data/data/$packageName"
        val guestUserPrefix = "/data/user/0/$packageName"
        val guestSdcardPrefix = "/sdcard/Android/data/$packageName"
        val guestStoragePrefix = "/storage/emulated/0/Android/data/$packageName"
        val guestObbPrefix = "/sdcard/Android/obb/$packageName"
        val guestStorageObbPrefix = "/storage/emulated/0/Android/obb/$packageName"

        val targetPath = when {
            originalPath.startsWith(guestDataPrefix) -> {
                originalPath.replaceFirst(guestDataPrefix, internalData)
            }
            originalPath.startsWith(guestUserPrefix) -> {
                originalPath.replaceFirst(guestUserPrefix, internalData)
            }
            originalPath.startsWith(guestSdcardPrefix) -> {
                originalPath.replaceFirst(guestSdcardPrefix, externalData)
            }
            originalPath.startsWith(guestStoragePrefix) -> {
                originalPath.replaceFirst(guestStoragePrefix, externalData)
            }
            originalPath.startsWith(guestObbPrefix) -> {
                val obbDir = File(getExternalDataDir(envId, packageName).parentFile, "obb/$packageName")
                originalPath.replaceFirst(guestObbPrefix, obbDir.apply { mkdirs() }.absolutePath)
            }
            originalPath.startsWith(guestStorageObbPrefix) -> {
                val obbDir = File(getExternalDataDir(envId, packageName).parentFile, "obb/$packageName")
                originalPath.replaceFirst(guestStorageObbPrefix, obbDir.apply { mkdirs() }.absolutePath)
            }
            // Relative paths resolve inside package files directory
            !originalPath.startsWith("/") -> {
                File(File(internalData, "files"), originalPath).absolutePath
            }
            else -> originalPath
        }

        // Security check: Verify path does not escape environment sandbox
        validateSandboxContainment(targetPath, envId)

        return targetPath
    }

    /**
     * Resolves a File object by redirecting its path.
     */
    fun redirectFile(file: File, envId: String, packageName: String): File {
        return File(redirectPath(file.absolutePath, envId, packageName))
    }

    /**
     * Translates a host sandbox path back to its virtual guest perspective.
     * Prevents guest apps from discovering they are containerized under com.sandboxr.
     */
    fun toGuestPath(hostPath: String, envId: String, packageName: String): String {
        val internalData = getInternalDataDir(envId, packageName).absolutePath
        val externalData = getExternalDataDir(envId, packageName).absolutePath

        return when {
            hostPath.startsWith(internalData) -> {
                hostPath.replaceFirst(internalData, "/data/data/$packageName")
            }
            hostPath.startsWith(externalData) -> {
                hostPath.replaceFirst(externalData, "/sdcard/Android/data/$packageName")
            }
            else -> hostPath
        }
    }

    /**
     * Completely purges all data for an environment (used on deletion or duress wipe).
     */
    fun wipeEnvironmentStorage(envId: String): Boolean {
        val prefix = "$envId:"
        internalDirCache.keys.removeIf { it.startsWith(prefix) }
        externalDirCache.keys.removeIf { it.startsWith(prefix) }

        val envDir = File(hostInternalBaseDir, "envs/$envId")
        val internalDeleted = if (envDir.exists()) envDir.deleteRecursively() else true

        // Clean up any legacy directory under files/envs/$envId
        val legacyDir = File(hostInternalBaseDir, "files/envs/$envId")
        if (legacyDir.exists()) {
            legacyDir.deleteRecursively()
        }

        val baseExternal = hostExternalBaseDir ?: File(hostInternalBaseDir, "mock_sdcard")
        val externalEnvDir = File(baseExternal, "envs/$envId")
        val externalDeleted = if (externalEnvDir.exists()) externalEnvDir.deleteRecursively() else true

        return internalDeleted && externalDeleted
    }

    /**
     * Computes total storage size (in bytes) occupied by an environment.
     */
    fun calculateEnvironmentStorageSize(envId: String): Long {
        val envDir = File(hostInternalBaseDir, "envs/$envId")
        val baseExternal = hostExternalBaseDir ?: File(hostInternalBaseDir, "mock_sdcard")
        val externalEnvDir = File(baseExternal, "envs/$envId")

        return computeDirectorySize(envDir) + computeDirectorySize(externalEnvDir)
    }

    /**
     * Computes storage size (in bytes) occupied by a specific package in an environment.
     */
    fun calculatePackageStorageSize(envId: String, packageName: String): Long {
        val internalPkgDir = File(hostInternalBaseDir, "envs/$envId/data/$packageName")
        val baseExternal = hostExternalBaseDir ?: File(hostInternalBaseDir, "mock_sdcard")
        val externalPkgDir = File(baseExternal, "envs/$envId/$packageName")

        return computeDirectorySize(internalPkgDir) + computeDirectorySize(externalPkgDir)
    }

    private fun computeDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            size += if (child.isDirectory) computeDirectorySize(child) else child.length()
        }
        return size
    }

    private fun validateSandboxContainment(targetPath: String, envId: String) {
        val file = File(targetPath)

        // Find deepest existing ancestor to resolve all symlinks and directory escapes
        var existing: File? = file
        while (existing != null && !existing.exists()) {
            existing = existing.parentFile
        }
        val canonicalExisting = existing?.canonicalFile ?: file.canonicalFile
        val canonicalExistingPath = canonicalExisting.path

        val internalEnvRoot = getEnvironmentRootDir(envId).canonicalPath
        val externalEnvRoot = externalBaseCanonical?.let { File(it, "envs/$envId").canonicalPath }
            ?: (hostExternalBaseDir?.let { File(it, "envs/$envId").canonicalPath }
                ?: File(hostInternalBaseDir, "mock_sdcard/envs/$envId").canonicalPath)

        // 1. Check if the path targets the host internal storage directory
        val isInsideInternalBase = canonicalExistingPath == internalBaseCanonical ||
                canonicalExistingPath.startsWith(internalBaseCanonical + File.separator) ||
                canonicalExistingPath == hostInternalBaseDir.canonicalPath ||
                canonicalExistingPath.startsWith(hostInternalBaseDir.canonicalPath + File.separator)

        if (isInsideInternalBase) {
            val isWithinInternalEnv = canonicalExistingPath == internalEnvRoot ||
                    canonicalExistingPath.startsWith(internalEnvRoot + File.separator)
            if (!isWithinInternalEnv) {
                throw SecurityException("Path traversal blocked: path '$targetPath' (resolves to '$canonicalExistingPath') escapes sandbox for environment '$envId'")
            }
        }

        // 2. Check if the path targets external storage directory
        val isInsideExternalBase = externalBaseCanonical?.let {
            canonicalExistingPath == it || canonicalExistingPath.startsWith(it + File.separator)
        } ?: false || (hostExternalBaseDir?.let {
            val p = it.canonicalPath
            canonicalExistingPath == p || canonicalExistingPath.startsWith(p + File.separator)
        } ?: false)

        if (isInsideExternalBase) {
            val isWithinExternalEnv = canonicalExistingPath == externalEnvRoot ||
                    canonicalExistingPath.startsWith(externalEnvRoot + File.separator)
            if (!isWithinExternalEnv) {
                throw SecurityException("Path traversal blocked: external path '$targetPath' (resolves to '$canonicalExistingPath') escapes sandbox for environment '$envId'")
            }
        }
    }
}
