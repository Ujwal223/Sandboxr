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

import android.content.ClipDescription;
import android.content.Context;
import android.content.pm.LauncherApps.PinItemRequest;
import android.graphics.Rect;
import android.view.DragEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.sandboxr.launcher.CellLayout;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.android.launcher3.LauncherAppState;
import com.sandboxr.launcher.LauncherSettings;
import com.sandboxr.launcher.LauncherState;
import com.sandboxr.launcher.Workspace;
import com.sandboxr.launcher.model.IModelWriter;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;

/**
 * Drag listener handling external drag events originated from AddItemActivity
 * when a user drags an unconfirmed pinned item directly onto the home screen workspace.
 */
public class PinItemDragListener extends BaseItemDragListener<Launcher> {

    public static final String MIME_TYPE_PIN_SHORTCUT = "vnd.android.launcher3/pin-shortcut";
    public static final String MIME_TYPE_PIN_WIDGET = "vnd.android.launcher3/pin-widget";

    private final PinItemRequest mRequest;
    private Launcher mLauncher;

    public PinItemDragListener(PinItemRequest request, Rect previewRect, int previewBitmapWidth, int previewViewWidth) {
        super(previewRect, previewBitmapWidth, previewViewWidth);
        mRequest = request;
    }

    public PinItemRequest getPinItemRequest() {
        return mRequest;
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    @Override
    public boolean onDrag(DragEvent event) {
        return handleDragEvent(event);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        return handleDragEvent(event);
    }

    private boolean handleDragEvent(DragEvent event) {
        if (mRequest == null || !mRequest.isValid()) {
            return false;
        }

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: {
                ClipDescription desc = event.getClipDescription();
                if (desc == null) return false;
                boolean isShortcut = desc.hasMimeType(MIME_TYPE_PIN_SHORTCUT)
                        || desc.hasMimeType(MIME_TYPE_INTERNAL_APP_SHORTCUT);
                boolean isWidget = desc.hasMimeType(MIME_TYPE_PIN_WIDGET);
                if (!isShortcut && !isWidget) {
                    return false;
                }

                if (mLauncher != null) {
                    mLauncher.getStateManager().goToState(LauncherState.SPRING_LOADED);
                }
                return true;
            }

            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_LOCATION:
                return true;

            case DragEvent.ACTION_DROP: {
                boolean accepted = onDrop(event);
                if (mLauncher != null) {
                    mLauncher.getStateManager().goToState(LauncherState.NORMAL);
                }
                return accepted;
            }

            case DragEvent.ACTION_DRAG_ENDED: {
                if (mLauncher != null) {
                    mLauncher.getStateManager().goToState(LauncherState.NORMAL);
                }
                return true;
            }
        }
        return false;
    }

    protected boolean onDrop(DragEvent event) {
        if (mLauncher == null || mRequest == null || !mRequest.isValid()) {
            return false;
        }

        Workspace<?> workspace = mLauncher.getWorkspace();
        if (workspace == null) return false;

        CellLayout layout = workspace.getCurrentCellLayout();
        if (layout == null) return false;

        int[] cell = new int[2];
        float x = event.getX();
        float y = event.getY();
        layout.pointToCellExact((int) x, (int) y, cell);

        int cellX = Math.max(0, cell[0]);
        int cellY = Math.max(0, cell[1]);
        int screenId = workspace.getCurrentPage();
        int container = LauncherSettings.Favorites.CONTAINER_DESKTOP;

        if (mRequest.getRequestType() == PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            PinShortcutRequestActivityInfo shortcutInfo =
                    new PinShortcutRequestActivityInfo(mRequest, mLauncher);
            WorkspaceItemInfo item = shortcutInfo.createWorkspaceItemInfo(
                    LauncherAppState.getInstance(mLauncher).getIconCache()
            );
            item.container = container;
            item.screenId = screenId;
            item.cellX = cellX;
            item.cellY = cellY;
            item.spanX = 1;
            item.spanY = 1;

            IModelWriter writer = mLauncher.getModelWriter();
            if (writer != null) {
                writer.addItemToDatabase(item, container, screenId, cellX, cellY);
            }
            return mRequest.accept();
        } else if (mRequest.getRequestType() == PinItemRequest.REQUEST_TYPE_APPWIDGET) {
            PinWidgetFlowHandler widgetHandler = new PinWidgetFlowHandler(mRequest, mLauncher);
            int widgetId = 0; // Host allocated ID or placeholder
            LauncherAppWidgetInfo info = widgetHandler.createAppWidgetInfo(
                    widgetId, container, screenId, cellX, cellY
            );
            if (info != null) {
                IModelWriter writer = mLauncher.getModelWriter();
                if (writer != null) {
                    writer.addItemToDatabase(info, container, screenId, cellX, cellY);
                }
            }
            return widgetHandler.finishConfirmation(mLauncher, null);
        }

        return false;
    }
}
