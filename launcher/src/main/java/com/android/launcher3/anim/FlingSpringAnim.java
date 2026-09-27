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

package com.android.launcher3.anim;

import android.content.Context;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.anim.FlingSpringAnim].
 */
public class FlingSpringAnim<K> extends com.sandboxr.launcher.anim.FlingSpringAnim<K> {
    public FlingSpringAnim(
            K object,
            Context context,
            FloatPropertyCompat<K> property,
            float startPosition,
            float targetPosition,
            float startVelocityPxPerS,
            float minVisChange,
            float minValue,
            float maxValue,
            float damping,
            float stiffness,
            DynamicAnimation.OnAnimationEndListener onEndListener
    ) {
        super(
                object,
                context,
                property,
                startPosition,
                targetPosition,
                startVelocityPxPerS,
                minVisChange,
                minValue,
                maxValue,
                damping,
                stiffness,
                onEndListener
        );
    }
}
