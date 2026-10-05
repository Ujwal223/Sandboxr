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

package com.sandboxr.launcher.widgetpicker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.widget.model.WidgetsListBaseEntry;
import com.sandboxr.launcher.FastScrollRecyclerView;

import java.util.List;

/**
 * Fast-scrollable RecyclerView hosting the widget picker entries.
 * Synchronizes with fast-scroller thumb and alphabet index overlays.
 */
public class WidgetsRecyclerView extends FastScrollRecyclerView {

    private WidgetsListAdapter mAdapter;

    public WidgetsRecyclerView(Context context) {
        this(context, null);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutManager(new LinearLayoutManager(getContext()));
        setHasFixedSize(false);
        setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        super.setAdapter(adapter);
        if (adapter instanceof WidgetsListAdapter) {
            mAdapter = (WidgetsListAdapter) adapter;
        } else {
            mAdapter = null;
        }
    }

    public WidgetsListAdapter getWidgetsListAdapter() {
        return mAdapter;
    }

    @Override
    public void onUpdateScrollbar(int dy) {
        if (mAdapter == null) return;
        int count = mAdapter.getItemCount();
        if (count == 0) {
            if (mScrollbar != null) {
                mScrollbar.setThumbOffsetY(-1);
            }
            return;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        if (layoutManager == null) return;

        int firstPos = layoutManager.findFirstVisibleItemPosition();
        if (firstPos == RecyclerView.NO_POSITION) {
            if (mScrollbar != null) {
                mScrollbar.setThumbOffsetY(-1);
            }
            return;
        }

        int availableScrollHeight = getAvailableScrollHeight();
        if (availableScrollHeight <= 0) {
            if (mScrollbar != null) {
                mScrollbar.setThumbOffsetY(-1);
            }
            return;
        }

        int scrollY = (int) (((float) firstPos / count) * availableScrollHeight);
        synchronizeScrollBarThumbOffsetToViewScroll(scrollY, availableScrollHeight);
    }

    @Override
    public CharSequence scrollToPositionAtProgress(float touchFraction) {
        if (mAdapter == null) return "";
        List<WidgetsListBaseEntry> entries = mAdapter.getVisibleEntries();
        if (entries == null || entries.isEmpty()) return "";

        int targetIndex = (int) (touchFraction * entries.size());
        targetIndex = Math.max(0, Math.min(targetIndex, entries.size() - 1));

        stopScroll();
        LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(targetIndex, 0);
        }

        WidgetsListBaseEntry entry = entries.get(targetIndex);
        return entry.mTitleSectionName != null ? entry.mTitleSectionName : "";
    }
}
