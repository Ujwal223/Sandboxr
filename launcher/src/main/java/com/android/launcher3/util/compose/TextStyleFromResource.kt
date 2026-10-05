/*
 * Copyright (C) 2026 Sandboxr Platform
 */

package com.android.launcher3.util.compose

import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
fun textStyleFromResource(@StyleRes styleResId: Int): TextStyle =
    com.sandboxr.launcher.util.compose.textStyleFromResource(styleResId)
