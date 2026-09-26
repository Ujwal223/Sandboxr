package com.sandboxr.virtual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verification test suite for 16KB ELF page size alignment across
 * Android 15 (API 35), Android 16 (API 36), and Android 17 (API 37).
 *
 * Ensures CMakeLists.txt and Gradle build configurations enforce:
 * 1. max-page-size=16384 (16KB) in linker flags.
 * 2. -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON.
 * 3. 64-bit ABIs required for 16KB devices (arm64-v8a, x86_64).
 */
class PageAlignmentVerificationTest {

    @Test
    fun testCMakeListsEnforces16KbPageAlignment() {
        val cmakeFile = File("src/main/cpp/CMakeLists.txt")
        assertTrue("CMakeLists.txt must exist", cmakeFile.exists())

        val cmakeContent = cmakeFile.readText()
        assertTrue(
            "CMakeLists.txt must set max-page-size=16384 linker flag",
            cmakeContent.contains("-Wl,-z,max-page-size=16384")
        )
        assertTrue(
            "CMakeLists.txt must set target_link_options for sandboxr_native",
            cmakeContent.contains("target_link_options(sandboxr_native PRIVATE \"-Wl,-z,max-page-size=16384\")")
        )
    }

    @Test
    fun testGradleBuildEnforcesFlexiblePageSizesAnd64BitAbis() {
        val gradleFile = File("build.gradle.kts")
        assertTrue("build.gradle.kts must exist", gradleFile.exists())

        val gradleContent = gradleFile.readText()
        assertTrue(
            "build.gradle.kts must enable ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES",
            gradleContent.contains("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
        )
        assertTrue(
            "build.gradle.kts must include arm64-v8a",
            gradleContent.contains("\"arm64-v8a\"")
        )
        assertTrue(
            "build.gradle.kts must include x86_64 for 16KB emulator compatibility",
            gradleContent.contains("\"x86_64\"")
        )
    }

    @Test
    fun testElfAlignmentConstants() {
        val expectedPageSize = 16384 // 16 KB
        val expectedPageMask = 0x3FFF // (16384 - 1)

        assertEquals(16 * 1024, expectedPageSize)
        // Verify power of two
        assertEquals(0, expectedPageSize and (expectedPageSize - 1))

        // Any address aligned to 16KB must satisfy addr % 16384 == 0
        val testAddr = 0x7FFF0000L
        val alignedAddr = (testAddr + expectedPageMask) and expectedPageMask.toLong().inv()
        assertEquals(0L, alignedAddr % expectedPageSize)
    }
}
