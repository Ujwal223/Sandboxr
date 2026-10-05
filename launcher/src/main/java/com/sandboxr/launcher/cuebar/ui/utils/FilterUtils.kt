/*
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

package com.sandboxr.launcher.cuebar.ui.utils

import com.sandboxr.launcher.cuebar.ui.viewmodel.ActionType
import com.sandboxr.launcher.cuebar.ui.viewmodel.ActionViewModel

object FilterUtils {
    /**
     * Filters a list of actions, combining actions with matching iconId and prioritizing MR actions.
     */
    fun filterActions(actions: List<ActionViewModel>): List<ActionViewModel> {
        val filteredActionMap = mutableMapOf<String, ActionViewModel>()
        actions.forEach { action ->
            filteredActionMap.getOrPut(action.icon.iconId) { action }
                .also { existingAction ->
                    if (existingAction !== action) {
                        filteredActionMap[action.icon.iconId] = existingAction.copy(
                            icon = existingAction.icon.copy(
                                repeatCount = existingAction.icon.repeatCount + 1
                            )
                        )
                    }
                }
        }
        val filteredList = mutableListOf<ActionViewModel>()
        for (action in filteredActionMap.values) {
            if (action.actionType == ActionType.MR) {
                filteredList.add(0, action)
            } else {
                filteredList.add(action)
            }
        }
        return filteredList
    }
}
