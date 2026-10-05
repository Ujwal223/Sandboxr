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

package com.sandboxr.launcher.popup;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * A drawable used for the caret arrow on a rounded rectangle popup bubble.
 * Draws a triangle with one rounded tip, the opposite edge is clipped by the body of the popup
 * so there is no overlap when drawing them together.
 */
public class RoundedArrowDrawable extends Drawable {

    private final Path mPath;
    private final Paint mPaint;

    public RoundedArrowDrawable(float width, float height, float radius, float popupRadius,
            float popupWidth, float popupHeight,
            float arrowOffsetX, float arrowOffsetY, boolean isPointingUp, boolean leftAligned,
            int color) {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);

        addDownPointingRoundedTriangleToPath(width, height, radius, mPath);
        clipPopupBodyFromPath(popupRadius, popupWidth, popupHeight, arrowOffsetX, arrowOffsetY,
                mPath);

        Matrix pathTransform = new Matrix();
        pathTransform.setScale(
                leftAligned ? 1 : -1, isPointingUp ? -1 : 1, width * 0.5f, height * 0.5f);
        mPath.transform(pathTransform);
    }

    private RoundedArrowDrawable(float width, float height, float radius, boolean isHorizontal,
            boolean isLeftOrTop, int color) {
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);

        addDownPointingRoundedTriangleToPath(width, height, radius, mPath);

        if (isHorizontal || isLeftOrTop) {
            Matrix pathTransform = new Matrix();
            int rotationAngle;
            if (isHorizontal) {
                rotationAngle = isLeftOrTop ? 90 : -90;
            } else {
                rotationAngle = 180;
            }
            pathTransform.setRotate(rotationAngle, width * 0.5f, height * 0.5f);
            mPath.transform(pathTransform);
        }
    }

    public static RoundedArrowDrawable createHorizontalRoundedArrow(float width, float height,
            float radius, boolean isPointingLeft, int color) {
        return new RoundedArrowDrawable(width, height, radius, true, isPointingLeft, color);
    }

    public static RoundedArrowDrawable createVerticalRoundedArrow(float width, float height,
            float radius, boolean isPointingUp, int color) {
        return new RoundedArrowDrawable(width, height, radius, false, isPointingUp, color);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public void getOutline(Outline outline) {
        outline.setPath(mPath);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int i) {
        mPaint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    public void setShadowLayer(float shadowBlur, float dx, float dy, int shadowColor) {
        mPaint.setShadowLayer(shadowBlur, dx, dy, shadowColor);
    }

    public static void addDownPointingRoundedTriangleToPath(float width, float height,
            float radius, Path path) {
        float tanTheta = width / (2.0f * height);
        float theta = (float) atan(tanTheta);
        float roundedPointCenterY = (float) (height - (radius / sin(theta)));
        float p = radius / tanTheta;
        float lineRoundPointIntersectFromCenter = (float) (p * sin(theta));
        float lineRoundPointIntersectFromTop = (float) (height - (p * cos(theta)));

        float centerX = width / 2.0f;
        float thetaDeg = (float) toDegrees(theta);

        path.reset();
        path.moveTo(0, 0);
        path.lineTo(width, 0);
        path.lineTo(
                centerX + lineRoundPointIntersectFromCenter,
                lineRoundPointIntersectFromTop);
        path.arcTo(
                centerX - radius,
                roundedPointCenterY - radius,
                centerX + radius,
                roundedPointCenterY + radius,
                thetaDeg,
                180 - (2 * thetaDeg),
                false);
        path.lineTo(0, 0);
        path.close();
    }

    private static void clipPopupBodyFromPath(float popupRadius, float popupWidth,
            float popupHeight, float arrowOffsetX, float arrowOffsetY, Path path) {
        Path clipPiece = new Path();
        clipPiece.addRoundRect(
                0, 0, popupWidth, popupHeight,
                popupRadius, popupRadius, Path.Direction.CW);
        clipPiece.offset(-arrowOffsetX, -popupHeight + arrowOffsetY - 0.5f);
        path.op(clipPiece, Path.Op.DIFFERENCE);
    }
}
