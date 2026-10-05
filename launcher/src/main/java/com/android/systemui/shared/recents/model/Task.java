/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.systemui.shared.recents.model;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

/**
 * Task model representing an active or recent Android task.
 */
public class Task {

    public static class TaskKey {
        public final int id;
        public int windowingMode;
        @Nullable
        public final Intent baseIntent;
        @Nullable
        public final ComponentName sourceComponent;
        public final int userId;
        public long lastActiveTime;
        public int displayId;

        public TaskKey(int id, int windowingMode, @Nullable Intent baseIntent,
                       @Nullable ComponentName sourceComponent, int userId,
                       long lastActiveTime, int displayId) {
            this.id = id;
            this.windowingMode = windowingMode;
            this.baseIntent = baseIntent;
            this.sourceComponent = sourceComponent;
            this.userId = userId;
            this.lastActiveTime = lastActiveTime;
            this.displayId = displayId;
        }

        public TaskKey(int id, int windowingMode, @Nullable Intent baseIntent,
                       @Nullable ComponentName sourceComponent, int userId,
                       long lastActiveTime) {
            this(id, windowingMode, baseIntent, sourceComponent, userId, lastActiveTime, 0);
        }

        public TaskKey(int id) {
            this(id, 0, null, null, 0, 0, 0);
        }

        @Nullable
        public String getPackageName() {
            if (sourceComponent != null) {
                return sourceComponent.getPackageName();
            }
            if (baseIntent != null && baseIntent.getComponent() != null) {
                return baseIntent.getComponent().getPackageName();
            }
            if (baseIntent != null && baseIntent.getPackage() != null) {
                return baseIntent.getPackage();
            }
            return "";
        }

        @Nullable
        public ComponentName getComponent() {
            if (sourceComponent != null) {
                return sourceComponent;
            }
            if (baseIntent != null) {
                return baseIntent.getComponent();
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TaskKey)) return false;
            TaskKey taskKey = (TaskKey) o;
            return id == taskKey.id && userId == taskKey.userId && displayId == taskKey.displayId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, userId, displayId);
        }

        @NonNull
        @Override
        public String toString() {
            return "TaskKey(id=" + id + ", pkg=" + getPackageName() + ", user=" + userId + ")";
        }
    }

    @NonNull
    public TaskKey key;
    @Nullable
    public ThumbnailData thumbnail;
    @Nullable
    public Drawable icon;
    @Nullable
    public String title;
    @Nullable
    public String titleDescription;
    public int colorBackground;
    public int colorPrimary;
    public boolean isLocked;
    public boolean isRunning;

    public Task() {
        this.key = new TaskKey(0);
    }

    public Task(@NonNull TaskKey key) {
        this.key = key;
    }

    public Task(@NonNull TaskKey key, int colorBackground, int colorPrimary,
                boolean isLocked, boolean isRunning, @Nullable Drawable icon,
                @Nullable ThumbnailData thumbnail) {
        this.key = key;
        this.colorBackground = colorBackground;
        this.colorPrimary = colorPrimary;
        this.isLocked = isLocked;
        this.isRunning = isRunning;
        this.icon = icon;
        this.thumbnail = thumbnail;
    }

    public Task(int id, @NonNull String packageName, @Nullable String title,
                @Nullable Drawable icon, @Nullable ThumbnailData thumbnail) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, packageName + ".MainActivity"));
        this.key = new TaskKey(id, 0, intent, intent.getComponent(), 0, System.currentTimeMillis());
        this.title = title;
        this.titleDescription = title;
        this.icon = icon;
        this.thumbnail = thumbnail;
    }

    public Task copy() {
        Task clone = new Task(this.key, this.colorBackground, this.colorPrimary,
                this.isLocked, this.isRunning, this.icon, this.thumbnail);
        clone.title = this.title;
        clone.titleDescription = this.titleDescription;
        return clone;
    }

    @NonNull
    @Override
    public String toString() {
        return "Task(id=" + key.id + ", title=" + title + ", pkg=" + key.getPackageName() + ")";
    }
}
