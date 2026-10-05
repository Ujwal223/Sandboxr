/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.sandboxr.launcher.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import com.sandboxr.launcher.ButtonDropTarget;
import com.sandboxr.launcher.DropTarget;
import com.sandboxr.launcher.Launcher;
import com.sandboxr.launcher.LauncherState;
import com.sandboxr.launcher.dragndrop.DragLayer;
import com.sandboxr.launcher.dragndrop.DragOptions;
import com.sandboxr.launcher.dragndrop.DragView;

/**
 * High-velocity fling-to-delete animation calculation and animator runner.
 */
public class FlingAnimation implements ValueAnimator.AnimatorUpdateListener, Runnable {

    private static final float MAX_ACCELERATION = 0.5f;
    private static final int DRAG_END_DELAY = 300;

    private final ButtonDropTarget mDropTarget;
    private final Launcher mLauncher;

    protected final DropTarget.DragObject mDragObject;
    protected final DragOptions mDragOptions;
    protected final DragLayer mDragLayer;
    protected final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.75f);
    protected final float mUX, mUY;

    protected Rect mIconRect;
    protected RectF mFrom;
    protected int mDuration;
    protected float mAnimationTimeFraction;

    protected float mAX, mAY;

    public FlingAnimation(DropTarget.DragObject d, PointF vel, ButtonDropTarget dropTarget, Launcher launcher,
            DragOptions options) {
        mDropTarget = dropTarget;
        mLauncher = launcher;
        mDragObject = d;
        mUX = vel.x / 1000f;
        mUY = vel.y / 1000f;
        mDragLayer = mLauncher.getDragLayer();
        mDragOptions = options;
    }

    @Override
    public void run() {
        if (mDropTarget != null) {
            mIconRect = mDropTarget.getIconRect(mDragObject);
        } else {
            mIconRect = new Rect(0, 0, 100, 100);
        }

        DragView dv = (DragView) mDragObject.dragView;
        if (dv != null) {
            dv.cancelAnimation();
            dv.requestLayout();
        }

        Rect from = new Rect();
        if (mDragLayer != null && dv != null) {
            mDragLayer.getViewRectRelativeToSelf(dv, from);
        }

        mFrom = new RectF(from);
        if (dv != null) {
            mFrom.inset(
                    ((1 - dv.getScaleX()) * from.width()) / 2f,
                    ((1 - dv.getScaleY()) * from.height()) / 2f);
        }
        mDuration = Math.abs(mUY) > Math.abs(mUX) ? initFlingUpDuration() : initFlingLeftDuration();
        mAnimationTimeFraction = ((float) mDuration) / (mDuration + DRAG_END_DELAY);

        final int duration = mDuration + DRAG_END_DELAY;
        final long startTime = AnimationUtils.currentAnimationTimeMillis();

        final TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0f;

            @Override
            public float getInterpolation(float t) {
                if (mCount < 0) {
                    mCount++;
                } else if (mCount == 0) {
                    mOffset = Math.min(0.5f, (float) (AnimationUtils.currentAnimationTimeMillis() - startTime) / duration);
                    mCount++;
                }
                return Math.min(1f, mOffset + t);
            }
        };

        if (mDropTarget != null) {
            mDropTarget.onDrop(mDragObject, mDragOptions);
        }
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(duration);
        anim.setInterpolator(tInterpolator);
        anim.addUpdateListener(this);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLauncher.getStateManager().goToState(LauncherState.NORMAL);
                if (mDropTarget != null) {
                    mDropTarget.completeDrop(mDragObject);
                }
            }
        });

        if (mDragLayer != null && dv != null) {
            mDragLayer.playDropAnimation(dv, anim, DragLayer.ANIMATION_END_DISAPPEAR);
        }
    }

    protected int initFlingUpDuration() {
        float sY = -mFrom.bottom;
        float d = mUY * mUY + 2 * sY * MAX_ACCELERATION;
        if (d >= 0) {
            mAY = MAX_ACCELERATION;
        } else {
            d = 0;
            mAY = mUY * mUY / (2 * -sY);
        }
        double t = (-mUY - Math.sqrt(d)) / mAY;
        float sX = -mFrom.centerX() + (mIconRect != null ? mIconRect.exactCenterX() : 0);
        mAX = (float) ((sX - t * mUX) * 2 / (t * t));
        return (int) Math.round(t);
    }

    protected int initFlingLeftDuration() {
        float sX = -mFrom.right;
        float d = mUX * mUX + 2 * sX * MAX_ACCELERATION;
        if (d >= 0) {
            mAX = MAX_ACCELERATION;
        } else {
            d = 0;
            mAX = mUX * mUX / (2 * -sX);
        }
        double t = (-mUX - Math.sqrt(d)) / mAX;
        float sY = -mFrom.centerY() + (mIconRect != null ? mIconRect.exactCenterY() : 0);
        mAY = (float) ((sY - t * mUY) * 2 / (t * t));
        return (int) Math.round(t);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float t = animation.getAnimatedFraction();
        if (t > mAnimationTimeFraction) {
            t = 1;
        } else {
            t = t / mAnimationTimeFraction;
        }
        if (mDragLayer != null) {
            final DragView dragView = (DragView) mDragLayer.getAnimatedView();
            if (dragView != null) {
                final float time = t * mDuration;
                dragView.setTranslationX(time * mUX + mFrom.left + mAX * time * time / 2);
                dragView.setTranslationY(time * mUY + mFrom.top + mAY * time * time / 2);
                dragView.setAlpha(1f - mAlphaInterpolator.getInterpolation(t));
            }
        }
    }
}
