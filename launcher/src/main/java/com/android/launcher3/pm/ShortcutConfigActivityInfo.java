package com.android.launcher3.pm;

import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;
import androidx.annotation.Nullable;
import com.sandboxr.launcher.util.PackageUserKey;
import com.sandboxr.launcher.icons.cache.CachedObject;
import java.util.Collections;
import java.util.List;

/** Bridge stub for ShortcutConfigActivityInfo. */
public abstract class ShortcutConfigActivityInfo implements CachedObject {
    public abstract String getLabel();
    public ComponentName getComponent() { return new ComponentName("", ""); }
    @Override
    public ComponentName getComponentName() { return getComponent(); }
    @Override
    public UserHandle getUser() { return Process.myUserHandle(); }
    public boolean isPersistable() { return false; }

    public static List<ShortcutConfigActivityInfo> queryList(Context context, @Nullable PackageUserKey packageUserKey) {
        return Collections.emptyList();
    }

    public static List<ShortcutConfigActivityInfo> queryList(Context context, @Nullable com.android.launcher3.util.PackageUserKey packageUserKey) {
        return Collections.emptyList();
    }
}
