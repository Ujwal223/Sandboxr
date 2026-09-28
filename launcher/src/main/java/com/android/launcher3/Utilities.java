/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Build;
import androidx.annotation.ChecksSdkIntAtLeast;

public final class Utilities {

    private Utilities() {}

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU, codename = "T")
    public static final boolean ATLEAST_T = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codename = "U")
    public static final boolean ATLEAST_U = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM, codename = "V")
    public static final boolean ATLEAST_V = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM;

    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String TRIM_PATTERN = "(^\\h+|\\h+$)";

    public static String trim(CharSequence s) {
        if (s == null) {
            return "";
        }
        return s.toString().replaceAll(TRIM_PATTERN, "").trim();
    }

    public static String createDbSelectionQuery(String columnName, com.sandboxr.launcher.util.IntArray values) {
        return String.format(java.util.Locale.ENGLISH, "%s IN (%s)", columnName, values.toConcatString());
    }

    public static boolean isBinderSizeError(Exception e) {
        return e.getCause() instanceof android.os.TransactionTooLargeException
                || e.getCause() instanceof android.os.DeadObjectException;
    }

    public static boolean isBootCompleted() {
        return true;
    }
}
