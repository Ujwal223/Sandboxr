/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.sandboxr.launcher.DragSource;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.util.TouchController;
import com.sandboxr.launcher.views.ActivityContext;
import com.sandboxr.launcher.views.BaseDragLayer;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Core engine initiating, managing, tracking, and dropping items across views.
 */
public class DragController implements DragDriver.EventListener, TouchController {

    public interface DragListener {
        void onDragStart(DropTarget.DragObject dragObject, DragOptions options);
        void onDragEnd();
    }

    public interface SystemDragHandler {
        boolean onDrag(DragEvent event);
    }

    protected final ActivityContext mActivity;
    protected final Rect mRectTemp = new Rect();
    protected final int[] mCoordinatesTemp = new int[2];

    protected DragDriver mDragDriver = null;
    public DragOptions mOptions;
    protected final Point mMotionDown = new Point();
    protected final Point mLastTouch = new Point();

    public DropTarget.DragObject mDragObject;
    protected final ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    protected final ArrayList<DragListener> mListeners = new ArrayList<>();

    protected DropTarget mLastDropTarget;
    protected boolean mDragging = false;
    protected boolean mIsInPreDrag = false;

    public DragController(ActivityContext activity) {
        mActivity = activity;
    }

    public boolean isDragging() {
        return mDragging;
    }

    public int getX(MotionEvent ev) {
        return Math.round(ev.getX());
    }

    public int getY(MotionEvent ev) {
        return Math.round(ev.getY());
    }

    public void addDropTarget(DropTarget target) {
        if (!mDropTargets.contains(target)) {
            mDropTargets.add(target);
        }
    }

    public void removeDropTarget(DropTarget target) {
        mDropTargets.remove(target);
    }

    public void addDragListener(DragListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeDragListener(DragListener listener) {
        mListeners.remove(listener);
    }

    public DragView startDrag(
            View view,
            DragSource source,
            ItemInfo dragInfo,
            DragOptions options
    ) {
        int[] loc = new int[2];
        if (view != null) {
            view.getLocationInWindow(loc);
        }
        Rect dragRegion = new Rect(0, 0, view != null ? view.getWidth() : 120, view != null ? view.getHeight() : 120);
        return startDrag(
                null,
                view,
                view instanceof DraggableView ? (DraggableView) view : null,
                loc[0],
                loc[1],
                source,
                dragInfo,
                dragRegion,
                1.0f,
                1.0f,
                options
        );
    }

    public DragView startDrag(
            @Nullable Drawable drawable,
            @Nullable View view,
            DraggableView originalView,
            int dragLayerX,
            int dragLayerY,
            DragSource source,
            ItemInfo dragInfo,
            Rect dragRegion,
            float initialDragViewScale,
            float dragViewScaleOnDrop,
            DragOptions options
    ) {
        mOptions = options != null ? options : new DragOptions();

        BaseDragLayer<?> dragLayer = mActivity.getDragLayer();
        if (dragLayer == null) return null;

        mMotionDown.set(dragLayerX, dragLayerY);
        mLastTouch.set(dragLayerX, dragLayerY);

        DragView dragView = createDragView(
                drawable, view, originalView, dragInfo, dragLayerX, dragLayerY,
                dragRegion, initialDragViewScale, dragViewScaleOnDrop, true
        );

        int viewWidth = (dragRegion != null && dragRegion.width() > 0) ? dragRegion.width() : 120;
        int viewHeight = (dragRegion != null && dragRegion.height() > 0) ? dragRegion.height() : 120;
        BaseDragLayer.LayoutParams lp = new BaseDragLayer.LayoutParams(viewWidth, viewHeight);
        lp.x = dragLayerX - dragView.getRegistrationX();
        lp.y = dragLayerY - dragView.getRegistrationY();
        lp.customPosition = true;
        dragLayer.addView(dragView, lp);

        mDragObject = new DropTarget.DragObject();
        mDragObject.x = dragLayerX;
        mDragObject.y = dragLayerY;
        mDragObject.xOffset = dragView.getRegistrationX();
        mDragObject.yOffset = dragView.getRegistrationY();
        mDragObject.dragView = dragView;
        mDragObject.dragInfo = dragInfo;
        mDragObject.dragSource = source;

        mDragging = true;
        mIsInPreDrag = mOptions.preDragCondition != null;

        if (mIsInPreDrag) {
            mOptions.preDragCondition.onPreDragStart(mDragObject);
        } else {
            notifyDragStart();
        }

        mDragDriver = DragDriver.create(this, mOptions, getSecondaryEventConsumer());
        onDragViewInitialized();
        return dragView;
    }

    protected void notifyDragStart() {
        for (DragListener listener : new ArrayList<>(mListeners)) {
            listener.onDragStart(mDragObject, mOptions);
        }
    }

    protected void onDragViewInitialized() {}

    protected Consumer<MotionEvent> getSecondaryEventConsumer() {
        return null;
    }

    protected DragView createDragView(
            @Nullable Drawable drawable,
            @Nullable View view,
            DraggableView originalView,
            ItemInfo dragInfo,
            int dragLayerX,
            int dragLayerY,
            Rect dragRegion,
            float initialDragViewScale,
            float dragViewScaleOnDrop,
            boolean allowSpringDrawable
    ) {
        int regX = (dragRegion != null && dragRegion.width() > 0) ? dragRegion.width() / 2 : 60;
        int regY = (dragRegion != null && dragRegion.height() > 0) ? dragRegion.height() / 2 : 60;

        if (drawable != null) {
            return new DragView(mActivity.asContext(), drawable, regX, regY,
                    initialDragViewScale, dragViewScaleOnDrop, 0f, allowSpringDrawable);
        } else if (view != null) {
            return new DragView(mActivity.asContext(), view, view.getMeasuredWidth(), view.getMeasuredHeight(),
                    regX, regY, initialDragViewScale, dragViewScaleOnDrop, 0f, allowSpringDrawable);
        } else {
            return new DragView(mActivity.asContext(), new android.graphics.drawable.ColorDrawable(0xFF4E80EE),
                    regX, regY, initialDragViewScale, dragViewScaleOnDrop, 0f, allowSpringDrawable);
        }
    }

    @Override
    public void onDriverDragMove(float x, float y) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        mLastTouch.set(ix, iy);

        if (mDragObject != null && mDragObject.dragView != null) {
            ((DragView) mDragObject.dragView).move(ix, iy);
        }

        if (mIsInPreDrag && mOptions.preDragCondition != null) {
            double distance = Math.hypot(ix - mMotionDown.x, iy - mMotionDown.y);
            if (mOptions.preDragCondition.shouldStartDrag(distance)) {
                mIsInPreDrag = false;
                mOptions.preDragCondition.onPreDragEnd(mDragObject, true);
                notifyDragStart();
            }
        }

        handleMoveEvent(ix, iy);
    }

