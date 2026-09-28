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

package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatProperty;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.CellLayout].
 */
public class CellLayout extends com.sandboxr.launcher.CellLayout {

    public static final int WORKSPACE = com.sandboxr.launcher.CellLayout.WORKSPACE;
    public static final int HOTSEAT = com.sandboxr.launcher.CellLayout.HOTSEAT;
    public static final int FOLDER = com.sandboxr.launcher.CellLayout.FOLDER;
    public static final float DEFAULT_SCALE = com.sandboxr.launcher.CellLayout.DEFAULT_SCALE;
    public static final int REORDER_ANIMATION_DURATION = com.sandboxr.launcher.CellLayout.REORDER_ANIMATION_DURATION;
    public static final float REORDER_PREVIEW_MAGNITUDE = com.sandboxr.launcher.CellLayout.REORDER_PREVIEW_MAGNITUDE;
    public static final int MODE_SHOW_REORDER_HINT = com.sandboxr.launcher.CellLayout.MODE_SHOW_REORDER_HINT;
    public static final int MODE_DRAG_OVER = com.sandboxr.launcher.CellLayout.MODE_DRAG_OVER;
    public static final int MODE_ON_DROP = com.sandboxr.launcher.CellLayout.MODE_ON_DROP;
    public static final int MODE_ON_DROP_EXTERNAL = com.sandboxr.launcher.CellLayout.MODE_ON_DROP_EXTERNAL;
    public static final int MODE_ACCEPT_DROP = com.sandboxr.launcher.CellLayout.MODE_ACCEPT_DROP;

    public static final FloatProperty<com.sandboxr.launcher.CellLayout> SPRING_LOADED_PROGRESS =
            com.sandboxr.launcher.CellLayout.SPRING_LOADED_PROGRESS;

    public CellLayout(Context context) {
        super(context);
    }

    public CellLayout(Context context, CellLayoutContainer container) {
        super(context, container);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
