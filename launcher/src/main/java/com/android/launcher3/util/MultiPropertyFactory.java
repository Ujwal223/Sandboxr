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

package com.android.launcher3.util;

import android.util.FloatProperty;

public class MultiPropertyFactory<T> extends com.sandboxr.launcher.util.MultiPropertyFactory<T> {
    public MultiPropertyFactory(T target, FloatProperty<T> property, int size, FloatBiFunction aggregator) {
        super(target, property, size, aggregator, 0f);
    }

    public MultiPropertyFactory(T target, FloatProperty<T> property, int size, FloatBiFunction aggregator, float defaultPropertyValue) {
        super(target, property, size, aggregator, defaultPropertyValue);
    }
}
