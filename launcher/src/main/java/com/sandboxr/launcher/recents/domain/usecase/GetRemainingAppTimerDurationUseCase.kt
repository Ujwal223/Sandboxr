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

package com.sandboxr.launcher.recents.domain.usecase

import android.os.UserHandle
import com.sandboxr.launcher.recents.data.AppTimersRepository
import java.time.Duration
import javax.inject.Inject

class GetRemainingAppTimerDurationUseCase
@Inject
constructor(private val appTimersRepository: AppTimersRepository) {
    operator fun invoke(packageName: String, userHandle: UserHandle): Duration? {
        return appTimersRepository.getRemainingDuration(packageName, userHandle)
    }
}
