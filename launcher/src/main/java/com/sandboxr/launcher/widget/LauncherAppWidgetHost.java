/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * AppWidgetHost subclass for SANDBOXR managing widget instantiation, allocation,
 * and lifecycle listeners.
 */
public class LauncherAppWidgetHost extends AppWidgetHost {

    public static final int APPWIDGET_HOST_ID = 1024;

    public interface ProviderChangedListener {
        void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget);
        void onProvidersChanged();
    }

    private final List<ProviderChangedListener> mListeners = new ArrayList<>();

    public LauncherAppWidgetHost(Context context) {
        this(context, APPWIDGET_HOST_ID);
    }

    public LauncherAppWidgetHost(Context context, int hostId) {
        super(context, hostId);
    }

    public void addProviderChangeListener(ProviderChangedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeProviderChangeListener(ProviderChangedListener listener) {
        mListeners.remove(listener);
    }

    @Override
    protected AppWidgetHostView onCreateView(
            Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
        NavigableAppWidgetHostView hostView = new NavigableAppWidgetHostView(context);
        LauncherAppWidgetProviderInfo info = (appWidget instanceof LauncherAppWidgetProviderInfo)
                ? (LauncherAppWidgetProviderInfo) appWidget
                : LauncherAppWidgetProviderInfo.fromProviderInfo(context, appWidget);
        hostView.setAppWidget(appWidgetId, info);
        return hostView;
    }

    @Override
    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget) {
        super.onProviderChanged(appWidgetId, appWidget);
        for (ProviderChangedListener l : new ArrayList<>(mListeners)) {
            l.onProviderChanged(appWidgetId, appWidget);
        }
    }

    @Override
    protected void onProvidersChanged() {
        super.onProvidersChanged();
        for (ProviderChangedListener l : new ArrayList<>(mListeners)) {
            l.onProvidersChanged();
        }
    }
}
