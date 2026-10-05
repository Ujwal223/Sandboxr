/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.sandboxr.launcher.widget;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.dragndrop.DraggableView;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;

/**
 * AppWidgetHostView customized for SANDBOXR.
 * Implements DraggableView for drag and drop, Liquid Glass rounded corner clipping,
 * and adaptive widget resizing.
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView implements DraggableView, DropTarget {

    private Launcher mLauncher;
    private LauncherAppWidgetProviderInfo mProviderInfo;
    private LauncherAppWidgetInfo mItemInfo;
    private final Rect mTempRect = new Rect();
    private float mScaleToFit = 1.0f;

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (getContext() instanceof Launcher) {
            mLauncher = (Launcher) getContext();
        }
        // Liquid Glass rounded corner outline
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float cornerRadius = 24f * getResources().getDisplayMetrics().density;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);
    }

    public void setAppWidget(int appWidgetId, LauncherAppWidgetProviderInfo info) {
        super.setAppWidget(appWidgetId, info);
        mProviderInfo = info;
    }

    public LauncherAppWidgetProviderInfo getAppWidgetInfo() {
        return mProviderInfo;
    }

    public void setItemInfo(LauncherAppWidgetInfo itemInfo) {
        mItemInfo = itemInfo;
        setTag(itemInfo);
    }

    public LauncherAppWidgetInfo getItemInfo() {
        return mItemInfo;
    }

    @Override
    public int getViewType() {
        return DraggableView.DRAGGABLE_WIDGET;
    }

    @Override
    public void getSourceBounds(Rect outBounds) {
        if (outBounds != null) {
            outBounds.set(0, 0, getWidth(), getHeight());
        }
    }

    public void setScaleToFit(float scale) {
        mScaleToFit = scale;
        setScaleX(scale);
        setScaleY(scale);
    }

    public float getScaleToFit() {
        return mScaleToFit;
    }

    @Override
    public boolean isDropEnabled() {
        return false;
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
    }
}
