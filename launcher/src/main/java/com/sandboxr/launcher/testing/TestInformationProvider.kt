/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.sandboxr.launcher.testing

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.android.launcher3.util.ContentProviderProxy
import com.sandboxr.launcher.debug.DebugTools

/**
 * ContentProvider proxy for exposing internal test state to TAPL and test harnesses.
 * Declared in AndroidManifest.xml under authority "${applicationId}.TestInfo".
 */
class TestInformationProvider : ContentProviderProxy() {

    override fun getProxy(ctx: Context): ProxyProvider? {
        if (ActivityManager.isRunningInTestHarness() || DebugTools.isDebugBuild(ctx)) {
            return object : ProxyProvider {
                override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
                    val handler = TestInformationHandler.newInstance(ctx)
                    val response = handler.call(method, arg, extras)
                    if (response == null) {
                        Log.e(TAG, "Couldn't handle method: $method; current handler=${handler.javaClass.simpleName}")
                    }
                    return response
                }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "TestInformationProvider"
    }
}
