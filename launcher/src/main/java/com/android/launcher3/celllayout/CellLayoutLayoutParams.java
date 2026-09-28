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

package com.android.launcher3.celllayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.celllayout.CellLayoutLayoutParams].
 */
public class CellLayoutLayoutParams extends com.sandboxr.launcher.celllayout.CellLayoutLayoutParams {

    public CellLayoutLayoutParams(Context c, AttributeSet attrs) {
        super(c, attrs);
    }

    public CellLayoutLayoutParams(ViewGroup.LayoutParams source) {
        super(source);
    }

    public CellLayoutLayoutParams(com.sandboxr.launcher.celllayout.CellLayoutLayoutParams source) {
        super(source);
    }

    public CellLayoutLayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
        super(cellX, cellY, cellHSpan, cellVSpan);
    }
}
