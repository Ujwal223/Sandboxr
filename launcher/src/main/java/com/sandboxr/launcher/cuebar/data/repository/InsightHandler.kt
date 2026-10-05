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

package com.sandboxr.launcher.cuebar.data.repository

/**
 * Handles insight egress after user performs an action.
 *
 * This is a stub for the Sandboxr platform; the upstream AOSP implementation sends
 * egress signals to the PersonalContext intelligence service. Sandboxr does not integrate
 * with that service, so this class is intentionally a no-op.
 */
class InsightHandler {

    /**
     * Egresses the given insight (reports user action back to the intelligence service).
     * No-op in Sandboxr since PersonalContext ACE is not present.
     *
     * @param insight The insight object associated with the user interaction.
     */
    fun egress(insight: Any?) {
        // No-op: Sandboxr does not use the PersonalContext ACE service.
    }
}
