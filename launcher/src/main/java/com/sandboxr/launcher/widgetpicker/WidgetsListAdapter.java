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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.widget.model.WidgetsListBaseEntry;
import com.android.launcher3.widget.model.WidgetsListContentEntry;
import com.android.launcher3.widget.model.WidgetsListHeaderEntry;
import com.sandboxr.launcher.DragSource;
import com.sandboxr.launcher.model.data.PackageItemInfo;
import com.sandboxr.launcher.widget.DatabaseWidgetPreviewLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recycler adapter displaying app headers and expandable horizontal rows of widget cells.
 */
public class WidgetsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_HEADER = 1;
    public static final int VIEW_TYPE_CONTENT = 2;

    private final Context mContext;
    private final DatabaseWidgetPreviewLoader mPreviewLoader;
    private final List<WidgetsListBaseEntry> mAllEntries = new ArrayList<>();
    private final List<WidgetsListBaseEntry> mVisibleEntries = new ArrayList<>();
    private final Set<String> mExpandedPackages = new HashSet<>();
    private String mFilterQuery = "";
    private DragSource mDragSource;

    public WidgetsListAdapter(Context context, @Nullable DatabaseWidgetPreviewLoader previewLoader) {
        mContext = context;
        mPreviewLoader = previewLoader;
    }

    public void setDragSource(DragSource dragSource) {
        mDragSource = dragSource;
    }

    public void setWidgets(List<WidgetsListBaseEntry> entries) {
        mAllEntries.clear();
        if (entries != null) {
            mAllEntries.addAll(entries);
        }
        rebuildVisibleEntries();
    }

    public void setFilter(@Nullable String query) {
        mFilterQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        rebuildVisibleEntries();
    }

    public List<WidgetsListBaseEntry> getVisibleEntries() {
        return mVisibleEntries;
    }

    private void rebuildVisibleEntries() {
        mVisibleEntries.clear();
        boolean hasFilter = !mFilterQuery.isEmpty();

        for (WidgetsListBaseEntry entry : mAllEntries) {
            PackageItemInfo pkg = entry.mPkgItem;
            String pkgName = pkg != null ? pkg.packageName : "";
            String title = pkg != null && pkg.title != null ? pkg.title.toString() : "";

            // If filtering, match app title or any widget label inside
            List<WidgetItem> matchingWidgets = new ArrayList<>();
            if (hasFilter) {
                boolean appMatches = title.toLowerCase(Locale.ROOT).contains(mFilterQuery);
                for (WidgetItem item : entry.mWidgets) {
                    if (appMatches || (item.label != null && item.label.toLowerCase(Locale.ROOT).contains(mFilterQuery))) {
                        matchingWidgets.add(item);
                    }
                }
                if (matchingWidgets.isEmpty()) {
                    continue;
                }
            } else {
                matchingWidgets.addAll(entry.mWidgets);
            }

            boolean isExpanded = hasFilter || mExpandedPackages.contains(pkgName);
            WidgetsListHeaderEntry header = new WidgetsListHeaderEntry(
                    pkg, entry.mTitleSectionName, matchingWidgets, isExpanded
            );
            mVisibleEntries.add(header);

            if (isExpanded && !matchingWidgets.isEmpty()) {
                mVisibleEntries.add(new WidgetsListContentEntry(
                        pkg, entry.mTitleSectionName, matchingWidgets
                ));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        WidgetsListBaseEntry entry = mVisibleEntries.get(position);
        if (entry instanceof WidgetsListContentEntry) {
            return VIEW_TYPE_CONTENT;
        }
        return VIEW_TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CONTENT) {
            HorizontalScrollView scrollView = new HorizontalScrollView(mContext);
            scrollView.setHorizontalScrollBarEnabled(false);
            scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
            LinearLayout contentLayout = new LinearLayout(mContext);
            contentLayout.setOrientation(LinearLayout.HORIZONTAL);
            int pad = dpToPx(8);
            contentLayout.setPadding(pad, 0, pad, pad);
            scrollView.addView(contentLayout, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return new ContentViewHolder(scrollView, contentLayout);
        } else {
            LinearLayout headerLayout = new LinearLayout(mContext);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setGravity(Gravity.CENTER_VERTICAL);
            int padH = dpToPx(16);
            int padV = dpToPx(12);
            headerLayout.setPadding(padH, padV, padH, padV);

            // Translucent glass card header
            GradientDrawable headerBg = new GradientDrawable();
            headerBg.setColor(0x18FFFFFF);
            headerBg.setCornerRadius(dpToPx(12));
            headerLayout.setBackground(headerBg);

            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            headerLayout.setLayoutParams(lp);

            return new HeaderViewHolder(headerLayout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        WidgetsListBaseEntry entry = mVisibleEntries.get(position);

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.bind((WidgetsListHeaderEntry) entry);
        } else if (holder instanceof ContentViewHolder) {
            ContentViewHolder contentHolder = (ContentViewHolder) holder;
            contentHolder.bind((WidgetsListContentEntry) entry);
        }
    }

    @Override
    public int getItemCount() {
        return mVisibleEntries.size();
    }

    public class HeaderViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ImageView mAppIcon;
        private final TextView mAppTitle;
        private final TextView mWidgetCount;
        private final TextView mArrow;
        private WidgetsListHeaderEntry mEntry;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            LinearLayout layout = (LinearLayout) itemView;
            layout.setOnClickListener(this);

            mAppIcon = new ImageView(mContext);
            int iconSize = dpToPx(36);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconLp.rightMargin = dpToPx(12);
            layout.addView(mAppIcon, iconLp);

            LinearLayout textCol = new LinearLayout(mContext);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            );
            layout.addView(textCol, textLp);

            mAppTitle = new TextView(mContext);
            mAppTitle.setTextColor(Color.WHITE);
            mAppTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            mAppTitle.getPaint().setFakeBoldText(true);
            textCol.addView(mAppTitle);

            mWidgetCount = new TextView(mContext);
            mWidgetCount.setTextColor(0xAAFFFFFF);
            mWidgetCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            textCol.addView(mWidgetCount);

            mArrow = new TextView(mContext);
            mArrow.setTextColor(0xFF64D2FF);
            mArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            layout.addView(mArrow);
        }

        public void bind(WidgetsListHeaderEntry entry) {
            mEntry = entry;
            PackageItemInfo pkg = entry.mPkgItem;
            mAppTitle.setText(pkg != null && pkg.title != null ? pkg.title : "");
            if (pkg != null && pkg.bitmap != null && pkg.bitmap.icon != null) {
                mAppIcon.setImageBitmap(pkg.bitmap.icon);
            } else {
                mAppIcon.setImageDrawable(null);
            }

            int count = entry.mWidgets.size();
            mWidgetCount.setText(count + (count == 1 ? " widget" : " widgets"));
            mArrow.setText(entry.isWidgetListShown ? "▲" : "▼");
        }

        @Override
        public void onClick(View v) {
            if (mEntry == null || mEntry.mPkgItem == null) return;
            String pkgName = mEntry.mPkgItem.packageName;
            if (mExpandedPackages.contains(pkgName)) {
                mExpandedPackages.remove(pkgName);
            } else {
                mExpandedPackages.add(pkgName);
            }
            rebuildVisibleEntries();
        }
    }

    public class ContentViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout mContentLayout;

        public ContentViewHolder(@NonNull View itemView, LinearLayout contentLayout) {
            super(itemView);
            mContentLayout = contentLayout;
        }

        public void bind(WidgetsListContentEntry entry) {
            mContentLayout.removeAllViews();
            for (WidgetItem item : entry.mWidgets) {
                WidgetCell cell = new WidgetCell(mContext);
                cell.setDragSource(mDragSource);
                cell.applyFromCellItem(item, mPreviewLoader);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));
                mContentLayout.addView(cell, lp);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics()
        );
    }
}
