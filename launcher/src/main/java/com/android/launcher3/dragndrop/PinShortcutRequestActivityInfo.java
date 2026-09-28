package com.android.launcher3.dragndrop;

import android.content.Context;
import android.content.pm.LauncherApps.PinItemRequest;
import com.android.launcher3.pm.ShortcutConfigActivityInfo;

public class PinShortcutRequestActivityInfo extends ShortcutConfigActivityInfo {
    public PinShortcutRequestActivityInfo(PinItemRequest request, Context context) {
    }

    @Override
    public String getLabel() {
        return "";
    }
}
