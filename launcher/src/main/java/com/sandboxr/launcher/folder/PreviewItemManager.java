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

package com.sandboxr.launcher.folder;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.sandboxr.launcher.icons.FastBitmapDrawable;
import com.sandboxr.launcher.model.data.FolderInfo;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.ItemInfoWithIcon;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the layout and rendering of up to 4 mini-app icons within a FolderIcon preview.
 */
public class PreviewItemManager {

    private final View mFolderIcon;
    private final PreviewBackground mBackground;
    private final Rect mTempRect = new Rect();

    public PreviewItemManager(View folderIcon, PreviewBackground background) {
        mFolderIcon = folderIcon;
        mBackground = background;
    }

    public void draw(Canvas canvas, FolderInfo info) {
        if (info == null) return;
        ArrayList<ItemInfo> contents = info.getContents();
        int count = Math.min(4, contents.size());
        if (count == 0) return;

        int previewSize = mBackground.previewSize;
        float miniSize = previewSize * 0.40f;
        float padding = (previewSize - 2f * miniSize) / 3f;

        float originX = mBackground.basePreviewOffsetX;
        float originY = mBackground.basePreviewOffsetY;

        for (int i = 0; i < count; i++) {
            ItemInfo item = contents.get(i);
            int col = i % 2;
            int row = i / 2;

            float left = originX + padding + col * (miniSize + padding);
            float top = originY + padding + row * (miniSize + padding);

            mTempRect.set(
                (int) left,
                (int) top,
                (int) (left + miniSize),
                (int) (top + miniSize)
            );

            Drawable iconDrawable = getItemDrawable(item);
            if (iconDrawable != null) {
                iconDrawable.setBounds(mTempRect);
                iconDrawable.draw(canvas);
            }
        }
    }

    private Drawable getItemDrawable(ItemInfo item) {
        if (item instanceof ItemInfoWithIcon) {
            ItemInfoWithIcon itemWithIcon = (ItemInfoWithIcon) item;
            if (itemWithIcon.bitmap != null && itemWithIcon.bitmap.icon != null) {
                return itemWithIcon.bitmap.newIcon(mFolderIcon.getContext());
            }
        }
        return null;
    }
}
