package com.android.launcher3.widget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sandboxr.launcher.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.icons.cache.CachedObject;
import com.android.launcher3.icons.cache.IconLoadRequest;
import com.sandboxr.launcher.model.data.LauncherAppWidgetInfo;

/**
 * Thin wrapper around AppWidgetProviderInfo for Launcher.
 */
public class LauncherAppWidgetProviderInfo extends com.sandboxr.launcher.widget.LauncherAppWidgetProviderInfo implements CachedObject {

    public static final String CUSTOM_WIDGET_PACKAGE = "custom-widget";

    private boolean mIsMinSizeFulfilled;

    public static LauncherAppWidgetProviderInfo fromProviderInfo(
            Context context, AppWidgetProviderInfo info) {
        final LauncherAppWidgetProviderInfo launcherInfo;
        if (info instanceof LauncherAppWidgetProviderInfo) {
            launcherInfo = (LauncherAppWidgetProviderInfo) info;
        } else {
            Parcel p = Parcel.obtain();
            info.writeToParcel(p, 0);
            p.setDataPosition(0);
            launcherInfo = new LauncherAppWidgetProviderInfo(p);
            p.recycle();
        }
        launcherInfo.initSpans(context, LauncherAppState.getIDP(context));
        return launcherInfo;
    }

    public LauncherAppWidgetProviderInfo() {}

    public LauncherAppWidgetProviderInfo(Parcel in) {
        super(in);
    }

    public void initSpans(Context context, InvariantDeviceProfile idp) {
        mPM = context.getApplicationContext().getPackageManager();
        int minSpanX = Math.max(1, minResizeWidth > 0 ? (int) Math.ceil((float) minResizeWidth / 70f) : 1);
        int minSpanY = Math.max(1, minResizeHeight > 0 ? (int) Math.ceil((float) minResizeHeight / 70f) : 1);
        int maxSpanX = idp.numColumns;
        int maxSpanY = idp.numRows;
        int spanX = Math.max(1, minWidth > 0 ? (int) Math.ceil((float) minWidth / 70f) : 1);
        int spanY = Math.max(1, minHeight > 0 ? (int) Math.ceil((float) minHeight / 70f) : 1);

        if (maxResizeWidth > 0) {
            maxSpanX = Math.min(maxSpanX, Math.max(1, (int) Math.ceil((float) maxResizeWidth / 70f)));
        }
        if (maxResizeHeight > 0) {
            maxSpanY = Math.min(maxSpanY, Math.max(1, (int) Math.ceil((float) maxResizeHeight / 70f)));
        }

        maxSpanX = Math.max(maxSpanX, minSpanX);
        maxSpanY = Math.max(maxSpanY, minSpanY);

        if (targetCellWidth >= minSpanX && targetCellWidth <= maxSpanX
                && targetCellHeight >= minSpanY && targetCellHeight <= maxSpanY) {
            spanX = targetCellWidth;
            spanY = targetCellHeight;
        }

        this.minSpanX = Math.min(spanX, minSpanX);
        this.minSpanY = Math.min(spanY, minSpanY);
        this.maxSpanX = maxSpanX;
        this.maxSpanY = maxSpanY;
        this.mIsMinSizeFulfilled = Math.min(spanX, minSpanX) <= idp.numColumns
                && Math.min(spanY, minSpanY) <= idp.numRows;
        this.spanX = Math.min(spanX, idp.numColumns);
        this.spanY = Math.min(spanY, idp.numRows);
    }

    public boolean isMinSizeFulfilled() {
        return mIsMinSizeFulfilled;
    }

    public CharSequence getLabel() {
        return super.loadLabel(mPM);
    }

    public Point getMinSpans() {
        return new Point((resizeMode & RESIZE_HORIZONTAL) != 0 ? minSpanX : -1,
                (resizeMode & RESIZE_VERTICAL) != 0 ? minSpanY : -1);
    }

    public boolean isCustomWidget() {
        return provider != null && provider.getPackageName().equals(CUSTOM_WIDGET_PACKAGE);
    }

    public int getWidgetFeatures() {
        return widgetFeatures;
    }

    public boolean isReconfigurable() {
        return configure != null && (getWidgetFeatures() & WIDGET_FEATURE_RECONFIGURABLE) != 0;
    }

    public boolean isConfigurationOptional() {
        return isReconfigurable()
                && (getWidgetFeatures() & WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0;
    }

    @Override
    public ComponentName getComponentName() {
        return provider;
    }

    public final ComponentName getComponent() {
        return provider;
    }

    @Override
    public final UserHandle getUser() {
        try {
            UserHandle profile = getProfile();
            if (profile != null) return profile;
        } catch (Throwable ignored) {}
        return android.os.Process.myUserHandle();
    }

    public Drawable getFullResIcon(@NonNull IconLoadRequest<CachedObject> request) {
        return null;
    }

    @Nullable
    public ApplicationInfo getApplicationInfo() {
        return getActivityInfo() != null ? getActivityInfo().applicationInfo : null;
    }
}