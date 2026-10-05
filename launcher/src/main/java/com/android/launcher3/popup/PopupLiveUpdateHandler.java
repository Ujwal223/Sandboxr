/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.popup;

import android.content.Context;
import com.sandboxr.launcher.views.ActivityContext;

public abstract class PopupLiveUpdateHandler<T extends Context & ActivityContext> extends com.sandboxr.launcher.popup.PopupLiveUpdateHandler<T> {
    public PopupLiveUpdateHandler(T context, com.sandboxr.launcher.popup.PopupContainerWithArrow<T> popupContainerWithArrow) {
        super(context, popupContainerWithArrow);
    }
}
