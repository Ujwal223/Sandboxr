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

import android.app.Application
import android.content.Context
import com.sandboxr.launcher.dagger.DaggerLauncherAppComponent
import com.sandboxr.launcher.dagger.LauncherAppComponent
import com.sandboxr.launcher.dagger.LauncherComponentProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherApplicationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        LauncherComponentProvider.setTestComponent(null)
    }

    @After
    fun tearDown() {
        LauncherComponentProvider.setTestComponent(null)
        LauncherApplication.setInstanceForTesting(null)
    }

    @Test
    fun testLauncherApplicationLifecycleAndSingleton() {
        val app = TestableLauncherApplication("com.ujwal.sandboxr")
        app.onCreate()

        // 1. Singleton access
        assertSame(app, LauncherApplication.instance)

        // 2. Dagger component resolution on demand
        val component = app.appComponent
        assertNotNull(component)

        // 3. DI bindings verification
        assertNotNull(component.applicationContext())
        assertNotNull(component.mainDispatcher())
        assertNotNull(component.ioDispatcher())
        assertTrue(component.appTag().contains("com.ujwal.sandboxr") || component.appTag().contains("SandboxrLauncherApp"))

        // 4. MainProcessInitializer resolution
        val initializer = component.getMainProcessInitializer()
        assertNotNull(initializer)
        assertTrue(initializer.isInitialized())
    }

    @Test
    fun testMainProcessInitializerIdempotency() {
        val initializer = MainProcessInitializer()
        assertFalse(initializer.isInitialized())

        initializer.init(context)
        assertTrue(initializer.isInitialized())

        // Second call must complete without exception and remain initialized
        initializer.init(context)
        assertTrue(initializer.isInitialized())
    }

    @Test
    fun testGuestSubprocessDetectionSkipsLauncherInit() {
        var hostInitCalled = false

        val guestApp = object : TestableLauncherApplication("com.ujwal.sandboxr:virtual_p0") {
            override fun initMainProcess() {
                hostInitCalled = true
                super.initMainProcess()
            }
        }

        assertFalse("Guest process must not be classified as main process", guestApp.isMainProcess())

        guestApp.onCreate()

        assertFalse("Main process initialization must be skipped for guest subprocesses", hostInitCalled)
    }

    @Test
    fun testHostProcessRunsMainProcessInit() {
        var hostInitCalled = false

        val hostApp = object : TestableLauncherApplication("com.ujwal.sandboxr") {
            override fun initMainProcess() {
                hostInitCalled = true
                super.initMainProcess()
            }
        }

        assertTrue("Host app without colon must be classified as main process", hostApp.isMainProcess())

        hostApp.onCreate()

        assertTrue("Host main process must execute initMainProcess()", hostInitCalled)
    }

    @Test
    fun testLauncherComponentProviderLookup() {
        val app = TestableLauncherApplication("com.ujwal.sandboxr")
        app.onCreate()

        val direct = app.appComponent
        val fromProvider = LauncherComponentProvider.get(app)
        val fromAospBridge = com.android.launcher3.dagger.LauncherComponentProvider.get(app)

        assertSame("Provider must return the Application's component", direct, fromProvider)
        assertSame("AOSP bridge must return the same component", direct, fromAospBridge)
    }

    @Test
    fun testCustomTestComponentInjection() {
        val app = TestableLauncherApplication("com.ujwal.sandboxr")
        val customComponent = DaggerLauncherAppComponent.builder()
            .bindContext(context)
            .build()

        app.initDaggerComponent(customComponent)

        assertSame(customComponent, app.appComponent)
        assertSame(customComponent, LauncherComponentProvider.get(app))
    }

    private open class TestableLauncherApplication(
        private val simulatedProcessName: String
    ) : LauncherApplication() {

        override fun getApplicationContext(): Context {
            return this
        }

        override fun getPackageName(): String {
            return "com.ujwal.sandboxr"
        }

        override fun getCurrentProcessName(): String {
            return simulatedProcessName
        }

        override fun createDaggerComponent(): LauncherAppComponent {
            val baseApp = RuntimeEnvironment.getApplication()
            return DaggerLauncherAppComponent.builder()
                .bindContext(baseApp)
                .build()
        }
    }
}
