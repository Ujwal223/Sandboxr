/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.AbstractFloatingView;
import com.sandboxr.launcher.CellLayout;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.celllayout.CellLayoutLayoutParams;
import com.sandboxr.launcher.dragndrop.DragLayer;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;
import com.sandboxr.launcher.widget.util.WidgetSizeHandler;

/**
 * Interactive resize overlay frame displaying 4 drag handles around a placed widget.
 * Enforces snap-to-grid constraints within min/max span ranges and dispatches size updates.
 */
public class AppWidgetResizeFrame extends AbstractFloatingView {

    public static final int HANDLE_LEFT = 1;
    public static final int HANDLE_TOP = 2;
    public static final int HANDLE_RIGHT = 4;
    public static final int HANDLE_BOTTOM = 8;

    private LauncherAppWidgetHostView mWidgetView;
    private CellLayout mCellLayout;
    private DragLayer mDragLayer;
    private Launcher mLauncher;

    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mTargetRect = new Rect();
    private final RectF mDrawRect = new RectF();

    private int mActiveHandle = 0;
    private int mDownX, mDownY;
    private int mSpanX, mSpanY;
    private int mMinSpanX, mMinSpanY;
    private int mMaxSpanX, mMaxSpanY;

    public AppWidgetResizeFrame(Context context) {
        this(context, null);
    }

    public AppWidgetResizeFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppWidgetResizeFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        // Liquid Glass cyan accent glow border
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
        mBorderPaint.setColor(Color.parseColor("#FF64D2FF"));

