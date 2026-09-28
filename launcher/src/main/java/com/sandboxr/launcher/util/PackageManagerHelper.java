/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sandboxr.launcher.PendingAddItemInfo;
import com.sandboxr.launcher.dagger.ApplicationContext;
import com.sandboxr.launcher.dagger.LauncherAppSingleton;
import com.sandboxr.launcher.model.data.AppInfo;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.ItemInfoWithIcon;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

/**
 * Utility methods using package manager.
 */
@LauncherAppSingleton
public class PackageManagerHelper {

    public static final DaggerSingletonObject<PackageManagerHelper> INSTANCE =
            new DaggerSingletonObject<>(c -> new PackageManagerHelper(c.applicationContext()));

    private static final String TAG = "PackageManagerHelper";

    @NonNull
    private final Context mContext;

    @NonNull
    private final PackageManager mPm;

    @NonNull
    private final LauncherApps mLauncherApps;

    @Inject
    public PackageManagerHelper(@ApplicationContext final Context context) {
        mContext = context;
        mPm = context.getPackageManager();
        mLauncherApps = Objects.requireNonNull(context.getSystemService(LauncherApps.class));
    }

    public String getAppInstallerPackage(@NonNull final String packageName) {
        try {
            return mPm.getInstallSourceInfo(packageName).getInstallingPackageName();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed to get installer package for app package:" + packageName, e);
            return null;
        }
    }

    @Nullable
    public Intent getAppLaunchIntent(@Nullable final String pkg, @NonNull final UserHandle user) {
        LauncherActivityInfo info = getAppLaunchInfo(pkg, user);
        return info != null ? AppInfo.makeLaunchIntent(info) : null;
    }

    @Nullable
    public LauncherActivityInfo getAppLaunchInfo(@Nullable final String pkg,
            @NonNull final UserHandle user) {
        List<LauncherActivityInfo> activities = mLauncherApps.getActivityList(pkg, user);
        return activities.isEmpty() ? null : activities.get(0);
    }

    public static void startDetailsActivityForInfo(Context context, ItemInfo info,
            Rect sourceBounds, Bundle opts) {
        ComponentName componentName = null;
        if (info instanceof AppInfo) {
            componentName = ((AppInfo) info).componentName;
        } else if (info instanceof WorkspaceItemInfo) {
            componentName = info.getTargetComponent();
        } else if (info instanceof PendingAddItemInfo) {
            componentName = ((PendingAddItemInfo) info).componentName;
        } else if (info instanceof LauncherAppWidgetInfo) {
            componentName = ((LauncherAppWidgetInfo) info).providerName;
        }
        if (componentName != null) {
            try {
                context.getSystemService(LauncherApps.class).startAppDetailsActivity(componentName,
                        info.user, sourceBounds, opts);
            } catch (SecurityException | ActivityNotFoundException e) {
                Log.e(TAG, "Unable to launch settings", e);
            }
        }
    }

    public static boolean isLauncherAppTarget(Intent launchIntent) {
        if (launchIntent != null
                && Intent.ACTION_MAIN.equals(launchIntent.getAction())
                && launchIntent.getComponent() != null
                && launchIntent.getCategories() != null
                && launchIntent.getCategories().size() == 1
                && launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && TextUtils.isEmpty(launchIntent.getDataString())) {
            Bundle extras = launchIntent.getExtras();
            return extras == null || extras.keySet().isEmpty();
        }
        return false;
    }

    public static boolean hasShortcutsPermission(Context context) {
        try {
            return context.getSystemService(LauncherApps.class).hasShortcutHostPermission();
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Failed to make shortcut manager call", e);
        }
        return false;
    }

    public static int getLoadingProgress(LauncherActivityInfo info) {
        return (int) (100 * info.getLoadingProgress());
    }

    public static boolean isSameAppForMultiInstance(@NonNull ItemInfo app1,
            @NonNull ItemInfo app2) {
        return app1.getTargetPackage().equals(app2.getTargetPackage())
                && app1.user.equals(app2.user);
    }
}
