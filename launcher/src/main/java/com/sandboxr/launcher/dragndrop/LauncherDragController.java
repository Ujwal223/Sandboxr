/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.LauncherState;
import com.sandboxr.launcher.model.data.ItemInfo;

import java.util.function.Consumer;

/**
 * Drag controller tailored for [Launcher], managing haptic feedback,
 * fling-to-delete velocity tracking, and default Workspace drop routing.
 */
public class LauncherDragController extends DragController {

    public static final String TAG = "LauncherDragController";

    private final FlingToDeleteHelper mFlingToDeleteHelper;
    private final Launcher mLauncher;

    public LauncherDragController(Launcher launcher) {
        super(launcher);
        mLauncher = launcher;
        mFlingToDeleteHelper = new FlingToDeleteHelper(launcher);
    }

    public FlingToDeleteHelper getFlingToDeleteHelper() {
        return mFlingToDeleteHelper;
    }

    @Override
    protected Consumer<MotionEvent> getSecondaryEventConsumer() {
        return mFlingToDeleteHelper::recordMotionEvent;
    }

    @Override
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
        int registrationX = (dragRegion != null && dragRegion.width() > 0)
                ? dragRegion.width() / 2 : 60;
        int registrationY = (dragRegion != null && dragRegion.height() > 0)
                ? dragRegion.height() / 2 : 60;

        if (drawable != null) {
            return new LauncherDragView(
                    mLauncher,
                    drawable,
                    registrationX,
                    registrationY,
                    initialDragViewScale,
                    dragViewScaleOnDrop,
                    0f,
                    allowSpringDrawable
            );
        } else if (view != null) {
            return new LauncherDragView(
                    mLauncher,
                    view,
                    view.getMeasuredWidth() > 0 ? view.getMeasuredWidth() : 120,
                    view.getMeasuredHeight() > 0 ? view.getMeasuredHeight() : 120,
                    registrationX,
                    registrationY,
                    initialDragViewScale,
                    dragViewScaleOnDrop,
                    0f,
                    allowSpringDrawable
            );
        } else {
            return new LauncherDragView(
                    mLauncher,
                    new android.graphics.drawable.ColorDrawable(0xFF4E80EE),
                    registrationX,
                    registrationY,
                    initialDragViewScale,
                    dragViewScaleOnDrop,
                    0f,
                    allowSpringDrawable
            );
        }
    }

    @Override
    protected void onDragViewInitialized() {
        if (mLauncher.getDragLayer() != null) {
            mLauncher.getDragLayer().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    @Override
    protected void exitDrag() {
        if (!mIsInPreDrag && !mLauncher.isInState(LauncherState.EDIT_MODE)) {
            mLauncher.getStateManager().goToState(LauncherState.NORMAL);
        }
    }

    @Override
    protected boolean endWithFlingAnimation() {
        Runnable flingAnimation = mFlingToDeleteHelper.getFlingAnimation(mDragObject, mOptions);
        if (flingAnimation != null) {
            drop(mFlingToDeleteHelper.getDropTarget(), flingAnimation);
            return true;
        }
        return super.endWithFlingAnimation();
    }

    @Override
    protected void endDrag() {
        super.endDrag();
        mFlingToDeleteHelper.releaseVelocityTracker();
    }

    @Override
    protected DropTarget getDefaultDropTarget(int[] dropCoordinates) {
        if (mLauncher.getWorkspace() != null && mLauncher.getDragLayer() != null) {
            mLauncher.getDragLayer().mapCoordInSelfToDescendant(mLauncher.getWorkspace(), dropCoordinates);
            return mLauncher.getWorkspace();
        }
        return null;
    }
}
