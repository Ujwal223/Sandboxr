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

package com.sandboxr.launcher.model

import android.content.Context
import com.sandboxr.launcher.LauncherModel
import com.android.launcher3.celllayout.CellPosMapper
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.model.BgDataModel.Callbacks
import com.sandboxr.launcher.model.BgDataModel.ModificationSource
import com.android.launcher3.ui.DefaultLauncherUiStateNotifier
import com.android.launcher3.ui.NoOpLauncherUiStateNotifier
import com.sandboxr.launcher.util.Executors.MAIN_EXECUTOR
import com.sandboxr.launcher.util.Executors.MODEL_EXECUTOR
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Provider

/**
 * A factory that constructs instances of [IModelWriter]. This factory encapsulates the assembly of
 * the model writer and its required UI state notifier.
 */
interface ModelWriterFactory {

    /**
     * Creates an [IModelWriter] instance with the provided configuration.
     */
    fun create(
        verifyChanges: Boolean,
        cellPosMapper: CellPosMapper,
        modificationSource: ModificationSource,
        owner: Callbacks?,
        modelExecutor: Executor = MODEL_EXECUTOR,
        uiExecutor: Executor? = null,
    ): IModelWriter
}

/** Default implementation of [ModelWriterFactory]. */
class ModelWriterFactoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val bgDataModel: BgDataModel,
    private val launcherModelProvider: Provider<LauncherModel>,
) : ModelWriterFactory {

    override fun create(
        verifyChanges: Boolean,
        cellPosMapper: CellPosMapper,
        modificationSource: ModificationSource,
        owner: Callbacks?,
        modelExecutor: Executor,
        uiExecutor: Executor?,
    ): IModelWriter {
        val launcherModel = launcherModelProvider.get()
        val actualUiExecutor = uiExecutor ?: MAIN_EXECUTOR
        val launcherStateNotifier =
            owner?.let {
                DefaultLauncherUiStateNotifier(
                    actualUiExecutor,
                    bgDataModel,
                    verifyChanges,
                    launcherModel,
                )
            } ?: NoOpLauncherUiStateNotifier()

        return ModelWriter(
            context,
            launcherModel,
            bgDataModel,
            cellPosMapper,
            modificationSource,
            launcherStateNotifier,
            owner,
            modelExecutor,
        )
    }
}
