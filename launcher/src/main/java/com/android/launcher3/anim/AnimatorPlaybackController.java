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

import android.animation.AnimatorSet;
import java.util.ArrayList;

/**
 * AOSP compatibility bridge for {@link com.sandboxr.launcher.anim.AnimatorPlaybackController}.
 *
 * <p>This bridge re-exposes the static {@code wrap} factory, returning the Kotlin base type
 * directly. Java callers should use this class's {@code wrap} and cast to the base type as needed.
 */
public final class AnimatorPlaybackController {

    private AnimatorPlaybackController() {}

    /**
     * Creates an {@link com.sandboxr.launcher.anim.AnimatorPlaybackController} for the given
     * {@link AnimatorSet} and duration.
     */
    public static com.sandboxr.launcher.anim.AnimatorPlaybackController wrap(
            AnimatorSet anim, long duration) {
        return com.sandboxr.launcher.anim.AnimatorPlaybackController.wrap(anim, duration);
    }
}
