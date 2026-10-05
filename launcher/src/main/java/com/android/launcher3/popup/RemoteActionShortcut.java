/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.popup;

import android.content.Context;
import com.sandboxr.launcher.views.ActivityContext;

public class RemoteActionShortcut<T extends Context & ActivityContext> extends com.sandboxr.launcher.popup.RemoteActionShortcut<T> {
    public RemoteActionShortcut(android.app.RemoteAction action, T context, com.sandboxr.launcher.model.data.ItemInfo itemInfo, android.view.View originalView) {
        super(action, context, itemInfo, originalView);
    }
}
