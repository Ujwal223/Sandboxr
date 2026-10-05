/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */

package com.android.launcher3.icons;

import android.graphics.Bitmap;
import android.graphics.Canvas;

@FunctionalInterface
public interface BitmapRenderer {
    void draw(Canvas out);

    static Bitmap createHardwareBitmap(int width, int height, BitmapRenderer renderer) {
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, width), Math.max(1, height), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        renderer.draw(canvas);
        return bitmap;
    }
}
