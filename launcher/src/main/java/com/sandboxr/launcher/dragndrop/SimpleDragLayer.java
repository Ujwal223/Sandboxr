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

package com.sandboxr.launcher.dragndrop;

import android.content.Context;
import android.util.AttributeSet;
import com.sandboxr.launcher.views.ActivityContext;
import com.sandboxr.launcher.views.BaseDragLayer;

/**
 * Lightweight BaseDragLayer for secondary dialogs and popup windows.
 */
public class SimpleDragLayer<T extends ActivityContext> extends BaseDragLayer<T> {

    public SimpleDragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void recreateControllers() {
        super.recreateControllers();
    }
}
