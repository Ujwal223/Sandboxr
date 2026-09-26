package com.sandboxr.virtual

import com.sandboxr.virtual.core.VClassLoader
import com.sandboxr.virtual.core.VEnvironment
import com.sandboxr.virtual.model.SpoofProfile
import com.sandboxr.virtual.server.fs.VFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider

class VClientRuntimeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSpoofProfileLuhnComplianceAndFormats() {
        val profile = SpoofProfile.generate()

        // 1. Verify IMEI format and Luhn checksum
        assertEquals(15, profile.imei.length)
        assertTrue("IMEI should be valid Luhn number", SpoofProfile.isValidImei(profile.imei))

        // 2. Verify Android ID is 64-bit hex (16 chars)
        assertEquals(16, profile.androidId.length)
        assertTrue(profile.androidId.all { it in "0123456789abcdef" })

        // 3. Verify MAC address format (6 colon-separated pairs)
        val macParts = profile.macAddress.split(":")
        assertEquals(6, macParts.size)
        assertTrue(macParts.all { it.length == 2 })

        // 4. Verify Serial length
        assertEquals(16, profile.serial.length)

        // 5. Verify Advertising ID is valid UUID
        assertNotNull(java.util.UUID.fromString(profile.advertisingId))
    }

    @Test
    fun testFileSystemPathRedirection() {
        val rootDir = tempFolder.newFolder("env_root")
        val dataDir = File(rootDir, "data").apply { mkdirs() }
        val storageDir = File(rootDir, "storage").apply { mkdirs() }

        val env = VEnvironment(
            id = "test-env-uuid",
            name = "Test Env",
            color = 0xFF5B8AF5,
            rootDir = rootDir,
            dataDir = dataDir,
            storageDir = storageDir
        )

        val vfs = VFileSystem(env)
        val testPkg = "com.privacy.sample"

        val originalPath = "/data/data/$testPkg/files/secret.dat"
        val redirected = vfs.redirectPath(originalPath, testPkg)

        val expected = env.getPackageDataDir(testPkg).absolutePath + "/files/secret.dat"
        assertEquals(expected, redirected)
    }

    @Test
    fun testDynamicDexClassLoaderUserspaceExecution() {
        // Compile a standalone guest MainActivity dynamically into a JAR/APK structure
        val srcDir = tempFolder.newFolder("src")
        val binDir = tempFolder.newFolder("bin")
        val envDataDir = tempFolder.newFolder("env_data")

        val javaSource = """
            package com.guest.test;
            public class MainActivity {
                private boolean created = false;
                public void onCreate() {
                    this.created = true;
                }
                public boolean isCreated() {
                    return this.created;
                }
            }
        """.trimIndent()

        val sourceFile = File(srcDir, "MainActivity.java")
        sourceFile.writeText(javaSource)

        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler != null) {
            val result = compiler.run(null, null, null, "-d", binDir.absolutePath, sourceFile.absolutePath)
            assertEquals("Java compilation must succeed", 0, result)

            // Package into a guest APK / archive
            val apkFile = File(tempFolder.root, "guest-sample.apk")
            JarOutputStream(FileOutputStream(apkFile)).use { jos ->
                val classFile = File(binDir, "com/guest/test/MainActivity.class")
                jos.putNextEntry(JarEntry("com/guest/test/MainActivity.class"))
                jos.write(classFile.readBytes())
                jos.closeEntry()
            }

            // Load dynamically via VClassLoader
            val classLoader = VClassLoader.create(apkFile, envDataDir, javaClass.classLoader!!)
            val guestClass = classLoader.loadClass("com.guest.test.MainActivity")
            assertNotNull("Guest MainActivity class should be loaded in userspace", guestClass)

            // Instantiate and run lifecycle in userspace without system installation
            val instance = guestClass.getDeclaredConstructor().newInstance()
            val onCreateMethod = guestClass.getMethod("onCreate")
            onCreateMethod.invoke(instance)

            val isCreatedMethod = guestClass.getMethod("isCreated")
            val isCreated = isCreatedMethod.invoke(instance) as Boolean
            assertTrue("Guest MainActivity lifecycle onCreate should execute successfully", isCreated)
        }
    }
}
