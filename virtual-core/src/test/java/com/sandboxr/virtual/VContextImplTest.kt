package com.sandboxr.virtual

import android.content.Context
import android.content.pm.ApplicationInfo
import com.sandboxr.virtual.core.VContextImpl
import com.sandboxr.virtual.core.VEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VContextImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseContext: Context
    private lateinit var vContext: VContextImpl
    private val envId = "env-context-test"
    private val guestPkg = "com.isolated.guest"

    @Before
    fun setUp() {
        baseContext = RuntimeEnvironment.getApplication()
        val env = VEnvironment.create(baseContext, envId, "Isolated Test", 0xFF00FF)
        val appInfo = ApplicationInfo().apply {
            packageName = guestPkg
            sourceDir = "/data/app/test.apk"
        }

        vContext = VContextImpl(
            base = baseContext,
            environment = env,
            guestPackageName = guestPkg,
            guestClassLoader = javaClass.classLoader!!,
            guestAppInfo = appInfo
        )
    }

    @Test
    fun testSharedPreferencesAreIsolatedPerEnvironmentAndPackage() {
        val sp = vContext.getSharedPreferences("user_config", Context.MODE_PRIVATE)
        sp.edit()
            .putString("auth_token", "GUEST_SECRET_TOKEN_999")
            .putInt("launch_count", 42)
            .commit()

        assertEquals("GUEST_SECRET_TOKEN_999", sp.getString("auth_token", null))
        assertEquals(42, sp.getInt("launch_count", 0))

        // Ensure host SharedPreferences is completely unpolluted
        val hostSp = baseContext.getSharedPreferences("user_config", Context.MODE_PRIVATE)
        assertFalse(hostSp.contains("auth_token"))
        assertFalse(hostSp.contains("launch_count"))

        // Check physical file location is inside environment data directory
        val envSpFile = File(vContext.dataDir, "shared_prefs/user_config.xml")
        assertTrue("SP file must be stored inside guest dataDir", envSpFile.exists())
    }

    @Test
    fun testDatabaseIsIsolatedInsideEnvironment() {
        val db = vContext.openOrCreateDatabase("messages.db", Context.MODE_PRIVATE, null)
        db.execSQL("CREATE TABLE test (id INTEGER, val TEXT);")
        db.execSQL("INSERT INTO test VALUES (1, 'VIRTUAL_DATA');")
        db.close()

        val dbFile = vContext.getDatabasePath("messages.db")
        assertTrue("Database file must exist in guest directory", dbFile.exists())
        assertTrue("Database path must contain environment ID", dbFile.absolutePath.contains(envId))

        // Ensure host database directory does not have this database
        val hostDbFile = baseContext.getDatabasePath("messages.db")
        assertFalse("Host database must not exist", hostDbFile.exists())

        // Verify databaseList
        val dbs = vContext.databaseList()
        assertTrue(dbs.contains("messages.db"))

        // Verify deleteDatabase
        val deleted = vContext.deleteDatabase("messages.db")
        assertTrue(deleted)
        assertFalse(dbFile.exists())
    }

    @Test
    fun testPathTraversalAttemptsAreBlockedInContextMethods() {
        try {
            vContext.getDatabasePath("../../escaped.db")
            fail("Expected SecurityException on database path traversal")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("traversal blocked"))
        }

        try {
            vContext.openFileInput("../../secret.txt")
            fail("Expected SecurityException on openFileInput path traversal")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("traversal attempt"))
        }

        try {
            vContext.openFileOutput("../../secret.txt", Context.MODE_PRIVATE)
            fail("Expected SecurityException on openFileOutput path traversal")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("traversal attempt"))
        }
    }
}
