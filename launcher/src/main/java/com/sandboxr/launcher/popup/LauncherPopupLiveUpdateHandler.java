/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.view.View;
import android.view.ViewGroup;

import com.sandboxr.launcher.BubbleTextView;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.R;
import com.sandboxr.launcher.model.data.ItemInfo;

/**
 * Utility class to handle updates while the popup is visible on the Launcher
 */
public class LauncherPopupLiveUpdateHandler extends PopupLiveUpdateHandler<Launcher> {

    public LauncherPopupLiveUpdateHandler(
            Launcher launcher, PopupContainerWithArrow<Launcher> popupContainerWithArrow) {
        super(launcher, popupContainerWithArrow);
    }

    private View getWidgetsView(ViewGroup container) {
        for (int i = container.getChildCount() - 1; i >= 0; --i) {
            View systemShortcutView = container.getChildAt(i);
            if (systemShortcutView.getTag() instanceof SystemShortcut.Widgets) {
                return systemShortcutView;
            }
        }
        return null;
    }

    @Override
    public void onWidgetsBound() {
        View originalIcon = mPopupContainerWithArrow.getOriginalIcon();
        if (!(originalIcon instanceof BubbleTextView
                && originalIcon.getTag() instanceof ItemInfo)) {
            return;
        }
        ItemInfo info = (ItemInfo) originalIcon.getTag();
        SystemShortcut<?> widgetInfo = SystemShortcut.WIDGETS.getShortcut(mContext, info, originalIcon);
        View widgetsView = getWidgetsView(mPopupContainerWithArrow);
        if (widgetsView == null && mPopupContainerWithArrow.getWidgetContainer() != null) {
            widgetsView = getWidgetsView(mPopupContainerWithArrow.getWidgetContainer());
        }

        if (widgetInfo != null && widgetsView == null) {
            if (mPopupContainerWithArrow.getSystemShortcutContainer() != mPopupContainerWithArrow) {
                if (mPopupContainerWithArrow.getWidgetContainer() == null) {
                    mPopupContainerWithArrow.setWidgetContainer(
                            mPopupContainerWithArrow.inflateAndAdd(
                                    R.layout.widget_shortcut_container,
                                    mPopupContainerWithArrow));
                }
                mPopupContainerWithArrow.initializeWidgetShortcut(
                        mPopupContainerWithArrow.getWidgetContainer(),
                        widgetInfo);
            } else {
                mPopupContainerWithArrow.close(false);
                if (mContext.getPopupControllerForAppIcons() != null) {
                    mContext.getPopupControllerForAppIcons().show(originalIcon);
                }
            }
        } else if (widgetInfo == null && widgetsView != null) {
            if (mPopupContainerWithArrow.getSystemShortcutContainer() != mPopupContainerWithArrow
                    && mPopupContainerWithArrow.getWidgetContainer() != null) {
                mPopupContainerWithArrow.getWidgetContainer().removeView(widgetsView);
            } else {
                mPopupContainerWithArrow.close(false);
                if (mContext.getPopupControllerForAppIcons() != null) {
                    mContext.getPopupControllerForAppIcons().show(originalIcon);
                }
            }
        }
    }
}
