package com.ujwal.sandboxr.vault

import android.content.Context
import android.util.Log
import com.ujwal.sandboxr.data.db.ClipboardMode
import com.ujwal.sandboxr.data.db.EnvironmentEntity
import com.ujwal.sandboxr.data.db.NetworkConfig
import com.ujwal.sandboxr.data.db.SandboxrDatabase
import com.sandboxr.virtual.VirtualCore
import com.sandboxr.virtual.core.VEnvironment
import com.sandboxr.virtual.model.SpoofProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Header metadata parsed from a .senv container file.
 */
data class SenvHeader(
    val formatVersion: Int,
    val iterationCount: Int,
    val salt: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SenvHeader) return false
        return formatVersion == other.formatVersion &&
                iterationCount == other.iterationCount &&
                salt.contentEquals(other.salt) &&
                iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        var result = formatVersion
        result = 31 * result + iterationCount
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

/**
 * Result of a .senv export operation.
 */
data class SenvExportResult(
    val outputFile: File,
    val environmentId: String,
    val environmentName: String,
    val packagesCount: Int,
    val totalSizeBytes: Long
)

/**
 * Result of a .senv import operation.
 */
data class SenvImportResult(
    val environment: EnvironmentEntity,
    val restoredPackageNames: List<String>
)

/**
 * Master Vault Manager for SANDBOXR Encrypted Environment Containers (.senv).
 *
 * Implements the canonical .senv AES-256-GCM specification:
 * - Magic Identifier: 4 bytes "SENV" (0x53, 0x45, 0x4E, 0x56)
 * - Format Version: 2 bytes (0x0001)
 * - PBKDF2-HMAC-SHA256 user password key derivation (100,000 rounds, 16-byte cryptographically secure salt)
 * - AES/GCM/NoPadding encryption with 12-byte IV and 128-bit authentication tag
 * - Encrypted payload bundling:
 *     1. environment.json (EnvironmentEntity metadata, hardware spoofing profile, network config)
 *     2. manifest.json (Package inventory, file paths, export timestamp)
 *     3. apks/ (Archived APK binaries for each installed application)
 *     4. data/ (Compressed filesystem tree of private app data /envs/{uuid}/data/)
 */
