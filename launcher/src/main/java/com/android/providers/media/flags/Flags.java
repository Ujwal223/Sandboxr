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

package com.android.providers.media.flags;

public final class Flags {
    public static final String FLAG_ENABLE_TRASH_AND_RESTORE_BY_FILE_PATH_API =
            "com.android.providers.media.flags.enable_trash_and_restore_by_file_path_api";

    private Flags() {}

    public static boolean enableTrashAndRestoreByFilePathApi() {
        return false;
    }
}
