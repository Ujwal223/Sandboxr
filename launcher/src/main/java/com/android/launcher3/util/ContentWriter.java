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

package com.android.launcher3.util;

import android.content.ContentValues;
import android.content.Context;

public class ContentWriter extends com.sandboxr.launcher.util.ContentWriter {

    public ContentWriter(Context context) {
        super(context);
    }

    public ContentWriter(ContentValues values, Context context) {
        super(values, context);
    }

    public ContentWriter(Context context, com.sandboxr.launcher.util.ContentWriter.CommitParams commitParams) {
        super(context, commitParams);
    }

    public static class CommitParams extends com.sandboxr.launcher.util.ContentWriter.CommitParams {
        public CommitParams(com.sandboxr.launcher.util.ContentWriter.UpdateRunner runner, String where, String[] selectionArgs) {
            super(runner, where, selectionArgs);
        }
    }
}
