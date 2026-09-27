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

import javax.inject.Qualifier
import javax.inject.Scope

/**
 * Scope annotation for singleton items within the [LauncherAppComponent].
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Scope
annotation class LauncherAppSingleton

/**
 * Scope annotation for singletons associated with a Launcher Activity context.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Scope
annotation class ActivityContextSingleton

/**
 * Scope annotation for instances associated with a specific Display.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Scope
annotation class PerDisplayScope

/**
 * Qualifier for the Application [android.content.Context].
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationContext

/**
 * Qualifier for Window [android.content.Context].
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class WindowContext

/**
 * Qualifier for the Display ID integer.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DisplayId

/**
 * Qualifier for displays configured with window decorations.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DisplaysWithDecorations

/**
 * Qualifier for Activity [android.content.Context].
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ActivityContext

/**
 * Qualifier for the Main coroutine dispatcher.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher

/**
 * Qualifier for the IO coroutine dispatcher.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

/**
 * Qualifier for Application tag string.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class AppTag

/**
 * Qualifier for Activity tag string.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ActivityTag

/**
 * Qualifier for Display tag string.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DisplayTag
