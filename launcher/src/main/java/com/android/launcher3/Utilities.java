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

    public static boolean isRtl(android.content.res.Resources res) {
        return res.getConfiguration().getLayoutDirection() == android.view.View.LAYOUT_DIRECTION_RTL;
    }

    public static final boolean IS_DEBUG_DEVICE = false;

    public static android.app.ActivityOptions allowBGLaunch(android.app.ActivityOptions options) {
        if (ATLEAST_U) {
            options.setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
        }
        return options;
    }

    public static android.graphics.Rect getViewBounds(android.view.View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new android.graphics.Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

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

    private static final android.graphics.Matrix sMatrix = new android.graphics.Matrix();
    private static final android.graphics.Matrix sInverseMatrix = new android.graphics.Matrix();

    public static void offsetPoints(float[] points, float offsetX, float offsetY) {
        for (int i = 0; i < points.length; i += 2) {
            points[i] += offsetX;
            points[i + 1] += offsetY;
        }
    }

    public static float getDescendantCoordRelativeToAncestor(
            android.view.View descendant, android.view.View ancestor, float[] coord, boolean includeRootScroll) {
        return getDescendantCoordRelativeToAncestor(descendant, ancestor, coord, includeRootScroll, false);
    }

    public static float getDescendantCoordRelativeToAncestor(
            android.view.View descendant, android.view.View ancestor, float[] coord,
            boolean includeRootScroll, boolean ignoreTransform) {
        float scale = 1.0f;
        android.view.View v = descendant;
        while (v != ancestor && v != null) {
            if (v != descendant || includeRootScroll) {
                offsetPoints(coord, -v.getScrollX(), -v.getScrollY());
            }

            if (!ignoreTransform) {
                v.getMatrix().mapPoints(coord);
            }
            offsetPoints(coord, v.getLeft(), v.getTop());
            scale *= v.getScaleX();

            v = v.getParent() instanceof android.view.View ? (android.view.View) v.getParent() : null;
        }
        return scale;
    }

    public static void mapCoordInSelfToDescendant(android.view.View descendant, android.view.View root, float[] coord) {
        synchronized (sMatrix) {
            sMatrix.reset();
            android.view.View v = descendant;
            while (v != root && v != null) {
                sMatrix.postTranslate(-v.getScrollX(), -v.getScrollY());
                sMatrix.postConcat(v.getMatrix());
                sMatrix.postTranslate(v.getLeft(), v.getTop());
                v = v.getParent() instanceof android.view.View ? (android.view.View) v.getParent() : null;
            }
            if (v != null) {
                sMatrix.postTranslate(-v.getScrollX(), -v.getScrollY());
            }
            sMatrix.invert(sInverseMatrix);
            sInverseMatrix.mapPoints(coord);
        }
    }

    public static void mapRectInSelfToDescendant(android.view.View descendant, android.view.View root, android.graphics.Rect rect) {
        float[] coords = new float[]{rect.left, rect.top, rect.right, rect.bottom};
        mapCoordInSelfToDescendant(descendant, root, coords);
        rect.set((int) coords[0], (int) coords[1], (int) coords[2], (int) coords[3]);
    }

    public static void roundArray(float[] in, int[] out) {
        for (int i = 0; i < in.length && i < out.length; i++) {
            out[i] = Math.round(in[i]);
        }
    }

    public static void scaleRectFAboutPivot(android.graphics.RectF rectF, float pivotX, float pivotY, float scale) {
        rectF.offset(-pivotX, -pivotY);
        rectF.left *= scale;
        rectF.top *= scale;
        rectF.right *= scale;
        rectF.bottom *= scale;
        rectF.offset(pivotX, pivotY);
    }

    public static float squaredHypot(float x, float y) {
        return x * x + y * y;
    }

    public static float squaredTouchSlop(android.content.Context context) {
        float slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        return slop * slop;
    }
}
