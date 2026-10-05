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

package com.sandboxr.launcher.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.SparseIntArray;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts and coordinates localized wallpaper and background colors
 * behind placed widgets for adaptive dynamic theming.
 */
public class LocalColorExtractor {

    public interface OnColorsChangedListener {
        void onColorsChanged(RectF rect, SparseIntArray colors);
    }

    private final Context mContext;
    private final List<OnColorsChangedListener> mListeners = new ArrayList<>();

    public LocalColorExtractor(Context context) {
        mContext = context.getApplicationContext();
    }

    public static LocalColorExtractor newInstance(Context context) {
        return new LocalColorExtractor(context);
    }

    public void addListener(OnColorsChangedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(OnColorsChangedListener listener) {
        mListeners.remove(listener);
    }

    public SparseIntArray generateColorsForBounds(RectF bounds) {
        SparseIntArray colors = new SparseIntArray();
        // Liquid Glass adaptive default palette: primary accent, secondary surface, dark base
        colors.put(0, Color.parseColor("#FF64D2FF")); // Accent cyan
        colors.put(1, Color.parseColor("#FF12121A")); // Surface dark
        colors.put(2, Color.parseColor("#FFFFFFFF")); // Text highlight
        return colors;
    }
}
