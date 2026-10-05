/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 */
package com.android.launcher3.widget.model;

import android.content.Context;
import com.android.launcher3.model.WidgetItem;
import com.sandboxr.launcher.model.data.PackageItemInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds a list of widget entries suitable for the widget picker list.
 */
public class WidgetsListBaseEntriesBuilder {
    private final Context mContext;

    public WidgetsListBaseEntriesBuilder(Context context) {
        mContext = context;
    }

    @SuppressWarnings("unchecked")
    public List<WidgetsListBaseEntry> build(Map<?, ?> widgetsMap) {
        List<WidgetsListBaseEntry> entries = new ArrayList<>();
        if (widgetsMap == null || widgetsMap.isEmpty()) {
            return entries;
        }

        List<Map.Entry<PackageItemInfo, List<WidgetItem>>> sortedEntries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : widgetsMap.entrySet()) {
            if (entry.getKey() instanceof PackageItemInfo && entry.getValue() instanceof List) {
                sortedEntries.add((Map.Entry<PackageItemInfo, List<WidgetItem>>) (Map.Entry<?, ?>) entry);
            }
        }

        sortedEntries.sort((a, b) -> {
            String titleA = a.getKey().title != null ? a.getKey().title.toString() : "";
            String titleB = b.getKey().title != null ? b.getKey().title.toString() : "";
            return titleA.compareToIgnoreCase(titleB);
        });

        for (Map.Entry<PackageItemInfo, List<WidgetItem>> entry : sortedEntries) {
            PackageItemInfo pkgItem = entry.getKey();
            List<WidgetItem> items = new ArrayList<>(entry.getValue());
            items.sort((a, b) -> {
                int areaA = a.spanX * a.spanY;
                int areaB = b.spanX * b.spanY;
                if (areaA != areaB) return Integer.compare(areaA, areaB);
                String lA = a.label != null ? a.label : "";
                String lB = b.label != null ? b.label : "";
                return lA.compareToIgnoreCase(lB);
            });

            String sectionName = pkgItem.title != null && pkgItem.title.length() > 0
                    ? pkgItem.title.subSequence(0, 1).toString().toUpperCase()
                    : "";
            entries.add(new WidgetsListHeaderEntry(pkgItem, sectionName, items, false));
        }

        return entries;
    }

    public List<WidgetsListBaseEntry> build(List<?> widgetsList) {
        List<WidgetsListBaseEntry> entries = new ArrayList<>();
        if (widgetsList != null) {
            for (Object obj : widgetsList) {
                if (obj instanceof WidgetsListBaseEntry) {
                    entries.add((WidgetsListBaseEntry) obj);
                }
            }
        }
        return entries;
    }
}
