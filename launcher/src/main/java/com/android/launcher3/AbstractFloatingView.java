/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.sandboxr.launcher.util.TouchController;
import com.sandboxr.launcher.views.ActivityContext;
import com.sandboxr.launcher.views.BaseDragLayer;

/**
 * Base class for a View which shows a floating UI on top of the launcher UI.
 */
public abstract class AbstractFloatingView extends LinearLayout implements TouchController {

    public static final int TYPE_FOLDER = 1 << 0;
    public static final int TYPE_ACTION_POPUP = 1 << 1;
    public static final int TYPE_WIDGETS_BOTTOM_SHEET = 1 << 2;
    public static final int TYPE_WIDGET_RESIZE_FRAME = 1 << 3;
    public static final int TYPE_ON_BOARD_POPUP = 1 << 5;
    public static final int TYPE_DISCOVERY_BOUNCE = 1 << 6;
    public static final int TYPE_SNACKBAR = 1 << 7;
    public static final int TYPE_LISTENER = 1 << 8;
    public static final int TYPE_ALL_APPS_EDU = 1 << 9;
    public static final int TYPE_DRAG_DROP_POPUP = 1 << 10;
    public static final int TYPE_TASK_MENU = 1 << 11;
    public static final int TYPE_OPTIONS_POPUP = 1 << 12;
    public static final int TYPE_ICON_SURFACE = 1 << 13;
    public static final int TYPE_OPTIONS_POPUP_DIALOG = 1 << 14;

    public static final int TYPE_ALL = ~0;

    protected boolean mIsOpen = false;

    public AbstractFloatingView(Context context) {
        this(context, null);
    }

    public AbstractFloatingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbstractFloatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    public final void close(boolean animate) {
        handleClose(animate);
        mIsOpen = false;
    }

    protected abstract void handleClose(boolean animate);

    public final boolean isOpen() {
        return mIsOpen;
    }

    protected abstract boolean isOfType(int type);

    public boolean canHandleBack() {
        return true;
    }

    public void onBackInvoked() {
        close(true);
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractFloatingView> T getView(
            ActivityContext activity, int type, boolean mustBeOpen) {
        if (activity == null) return null;
        BaseDragLayer<?> dragLayer = activity.getDragLayer();
        if (dragLayer == null) return null;
        for (int i = dragLayer.getChildCount() - 1; i >= 0; i--) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof AbstractFloatingView) {
                AbstractFloatingView view = (AbstractFloatingView) child;
                if (view.isOfType(type) && (!mustBeOpen || view.isOpen())) {
                    return (T) view;
                }
            }
        }
        return null;
    }

    public static <T extends AbstractFloatingView> T getOpenView(
            ActivityContext activity, int type) {
        return getView(activity, type, true);
    }

    public static boolean hasOpenView(ActivityContext activity, int type) {
        return getOpenView(activity, type) != null;
    }

    public static AbstractFloatingView getTopOpenView(ActivityContext activity) {
        return getOpenView(activity, TYPE_ALL);
    }

    public static void closeOpenViews(ActivityContext activity, boolean animate, int type) {
        if (activity == null) return;
        BaseDragLayer<?> dragLayer = activity.getDragLayer();
        if (dragLayer == null) return;
        for (int i = dragLayer.getChildCount() - 1; i >= 0; i--) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof AbstractFloatingView) {
                AbstractFloatingView view = (AbstractFloatingView) child;
                if (view.isOfType(type)) {
                    view.close(animate);
                }
            }
        }
    }

    public static void closeAllOpenViews(ActivityContext activity, boolean animate) {
        closeOpenViews(activity, animate, TYPE_ALL);
    }

    public static void closeAllOpenViews(ActivityContext activity) {
        closeAllOpenViews(activity, true);
    }
}
