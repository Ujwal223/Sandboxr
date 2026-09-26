package com.sandboxr.virtual.server.fs

import com.sandboxr.virtual.core.VEnvironment
import java.io.File

/**
 * Handles filesystem path redirection for virtualized guest applications.
 * Bridges environment models to the centralized StorageRedirector engine.
 */
class VFileSystem(
    private val environment: VEnvironment,
    private val redirector: StorageRedirector? = null
) {

    /**
     * Redirects a target path to the isolated environment storage location.
     */
    fun redirectPath(originalPath: String, packageName: String): String {
        if (redirector != null) {
            return redirector.redirectPath(originalPath, environment.id, packageName)
        }

        val guestDataPrefix = "/data/data/$packageName"
        val guestUserPrefix = "/data/user/0/$packageName"
        val guestSdcardPrefix = "/sdcard/Android/data/$packageName"
        val guestStoragePrefix = "/storage/emulated/0/Android/data/$packageName"

        val envDataDir = environment.getPackageDataDir(packageName).absolutePath
        val envStorageDir = environment.getPackageExternalDir(packageName).absolutePath

        return when {
            originalPath.startsWith(guestDataPrefix) -> {
                originalPath.replaceFirst(guestDataPrefix, envDataDir)
            }
            originalPath.startsWith(guestUserPrefix) -> {
                originalPath.replaceFirst(guestUserPrefix, envDataDir)
            }
            originalPath.startsWith(guestSdcardPrefix) -> {
                originalPath.replaceFirst(guestSdcardPrefix, envStorageDir)
            }
            originalPath.startsWith(guestStoragePrefix) -> {
                originalPath.replaceFirst(guestStoragePrefix, envStorageDir)
            }
            else -> originalPath
        }
    }

    /**
     * Resolves a File object through path redirection.
     */
    fun redirectFile(file: File, packageName: String): File {
        val redirected = redirectPath(file.absolutePath, packageName)
        return if (redirected != file.absolutePath) File(redirected) else file
    }
}
