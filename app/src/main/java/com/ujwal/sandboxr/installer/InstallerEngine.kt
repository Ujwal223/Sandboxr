package com.ujwal.sandboxr.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.model.InstalledPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Target destination for an APK installation.
 */
sealed class InstallDestination {
    data object System : InstallDestination()
    data class Virtual(val environmentId: String, val environmentName: String) : InstallDestination()
}

/**
 * Result of an installation operation dispatched by InstallerEngine.
 */
sealed class InstallResult {
    data class Success(
        val destination: InstallDestination,
        val packageName: String,
        val details: String
    ) : InstallResult()

    data class Error(
        val destination: InstallDestination,
        val message: String,
        val cause: Throwable? = null
    ) : InstallResult()
}

/**
 * Core engine dispatching APK installation requests and managing staging lifecycle.
 *
 * Responsibilities:
 * 1. System Install Handoff: Delegates unvirtualized APK installation to Android's host
 *    PackageInstaller via FileProvider content URI with transient read grants.
 * 2. Virtual Environment Installation: Hands off staged APK to VirtualCore.installPackage()
 *    for userspace sandbox registration.
 * 3. Staging File Cleanup: Deletes the staged binary immediately upon virtual installation
 *    completion, or schedules cleanup for system handoffs once consumed.
 * 4. Orphan Cleanup: Purges stale/abandoned staging files on application launch.
 */
class InstallerEngine(
    private val virtualCoreProvider: () -> VirtualCore? = {
        try { VirtualCore.get() } catch (_: Exception) { null }
    },
    private val fileUriProvider: (Context, String, File) -> Uri = { ctx, auth, file ->
        FileProvider.getUriForFile(ctx, auth, file)
    }
) {

    companion object {
        private const val TAG = "InstallerEngine"
        private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

        /**
         * Cleans up orphaned staging APK files on application launch or crash recovery.
         * Default threshold is 1 hour for standard orphans, 24 hours for fallback safety.
         */
        fun cleanupOrphanedStagingFiles(context: Context, maxAgeMillis: Long = 60 * 60 * 1000L): Int {
            return ApkStagingManager.purgeOrphanedStagingFiles(context, maxAgeMillis)
        }
    }

    /**
     * Dispatches installation to the specified destination.
     */
    suspend fun install(
        context: Context,
        stagedApk: StagedApkInfo,
        destination: InstallDestination
    ): InstallResult = withContext(Dispatchers.IO) {
        when (destination) {
            is InstallDestination.System -> {
                handoffToSystemInstaller(context, stagedApk)
            }
            is InstallDestination.Virtual -> {
                installToVirtualEnvironment(context, stagedApk, destination)
            }
        }
    }

    /**
     * Hands off installation to the Android OS PackageInstaller.
     */
    fun handoffToSystemInstaller(
        context: Context,
        stagedApk: StagedApkInfo
    ): InstallResult {
        return try {
            val apkFile = stagedApk.stagedFile
            if (!apkFile.exists()) {
                return InstallResult.Error(
                    destination = InstallDestination.System,
                    message = "Staged APK file missing: ${apkFile.absolutePath}"
                )
            }

            val authority = "${context.packageName}$FILE_PROVIDER_SUFFIX"
            val contentUri: Uri = try {
                fileUriProvider(context, authority, apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate secure FileProvider URI for staged APK", e)
                return InstallResult.Error(
                    destination = InstallDestination.System,
                    message = "Failed to generate secure FileProvider URI: ${e.message}",
                    cause = e
                )
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)

            Log.i(TAG, "Successfully dispatched ${stagedApk.packageName} to host PackageInstaller")
            InstallResult.Success(
                destination = InstallDestination.System,
                packageName = stagedApk.packageName,
                details = "Handed off to Android System PackageInstaller."
            )
        } catch (e: Exception) {
            Log.e(TAG, "System package installer handoff failed", e)
            InstallResult.Error(
                destination = InstallDestination.System,
                message = "Failed to launch System PackageInstaller: ${e.message}",
                cause = e
            )
        }
    }

    /**
     * Installs the staged APK into a SANDBOXR Virtual Environment via VirtualCore.
     */
    fun installToVirtualEnvironment(
        context: Context,
        stagedApk: StagedApkInfo,
        destination: InstallDestination.Virtual
    ): InstallResult {
        val apkFile = stagedApk.stagedFile
        try {
            if (!apkFile.exists()) {
                return InstallResult.Error(
                    destination = destination,
                    message = "Staged APK file not found at ${apkFile.absolutePath}"
                )
            }

            val vc = virtualCoreProvider()
                ?: return InstallResult.Error(
                    destination = destination,
                    message = "VirtualCore runtime is not initialized."
                )

            Log.i(TAG, "Installing ${stagedApk.packageName} into virtual vault: ${destination.environmentId}")
            val installed: InstalledPackage = vc.installPackage(apkFile, destination.environmentId)

            Log.i(TAG, "Successfully installed ${installed.packageName} into ${destination.environmentName}")
            return InstallResult.Success(
                destination = destination,
                packageName = installed.packageName,
                details = "Installed into ${destination.environmentName} vault."
            )

        } catch (e: Exception) {
            Log.e(TAG, "VirtualCore package installation failed for ${stagedApk.packageName}", e)
            return InstallResult.Error(
                destination = destination,
                message = "Virtual environment installation failed: ${e.message}",
                cause = e
            )
        } finally {
            // Guarantee immediate staging cleanup on completion of virtual installation
            val deleted = ApkStagingManager.deleteStagedFile(apkFile)
            Log.d(TAG, "Virtual install staging file cleanup: $deleted (${apkFile.absolutePath})")
        }
    }

    /**
     * Deletes a staged file explicitly when no longer needed.
     */
    fun deleteStagedApk(stagedApk: StagedApkInfo): Boolean {
        return ApkStagingManager.deleteStagedFile(stagedApk.stagedFile)
    }
}
