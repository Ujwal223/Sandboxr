/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3;

import android.view.View;
import com.sandboxr.launcher.views.ActivityContext;

public abstract class AbstractFloatingView extends View {
    public static final int TYPE_SNACKBAR = 1 << 6;

    public AbstractFloatingView(android.content.Context context) {
        super(context);
    }

    public static void closeOpenViews(ActivityContext activity, boolean animate, int type) {
        // Compatibility stub for closing open views
    }
}
