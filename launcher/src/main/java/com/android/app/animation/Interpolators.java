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

package com.android.app.animation;

import android.animation.TimeInterpolator;
import android.view.animation.Interpolator;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.anim.Interpolators].
 */
public class Interpolators {
    public static final Interpolator LINEAR = com.sandboxr.launcher.anim.Interpolators.LINEAR;
    public static final Interpolator ACCELERATE = com.sandboxr.launcher.anim.Interpolators.ACCELERATE;
    public static final Interpolator ACCELERATE_2 = com.sandboxr.launcher.anim.Interpolators.ACCELERATE_2;
    public static final Interpolator DECELERATE = com.sandboxr.launcher.anim.Interpolators.DECELERATE;
    public static final Interpolator DECELERATE_1_5 = com.sandboxr.launcher.anim.Interpolators.DECELERATE_1_5;
    public static final Interpolator DECELERATE_1_7 = com.sandboxr.launcher.anim.Interpolators.DECELERATE_1_7;
    public static final Interpolator DECELERATE_2 = com.sandboxr.launcher.anim.Interpolators.DECELERATE_2;
    public static final Interpolator ACCELERATE_DECELERATE = com.sandboxr.launcher.anim.Interpolators.ACCELERATE_DECELERATE;
    public static final Interpolator FAST_OUT_SLOW_IN = com.sandboxr.launcher.anim.Interpolators.FAST_OUT_SLOW_IN;
    public static final Interpolator SLOW_IN_FAST_OUT = com.sandboxr.launcher.anim.Interpolators.SLOW_IN_FAST_OUT;
    public static final Interpolator AGGRESSIVE_EASE = com.sandboxr.launcher.anim.Interpolators.AGGRESSIVE_EASE;
    public static final Interpolator EXAGGERATED_EASE = com.sandboxr.launcher.anim.Interpolators.EXAGGERATED_EASE;
    public static final Interpolator EMPHASIZED = com.sandboxr.launcher.anim.Interpolators.EMPHASIZED;
    public static final Interpolator EMPHASIZED_DECELERATE = com.sandboxr.launcher.anim.Interpolators.EMPHASIZED_DECELERATE;
    public static final Interpolator EMPHASIZED_ACCELERATE = com.sandboxr.launcher.anim.Interpolators.EMPHASIZED_ACCELERATE;
    public static final Interpolator ZOOM_OUT = com.sandboxr.launcher.anim.Interpolators.ZOOM_OUT;
    public static final Interpolator OVERSHOOT_1_2 = com.sandboxr.launcher.anim.Interpolators.OVERSHOOT_1_2;
    public static final Interpolator FINAL_FRAME = com.sandboxr.launcher.anim.Interpolators.FINAL_FRAME;

    public static TimeInterpolator clampToProgress(TimeInterpolator interpolator, float lowerBound, float upperBound) {
        return com.sandboxr.launcher.anim.Interpolators.clampToProgress(interpolator, lowerBound, upperBound);
    }

    public static Interpolator scrollInterpolatorForVelocity(float velocityPxPerMs) {
        return com.sandboxr.launcher.anim.Interpolators.scrollInterpolatorForVelocity(velocityPxPerMs);
    }
}
