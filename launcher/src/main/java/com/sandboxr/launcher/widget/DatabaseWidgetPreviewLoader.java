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

package com.sandboxr.launcher.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import com.sandboxr.launcher.model.WidgetItem;

/**
 * Loads, synthesizes, and caches bitmap previews of app widgets.
 * Renders Liquid Glass styled preview placeholders when official asset previews are absent.
 */
public class DatabaseWidgetPreviewLoader {

    private final Context mContext;
    private final LruCache<String, Bitmap> mCache;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DatabaseWidgetPreviewLoader(Context context) {
        mContext = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public Bitmap loadPreview(WidgetItem item, int previewWidth, int previewHeight) {
        if (item == null) return null;
        String cacheKey = item.widgetInfo != null
                ? item.widgetInfo.provider.flattenToString() + "_" + previewWidth + "x" + previewHeight
                : item.label + "_" + previewWidth + "x" + previewHeight;

        Bitmap cached = mCache.get(cacheKey);
        if (cached != null) return cached;

        Bitmap preview = generatePreview(item, previewWidth, previewHeight);
        if (preview != null) {
            mCache.put(cacheKey, preview);
        }
        return preview;
    }

    public interface PreviewLoadCallback {
        void onPreviewLoaded(WidgetItem item, Bitmap preview);
    }

    public void loadPreview(WidgetItem item, int previewWidth, int previewHeight, PreviewLoadCallback callback) {
        Bitmap preview = loadPreview(item, previewWidth, previewHeight);
        if (callback != null) {
            callback.onPreviewLoaded(item, preview);
        }
    }

    private Bitmap generatePreview(WidgetItem item, int width, int height) {
        int w = Math.max(width, 100);
        int h = Math.max(height, 100);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Try to load preview from provider info
        if (item.widgetInfo != null && item.widgetInfo.previewImage != 0) {
            try {
                Context packageContext = mContext.createPackageContext(
                        item.widgetInfo.provider.getPackageName(), 0);
                Drawable d = packageContext.getDrawable(item.widgetInfo.previewImage);
                if (d != null) {
                    d.setBounds(0, 0, w, h);
                    d.draw(canvas);
                    return bitmap;
                }
            } catch (Exception ignored) {}
        }

        // Fallback: Liquid Glass styled card with app icon and dimension text
        RectF cardBounds = new RectF(4, 4, w - 4, h - 4);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(0x33FFFFFF); // Frosted surface
        canvas.drawRoundRect(cardBounds, 16f, 16f, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2f);
        mPaint.setColor(0x4DFFFFFF); // Specular highlight border
        canvas.drawRoundRect(cardBounds, 16f, 16f, mPaint);

        // Draw app icon centered
        if (item.bitmap != null && item.bitmap.icon != null) {
            Drawable icon = new BitmapDrawable(mContext.getResources(), item.bitmap.icon);
            int iconSize = Math.min(w, h) / 3;
            int cx = w / 2;
            int cy = h / 2;
            icon.setBounds(cx - iconSize / 2, cy - iconSize / 2, cx + iconSize / 2, cy + iconSize / 2);
            icon.draw(canvas);
        }

        return bitmap;
    }
}