    protected void handleMoveEvent(int x, int y) {
        if (mDragObject == null) return;
        mDragObject.x = x;
        mDragObject.y = y;

        int[] dropCoordinates = mCoordinatesTemp;
        DropTarget target = findDropTarget(x, y, dropCoordinates);

        if (target != mLastDropTarget) {
            if (mLastDropTarget != null) {
                mLastDropTarget.onDragExit(mDragObject);
            }
            if (target != null && target.isDropEnabled()) {
                target.onDragEnter(mDragObject);
            }
        }

        mLastDropTarget = target;
        if (target != null && target.isDropEnabled()) {
            target.onDragOver(mDragObject);
        }
    }

    @Nullable
    protected DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        mRectTemp.setEmpty();
        for (int i = mDropTargets.size() - 1; i >= 0; i--) {
            DropTarget target = mDropTargets.get(i);
            if (!target.isDropEnabled()) continue;
            target.getHitRectRelativeToDragLayer(mRectTemp);
            if (mRectTemp.contains(x, y)) {
                dropCoordinates[0] = x;
                dropCoordinates[1] = y;
                return target;
            }
        }
        return getDefaultDropTarget(dropCoordinates);
    }

    protected DropTarget getDefaultDropTarget(int[] dropCoordinates) {
        return null;
    }

    @Override
    public void onDriverDragEnterWindow() {}

    @Override
    public void onDriverDragExitWindow() {}

    @Override
    public void onDriverDragEnd(float x, float y) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        mLastTouch.set(ix, iy);

        if (endWithFlingAnimation()) {
            return;
        }

        int[] dropCoordinates = mCoordinatesTemp;
        DropTarget target = findDropTarget(ix, iy, dropCoordinates);
        drop(target, null);
    }

    protected boolean endWithFlingAnimation() {
        return false;
    }

    public void drop(@Nullable DropTarget dropTarget, @Nullable Runnable flingAnimation) {
        if (!mDragging) return;

        boolean accepted = false;
        if (dropTarget != null && dropTarget.isDropEnabled() && dropTarget.acceptDrop(mDragObject)) {
            dropTarget.onDrop(mDragObject, mOptions);
            accepted = true;
        }

        if (mDragObject.dragSource != null) {
            mDragObject.dragSource.onDropCompleted((dropTarget instanceof View) ? (View) dropTarget : null,
                    mDragObject, accepted);
        }

        if (flingAnimation != null) {
            flingAnimation.run();
        } else if (mDragObject.dragView != null) {
            ((DragView) mDragObject.dragView).animateTo(mLastTouch.x, mLastTouch.y, null, 200);
        }

        endDrag();
    }

    @Override
    public void onDriverDragCancel() {
        cancelDrag();
    }

    public void cancelDrag() {
        if (!mDragging) return;
        mDragObject.cancelled = true;

        if (mLastDropTarget != null) {
            mLastDropTarget.onDragExit(mDragObject);
            mLastDropTarget = null;
        }

        if (mDragObject.dragSource != null) {
            mDragObject.dragSource.onDropCompleted(null, mDragObject, false);
        }

        if (mDragObject.dragView != null) {
            ((DragView) mDragObject.dragView).remove();
        }

        endDrag();
    }

    protected void endDrag() {
        if (!mDragging) return;
        mDragging = false;
        mIsInPreDrag = false;
        mDragDriver = null;

        for (DragListener listener : new ArrayList<>(mListeners)) {
            listener.onDragEnd();
        }
        exitDrag();
    }

    protected void exitDrag() {}

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        return mDragDriver != null && mDragDriver.onTouchEvent(ev);
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (!mDragging) return false;
        return mDragDriver != null && mDragDriver.onInterceptTouchEvent(ev);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE && event.getAction() == KeyEvent.ACTION_UP) {
            cancelDrag();
            return true;
        }
        return false;
    }
}
