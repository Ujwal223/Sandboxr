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

package com.sandboxr.launcher.util;

import static com.sandboxr.launcher.model.data.PackageItemInfo.NO_CATEGORY;

import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.PackageItemInfo;

import java.util.Objects;

/** Creates a hash key based on package name, widget category, and user. */
public class PackageUserKey {

    public String mPackageName;
    public int mWidgetCategory;
    public UserHandle mUser;
    private int mHashCode;

    @Nullable
    public static PackageUserKey fromItemInfo(ItemInfo info) {
        if (info.getTargetComponent() == null) return null;
        return new PackageUserKey(info.getTargetComponent().getPackageName(), info.user);
    }

    public static PackageUserKey fromNotification(StatusBarNotification notification) {
        return new PackageUserKey(notification.getPackageName(), notification.getUser());
    }

    /** Creates a {@link PackageUserKey} from {@link PackageItemInfo}. */
    public static PackageUserKey fromPackageItemInfo(PackageItemInfo info) {
        if (TextUtils.isEmpty(info.packageName) && info.widgetCategory != NO_CATEGORY) {
            return new PackageUserKey(info.widgetCategory, info.user);
        }
        return new PackageUserKey(info.packageName, info.user);
    }

    public PackageUserKey(String packageName, UserHandle user) {
        update(packageName, user);
    }

    public PackageUserKey(int widgetCategory, UserHandle user) {
        update(/* packageName= */ "", widgetCategory, user);
    }

    public void update(String packageName, UserHandle user) {
        update(packageName, NO_CATEGORY, user);
    }

    private void update(String packageName, int widgetCategory, UserHandle user) {
        mPackageName = packageName;
        mWidgetCategory = widgetCategory;
        mUser = user;
        mHashCode = Objects.hash(packageName, widgetCategory, user);
    }

    /**
     * This should only be called to avoid new object creations in a loop.
     * @return Whether this PackageUserKey was successfully updated - it shouldn't be used if not.
     */
    public boolean updateFromItemInfo(ItemInfo info) {
        if (info.getTargetComponent() == null) return false;
        if (ShortcutUtil.supportsShortcuts(info)) {
            update(info.getTargetComponent().getPackageName(), info.user);
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PackageUserKey)) return false;
        PackageUserKey otherKey = (PackageUserKey) obj;
        return Objects.equals(mPackageName, otherKey.mPackageName)
                && mWidgetCategory == otherKey.mWidgetCategory
                && Objects.equals(mUser, otherKey.mUser);
    }

    @NonNull
    @Override
    public String toString() {
        return mPackageName + "#" + mUser + ",category=" + mWidgetCategory;
    }
}
