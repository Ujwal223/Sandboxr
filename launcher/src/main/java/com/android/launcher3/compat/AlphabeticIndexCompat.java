package com.android.launcher3.compat;

import android.content.Context;
import android.os.LocaleList;

/** Bridge stub for AlphabeticIndexCompat. */
public class AlphabeticIndexCompat {
    public AlphabeticIndexCompat(Context context) {}
    public AlphabeticIndexCompat(LocaleList localeList) {}
    public String computeSectionName(CharSequence label) {
        return label != null && label.length() > 0 ? label.subSequence(0, 1).toString().toUpperCase() : "";
    }
}
