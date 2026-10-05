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

package com.sandboxr.launcher.recents.data

import android.view.Surface
import com.sandboxr.launcher.DeviceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface RecentsRotationStateRepository {
    val rotationState: StateFlow<Int>
    fun setRotation(rotation: Int)
}

@Singleton
class RecentsRotationStateRepositoryImpl @Inject constructor() : RecentsRotationStateRepository {
    private val _rotationState = MutableStateFlow(Surface.ROTATION_0)
    override val rotationState: StateFlow<Int> = _rotationState.asStateFlow()

    override fun setRotation(rotation: Int) {
        _rotationState.value = rotation
    }
}

interface RecentsDeviceProfileRepository {
    val deviceProfile: StateFlow<DeviceProfile?>
    fun setDeviceProfile(dp: DeviceProfile)
}

@Singleton
class RecentsDeviceProfileRepositoryImpl @Inject constructor() : RecentsDeviceProfileRepository {
    private val _deviceProfile = MutableStateFlow<DeviceProfile?>(null)
    override val deviceProfile: StateFlow<DeviceProfile?> = _deviceProfile.asStateFlow()

    override fun setDeviceProfile(dp: DeviceProfile) {
        _deviceProfile.value = dp
    }
}
