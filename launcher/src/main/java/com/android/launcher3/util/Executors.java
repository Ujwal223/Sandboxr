/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.util;

import com.sandboxr.launcher.util.LooperExecutor;

public final class Executors {
    public static final LooperExecutor MAIN_EXECUTOR = com.sandboxr.launcher.util.Executors.MAIN_EXECUTOR;
    public static final LooperExecutor UI_HELPER_EXECUTOR = com.sandboxr.launcher.util.Executors.UI_HELPER_EXECUTOR;
    public static final LooperExecutor MODEL_EXECUTOR = com.sandboxr.launcher.util.Executors.MODEL_EXECUTOR;
    public static final java.util.concurrent.ThreadPoolExecutor THREAD_POOL_EXECUTOR =
            (java.util.concurrent.ThreadPoolExecutor) java.util.concurrent.Executors.newCachedThreadPool();
    private Executors() {}
}
