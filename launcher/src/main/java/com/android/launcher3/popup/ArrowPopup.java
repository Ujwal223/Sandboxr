/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.popup;

import com.sandboxr.launcher.views.ActivityContext;

public abstract class ArrowPopup<T extends ActivityContext> extends com.sandboxr.launcher.popup.ArrowPopup<T> {
    public ArrowPopup(android.content.Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ArrowPopup(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public ArrowPopup(android.content.Context context) {
        super(context);
    }
}
