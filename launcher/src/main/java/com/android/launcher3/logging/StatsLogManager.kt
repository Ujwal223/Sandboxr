/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.launcher3.logging

import android.content.Context
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.model.data.ItemInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * Handles user event logging.
 * Lightweight stub: all logging methods are no-ops.
 * Actual stats events are a GrapheneOS extension that Sandboxr does not currently implement.
 */
class StatsLogManager
@AssistedInject
constructor(
    @Assisted context: Context,
    private val loggerProvider: Provider<StatsLogger>,
) {

    /** Factory interface for Dagger assisted injection. */
    @AssistedFactory
    interface StatsLogManagerFactory {
        fun create(@Assisted context: Context): StatsLogManager
    }

    interface EventEnum {
        val id: Int
    }

    /** No-op stats logger stub. */
    interface StatsLogger {
        fun withItemInfo(itemInfo: Any?) = this
        fun withInstanceId(instanceId: InstanceId?) = this
        fun withRank(rank: Int) = this
        fun withSrcState(srcState: Int) = this
        fun withDstState(dstState: Int) = this
        fun withCardinality(cardinality: Int) = this
        fun log(event: EventEnum) {}
        fun sendToInteractionJankMonitor(event: EventEnum, v: android.view.View?) {}
    }

    /** No-op latency logger stub. */
    interface StatsLatencyLogger {
        fun withInstanceId(instanceId: InstanceId?) = this
        fun withLatency(latency: Long) = this
        fun log(event: EventEnum) {}
    }

    /** No-op impression logger stub. */
    interface StatsImpressionLogger {
        fun log(event: EventEnum) {}
    }

    fun logger(): StatsLogger = loggerProvider.get()

    fun latencyLogger(): StatsLatencyLogger = NoOpLatencyLogger

    fun impressionLogger(): StatsImpressionLogger = NoOpImpressionLogger

    private object NoOpLatencyLogger : StatsLatencyLogger
    private object NoOpImpressionLogger : StatsImpressionLogger

    enum class LauncherEvent(override val id: Int) : EventEnum {
        IGNORE(-1),
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
        LAUNCHER_OPEN_APP_PAIR_LONG_PRESS_MENU(2001),
        LAUNCHER_OPEN_FOLDER_LONG_PRESS_MENU(2002),
        LAUNCHER_OPEN_WIDGET_LONG_PRESS_MENU(2003),
        LAUNCHER_OPEN_APP_LONG_PRESS_MENU(2004),
        LAUNCHER_OPEN_APP_SHORTCUT_LONG_PRESS_MENU(2005),
        LAUNCHER_CLOSE_APP_PAIR_LONG_PRESS_MENU(2006),
        LAUNCHER_CLOSE_FOLDER_LONG_PRESS_MENU(2007),
        LAUNCHER_CLOSE_WIDGET_LONG_PRESS_MENU(2008),
        LAUNCHER_CLOSE_APP_LONG_PRESS_MENU(2009),
        LAUNCHER_CLOSE_APP_SHORTCUT_LONG_PRESS_MENU(2010),
        LAUNCHER_SYSTEM_SHORTCUT_APP_INFO_TAP(2011),
        LAUNCHER_SYSTEM_SHORTCUT_WIDGETS_TAP(2012),
        LAUNCHER_SYSTEM_SHORTCUT_INSTALL_TAP(2013),
        LAUNCHER_SYSTEM_SHORTCUT_DONT_SUGGEST_APP_TAP(2014),
        LAUNCHER_SYSTEM_SHORTCUT_PAUSE_TAP(2015),
        LAUNCHER_SYSTEM_SHORTCUT_ENABLE_APP_LOCK_TAP(2016),
        LAUNCHER_SYSTEM_SHORTCUT_DISABLE_APP_LOCK_TAP(2017),
        LAUNCHER_PRIVATE_SPACE_INSTALL_SYSTEM_SHORTCUT_TAP(2018),
        LAUNCHER_PRIVATE_SPACE_UNINSTALL_SYSTEM_SHORTCUT_TAP(2019),
        LAUNCHER_DISMISS_PREDICTION_UNDO(2020),
        LAUNCHER_TAP_TO_ADD_TO_HOME_SCREEN_FROM_ALL_APPS(2021),
        LAUNCHER_ALL_APPS_TAP_OR_LONGPRESS(2022),
        LAUNCHER_SETTINGS_BUTTON_TAP_OR_LONGPRESS(2023),
        LAUNCHER_WIDGETSTRAY_BUTTON_TAP_OR_LONGPRESS(2024),
        LAUNCHER_CREATE_NEW_FOLDER_BUTTON_TAP_OR_LONGPRESS(2025),
        LAUNCHER_TAP_TO_ADD_DEEP_SHORTCUT(2446),
    }

    companion object {
        @JvmStatic
        fun newInstance(context: Context): StatsLogManager {
            return StatsLogManager(context) {
                object : StatsLogger {}
            }
        }
    }

    enum class LauncherLatencyEvent(override val id: Int) : EventEnum {
        LAUNCHER_LATENCY_STARTUP_TOTAL_DURATION(1362),
        LAUNCHER_LATENCY_STARTUP_ACTIVITY_ON_CREATE(1363),
        LAUNCHER_LATENCY_STARTUP_VIEW_INFLATION(1364),
        LAUNCHER_LATENCY_STARTUP_WORKSPACE_LOADER_ASYNC(1367),
    }
}
