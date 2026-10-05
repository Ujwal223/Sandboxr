/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.os.Bundle;

import com.sandboxr.launcher.LauncherSettings;
import com.sandboxr.launcher.PendingAddItemInfo;

/**
 * Meta-data used for an app widget that has not yet been bound or added to the workspace.
 */
public class PendingAddWidgetInfo extends PendingAddItemInfo {

    public final LauncherAppWidgetProviderInfo info;
    public final int minSpanX;
    public final int minSpanY;
    public final int maxSpanX;
    public final int maxSpanY;
    public int previewImage;
    public int icon;
    public AppWidgetHostView previewHostView;
    public Bundle bindOptions = null;

    public PendingAddWidgetInfo(LauncherAppWidgetProviderInfo info, int container) {
        this.info = info;
        this.user = (info != null && info.getUser() != null)
                ? info.getUser()
                : android.os.Process.myUserHandle();
        this.componentName = info != null ? info.provider : null;
        this.minSpanX = info != null ? info.minSpanX : 1;
        this.minSpanY = info != null ? info.minSpanY : 1;
        this.maxSpanX = info != null ? info.maxSpanX : 4;
        this.maxSpanY = info != null ? info.maxSpanY : 4;
        this.spanX = info != null ? info.spanX : 2;
        this.spanY = info != null ? info.spanY : 2;
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
        this.container = container;
    }

    public PendingAddWidgetInfo(PendingAddWidgetInfo copy) {
        super(copy);
        this.info = copy.info;
        this.minSpanX = copy.minSpanX;
        this.minSpanY = copy.minSpanY;
        this.maxSpanX = copy.maxSpanX;
        this.maxSpanY = copy.maxSpanY;
        this.previewImage = copy.previewImage;
        this.icon = copy.icon;
        this.previewHostView = copy.previewHostView;
        this.bindOptions = copy.bindOptions;
    }

    @Override
    public PendingAddWidgetInfo clone() {
        return new PendingAddWidgetInfo(this);
    }
}
