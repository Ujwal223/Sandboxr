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

package com.sandboxr.launcher.celllayout;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewDebug;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

/**
 * Represents the layout parameters of an item inside a [CellLayout], including grid
 * coordinates (cellX, cellY), cell spans (cellHSpan, cellVSpan), and measured pixel bounds.
 */
public class CellLayoutLayoutParams extends ViewGroup.MarginLayoutParams {

    @ViewDebug.ExportedProperty
    private int mCellX;

    @ViewDebug.ExportedProperty
    private int mCellY;

    private int mTmpCellX;
    private int mTmpCellY;

    public boolean useTmpCoords;

    @ViewDebug.ExportedProperty
    public int cellHSpan;

    @ViewDebug.ExportedProperty
    public int cellVSpan;

    public boolean isLockedToGrid = true;
    public boolean canReorder = true;

    @ViewDebug.ExportedProperty
    public int x;

    @ViewDebug.ExportedProperty
    public int y;

    public boolean dropped;

    public CellLayoutLayoutParams(Context c, AttributeSet attrs) {
        super(c, attrs);
        cellHSpan = 1;
        cellVSpan = 1;
    }

    public CellLayoutLayoutParams(ViewGroup.LayoutParams source) {
        super(source);
        cellHSpan = 1;
        cellVSpan = 1;
    }

    public CellLayoutLayoutParams(CellLayoutLayoutParams source) {
        super(source);
        this.mCellX = source.getCellX();
        this.mCellY = source.getCellY();
        this.cellHSpan = source.cellHSpan;
        this.cellVSpan = source.cellVSpan;
        this.mTmpCellX = source.getTmpCellX();
        this.mTmpCellY = source.getTmpCellY();
        this.useTmpCoords = source.useTmpCoords;
    }

    public CellLayoutLayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
        super(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.mCellX = cellX;
        this.mCellY = cellY;
        this.cellHSpan = cellHSpan;
        this.cellVSpan = cellVSpan;
    }

    public void setup(int cellWidth, int cellHeight, boolean invertHorizontally, int colCount,
            int rowCount, Point borderSpace) {
        setup(cellWidth, cellHeight, invertHorizontally, colCount, rowCount, 1.0f, 1.0f,
                borderSpace, null);
    }

    public void setup(int cellWidth, int cellHeight, boolean invertHorizontally, int colCount,
            int rowCount, float cellScaleX, float cellScaleY, Point borderSpace,
            @Nullable Rect inset) {
        if (isLockedToGrid) {
            final int myCellHSpan = cellHSpan;
            final int myCellVSpan = cellVSpan;
            int myCellX = useTmpCoords ? getTmpCellX() : getCellX();
            int myCellY = useTmpCoords ? getTmpCellY() : getCellY();

            if (invertHorizontally) {
                myCellX = colCount - myCellX - cellHSpan;
            }

            int hBorderSpacing = (myCellHSpan - 1) * (borderSpace != null ? borderSpace.x : 0);
            int vBorderSpacing = (myCellVSpan - 1) * (borderSpace != null ? borderSpace.y : 0);

            float myCellWidth = ((myCellHSpan * cellWidth) + hBorderSpacing) / cellScaleX;
            float myCellHeight = ((myCellVSpan * cellHeight) + vBorderSpacing) / cellScaleY;

            width = Math.round(myCellWidth) - leftMargin - rightMargin;
            height = Math.round(myCellHeight) - topMargin - bottomMargin;
            x = leftMargin + (myCellX * cellWidth) + (myCellX * (borderSpace != null ? borderSpace.x : 0));
            y = topMargin + (myCellY * cellHeight) + (myCellY * (borderSpace != null ? borderSpace.y : 0));

            if (inset != null) {
                x += inset.left;
                y += inset.top;
                width -= inset.left + inset.right;
                height -= inset.top + inset.bottom;
            }
        }
    }

    public void setCellXY(Point point) {
        setCellX(point.x);
        setCellY(point.y);
    }

    @Override
    public String toString() {
        return "(" + this.getCellX() + ", " + this.getCellY() + ")";
    }

    public int getCellX() {
        return mCellX;
    }

    public void setCellX(int cellX) {
        this.mCellX = cellX;
    }

    public int getCellY() {
        return mCellY;
    }

    public void setCellY(int cellY) {
        this.mCellY = cellY;
    }

    public int getTmpCellX() {
        return mTmpCellX;
    }

    public void setTmpCellX(int tmpCellX) {
        this.mTmpCellX = tmpCellX;
    }

    public int getTmpCellY() {
        return mTmpCellY;
    }

    public void setTmpCellY(int tmpCellY) {
        this.mTmpCellY = tmpCellY;
    }
}
