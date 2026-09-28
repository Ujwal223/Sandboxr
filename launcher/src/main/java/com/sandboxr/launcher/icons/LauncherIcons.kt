/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.icons

import android.content.Context
import com.sandboxr.launcher.InvariantDeviceProfile
import com.sandboxr.launcher.dagger.ApplicationContext
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.dagger.LauncherComponentProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

/**
 * Thread-safe wrapper providing access to [BaseIconFactory] with an object pool
 * to prevent excessive allocations during fast scroll and bulk app icon loading.
 */
class LauncherIcons
@AssistedInject
internal constructor(
    @ApplicationContext context: Context,
    idp: InvariantDeviceProfile,
    @Assisted private val pool: ConcurrentLinkedQueue<LauncherIcons>?,
) : BaseIconFactory(
    context = context,
    fillResIconDpi = idp.fillResIconDpi,
    iconBitmapSize = idp.iconBitmapSize,
    shapeDetection = true
) {

    fun recycle() {
        pool?.add(this)
    }

    override fun close() {
        recycle()
    }

    @AssistedFactory
    internal interface LauncherIconsFactory {
        fun create(pool: ConcurrentLinkedQueue<LauncherIcons>): LauncherIcons
    }

    @LauncherAppSingleton
    class IconPool @Inject internal constructor(private val factory: LauncherIconsFactory) {
        private var pool = ConcurrentLinkedQueue<LauncherIcons>()

        fun obtain(): LauncherIcons = pool.poll() ?: factory.create(pool)

        fun clear() {
            pool.clear()
        }
    }

    companion object {
        private val localPool = ConcurrentLinkedQueue<LauncherIcons>()

        @JvmStatic
        fun obtain(context: Context): LauncherIcons {
            val fromLocal = localPool.poll()
            if (fromLocal != null) return fromLocal

            val idp = InvariantDeviceProfile.INSTANCE(context)
            return LauncherIcons(context.applicationContext, idp, localPool)
        }

        @JvmStatic
        fun clearPool(context: Context) {
            localPool.clear()
        }
    }
}
