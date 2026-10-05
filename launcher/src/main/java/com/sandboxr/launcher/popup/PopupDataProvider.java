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

package com.sandboxr.launcher.popup;

import android.content.ComponentName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sandboxr.launcher.BubbleTextView;
import com.sandboxr.launcher.allapps.AllAppsStore;
import com.sandboxr.launcher.dagger.ActivityContextSingleton;
import com.sandboxr.launcher.dot.DotInfo;
import com.sandboxr.launcher.folder.Folder;
import com.sandboxr.launcher.folder.FolderIcon;
import com.sandboxr.launcher.model.BgDataModel;
import com.sandboxr.launcher.model.data.FolderInfo;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.ItemInfoWithIcon;
import com.sandboxr.launcher.notification.NotificationRepository;
import java.util.Arrays;
import com.sandboxr.launcher.util.ComponentKey;
import com.sandboxr.launcher.util.LauncherBindableItemsContainer;
import com.sandboxr.launcher.util.LauncherBindableItemsContainer.ItemOperator;
import com.sandboxr.launcher.util.PackageUserKey;
import com.sandboxr.launcher.util.ShortcutUtil;
import com.sandboxr.launcher.views.ActivityContext;

import kotlin.Unit;

import java.util.Arrays;
import java.util.function.Predicate;

import javax.inject.Inject;

/**
 * Provides data for the popup menu that appears after long-clicking on apps.
 */
@ActivityContextSingleton
public class PopupDataProvider {

    private final NotificationRepository mNotificationRepo;
    private final ActivityContext mContext;
    private final AllAppsStore mAppsStore;
    private final BgDataModel mBgDataModel;

    public PopupDataProvider(ActivityContext context) {
        this(context, null, null, null);
    }

    @Inject
    public PopupDataProvider(
            ActivityContext context,
            NotificationRepository notificationRepository,
            AllAppsStore appsStore,
            BgDataModel dataModel) {
        mContext = context;
        mNotificationRepo = notificationRepository;
        mAppsStore = appsStore;
        mBgDataModel = dataModel;

        if (mNotificationRepo != null && mNotificationRepo.getUpdateStream() != null) {
            mContext.closeOnDestroy(mNotificationRepo.getUpdateStream().forEach(
                    mContext.getUiExecutor(), this::updateNotificationDots));
        }
    }

    private Unit updateNotificationDots(Predicate<PackageUserKey> updatedDots) {
        final PackageUserKey packageUserKey = new PackageUserKey(null, null);
        Predicate<ItemInfo> matcher = info -> !packageUserKey.updateFromItemInfo(info)
                || updatedDots.test(packageUserKey);

        ItemOperator op = (info, v) -> {
            if (v instanceof BubbleTextView && info instanceof ItemInfoWithIcon && matcher.test(info)) {
                ((BubbleTextView) v).applyDotState((ItemInfoWithIcon) info, true /* animate */);
            } else if (v instanceof FolderIcon
                    && info instanceof FolderInfo && ((FolderInfo) info).anyMatch(matcher)) {
                ((FolderIcon) v).updateDotInfo();
            }
            return false;
        };

        LauncherBindableItemsContainer content = mContext.getContent();
        if (content != null) {
            content.mapOverItems(op);
        }
        Folder folder = Folder.getOpen(mContext);
        if (folder != null) {
            folder.mapOverItems(op);
        }
        if (mAppsStore != null) {
            mAppsStore.updateNotificationDots(updatedDots::test);
        }
        return null;
    }

    public int getShortcutCountForItem(ItemInfo info) {
        if (!ShortcutUtil.supportsDeepShortcuts(info)) {
            return 0;
        }
        ComponentName component = info.getTargetComponent();
        if (component == null) {
            return 0;
        }

        if (mBgDataModel != null && mBgDataModel.getDeepShortcutMap() != null) {
            return mBgDataModel.getDeepShortcutMap()
                    .getOrDefault(new ComponentKey(component, info.user), 0);
        }
        return 0;
    }

    public @Nullable DotInfo getDotInfoForItem(@NonNull ItemInfo info) {
        if (!ShortcutUtil.supportsShortcuts(info) || mNotificationRepo == null) {
            return null;
        }
        DotInfo dotInfo = mNotificationRepo.getPackageUserToDotInfos()
                .get(PackageUserKey.fromItemInfo(info));
        if (dotInfo == null) {
            return null;
        }

        String shortcutId = ShortcutUtil.getShortcutIdIfPinnedShortcut(info);
        if (shortcutId == null) {
            return dotInfo;
        }
        String[] personKeys = ShortcutUtil.getPersonKeysIfPinnedShortcut(info);
        return (dotInfo.getNotificationKeys().stream().anyMatch(notification -> {
            if (notification.getShortcutId() != null) {
                return notification.getShortcutId().equals(shortcutId);
            }
            if (notification.getPersonKeys().length != 0) {
                return Arrays.equals(notification.getPersonKeys(), personKeys);
            }
            return false;
        })) ? dotInfo : null;
    }
}
