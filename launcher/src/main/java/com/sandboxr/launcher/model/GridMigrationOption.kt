/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.sandboxr.launcher.model

/**
 * Valid grid configurations for grid migration checks.
 */
sealed class GridMigrationOption(val columns: Int, val rows: Int) {

    fun canMigrate(destGridMigrationOption: GridMigrationOption, isAfterRestore: Boolean): Boolean {
        return validDestinations.contains(destGridMigrationOption) || isAfterRestore
    }

    private val validDestinations: List<GridMigrationOption>
        get() =
            when (this) {
                TwoByTwo,
                ThreeByThree,
                FourByFour,
                FourByFive,
                FourBySix,
                FiveByFive,
                FiveBySix,
                EightByThree,
                SevenByThree -> validDestinationsForPhone
                SixByFive -> validDestinationsForTablet
            }

    private val validDestinationsForPhone: List<GridMigrationOption>
        get() =
            listOf(
                TwoByTwo,
                ThreeByThree,
                FourByFour,
                FourByFive,
                FourBySix,
                FiveByFive,
                FiveBySix,
                EightByThree,
                SevenByThree,
            )

    private val validDestinationsForTablet: List<GridMigrationOption>
        get() = listOf(SixByFive)

    data object TwoByTwo : GridMigrationOption(columns = 2, rows = 2)
    data object ThreeByThree : GridMigrationOption(columns = 3, rows = 3)
    data object FourByFour : GridMigrationOption(columns = 4, rows = 4)
    data object FourByFive : GridMigrationOption(columns = 4, rows = 5)
    data object FourBySix : GridMigrationOption(columns = 4, rows = 6)
    data object FiveByFive : GridMigrationOption(columns = 5, rows = 5)
    data object FiveBySix : GridMigrationOption(columns = 5, rows = 6)
    data object SixByFive : GridMigrationOption(columns = 6, rows = 5)
    data object EightByThree : GridMigrationOption(columns = 8, rows = 3)
    data object SevenByThree : GridMigrationOption(columns = 7, rows = 3)

    companion object {
        @JvmStatic
        fun from(columns: Int, rows: Int): GridMigrationOption? =
            when {
                columns == 2 && rows == 2 -> TwoByTwo
                columns == 3 && rows == 3 -> ThreeByThree
                columns == 4 && rows == 4 -> FourByFour
                columns == 4 && rows == 5 -> FourByFive
                columns == 4 && rows == 6 -> FourBySix
                columns == 5 && rows == 5 -> FiveByFive
                columns == 5 && rows == 6 -> FiveBySix
                columns == 6 && rows == 5 -> SixByFive
                columns == 8 && rows == 3 -> EightByThree
                columns == 7 && rows == 3 -> SevenByThree
                else -> null
            }
    }
}
