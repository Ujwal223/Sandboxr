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

import android.util.FloatProperty;
import java.util.function.Consumer;

/**
 * AOSP compatibility bridge for [com.sandboxr.launcher.anim.AnimatedFloat].
 */
public class AnimatedFloat extends com.sandboxr.launcher.anim.AnimatedFloat {
    public static final FloatProperty<AnimatedFloat> VALUE = new FloatProperty<AnimatedFloat>("value") {
        @Override
        public void setValue(AnimatedFloat obj, float v) {
            obj.updateValue(v);
        }

        @Override
        public Float get(AnimatedFloat obj) {
            return obj.value;
        }
    };

    public AnimatedFloat() {
        super(t -> {}, 0f);
    }

    public AnimatedFloat(Runnable updateCallback) {
        super(updateCallback);
    }

    public AnimatedFloat(Consumer<Float> updateCallback) {
        super(updateCallback, 0f);
    }

    public AnimatedFloat(Runnable updateCallback, float initialValue) {
        super(v -> updateCallback.run(), initialValue);
    }

    public AnimatedFloat(Consumer<Float> updateCallback, float initialValue) {
        super(updateCallback, initialValue);
    }
}
