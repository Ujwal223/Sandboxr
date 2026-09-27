package com.sandboxr.virtual.core

import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File
import java.net.URLClassLoader

/**
 * ClassLoader for dynamically loading guest APK code inside the isolated process.
 * Supports ART runtime with DexClassLoader and host JVM unit-testing fallback.
 * Strictly complies with Android 14+ (API 34+) Dynamic Code Loading (DCL) read-only security enforcement.
 */
class VClassLoader(
    dexPath: String,
    optimizedDirectory: String,
    librarySearchPath: String?,
    parent: ClassLoader
) : ClassLoader(parent) {

    private val delegate: ClassLoader = run {
        val isDalvik = System.getProperty("java.vm.name")?.contains("Dalvik", ignoreCase = true) == true
        if (isDalvik) {
            // Android 14+ (API 34+) strictly requires dynamically loaded DEX/APK files to be read-only.
            dexPath.split(File.pathSeparator).forEach { path ->
                if (path.isNotBlank()) {
                    ensureFileReadOnly(File(path))
                }
            }
            try {
                DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent)
            } catch (t: Throwable) {
                Log.e(TAG, "DexClassLoader failed to load $dexPath: ${t.message}", t)
                throw t
            }
        } else {
            val urls = dexPath.split(File.pathSeparator).mapNotNull { path ->
                if (path.isNotBlank()) File(path).toURI().toURL() else null
            }.toTypedArray()
            URLClassLoader(urls, parent)
        }
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return try {
            delegate.loadClass(name)
        } catch (e: ClassNotFoundException) {
            super.loadClass(name, resolve)
        }
    }

    override fun findClass(name: String): Class<*> {
        return try {
            val method = delegate.javaClass.getDeclaredMethod("findClass", String::class.java)
            method.isAccessible = true
            method.invoke(delegate, name) as Class<*>
        } catch (e: Exception) {
            super.findClass(name)
        }
    }

    companion object {
        private const val TAG = "VClassLoader"

        /**
         * Enforces read-only permissions on a DEX/APK file to comply with Android 14+ (API 34+)
         * Dynamic Code Loading (DCL) security enforcement.
         * Prevents ART from throwing: "Attempt to load writable dex file".
         */
        fun ensureFileReadOnly(file: File) {
            if (!file.exists()) return
            try {
                file.setReadOnly()
                file.setWritable(false, false)
            } catch (_: Throwable) {}
            try {
                // Posix chmod 0444 (read-only for user, group, others: 256 + 32 + 4 = 292)
                android.system.Os.chmod(file.absolutePath, 292)
            } catch (_: Throwable) {}
        }

        /**
         * Creates an isolated VClassLoader for a guest APK.
         * Supports split APK modules, optional native library directory mapping,
         * and automatic APK native library extraction.
         */
        fun create(
            apkFile: File,
            envDataDir: File,
            parent: ClassLoader = VClassLoader::class.java.classLoader!!,
            nativeLibraryDir: String? = null,
            splitApkFiles: List<File> = emptyList()
        ): VClassLoader {
            // Guarantee base APK and all split APK files are read-only prior to class loading
            ensureFileReadOnly(apkFile)
            splitApkFiles.forEach { ensureFileReadOnly(it) }

            val optDir = File(envDataDir, "code_cache/dex_opt").apply { mkdirs() }
            val libDir = File(envDataDir, "lib").apply { mkdirs() }

            // Extract native libraries from base APK and any split APKs
            NativeLibraryHelper.extractNativeLibrariesIfPresent(apkFile, libDir)
            splitApkFiles.forEach { splitFile ->
                NativeLibraryHelper.extractNativeLibrariesIfPresent(splitFile, libDir)
            }

            val searchPaths = listOfNotNull(
                libDir.absolutePath,
                nativeLibraryDir?.takeIf { it.isNotBlank() && File(it).exists() }
            ).joinToString(File.pathSeparator)

            val allApks = (listOf(apkFile) + splitApkFiles).filter { it.exists() }
            val combinedDexPath = allApks.joinToString(File.pathSeparator) { it.absolutePath }

            return VClassLoader(
                dexPath = combinedDexPath,
                optimizedDirectory = optDir.absolutePath,
                librarySearchPath = searchPaths.ifBlank { null },
                parent = parent
            )
        }
    }
}
