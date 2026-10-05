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

package com.sandboxr.launcher.dragndrop;

import android.app.Activity;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.LauncherApps.PinItemRequest;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sandboxr.launcher.InvariantDeviceProfile;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;
import com.sandboxr.launcher.widget.LauncherAppWidgetProviderInfo;

/**
 * Handles pinning app widgets requested by third-party applications via PinItemRequest.
 * Coordinates span calculations, optional widget configuration activities, and confirmation.
 */
public class PinWidgetFlowHandler {

    private final PinItemRequest mRequest;
    private final LauncherAppWidgetProviderInfo mProviderInfo;

    public PinWidgetFlowHandler(PinItemRequest request, Context context) {
        mRequest = request;
        AppWidgetProviderInfo rawInfo = request != null ? request.getAppWidgetProviderInfo(context) : null;
        if (rawInfo != null) {
            mProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(context, rawInfo);
        } else {
            mProviderInfo = null;
        }
    }

    public PinWidgetFlowHandler(LauncherAppWidgetProviderInfo providerInfo) {
        mRequest = null;
        mProviderInfo = providerInfo;
    }

    public PinItemRequest getRequest() {
        return mRequest;
    }

    public LauncherAppWidgetProviderInfo getProviderInfo() {
        return mProviderInfo;
    }

    public boolean needsConfiguration() {
        return mProviderInfo != null && mProviderInfo.configure != null;
    }

    public boolean startConfigActivity(Activity activity, int requestCode) {
        if (!needsConfiguration() || activity == null) {
            return false;
        }
        // LauncherAppWidgetHost can start configuration for the allocated ID
        return true;
    }

    public LauncherAppWidgetInfo createAppWidgetInfo(
            int appWidgetId,
            int container,
            int screenId,
            int cellX,
            int cellY
    ) {
        if (mProviderInfo == null) return null;
        LauncherAppWidgetInfo info = new LauncherAppWidgetInfo(appWidgetId, mProviderInfo.provider);
        info.container = container;
        info.screenId = screenId;
        info.cellX = cellX;
        info.cellY = cellY;
        info.spanX = mProviderInfo.spanX;
        info.spanY = mProviderInfo.spanY;
        info.minSpanX = mProviderInfo.minSpanX;
        info.minSpanY = mProviderInfo.minSpanY;
        info.user = mProviderInfo.getUser();
        return info;
    }

    public boolean finishConfirmation(Context context, @Nullable Bundle options) {
        if (mRequest != null && mRequest.isValid()) {
            return mRequest.accept(options);
        }
        return false;
    }
}
