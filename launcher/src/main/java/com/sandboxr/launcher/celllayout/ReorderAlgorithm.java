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

import android.graphics.Rect;
import android.view.View;

import com.sandboxr.launcher.CellLayout;
import com.sandboxr.launcher.util.CellAndSpan;
import com.sandboxr.launcher.util.GridOccupancy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Contains the logic for cell layout reordering and item displacement during drag operations.
 */
public class ReorderAlgorithm {

    CellLayout mCellLayout;

    public ReorderAlgorithm(CellLayout cellLayout) {
        mCellLayout = cellLayout;
    }

    public ItemConfiguration findReorderSolution(ReorderParameters reorderParameters, boolean decX) {
        return findReorderSolution(reorderParameters, mCellLayout.mDirectionVector, decX);
    }

    public ItemConfiguration findReorderSolution(ReorderParameters reorderParameters,
            int[] direction, boolean decX) {
        return findReorderSolutionRecursive(reorderParameters.getPixelX(),
                reorderParameters.getPixelY(), reorderParameters.getMinSpanX(),
                reorderParameters.getMinSpanY(), reorderParameters.getSpanX(),
                reorderParameters.getSpanY(), direction,
                reorderParameters.getDragView(), decX, reorderParameters.getSolution());
    }

    private ItemConfiguration findReorderSolutionRecursive(int pixelX, int pixelY, int minSpanX,
            int minSpanY, int spanX, int spanY, int[] direction, View dragView, boolean decX,
            ItemConfiguration solution) {
        mCellLayout.copyCurrentStateToSolution(solution);
        mCellLayout.getOccupied().copyTo(mCellLayout.mTmpOccupied);

        int[] result = new int[2];
        result = mCellLayout.findNearestAreaIgnoreOccupied(pixelX, pixelY, spanX, spanY, result);

        boolean success = rearrangementExists(result[0], result[1], spanX, spanY, direction, dragView,
                solution);

        if (!success) {
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return findReorderSolutionRecursive(pixelX, pixelY, minSpanX, minSpanY, spanX - 1,
                        spanY, direction, dragView, false, solution);
            } else if (spanY > minSpanY) {
                return findReorderSolutionRecursive(pixelX, pixelY, minSpanX, minSpanY, spanX,
                        spanY - 1, direction, dragView, true, solution);
            }
            solution.isSolution = false;
        } else {
            solution.isSolution = true;
            solution.cellX = result[0];
            solution.cellY = result[1];
            solution.spanX = spanX;
            solution.spanY = spanY;
        }
        return solution;
    }

    private boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction,
            View ignoreView, ItemConfiguration solution) {
        if (cellX < 0 || cellY < 0) return false;

        ArrayList<View> intersectingViews = new ArrayList<>();
        Rect occupiedRect = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);

        if (ignoreView != null) {
            CellAndSpan c = solution.map.get(ignoreView);
            if (c != null) {
                c.cellX = cellX;
                c.cellY = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();

        Comparator<View> comparator = Comparator.comparing(
                (View view) -> ((CellLayoutLayoutParams) view.getLayoutParams()).getCellX()
        ).thenComparing(
                (View view) -> ((CellLayoutLayoutParams) view.getLayoutParams()).getCellY()
        );
        List<View> views = solution.map.keySet().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        for (View child : views) {
            if (child == ignoreView) continue;
            CellAndSpan c = solution.map.get(child);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            r1.set(c.cellX, c.cellY, c.cellX + c.spanX, c.cellY + c.spanY);
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false;
                }
                intersectingViews.add(child);
            }
        }

        solution.intersectingViews = intersectingViews;

        if (attemptPushInDirection(intersectingViews, occupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        if (addViewsToTempLocation(intersectingViews, occupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        for (View v : intersectingViews) {
            if (!addViewToTempLocation(v, occupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }

    private boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop, int[] direction,
            ItemConfiguration currentState) {
        CellAndSpan c = currentState.map.get(v);
        boolean success = false;
        mCellLayout.mTmpOccupied.markCells(c, false);
        mCellLayout.mTmpOccupied.markCells(rectOccupiedByPotentialDrop, true);

        int[] tmpLocation = findNearestArea(c.cellX, c.cellY, c.spanX, c.spanY, direction,
                mCellLayout.mTmpOccupied.cells, null, new int[2]);

        if (tmpLocation[0] >= 0 && tmpLocation[1] >= 0) {
            c.cellX = tmpLocation[0];
            c.cellY = tmpLocation[1];
            success = true;
        }
        mCellLayout.mTmpOccupied.markCells(c, true);
        return success;
    }

    private boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {

        ViewCluster cluster = new ViewCluster(mCellLayout, views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        int whichEdge;
        int pushDistance;
        boolean fail = false;

        if (direction[0] < 0) {
            whichEdge = ViewCluster.LEFT;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = ViewCluster.RIGHT;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = ViewCluster.TOP;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = ViewCluster.BOTTOM;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }

        if (pushDistance <= 0) {
            return false;
        }

        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            mCellLayout.mTmpOccupied.markCells(c, false);
        }

        currentState.save();
        cluster.sortConfigurationForEdgePush(whichEdge);

        while (pushDistance > 0 && !fail) {
            for (View v : currentState.sortedViews) {
                if (!cluster.views.contains(v) && v != dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
                        if (!lp.canReorder) {
                            fail = true;
                            break;
                        }
                        cluster.addView(v);
                        CellAndSpan c = currentState.map.get(v);
                        mCellLayout.mTmpOccupied.markCells(c, false);
                    }
                }
            }
            pushDistance--;
            cluster.shift(whichEdge, 1);
        }

        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();

        if (!fail && clusterRect.left >= 0 && clusterRect.right <= mCellLayout.getCountX()
                && clusterRect.top >= 0 && clusterRect.bottom <= mCellLayout.getCountY()) {
            foundSolution = true;
        } else {
            currentState.restore();
        }

        for (View v : cluster.views) {
            CellAndSpan c = currentState.map.get(v);
            mCellLayout.mTmpOccupied.markCells(c, true);
        }

        return foundSolution;
    }

    private void revertDir(int[] direction) {
        direction[0] *= -1;
        direction[1] *= -1;
    }

    private boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
            int[] direction, View ignoreView, ItemConfiguration solution) {
        if ((Math.abs(direction[0]) + Math.abs(direction[1])) > 1) {
            int temp;
            for (int j = 0; j < 2; j++) {
                for (int i = 1; i >= 0; i--) {
                    temp = direction[i];
                    direction[i] = 0;
                    if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView,
                            solution)) {
                        return true;
                    }
                    direction[i] = temp;
                }
                revertDir(direction);
            }
        } else {
            int temp;
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < 2; i++) {
                    if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView,
                            solution)) {
                        return true;
                    }
                    revertDir(direction);
                }
                temp = direction[1];
                direction[1] = direction[0];
                direction[0] = temp;
            }
        }
        return false;
    }

    private boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.isEmpty()) return true;

        boolean success = false;
        Rect boundingRect = new Rect();
        currentState.getBoundingRectForViews(views, boundingRect);

        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            mCellLayout.mTmpOccupied.markCells(c, false);
        }

        GridOccupancy blockOccupied = new GridOccupancy(boundingRect.width(),
                boundingRect.height());
        int top = boundingRect.top;
        int left = boundingRect.left;
        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            blockOccupied.markCells(c.cellX - left, c.cellY - top, c.spanX, c.spanY, true);
        }

        mCellLayout.mTmpOccupied.markCells(rectOccupiedByPotentialDrop, true);

        int[] tmpLocation = findNearestArea(boundingRect.left, boundingRect.top,
                boundingRect.width(), boundingRect.height(), direction,
                mCellLayout.mTmpOccupied.cells, blockOccupied.cells, new int[2]);

        if (tmpLocation[0] >= 0 && tmpLocation[1] >= 0) {
            int deltaX = tmpLocation[0] - boundingRect.left;
            int deltaY = tmpLocation[1] - boundingRect.top;
            for (View v : views) {
                CellAndSpan c = currentState.map.get(v);
                c.cellX += deltaX;
                c.cellY += deltaY;
            }
            success = true;
        }

        for (View v : views) {
            CellAndSpan c = currentState.map.get(v);
            mCellLayout.mTmpOccupied.markCells(c, true);
        }
        return success;
    }

    public ItemConfiguration dropInPlaceSolution(ReorderParameters reorderParameters) {
        int[] result = mCellLayout.findNearestAreaIgnoreOccupied(reorderParameters.getPixelX(),
                reorderParameters.getPixelY(), reorderParameters.getSpanX(),
                reorderParameters.getSpanY(), new int[2]);
        ItemConfiguration solution = new ItemConfiguration();
        mCellLayout.copyCurrentStateToSolution(solution);

        solution.isSolution = !isConfigurationRegionOccupied(
                new Rect(result[0], result[1], result[0] + reorderParameters.getSpanX(),
                        result[1] + reorderParameters.getSpanY()), solution,
                reorderParameters.getDragView());
        if (!solution.isSolution) {
            return solution;
        }
        solution.cellX = result[0];
        solution.cellY = result[1];
        solution.spanX = reorderParameters.getSpanX();
        solution.spanY = reorderParameters.getSpanY();
        return solution;
    }

    private boolean isConfigurationRegionOccupied(Rect region, ItemConfiguration configuration,
            View ignoreView) {
        return configuration.map
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != ignoreView)
                .map(Entry::getValue)
                .anyMatch(cellAndSpan -> region.intersect(
                        cellAndSpan.cellX,
                        cellAndSpan.cellY,
                        cellAndSpan.cellX + cellAndSpan.spanX,
                        cellAndSpan.cellY + cellAndSpan.spanY
                        )
                );
    }

    public ItemConfiguration closestEmptySpaceReorder(ReorderParameters reorderParameters) {
        ItemConfiguration solution = new ItemConfiguration();
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        mCellLayout.findNearestVacantArea(reorderParameters.getPixelX(),
                reorderParameters.getPixelY(), reorderParameters.getMinSpanX(),
                reorderParameters.getMinSpanY(), reorderParameters.getSpanX(),
                reorderParameters.getSpanY(), result, resultSpan);
        if (result[0] >= 0 && result[1] >= 0) {
            mCellLayout.copyCurrentStateToSolution(solution);
            solution.cellX = result[0];
            solution.cellY = result[1];
            solution.spanX = resultSpan[0];
            solution.spanY = resultSpan[1];
            solution.isSolution = true;
        } else {
            solution.isSolution = false;
        }
        return solution;
    }

    public ItemConfiguration calculateReorder(ReorderParameters reorderParameters) {
        getDirectionVectorForDrop(reorderParameters, mCellLayout.mDirectionVector);

        ItemConfiguration dropInPlaceSolution = dropInPlaceSolution(reorderParameters);
        ItemConfiguration swapSolution = findReorderSolution(reorderParameters, true);
        ItemConfiguration closestSpaceSolution = closestEmptySpaceReorder(reorderParameters);

        if (swapSolution.isSolution && swapSolution.area() >= closestSpaceSolution.area()) {
            return swapSolution;
        } else if (closestSpaceSolution.isSolution) {
            return closestSpaceSolution;
        } else if (dropInPlaceSolution.isSolution) {
            return dropInPlaceSolution;
        }
        return null;
    }

    private void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan(deltaY / (deltaX == 0 ? 0.0001f : deltaX));

        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5f) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5f) {
            result[1] = (int) Math.signum(deltaY);
        }
    }

    public void getDirectionVectorForDrop(ReorderParameters reorderParameters,
            int[] resultDirection) {
        int[] targetDestination = new int[2];

        mCellLayout.findNearestAreaIgnoreOccupied(reorderParameters.getPixelX(),
                reorderParameters.getPixelY(), reorderParameters.getSpanX(),
                reorderParameters.getSpanY(), targetDestination);
        Rect dragRect = new Rect();
        mCellLayout.cellToRect(targetDestination[0], targetDestination[1],
                reorderParameters.getSpanX(), reorderParameters.getSpanY(), dragRect);
        dragRect.offset(reorderParameters.getPixelX() - dragRect.centerX(),
                reorderParameters.getPixelY() - dragRect.centerY());

        Rect region = new Rect(targetDestination[0], targetDestination[1],
                targetDestination[0] + reorderParameters.getSpanX(),
                targetDestination[1] + reorderParameters.getSpanY());
        Rect dropRegionRect = mCellLayout.getIntersectingRectanglesInRegion(region,
                reorderParameters.getDragView());
        if (dropRegionRect == null) dropRegionRect = new Rect(region);

        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();

        mCellLayout.cellToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
                dropRegionRect.height(), dropRegionRect);

        int spanX = Math.max(1, reorderParameters.getSpanX());
        int spanY = Math.max(1, reorderParameters.getSpanY());
        int deltaX = (dropRegionRect.centerX() - reorderParameters.getPixelX()) / spanX;
        int deltaY = (dropRegionRect.centerY() - reorderParameters.getPixelY()) / spanY;

        if (dropRegionSpanX == mCellLayout.getCountX()
                || reorderParameters.getSpanX() == mCellLayout.getCountX()) {
            deltaX = 0;
        }
        if (dropRegionSpanY == mCellLayout.getCountY()
                || reorderParameters.getSpanY() == mCellLayout.getCountY()) {
            deltaY = 0;
        }

        if (deltaX == 0 && deltaY == 0) {
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector(deltaX, deltaY, resultDirection);
        }
    }

    public int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction,
            boolean[][] occupied, boolean[][] blockOccupied, int[] result) {
        final int[] bestXY = result != null ? result : new int[2];
        float bestDistance = Float.MAX_VALUE;
        int bestDirectionScore = Integer.MIN_VALUE;

        final int countX = mCellLayout.getCountX();
        final int countY = mCellLayout.getCountY();

        for (int y = 0; y < countY - (spanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (spanX - 1); x++) {
                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[x + i][y + j] && (blockOccupied == null
                                || blockOccupied[i][j])) {
                            continue inner;
                        }
                    }
                }

                float distance = (float) Math.hypot(x - cellX, y - cellY);
                int[] curDirection = new int[2];
                computeDirectionVector(x - cellX, y - cellY, curDirection);
                int curDirectionScore =
                        direction[0] * curDirection[0] + direction[1] * curDirection[1];
                if (Float.compare(distance, bestDistance) < 0 || (Float.compare(distance,
                        bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                    bestDistance = distance;
                    bestDirectionScore = curDirectionScore;
                    bestXY[0] = x;
                    bestXY[1] = y;
                }
            }
        }

        if (bestDistance == Float.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }
}
