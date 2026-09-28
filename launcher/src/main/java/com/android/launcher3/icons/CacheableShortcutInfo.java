package com.android.launcher3.icons;

import android.content.pm.LauncherActivityInfo;
import android.content.pm.ShortcutInfo;
import java.util.Collections;
import java.util.List;

/** Bridge stub for CacheableShortcutInfo. */
public class CacheableShortcutInfo {
    public static List<CacheableShortcutInfo> convertShortcutsToCacheableShortcuts(List<ShortcutInfo> shortcuts) {
        return Collections.emptyList();
    }

    public static List<CacheableShortcutInfo> convertShortcutsToCacheableShortcuts(
            List<ShortcutInfo> shortcuts, List<LauncherActivityInfo> activities) {
        return Collections.emptyList();
    }
}
