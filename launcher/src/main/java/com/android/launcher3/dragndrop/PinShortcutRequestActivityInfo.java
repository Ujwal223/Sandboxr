package com.android.launcher3.dragndrop;

import android.content.Context;
import android.content.pm.LauncherApps.PinItemRequest;
import com.android.launcher3.pm.ShortcutConfigActivityInfo;

public class PinShortcutRequestActivityInfo extends ShortcutConfigActivityInfo {
    private final com.sandboxr.launcher.dragndrop.PinShortcutRequestActivityInfo mDelegate;

    public PinShortcutRequestActivityInfo(PinItemRequest request, Context context) {
        mDelegate = new com.sandboxr.launcher.dragndrop.PinShortcutRequestActivityInfo(request, context);
    }

    @Override
    public String getLabel() {
        CharSequence label = mDelegate.getLabel();
        return label != null ? label.toString() : "";
    }

    public com.sandboxr.launcher.dragndrop.PinShortcutRequestActivityInfo getDelegate() {
        return mDelegate;
    }
}
