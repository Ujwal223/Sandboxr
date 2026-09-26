package com.sandboxr.installer

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Metadata descriptor for an APK file staged in the private Userspace Isolation staging directory.
 */
data class StagedApkInfo(
    val stagingId: String,
    val stagedFile: File,
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val fileSizeBytes: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val sourceUri: String,
    val stagedAtEpochMs: Long = System.currentTimeMillis(),
    val icon: Drawable? = null
)

/**
 * Result wrapper for APK staging operations.
 */
sealed class StagedApkResult {
    data class Success(val info: StagedApkInfo) : StagedApkResult()
    data class Error(val message: String, val cause: Throwable? = null) : StagedApkResult()
}

/**
 * High-performance APK Staging Manager.
 *
 * Implements the immediate streaming pipeline required by SANDBOXR:
 * When an incoming intent is received with a `content://` or `file://` URI, the transient
 * security access grant from the source (Downloads, Files app, external file manager) can expire
 * rapidly if the activity yields or delays reading.
 *
 * This manager immediately opens the InputStream and copies the byte stream to
 * `/data/data/com.sandboxr.app/staging/{uuid}.apk`, isolating it from external permission timeouts.
 */
object ApkStagingManager {

    private const val TAG = "ApkStagingManager"
    private const val STAGING_DIR_NAME = "staging"
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB buffer for high-throughput transfer

    /**
     * Resolves the staging directory within the app's private sandbox storage.
     */
    fun getStagingDirectory(context: Context): File {
        val stagingDir = File(context.filesDir, STAGING_DIR_NAME)
        if (!stagingDir.exists()) {
            stagingDir.mkdirs()
        }
        return stagingDir
    }

    /**
     * Asynchronously stages an APK from a URI onto the local filesystem.
     */
    suspend fun stageApk(context: Context, sourceUri: Uri): StagedApkResult = withContext(Dispatchers.IO) {
        stageApkSync(context, sourceUri)
    }

    /**
     * Synchronously streams the APK from the provided URI into the staging directory.
     * Must be called immediately upon intent dispatch to guarantee URI validity.
     */
    fun stageApkSync(context: Context, sourceUri: Uri): StagedApkResult {
        val stagingId = UUID.randomUUID().toString()
        val stagingDir = getStagingDirectory(context)
        val stagedFile = File(stagingDir, "$stagingId.apk")

        var inputStream: InputStream? = null
        return try {
            Log.d(TAG, "Starting immediate staging copy for URI: $sourceUri -> ${stagedFile.absolutePath}")

            // 1. Open input stream (contentResolver handles content://, file://, and android.resource://)
            inputStream = try {
                context.contentResolver.openInputStream(sourceUri)
            } catch (e: Exception) {
                if (sourceUri.scheme == "file" && sourceUri.path != null) {
                    val rawFile = File(sourceUri.path!!)
                    if (rawFile.exists() && rawFile.canRead()) {
                        FileInputStream(rawFile)
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }

            if (inputStream == null) {
                return StagedApkResult.Error("Unable to open input stream for URI: $sourceUri")
            }

            // 2. Stream directly to staging file
            inputStream.use { input ->
                FileOutputStream(stagedFile).use { output ->
                    input.copyTo(output, bufferSize = BUFFER_SIZE)
                    output.flush()
                }
            }

            // 3. Verify staged file integrity
            if (!stagedFile.exists() || stagedFile.length() == 0L) {
                deleteStagedFile(stagedFile)
                return StagedApkResult.Error("Staged file is empty or missing after stream copy: $sourceUri")
            }

            // 4. Parse APK archive metadata
            val stagedInfo = parseApkMetadata(context, stagedFile, sourceUri, stagingId)
            Log.i(TAG, "Successfully staged APK: ${stagedInfo.packageName} (${stagedInfo.fileSizeBytes} bytes)")
            StagedApkResult.Success(stagedInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage APK from $sourceUri", e)
            deleteStagedFile(stagedFile)
            StagedApkResult.Error(
                message = "Failed to copy APK to staging area: ${e.message}",
                cause = e
            )
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Parses application label, package name, version, and icon from the staged APK.
     */
    fun parseApkMetadata(
        context: Context,
        stagedFile: File,
        sourceUri: Uri,
        stagingId: String = stagedFile.nameWithoutExtension
    ): StagedApkInfo {
        val pm = context.packageManager
        val packageInfo = try {
            pm.getPackageArchiveInfo(
                stagedFile.absolutePath,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
            )
        } catch (e: Exception) {
            Log.w(TAG, "PackageManager failed to inspect archive: ${e.message}")
            null
        }

        return if (packageInfo != null) {
            // Set sourceDir so AssetManager can resolve string & drawable resources
            packageInfo.applicationInfo?.sourceDir = stagedFile.absolutePath
            packageInfo.applicationInfo?.publicSourceDir = stagedFile.absolutePath

            val label = try {
                packageInfo.applicationInfo?.loadLabel(pm)?.toString()
                    ?: (packageInfo.packageName.ifEmpty { stagedFile.nameWithoutExtension })
            } catch (_: Exception) {
                packageInfo.packageName.ifEmpty { stagedFile.nameWithoutExtension }
            }

            val icon = try {
                packageInfo.applicationInfo?.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            val pkgName = packageInfo.packageName.ifEmpty { "unknown.package" }
            val versionName = packageInfo.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: 0
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageInfo.applicationInfo?.minSdkVersion ?: 0
            } else {
                0
            }

            StagedApkInfo(
                stagingId = stagingId,
                stagedFile = stagedFile,
                packageName = pkgName,
                label = label,
                versionName = versionName,
                versionCode = versionCode,
                fileSizeBytes = stagedFile.length(),
                targetSdk = targetSdk,
                minSdk = minSdk,
                sourceUri = sourceUri.toString(),
                icon = icon
            )
        } else {
            // Fallback metadata descriptor for unparseable archives or test stubs
            StagedApkInfo(
                stagingId = stagingId,
                stagedFile = stagedFile,
                packageName = stagedFile.nameWithoutExtension,
                label = stagedFile.nameWithoutExtension,
                versionName = "1.0",
                versionCode = 1L,
                fileSizeBytes = stagedFile.length(),
                targetSdk = 35,
                minSdk = 29,
                sourceUri = sourceUri.toString(),
                icon = null
            )
        }
    }

    /**
     * Safely deletes a staged APK file.
     */
    fun deleteStagedFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete staged file ${file.absolutePath}: ${e.message}")
            false
        }
    }

    /**
     * Purges orphaned staging files older than [maxAgeMillis] (default 24 hours).
     * Useful on application startup and crash recovery to reclaim internal storage.
     */
    fun purgeOrphanedStagingFiles(context: Context, maxAgeMillis: Long = 24 * 60 * 60 * 1000L): Int {
        val stagingDir = getStagingDirectory(context)
        if (!stagingDir.exists() || !stagingDir.isDirectory) return 0

        val threshold = System.currentTimeMillis() - maxAgeMillis
        var purgedCount = 0

        val files = stagingDir.listFiles() ?: return 0
        for (file in files) {
            if (file.isFile && file.lastModified() <= threshold) {
                if (file.delete()) {
                    purgedCount++
                }
            }
        }
        if (purgedCount > 0) {
            Log.i(TAG, "Purged $purgedCount orphaned staging files older than $maxAgeMillis ms.")
        }
        return purgedCount
    }
}
