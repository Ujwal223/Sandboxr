package com.sandboxr.virtual.core

import dalvik.system.DexClassLoader
import java.io.File
import java.net.URLClassLoader

/**
 * ClassLoader for dynamically loading guest APK code inside the isolated process.
 * Supports ART runtime with DexClassLoader and host JVM unit-testing fallback.
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
            try {
                DexClassLoader(dexPath, optimizedDirectory, librarySearchPath, parent)
            } catch (t: Throwable) {
                URLClassLoader(arrayOf(File(dexPath).toURI().toURL()), parent)
            }
        } else {
            URLClassLoader(arrayOf(File(dexPath).toURI().toURL()), parent)
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
        /**
         * Creates an isolated VClassLoader for a guest APK.
         */
        fun create(
            apkFile: File,
            envDataDir: File,
            parent: ClassLoader = VClassLoader::class.java.classLoader!!
        ): VClassLoader {
            val optDir = File(envDataDir, "code_cache/dex_opt").apply { mkdirs() }
            val libDir = File(envDataDir, "lib").apply { mkdirs() }

            return VClassLoader(
                dexPath = apkFile.absolutePath,
                optimizedDirectory = optDir.absolutePath,
                librarySearchPath = libDir.absolutePath,
                parent = parent
            )
        }
    }
}
