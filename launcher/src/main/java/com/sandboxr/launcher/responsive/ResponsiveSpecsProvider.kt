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

package com.sandboxr.launcher.responsive

import com.sandboxr.launcher.responsive.ResponsiveSpec.Companion.ResponsiveSpecType
import com.sandboxr.launcher.responsive.ResponsiveSpec.DimensionType
import com.sandboxr.launcher.util.ResourceHelper

/**
 * Provider for responsive grid specs for workspace, folder, and all apps.
 */
class ResponsiveSpecsProvider(
    val type: ResponsiveSpecType,
    groupOfSpecs: List<ResponsiveSpecGroup<ResponsiveSpec>>
) {
    private val groupOfSpecs: List<ResponsiveSpecGroup<ResponsiveSpec>>

    init {
        this.groupOfSpecs =
            groupOfSpecs
                .onEach { group ->
                    check(group.widthSpecs.isNotEmpty() && group.heightSpecs.isNotEmpty()) {
                        "$LOG_TAG is incomplete - " +
                            "width list size = ${group.widthSpecs.size}; " +
                            "height list size = ${group.heightSpecs.size}."
                    }
                }
                .sortedBy { it.aspectRatio }
    }

    fun getSpecsByAspectRatio(aspectRatio: Float): ResponsiveSpecGroup<ResponsiveSpec> {
        check(aspectRatio > 0f) { "Invalid aspect ratio! The value should be bigger than 0." }

        val specsGroup = groupOfSpecs.firstOrNull { aspectRatio <= it.aspectRatio }
        checkNotNull(specsGroup) { "No available spec with aspectRatio within $aspectRatio." }

        return specsGroup
    }

    fun getCalculatedSpec(
        aspectRatio: Float,
        dimensionType: DimensionType,
        numCells: Int,
        availableSpace: Int,
    ): CalculatedResponsiveSpec {
        val specsGroup = getSpecsByAspectRatio(aspectRatio)
        val spec = specsGroup.getSpec(dimensionType, availableSpace)
        return CalculatedResponsiveSpec(aspectRatio, availableSpace, numCells, spec)
    }

    fun getCalculatedSpec(
        aspectRatio: Float,
        dimensionType: DimensionType,
        numCells: Int,
        availableSpace: Int,
        calculatedWorkspaceSpec: CalculatedResponsiveSpec
    ): CalculatedResponsiveSpec {
        check(calculatedWorkspaceSpec.spec.dimensionType == dimensionType) {
            "Invalid specType for CalculatedWorkspaceSpec. " +
                "Expected: $dimensionType - " +
                "Found: ${calculatedWorkspaceSpec.spec.dimensionType}}"
        }

        check(calculatedWorkspaceSpec.isResponsiveSpecType(ResponsiveSpecType.Workspace)) {
            "Invalid specType for CalculatedWorkspaceSpec. " +
                "Expected: ${ResponsiveSpecType.Workspace} - " +
                "Found: ${calculatedWorkspaceSpec.spec.specType}}"
        }

        val specsGroup = getSpecsByAspectRatio(aspectRatio)
        val spec = specsGroup.getSpec(dimensionType, availableSpace)
        return CalculatedResponsiveSpec(
            aspectRatio,
            availableSpace,
            numCells,
            spec,
            calculatedWorkspaceSpec
        )
    }

    companion object {
        private const val LOG_TAG = "ResponsiveSpecsProvider"

        @JvmStatic
        fun create(
            resourceHelper: ResourceHelper,
            type: ResponsiveSpecType
        ): ResponsiveSpecsProvider {
            val parser = ResponsiveSpecsParser(resourceHelper)
            val specs = parser.parseXML(type, ::ResponsiveSpec)
            return ResponsiveSpecsProvider(type, specs)
        }
    }
}
