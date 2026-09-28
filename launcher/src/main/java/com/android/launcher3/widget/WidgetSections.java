package com.android.launcher3.widget;

import android.content.Context;
import java.util.Collections;
import java.util.Map;

/** Bridge stub for WidgetSections. */
public class WidgetSections {
    public static final int NO_CATEGORY = -1;
    
    public static Map<Integer, WidgetSection> getWidgetSections(Context context) {
        return Collections.emptyMap();
    }

    public static Map<android.content.ComponentName, com.sandboxr.launcher.util.IntSet> getWidgetsToCategory(Context context) {
        return Collections.emptyMap();
    }
    
    public static class WidgetSection {
        public int category;
        public CharSequence title;
    }
}
