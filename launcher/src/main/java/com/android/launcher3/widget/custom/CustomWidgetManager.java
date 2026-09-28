/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.launcher3.widget.custom;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.dagger.ApplicationContext;
import com.android.launcher3.dagger.LauncherAppSingleton;
import com.sandboxr.launcher.util.DaggerSingletonTracker;
import com.android.launcher3.util.SafeCloseable;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import javax.inject.Inject;

/**
 * CustomWidgetManager stub: AOSP/GrapheneOS launcher uses a SystemUI plugin interface for custom
 * widgets. Sandboxr does not implement the plugin system, so this is a lightweight no-op stub
 * that satisfies the Dagger graph while the launcher still supports standard AppWidgets.
 */
@LauncherAppSingleton
public class CustomWidgetManager {

    public static final String NAMED_CUSTOM_WIDGETS = "CUSTOM_WIDGETS";

    public static final com.android.launcher3.util.DaggerSingletonObject<CustomWidgetManager> INSTANCE =
            new com.android.launcher3.util.DaggerSingletonObject<>(c -> new CustomWidgetManager(c.applicationContext(), new DaggerSingletonTracker()));

    private final List<Runnable> mWidgetRefreshCallbacks = new CopyOnWriteArrayList<>();

    @Inject
    public CustomWidgetManager(
            @ApplicationContext Context context,
            DaggerSingletonTracker tracker) {
        // No plugin system in Sandboxr; custom widgets are not supported.
    }

    /** Registers a callback invoked whenever the custom widget list changes. */
    public SafeCloseable addWidgetRefreshCallback(Runnable callback) {
        mWidgetRefreshCallbacks.add(callback);
        return () -> mWidgetRefreshCallbacks.remove(callback);
    }

    /** Creates a host view for a custom widget. Returns null (no custom widgets). */
    @Nullable
    public AppWidgetHostView createView(Context context, LauncherAppWidgetProviderInfo info) {
        return null;
    }

    /** Returns an empty stream (no custom widgets). */
    @NonNull
    public Stream<LauncherAppWidgetProviderInfo> stream() {
        return new ArrayList<LauncherAppWidgetProviderInfo>().stream();
    }

    /** Returns null (no custom widgets). */
    @Nullable
    public LauncherAppWidgetProviderInfo getWidgetProvider(ComponentName cn) {
        return null;
    }

    /** Returns -1 (no custom widget IDs). */
    public int allocateCustomAppWidgetId(ComponentName componentName) {
        return -1;
    }
}
