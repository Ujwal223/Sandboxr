/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Parcel;
import android.os.UserHandle;
import androidx.annotation.NonNull;
import com.sandboxr.launcher.InvariantDeviceProfile;

/**
 * Enhanced AppWidgetProviderInfo for SANDBOXR launcher.
 * Calculates grid cell spans, minimum/maximum resize limits, and configuration status.
 */
public class LauncherAppWidgetProviderInfo extends AppWidgetProviderInfo {

    public static final String CUSTOM_WIDGET_PACKAGE = "custom-widget";

    public int spanX = 2;
    public int spanY = 2;
    public int minSpanX = 1;
    public int minSpanY = 1;
    public int maxSpanX = 4;
    public int maxSpanY = 4;

    protected PackageManager mPM;

    public static LauncherAppWidgetProviderInfo fromProviderInfo(
            Context context, AppWidgetProviderInfo info) {
        final LauncherAppWidgetProviderInfo launcherInfo;
        if (info instanceof LauncherAppWidgetProviderInfo) {
            launcherInfo = (LauncherAppWidgetProviderInfo) info;
        } else {
            Parcel p = Parcel.obtain();
            info.writeToParcel(p, 0);
            p.setDataPosition(0);
            launcherInfo = new LauncherAppWidgetProviderInfo(p);
            p.recycle();
        }
        launcherInfo.initSpans(context, InvariantDeviceProfile.INSTANCE(context));
        return launcherInfo;
    }

    public LauncherAppWidgetProviderInfo() {}

    public LauncherAppWidgetProviderInfo(Parcel in) {
        super(in);
    }

    public void initSpans(Context context, InvariantDeviceProfile idp) {
        mPM = context.getApplicationContext().getPackageManager();
        int cols = idp != null ? idp.numColumns : 4;
        int rows = idp != null ? idp.numRows : 5;

        minSpanX = Math.max(1, minResizeWidth > 0 ? (int) Math.ceil((float) minResizeWidth / 70f) : 1);
        minSpanY = Math.max(1, minResizeHeight > 0 ? (int) Math.ceil((float) minResizeHeight / 70f) : 1);
        maxSpanX = cols;
        maxSpanY = rows;

        spanX = Math.max(minSpanX, minWidth > 0 ? (int) Math.ceil((float) minWidth / 70f) : 2);
        spanY = Math.max(minSpanY, minHeight > 0 ? (int) Math.ceil((float) minHeight / 70f) : 2);

        if (maxResizeWidth > 0) {
            maxSpanX = Math.min(maxSpanX, Math.max(1, (int) Math.ceil((float) maxResizeWidth / 70f)));
        }
        if (maxResizeHeight > 0) {
            maxSpanY = Math.min(maxSpanY, Math.max(1, (int) Math.ceil((float) maxResizeHeight / 70f)));
        }

        spanX = Math.min(spanX, maxSpanX);
        spanY = Math.min(spanY, maxSpanY);
    }

    public CharSequence getLabel() {
        return mPM != null ? super.loadLabel(mPM) : (label != null ? label : "");
    }

    public Point getMinSpans() {
        return new Point(
            (resizeMode & RESIZE_HORIZONTAL) != 0 ? minSpanX : spanX,
            (resizeMode & RESIZE_VERTICAL) != 0 ? minSpanY : spanY
        );
    }

    public Point getMaxSpans() {
        return new Point(
            (resizeMode & RESIZE_HORIZONTAL) != 0 ? maxSpanX : spanX,
            (resizeMode & RESIZE_VERTICAL) != 0 ? maxSpanY : spanY
        );
    }

    public boolean isReconfigurable() {
        return configure != null;
    }

    public ComponentName getComponentName() {
        return provider;
    }

    public UserHandle getUser() {
        try {
            UserHandle profile = getProfile();
            if (profile != null) return profile;
        } catch (Exception ignored) {}
        return android.os.Process.myUserHandle();
    }
}
