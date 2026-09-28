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
package com.android.launcher3.ui

import com.sandboxr.launcher.model.BgDataModel.Callbacks
import com.sandboxr.launcher.model.IModelWriter
import com.sandboxr.launcher.model.data.ItemInfo

/**
 * Interface for a component that manages and dispatches UI state notifications.
 */
interface LauncherUiStateNotifier {

    fun addCallback(callback: Callbacks)

    fun removeCallback(callback: Callbacks)

    fun notifyItemModifiedOptimistically(item: ItemInfo)

    fun notifyModelChanged(changeLog: IModelWriter.ChangeLog, owner: Callbacks?)
}

/** A no-op implementation of [LauncherUiStateNotifier]. */
class NoOpLauncherUiStateNotifier : LauncherUiStateNotifier {
    override fun addCallback(callback: Callbacks) {}

    override fun removeCallback(callback: Callbacks) {}

    override fun notifyItemModifiedOptimistically(item: ItemInfo) {}

    override fun notifyModelChanged(changeLog: IModelWriter.ChangeLog, owner: Callbacks?) {}
}
