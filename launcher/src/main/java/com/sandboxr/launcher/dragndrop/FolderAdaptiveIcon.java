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

package com.sandboxr.launcher.dragndrop;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.Nullable;

/**
 * Adaptive icon drawable rendering preview layers and badges for dragged folder icons.
 */
@TargetApi(Build.VERSION_CODES.O)
public class FolderAdaptiveIcon extends AdaptiveIconDrawable {

    private final Drawable mBadge;
    private final Path mMask;

    public FolderAdaptiveIcon(Drawable bg, Drawable fg, @Nullable Drawable badge, @Nullable Path mask) {
        super(bg != null ? bg : new ColorDrawable(Color.DKGRAY), fg != null ? fg : new ColorDrawable(Color.WHITE));
        mBadge = badge != null ? badge : new ColorDrawable(Color.TRANSPARENT);
        mMask = mask != null ? mask : new Path();
    }

    @Override
    public Path getIconMask() {
        return mMask;
    }

    public Drawable getBadge() {
        return mBadge;
    }

    public static FolderAdaptiveIcon createSimpleFolderIcon(int sizePx) {
        Path mask = new Path();
        mask.addRoundRect(0f, 0f, sizePx, sizePx, sizePx * 0.28f, sizePx * 0.28f, Path.Direction.CCW);

        Drawable bg = new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            { paint.setColor(Color.parseColor("#E622222E")); }

            @Override
            public void draw(Canvas canvas) {
                canvas.drawRoundRect(getBounds().left, getBounds().top, getBounds().right, getBounds().bottom,
                        getBounds().width() * 0.28f, getBounds().height() * 0.28f, paint);
            }
            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
            @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };

        Drawable fg = new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            { paint.setColor(Color.parseColor("#B3FFFFFF")); }

            @Override
            public void draw(Canvas canvas) {
                float w = getBounds().width();
                float h = getBounds().height();
                float dotSize = w * 0.16f;
                // Draw 4 mini grid dots
                canvas.drawCircle(w * 0.35f, h * 0.35f, dotSize / 2f, paint);
                canvas.drawCircle(w * 0.65f, h * 0.35f, dotSize / 2f, paint);
                canvas.drawCircle(w * 0.35f, h * 0.65f, dotSize / 2f, paint);
                canvas.drawCircle(w * 0.65f, h * 0.65f, dotSize / 2f, paint);
            }
            @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
            @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };

        return new FolderAdaptiveIcon(bg, fg, null, mask);
    }
}
