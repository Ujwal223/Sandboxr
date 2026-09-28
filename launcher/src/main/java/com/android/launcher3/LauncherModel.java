/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.android.launcher3;

import com.sandboxr.launcher.model.BgDataModel;
import com.sandboxr.launcher.model.AllAppsList;
import com.sandboxr.launcher.model.ModelTaskController;

/**
 * AOSP bridge for LauncherModel providing nested ModelUpdateTask interface for Java code.
 * Kotlin code should use com.sandboxr.launcher.LauncherModel directly.
 */
public final class LauncherModel {
    private LauncherModel() {}

    /**
     * Bridge nested interface that maps to com.sandboxr.launcher.LauncherModel.ModelUpdateTask.
     * Java-based tasks implement this interface.
     */
    public interface ModelUpdateTask extends com.sandboxr.launcher.LauncherModel.ModelUpdateTask {
        // extends the functional interface from sandboxr.launcher
    }
}
