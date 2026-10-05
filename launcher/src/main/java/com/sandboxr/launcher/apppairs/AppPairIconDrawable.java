/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.sandboxr.launcher.apppairs;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;

import com.android.launcher3.Utilities;

/**
 * Custom composed Drawable for rendering dual-app App Pair icons.
 * Features Liquid Glass semi-translucent rounded backdrops,
 * orientation-aware left/right or top/bottom layout division,
 * and scaled member app icons.
 */
public class AppPairIconDrawable extends Drawable {

    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AppPairIconDrawingParams mP;
    private final Drawable mIcon1;
    private final Drawable mIcon2;

    private static final RectF EMPTY_RECT = new RectF();
    private static final float[] ARRAY_OF_ZEROES = new float[8];

    public AppPairIconDrawable(AppPairIconDrawingParams p, Drawable icon1, Drawable icon2) {
        mP = p;
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(p.getBgColor());

        // Subtle specular highlight border for Liquid Glass feel
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(1.5f);
        mBorderPaint.setColor(0x33FFFFFF);

        mIcon1 = icon1;
        mIcon2 = icon2;
    }

    public AppPairIconDrawingParams getDrawingParams() {
        return mP;
    }

    public Drawable getFirstIcon() {
        return mIcon1;
    }

    public Drawable getSecondIcon() {
        return mIcon2;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mP.isLeftRightSplit()) {
            drawLeftRightSplit(canvas);
        } else {
            drawTopBottomSplit(canvas);
        }

        canvas.save();
        canvas.translate(
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                mP.getStandardIconPadding() + mP.getOuterPadding()
        );

        // Draw first icon
        canvas.save();
        if (mP.isLeftRightSplit()) {
            canvas.translate(
                    mP.getInnerPadding(),
                    mP.getBackgroundSize() / 2f - mP.getMemberIconSize() / 2f
            );
        } else {
            canvas.translate(
                    mP.getBackgroundSize() / 2f - mP.getMemberIconSize() / 2f,
                    mP.getInnerPadding()
            );
        }
        if (mIcon1 != null) {
            mIcon1.draw(canvas);
        }
        canvas.restore();

        // Draw second icon
        canvas.save();
        if (mP.isLeftRightSplit()) {
            canvas.translate(
                    mP.getBackgroundSize() - (mP.getInnerPadding() + mP.getMemberIconSize()),
                    mP.getBackgroundSize() / 2f - mP.getMemberIconSize() / 2f
            );
        } else {
            canvas.translate(
                    mP.getBackgroundSize() / 2f - mP.getMemberIconSize() / 2f,
                    mP.getBackgroundSize() - (mP.getInnerPadding() + mP.getMemberIconSize())
            );
        }
        if (mIcon2 != null) {
            mIcon2.draw(canvas);
        }
        canvas.restore();

        canvas.restore();
    }

    private void drawLeftRightSplit(Canvas canvas) {
        int width = mP.getIconSize();
        int height = mP.getIconSize();

        RectF leftSide = new RectF(
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                (width / 2f) - (mP.getCenterChannelSize() / 2f),
                height - (mP.getStandardIconPadding() + mP.getOuterPadding())
        );

        RectF rightSide = new RectF(
                (width / 2f) + (mP.getCenterChannelSize() / 2f),
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                width - (mP.getStandardIconPadding() + mP.getOuterPadding()),
                height - (mP.getStandardIconPadding() + mP.getOuterPadding())
        );

        Utilities.scaleRectFAboutPivot(
                leftSide,
                leftSide.left + leftSide.width(),
                leftSide.top + leftSide.centerY(),
                mP.getHoverScale()
        );
        Utilities.scaleRectFAboutPivot(
                rightSide,
                rightSide.left,
                rightSide.top + rightSide.centerY(),
                mP.getHoverScale()
        );

        float[] leftRadii = new float[]{
                mP.getBigRadius(), mP.getBigRadius(),
                mP.getSmallRadius(), mP.getSmallRadius(),
                mP.getSmallRadius(), mP.getSmallRadius(),
                mP.getBigRadius(), mP.getBigRadius()
        };

        float[] rightRadii = new float[]{
                mP.getSmallRadius(), mP.getSmallRadius(),
                mP.getBigRadius(), mP.getBigRadius(),
                mP.getBigRadius(), mP.getBigRadius(),
                mP.getSmallRadius(), mP.getSmallRadius()
        };

        drawCustomRoundedRect(canvas, leftSide, leftRadii);
        drawCustomRoundedRect(canvas, rightSide, rightRadii);
    }

    private void drawTopBottomSplit(Canvas canvas) {
        int width = mP.getIconSize();
        int height = mP.getIconSize();

        RectF topSide = new RectF(
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                width - (mP.getStandardIconPadding() + mP.getOuterPadding()),
                (height / 2f) - (mP.getCenterChannelSize() / 2f)
        );

        RectF bottomSide = new RectF(
                mP.getStandardIconPadding() + mP.getOuterPadding(),
                (height / 2f) + (mP.getCenterChannelSize() / 2f),
                width - (mP.getStandardIconPadding() + mP.getOuterPadding()),
                height - (mP.getStandardIconPadding() + mP.getOuterPadding())
        );

        Utilities.scaleRectFAboutPivot(
                topSide,
                topSide.left + topSide.centerX(),
                topSide.top + topSide.height(),
                mP.getHoverScale()
        );
        Utilities.scaleRectFAboutPivot(
                bottomSide,
                bottomSide.left + bottomSide.centerX(),
                bottomSide.top,
                mP.getHoverScale()
        );

        float[] topRadii = new float[]{
                mP.getBigRadius(), mP.getBigRadius(),
                mP.getBigRadius(), mP.getBigRadius(),
                mP.getSmallRadius(), mP.getSmallRadius(),
                mP.getSmallRadius(), mP.getSmallRadius()
        };

        float[] bottomRadii = new float[]{
                mP.getSmallRadius(), mP.getSmallRadius(),
                mP.getSmallRadius(), mP.getSmallRadius(),
                mP.getBigRadius(), mP.getBigRadius(),
                mP.getBigRadius(), mP.getBigRadius()
        };

        drawCustomRoundedRect(canvas, topSide, topRadii);
        drawCustomRoundedRect(canvas, bottomSide, bottomRadii);
    }

    private void drawCustomRoundedRect(Canvas c, RectF rect, float[] radii) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            c.drawDoubleRoundRect(rect, radii, EMPTY_RECT, ARRAY_OF_ZEROES, mBackgroundPaint);
            c.drawDoubleRoundRect(rect, radii, EMPTY_RECT, ARRAY_OF_ZEROES, mBorderPaint);
        } else {
            c.drawRoundRect(rect, mP.getBigRadius(), mP.getBigRadius(), mBackgroundPaint);
            c.drawRoundRect(rect, mP.getBigRadius(), mP.getBigRadius(), mBorderPaint);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        mBackgroundPaint.setAlpha(alpha);
        mBorderPaint.setAlpha((int) (alpha * 0.2f));
        if (mIcon1 != null) mIcon1.setAlpha(alpha);
        if (mIcon2 != null) mIcon2.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mBackgroundPaint.setColorFilter(colorFilter);
        if (mIcon1 != null) mIcon1.setColorFilter(colorFilter);
        if (mIcon2 != null) mIcon2.setColorFilter(colorFilter);
    }

    @Override
    public int getIntrinsicWidth() {
        return mP.getIconSize();
    }

    @Override
    public int getIntrinsicHeight() {
        return mP.getIconSize();
    }
}
