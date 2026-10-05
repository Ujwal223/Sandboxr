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

package com.sandboxr.launcher.popup

import android.view.View
import com.android.launcher3.logging.StatsLogManager
import com.sandboxr.launcher.DeviceProfile
import com.sandboxr.launcher.Launcher
import com.sandboxr.launcher.Workspace
import com.sandboxr.launcher.dagger.ActivityContextComponent
import com.sandboxr.launcher.dragndrop.DragController
import com.sandboxr.launcher.views.ActivityContext

inline val ActivityContext.deviceProfile: DeviceProfile get() = getDeviceProfile()
inline val ActivityContext.itemOnClickListener: View.OnClickListener? get() = getItemOnClickListener()
inline val ActivityContext.statsLogManager: StatsLogManager get() = getStatsLogManager()
inline val ActivityContext.dragController: DragController? get() = getDragController()
inline val ActivityContext.accessibilityDelegate: View.AccessibilityDelegate? get() = getAccessibilityDelegate()
inline val ActivityContext.popupDataProvider: PopupDataProvider? get() = getPopupDataProvider()
inline val ActivityContext.activityComponent: ActivityContextComponent? get() = getActivityComponent()
inline val Launcher.workspace: Workspace<*>? get() = getWorkspace()
