/*
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3;

import android.content.Context;
import com.sandboxr.launcher.ConstantItem;
import com.sandboxr.launcher.Item;

public class LauncherPrefs extends com.sandboxr.launcher.LauncherPrefs {

    public static final ConstantItem<String> WORKSPACE_SIZE = com.sandboxr.launcher.LauncherPrefs.WORKSPACE_SIZE;
    public static final ConstantItem<Integer> HOTSEAT_COUNT = com.sandboxr.launcher.LauncherPrefs.HOTSEAT_COUNT;
    public static final ConstantItem<Integer> DEVICE_TYPE = com.sandboxr.launcher.LauncherPrefs.DEVICE_TYPE;
    public static final ConstantItem<String> DB_FILE = com.sandboxr.launcher.LauncherPrefs.DB_FILE;
    public static final ConstantItem<Integer> GRID_TYPE = com.sandboxr.launcher.LauncherPrefs.GRID_TYPE;

    public LauncherPrefs(Context context) {
        super(context);
    }
}
