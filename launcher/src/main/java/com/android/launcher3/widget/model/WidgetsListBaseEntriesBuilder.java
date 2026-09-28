/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.widget.model;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WidgetsListBaseEntriesBuilder {
    private final Context mContext;

    public WidgetsListBaseEntriesBuilder(Context context) {
        mContext = context;
    }

    public List<WidgetsListBaseEntry> build(Map<?, ?> widgetsMap) {
        return new ArrayList<>();
    }

    public List<WidgetsListBaseEntry> build(List<?> widgetsList) {
        return new ArrayList<>();
    }
}
