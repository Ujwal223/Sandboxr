/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.views;

import android.content.Context;
import com.sandboxr.launcher.views.ActivityContext;

public class OptionsPopupView<T extends Context & ActivityContext> extends com.sandboxr.launcher.views.OptionsPopupView<T> {
    public OptionsPopupView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public OptionsPopupView(Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
