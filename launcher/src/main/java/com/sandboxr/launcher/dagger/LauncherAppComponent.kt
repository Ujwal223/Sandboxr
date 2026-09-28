/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.sandboxr.launcher.dagger

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Root Application-level Dagger Component for the Sandboxr Launcher.
 */
@LauncherAppSingleton
@Component(modules = [LauncherAppModule::class, AppModule::class])
interface LauncherAppComponent {

    @ApplicationContext
    fun applicationContext(): Context

    @MainDispatcher
    fun mainDispatcher(): CoroutineDispatcher

    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher

    @AppTag
    fun appTag(): String

    fun getMainProcessInitializer(): com.sandboxr.launcher.MainProcessInitializer
    fun getLauncherPrefs(): com.sandboxr.launcher.LauncherPrefs

    fun activityContextComponentBuilder(): ActivityContextComponent.Builder
    fun perDisplayComponentBuilder(): PerDisplayComponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun bindContext(@ApplicationContext context: Context): Builder

        fun build(): LauncherAppComponent
    }
}

/**
 * Interface implemented by Application instances that expose a [LauncherAppComponent].
 */
interface LauncherAppComponentProvider {
    val appComponent: LauncherAppComponent
}

/**
 * Provider helper to retrieve or create the [LauncherAppComponent] from a Context.
 */
object LauncherComponentProvider {
    @Volatile
    private var testComponent: LauncherAppComponent? = null

    @JvmStatic
    fun get(context: Context): LauncherAppComponent {
        testComponent?.let { return it }

        val app = context.applicationContext
        if (app is LauncherAppComponentProvider) {
            return app.appComponent
        }

        return DaggerLauncherAppComponent.builder()
            .bindContext(app)
            .build()
    }

    @VisibleForTesting
    @JvmStatic
    fun setTestComponent(component: LauncherAppComponent?) {
        testComponent = component
    }
}
