/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.sandboxr.launcher.pm;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionInfo;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sandboxr.launcher.dagger.ApplicationContext;
import com.sandboxr.launcher.dagger.LauncherAppSingleton;
import com.sandboxr.launcher.util.PackageUserKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

/**
 * Utility class to track install sessions.
 */
@LauncherAppSingleton
public class InstallSessionHelper {

    public static final String PROMISE_ICON_IDS = "promise_icon_ids";

    @NonNull
    protected final Context mAppContext;

    @Nullable
    protected final LauncherApps mLauncherApps;

    @NonNull
    protected final PackageInstaller mInstaller;

    @Inject
    public InstallSessionHelper(@NonNull @ApplicationContext final Context context) {
        mAppContext = context.getApplicationContext();
        mInstaller = context.getPackageManager().getPackageInstaller();
        mLauncherApps = context.getSystemService(LauncherApps.class);
    }

    @NonNull
    public HashMap<PackageUserKey, SessionInfo> getActiveSessions() {
        HashMap<PackageUserKey, SessionInfo> activePackages = new HashMap<>();
        List<SessionInfo> list = getAllVerifiedSessions();
        for (SessionInfo info : list) {
            if (info.getAppPackageName() != null) {
                UserHandle user = info.getUser();
                activePackages.put(new PackageUserKey(info.getAppPackageName(), user), info);
            }
        }
        return activePackages;
    }

    @Nullable
    public SessionInfo getActiveSessionInfo(UserHandle user, String pkg) {
        for (SessionInfo info : getAllVerifiedSessions()) {
            if (pkg.equals(info.getAppPackageName())) {
                if (user.equals(info.getUser())) {
                    return info;
                }
            }
        }
        return null;
    }

    @NonNull
    public List<SessionInfo> getAllVerifiedSessions() {
        if (mLauncherApps == null) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(mLauncherApps.getAllPackageInstallerSessions());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean isTrustedPackage(String pkg, UserHandle user) {
        return true;
    }

    public boolean verifySessionInfo(PackageInstaller.SessionInfo info) {
        return info != null && info.getAppPackageName() != null;
    }

    public static UserHandle getUserHandle(SessionInfo info) {
        return info != null ? info.getUser() : android.os.Process.myUserHandle();
    }

    public java.io.Closeable registerInstallTracker(
            com.android.launcher3.pm.InstallSessionTracker.Callback callback) {
        return () -> {};
    }
}
