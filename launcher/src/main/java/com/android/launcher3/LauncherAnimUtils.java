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

import android.animation.Animator;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.FloatProperty;
import android.util.IntProperty;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.function.Function;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.LauncherAnimUtils].
 */
public class LauncherAnimUtils {
    public static final int SPRING_LOADED_EXIT_DELAY = com.sandboxr.launcher.LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY;
    public static final float SUCCESS_TRANSITION_PROGRESS = com.sandboxr.launcher.LauncherAnimUtils.SUCCESS_TRANSITION_PROGRESS;
    public static final float TABLET_BOTTOM_SHEET_SUCCESS_TRANSITION_PROGRESS = com.sandboxr.launcher.LauncherAnimUtils.TABLET_BOTTOM_SHEET_SUCCESS_TRANSITION_PROGRESS;

    public static final int SCALE_INDEX_UNFOLD_ANIMATION = com.sandboxr.launcher.LauncherAnimUtils.SCALE_INDEX_UNFOLD_ANIMATION;
    public static final int SCALE_INDEX_WORKSPACE_STATE = com.sandboxr.launcher.LauncherAnimUtils.SCALE_INDEX_WORKSPACE_STATE;
    public static final int SCALE_INDEX_REVEAL_ANIM = com.sandboxr.launcher.LauncherAnimUtils.SCALE_INDEX_REVEAL_ANIM;
    public static final int SCALE_INDEX_WIDGET_TRANSITION = com.sandboxr.launcher.LauncherAnimUtils.SCALE_INDEX_WIDGET_TRANSITION;
    public static final int SCALE_INDEX_FOLDER_ANIM = com.sandboxr.launcher.LauncherAnimUtils.SCALE_INDEX_FOLDER_ANIM;

    public static final IntProperty<Drawable> DRAWABLE_ALPHA = com.sandboxr.launcher.LauncherAnimUtils.DRAWABLE_ALPHA;
    public static final FloatProperty<View> SCALE_PROPERTY = com.sandboxr.launcher.LauncherAnimUtils.SCALE_PROPERTY;
    public static final IntProperty<LayoutParams> LAYOUT_WIDTH = com.sandboxr.launcher.LauncherAnimUtils.LAYOUT_WIDTH;
    public static final IntProperty<LayoutParams> LAYOUT_HEIGHT = com.sandboxr.launcher.LauncherAnimUtils.LAYOUT_HEIGHT;
    public static final IntProperty<TextView> TEXT_COLOR = com.sandboxr.launcher.LauncherAnimUtils.TEXT_COLOR;
    public static final IntProperty<TextView> HINT_TEXT_COLOR = com.sandboxr.launcher.LauncherAnimUtils.HINT_TEXT_COLOR;

    public static final FloatProperty<View> VIEW_ALPHA = com.sandboxr.launcher.LauncherAnimUtils.VIEW_ALPHA;
    public static final FloatProperty<View> VIEW_TRANSLATE_X = com.sandboxr.launcher.LauncherAnimUtils.VIEW_TRANSLATE_X;
    public static final FloatProperty<View> VIEW_TRANSLATE_Y = com.sandboxr.launcher.LauncherAnimUtils.VIEW_TRANSLATE_Y;
    public static final FloatProperty<View> VIEW_TRANSLATE_Z = com.sandboxr.launcher.LauncherAnimUtils.VIEW_TRANSLATE_Z;
    public static final IntProperty<View> VIEW_BACKGROUND_COLOR = com.sandboxr.launcher.LauncherAnimUtils.VIEW_BACKGROUND_COLOR;
    public static final FloatProperty<ImageView> ROTATION_DRAWABLE_PERCENT = com.sandboxr.launcher.LauncherAnimUtils.ROTATION_DRAWABLE_PERCENT;

    public static FloatProperty<View> getScaleProperty() {
        return com.sandboxr.launcher.LauncherAnimUtils.getScaleProperty();
    }

    public static int blockedFlingDurationFactor(float velocity) {
        return com.sandboxr.launcher.LauncherAnimUtils.blockedFlingDurationFactor(velocity);
    }

    public static Animator.AnimatorListener newSingleUseCancelListener(Runnable callback) {
        return com.sandboxr.launcher.LauncherAnimUtils.newSingleUseCancelListener(callback);
    }

    public static Animator.AnimatorListener newCancelListener(Runnable callback, boolean isSingleUse) {
        return com.sandboxr.launcher.LauncherAnimUtils.newCancelListener(callback, isSingleUse);
    }

    public static Function<RectF, Float> getPosProviderForRect(RectF start, RectF target) {
        kotlin.jvm.functions.Function1<RectF, Float> kotlinFunc = com.sandboxr.launcher.LauncherAnimUtils.getPosProviderForRect(start, target);
        return kotlinFunc::invoke;
    }

    public static class ClampedProperty<T> extends com.sandboxr.launcher.LauncherAnimUtils.ClampedProperty<T> {
        public ClampedProperty(FloatProperty<T> property, float minValue, float maxValue) {
            super(property, minValue, maxValue);
        }
    }
}
