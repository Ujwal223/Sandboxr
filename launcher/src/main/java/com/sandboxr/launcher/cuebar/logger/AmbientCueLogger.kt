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

package com.sandboxr.launcher.cuebar.logger

import android.content.Context
import android.util.Log

/**
 * Data object recording AmbientCue interaction and display events.
 */
data class AmbientCueEventReported(
    var displayDurationMillis: Long = 0L,
    var fulfilledWithMaIntentMillis: Long = 0L,
    var fulfilledWithMrIntentMillis: Long = 0L,
    var loseFocusMillis: Long = 0L,
    var maCount: Int = 0,
    var mrCount: Int = 0,
    var fulfilledWithMaIntent: Boolean = false,
    var fulfilledWithMrIntent: Boolean = false,
    var clickedCloseButton: Boolean = false,
    var reachedTimeout: Boolean = false,
    var packageName: String = "",
)

/**
 * Interface for logging AmbientCue UI events and lifecycle metrics.
 */
interface AmbientCueLogger {
    fun setAmbientCueDisplayStatus(maCount: Int, mrCount: Int)
    fun setPackageName(packageName: String)
    fun setLoseFocusMillis()
    fun setFulfilledWithMaStatus()
    fun setFulfilledWithMrStatus()
    fun setClickedCloseButtonStatus()
    fun setReachedTimeoutStatus()
    fun flushAmbientCueEventReported()
    fun clear()
    fun getLastReport(): AmbientCueEventReported
}

/**
 * Implementation of [AmbientCueLogger] for recording and logging interaction events.
 */
class AmbientCueLoggerImpl(
    private val context: Context? = null,
) : AmbientCueLogger {

    private var report = AmbientCueEventReported()
    private var displayTimeMillis: Long = 0L

    override fun setAmbientCueDisplayStatus(maCount: Int, mrCount: Int) {
        displayTimeMillis = System.currentTimeMillis()
        report.maCount = maCount
        report.mrCount = mrCount
    }

    override fun setPackageName(packageName: String) {
        report.packageName = packageName
    }

    override fun setLoseFocusMillis() {
        report.loseFocusMillis = System.currentTimeMillis()
    }

    override fun setFulfilledWithMaStatus() {
        report.fulfilledWithMaIntent = true
        report.fulfilledWithMaIntentMillis = System.currentTimeMillis() - displayTimeMillis
    }

    override fun setFulfilledWithMrStatus() {
        report.fulfilledWithMrIntent = true
        report.fulfilledWithMrIntentMillis = System.currentTimeMillis() - displayTimeMillis
    }

    override fun setClickedCloseButtonStatus() {
        report.clickedCloseButton = true
    }

    override fun setReachedTimeoutStatus() {
        report.reachedTimeout = true
    }

    override fun flushAmbientCueEventReported() {
        report.displayDurationMillis = System.currentTimeMillis() - displayTimeMillis
        Log.i(TAG, "flushAmbientCueEventReported: $report")
    }

    override fun clear() {
        report = AmbientCueEventReported()
        displayTimeMillis = 0L
    }

    override fun getLastReport(): AmbientCueEventReported = report

    companion object {
        private const val TAG = "AmbientCueLogger"
    }
}
