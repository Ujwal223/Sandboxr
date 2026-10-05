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

package com.sandboxr.launcher.folder;

import android.graphics.Point;
import com.sandboxr.launcher.DeviceProfile;

/**
 * Organizes item positions, columns, rows, and page partitioning for folder grids.
 */
public class FolderGridOrganizer {

    public static final int DEFAULT_MAX_COLS = 4;
    public static final int DEFAULT_MAX_ROWS = 4;

    private int mCountX = DEFAULT_MAX_COLS;
    private int mCountY = DEFAULT_MAX_ROWS;
    private int mMaxItemsPerPage = DEFAULT_MAX_COLS * DEFAULT_MAX_ROWS;

    public FolderGridOrganizer() {
        this(DEFAULT_MAX_COLS, DEFAULT_MAX_ROWS);
    }

    public FolderGridOrganizer(int maxCols, int maxRows) {
        mCountX = Math.max(1, maxCols);
        mCountY = Math.max(1, maxRows);
        mMaxItemsPerPage = mCountX * mCountY;
    }

    public void setProfile(DeviceProfile profile) {
        if (profile != null) {
            mCountX = Math.min(DEFAULT_MAX_COLS, profile.numFolderColumns > 0 ? profile.numFolderColumns : DEFAULT_MAX_COLS);
            mCountY = Math.min(DEFAULT_MAX_ROWS, profile.numFolderRows > 0 ? profile.numFolderRows : DEFAULT_MAX_ROWS);
            mMaxItemsPerPage = mCountX * mCountY;
        }
    }

    public void calculateGridSize(int itemCount) {
        if (itemCount <= 0) {
            mCountX = 1;
            mCountY = 1;
        } else if (itemCount <= 2) {
            mCountX = itemCount;
            mCountY = 1;
        } else if (itemCount <= 4) {
            mCountX = 2;
            mCountY = 2;
        } else if (itemCount <= 6) {
            mCountX = 3;
            mCountY = 2;
        } else if (itemCount <= 9) {
            mCountX = 3;
            mCountY = 3;
        } else {
            mCountX = Math.min(DEFAULT_MAX_COLS, 4);
            mCountY = Math.min(DEFAULT_MAX_ROWS, 4);
        }
        mMaxItemsPerPage = mCountX * mCountY;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    public int getMaxItemsPerPage() {
        return mMaxItemsPerPage;
    }

    public int getNumPages(int totalCount) {
        if (totalCount <= 0) return 1;
        return (int) Math.ceil((double) totalCount / mMaxItemsPerPage);
    }

    public int getPageForRank(int rank) {
        if (rank < 0) return 0;
        return rank / mMaxItemsPerPage;
    }

    public void getPosForRank(int rank, Point outPos) {
        if (outPos == null) return;
        int pageRank = rank % mMaxItemsPerPage;
        outPos.x = pageRank % mCountX;
        outPos.y = pageRank / mCountX;
    }
}
