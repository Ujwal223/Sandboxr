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

package com.sandboxr.launcher.homescreenfiles

import android.net.Uri
import android.os.Process
import android.os.UserHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenFilesTest {

    @Test
    fun testHomeScreenFileModel() {
        val uri = Uri.parse("content://media/external/file/123")
        val user = Process.myUserHandle()

        val file = HomeScreenFile(
            uri = uri,
            displayName = "confidential_document.pdf",
            mimeType = "application/pdf",
            isDirectory = false,
            user = user,
            envId = "env-secret-uuid"
        )

        assertEquals("confidential_document.pdf", file.displayName)
        assertEquals("application/pdf", file.mimeType)
        assertFalse(file.isDirectory)
        assertEquals("env-secret-uuid", file.envId)
    }

    @Test
    fun testHomeScreenFilesUtils() {
        val uri = Uri.parse("content://media/external/file/456")
        val launchIntent = HomeScreenFilesUtils.buildLaunchIntent(uri, "image/png")

        assertNotNull(launchIntent)
        assertEquals(uri, launchIntent.data)
        assertEquals("image/png", launchIntent.type)
        assertTrue(launchIntent.flags and HomeScreenFilesUtils.LAUNCH_INTENT_DEFAULT_FLAGS != 0)
    }

    @Test
    fun testNoOpProviderCompletableFutures() {
        val noOp = HomeScreenFilesNoOpProvider()

        assertTrue(noOp.onReady().isDone)
        assertFalse(noOp.canCreateNewFolder())
        assertFalse(noOp.canMoveToHomeScreen(emptyList()))
        assertFalse(noOp.createNewFolder(HomeScreenFilesUpdate.Extras()).get())
        assertFalse(noOp.renameFile(Uri.EMPTY, "new_name").get())
        assertFalse(noOp.deleteFile(Uri.EMPTY).get())
    }
}
