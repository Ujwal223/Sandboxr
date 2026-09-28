/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import com.sandboxr.launcher.LauncherSettings;
import com.sandboxr.launcher.icons.BitmapInfo;
import com.sandboxr.launcher.icons.GraphicsUtils;
import com.sandboxr.launcher.pm.UserCache;

/**
 * A wrapper around {@link ContentValues} with helper methods for serialization.
 */
public class ContentWriter {

    private final ContentValues mValues;
    private final Context mContext;

    private CommitParams mCommitParams;
    private BitmapInfo mIcon;
    private UserHandle mUser;

    public ContentWriter(Context context, CommitParams commitParams) {
        this(context);
        mCommitParams = commitParams;
    }

    public ContentWriter(Context context) {
        this(new ContentValues(), context);
    }

    public ContentWriter(ContentValues values, Context context) {
        mValues = values;
        mContext = context;
    }

    public ContentWriter put(String key, Integer value) {
        mValues.put(key, value);
        return this;
    }

    public ContentWriter put(String key, Long value) {
        mValues.put(key, value);
        return this;
    }

    public ContentWriter put(String key, String value) {
        mValues.put(key, value);
        return this;
    }

    public ContentWriter put(String key, CharSequence value) {
        mValues.put(key, value == null ? null : value.toString());
        return this;
    }

    public ContentWriter put(String key, Intent value) {
        mValues.put(key, value == null ? null : value.toUri(0));
        return this;
    }

    public ContentWriter putIcon(BitmapInfo value, UserHandle user) {
        mIcon = value;
        mUser = user;
        return this;
    }

    public ContentWriter put(String key, UserHandle user) {
        return put(key, UserCache.getInstance(mContext).getSerialNumberForUser(user));
    }

    public ContentValues getValues(Context context) {
        if (mIcon != null && !mIcon.isLowRes()) {
            mValues.put(
                    LauncherSettings.Favorites.ICON,
                    GraphicsUtils.createDefaultFlatBitmap(mIcon)
            );
            mIcon = null;
        }
        return mValues;
    }

    public int commit() {
        if (mCommitParams != null && mCommitParams.updateRunner != null) {
            return mCommitParams.updateRunner.update(getValues(mContext), mCommitParams.mWhere, mCommitParams.mSelectionArgs);
        }
        return 0;
    }

    @FunctionalInterface
    public interface UpdateRunner {
        int update(ContentValues values, String where, String[] selectionArgs);
    }

    public static class CommitParams {
        final UpdateRunner updateRunner;
        final String mWhere;
        final String[] mSelectionArgs;

        public CommitParams(UpdateRunner runner, String where, String[] selectionArgs) {
            updateRunner = runner;
            mWhere = where;
            mSelectionArgs = selectionArgs;
        }
    }
}
