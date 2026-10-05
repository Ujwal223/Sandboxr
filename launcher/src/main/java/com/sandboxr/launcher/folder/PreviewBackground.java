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

package com.sandboxr.launcher.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Manages the background circle/squircle container of a FolderIcon in both resting
 * and drag-hovered ("accepting") states. Built with Liquid Glass translucent styling.
 */
public class PreviewBackground {

    public static final int CONSUMPTION_ANIMATION_DURATION = 100;
    public static final int ACCEPT_ANIMATION_DURATION = 150;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mClipPath = new Path();
    private final RectF mBounds = new RectF();

    public int previewSize;
    public int basePreviewOffsetX;
    public int basePreviewOffsetY;

    private float mScale = 1.0f;
    private float mTargetScale = 1.0f;
    private boolean mIsAccepting = false;
    private ValueAnimator mScaleAnimator;
    private View mInvalidateDelegate;

    private int mNormalFillColor = 0x24FFFFFF;
    private int mNormalStrokeColor = 0x33FFFFFF;
    private int mAcceptFillColor = 0x4064D2FF;
    private int mAcceptStrokeColor = 0x9964D2FF;

    public PreviewBackground() {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mNormalFillColor);

        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(2f);
        mStrokePaint.setColor(mNormalStrokeColor);
    }

    public void setup(Context context, View invalidateDelegate, int size) {
        mInvalidateDelegate = invalidateDelegate;
        previewSize = size;
        basePreviewOffsetX = 0;
        basePreviewOffsetY = 0;
    }

    public int getRadius() {
        return (int) (previewSize * mScale / 2f);
    }

    public float getScale() {
        return mScale;
    }

    public boolean isAccepting() {
        return mIsAccepting;
    }

    public void animateToAccept() {
        if (mIsAccepting) return;
        mIsAccepting = true;
        animateScale(1.15f, mAcceptFillColor, mAcceptStrokeColor);
    }

    public void animateToRest() {
        if (!mIsAccepting) return;
        mIsAccepting = false;
        animateScale(1.0f, mNormalFillColor, mNormalStrokeColor);
    }

    private void animateScale(float targetScale, int targetFill, int targetStroke) {
        if (mScaleAnimator != null) {
            mScaleAnimator.cancel();
        }
        final float startScale = mScale;
        final int startFill = mPaint.getColor();
        final int startStroke = mStrokePaint.getColor();

        mScaleAnimator = ValueAnimator.ofFloat(0f, 1f);
        mScaleAnimator.setDuration(ACCEPT_ANIMATION_DURATION);
        mScaleAnimator.setInterpolator(new DecelerateInterpolator());
        mScaleAnimator.addUpdateListener(anim -> {
            float progress = (float) anim.getAnimatedValue();
            mScale = startScale + progress * (targetScale - startScale);
            mPaint.setColor(interpolateColor(startFill, targetFill, progress));
            mStrokePaint.setColor(interpolateColor(startStroke, targetStroke, progress));
            if (mInvalidateDelegate != null) {
                mInvalidateDelegate.invalidate();
            }
        });
        mScaleAnimator.start();
    }

    public void drawBackground(Canvas canvas) {
        float centerX = basePreviewOffsetX + previewSize / 2f;
        float centerY = basePreviewOffsetY + previewSize / 2f;
        float radius = (previewSize * mScale) / 2f;

        mBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        // Liquid Glass superellipse rounded rect
        float cornerRadius = radius * 0.55f;
        canvas.drawRoundRect(mBounds, cornerRadius, cornerRadius, mPaint);
    }

    public void drawBackgroundStroke(Canvas canvas) {
        float centerX = basePreviewOffsetX + previewSize / 2f;
        float centerY = basePreviewOffsetY + previewSize / 2f;
        float radius = (previewSize * mScale) / 2f;

        mBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        float cornerRadius = radius * 0.55f;
        canvas.drawRoundRect(mBounds, cornerRadius, cornerRadius, mStrokePaint);
    }

    public void getBounds(Rect outRect) {
        float centerX = basePreviewOffsetX + previewSize / 2f;
        float centerY = basePreviewOffsetY + previewSize / 2f;
        float radius = (previewSize * mScale) / 2f;
        outRect.set(
            (int) (centerX - radius),
            (int) (centerY - radius),
            (int) (centerX + radius),
            (int) (centerY + radius)
        );
    }

    private int interpolateColor(int colorStart, int colorEnd, float fraction) {
        int startA = Color.alpha(colorStart);
        int startR = Color.red(colorStart);
        int startG = Color.green(colorStart);
        int startB = Color.blue(colorStart);

        int endA = Color.alpha(colorEnd);
        int endR = Color.red(colorEnd);
        int endG = Color.green(colorEnd);
        int endB = Color.blue(colorEnd);

        return Color.argb(
            (int) (startA + fraction * (endA - startA)),
            (int) (startR + fraction * (endR - startR)),
            (int) (startG + fraction * (endG - startG)),
            (int) (startB + fraction * (endB - startB))
        );
    }
}
