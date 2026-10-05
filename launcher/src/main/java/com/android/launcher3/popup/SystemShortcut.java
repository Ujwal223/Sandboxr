/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.popup;

import android.view.View;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.views.ActivityContext;

public abstract class SystemShortcut<T extends ActivityContext> extends com.sandboxr.launcher.popup.SystemShortcut<T> {
    public SystemShortcut(int iconResId, int labelResId, T target, ItemInfo itemInfo, View originalView) {
        super(iconResId, labelResId, target, itemInfo, originalView);
    }

    public SystemShortcut(int iconResId, int labelResId, T target, ItemInfo itemInfo, View originalView, boolean isCollapsible) {
        super(iconResId, labelResId, target, itemInfo, originalView, isCollapsible);
    }
}
