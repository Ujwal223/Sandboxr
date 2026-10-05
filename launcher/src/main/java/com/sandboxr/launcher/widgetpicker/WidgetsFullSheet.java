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

package com.sandboxr.launcher.widgetpicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.widget.model.WidgetsListBaseEntriesBuilder;
import com.android.launcher3.widget.model.WidgetsListBaseEntry;
import com.sandboxr.launcher.DragSource;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.ExtendedEditText;
import com.sandboxr.launcher.Insettable;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.views.BaseDragLayer;
import com.sandboxr.launcher.dragndrop.DragLayer;
import com.sandboxr.launcher.model.WidgetsModel;
import com.sandboxr.launcher.widget.DatabaseWidgetPreviewLoader;

import java.util.List;

/**
 * Interactive bottom sheet displaying available app widgets and shortcuts for user placement.
 * Features Liquid Glass aesthetics, search filtering, fast scrolling, and drag-and-drop integration.
 */
public class WidgetsFullSheet extends AbstractFloatingView
        implements DragSource, Insettable, TextWatcher {

    private static final int CORNER_RADIUS = 28; // dp
    private static final int BG_COLOR = 0xEA12121A; // Dark translucent glass
    private static final int BORDER_COLOR = 0x4064D2FF; // Cyan specular rim
    private static final int HANDLE_COLOR = 0x50FFFFFF;

    private Launcher mLauncher;
    private View mDragHandle;
    private TextView mTitleText;
    private ExtendedEditText mSearchBar;
    private WidgetsRecyclerView mRecyclerView;
    private WidgetsListAdapter mAdapter;
    private DatabaseWidgetPreviewLoader mPreviewLoader;

    private final Rect mInsets = new Rect();
    private boolean mIsAnimating = false;

    public WidgetsFullSheet(Context context) {
        this(context, null);
    }

    public WidgetsFullSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsFullSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setClickable(true);
        setFocusable(true);

        // Liquid Glass styling: rounded top edges with specular rim
        GradientDrawable background = new GradientDrawable();
        background.setColor(BG_COLOR);
        background.setCornerRadii(new float[]{
                dpToPx(CORNER_RADIUS), dpToPx(CORNER_RADIUS),
                dpToPx(CORNER_RADIUS), dpToPx(CORNER_RADIUS),
                0, 0, 0, 0
        });
        background.setStroke(dpToPx(1), BORDER_COLOR);
        setBackground(background);

        int padH = dpToPx(16);
        int padTop = dpToPx(12);
        setPadding(padH, padTop, padH, 0);

        // Top drag handle pill
        mDragHandle = new View(getContext());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(HANDLE_COLOR);
        handleBg.setCornerRadius(dpToPx(2));
        mDragHandle.setBackground(handleBg);
        LayoutParams handleLp = new LayoutParams(dpToPx(36), dpToPx(4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin = dpToPx(12);
        addView(mDragHandle, handleLp);

        // Header Title
        mTitleText = new TextView(getContext());
        mTitleText.setText("Widgets");
        mTitleText.setTextColor(Color.WHITE);
        mTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        mTitleText.getPaint().setFakeBoldText(true);
        LayoutParams titleLp = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleLp.bottomMargin = dpToPx(12);
        addView(mTitleText, titleLp);

        // Search Input Bar
        mSearchBar = new ExtendedEditText(getContext());
        mSearchBar.setHint("Search widgets...");
        mSearchBar.setHintTextColor(0x70FFFFFF);
        mSearchBar.setTextColor(Color.WHITE);
        mSearchBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mSearchBar.setSingleLine(true);
        int searchPad = dpToPx(12);
        mSearchBar.setPadding(searchPad, searchPad, searchPad, searchPad);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(0x18FFFFFF);
        searchBg.setCornerRadius(dpToPx(20));
        searchBg.setStroke(dpToPx(1), 0x3064D2FF);
        mSearchBar.setBackground(searchBg);
        mSearchBar.addTextChangedListener(this);

        LayoutParams searchLp = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        searchLp.bottomMargin = dpToPx(12);
        addView(mSearchBar, searchLp);

        // RecyclerView with Widgets
        mPreviewLoader = new DatabaseWidgetPreviewLoader(getContext());
        mAdapter = new WidgetsListAdapter(getContext(), mPreviewLoader);
        mAdapter.setDragSource(this);

        mRecyclerView = new WidgetsRecyclerView(getContext());
        mRecyclerView.setAdapter(mAdapter);

        LayoutParams recyclerLp = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        addView(mRecyclerView, recyclerLp);
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    public static WidgetsFullSheet show(Launcher launcher, boolean animate) {
        // Close any existing sheet
        AbstractFloatingView.closeOpenViews(
                launcher,
                false,
                AbstractFloatingView.TYPE_WIDGETS_BOTTOM_SHEET
        );

        WidgetsFullSheet sheet = new WidgetsFullSheet(launcher);
        sheet.setLauncher(launcher);
        sheet.open(animate);
        return sheet;
    }

    public void open(boolean animate) {
        if (mIsOpen || mLauncher == null) return;
        mIsOpen = true;

        DragLayer dragLayer = mLauncher.getDragLayer();
        if (dragLayer == null) return;

        if (getParent() == null) {
            dragLayer.addView(this);
        }

        loadWidgets();

        int screenWidth = dragLayer.getWidth() > 0 ? dragLayer.getWidth() : 1080;
        int screenHeight = dragLayer.getHeight() > 0 ? dragLayer.getHeight() : 1920;

        int sheetHeight = (int) (screenHeight * 0.75f);
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(screenWidth, sheetHeight);
        lp.x = 0;
        lp.y = screenHeight - sheetHeight;
        lp.customPosition = true;
        setLayoutParams(lp);

        measure(
                MeasureSpec.makeMeasureSpec(screenWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(sheetHeight, MeasureSpec.EXACTLY)
        );
        layout(lp.x, lp.y, lp.x + screenWidth, lp.y + sheetHeight);

        if (animate) {
            setTranslationY(sheetHeight);
            mIsAnimating = true;
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, sheetHeight, 0f);
            anim.setDuration(280);
            anim.setInterpolator(new DecelerateInterpolator(1.8f));
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsAnimating = false;
                }
            });
            anim.start();
        } else {
            setTranslationY(0f);
        }
    }

    public void loadWidgets() {
        if (mLauncher == null) return;
        try {
            WidgetsModel model = new WidgetsModel(mLauncher);
            WidgetsListBaseEntriesBuilder builder = new WidgetsListBaseEntriesBuilder(mLauncher);
            List<WidgetsListBaseEntry> entries = builder.build(model.getWidgetsByPackageItem());
            mAdapter.setWidgets(entries);
        } catch (Exception ignored) {}
    }

    public void setWidgets(List<WidgetsListBaseEntry> entries) {
        mAdapter.setWidgets(entries);
    }

    public WidgetsRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public WidgetsListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!mIsOpen) return;
        mIsOpen = false;

        if (mSearchBar != null) {
            mSearchBar.hideKeyboard();
        }

        if (animate && getParent() != null) {
            mIsAnimating = true;
            int sheetHeight = getHeight() > 0 ? getHeight() : 1000;
            ObjectAnimator anim = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, getTranslationY(), sheetHeight);
            anim.setDuration(220);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsAnimating = false;
                    removeFromParent();
                }
            });
            anim.start();
        } else {
            removeFromParent();
        }
    }

    private void removeFromParent() {
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    @Override
    public boolean isOfType(int type) {
        return (type & TYPE_WIDGETS_BOTTOM_SHEET) != 0;
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        setPadding(
                getPaddingLeft(),
                getPaddingTop(),
                getPaddingRight(),
                insets.bottom + dpToPx(8)
        );
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean success) {
        // Completed drop
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            BaseDragLayer<?> dl = mLauncher != null ? mLauncher.getDragLayer() : null;
            if (dl != null && !dl.isEventOverView(this, ev)) {
                close(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mAdapter != null) {
            mAdapter.setFilter(s != null ? s.toString() : null);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
