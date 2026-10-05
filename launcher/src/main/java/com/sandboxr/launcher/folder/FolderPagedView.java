/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.sandboxr.launcher.BubbleTextView;
import com.sandboxr.launcher.CellLayout;
import com.sandboxr.launcher.PagedView;
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.WorkspaceItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated horizontal scroll container for folder icon contents.
 * Arranges items across multiple CellLayout pages according to FolderGridOrganizer.
 */
public class FolderPagedView extends PagedView<View> {

    private final FolderGridOrganizer mOrganizer = new FolderGridOrganizer();
    private Folder mFolder;
    private final Point mTempPoint = new Point();

    public FolderPagedView(Context context) {
        this(context, null);
    }

    public FolderPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderPagedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setPageSpacing(0);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    public FolderGridOrganizer getOrganizer() {
        return mOrganizer;
    }

    public CellLayout createAndAddPage() {
        CellLayout page = new CellLayout(getContext(), null, 0, CellLayout.CONTAINER_TYPE_FOLDER);
        page.setCountX(mOrganizer.getCountX());
        page.setCountY(mOrganizer.getCountY());
        addView(page);
        return page;
    }

    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public void bindItems(List<ItemInfo> items) {
        removeAllViews();
        if (items == null || items.isEmpty()) {
            createAndAddPage();
            return;
        }

        mOrganizer.calculateGridSize(items.size());
        int numPages = mOrganizer.getNumPages(items.size());

        for (int p = 0; p < numPages; p++) {
            createAndAddPage();
        }

        for (int i = 0; i < items.size(); i++) {
            ItemInfo item = items.get(i);
            int pageIndex = mOrganizer.getPageForRank(i);
            mOrganizer.getPosForRank(i, mTempPoint);

            CellLayout page = getPageAt(pageIndex);
            if (page != null) {
                View iconView = createIconView(item);
                CellLayoutLayoutParams lp = new CellLayoutLayoutParams(
                    mTempPoint.x, mTempPoint.y, item.spanX > 0 ? item.spanX : 1, item.spanY > 0 ? item.spanY : 1
                );
                page.addViewToCellLayout(iconView, -1, item.id, lp, true);
            }
        }
    }

    private View createIconView(ItemInfo item) {
        BubbleTextView btv = new BubbleTextView(getContext());
        btv.applyFromWorkspaceItem((WorkspaceItemInfo) (item instanceof WorkspaceItemInfo ? item : new WorkspaceItemInfo()), false);
        btv.setTag(item);
        if (mFolder != null) {
            btv.setOnClickListener(mFolder);
            btv.setOnLongClickListener(mFolder);
        }
        return btv;
    }

    public View findViewForItem(ItemInfo targetItem) {
        if (targetItem == null) return null;
        for (int p = 0; p < getChildCount(); p++) {
            CellLayout page = getPageAt(p);
            if (page == null) continue;
            for (int i = 0; i < page.getShortcutsAndWidgets().getChildCount(); i++) {
                View child = page.getShortcutsAndWidgets().getChildAt(i);
                if (child != null && targetItem.equals(child.getTag())) {
                    return child;
                }
            }
        }
        return null;
    }

    public int allocateSpaceForRank(int rank) {
        int page = mOrganizer.getPageForRank(rank);
        while (getChildCount() <= page) {
            createAndAddPage();
        }
        return page;
    }

    public int getAllocatedContentSize() {
        return mFolder != null && mFolder.getInfo() != null
                ? mFolder.getInfo().getContents().size()
                : getChildCount() * mOrganizer.getCountX() * mOrganizer.getCountY();
    }
}
