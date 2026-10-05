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

import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.util.Log
import com.android.launcher3.testing.shared.TestProtocol
import com.sandboxr.launcher.debug.DebugTools
import com.sandboxr.launcher.debug.TestEventEmitter
import com.sandboxr.launcher.logging.StatsLogManager
import javax.inject.Inject

/**
 * Handles test protocol queries and control requests from test harnesses and TAPL.
 */
open class TestInformationHandler @Inject constructor() {

    protected var mContext: Context? = null

    open fun init(context: Context) {
        mContext = context
    }

    open fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val response = Bundle()
        Log.d(TAG, "TestInformationHandler.call: method=$method, arg=$arg")

        when (method) {
            TestProtocol.REQUEST_PID -> {
                response.putInt(TestProtocol.TEST_INFO_RESPONSE_FIELD, Process.myPid())
                return response
            }

            TestProtocol.REQUEST_FORCE_GC -> {
                Runtime.getRuntime().gc()
                return response
            }

            REQUEST_TOTAL_PSS_KB -> {
                val memInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memInfo)
                response.putInt(TestProtocol.TEST_INFO_RESPONSE_FIELD, memInfo.totalPss)
                return response
            }

            "request_enable_debug_tracing" -> {
                DebugTools.setPerformanceLoggingEnabled(true)
                return response
            }

            "request_disable_debug_tracing" -> {
                DebugTools.setPerformanceLoggingEnabled(false)
                return response
            }

            "request_clear_test_events" -> {
                TestEventEmitter.clearEvents()
                StatsLogManager.clearHistory()
                TestLogging.clearEvents()
                return response
            }

            "request_get_emitted_test_events" -> {
                val eventNames = ArrayList(TestEventEmitter.getEmittedEvents().map { it.name })
                response.putStringArrayList(TestProtocol.TEST_INFO_RESPONSE_FIELD, eventNames)
                return response
            }

            "request_diagnostics_summary" -> {
                val context = mContext
                if (context != null) {
                    val summary = DebugTools.getDiagnosticsSummary(context)
                    summary.forEach { (k, v) ->
                        response.putString(k, v.toString())
                    }
                }
                return response
            }

            else -> {
                // Return empty bundle for unhandled methods to indicate call was received
                return response
            }
        }
    }

    companion object {
        private const val TAG = "TestInformationHandler"
        const val REQUEST_TOTAL_PSS_KB = "total-pss-kb"

        @JvmStatic
        fun newInstance(context: Context): TestInformationHandler {
            return TestInformationHandler().apply { init(context) }
        }
    }
}
