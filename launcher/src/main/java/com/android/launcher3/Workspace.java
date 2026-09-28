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
import android.view.View;
import com.sandboxr.launcher.pageindicators.PageIndicator;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.Workspace].
 */
public class Workspace<T extends View & PageIndicator> extends com.sandboxr.launcher.Workspace<T> {

    public static final int FIRST_SCREEN_ID = com.sandboxr.launcher.Workspace.FIRST_SCREEN_ID;
    public static final int EXTRA_EMPTY_SCREEN_ID = com.sandboxr.launcher.Workspace.EXTRA_EMPTY_SCREEN_ID;
    public static final int EXTRA_EMPTY_SCREEN_SECOND_ID = com.sandboxr.launcher.Workspace.EXTRA_EMPTY_SCREEN_SECOND_ID;
    public static final com.sandboxr.launcher.util.IntSet EXTRA_EMPTY_SCREEN_IDS =
            com.sandboxr.launcher.Workspace.EXTRA_EMPTY_SCREEN_IDS;

    public static final FloatProperty<com.sandboxr.launcher.Workspace<?>> WORKSPACE_SCALE_PROPERTY =
            com.sandboxr.launcher.Workspace.WORKSPACE_SCALE_PROPERTY;

    public Workspace(Context context) {
        super(context);
    }

    public Workspace(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
