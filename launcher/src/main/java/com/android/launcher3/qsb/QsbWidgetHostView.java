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

package com.android.launcher3.qsb;

import android.content.Context;
import android.util.AttributeSet;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.qsb.QsbWidgetHostView].
 */
public class QsbWidgetHostView extends com.sandboxr.launcher.qsb.QsbWidgetHostView {

    public QsbWidgetHostView(Context context) {
        super(context);
    }

    public QsbWidgetHostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QsbWidgetHostView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
