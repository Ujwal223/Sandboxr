/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.sandboxr.launcher.dragndrop;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.sandboxr.launcher.DragSource;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.views.ActivityContext;

/**
 * Base drag listener handling external drag events and cross-window drops.
 */
public abstract class BaseItemDragListener<T extends ActivityContext>
        implements DragController.SystemDragHandler, View.OnDragListener, DragSource,
        DragOptions.PreDragCondition {

    public static final String MIME_TYPE_INTERNAL_APP_SHORTCUT = "vnd.android.launcher3/app-shortcut";
    public static final String MIME_TYPE_INTERNAL_FOLDER = "vnd.android.launcher3/app-folder";
    public static final String MIME_TYPE_INTERNAL_APP_GROUP = "vnd.android.launcher3/app-group";
    public static final String MIME_TYPE_INTERNAL_APP_ACTIVITY = "vnd.android.launcher3/app-item";
    public static final String EXTRA_PIN_ITEM_DRAG_LISTENER = "pin_item_drag_listener";

    protected final Rect mPreviewRect;
    protected final int mPreviewBitmapWidth;
    protected final int mPreviewViewWidth;

    protected final Handler mWorkerHandler = new Handler(Looper.getMainLooper());
    protected DragController mDragController;

    public BaseItemDragListener(Rect previewRect, int previewBitmapWidth, int previewViewWidth) {
        mPreviewRect = previewRect;
        mPreviewBitmapWidth = previewBitmapWidth;
        mPreviewViewWidth = previewViewWidth;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        return onDrag(event);
    }

    @Override
    public boolean onDrag(DragEvent event) {
        return false;
    }

    @Override
    public boolean shouldStartDrag(double distanceDragged) {
        return distanceDragged > 15;
    }

    @Override
    public void onPreDragStart(DropTarget.DragObject dragObject) {}

    @Override
    public void onPreDragEnd(DropTarget.DragObject dragObject, boolean dragStarted) {}

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean success) {}
}
