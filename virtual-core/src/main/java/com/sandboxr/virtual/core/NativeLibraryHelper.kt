package com.sandboxr.virtual.core

import android.os.Build
import android.util.Log
import java.io.File
import java.util.zip.ZipFile

/**
 * Extracts architecture-appropriate native shared libraries (.so) from guest APKs
 * into isolated environment library directories.
 */
object NativeLibraryHelper {
    private const val TAG = "NativeLibraryHelper"

    fun extractNativeLibrariesIfPresent(apkFile: File, destinationDir: File) {
        if (!apkFile.exists() || !apkFile.isFile) return
        destinationDir.mkdirs()

        try {
            ZipFile(apkFile).use { zip ->
                val isDalvik = System.getProperty("java.vm.name")?.contains("Dalvik", ignoreCase = true) == true
                val supportedAbis = if (isDalvik) {
                    try {
                        Build.SUPPORTED_ABIS?.toList() ?: listOf("arm64-v8a", "armeabi-v7a")
                    } catch (_: Throwable) {
                        listOf("arm64-v8a", "armeabi-v7a")
                    }
                } else {
                    listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
                }

                var matchedAbi: String? = null
                for (abi in supportedAbis) {
                    val prefix = "lib/$abi/"
                    val hasEntries = zip.entries().asSequence().any { it.name.startsWith(prefix) && it.name.endsWith(".so") }
                    if (hasEntries) {
                        matchedAbi = abi
                        break
                    }
                }

                if (matchedAbi != null) {
                    val prefix = "lib/$matchedAbi/"
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.startsWith(prefix) && entry.name.endsWith(".so") && !entry.isDirectory) {
                            val soFileName = entry.name.substringAfterLast('/')
                            val destFile = File(destinationDir, soFileName)
                            if (destFile.exists() && destFile.length() == entry.size) {
                                continue
                            }
                            if (destFile.exists()) {
                                destFile.setWritable(true)
                            }
                            zip.getInputStream(entry).use { input ->
                                destFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            try {
                                destFile.setReadOnly()
                                android.system.Os.chmod(destFile.absolutePath, 365) // 0555: r-xr-xr-x
                            } catch (_: Throwable) {}
                        }
                    }
                    Log.i(TAG, "Extracted native libraries for $matchedAbi from ${apkFile.name} to ${destinationDir.absolutePath}")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed extracting native libraries from ${apkFile.name}: ${t.message}")
        }
    }
}
