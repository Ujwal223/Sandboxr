/*
 * Copyright (C) 2025 The Android Open Source Project
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

import com.google.common.util.concurrent.ListeningExecutorService
import com.sandboxr.launcher.concurrent.annotations.Background
import com.sandboxr.launcher.concurrent.annotations.LightweightBackground
import com.sandboxr.launcher.concurrent.annotations.ThreadPool
import com.sandboxr.launcher.concurrent.annotations.Ui
import dagger.Binds
import dagger.Module
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

/**
 * Module that stipulates the executors used across the launcher for non-blocking concurrent work.
 */
@Module
interface ExecutorsModule {

    @Binds
    @ThreadPool
    fun provideThreadPoolExecutor(
        @ThreadPool listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @ThreadPool
    fun provideThreadPoolExecutorService(
        @ThreadPool listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @Background
    fun provideBackgroundExecutor(
        @Background listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @Background
    fun provideBackgroundExecutorService(
        @Background listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @LightweightBackground
    fun provideLightweightBackgroundExecutor(
        @LightweightBackground listeningExecutorService: ListeningExecutorService
    ): Executor

    @Binds
    @LightweightBackground
    fun provideLightweightBackgroundExecutorService(
        @LightweightBackground listeningExecutorService: ListeningExecutorService
    ): ExecutorService

    @Binds
    @Ui
    fun provideUiExecutor(
        @Ui listeningExecutorService: ListeningExecutorService
    ): Executor
}
