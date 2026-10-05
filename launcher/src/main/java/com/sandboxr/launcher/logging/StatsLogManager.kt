/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.sandboxr.launcher.logging

import android.content.Context
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import com.android.launcher3.logging.InstanceId
import com.sandboxr.launcher.dagger.LauncherAppSingleton
import com.sandboxr.launcher.model.data.ItemInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Provider

/**
 * Handles user event logging, interaction tracking, and latency diagnostics in Sandboxr Launcher.
 * Captures user interactions into an in-memory buffer for diagnostics, test verification, and analytics.
 */
open class StatsLogManager @AssistedInject constructor(
    @Assisted private val context: Context,
    private val loggerProvider: Provider<StatsLogger>? = null
) {

    @AssistedFactory
    interface StatsLogManagerFactory {
        fun create(@Assisted context: Context): StatsLogManager
    }

    interface EventEnum {
        val id: Int
    }

    /** Recorded event for testing and telemetry diagnostics. */
    data class LoggedEvent(
        val event: EventEnum,
        val itemInfo: Any? = null,
        val instanceId: InstanceId? = null,
        val rank: Int = 0,
        val srcState: Int = 0,
        val dstState: Int = 0,
        val cardinality: Int = 0,
        val editText: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /** Recorded latency metric for performance analysis. */
    data class LoggedLatency(
        val event: EventEnum,
        val latencyMs: Long,
        val instanceId: InstanceId? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    interface StatsLogger {
        fun withItemInfo(itemInfo: Any?): StatsLogger = this
        fun withInstanceId(instanceId: InstanceId?): StatsLogger = this
        fun withRank(rank: Int): StatsLogger = this
        fun withSrcState(srcState: Int): StatsLogger = this
        fun withDstState(dstState: Int): StatsLogger = this
        fun withCardinality(cardinality: Int): StatsLogger = this
        fun withEditText(editText: String?): StatsLogger = this
        fun log(event: EventEnum)
        fun sendToInteractionJankMonitor(event: EventEnum, v: View?) {}
    }

    interface StatsLatencyLogger {
        fun withInstanceId(instanceId: InstanceId?): StatsLatencyLogger = this
        fun withLatency(latency: Long): StatsLatencyLogger = this
        fun withType(type: LatencyType): StatsLatencyLogger = this
        fun log(event: EventEnum)

        enum class LatencyType(val id: Int) {
            UNKNOWN(0),
            COLD(1),
            HOT(2),
            WARM(3),
            TIMEOUT(4),
            FAIL(5)
        }

        companion object {
            const val LAUNCHER_LATENCY_PACKAGE_ID = 2
        }
    }

    interface StatsImpressionLogger {
        fun log(event: EventEnum)
    }

    open fun logger(): StatsLogger = DefaultStatsLogger()

    open fun latencyLogger(): StatsLatencyLogger = DefaultLatencyLogger()

    open fun impressionLogger(): StatsImpressionLogger = DefaultImpressionLogger()

    private inner class DefaultStatsLogger : StatsLogger {
        private var mItemInfo: Any? = null
        private var mInstanceId: InstanceId? = null
        private var mRank: Int = 0
        private var mSrcState: Int = 0
        private var mDstState: Int = 0
        private var mCardinality: Int = 0
        private var mEditText: String? = null

        override fun withItemInfo(itemInfo: Any?): StatsLogger {
            mItemInfo = itemInfo
            return this
        }

        override fun withInstanceId(instanceId: InstanceId?): StatsLogger {
            mInstanceId = instanceId
            return this
        }

        override fun withRank(rank: Int): StatsLogger {
            mRank = rank
            return this
        }

        override fun withSrcState(srcState: Int): StatsLogger {
            mSrcState = srcState
            return this
        }

        override fun withDstState(dstState: Int): StatsLogger {
            mDstState = dstState
            return this
        }

        override fun withCardinality(cardinality: Int): StatsLogger {
            mCardinality = cardinality
            return this
        }

        override fun withEditText(editText: String?): StatsLogger {
            mEditText = editText
            return this
        }

        override fun log(event: EventEnum) {
            val record = LoggedEvent(
                event = event,
                itemInfo = mItemInfo,
                instanceId = mInstanceId,
                rank = mRank,
                srcState = mSrcState,
                dstState = mDstState,
                cardinality = mCardinality,
                editText = mEditText
            )
            sRecordedEvents.add(record)
            if (sRecordedEvents.size > MAX_EVENT_HISTORY) {
                sRecordedEvents.removeAt(0)
            }
            Log.d(TAG, "StatsEvent [${event.id}]: $event, rank=$mRank, item=$mItemInfo")
        }

        override fun sendToInteractionJankMonitor(event: EventEnum, v: View?) {
            log(event)
        }
    }

    private inner class DefaultLatencyLogger : StatsLatencyLogger {
        private var mInstanceId: InstanceId? = null
        private var mLatency: Long = 0
        private var mType: StatsLatencyLogger.LatencyType = StatsLatencyLogger.LatencyType.UNKNOWN

        override fun withInstanceId(instanceId: InstanceId?): StatsLatencyLogger {
            mInstanceId = instanceId
            return this
        }

        override fun withLatency(latency: Long): StatsLatencyLogger {
            mLatency = latency
            return this
        }

        override fun withType(type: StatsLatencyLogger.LatencyType): StatsLatencyLogger {
            mType = type
            return this
        }

        override fun log(event: EventEnum) {
            val record = LoggedLatency(
                event = event,
                latencyMs = mLatency,
                instanceId = mInstanceId
            )
            sRecordedLatencies.add(record)
            if (sRecordedLatencies.size > MAX_EVENT_HISTORY) {
                sRecordedLatencies.removeAt(0)
            }
            Log.d(TAG, "LatencyEvent [${event.id}]: $event, duration=${mLatency}ms, type=$mType")
        }
    }

    private inner class DefaultImpressionLogger : StatsImpressionLogger {
        override fun log(event: EventEnum) {
            sRecordedEvents.add(LoggedEvent(event = event))
            Log.d(TAG, "ImpressionEvent [${event.id}]: $event")
        }
    }

    enum class LauncherEvent(override val id: Int) : EventEnum {
        IGNORE(-1),
        LAUNCHER_APP_LAUNCH_TAP(100),
        LAUNCHER_NOTIFICATION_DOT_OPEN(101),
        LAUNCHER_SHORTCUT_PINNED(102),
        LAUNCHER_WORKSPACE_ITEM_MOVED(103),
        LAUNCHER_WORKSPACE_ITEM_DELETED(104),
        LAUNCHER_FOLDER_OPEN(105),
        LAUNCHER_FOLDER_CLOSE(106),
        LAUNCHER_FOLDER_CREATED(107),
        LAUNCHER_FOLDER_CONVERTED_TO_ICON(108),
        LAUNCHER_ALLAPPS_OPEN(109),
        LAUNCHER_ALLAPPS_CLOSE(110),
        LAUNCHER_ALLAPPS_SEARCH(111),
        LAUNCHER_ALLAPPS_SWIPE_UP(112),
        LAUNCHER_ALLAPPS_SWIPE_DOWN(113),
        LAUNCHER_WIDGET_CONFIGURED(114),
        LAUNCHER_WIDGET_RESIZED(115),
        LAUNCHER_WIDGET_DELETED(116),
        LAUNCHER_APP_PAIR_LAUNCH(117),
        LAUNCHER_SPLIT_SCREEN_ENTER(118),
        LAUNCHER_DESKTOP_MODE_ENTER(119),
        LAUNCHER_TASKBAR_STASH(120),
        LAUNCHER_TASKBAR_UNSTASH(121),
        LAUNCHER_SETTINGS_OPEN(122),
        LAUNCHER_SETTINGS_CHANGE(123),
        LAUNCHER_HOME_SCREEN_FILES_COUNT(2554),
        LAUNCHER_STANDARD_GRID_MIGRATION(2200),
        LAUNCHER_ROW_SHIFT_GRID_MIGRATION(2201),
        LAUNCHER_STANDARD_ONE_GRID_MIGRATION(2205),
        LAUNCHER_ROW_SHIFT_ONE_GRID_MIGRATION(2206),
        LAUNCHER_HOME_SCREEN_FILES_OPEN_VIA_CONTEXT_MENU(2542),
        LAUNCHER_HOME_SCREEN_FILES_COPY_VIA_CONTEXT_MENU(2701),
        LAUNCHER_HOME_SCREEN_FILES_RENAME_VIA_CONTEXT_MENU(2660),
        LAUNCHER_HOME_SCREEN_FILES_DELETE_VIA_CONTEXT_MENU(2543),
        LAUNCHER_HOME_SCREEN_FILES_DELETE_VIA_DRAG_AND_DROP(2544),
        LAUNCHER_GRID_SIZE_2_BY_2(2207),
        LAUNCHER_GRID_SIZE_3_BY_3(2208),
        LAUNCHER_GRID_SIZE_4_BY_4(2209),
        LAUNCHER_GRID_SIZE_4_BY_5(2210),
        LAUNCHER_GRID_SIZE_4_BY_6(2211),
        LAUNCHER_GRID_SIZE_5_BY_5(2212),
        LAUNCHER_GRID_SIZE_5_BY_6(2213),
        LAUNCHER_GRID_SIZE_6_BY_5(2214),
        LAUNCHER_SECONDARY_DISPLAY_LAUNCH(2300),
        LAUNCHER_SECONDARY_DISPLAY_PIN(2301),
        LAUNCHER_SECONDARY_DISPLAY_UNPIN(2302)
    }

    enum class LauncherLatencyEvent(override val id: Int) : EventEnum {
        LAUNCHER_LATENCY_STARTUP_TOTAL_DURATION(1),
        LAUNCHER_LATENCY_STARTUP_WORKSPACE_LOADER_ASYNC(2),
        LAUNCHER_LATENCY_RECREATE_TASKBAR(3),
        LAUNCHER_LATENCY_APP_LAUNCH_ANIMATION(4),
        LAUNCHER_LATENCY_RECENTS_TRANSITION(5),
        LAUNCHER_LATENCY_ALL_APPS_TRANSITION(6)
    }

    companion object {
        private const val TAG = "StatsLogManager"
        private const val MAX_EVENT_HISTORY = 500

        private val sRecordedEvents = CopyOnWriteArrayList<LoggedEvent>()
        private val sRecordedLatencies = CopyOnWriteArrayList<LoggedLatency>()

        @JvmStatic
        fun newInstance(context: Context): StatsLogManager {
            return StatsLogManager(context.applicationContext ?: context)
        }

        /** Returns an unmodifiable snapshot of all recorded interaction events. */
        @VisibleForTesting
        fun getRecordedEvents(): List<LoggedEvent> = sRecordedEvents.toList()

        /** Returns an unmodifiable snapshot of all recorded latency events. */
        @VisibleForTesting
        fun getRecordedLatencies(): List<LoggedLatency> = sRecordedLatencies.toList()

        /** Clears recorded interaction and latency buffers (for testing). */
        @VisibleForTesting
        fun clearHistory() {
            sRecordedEvents.clear()
            sRecordedLatencies.clear()
        }

        /** Checks if a specific event has been logged. */
        @VisibleForTesting
        fun hasLoggedEvent(event: EventEnum): Boolean {
            return sRecordedEvents.any { it.event == event }
        }
    }
}
