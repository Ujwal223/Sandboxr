/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.launcher3.widget;

import android.content.Context;
import androidx.annotation.Nullable;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;

/**
 * Inflates and validates widgets during model loading pass.
 */
public class WidgetInflater {

    public static final int TYPE_DELETE = 0;
    public static final int TYPE_PENDING = 1;
    public static final int TYPE_REAL = 2;

    public static class InflationResult {
        public final int type;
        public final boolean isUpdate;
        @Nullable public final LauncherAppWidgetProviderInfo widgetInfo;
        @Nullable public final String reason;
        @Nullable public final String restoreErrorType;

        public InflationResult(int type, boolean isUpdate,
                @Nullable LauncherAppWidgetProviderInfo widgetInfo,
                @Nullable String reason, @Nullable String restoreErrorType) {
            this.type = type;
            this.isUpdate = isUpdate;
            this.widgetInfo = widgetInfo;
            this.reason = reason;
            this.restoreErrorType = restoreErrorType;
        }

        public InflationResult(int type, boolean isUpdate) {
            this(type, isUpdate, null, null, null);
        }

        public InflationResult(int type) {
            this(type, false, null, null, null);
        }
    }

    protected final Context mContext;
    protected final boolean mIsSafeMode;

    public WidgetInflater(Context context, boolean isSafeMode) {
        mContext = context;
        mIsSafeMode = isSafeMode;
    }

    public InflationResult inflateAppWidget(LauncherAppWidgetInfo info) {
        return new InflationResult(TYPE_REAL, false);
    }
}
