/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.dagger.DaggerLauncherAppComponent
import com.sandboxr.launcher.dagger.LauncherAppComponent
import com.sandboxr.launcher.dagger.LauncherAppComponentProvider
import com.sandboxr.launcher.dagger.LauncherComponentProvider
import java.io.File

/**
 * Main application class for Sandboxr Launcher.
 * Provides application-level initialization, Dagger dependency injection bootstrap,
 * and integration points with Sandboxr's virtualization engine.
 */
open class LauncherApplication : Application(), LauncherAppComponentProvider {

    companion object {
        private const val TAG = "LauncherApplication"

        @Volatile
        private var sInstance: LauncherApplication? = null

        @JvmStatic
        val instance: LauncherApplication
            get() = checkNotNull(sInstance) {
                "LauncherApplication has not been initialized yet"
            }

        @VisibleForTesting
        fun setInstanceForTesting(application: LauncherApplication?) {
            sInstance = application
        }
    }

    @Volatile
    private var mAppComponent: LauncherAppComponent? = null

    /**
     * Retrieves the Application-level Dagger Component with thread-safe lazy initialization.
     * Content providers can be initialized before Application.onCreate() (b/36917845#comment4),
     * so this on-demand initialization guarantees the component is always accessible.
     * In Java bytecode, this generates public LauncherAppComponent getAppComponent().
     */
    override val appComponent: LauncherAppComponent
        get() {
            var component = mAppComponent
            if (component == null) {
                synchronized(this) {
                    component = mAppComponent
                    if (component == null) {
                        component = createDaggerComponent()
                        mAppComponent = component
                    }
                }
            }
            return component!!
        }

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        sInstance = this

        // Check if this is the main process before running UI and singleton initialization.
        // Guest virtual container processes (or isolated services) must not run host launcher setup.
        if (isMainProcess()) {
            initMainProcess()
        } else {
            Log.i(TAG, "Subprocess '${getCurrentProcessName()}' started; skipping host launcher main process init.")
        }
    }

    /**
     * Factory method for creating the default [LauncherAppComponent].
     */
    protected open fun createDaggerComponent(): LauncherAppComponent {
        return DaggerLauncherAppComponent.builder()
            .bindContext(this)
            .build()
    }

    /**
     * Initializes the Dagger component with a custom component instance (e.g. for testing).
     */
    open fun initDaggerComponent(component: LauncherAppComponent) {
        synchronized(this) {
            mAppComponent = component
            LauncherComponentProvider.setTestComponent(component)
        }
    }

    /**
     * Executes main process one-time initializations.
     */
    @CallSuper
    open fun initMainProcess() {
        Log.i(TAG, "Initializing Sandboxr Launcher main process...")
        val initializer = LauncherComponentProvider.get(this).getMainProcessInitializer()
        initializer.init(this)
    }

    /**
     * Determines whether the current process is the host main process or a guest virtual container.
     * Guest processes launched by VirtualCore contain ':' (e.g. ':virtual_p0').
     */
    open fun isMainProcess(): Boolean {
        val processName = getCurrentProcessName()
        return processName == null || !processName.contains(':')
    }

    /**
     * Returns the name of the current process.
     */
    protected open fun getCurrentProcessName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val appProcessName = Application.getProcessName()
            if (!appProcessName.isNullOrBlank()) {
                return appProcessName
            }
        }
        return getProcessNameFromProc()
    }

    private fun getProcessNameFromProc(): String? {
        return try {
            val cmdlineFile = File("/proc/self/cmdline")
            if (cmdlineFile.exists()) {
                val cmdline = cmdlineFile.readText().trim().trimEnd('\u0000')
                if (cmdline.isNotEmpty()) cmdline else null
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
