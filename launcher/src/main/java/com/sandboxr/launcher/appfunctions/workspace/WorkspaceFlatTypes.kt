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

package com.sandboxr.launcher.appfunctions.workspace

/**
 * Annotation for AppFunction serialization.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AppFunctionSerializable(val isDescribedByKDoc: Boolean = false)

/**
 * Launcher workspace spec: screens and hotseat.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class WorkspaceSpec(
    val screens: List<WorkspaceScreenSpec>,
    val hotseat: HotseatSpec,
    val rows: Int?,
    val columns: Int?
)

/**
 * A workspace screen containing items.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class WorkspaceScreenSpec(
    val items: List<WorkspaceItemSpec>
)

/**
 * Hotseat bar containing items.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class HotseatSpec(
    val items: List<HotseatItemSpec>
)

/**
 * Spec for an item placed on a workspace screen.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class WorkspaceItemSpec(
    val id: Int,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int,
    val spanY: Int,
    val title: String,
    val packageName: String,
    val itemType: Int
)

/**
 * Spec for an item placed in the hotseat dock.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class HotseatItemSpec(
    val id: Int,
    val rank: Int,
    val title: String,
    val packageName: String,
    val itemType: Int
)

/**
 * Spec for an installed app not yet placed on workspace.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class UnplacedAppSpec(
    val packageName: String,
    val className: String,
    val label: String
)

/**
 * Spec for an installed widget not yet placed on workspace.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class UnplacedWidgetSpec(
    val providerPackage: String,
    val providerClass: String,
    val label: String,
    val minSpanX: Int,
    val minSpanY: Int
)

/**
 * Parameters for removing an item from workspace or hotseat.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class RemoveItemParamsSpec(
    val id: Int,
    val container: Int
)
