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

package com.sandboxr.launcher

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Device profile parameters that remain invariant across orientation and window changes.
 */
open class InvariantDeviceProfile(
    @JvmField var numRows: Int = 5,
    @JvmField var numColumns: Int = 5,
    @JvmField var numDatabaseHotseatIcons: Int = 5
) {
    private val changeListeners = CopyOnWriteArrayList<OnIDPChangeListener>()

    fun interface OnIDPChangeListener {
        fun onIdpChanged(modelPropertiesChanged: Boolean)
    }

    fun addOnChangeListener(listener: OnIDPChangeListener) {
        changeListeners.add(listener)
    }

    fun removeOnChangeListener(listener: OnIDPChangeListener) {
        changeListeners.remove(listener)
    }

    fun notifyChange(modelPropertiesChanged: Boolean) {
        for (listener in changeListeners) {
            listener.onIdpChanged(modelPropertiesChanged)
        }
    }

    open fun getDeviceProfile(context: Context): DeviceProfile {
        return DeviceProfile.fromContext(context).apply {
            numRows = this@InvariantDeviceProfile.numRows
            numColumns = this@InvariantDeviceProfile.numColumns
            numShownHotseatIcons = this@InvariantDeviceProfile.numDatabaseHotseatIcons
        }
    }

    companion object {
        @JvmField
        val INSTANCE = InvariantDeviceProfile()
    }
}
