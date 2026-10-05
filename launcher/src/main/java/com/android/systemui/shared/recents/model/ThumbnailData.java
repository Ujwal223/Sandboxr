/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.systemui.shared.recents.model;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import androidx.annotation.Nullable;

/**
 * Data class representing captured task thumbnail snapshot.
 */
public class ThumbnailData {

    @Nullable
    public Bitmap thumbnail;
    public int orientation = 0;
    public int rotation = 0;
    public Rect insets = new Rect();
    public boolean reducedResolution = false;
    public boolean isRealSnapshot = true;
    public boolean isTranslucent = false;
    public int windowingMode = 0;
    public int appearance = 0;
    public float scale = 1.0f;
    public long snapshotId = 0L;

    public ThumbnailData() {
    }

    public ThumbnailData(@Nullable Bitmap thumbnail) {
        this.thumbnail = thumbnail;
        if (thumbnail != null) {
            this.snapshotId = thumbnail.hashCode();
        }
    }

    public ThumbnailData(@Nullable Bitmap thumbnail, int orientation, int rotation) {
        this.thumbnail = thumbnail;
        this.orientation = orientation;
        this.rotation = rotation;
        if (thumbnail != null) {
            this.snapshotId = thumbnail.hashCode();
        }
    }

    public static ThumbnailData createFromBitmap(Bitmap bitmap) {
        return new ThumbnailData(bitmap);
    }
}
