package com.android.launcher3.util;

import android.content.Context;

/** Bridge stub for InstantAppResolver. */
public class InstantAppResolver {
    public static InstantAppResolver newInstance(Context context) {
        return new InstantAppResolver();
    }
    public boolean isInstantApp(String packageName) { return false; }
    public boolean isInstantApp(android.content.pm.ApplicationInfo info) { return false; }
}
