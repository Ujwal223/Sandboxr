/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.launcher3.icons

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppSingleton
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject

/**
 * Stub wrapper providing access to launcher-specific icon operations.
 * The full BaseIconFactory implementation requires android.graphics internals that are
 * not directly accessible in the current build configuration. This stub satisfies the
 * Dagger graph while the IconPool provides the injection point for ModelInitializer.
 */
class LauncherIcons
@AssistedInject
internal constructor(
    @ApplicationContext context: Context,
    idp: InvariantDeviceProfile,
    @Assisted private val pool: ConcurrentLinkedQueue<LauncherIcons>,
) : BaseIconFactory(context, 0, idp.iconBitmapSize) {

    /** Recycles this LauncherIcons instance back to the pool. */
    fun recycle() {
        pool.add(this)
    }

    override fun close() {
        recycle()
    }

    @AssistedFactory
    internal interface LauncherIconsFactory {
        fun create(pool: ConcurrentLinkedQueue<LauncherIcons>): LauncherIcons
    }

    /** Singleton pool of LauncherIcons instances, injected by Dagger. */
    @LauncherAppSingleton
    class IconPool @Inject internal constructor(private val factory: LauncherIconsFactory) {
        private var pool = ConcurrentLinkedQueue<LauncherIcons>()

        /** Returns a LauncherIcons instance from the pool, creating one if needed. */
        fun obtain(): LauncherIcons = pool.let { it.poll() ?: factory.create(it) }

        /** Clears the pool, invalidating all cached instances. */
        fun clear() {
            pool = ConcurrentLinkedQueue()
        }
    }

    companion object {
        @JvmStatic
        fun obtain(context: Context): LauncherIcons {
            // Delegate to the Dagger component via the bridge provider
            val comp = com.android.launcher3.dagger.LauncherComponentProvider.get(context)
            return (comp as? HasIconPool)?.iconPool?.obtain()
                ?: throw IllegalStateException("LauncherAppComponent does not implement HasIconPool")
        }

        @JvmStatic
        fun clearPool(context: Context) {
            val comp = com.android.launcher3.dagger.LauncherComponentProvider.get(context)
            (comp as? HasIconPool)?.iconPool?.clear()
        }
    }

    /** Optional interface for components that expose the IconPool. */
    interface HasIconPool {
        val iconPool: IconPool
    }
}
