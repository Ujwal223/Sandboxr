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

package com.sandboxr.launcher.shortcuts;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.sandboxr.launcher.graphics.DragPreviewProvider;
import com.sandboxr.launcher.icons.BitmapRenderer;
import com.sandboxr.launcher.icons.FastBitmapDrawable;
import com.sandboxr.launcher.views.ActivityContext;

/**
 * Extension of {@link DragPreviewProvider} which generates bitmaps scaled to default icon size.
 */
public class ShortcutDragPreviewProvider extends DragPreviewProvider {

    private final Point mPositionShift;

    public ShortcutDragPreviewProvider(View icon, Point shift) {
        super(icon);
        mPositionShift = shift;
    }

    @Override
    public Drawable createDrawable() {
        int size = ActivityContext.lookupContext(mView.getContext())
                .getDeviceProfile().getWorkspaceProfile().getIconSizePx();
        return new FastBitmapDrawable(
                BitmapRenderer.createHardwareBitmap(
                        size + blurSizeOutline,
                        size + blurSizeOutline,
                        (c) -> drawDragViewOnBackground(c, size)));
    }

    private void drawDragViewOnBackground(Canvas canvas, float size) {
        Drawable d = mView.getBackground();
        Rect bounds = getDrawableBounds(d);

        canvas.translate(blurSizeOutline / 2f, blurSizeOutline / 2f);
        if (bounds.width() > 0 && bounds.height() > 0) {
            canvas.scale(size / bounds.width(), size / bounds.height(), 0, 0);
        }
        canvas.translate(bounds.left, bounds.top);
        if (d != null) {
            d.draw(canvas);
        }
    }
}
