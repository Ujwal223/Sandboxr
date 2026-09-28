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

package com.sandboxr.launcher.util;

import static android.provider.Settings.System.ACCELEROMETER_ROTATION;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.sandboxr.launcher.dagger.ApplicationContext;
import com.sandboxr.launcher.dagger.LauncherAppSingleton;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import javax.inject.Inject;

@LauncherAppSingleton
public class SettingsCache extends ContentObserver {
    private static final String TAG = "SettingsCache";

    public static final Uri NOTIFICATION_BADGING_URI =
            Settings.Secure.getUriFor("notification_badging");
    public static final String ONE_HANDED_ENABLED = "one_handed_mode_enabled";
    public static final String ONE_HANDED_SWIPE_BOTTOM_TO_NOTIFICATION_ENABLED =
            "swipe_bottom_to_notification_enabled";
    public static final Uri PRIVATE_SPACE_HIDE_WHEN_LOCKED_URI =
            Settings.Secure.getUriFor("hide_privatespace_entry_point");
    public static final Uri ROTATION_SETTING_URI =
            Settings.System.getUriFor(ACCELEROMETER_ROTATION);
    public static final Uri TOUCHPAD_NATURAL_SCROLLING =
            Settings.System.getUriFor("touchpad_natural_scrolling");

    public interface OnChangeListener {
        void onSettingsChanged(boolean isEnabled);
    }

    private final ContentResolver mResolver;
    private final Map<Uri, Boolean> mKeyCache = new ConcurrentHashMap<>();
    private final Map<Uri, Set<OnChangeListener>> mListeners = new ConcurrentHashMap<>();

    private static volatile SettingsCache sInstance;

    public static SettingsCache getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SettingsCache.class) {
                if (sInstance == null) {
                    sInstance = new SettingsCache(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    @Inject
    public SettingsCache(@ApplicationContext Context context) {
        super(new Handler(Executors.MODEL_EXECUTOR.getLooper()));
        mResolver = context.getContentResolver();
    }

    public boolean getValue(Uri uri) {
        return getValue(uri, false);
    }

    public boolean getValue(Uri uri, boolean defaultValue) {
        Boolean val = mKeyCache.get(uri);
        if (val != null) {
            return val;
        }
        boolean readVal = querySetting(uri, defaultValue);
        mKeyCache.put(uri, readVal);
        return readVal;
    }

    public boolean getValue(String key, boolean defaultValue) {
        return getValue(Settings.Secure.getUriFor(key), defaultValue);
    }

    private boolean querySetting(Uri uri, boolean defaultValue) {
        try {
            String lastPath = uri.getLastPathSegment();
            if (lastPath == null) return defaultValue;
            int result = Settings.Secure.getInt(mResolver, lastPath, defaultValue ? 1 : 0);
            return result != 0;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void register(Uri uri, OnChangeListener listener) {
        mListeners.computeIfAbsent(uri, u -> {
            try {
                mResolver.registerContentObserver(uri, false, this);
            } catch (Exception e) {
                Log.e(TAG, "Error registering observer for " + uri, e);
            }
            return new CopyOnWriteArraySet<>();
        }).add(listener);
    }

    public void unregister(Uri uri, OnChangeListener listener) {
        Set<OnChangeListener> set = mListeners.get(uri);
        if (set != null) {
            set.remove(listener);
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri == null) return;
        boolean newVal = querySetting(uri, false);
        mKeyCache.put(uri, newVal);
        Set<OnChangeListener> set = mListeners.get(uri);
        if (set != null) {
            for (OnChangeListener listener : set) {
                Executors.MAIN_EXECUTOR.execute(() -> listener.onSettingsChanged(newVal));
            }
        }
    }

    public com.sandboxr.launcher.util.ListenableRef<Boolean> getListenableRef(android.net.Uri uri) {
        com.sandboxr.launcher.util.MutableListenableRef<Boolean> ref =
                new com.sandboxr.launcher.util.MutableListenableRef<>(getValue(uri, false));
        register(uri, enabled -> ref.setValue(enabled));
        return ref.asListenable();
    }
}
