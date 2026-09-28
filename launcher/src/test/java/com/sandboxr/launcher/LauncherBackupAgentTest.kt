/*
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher

import android.content.Context
import com.sandboxr.launcher.backuprestore.LauncherRestoreEventLogger
import com.sandboxr.launcher.provider.RestoreDbTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherBackupAgentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testBackupSchemeXmlIsValid() {
        val parser = context.resources.getXml(R.xml.backupscheme)
        assertNotNull(parser)

        var foundFullBackupContent = false
        var includeCount = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if ("full-backup-content" == parser.name) {
                    foundFullBackupContent = true
                } else if ("include" == parser.name) {
                    includeCount++
                }
            }
            eventType = parser.next()
        }

        assertTrue("backupscheme.xml must have <full-backup-content> root", foundFullBackupContent)
        assertTrue("backupscheme.xml must include files/databases", includeCount >= 10)
    }

    @Test
    fun testRestoreEventLoggerTracking() {
        val logger = LauncherRestoreEventLogger()
        logger.logLauncherItemsRestored("app", 5)
        logger.logSingleItemRestored("shortcut")
        logger.logLauncherItemsRestoreFailed("widget", 2, LauncherRestoreEventLogger.RestoreError.MISSING_WIDGET_PROVIDER)
        logger.logSingleItemFailed("app", LauncherRestoreEventLogger.RestoreError.APP_NO_LAUNCH_INTENT)

        assertEquals(6, logger.getTotalRestoredCount())
        assertEquals(3, logger.getTotalFailedCount())
    }

    @Test
    fun testRestoreDbTaskPendingState() {
        RestoreDbTask.setPending(context, false)
        assertFalse(RestoreDbTask.isPending(context))

        RestoreDbTask.setPending(context, true)
        assertTrue(RestoreDbTask.isPending(context))

        RestoreDbTask.setPending(context, false)
        assertFalse(RestoreDbTask.isPending(context))
    }

    private class TestLauncherBackupAgent(base: Context) : LauncherBackupAgent() {
        init {
            attachBaseContext(base)
        }
    }

    @Test
    fun testLauncherBackupAgentLifecycle() {
        val agent = TestLauncherBackupAgent(context)
        assertNotNull(agent)

        // Simulating onRestoreFinished
        agent.onRestoreFinished()

        // Should have set RestoreDbTask pending flag
        assertTrue(RestoreDbTask.isPending(agent))
    }
}
