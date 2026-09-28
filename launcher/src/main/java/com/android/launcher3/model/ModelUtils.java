package com.android.launcher3.model;

import com.sandboxr.launcher.model.data.ItemInfo;
import com.sandboxr.launcher.util.IntSet;
import java.util.function.Predicate;

/** Bridge stub for ModelUtils. */
public class ModelUtils {
    public static final Predicate<ItemInfo> WIDGET_FILTER =
            com.sandboxr.launcher.model.ModelUtils.WIDGET_FILTER;

    public static Predicate<ItemInfo> currentScreenContentFilter(IntSet currentScreenIds) {
        return com.sandboxr.launcher.model.ModelUtils.currentScreenContentFilter(currentScreenIds);
    }

    public static Predicate<ItemInfo> currentScreenContentFilter() {
        return item -> true;
    }
}
