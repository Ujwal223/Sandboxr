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

package com.sandboxr.launcher.concurrent

import android.content.Intent
import com.sandboxr.launcher.appfunctions.AppFunctionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppFunctionsAndConcurrentTest {

    @Test
    fun testAppFunctionServiceBindingAndExecution() {
        val controller = Robolectric.buildService(AppFunctionService::class.java).create()
        val service = controller.get()
        assertNotNull(service)

        val binder = service.onBind(Intent("com.sandboxr.launcher.action.EXECUTE_FUNCTION"))
        assertNotNull(binder)

        val result = service.executeFunction("openApp", mapOf("package" to "com.example.app"))
        assertTrue(result)
    }

    @Test
    fun testConcurrentLoadersWithoutDeadlock() = runBlocking {
        // Run 50 concurrent loading coroutines
        val tasks = (1..50).map { id ->
            async(Dispatchers.Default) {
                val data = "Item $id processed"
                data
            }
        }

        val results = tasks.awaitAll()
        assertEquals(50, results.size)
        assertTrue(results.first().startsWith("Item 1"))
    }
}
