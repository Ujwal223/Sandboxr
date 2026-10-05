/*
 * Copyright (C) 2021 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.widget.model;

import com.android.launcher3.model.WidgetItem;
import com.sandboxr.launcher.model.data.PackageItemInfo;

import java.util.List;

/**
 * An entry representing an app's header in the widget list.
 */
public class WidgetsListHeaderEntry extends WidgetsListBaseEntry {

    public final boolean isWidgetListShown;

    public WidgetsListHeaderEntry(PackageItemInfo pkgItem, String titleSectionName, List<WidgetItem> items) {
        this(pkgItem, titleSectionName, items, false);
    }

    public WidgetsListHeaderEntry(PackageItemInfo pkgItem, String titleSectionName, List<WidgetItem> items, boolean isWidgetListShown) {
        super(pkgItem, titleSectionName, items);
        this.isWidgetListShown = isWidgetListShown;
    }

    public WidgetsListHeaderEntry withWidgetListShown(boolean shown) {
        if (shown == isWidgetListShown) return this;
        return new WidgetsListHeaderEntry(mPkgItem, mTitleSectionName, mWidgets, shown);
    }

    @Override
    public WidgetsListBaseEntry copy() {
        return new WidgetsListHeaderEntry(mPkgItem, mTitleSectionName, mWidgets, isWidgetListShown);
    }
}
