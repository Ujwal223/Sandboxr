/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.icons;

import android.graphics.Path;

public class IconShape {
    private final Path mPath;

    public IconShape() {
        this(new Path());
    }

    public IconShape(Path path) {
        this.mPath = path != null ? path : new Path();
    }

    public Path getPath() {
        return mPath;
    }
}
