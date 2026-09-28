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

import android.view.View;

import com.sandboxr.launcher.CellLayout;
import com.sandboxr.launcher.MultipageCellLayout;
import com.sandboxr.launcher.ShortcutAndWidgetContainer;
import com.sandboxr.launcher.util.GridOccupancy;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Variant of ReorderAlgorithm which simulates a foldable screen and adds a seam in the middle
 * to prevent items to be placed in the middle.
 */
public class MulticellReorderAlgorithm extends ReorderAlgorithm {

    private final View mSeam;

    public MulticellReorderAlgorithm(CellLayout cellLayout) {
        super(cellLayout);
        mSeam = new View(cellLayout.getContext());
    }

    public ItemConfiguration removeSeamFromSolution(ItemConfiguration solution) {
        solution.map.remove(mSeam);
        solution.map.forEach((view, cell) -> cell.cellX =
                cell.cellX > mCellLayout.getCountX() / 2 ? cell.cellX - 1 : cell.cellX);
        solution.cellX =
                solution.cellX > mCellLayout.getCountX() / 2 ? solution.cellX - 1 : solution.cellX;
        return solution;
    }

    @Override
    public ItemConfiguration closestEmptySpaceReorder(ReorderParameters reorderParameters) {
        return removeSeamFromSolution(simulateSeam(
                () -> super.closestEmptySpaceReorder(reorderParameters))
        );
    }

    @Override
    public ItemConfiguration findReorderSolution(ReorderParameters reorderParameters,
            boolean decX) {
        return removeSeamFromSolution(simulateSeam(
                () -> super.findReorderSolution(reorderParameters, decX)));
    }

    @Override
    public ItemConfiguration dropInPlaceSolution(ReorderParameters reorderParameters) {
        return removeSeamFromSolution(
                simulateSeam(() -> super.dropInPlaceSolution(reorderParameters))
        );
    }

    void addSeam() {
        if (!(mCellLayout instanceof MultipageCellLayout)) return;
        MultipageCellLayout mcl = (MultipageCellLayout) mCellLayout;
        mcl.setSeamWasAdded(true);
        CellLayoutLayoutParams lp = new CellLayoutLayoutParams(mcl.getCountX() / 2, 0, 1,
                mcl.getCountY());
        lp.canReorder = false;
        mcl.setCountX(mcl.getCountX() + 1);
        mcl.getShortcutsAndWidgets().addViewInLayout(mSeam, lp);
        mcl.setOccupied(createGridOccupancyWithSeam());
        mcl.mTmpOccupied = new GridOccupancy(mcl.getCountX(), mcl.getCountY());
    }

    void removeSeam() {
        if (!(mCellLayout instanceof MultipageCellLayout)) return;
        MultipageCellLayout mcl = (MultipageCellLayout) mCellLayout;
        mcl.setCountX(mcl.getCountX() - 1);
        mcl.getShortcutsAndWidgets().removeViewInLayout(mSeam);
        mcl.mTmpOccupied = new GridOccupancy(mcl.getCountX(), mcl.getCountY());
        mcl.setSeamWasAdded(false);
    }

    public <T> T simulateSeam(Supplier<T> f) {
        if (!(mCellLayout instanceof MultipageCellLayout)) {
            return f.get();
        }
        MultipageCellLayout mcl = (MultipageCellLayout) mCellLayout;
        if (mcl.isSeamWasAdded()) {
            return f.get();
        }
        GridOccupancy auxGrid = mcl.getOccupied();
        addSeam();
        T res = f.get();
        removeSeam();
        mcl.setOccupied(auxGrid);
        return res;
    }

    GridOccupancy createGridOccupancyWithSeam() {
        ShortcutAndWidgetContainer shortcutAndWidgets = mCellLayout.getShortcutsAndWidgets();
        GridOccupancy grid = new GridOccupancy(mCellLayout.getCountX(), mCellLayout.getCountY());
        for (int i = 0; i < shortcutAndWidgets.getChildCount(); i++) {
            View view = shortcutAndWidgets.getChildAt(i);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) view.getLayoutParams();
            int seamOffset = lp.getCellX() >= mCellLayout.getCountX() / 2 && lp.canReorder ? 1 : 0;
            grid.markCells(lp.getCellX() + seamOffset, lp.getCellY(), lp.cellHSpan, lp.cellVSpan,
                    true);
        }
        Arrays.fill(grid.cells[mCellLayout.getCountX() / 2], true);
        return grid;
    }
}
