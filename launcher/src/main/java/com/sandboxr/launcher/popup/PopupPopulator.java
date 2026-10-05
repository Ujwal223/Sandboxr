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

import static com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_SHORTCUTS;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Handler;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;

import com.android.launcher3.LauncherAppState;
import com.sandboxr.launcher.icons.IconCache;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.ItemInfoWithIcon;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;
import com.sandboxr.launcher.shortcuts.DeepShortcutView;
import com.sandboxr.launcher.shortcuts.ShortcutRequest;
import com.android.launcher3.util.ApplicationInfoWrapper;
import com.sandboxr.launcher.views.ActivityContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contains logic relevant to populating a {@link PopupContainerWithArrow}.
 */
public class PopupPopulator {

    public static final int MAX_SHORTCUTS = 4;
    @VisibleForTesting
    static final int NUM_DYNAMIC = 2;

    private static final Comparator<ShortcutInfo> SHORTCUT_RANK_COMPARATOR = (a, b) -> {
        if (a.isDeclaredInManifest() && !b.isDeclaredInManifest()) {
            return -1;
        }
        if (!a.isDeclaredInManifest() && b.isDeclaredInManifest()) {
            return 1;
        }
        return Integer.compare(a.getRank(), b.getRank());
    };

    public static List<ShortcutInfo> sortAndFilterShortcuts(List<ShortcutInfo> shortcuts) {
        shortcuts.sort(SHORTCUT_RANK_COMPARATOR);
        if (shortcuts.size() <= MAX_SHORTCUTS) {
            return shortcuts;
        }

        List<ShortcutInfo> filteredShortcuts = new ArrayList<>(MAX_SHORTCUTS);
        int numDynamic = 0;
        int size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ShortcutInfo shortcut = shortcuts.get(i);
            int filteredSize = filteredShortcuts.size();
            if (filteredSize < MAX_SHORTCUTS) {
                filteredShortcuts.add(shortcut);
                if (shortcut.isDynamic()) {
                    numDynamic++;
                }
                continue;
            }
            if (shortcut.isDynamic() && numDynamic < NUM_DYNAMIC) {
                numDynamic++;
                int lastStaticIndex = filteredSize - numDynamic;
                filteredShortcuts.remove(lastStaticIndex);
                filteredShortcuts.add(shortcut);
            }
        }
        return filteredShortcuts;
    }

    public static Runnable createUpdateRunnable(
            final ActivityContext activityContext,
            final ItemInfo originalInfo,
            final Handler uiHandler,
            final PopupContainerWithArrow<?> container,
            final List<DeepShortcutView> shortcutViews
    ) {
        final Context context = activityContext.asContext();
        final ComponentName activity = originalInfo.getTargetComponent();
        final UserHandle user = originalInfo.user;
        final String targetPackage = originalInfo.getTargetPackage();
        return () -> {
            ApplicationInfoWrapper infoWrapper =
                    new ApplicationInfoWrapper(context, targetPackage, user);
            List<ShortcutInfo> shortcuts = new ShortcutRequest(context, user)
                    .withContainer(activity)
                    .query(ShortcutRequest.PUBLISHED);
            shortcuts = PopupPopulator.sortAndFilterShortcuts(shortcuts);
            IconCache cache = LauncherAppState.getInstance(context).getIconCache();
            for (int i = 0; i < shortcuts.size() && i < shortcutViews.size(); i++) {
                final ShortcutInfo shortcut = shortcuts.get(i);
                final WorkspaceItemInfo si = new WorkspaceItemInfo(shortcut, context);
                cache.getShortcutIcon(si, shortcut, infoWrapper);
                si.rank = i;
                si.container = CONTAINER_SHORTCUTS;

                final DeepShortcutView view = shortcutViews.get(i);
                uiHandler.post(
                        () -> view.applyShortcutInfo(si, shortcut, container, activityContext));
            }
        };
    }

    public static Runnable createUpdateRunnable(
            final Context context,
            final ItemInfo originalInfo,
            final Handler uiHandler,
            final Consumer<List<ItemInfoWithIcon>> deepShortcutsConsumer
    ) {
        final ComponentName activity = originalInfo.getTargetComponent();
        final UserHandle user = originalInfo.user;
        final String targetPackage = originalInfo.getTargetPackage();
        return () -> {
            ApplicationInfoWrapper infoWrapper =
                    new ApplicationInfoWrapper(context, targetPackage, user);
            List<ShortcutInfo> shortcuts = new ShortcutRequest(context, user)
                    .withContainer(activity)
                    .query(ShortcutRequest.PUBLISHED);
            shortcuts = PopupPopulator.sortAndFilterShortcuts(shortcuts);
            IconCache cache = LauncherAppState.getInstance(context).getIconCache();
            List<ItemInfoWithIcon> deepShortcuts = new ArrayList<>();
            for (int i = 0; i < shortcuts.size(); i++) {
                final ShortcutInfo shortcut = shortcuts.get(i);
                final WorkspaceItemInfo si = new WorkspaceItemInfo(shortcut, context);
                cache.getShortcutIcon(si, shortcut, infoWrapper);
                si.rank = i;
                si.container = CONTAINER_SHORTCUTS;
                deepShortcuts.add(si);
            }
            uiHandler.post(() -> deepShortcutsConsumer.accept(deepShortcuts));
        };
    }
}
