/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.sandboxr.launcher.util

import android.view.InputDevice
import android.view.MotionEvent

/** Util class for touch events and mouse button classification. */
object TouchUtil {

    /**
     * Detect ACTION_DOWN or ACTION_MOVE from mouse right button.
     */
    @JvmStatic
    fun isMouseRightClickDownOrMove(event: MotionEvent): Boolean {
        return event.isFromSource(InputDevice.SOURCE_MOUSE) &&
                ((event.buttonState and MotionEvent.BUTTON_SECONDARY) != 0)
    }
}
