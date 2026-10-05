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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.PinItemRequest;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.sandboxr.launcher.LauncherSettings;
import com.sandboxr.launcher.icons.IconCache;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;
import com.sandboxr.launcher.pm.ShortcutConfigActivityInfo;

/**
 * Handles pinning shortcuts requested by third-party applications via PinItemRequest.
 */
public class PinShortcutRequestActivityInfo extends ShortcutConfigActivityInfo {

    private final PinItemRequest mRequest;
    private final ShortcutInfo mShortcutInfo;
    private final Context mContext;

    public PinShortcutRequestActivityInfo(PinItemRequest request, Context context) {
        super(
                request != null && request.getShortcutInfo() != null && request.getShortcutInfo().getActivity() != null
                        ? request.getShortcutInfo().getActivity()
                        : (request != null && request.getShortcutInfo() != null
                        ? new ComponentName(request.getShortcutInfo().getPackage(), "")
                        : new ComponentName("", "")),
                request != null && request.getShortcutInfo() != null && request.getShortcutInfo().getUserHandle() != null
                        ? request.getShortcutInfo().getUserHandle()
                        : Process.myUserHandle()
        );
        mRequest = request;
        mShortcutInfo = request != null ? request.getShortcutInfo() : null;
        mContext = context != null ? context.getApplicationContext() : null;
    }

    public PinItemRequest getPinItemRequest() {
        return mRequest;
    }

    public ShortcutInfo getShortcutInfo() {
        return mShortcutInfo;
    }

    @Override
    public CharSequence getLabel() {
        if (mShortcutInfo == null) return "";
        CharSequence shortLabel = mShortcutInfo.getShortLabel();
        if (shortLabel != null && shortLabel.length() > 0) return shortLabel;
        CharSequence longLabel = mShortcutInfo.getLongLabel();
        return longLabel != null ? longLabel : "";
    }

    @Override
    public Drawable getFullResIcon(Context context) {
        if (mShortcutInfo == null) return null;
        try {
            LauncherApps launcherApps =
                    (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps != null) {
                return launcherApps.getShortcutIconDrawable(
                        mShortcutInfo,
                        context.getResources().getDisplayMetrics().densityDpi
                );
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public boolean startConfigActivity(Activity activity, int requestCode) {
        return false;
    }

    public WorkspaceItemInfo createWorkspaceItemInfo(@Nullable IconCache iconCache) {
        WorkspaceItemInfo itemInfo = new WorkspaceItemInfo();
        itemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
        itemInfo.user = getUser();
        itemInfo.title = getLabel();
        if (mShortcutInfo != null) {
            itemInfo.intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(getComponentName());
            if (iconCache != null) {
                iconCache.getShortcutIcon(itemInfo, mShortcutInfo);
            }
        }
        return itemInfo;
    }
}
