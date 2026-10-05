/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.launcher3.AbstractFloatingView.TYPE_FOLDER;
import static com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS;
import static com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_DISMISS_PREDICTION_UNDO;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_PRIVATE_SPACE_INSTALL_SYSTEM_SHORTCUT_TAP;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_PRIVATE_SPACE_UNINSTALL_SYSTEM_SHORTCUT_TAP;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_APP_INFO_TAP;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_DONT_SUGGEST_APP_TAP;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SYSTEM_SHORTCUT_WIDGETS_TAP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.sandboxr.launcher.AbstractFloatingViewHelper;
import com.android.launcher3.SecondaryDropTarget;
import com.android.launcher3.Utilities;
import com.android.wm.shell.shared.bubbles.logging.EntryPoint;
import com.sandboxr.launcher.DropTargetHandler;
import com.sandboxr.launcher.LauncherSettings;
import com.sandboxr.launcher.R;
import com.android.launcher3.logging.StatsLogManager;
import com.sandboxr.launcher.model.data.AppInfo;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.ItemInfoWithIcon;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;
import com.sandboxr.launcher.util.ActivityOptionsWrapper;
import com.sandboxr.launcher.util.PackageUserKey;
import com.sandboxr.launcher.util.PackageManagerHelper;
import com.sandboxr.launcher.views.ActivityContext;

import java.util.stream.Stream;

/**
 * Represents a system shortcut for a given app. The shortcut should have a label and icon, and an
 * onClickListener that depends on the item that the shortcut services.
 *
 * @param <T> extends {@link ActivityContext}
 */
