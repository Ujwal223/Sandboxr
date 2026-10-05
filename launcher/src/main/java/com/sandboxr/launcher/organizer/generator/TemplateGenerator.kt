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

package com.sandboxr.launcher.organizer.generator

import android.graphics.Point
import com.sandboxr.launcher.LauncherSettings
import com.sandboxr.launcher.util.CellAndSpan
import javax.inject.Inject

/**
 * Predefined slot within a [Template].
 */
data class TemplateItem(
    val cellAndSpan: CellAndSpan,
    val itemTypeId: Int = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
)

/**
 * Layout specification for a single workspace screen page.
 */
data class Template(val items: List<TemplateItem>)

/**
 * Generates screen layout templates based on grid dimensions.
 */
interface TemplateGenerator {
    suspend fun generateTemplates(n: Int, gridSize: Point): List<Template>
}

/**
 * Generates preset grid templates (standard 4x4, 4x5, 5x5 layout slots).
 */
class PresetTemplateGenerator @Inject constructor() : TemplateGenerator {

    override suspend fun generateTemplates(n: Int, gridSize: Point): List<Template> {
        val cols = gridSize.x.coerceAtLeast(1)
        val rows = gridSize.y.coerceAtLeast(1)
        val slots = mutableListOf<TemplateItem>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                slots.add(TemplateItem(CellAndSpan(c, r, 1, 1)))
            }
        }
        return List(n) { Template(slots) }
    }
}