        // Drag handle pills
        mHandlePaint.setStyle(Paint.Style.FILL);
        mHandlePaint.setColor(Color.WHITE);
    }

    public static void showForWidget(LauncherAppWidgetHostView widgetView, CellLayout cellLayout) {
        if (widgetView == null || cellLayout == null) return;
        Context context = widgetView.getContext();
        Launcher launcher = (context instanceof Launcher) ? (Launcher) context : null;
        if (launcher == null) return;

        DragLayer dragLayer = launcher.getDragLayer();
        if (dragLayer == null) return;

        AppWidgetResizeFrame frame = new AppWidgetResizeFrame(launcher);
        frame.setup(widgetView, cellLayout, dragLayer);
        dragLayer.addView(frame);
        frame.mIsOpen = true;
    }

    public void setup(LauncherAppWidgetHostView widgetView, CellLayout cellLayout, DragLayer dragLayer) {
        mWidgetView = widgetView;
        mCellLayout = cellLayout;
        mDragLayer = dragLayer;
        mLauncher = (Launcher) getContext();

        LauncherAppWidgetInfo info = widgetView.getItemInfo();
        LauncherAppWidgetProviderInfo providerInfo = widgetView.getAppWidgetInfo();

        mSpanX = info != null ? info.spanX : 2;
        mSpanY = info != null ? info.spanY : 2;
        mMinSpanX = providerInfo != null ? providerInfo.minSpanX : 1;
        mMinSpanY = providerInfo != null ? providerInfo.minSpanY : 1;
        mMaxSpanX = providerInfo != null ? providerInfo.maxSpanX : 4;
        mMaxSpanY = providerInfo != null ? providerInfo.maxSpanY : 4;

        updateTargetRect();
    }

    private void updateTargetRect() {
        if (mWidgetView != null && mDragLayer != null) {
            mDragLayer.getDescendantRectRelativeToSelf(mWidgetView, mTargetRect);
            int margin = (int) (8 * getResources().getDisplayMetrics().density);
            mTargetRect.inset(-margin, -margin);
            mDrawRect.set(mTargetRect);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = 24f * getResources().getDisplayMetrics().density;
        canvas.drawRoundRect(mDrawRect, radius, radius, mBorderPaint);

        // Draw handles on 4 sides
        float handleRadius = 6f * getResources().getDisplayMetrics().density;
        float handleLength = 24f * getResources().getDisplayMetrics().density;

        // Top handle
        canvas.drawRoundRect(
            mDrawRect.centerX() - handleLength / 2, mDrawRect.top - handleRadius,
            mDrawRect.centerX() + handleLength / 2, mDrawRect.top + handleRadius,
            handleRadius, handleRadius, mHandlePaint
        );
        // Bottom handle
        canvas.drawRoundRect(
            mDrawRect.centerX() - handleLength / 2, mDrawRect.bottom - handleRadius,
            mDrawRect.centerX() + handleLength / 2, mDrawRect.bottom + handleRadius,
            handleRadius, handleRadius, mHandlePaint
        );
        // Left handle
        canvas.drawRoundRect(
            mDrawRect.left - handleRadius, mDrawRect.centerY() - handleLength / 2,
            mDrawRect.left + handleRadius, mDrawRect.centerY() + handleLength / 2,
            handleRadius, handleRadius, mHandlePaint
        );
        // Right handle
        canvas.drawRoundRect(
            mDrawRect.right - handleRadius, mDrawRect.centerY() - handleLength / 2,
            mDrawRect.right + handleRadius, mDrawRect.centerY() + handleLength / 2,
            handleRadius, handleRadius, mHandlePaint
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mActiveHandle = getTouchedHandle(x, y);
                mDownX = x;
                mDownY = y;
                if (mActiveHandle != 0) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    return true;
                } else if (!mTargetRect.contains(x, y)) {
                    // Tap outside dismisses frame
                    close(true);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActiveHandle != 0) {
                    handleDrag(x, y);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mActiveHandle != 0) {
                    commitResize();
                    mActiveHandle = 0;
                    return true;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private int getTouchedHandle(int x, int y) {
        int touchRadius = (int) (32 * getResources().getDisplayMetrics().density);
        if (Math.abs(y - mTargetRect.top) < touchRadius && Math.abs(x - mTargetRect.centerX()) < touchRadius * 2) {
            return HANDLE_TOP;
        }
        if (Math.abs(y - mTargetRect.bottom) < touchRadius && Math.abs(x - mTargetRect.centerX()) < touchRadius * 2) {
            return HANDLE_BOTTOM;
        }
        if (Math.abs(x - mTargetRect.left) < touchRadius && Math.abs(y - mTargetRect.centerY()) < touchRadius * 2) {
            return HANDLE_LEFT;
        }
        if (Math.abs(x - mTargetRect.right) < touchRadius && Math.abs(y - mTargetRect.centerY()) < touchRadius * 2) {
            return HANDLE_RIGHT;
        }
        return 0;
    }

    private void handleDrag(int x, int y) {
        if (mCellLayout == null) return;
        int deltaX = x - mDownX;
        int deltaY = y - mDownY;

        int cellWidth = mCellLayout.getCellWidth() > 0 ? mCellLayout.getCellWidth() : 200;
        int cellHeight = mCellLayout.getCellHeight() > 0 ? mCellLayout.getCellHeight() : 200;

        int spanXDelta = Math.round((float) deltaX / cellWidth);
        int spanYDelta = Math.round((float) deltaY / cellHeight);

        int newSpanX = mSpanX;
        int newSpanY = mSpanY;

        if ((mActiveHandle & HANDLE_RIGHT) != 0) {
            newSpanX = Math.min(mMaxSpanX, Math.max(mMinSpanX, mSpanX + spanXDelta));
        } else if ((mActiveHandle & HANDLE_LEFT) != 0) {
            newSpanX = Math.min(mMaxSpanX, Math.max(mMinSpanX, mSpanX - spanXDelta));
        }

        if ((mActiveHandle & HANDLE_BOTTOM) != 0) {
            newSpanY = Math.min(mMaxSpanY, Math.max(mMinSpanY, mSpanY + spanYDelta));
        } else if ((mActiveHandle & HANDLE_TOP) != 0) {
            newSpanY = Math.min(mMaxSpanY, Math.max(mMinSpanY, mSpanY - spanYDelta));
        }

        if (newSpanX != mSpanX || newSpanY != mSpanY) {
            mSpanX = newSpanX;
            mSpanY = newSpanY;
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            applyCurrentSpans();
        }
    }

    private void applyCurrentSpans() {
        if (mWidgetView != null) {
            ViewGroup.LayoutParams lp = mWidgetView.getLayoutParams();
            if (lp instanceof CellLayoutLayoutParams) {
                CellLayoutLayoutParams clp = (CellLayoutLayoutParams) lp;
                clp.setCellHSpan(mSpanX);
                clp.setCellVSpan(mSpanY);
                mWidgetView.requestLayout();
                updateTargetRect();
            }
        }
    }

    private void commitResize() {
        if (mWidgetView != null) {
            LauncherAppWidgetInfo info = mWidgetView.getItemInfo();
            if (info != null) {
                info.spanX = mSpanX;
                info.spanY = mSpanY;
                if (mLauncher != null && mLauncher.getModelWriter() != null) {
                    mLauncher.getModelWriter().updateItemInDatabase(info);
                }
            }
        }
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!mIsOpen) return;
        mIsOpen = false;
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    @Override
    public boolean isOfType(int type) {
        return (type & TYPE_WIDGET_RESIZE_FRAME) != 0;
    }

    public int getSpanX() {
        return mSpanX;
    }

    public int getSpanY() {
        return mSpanY;
    }
}
