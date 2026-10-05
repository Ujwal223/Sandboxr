/*
 * Copyright (C) 2026 Sandboxr Platform
 */

package com.android.launcher3.util;

import android.app.ActivityOptions;

public class ActivityOptionsWrapper extends com.sandboxr.launcher.util.ActivityOptionsWrapper {
    public ActivityOptionsWrapper(ActivityOptions options, com.sandboxr.launcher.util.RunnableList onEndCallback) {
        super(options, onEndCallback);
    }
}
