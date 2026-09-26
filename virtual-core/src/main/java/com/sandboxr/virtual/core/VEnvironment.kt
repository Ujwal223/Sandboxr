package com.sandboxr.virtual.core

import android.content.Context
import com.sandboxr.virtual.model.SpoofProfile
import java.io.File

/**
 * Supported network routing configurations per environment.
 */
enum class NetworkMode {
    DIRECT,
    SOCKS5,
    WIREGUARD,
    BLOCKED
}

/**
 * Representation of an isolated user-space container environment.
 */
data class VEnvironment(
    val id: String,
    val name: String,
    val color: Long,
    val rootDir: File,
    val dataDir: File,
    val storageDir: File,
    val networkMode: NetworkMode = NetworkMode.DIRECT,
    val spoofProfile: SpoofProfile = SpoofProfile.generate()
) {
    companion object {
        fun create(context: Context, id: String, name: String, color: Long): VEnvironment {
            val baseEnvDir = File(context.filesDir?.parentFile ?: context.filesDir, "envs/$id")
            val dataDir = File(baseEnvDir, "data")
            val extDir = try { context.getExternalFilesDir(null) } catch (e: Exception) { null }
            val storageParent = extDir?.parentFile?.parentFile
            val storageDir = if (storageParent != null) {
                File(storageParent, "envs/$id")
            } else {
                File(baseEnvDir, "storage")
            }

            baseEnvDir.mkdirs()
            dataDir.mkdirs()
            storageDir.mkdirs()

            return VEnvironment(
                id = id,
                name = name,
                color = color,
                rootDir = baseEnvDir,
                dataDir = dataDir,
                storageDir = storageDir
            )
        }
    }

    /**
     * Resolves the app-specific private data directory for a given guest package.
     */
    fun getPackageDataDir(packageName: String): File {
        val dir = File(dataDir, packageName)
        if (!dir.exists()) {
            dir.mkdirs()
            File(dir, "files").mkdirs()
            File(dir, "cache").mkdirs()
            File(dir, "code_cache").mkdirs()
            File(dir, "databases").mkdirs()
            File(dir, "shared_prefs").mkdirs()
        }
        return dir
    }

    /**
     * Resolves the app-specific external storage directory for a given guest package.
     */
    fun getPackageExternalDir(packageName: String): File {
        val dir = File(storageDir, packageName)
        if (!dir.exists()) {
            dir.mkdirs()
            File(dir, "files").mkdirs()
            File(dir, "cache").mkdirs()
        }
        return dir
    }
}
