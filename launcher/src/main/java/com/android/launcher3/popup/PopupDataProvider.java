/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.popup;

import com.sandboxr.launcher.views.ActivityContext;
import com.sandboxr.launcher.notification.NotificationRepository;
import com.sandboxr.launcher.allapps.AllAppsStore;
import com.sandboxr.launcher.model.BgDataModel;

public class PopupDataProvider extends com.sandboxr.launcher.popup.PopupDataProvider {
    public PopupDataProvider(ActivityContext context, NotificationRepository notificationRepository, AllAppsStore appsStore, BgDataModel dataModel) {
        super(context, notificationRepository, appsStore, dataModel);
    }
}
