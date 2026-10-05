/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.sandboxr.launcher;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.sandboxr.launcher.dragndrop.DragController;
import com.sandboxr.launcher.dragndrop.DragOptions;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.views.ActivityContext;

/**
 * Base drop target button (e.g. Delete, Uninstall, App Info) displayed in the DropTargetBar.
 * Features Liquid Glass glow styling and responsive layout handling.
 */
public abstract class ButtonDropTarget extends TextView 
        implements DropTarget, DragController.DragListener, View.OnClickListener {

    public static final int TOOLTIP_DEFAULT = 0;
    public static final int TOOLTIP_LEFT = 1;
    public static final int TOOLTIP_RIGHT = 2;

    protected static final TimeInterpolator HOVER_INTERPOLATOR = new DecelerateInterpolator();
    protected static final int HOVER_DURATION = 150;

    protected boolean mActive = false;
    protected DropTargetBar mDropTargetBar;
    protected DropTargetHandler mDropTargetHandler;

    protected CharSequence mText;
    protected Drawable mDrawable;
    protected boolean mTextVisible = true;
    protected boolean mIconVisible = true;
    protected boolean mTextMultiLine = false;
    protected int mToolTipLocation = TOOLTIP_DEFAULT;

    public ButtonDropTarget(Context context) {
        this(context, null, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnClickListener(this);
        mText = getText();
        if (getContext() instanceof Launcher) {
            mDropTargetHandler = new DropTargetHandler((Launcher) getContext());
        }
    }

    public void setDropTargetBar(DropTargetBar bar) {
        mDropTargetBar = bar;
    }

    public void setToolTipLocation(int location) {
        mToolTipLocation = location;
    }

    public void setTextVisible(boolean visible) {
        mTextVisible = visible;
        setText(visible ? mText : "");
    }

    public void setIconVisible(boolean visible) {
        mIconVisible = visible;
        updateCompoundDrawables();
    }

    public void setTextMultiLine(boolean multiLine) {
        mTextMultiLine = multiLine;
        setSingleLine(!multiLine);
        setMaxLines(multiLine ? 2 : 1);
    }

    public void setDrawable(int resId) {
        setDrawable(ContextCompat.getDrawable(getContext(), resId));
    }

    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
        if (mDrawable != null) {
            int size = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 20f, getResources().getDisplayMetrics());
            mDrawable.setBounds(0, 0, size, size);
        }
        updateCompoundDrawables();
    }

    private void updateCompoundDrawables() {
        Drawable draw = (mIconVisible && mDrawable != null) ? mDrawable : null;
        setCompoundDrawablesRelative(draw, null, null, null);
    }

    public boolean isTextTruncated(int availableWidth) {
        if (TextUtils.isEmpty(getText())) return false;
        float textWidth = getPaint().measureText(getText().toString());
        int padding = getCompoundPaddingLeft() + getCompoundPaddingRight();
        return (textWidth + padding) > availableWidth;
    }

    public float resizeTextToFit() {
        return getTextSize();
    }

    public abstract boolean supportsDrop(ItemInfo info);

    @Override
    public boolean isDropEnabled() {
        return mActive && isEnabled() && getVisibility() == VISIBLE;
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        mActive = dragObject != null && dragObject.dragInfo != null && supportsDrop(dragObject.dragInfo);
        setVisibility(mActive ? VISIBLE : GONE);
    }

    @Override
    public void onDragEnd() {
        mActive = false;
        setSelected(false);
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(HOVER_DURATION).start();
    }

    @Override
    public void onDragEnter(DropTarget.DragObject dragObject) {
        if (!isDropEnabled()) return;
        setSelected(true);
        // Liquid Glass hover feedback: subtle scale lift and opacity increase
        animate().scaleX(1.08f).scaleY(1.08f)
                .setDuration(HOVER_DURATION)
                .setInterpolator(HOVER_INTERPOLATOR)
                .start();
    }

    @Override
    public void onDragOver(DropTarget.DragObject dragObject) {}

    @Override
    public void onDragExit(DropTarget.DragObject dragObject) {
        setSelected(false);
        animate().scaleX(1.0f).scaleY(1.0f)
                .setDuration(HOVER_DURATION)
                .setInterpolator(HOVER_INTERPOLATOR)
                .start();
    }

    @Override
    public void onDrop(DropTarget.DragObject dragObject, Object options) {
        setSelected(false);
        animate().scaleX(1.0f).scaleY(1.0f).setDuration(HOVER_DURATION).start();
        completeDrop(dragObject);
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragObject) {
        return isDropEnabled() && dragObject != null && dragObject.dragInfo != null && supportsDrop(dragObject.dragInfo);
    }

    public Rect getIconRect(DropTarget.DragObject dragObject) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        return new Rect(loc[0], loc[1], loc[0] + getWidth(), loc[1] + getHeight());
    }

    public abstract void completeDrop(DropTarget.DragObject dragObject);

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        getHitRect(outRect);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        return false;
    }

    @Override
    public void onClick(View v) {
        // Accessibility click handler
    }
}
