/*
 * Copyright (C) 2026 Sandboxr Platform
 */

package com.android.launcher3.util.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sandboxr.launcher.util.compose.testTagContainer as sandboxrTestTagContainer
import com.sandboxr.launcher.util.compose.testTag as sandboxrTestTag

fun Modifier.testTagContainer(): Modifier = this.sandboxrTestTagContainer()

@Composable
fun Modifier.testTag(resId: String): Modifier = this.sandboxrTestTag(resId)
