/*
 * Copyright (C) 2026 Sandboxr Platform
 */

package com.sandboxr.launcher.icons;

import android.graphics.Bitmap;

@FunctionalInterface
public interface BitmapRenderer extends com.android.launcher3.icons.BitmapRenderer {
    static Bitmap createHardwareBitmap(int width, int height, com.android.launcher3.icons.BitmapRenderer renderer) {
        return com.android.launcher3.icons.BitmapRenderer.createHardwareBitmap(width, height, renderer);
    }
}