class SenvVaultManager(
    private val databaseProvider: (Context) -> SandboxrDatabase = { SandboxrDatabase.getInstance(it) },
    private val virtualCoreProvider: () -> VirtualCore? = {
        try { VirtualCore.get() } catch (_: Exception) { null }
    }
) {

    companion object {
        private const val TAG = "SenvVaultManager"

        // Binary header constants
        val SENV_MAGIC = byteArrayOf(0x53, 0x45, 0x4E, 0x56) // 'S', 'E', 'N', 'V'
        const val CURRENT_VERSION: Short = 1
        const val PBKDF2_ITERATIONS = 100_000
        const val SALT_LENGTH_BYTES = 16
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_LENGTH_BITS = 256

        private const val ENV_JSON_ENTRY = "environment.json"
        private const val MANIFEST_JSON_ENTRY = "manifest.json"
        private const val APKS_DIR_ENTRY = "apks/"
        private const val DATA_DIR_ENTRY = "data/"
    }

    /**
     * Inspects the header of a .senv file without decrypting its contents.
     */
    fun inspectHeader(file: File): SenvHeader {
        require(file.exists() && file.isFile) { "File does not exist: ${file.absolutePath}" }
        require(file.length() >= 4 + 2 + 4 + SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES) {
            "File too small to be a valid .senv container"
        }

        DataInputStream(FileInputStream(file)).use { dis ->
            val magic = ByteArray(4)
            dis.readFully(magic)
            require(magic.contentEquals(SENV_MAGIC)) {
                "Invalid .senv magic header: not a valid SANDBOXR container"
            }

            val version = dis.readShort().toInt()
            val iterations = dis.readInt()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            dis.readFully(salt)
            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            dis.readFully(iv)

            return SenvHeader(
                formatVersion = version,
                iterationCount = iterations,
                salt = salt,
                iv = iv
            )
        }
    }

    /**
     * Exports a complete environment, its APKs, and its private app data into an encrypted .senv file.
     */
    suspend fun exportEnvironment(
        context: Context,
        environmentId: String,
        password: CharArray,
        outputFile: File
    ): SenvExportResult = withContext(Dispatchers.IO) {
        val db = databaseProvider(context)
        val env = db.environmentDao().getEnvironmentById(environmentId)
            ?: error("Environment $environmentId does not exist in database")

        // 1. Generate cryptographic salt and IV
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { random.nextBytes(it) }

        // 2. Derive 256-bit AES key via PBKDF2-HMAC-SHA256
        val secretKey = deriveKey(password, salt, PBKDF2_ITERATIONS)

        // 3. Initialize AES-GCM Cipher
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        // 4. Resolve environment storage directories
        val baseEnvDir = File(context.filesDir.parentFile, "envs/$environmentId")
        val dataDir = File(baseEnvDir, "data")

        // 5. Gather installed packages for this environment
        val vc = virtualCoreProvider()
        val installedPackages = vc?.packageManagerService?.getInstalledPackages(0, environmentId) ?: emptyList()

        // 6. Write binary header and stream encrypted zip archive
        val tempOutput = File(outputFile.parentFile ?: context.cacheDir, "${outputFile.name}.tmp")
        if (tempOutput.exists()) tempOutput.delete()

        try {
            FileOutputStream(tempOutput).use { fos ->
                val dos = DataOutputStream(fos)
                // Write Header
                dos.write(SENV_MAGIC)
                dos.writeShort(CURRENT_VERSION.toInt())
                dos.writeInt(PBKDF2_ITERATIONS)
                dos.write(salt)
                dos.write(iv)
                dos.flush()

                // Encrypt payload stream
                CipherOutputStream(fos, cipher).use { cos ->
                    ZipOutputStream(cos).use { zos ->
                        // A. Write environment.json
                        zos.putNextEntry(ZipEntry(ENV_JSON_ENTRY))
                        val envJson = serializeEnvironment(env)
                        zos.write(envJson.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()

                        // B. Write manifest.json
                        val manifestJson = JSONObject().apply {
                            put("version", CURRENT_VERSION.toInt())
                            put("environmentId", env.id)
                            put("displayName", env.displayName)
                            put("exportedAt", System.currentTimeMillis())
                            val pkgsArray = JSONArray()
                            installedPackages.forEach { p ->
                                pkgsArray.put(JSONObject().apply {
                                    put("packageName", p.packageName)
                                    put("versionName", p.versionName ?: "1.0.0")
                                    val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                        p.longVersionCode
                                    } else {
                                        @Suppress("DEPRECATION")
                                        p.versionCode.toLong()
                                    }
                                    put("versionCode", vCode)
                                })
                            }
                            put("packages", pkgsArray)
                        }
                        zos.putNextEntry(ZipEntry(MANIFEST_JSON_ENTRY))
                        zos.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
                        zos.closeEntry()

                        // C. Write APK files
                        for (pkg in installedPackages) {
                            val sourcePath = pkg.applicationInfo?.sourceDir
                            val apkFile = if (sourcePath != null) File(sourcePath) else null
                            if (apkFile != null && apkFile.exists() && apkFile.canRead()) {
                                val safePkgName = File(pkg.packageName).name
                                if (safePkgName.contains("..") || safePkgName.contains("/") || safePkgName.contains("\\")) {
                                    continue
                                }
                                zos.putNextEntry(ZipEntry("$APKS_DIR_ENTRY$safePkgName.apk"))
                                FileInputStream(apkFile).use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }

                        // D. Recursively write private data files from data/
                        if (dataDir.exists() && dataDir.isDirectory) {
                            zipDirectory(dataDir, DATA_DIR_ENTRY, zos)
                        }
                    }
                }
            }

            if (outputFile.exists()) outputFile.delete()
            if (!tempOutput.renameTo(outputFile)) {
                tempOutput.copyTo(outputFile, overwrite = true)
                tempOutput.delete()
            }

            Log.i(TAG, "Successfully exported .senv vault for ${env.displayName} (${outputFile.length()} bytes)")
            SenvExportResult(
                outputFile = outputFile,
                environmentId = env.id,
                environmentName = env.displayName,
                packagesCount = installedPackages.size,
                totalSizeBytes = outputFile.length()
            )

        } catch (e: Exception) {
            tempOutput.delete()
            Log.e(TAG, "Failed to export .senv vault", e)
            throw e
        }
    }

    /**
     * Decrypts and restores an environment from an encrypted .senv file.
     *
     * @param targetEnvironmentId Optional new UUID to assign to the restored environment.
     *                            If null, uses the original UUID from the export.
     */
    suspend fun importEnvironment(
        context: Context,
        senvFile: File,
        password: CharArray,
        targetEnvironmentId: String? = null
    ): SenvImportResult = withContext(Dispatchers.IO) {
        val header = inspectHeader(senvFile)
        val secretKey = deriveKey(password, header.salt, header.iterationCount)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, header.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val headerSize = 4 + 2 + 4 + SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES
        var restoredEnv: EnvironmentEntity? = null
        val restoredPackages = mutableListOf<String>()

        FileInputStream(senvFile).use { fis ->
            // Skip the binary header
            val skipped = fis.skip(headerSize.toLong())
            require(skipped == headerSize.toLong()) { "Failed to read past .senv header" }

            CipherInputStream(fis, cipher).use { cis ->
                ZipInputStream(cis).use { zis ->
                    val tempExtractDir = File(context.cacheDir, "senv_temp_${System.currentTimeMillis()}").apply { mkdirs() }
                    try {
                        val extractedApkFiles = mutableListOf<File>()
                        val extractedDataFiles = mutableListOf<Pair<String, File>>()

                        var entry = zis.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (!entry.isDirectory) {
                                when {
                                    name == ENV_JSON_ENTRY -> {
                                        val content = zis.readBytes()
                                        val json = String(content, Charsets.UTF_8)
                                        restoredEnv = deserializeEnvironment(json)
                                    }
                                    name == MANIFEST_JSON_ENTRY -> {
                                        zis.readBytes()
                                    }
                                    name.startsWith(APKS_DIR_ENTRY) -> {
                                        val pkgFileName = name.removePrefix(APKS_DIR_ENTRY)
                                        if (pkgFileName.contains("..") || pkgFileName.startsWith("/") || pkgFileName.startsWith("\\")) {
                                            throw SecurityException("Path traversal (Zip Slip) attempt detected in container entry: $name")
                                        }
                                        val tempFile = File(tempExtractDir, pkgFileName)
                                        if (!tempFile.canonicalFile.path.startsWith(tempExtractDir.canonicalFile.path + File.separator)) {
                                            throw SecurityException("Path traversal (Zip Slip) attempt detected in container entry: $name")
                                        }
                                        tempFile.parentFile?.mkdirs()
                                        FileOutputStream(tempFile).use { fos -> zis.copyTo(fos) }
                                        extractedApkFiles.add(tempFile)
                                    }
                                    name.startsWith(DATA_DIR_ENTRY) -> {
                                        val relPath = name.removePrefix(DATA_DIR_ENTRY)
                                        if (relPath.contains("..") || relPath.startsWith("/") || relPath.startsWith("\\")) {
                                            throw SecurityException("Path traversal (Zip Slip) attempt detected in data entry: $name")
                                        }
                                        val tempFile = File(tempExtractDir, "data_${relPath.hashCode()}_${System.nanoTime()}")
                                        tempFile.parentFile?.mkdirs()
                                        FileOutputStream(tempFile).use { fos -> zis.copyTo(fos) }
                                        extractedDataFiles.add(Pair(relPath, tempFile))
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }

                        // Drain CipherInputStream to EOF to force AES-GCM MAC tag verification
                        val drainBuf = ByteArray(4096)
                        while (cis.read(drainBuf) != -1) {
                            // Draining remaining ciphertext to verify GCM tag
                        }

                        checkNotNull(restoredEnv) { "Corrupted .senv file: missing environment.json" }

                        // Apply new environment ID if provided or adjust to avoid collision
                        val finalEnvId = targetEnvironmentId ?: restoredEnv.id
                        val finalEnv = restoredEnv.copy(id = finalEnvId, isSystem = false)

                        // 1. Insert environment into database
                        val db = databaseProvider(context)
                        db.environmentDao().insert(finalEnv)

                        // 2. Setup VEnvironment filesystem directories
                        val vc = virtualCoreProvider()
                        vc?.createEnvironment(finalEnv.id, finalEnv.displayName, finalEnv.colorTag)

                        val baseEnvDir = File(context.filesDir.parentFile, "envs/${finalEnv.id}")
                        val dataDir = File(baseEnvDir, "data")
                        dataDir.mkdirs()

                        // 3. Restore private data files
                        for ((relPath, tempFile) in extractedDataFiles) {
                            val destFile = File(dataDir, relPath)
                            val canonicalDest = destFile.canonicalFile
                            val canonicalDataDir = dataDir.canonicalFile
                            if (!canonicalDest.path.startsWith(canonicalDataDir.path + File.separator)) {
                                throw SecurityException("Zip Slip destination traversal detected: $relPath escapes $dataDir")
                            }
                            destFile.parentFile?.mkdirs()
                            tempFile.copyTo(destFile, overwrite = true)
                            tempFile.delete()
                        }

                        // 4. Restore APKs and register into VirtualCore
                        val stagingDir = File(baseEnvDir, "apks")
                        stagingDir.mkdirs()

                        for (tempApk in extractedApkFiles) {
                            val safeName = File(tempApk.name).name
                            if (safeName.contains("..") || safeName.startsWith("/")) {
                                throw SecurityException("Path traversal in APK filename: ${tempApk.name}")
                            }
                            val apkFile = File(stagingDir, safeName)
                            if (!apkFile.canonicalFile.path.startsWith(stagingDir.canonicalFile.path + File.separator)) {
                                throw SecurityException("Path traversal in staged APK destination: ${apkFile.path}")
                            }
                            tempApk.copyTo(apkFile, overwrite = true)
                            tempApk.delete()
                            try {
                                if (vc != null) {
                                    val installed = vc.installPackage(apkFile, finalEnv.id)
                                    restoredPackages.add(installed.packageName)
                                } else {
                                    restoredPackages.add(tempApk.name.removeSuffix(".apk"))
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to reinstall package ${tempApk.name} during import: ${e.message}")
                                restoredPackages.add(tempApk.name.removeSuffix(".apk"))
                            }
                        }

                        Log.i(TAG, "Successfully imported .senv vault into environment: ${finalEnv.displayName} (${finalEnv.id})")
                        SenvImportResult(
                            environment = finalEnv,
                            restoredPackageNames = restoredPackages
                        )
                    } finally {
                        tempExtractDir.deleteRecursively()
                    }
                }
            }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun zipDirectory(rootDir: File, baseEntryPath: String, zos: ZipOutputStream) {
        val canonicalRoot = rootDir.canonicalFile
        val files = rootDir.listFiles() ?: return
        for (file in files) {
            val canonicalFile = file.canonicalFile
            // Verify symlink does not escape rootDir
            if (!canonicalFile.path.startsWith(canonicalRoot.path + File.separator) && canonicalFile != canonicalRoot) {
                Log.w(TAG, "Skipping symlink escaping container root: ${file.absolutePath} -> ${canonicalFile.absolutePath}")
                continue
            }
            if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
                Log.w(TAG, "Skipping symbolic link in container export: ${file.absolutePath}")
                continue
            }
            val entryPath = "$baseEntryPath${file.name}"
            if (file.isDirectory) {
                zos.putNextEntry(ZipEntry("$entryPath/"))
                zos.closeEntry()
                zipDirectory(file, "$entryPath/", zos)
            } else {
                zos.putNextEntry(ZipEntry(entryPath))
                FileInputStream(file).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /**
     * Serializes EnvironmentEntity to JSON string.
     */
    fun serializeEnvironment(env: EnvironmentEntity): String {
        return JSONObject().apply {
            put("id", env.id)
            put("displayName", env.displayName)
            put("colorTag", env.colorTag)
            put("iconEmoji", env.iconEmoji ?: JSONObject.NULL)
            put("sortOrder", env.sortOrder)
            put("networkConfig", env.networkConfig.name)
            put("proxyHost", env.proxyHost ?: JSONObject.NULL)
            put("proxyPort", env.proxyPort ?: JSONObject.NULL)
            put("wgConfig", env.wgConfig ?: JSONObject.NULL)
            put("dnsUpstream", env.dnsUpstream ?: JSONObject.NULL)
            put("adBlockEnabled", env.adBlockEnabled)
            put("clipboardMode", env.clipboardMode.name)
            put("gmsEnabled", env.gmsEnabled)
            put("freezeWhenInactive", env.freezeWhenInactive)
            put("notifIsolation", env.notifIsolation)
            put("isSystem", env.isSystem)
            put("createdAt", env.createdAt)

            // Hardware spoofing profile
            val hw = env.hardwareIds
            put("hardwareIds", JSONObject().apply {
                put("imei", hw.imei)
                put("androidId", hw.androidId)
                put("macAddress", hw.macAddress)
                put("serial", hw.serial)
                put("advertisingId", hw.advertisingId)
                put("wifiBssid", hw.wifiBssid)
            })
        }.toString(2)
    }

    /**
     * Deserializes EnvironmentEntity from JSON string.
     */
    fun deserializeEnvironment(jsonString: String): EnvironmentEntity {
        val obj = JSONObject(jsonString)
        val hwObj = obj.optJSONObject("hardwareIds")

        val spoofProfile = if (hwObj != null) {
            SpoofProfile(
                imei = hwObj.optString("imei", ""),
                androidId = hwObj.optString("androidId", ""),
                macAddress = hwObj.optString("macAddress", ""),
                serial = hwObj.optString("serial", ""),
                advertisingId = hwObj.optString("advertisingId", ""),
                wifiBssid = hwObj.optString("wifiBssid", "")
            )
        } else {
            SpoofProfile.generate()
        }

        return EnvironmentEntity(
            id = obj.getString("id"),
            displayName = obj.getString("displayName"),
            colorTag = obj.getLong("colorTag"),
            iconEmoji = if (obj.isNull("iconEmoji")) null else obj.optString("iconEmoji"),
            sortOrder = obj.optInt("sortOrder", 0),
            networkConfig = NetworkConfig.valueOf(obj.optString("networkConfig", NetworkConfig.DIRECT.name)),
            proxyHost = if (obj.isNull("proxyHost")) null else obj.optString("proxyHost"),
            proxyPort = if (obj.isNull("proxyPort")) null else obj.optInt("proxyPort"),
            wgConfig = if (obj.isNull("wgConfig")) null else obj.optString("wgConfig"),
            dnsUpstream = if (obj.isNull("dnsUpstream")) null else obj.optString("dnsUpstream"),
            adBlockEnabled = obj.optBoolean("adBlockEnabled", true),
            clipboardMode = ClipboardMode.valueOf(obj.optString("clipboardMode", ClipboardMode.ISOLATED.name)),
            gmsEnabled = obj.optBoolean("gmsEnabled", false),
            hardwareIds = spoofProfile,
            freezeWhenInactive = obj.optBoolean("freezeWhenInactive", true),
            notifIsolation = obj.optBoolean("notifIsolation", true),
            isSystem = obj.optBoolean("isSystem", false),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
