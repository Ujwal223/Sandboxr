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
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

/**
 * Module providing dependencies bound to an Activity Context.
 */
@Module
class LauncherActivityContextModule {

    @Provides
    @ActivityContextSingleton
    @ActivityTag
    fun provideActivityTag(): String = "LauncherActivityContext"
}

/**
 * Subcomponent for Dagger injection within an Activity lifecycle.
 */
@ActivityContextSingleton
@Subcomponent(modules = [LauncherActivityContextModule::class])
interface ActivityContextComponent {

    @ActivityContext
    fun activityContext(): Context

    @ActivityTag
    fun activityTag(): String

    val popupDataProvider: com.sandboxr.launcher.popup.PopupDataProvider?
        get() = null

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun bindContext(@ActivityContext context: Context): Builder

        fun build(): ActivityContextComponent
    }
}

/**
 * Interface implemented by Activities that host an [ActivityContextComponent].
 */
interface ActivityComponentProvider {
    val activityComponent: ActivityContextComponent
}
