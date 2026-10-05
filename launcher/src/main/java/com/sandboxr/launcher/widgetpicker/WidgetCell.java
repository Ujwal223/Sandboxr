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
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.sandboxr.launcher.DragSource;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.LauncherSettings;
import com.sandboxr.launcher.dragndrop.DragController;
import com.sandboxr.launcher.dragndrop.DragOptions;
import com.sandboxr.launcher.dragndrop.DraggableView;
import com.sandboxr.launcher.model.WidgetItem;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.widget.DatabaseWidgetPreviewLoader;
import com.sandboxr.launcher.widget.PendingAddWidgetInfo;

/**
 * Single widget tile in the widget picker displaying preview, label, and grid dimensions.
 * Built with Sandboxr Liquid Glass aesthetic and supports drag initiation onto the workspace.
 */
public class WidgetCell extends LinearLayout
        implements View.OnLongClickListener, View.OnClickListener, DraggableView, DragSource {

    private static final int DEFAULT_CELL_SIZE = 100; // dp
    private static final int CORNER_RADIUS = 16; // dp
    private static final int BORDER_COLOR = 0x3064D2FF; // subtle specular cyan border
    private static final int BADGE_BG_COLOR = 0x2064D2FF;
    private static final int BADGE_TEXT_COLOR = 0xFF64D2FF;

    private ImageView mWidgetImage;
    private TextView mWidgetTitle;
    private TextView mWidgetDims;

    private WidgetItem mItem;
    private PendingAddWidgetInfo mPendingInfo;
    private DatabaseWidgetPreviewLoader mPreviewLoader;
    private DragSource mDragSource = this;

    public WidgetCell(Context context) {
        this(context, null);
    }

    public WidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setFocusable(true);
        setClickable(true);
        setOnLongClickListener(this);
        setOnClickListener(this);

        int padding = dpToPx(10);
        setPadding(padding, padding, padding, padding);

        // Liquid Glass styling: rounded card with subtle gradient and cyan border
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0x221E1E2E, 0x1A12121A}
        );
        background.setCornerRadius(dpToPx(CORNER_RADIUS));
        background.setStroke(dpToPx(1), BORDER_COLOR);
        setBackground(background);

        // Preview image view
        mWidgetImage = new ImageView(getContext());
        mWidgetImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int imgSize = dpToPx(DEFAULT_CELL_SIZE);
        LayoutParams imgParams = new LayoutParams(imgSize, imgSize);
        imgParams.gravity = Gravity.CENTER_HORIZONTAL;
        imgParams.bottomMargin = dpToPx(8);
        addView(mWidgetImage, imgParams);

        // Title text view
        mWidgetTitle = new TextView(getContext());
        mWidgetTitle.setTextColor(Color.WHITE);
        mWidgetTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        mWidgetTitle.setSingleLine(true);
        mWidgetTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LayoutParams titleParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = dpToPx(4);
        addView(mWidgetTitle, titleParams);

        // Dimension badge pill (e.g., "3 × 2")
        mWidgetDims = new TextView(getContext());
        mWidgetDims.setTextColor(BADGE_TEXT_COLOR);
        mWidgetDims.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        mWidgetDims.setGravity(Gravity.CENTER);
        int badgePadH = dpToPx(8);
        int badgePadV = dpToPx(2);
        mWidgetDims.setPadding(badgePadH, badgePadV, badgePadH, badgePadV);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(BADGE_BG_COLOR);
        badgeBg.setCornerRadius(dpToPx(8));
        mWidgetDims.setBackground(badgeBg);

        LayoutParams dimsParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dimsParams.gravity = Gravity.CENTER_HORIZONTAL;
        addView(mWidgetDims, dimsParams);
    }

    /**
     * Binds the widget item and preview loader to this cell.
     */
    public void applyFromCellItem(WidgetItem item, @Nullable DatabaseWidgetPreviewLoader previewLoader) {
        mItem = item;
        mPreviewLoader = previewLoader;

        if (item.widgetInfo != null) {
            mPendingInfo = new PendingAddWidgetInfo(
                    item.widgetInfo,
                    LauncherSettings.Favorites.CONTAINER_WIDGETS_TRAY
            );
        } else {
            mPendingInfo = null;
        }

        mWidgetTitle.setText(item.label != null ? item.label : "");
        mWidgetDims.setText(String.format("%d × %d", item.spanX, item.spanY));
        setTag(mPendingInfo);

        // Asynchronously load preview bitmap
        int previewSize = dpToPx(DEFAULT_CELL_SIZE);
        if (previewLoader != null) {
            previewLoader.loadPreview(item, previewSize, previewSize, (loadedItem, bitmap) -> {
                if (mItem == loadedItem && bitmap != null) {
                    mWidgetImage.setImageBitmap(bitmap);
                }
            });
        }
    }

    public WidgetItem getWidgetItem() {
        return mItem;
    }

    public PendingAddWidgetInfo getPendingInfo() {
        return mPendingInfo;
    }

    public void setDragSource(DragSource dragSource) {
        mDragSource = dragSource != null ? dragSource : this;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mPendingInfo == null) {
            return false;
        }

        try {
            Launcher launcher = Launcher.getLauncher(getContext());
            DragController dragController = launcher.getDragController();
            if (dragController != null) {
                // Close the widgets sheet so user can position widget directly onto workspace
                AbstractFloatingView.closeOpenViews(
                        launcher,
                        true,
                        AbstractFloatingView.TYPE_WIDGETS_BOTTOM_SHEET
                );

                dragController.startDrag(
                        this,
                        mDragSource,
                        mPendingInfo,
                        new DragOptions()
                );
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    @Override
    public void onClick(View v) {
        // Accessibility / tap announcement
        callOnClick();
    }

    @Override
    public int getViewType() {
        return DraggableView.DRAGGABLE_WIDGET;
    }

    @Override
    public void getSourceBounds(Rect outBounds) {
        int[] pos = new int[2];
        getLocationOnScreen(pos);
        outBounds.set(pos[0], pos[1], pos[0] + getWidth(), pos[1] + getHeight());
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean success) {
        // No-op upon drop complete
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