public abstract class SystemShortcut<T extends ActivityContext> extends ItemInfo
        implements View.OnClickListener {
    private static final String TAG = "SystemShortcut";

    private final int mIconResId;
    protected final int mLabelResId;
    protected int mAccessibilityActionId;

    protected final T mTarget;
    protected final ItemInfo mItemInfo;
    protected final View mOriginalView;
    public final boolean mIsCollapsible;

    private final AbstractFloatingViewHelper mAbstractFloatingViewHelper;

    public SystemShortcut(int iconResId, int labelResId, T target, ItemInfo itemInfo,
            View originalView) {
        this(iconResId, labelResId, target, itemInfo, originalView,
                AbstractFloatingViewHelper.INSTANCE, /* isCollapsible */ true);
    }

    public SystemShortcut(int iconResId, int labelResId, T target, ItemInfo itemInfo,
            View originalView, boolean isCollapsible) {
        this(iconResId, labelResId, target, itemInfo, originalView,
                AbstractFloatingViewHelper.INSTANCE, isCollapsible);
    }

    public SystemShortcut(int iconResId, int labelResId, T target, ItemInfo itemInfo,
            View originalView, AbstractFloatingViewHelper abstractFloatingViewHelper) {
        this(iconResId, labelResId, target, itemInfo, originalView, abstractFloatingViewHelper,
                /* isCollapsible */ true);
    }

    public SystemShortcut(int iconResId, int labelResId, T target, ItemInfo itemInfo,
            View originalView, AbstractFloatingViewHelper abstractFloatingViewHelper,
            boolean isCollapsible) {
        mIconResId = iconResId;
        mLabelResId = labelResId;
        mAccessibilityActionId = labelResId;
        mTarget = target;
        mItemInfo = itemInfo;
        mOriginalView = originalView;
        mAbstractFloatingViewHelper = abstractFloatingViewHelper;
        mIsCollapsible = isCollapsible;
    }

    /** @return the resource id of the icon **/
    public int getIconResId() {
        return mIconResId;
    }

    /** @return the resource id of the label **/
    public int getLabelResId() {
        return mLabelResId;
    }

    public void setIconAndLabelFor(View iconView, TextView labelView) {
        iconView.setBackgroundResource(mIconResId);
        labelView.setText(mLabelResId);
    }

    public void setIconAndContentDescriptionFor(ImageView view) {
        view.setImageResource(mIconResId);
        view.setContentDescription(view.getContext().getText(mLabelResId));
    }

    public AccessibilityNodeInfo.AccessibilityAction createAccessibilityAction(Context context) {
        return new AccessibilityNodeInfo.AccessibilityAction(
                mAccessibilityActionId, context.getText(mLabelResId));
    }

    public boolean hasHandlerForAction(int action) {
        return mAccessibilityActionId == action;
    }

    public interface Factory<T extends ActivityContext> {
        @Nullable
        SystemShortcut<T> getShortcut(T context, ItemInfo itemInfo, @NonNull View originalView);
    }

    public static final Factory<ActivityContext> WIDGETS = (context, itemInfo, originalView) -> {
        final PackageUserKey packageUserKey = PackageUserKey.fromItemInfo(itemInfo);
        if (packageUserKey == null) return null;
        return new Widgets<>(context, itemInfo, originalView);
    };

    public static class Widgets<T extends ActivityContext> extends SystemShortcut<T> {

        public Widgets(T target, ItemInfo itemInfo, @NonNull View originalView) {
            super(getDrawableId(), R.string.widget_button_text, target, itemInfo, originalView,
                    false);
        }

        public static int getDrawableId() {
            return R.drawable.widgets_24px;
        }

        @Override
        public void onClick(View view) {
            AbstractFloatingView.closeAllOpenViews(mTarget);
            Context context = view.getContext();
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME,
                    mItemInfo.getTargetPackage());
            intent.putExtra(Intent.EXTRA_USER, mItemInfo.user);
            intent.setPackage(context.getPackageName());
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to start widget picker", e);
            }
            mTarget.getStatsLogManager().logger().withItemInfo(mItemInfo)
                    .log(LAUNCHER_SYSTEM_SHORTCUT_WIDGETS_TAP);
        }
    }

    public static final Factory<ActivityContext> APP_INFO = AppInfo::new;

    public static class AppInfo<T extends ActivityContext> extends SystemShortcut<T> {

        public AppInfo(T target, ItemInfo itemInfo, @NonNull View originalView) {
            super(getDrawableId(), R.string.app_info_drop_target_label, target,
                    itemInfo, originalView);
        }

        public static int getDrawableId() {
            return R.drawable.info_24px;
        }

        @Override
        public void onClick(View view) {
            Rect sourceBounds = Utilities.getViewBounds(view);
            ActivityOptionsWrapper options = mTarget.getActivityLaunchOptions(view, mItemInfo);
            options.getOnEndCallback().add(this::dismissTaskMenuView);
            PackageManagerHelper.startDetailsActivityForInfo(view.getContext(), mItemInfo,
                    sourceBounds, options.toBundle());
            mTarget.getStatsLogManager().logger().withItemInfo(mItemInfo)
                    .log(LAUNCHER_SYSTEM_SHORTCUT_APP_INFO_TAP);
        }
    }

    public static final Factory<ActivityContext> REMOVE = RemoveApp::new;

    public static class RemoveApp<T extends ActivityContext> extends SystemShortcut<T> {

        public RemoveApp(T target, ItemInfo itemInfo, @NonNull View originalView) {
            super(R.drawable.ic_uninstall_no_shadow, R.string.remove_system_shortcut_label, target,
                    itemInfo, originalView, false);
        }

        @Override
        public void onClick(View view) {
            AbstractFloatingView.closeAllOpenViewsExcept(mTarget, TYPE_FOLDER);
            DropTargetHandler dropTargetHandler =
                    ActivityContext.lookupContext(view.getContext()).getDropTargetHandler();
            if (dropTargetHandler != null) {
                dropTargetHandler.prepareToUndoDelete(mItemInfo);
                dropTargetHandler.onDeleteComplete(mItemInfo, mOriginalView);
            }
        }
    }

    public static final Factory<ActivityContext> ADD_TO_HOME_SCREEN =
            (activity, itemInfo, originalView) -> {
                if (itemInfo.container != CONTAINER_ALL_APPS
                        && itemInfo.container != CONTAINER_ALL_APPS_PREDICTION) {
                    return null;
                }
                return new AddToHomeScreen<>(activity, itemInfo, originalView);
            };

    public static class AddToHomeScreen<T extends ActivityContext> extends SystemShortcut<T> {

        public AddToHomeScreen(T target, ItemInfo itemInfo, @NonNull View originalView) {
            super(R.drawable.ic_plus, R.string.action_add_to_workspace, target,
                    itemInfo, originalView, false);
        }

        @Override
        public void onClick(View view) {
            AbstractFloatingView.closeAllOpenViews(mTarget);
            mTarget.getStatsLogManager().logger()
                    .withItemInfo(mItemInfo)
                    .log(StatsLogManager.LauncherEvent.LAUNCHER_TAP_TO_ADD_TO_HOME_SCREEN_FROM_ALL_APPS);
        }
    }

    public static final Factory<ActivityContext> INSTALL =
            (activity, itemInfo, originalView) -> {
                if (originalView == null) {
                    return null;
                }
                boolean supportsWebUI = (itemInfo instanceof WorkspaceItemInfo)
                        && ((WorkspaceItemInfo) itemInfo).hasStatusFlag(
                        WorkspaceItemInfo.FLAG_SUPPORTS_WEB_UI);
                if (!supportsWebUI) {
                    return null;
                }
                return new Install<>(activity, itemInfo, originalView);
            };

    public static class Install<T extends ActivityContext> extends SystemShortcut<T> {

        public Install(T target, ItemInfo itemInfo, @NonNull View originalView) {
            super(R.drawable.ic_install_no_shadow, R.string.install_drop_target_label,
                    target, itemInfo, originalView);
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (mItemInfo.getTargetComponent() != null) {
                intent.setData(android.net.Uri.parse("market://details?id="
                        + mItemInfo.getTargetComponent().getPackageName()));
            }
            mTarget.startActivitySafely(view, intent, mItemInfo);
            AbstractFloatingView.closeAllOpenViews(mTarget);
        }
    }

    public static final Factory<ActivityContext> DONT_SUGGEST_APP =
            (activity, itemInfo, originalView) -> {
                if (!itemInfo.isPredictedItem()) {
                    return null;
                }
                return new DontSuggestApp<>(activity, itemInfo, originalView);
            };

    private static class DontSuggestApp<T extends ActivityContext> extends SystemShortcut<T> {
        DontSuggestApp(T target, ItemInfo itemInfo, View originalView) {
            super(R.drawable.ic_block_no_shadow, R.string.dismiss_prediction_label, target,
                    itemInfo, originalView);
        }

        @Override
        public void onClick(View view) {
            dismissTaskMenuView();
            mTarget.getStatsLogManager().logger()
                    .withItemInfo(mItemInfo)
                    .log(LAUNCHER_SYSTEM_SHORTCUT_DONT_SUGGEST_APP_TAP);
            Toast.makeText(view.getContext(), R.string.item_removed, Toast.LENGTH_SHORT).show();
        }
    }

    public static final Factory<ActivityContext> UNINSTALL_APP =
            (activityContext, itemInfo, originalView) -> {
                if (originalView == null || itemInfo.getTargetComponent() == null) {
                    return null;
                }
                ComponentName cn = SecondaryDropTarget.getUninstallTarget(originalView.getContext(), itemInfo);
                if (cn == null) {
                    return null;
                }
                return new UninstallApp<>(activityContext, itemInfo, originalView, cn);
            };

    private static class UninstallApp<T extends ActivityContext> extends SystemShortcut<T> {
        @NonNull
        ComponentName mComponentName;

        UninstallApp(T target, ItemInfo itemInfo, @NonNull View originalView,
                @NonNull ComponentName cn) {
            super(R.drawable.ic_uninstall_no_shadow,
                    R.string.uninstall_private_system_shortcut_label, target,
                    itemInfo, originalView);
            mComponentName = cn;
        }

        @Override
        public void onClick(View view) {
            dismissTaskMenuView();
            SecondaryDropTarget.performUninstall(view.getContext(), mComponentName, mItemInfo);
            mTarget.getStatsLogManager()
                    .logger()
                    .withItemInfo(mItemInfo)
                    .log(LAUNCHER_PRIVATE_SPACE_UNINSTALL_SYSTEM_SHORTCUT_TAP);
        }
    }

    protected void dismissTaskMenuView() {
        mAbstractFloatingViewHelper.closeOpenViews(mTarget, true,
                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);
    }

    public static final Factory<ActivityContext> BUBBLE_SHORTCUT =
            (activity, itemInfo, originalView) -> {
                if ((itemInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                        && (itemInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)
                        && !(itemInfo instanceof WorkspaceItemInfo)) {
                    return null;
                }
                return new BubbleShortcut<>(activity, itemInfo, originalView);
            };

    public interface BubbleActivityStarter {
        void showShortcutBubble(ShortcutInfo info, EntryPoint entryPoint);
        void showAppBubble(Intent intent, UserHandle user, EntryPoint entryPoint);
    }

    public interface TaskbarBubbleActivityStarter extends BubbleActivityStarter {}

    public static class BubbleShortcut<T extends ActivityContext> extends SystemShortcut<T> {

        private BubbleActivityStarter mStarter;
        private final boolean mInTaskbar;

        public BubbleShortcut(T target, ItemInfo itemInfo, View originalView) {
            super(R.drawable.ic_bubble_button, R.string.bubble, target,
                    itemInfo, originalView);
            if (target instanceof BubbleActivityStarter) {
                mStarter = (BubbleActivityStarter) target;
            }
            mInTaskbar = target instanceof TaskbarBubbleActivityStarter;
        }

        private EntryPoint getEntryPoint() {
            if (mItemInfo.isInAllApps()) {
                return EntryPoint.ALL_APPS_ICON_MENU;
            }
            if (mItemInfo.isInHotseat()) {
                return mInTaskbar ? EntryPoint.TASKBAR_ICON_MENU : EntryPoint.HOTSEAT_ICON_MENU;
            }
            return EntryPoint.LAUNCHER_ICON_MENU;
        }

        @Override
        public void onClick(View view) {
            dismissTaskMenuView();
            if (mStarter == null) {
                Log.w(TAG, "starter null!");
                return;
            }
            if (mItemInfo instanceof WorkspaceItemInfo) {
                WorkspaceItemInfo workspaceItemInfo = (WorkspaceItemInfo) mItemInfo;
                ShortcutInfo shortcutInfo = workspaceItemInfo.getDeepShortcutInfo();
                if (shortcutInfo != null) {
                    mStarter.showShortcutBubble(shortcutInfo, getEntryPoint());
                    return;
                }
            }
            if (mItemInfo.getIntent() != null) {
                final Intent intent = new Intent(mItemInfo.getIntent());
                if (intent.getPackage() == null) {
                    intent.setPackage(mItemInfo.getTargetPackage());
                }
                mStarter.showAppBubble(intent, mItemInfo.user, getEntryPoint());
            } else {
                Log.w(TAG, "unable to bubble, no intent: " + mItemInfo);
            }
        }
    }

    public static final Factory<ActivityContext> APP_LOCK =
            (activity, itemInfo, originalView) -> {
                if (itemInfo instanceof ItemInfoWithIcon itemInfoWithIcon) {
                    if (itemInfoWithIcon.isAppLockSupported()) {
                        return AppLockShortcut.newInstance(activity, itemInfo, originalView,
                                itemInfoWithIcon.isAppLockEnabled());
                    }
                }
                return null;
            };
}
