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

package com.sandboxr.launcher.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Point
import android.util.Log
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.LauncherSettings.Favorites.CELLX
import com.sandboxr.launcher.LauncherSettings.Favorites.CELLY
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.sandboxr.launcher.LauncherSettings.Favorites.CONTAINER_HOTSEAT
import com.sandboxr.launcher.LauncherSettings.Favorites.SCREEN
import com.sandboxr.launcher.LauncherSettings.Favorites.SPANX
import com.sandboxr.launcher.LauncherSettings.Favorites.SPANY
import com.sandboxr.launcher.LauncherSettings.Favorites.TABLE_NAME
import com.sandboxr.launcher.LauncherSettings.Favorites._ID
import com.sandboxr.launcher.provider.LauncherDbUtils.SQLiteTransaction

/**
 * Task responsible for importing and migrating workspace favorites across different grid dimensions
 * or database files.
 */
class ImportDataTask(
    private val context: Context,
    private val srcDb: SQLiteDatabase,
    private val destDb: SQLiteDatabase,
    private val srcGridSize: Point,
    private val destGridSize: Point,
) {

    /**
     * Executes the grid migration / data import.
     *
     * @return Number of favorites migrated to [destDb].
     */
    fun importData(): Int {
        if (!LauncherDbUtils.tableExists(srcDb, TABLE_NAME)) return 0
        if (!LauncherDbUtils.tableExists(destDb, TABLE_NAME)) {
            Favorites.addTableToDb(destDb, 0L, optional = false)
        }

        var importedCount = 0

        try {
            SQLiteTransaction(destDb).use { tx ->
                srcDb.query(
                    TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "$CONTAINER ASC, $SCREEN ASC, $CELLY ASC, $CELLX ASC"
                ).use { cursor ->
                    val columnNames = cursor.columnNames
                    val occupiedCells = mutableMapOf<Int, Array<BooleanArray>>()

                    while (cursor.moveToNext()) {
                        val values = ContentValues()
                        for (col in columnNames) {
                            val idx = cursor.getColumnIndex(col)
                            if (cursor.isNull(idx)) {
                                values.putNull(col)
                            } else {
                                values.put(col, cursor.getString(idx))
                            }
                        }

                        val container = values.getAsInteger(CONTAINER) ?: CONTAINER_DESKTOP
                        if (container == CONTAINER_HOTSEAT) {
                            // Hotseat items are retained directly
                            destDb.insertWithOnConflict(
                                TABLE_NAME,
                                null,
                                values,
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                            importedCount++
                            continue
                        }

                        if (container != CONTAINER_DESKTOP) {
                            // Folder items or other container items preserved as-is
                            destDb.insertWithOnConflict(
                                TABLE_NAME,
                                null,
                                values,
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                            importedCount++
                            continue
                        }

                        // Desktop container placement
                        val screen = values.getAsInteger(SCREEN) ?: 0
                        val cellX = values.getAsInteger(CELLX) ?: 0
                        val cellY = values.getAsInteger(CELLY) ?: 0
                        val spanX = (values.getAsInteger(SPANX) ?: 1).coerceAtMost(destGridSize.x)
                        val spanY = (values.getAsInteger(SPANY) ?: 1).coerceAtMost(destGridSize.y)

                        values.put(SPANX, spanX)
                        values.put(SPANY, spanY)

                        val screenGrid = occupiedCells.getOrPut(screen) {
                            Array(destGridSize.x) { BooleanArray(destGridSize.y) }
                        }

                        val targetPos = findFittingPosition(screenGrid, spanX, spanY, cellX, cellY)
                        if (targetPos != null) {
                            values.put(CELLX, targetPos.x)
                            values.put(CELLY, targetPos.y)
                            markCellsOccupied(screenGrid, targetPos.x, targetPos.y, spanX, spanY)
                        } else {
                            // Overflow to a new screen
                            val overflowScreen = (occupiedCells.keys.maxOrNull() ?: screen) + 1
                            val newScreenGrid = occupiedCells.getOrPut(overflowScreen) {
                                Array(destGridSize.x) { BooleanArray(destGridSize.y) }
                            }
                            values.put(SCREEN, overflowScreen)
                            values.put(CELLX, 0)
                            values.put(CELLY, 0)
                            markCellsOccupied(newScreenGrid, 0, 0, spanX, spanY)
                        }

                        destDb.insertWithOnConflict(
                            TABLE_NAME,
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_REPLACE
                        )
                        importedCount++
                    }
                }
                tx.commit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during ImportDataTask migration", e)
        }

        return importedCount
    }

    private fun findFittingPosition(
        grid: Array<BooleanArray>,
        spanX: Int,
        spanY: Int,
        preferredX: Int,
        preferredY: Int,
    ): Point? {
        val maxX = destGridSize.x - spanX
        val maxY = destGridSize.y - spanY
        if (maxX < 0 || maxY < 0) return null

        val clampedPrefX = preferredX.coerceIn(0, maxX)
        val clampedPrefY = preferredY.coerceIn(0, maxY)

        if (isRegionEmpty(grid, clampedPrefX, clampedPrefY, spanX, spanY)) {
            return Point(clampedPrefX, clampedPrefY)
        }

        // Search for nearest free slot
        for (y in 0..maxY) {
            for (x in 0..maxX) {
                if (isRegionEmpty(grid, x, y, spanX, spanY)) {
                    return Point(x, y)
                }
            }
        }
        return null
    }

    private fun isRegionEmpty(grid: Array<BooleanArray>, x: Int, y: Int, spanX: Int, spanY: Int): Boolean {
        for (i in x until x + spanX) {
            for (j in y until y + spanY) {
                if (grid[i][j]) return false
            }
        }
        return true
    }

    private fun markCellsOccupied(grid: Array<BooleanArray>, x: Int, y: Int, spanX: Int, spanY: Int) {
        for (i in x until (x + spanX).coerceAtMost(destGridSize.x)) {
            for (j in y until (y + spanY).coerceAtMost(destGridSize.y)) {
                grid[i][j] = true
            }
        }
    }

    companion object {
        private const val TAG = "ImportDataTask"

        @JvmStatic
        fun performImport(
            context: Context,
            srcDb: SQLiteDatabase,
            destDb: SQLiteDatabase,
            srcGrid: Point,
            destGrid: Point,
        ): Int {
            return ImportDataTask(context, srcDb, destDb, srcGrid, destGrid).importData()
        }
    }
}
