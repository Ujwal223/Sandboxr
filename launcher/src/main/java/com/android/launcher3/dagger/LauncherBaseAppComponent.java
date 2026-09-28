package com.android.launcher3.dagger;

import com.sandboxr.launcher.dagger.LauncherAppComponent;
import com.sandboxr.launcher.model.ItemInstallQueue;

/** Bridge stub for LauncherBaseAppComponent. */
public interface LauncherBaseAppComponent {
    static ItemInstallQueue getItemInstallQueue(LauncherAppComponent appComponent) {
        return null;
    }
}
