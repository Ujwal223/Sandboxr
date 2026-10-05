/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.sandboxr.launcher.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import androidx.annotation.NonNull;

/**
 * Holder managing the LauncherAppWidgetHost instance across activity lifecycle events.
 */
public class LauncherWidgetHolder {

    public static final int APPWIDGET_HOST_ID = LauncherAppWidgetHost.APPWIDGET_HOST_ID;

    protected final Context mContext;
    protected final LauncherAppWidgetHost mAppWidgetHost;

    public LauncherWidgetHolder(@NonNull Context context) {
        mContext = context;
        mAppWidgetHost = createHost(context);
    }

    protected LauncherAppWidgetHost createHost(Context context) {
        return new LauncherAppWidgetHost(context, APPWIDGET_HOST_ID);
    }

    public static LauncherWidgetHolder newInstance(@NonNull Context context) {
        return new LauncherWidgetHolder(context);
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public int allocateAppWidgetId() {
        return mAppWidgetHost.allocateAppWidgetId();
    }

    public void deleteAppWidgetId(int appWidgetId) {
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
    }

    public AppWidgetHostView createView(Context context, int appWidgetId, LauncherAppWidgetProviderInfo providerInfo) {
        return mAppWidgetHost.createView(context, appWidgetId, providerInfo);
    }

    public void startListening() {
        try {
            mAppWidgetHost.startListening();
        } catch (Exception ignored) {}
    }

    public void stopListening() {
        try {
            mAppWidgetHost.stopListening();
        } catch (Exception ignored) {}
    }

    public void destroy() {
        stopListening();
    }
}
